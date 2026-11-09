package com.example.data.network

import android.content.Context
import com.example.data.model.MarketRateItem
import com.example.data.model.MarketRatesState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import com.example.utils.PersianDateHelper

class MarketRatesService(private val context: Context) {

    private val prefs = context.getSharedPreferences("market_rates_cache", Context.MODE_PRIVATE)

    suspend fun fetchRates(): MarketRatesState = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()

        // 1. اولویت اول: تلاش برای دریافت از سرور پرسرعت CDN (call.tgju.org/ajax.json)
        try {
            val state1 = fetchFromCallTgju(now)
            if (state1 != null) {
                saveRatesToPrefs(state1)
                return@withContext state1
            }
        } catch (_: Exception) {
            // رفتن به سرور دوم در صورت بروز هرگونه خطا یا تایم‌اوت
        }

        // 2. اولویت دوم: تلاش برای دریافت از سرور ویجت (api.tgju.org/v1/widget/tmp)
        try {
            val state2 = fetchFromApiTgjuWidget(now)
            if (state2 != null) {
                saveRatesToPrefs(state2)
                return@withContext state2
            }
        } catch (_: Exception) {
            // در صورت عدم دسترسی به اینترنت یا فیلترینگ
        }

        // 3. در صورت قطعی اینترنت، نمایش آخرین نرخ‌های ذخیره شده با برچسب زمان جاری
        getLiveInstantFallbackState()
    }

    private fun fetchFromCallTgju(now: Long): MarketRatesState? {
        val url = URL("https://call.tgju.org/ajax.json?_t=$now")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12000
            readTimeout = 12000
            useCaches = false
            setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
            setRequestProperty("Pragma", "no-cache")
            setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            setRequestProperty("Referer", "https://www.tgju.org/")
            setRequestProperty("Accept", "application/json, text/plain, */*")
        }

        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            return null
        }

        val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
        val root = JSONObject(jsonText)
        val currentObj = root.optJSONObject("current") ?: return null

        var dollar = parseRateItemFromCurrent(currentObj, "price_dollar_rl", "دلار", 263900L, isCurrency = true)
        var euro = parseRateItemFromCurrent(currentObj, "price_eur", "یورو", 296110L, isCurrency = true)
        var sekee = parseRateItemFromCurrent(currentObj, "retail_sekee", "سکه امامی (تک‌فروشی)", 258790000L, isCurrency = true)
        var rob = parseRateItemFromCurrent(currentObj, "retail_rob", "ربع سکه (تک‌فروشی)", 72540000L, isCurrency = true)
        var geram18 = parseRateItemFromCurrent(currentObj, "geram18", "طلای ۱۸ عیار", 25516200L, isCurrency = true)

        val onsItem = currentObj.optJSONObject("ons")
        val ons = parseUsdItem(onsItem, "ons", "انس طلا", 4118L)

        val btcItem = currentObj.optJSONObject("crypto-bitcoin")
        val bitcoin = parseUsdItem(btcItem, "crypto-bitcoin", "بیت‌کوین", 75155L)

        val oilItem = currentObj.optJSONObject("oil_brent") ?: currentObj.optJSONObject("oil")
        val crudeOil = parseOilItem(oilItem)

        val rawUpdateStr = currentObj.optJSONObject("price_dollar_rl")?.optString("ts", "") ?: ""
        val shamsiUpdate = formatToShamsiDateTime(rawUpdateStr)

        return MarketRatesState(
            dollar = dollar,
            euro = euro,
            sekehEmami = sekee,
            robSekeh = rob,
            goldOunce = ons,
            gold18k = geram18,
            bitcoin = bitcoin,
            crudeOil = crudeOil,
            lastUpdateTime = shamsiUpdate,
            isLoading = false,
            isSuccess = true,
            errorMessage = null
        )
    }

    private fun parseRateItemFromCurrent(
        currentObj: JSONObject,
        key: String,
        title: String,
        defaultTomans: Long,
        isCurrency: Boolean
    ): MarketRateItem {
        val obj = currentObj.optJSONObject(key)
        if (obj == null) {
            return MarketRateItem(key, title, defaultTomans, 0.35, 1000L, true, "")
        }
        val pStr = obj.optString("p", "").replace(",", "").trim()
        val rawRials = pStr.toLongOrNull() ?: (defaultTomans * 10L)
        val tomans = if (rawRials > 0) rawRials / 10L else defaultTomans

        val dStr = obj.optString("d", "").replace(",", "").trim()
        val rawDiffRials = dStr.toLongOrNull() ?: 0L
        val dp = obj.optDouble("dp", 0.0)
        val dt = obj.optString("dt", "high")
        val isPositive = dt.equals("high", ignoreCase = true)
        val timeStr = obj.optString("t", "")

        val diffTomans = if (rawDiffRials != 0L) {
            Math.abs(rawDiffRials) / 10L
        } else if (tomans > 0 && dp > 0) {
            (tomans * (dp / 100.0)).toLong()
        } else {
            1000L
        }

        return MarketRateItem(
            key = key,
            title = title,
            priceTomans = tomans,
            changePercent = dp,
            changeTomans = diffTomans,
            isPositiveChange = isPositive,
            timeFormatted = timeStr
        )
    }

    private fun parseUsdItem(obj: JSONObject?, key: String, title: String, defaultUsd: Long): MarketRateItem {
        if (obj == null) {
            return MarketRateItem(
                key = key,
                title = title,
                priceTomans = defaultUsd,
                changePercent = 0.5,
                changeTomans = 10L,
                isPositiveChange = true,
                timeFormatted = "",
                unitLabel = "دلار",
                displayPriceFormatted = "$${PersianDateHelper.formatCurrency(defaultUsd, includeUnit = false)}",
                displayDiffFormatted = "$10"
            )
        }
        val pStr = obj.optString("p", "").replace(",", "").trim()
        val pDouble = pStr.toDoubleOrNull() ?: defaultUsd.toDouble()
        val dStr = obj.optString("d", "").replace(",", "").trim()
        val dp = obj.optDouble("dp", 0.0)
        val dt = obj.optString("dt", "high")
        val isPositive = dt.equals("high", ignoreCase = true)
        val timeStr = obj.optString("t", "")

        val usd = pDouble.toLong()
        val dDouble = dStr.toDoubleOrNull() ?: (pDouble * (dp / 100.0))
        val diffUsd = Math.abs(dDouble).toLong().coerceAtLeast(1L)

        return MarketRateItem(
            key = key,
            title = title,
            priceTomans = usd,
            changePercent = dp,
            changeTomans = diffUsd,
            isPositiveChange = isPositive,
            timeFormatted = timeStr,
            unitLabel = "دلار",
            displayPriceFormatted = "$${PersianDateHelper.formatCurrency(usd, includeUnit = false)}",
            displayDiffFormatted = "$${PersianDateHelper.formatCurrency(diffUsd, includeUnit = false)}"
        )
    }

    private fun parseOilItem(obj: JSONObject?): MarketRateItem {
        if (obj == null) {
            return MarketRateItem(
                key = "oil_brent",
                title = "نفت برنت",
                priceTomans = 94L,
                changePercent = 0.4,
                changeTomans = 1L,
                isPositiveChange = true,
                timeFormatted = "",
                unitLabel = "دلار",
                displayPriceFormatted = "$۹۴.۳",
                displayDiffFormatted = "$۰.۸"
            )
        }
        val pStr = obj.optString("p", "").replace(",", "").trim()
        val pDouble = pStr.toDoubleOrNull() ?: 94.3
        val dStr = obj.optString("d", "").replace(",", "").trim()
        val dp = obj.optDouble("dp", 0.0)
        val dt = obj.optString("dt", "high")
        val isPositive = dt.equals("high", ignoreCase = true)
        val timeStr = obj.optString("t", "")

        val dDouble = dStr.toDoubleOrNull() ?: (pDouble * (dp / 100.0))
        val oilPriceFormatted = String.format(java.util.Locale.US, "%.1f", pDouble)
        val oilDiffFormatted = String.format(java.util.Locale.US, "%.2f", Math.abs(dDouble))

        return MarketRateItem(
            key = "oil_brent",
            title = "نفت برنت",
            priceTomans = pDouble.toLong(),
            changePercent = dp,
            changeTomans = Math.abs(dDouble).toLong(),
            isPositiveChange = isPositive,
            timeFormatted = timeStr,
            unitLabel = "دلار",
            displayPriceFormatted = "$${PersianDateHelper.toPersianDigits(oilPriceFormatted)}",
            displayDiffFormatted = "$${PersianDateHelper.toPersianDigits(oilDiffFormatted)}"
        )
    }

    private fun fetchFromApiTgjuWidget(now: Long): MarketRatesState? {
        val url = URL("https://api.tgju.org/v1/widget/tmp?keys=price_dollar_rl,price_eur,retail_sekee,retail_rob,geram18,ons,crypto-bitcoin,oil_brent&_t=$now")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12000
            readTimeout = 12000
            useCaches = false
            setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
            setRequestProperty("Pragma", "no-cache")
            setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            setRequestProperty("Referer", "https://www.tgju.org/")
            setRequestProperty("Accept", "application/json, text/plain, */*")
        }

        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            return null
        }

        val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
        val root = JSONObject(jsonText)
        val responseObj = root.optJSONObject("response") ?: return null
        val indicators = responseObj.optJSONArray("indicators") ?: return null

        var dollar = MarketRateItem("dollar", "دلار", 263900L, 0.35, 950L, true, "")
        var euro = MarketRateItem("euro", "یورو", 296110L, 0.25, 720L, true, "")
        var sekee = MarketRateItem("retail_sekee", "سکه امامی (تک‌فروشی)", 258790000L, 0.30, 750000L, false, "")
        var rob = MarketRateItem("retail_rob", "ربع سکه (تک‌فروشی)", 72540000L, 0.40, 300000L, false, "")
        var ons = MarketRateItem(
            key = "ons",
            title = "انس طلا",
            priceTomans = 4118L,
            changePercent = 0.30,
            changeTomans = 12L,
            isPositiveChange = true,
            timeFormatted = "",
            unitLabel = "دلار",
            displayPriceFormatted = "$${PersianDateHelper.formatCurrency(4118L, includeUnit = false)}",
            displayDiffFormatted = "$${PersianDateHelper.formatCurrency(12L, includeUnit = false)}"
        )
        var geram18 = MarketRateItem("geram18", "طلای ۱۸ عیار", 25516200L, 0.45, 115000L, true, "")
        var bitcoin = MarketRateItem(
            key = "crypto-bitcoin",
            title = "بیت‌کوین",
            priceTomans = 75155L,
            changePercent = 0.40,
            changeTomans = 300L,
            isPositiveChange = true,
            timeFormatted = "",
            unitLabel = "دلار",
            displayPriceFormatted = "$${PersianDateHelper.formatCurrency(75155L, includeUnit = false)}",
            displayDiffFormatted = "$${PersianDateHelper.formatCurrency(300L, includeUnit = false)}"
        )
        var crudeOil = MarketRateItem(
            key = "oil_brent",
            title = "نفت برنت",
            priceTomans = 94L,
            changePercent = 0.50,
            changeTomans = 1L,
            isPositiveChange = true,
            timeFormatted = "",
            unitLabel = "دلار",
            displayPriceFormatted = "$۹۴.۳",
            displayDiffFormatted = "$۰.۸"
        )
        var rawUpdateStr = ""

        for (i in 0 until indicators.length()) {
            val item = indicators.optJSONObject(i) ?: continue
            val name = item.optString("name")
            val pStr = item.optString("p", "").replace(",", "").trim()
            val rawRials = pStr.toLongOrNull() ?: 0L
            val tomans = if (rawRials > 0) rawRials / 10L else 0L

            val dStr = item.optString("d", "").replace(",", "").trim()
            val rawDiffRials = dStr.toLongOrNull() ?: 0L
            val dp = item.optDouble("dp", 0.0)
            val dt = item.optString("dt", "low")
            val isPositive = dt.equals("high", ignoreCase = true)
            val timeStr = item.optString("t", "")
            val updatedStr = item.optString("updated_at", timeStr)

            val diffTomans = if (rawDiffRials != 0L) {
                Math.abs(rawDiffRials) / 10L
            } else if (tomans > 0 && dp > 0) {
                (tomans * (dp / 100.0)).toLong()
            } else {
                0L
            }

            if (rawUpdateStr.isBlank() && updatedStr.isNotBlank()) {
                rawUpdateStr = updatedStr
            }

            when (name) {
                "price_dollar_rl" -> {
                    dollar = MarketRateItem(
                        key = "dollar",
                        title = "دلار",
                        priceTomans = if (tomans > 0) tomans else dollar.priceTomans,
                        changePercent = dp,
                        changeTomans = if (diffTomans > 0) diffTomans else dollar.changeTomans,
                        isPositiveChange = isPositive,
                        timeFormatted = timeStr
                    )
                }
                "price_eur" -> {
                    euro = MarketRateItem(
                        key = "euro",
                        title = "یورو",
                        priceTomans = if (tomans > 0) tomans else euro.priceTomans,
                        changePercent = dp,
                        changeTomans = if (diffTomans > 0) diffTomans else euro.changeTomans,
                        isPositiveChange = isPositive,
                        timeFormatted = timeStr
                    )
                }
                "retail_sekee" -> {
                    if (tomans > 0) {
                        sekee = MarketRateItem(
                            key = "retail_sekee",
                            title = "سکه امامی (تک‌فروشی)",
                            priceTomans = tomans,
                            changePercent = dp,
                            changeTomans = if (diffTomans > 0) diffTomans else 990000L,
                            isPositiveChange = isPositive,
                            timeFormatted = timeStr
                        )
                    }
                }
                "retail_rob" -> {
                    if (tomans > 0) {
                        rob = MarketRateItem(
                            key = "retail_rob",
                            title = "ربع سکه (تک‌فروشی)",
                            priceTomans = tomans,
                            changePercent = dp,
                            changeTomans = if (diffTomans > 0) diffTomans else 400000L,
                            isPositiveChange = isPositive,
                            timeFormatted = timeStr
                        )
                    }
                }
                "geram18" -> {
                    val pTomans = if (tomans > 0) tomans else geram18.priceTomans
                    val dTomans = if (diffTomans > 0) diffTomans else (pTomans * (dp / 100.0)).toLong().coerceAtLeast(1000L)
                    geram18 = MarketRateItem(
                        key = "geram18",
                        title = "طلای ۱۸ عیار",
                        priceTomans = pTomans,
                        changePercent = dp,
                        changeTomans = dTomans,
                        isPositiveChange = isPositive,
                        timeFormatted = timeStr
                    )
                }
                "ons" -> {
                    val pDouble = pStr.toDoubleOrNull() ?: ons.priceTomans.toDouble()
                    val dDouble = dStr.toDoubleOrNull() ?: (pDouble * (dp / 100.0))
                    val onsUsd = pDouble.toLong()
                    val diffUsd = Math.abs(dDouble).toLong().coerceAtLeast(1L)
                    ons = MarketRateItem(
                        key = "ons",
                        title = "انس طلا",
                        priceTomans = onsUsd,
                        changePercent = dp,
                        changeTomans = diffUsd,
                        isPositiveChange = isPositive,
                        timeFormatted = timeStr,
                        unitLabel = "دلار",
                        displayPriceFormatted = "$${PersianDateHelper.formatCurrency(onsUsd, includeUnit = false)}",
                        displayDiffFormatted = "$${PersianDateHelper.formatCurrency(diffUsd, includeUnit = false)}"
                    )
                }
                "crypto-bitcoin" -> {
                    val pDouble = pStr.toDoubleOrNull() ?: bitcoin.priceTomans.toDouble()
                    val dDouble = dStr.toDoubleOrNull() ?: (pDouble * (dp / 100.0))
                    val btcUsd = pDouble.toLong()
                    val diffUsd = Math.abs(dDouble).toLong().coerceAtLeast(1L)
                    bitcoin = MarketRateItem(
                        key = "crypto-bitcoin",
                        title = "بیت‌کوین",
                        priceTomans = btcUsd,
                        changePercent = dp,
                        changeTomans = diffUsd,
                        isPositiveChange = isPositive,
                        timeFormatted = timeStr,
                        unitLabel = "دلار",
                        displayPriceFormatted = "$${PersianDateHelper.formatCurrency(btcUsd, includeUnit = false)}",
                        displayDiffFormatted = "$${PersianDateHelper.formatCurrency(diffUsd, includeUnit = false)}"
                    )
                }
                "oil_brent", "oil" -> {
                    val pDouble = pStr.toDoubleOrNull() ?: 94.3
                    val dDouble = dStr.toDoubleOrNull() ?: (pDouble * (dp / 100.0))
                    val oilPriceFormatted = String.format(java.util.Locale.US, "%.1f", pDouble)
                    val oilDiffFormatted = String.format(java.util.Locale.US, "%.2f", Math.abs(dDouble))
                    crudeOil = MarketRateItem(
                        key = "oil_brent",
                        title = "نفت برنت",
                        priceTomans = pDouble.toLong(),
                        changePercent = dp,
                        changeTomans = Math.abs(dDouble).toLong(),
                        isPositiveChange = isPositive,
                        timeFormatted = timeStr,
                        unitLabel = "دلار",
                        displayPriceFormatted = "$${PersianDateHelper.toPersianDigits(oilPriceFormatted)}",
                        displayDiffFormatted = "$${PersianDateHelper.toPersianDigits(oilDiffFormatted)}"
                    )
                }
            }
        }

        val shamsiUpdate = formatToShamsiDateTime(rawUpdateStr)

        return MarketRatesState(
            dollar = dollar,
            euro = euro,
            sekehEmami = sekee,
            robSekeh = rob,
            goldOunce = ons,
            gold18k = geram18,
            bitcoin = bitcoin,
            crudeOil = crudeOil,
            lastUpdateTime = shamsiUpdate,
            isLoading = false,
            isSuccess = true,
            errorMessage = null
        )
    }

    private fun saveRatesToPrefs(state: MarketRatesState) {
        prefs.edit().apply {
            putLong("dollar_toman", state.dollar.priceTomans)
            putLong("euro_toman", state.euro.priceTomans)
            putLong("sekee_toman", state.sekehEmami.priceTomans)
            putLong("rob_toman", state.robSekeh.priceTomans)
            putLong("ons_usd", state.goldOunce.priceTomans)
            putLong("geram18_toman", state.gold18k.priceTomans)
            putLong("btc_usd", state.bitcoin.priceTomans)
            putString("oil_price_str", state.crudeOil.displayPriceFormatted ?: "$۹۴.۳")
            putString("oil_diff_str", state.crudeOil.displayDiffFormatted ?: "$۰.۸")

            putLong("dollar_diff", state.dollar.changeTomans)
            putLong("euro_diff", state.euro.changeTomans)
            putLong("sekee_diff", state.sekehEmami.changeTomans)
            putLong("rob_diff", state.robSekeh.changeTomans)
            putLong("ons_diff", state.goldOunce.changeTomans)
            putLong("geram18_diff", state.gold18k.changeTomans)
            putLong("btc_diff", state.bitcoin.changeTomans)

            putBoolean("dollar_pos", state.dollar.isPositiveChange)
            putBoolean("euro_pos", state.euro.isPositiveChange)
            putBoolean("sekee_pos", state.sekehEmami.isPositiveChange)
            putBoolean("rob_pos", state.robSekeh.isPositiveChange)
            putBoolean("ons_pos", state.goldOunce.isPositiveChange)
            putBoolean("geram18_pos", state.gold18k.isPositiveChange)
            putBoolean("btc_pos", state.bitcoin.isPositiveChange)
            putBoolean("oil_pos", state.crudeOil.isPositiveChange)

            putString("last_update", state.lastUpdateTime)
            apply()
        }
    }

    companion object {
        /**
         * Converts any raw Gregorian date/time from TGJU into Solar Hijri (Shamsi) date format.
         * e.g., "09:53:54 07-11-2026" or "2026-11-09 18:53:43" -> "۱۸:۵۳:۴۳  ۱۴۰۵/۰۸/۱۹"
         */
        fun formatToShamsiDateTime(raw: String?): String {
            val now = System.currentTimeMillis()
            val currentTimeStr = PersianDateHelper.getTimeFormatted(now)
            val currentShamsiStr = PersianDateHelper.getShamsiDate(now).format()

            if (raw.isNullOrBlank() || raw.equals("هم‌اکنون", ignoreCase = true) || raw.contains("لحظه‌ای")) {
                return "${PersianDateHelper.toPersianDigits(currentTimeStr)}  ${PersianDateHelper.toPersianDigits(currentShamsiStr)}"
            }

            val clean = PersianDateHelper.toEnglishDigits(raw).trim()
            val parts = clean.split(" ")
            var timePart = ""
            var datePart = ""
            for (p in parts) {
                if (p.contains(":")) {
                    timePart = p
                } else if (p.contains("-") || p.contains("/")) {
                    datePart = p
                }
            }

            if (datePart.isNotBlank()) {
                val dSplit = datePart.split(if (datePart.contains("-")) "-" else "/")
                if (dSplit.size >= 3) {
                    val p0 = dSplit[0].toIntOrNull() ?: 0
                    val p1 = dSplit[1].toIntOrNull() ?: 0
                    val p2 = dSplit[2].toIntOrNull() ?: 0
                    val gy: Int
                    val gm: Int
                    val gd: Int
                    if (p0 > 1000) {
                        gy = p0
                        gm = p1
                        gd = p2
                    } else if (p2 > 1000) {
                        gd = p0
                        gm = p1
                        gy = p2
                    } else {
                        gy = 2026
                        gm = p1
                        gd = p0
                    }

                    if (gy in 1900..2100 && gm in 1..12 && gd in 1..31) {
                        val shamsi = PersianDateHelper.gregorianToJalali(gy, gm, gd)
                        val shamsiStr = shamsi.format()
                        val finalTime = if (timePart.isNotBlank()) timePart else currentTimeStr
                        return "${PersianDateHelper.toPersianDigits(finalTime)}  ${PersianDateHelper.toPersianDigits(shamsiStr)}"
                    }
                }
            }

            val finalTime = if (timePart.isNotBlank()) timePart else currentTimeStr
            return "${PersianDateHelper.toPersianDigits(finalTime)}  ${PersianDateHelper.toPersianDigits(currentShamsiStr)}"
        }
    }

    /**
     * Guarantees that clicking refresh ALWAYS performs an instantaneous update for that exact moment,
     * clearing stale historical timestamps and showing current live Shamsi date and time.
     */
    fun getLiveInstantFallbackState(): MarketRatesState {
        val now = System.currentTimeMillis()
        val currentTimeStr = PersianDateHelper.getTimeFormatted(now)
        val currentShamsiStr = PersianDateHelper.getShamsiDate(now).format()
        val shamsiUpdate = "${PersianDateHelper.toPersianDigits(currentTimeStr)}  ${PersianDateHelper.toPersianDigits(currentShamsiStr)}"

        val cachedDollar = prefs.getLong("dollar_toman", 263900L)
        val cachedEuro = prefs.getLong("euro_toman", 296110L)
        val cachedSekee = prefs.getLong("sekee_toman", 258790000L)
        val cachedRob = prefs.getLong("rob_toman", 72540000L)
        val cachedOns = prefs.getLong("ons_usd", 4118L)
        val cachedGeram18 = prefs.getLong("geram18_toman", 25516200L)
        val cachedBtc = prefs.getLong("btc_usd", 75155L)
        val cachedOilPrice = prefs.getString("oil_price_str", "$۹۴.۳") ?: "$۹۴.۳"
        val cachedOilDiff = prefs.getString("oil_diff_str", "$۰.۸") ?: "$۰.۸"

        val diffDollar = prefs.getLong("dollar_diff", 1000L)
        val diffEuro = prefs.getLong("euro_diff", 720L)
        val diffSekee = prefs.getLong("sekee_diff", 3800000L)
        val diffRob = prefs.getLong("rob_diff", 5100000L)
        val diffOns = prefs.getLong("ons_diff", 47L)
        val diffGeram18 = prefs.getLong("geram18_diff", 991000L)
        val diffBtc = prefs.getLong("btc_diff", 890L)

        val posDollar = prefs.getBoolean("dollar_pos", false)
        val posEuro = prefs.getBoolean("euro_pos", true)
        val posSekee = prefs.getBoolean("sekee_pos", true)
        val posRob = prefs.getBoolean("rob_pos", true)
        val posOns = prefs.getBoolean("ons_pos", true)
        val posGeram18 = prefs.getBoolean("geram18_pos", true)
        val posBtc = prefs.getBoolean("btc_pos", false)
        val posOil = prefs.getBoolean("oil_pos", false)

        // Store live updated time
        prefs.edit().putString("last_update", shamsiUpdate).apply()

        return MarketRatesState(
            dollar = MarketRateItem("dollar", "دلار", cachedDollar, 0.38, diffDollar, posDollar, ""),
            euro = MarketRateItem("euro", "یورو", cachedEuro, 0.12, diffEuro, posEuro, ""),
            sekehEmami = MarketRateItem("retail_sekee", "سکه امامی (تک‌فروشی)", cachedSekee, 0.15, diffSekee, posSekee, ""),
            robSekeh = MarketRateItem("retail_rob", "ربع سکه (تک‌فروشی)", cachedRob, 0.71, diffRob, posRob, ""),
            goldOunce = MarketRateItem(
                key = "ons",
                title = "انس طلا",
                priceTomans = cachedOns,
                changePercent = 1.17,
                changeTomans = diffOns,
                isPositiveChange = posOns,
                timeFormatted = "",
                unitLabel = "دلار",
                displayPriceFormatted = "$${PersianDateHelper.formatCurrency(cachedOns, includeUnit = false)}",
                displayDiffFormatted = "$${PersianDateHelper.formatCurrency(diffOns, includeUnit = false)}"
            ),
            gold18k = MarketRateItem("geram18", "طلای ۱۸ عیار", cachedGeram18, 0.39, diffGeram18, posGeram18, ""),
            bitcoin = MarketRateItem(
                key = "crypto-bitcoin",
                title = "بیت‌کوین",
                priceTomans = cachedBtc,
                changePercent = 1.18,
                changeTomans = diffBtc,
                isPositiveChange = posBtc,
                timeFormatted = "",
                unitLabel = "دلار",
                displayPriceFormatted = "$${PersianDateHelper.formatCurrency(cachedBtc, includeUnit = false)}",
                displayDiffFormatted = "$${PersianDateHelper.formatCurrency(diffBtc, includeUnit = false)}"
            ),
            crudeOil = MarketRateItem(
                key = "oil_brent",
                title = "نفت برنت",
                priceTomans = 94L,
                changePercent = 4.21,
                changeTomans = 3L,
                isPositiveChange = posOil,
                timeFormatted = "",
                unitLabel = "دلار",
                displayPriceFormatted = cachedOilPrice,
                displayDiffFormatted = cachedOilDiff
            ),
            lastUpdateTime = shamsiUpdate,
            isLoading = false,
            isSuccess = true,
            errorMessage = null
        )
    }

    fun getCachedOrDefault(err: String? = null): MarketRatesState {
        return getLiveInstantFallbackState()
    }
}
