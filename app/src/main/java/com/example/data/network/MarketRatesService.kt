package com.example.data.network

import android.content.Context
import com.example.data.model.MarketRateItem
import com.example.data.model.MarketRatesState
import com.example.utils.PersianDateHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.abs

class MarketRatesService(private val context: Context) {

    private val prefs = context.getSharedPreferences("market_rates_cache", Context.MODE_PRIVATE)

    /**
     * Fetches ONLY Bourse (شاخص کل بورس) and Brent Oil (نفت برنت) from https://www.tgju.org/coin
     * and preserves Bale-extracted prices for Dollar, Euro, Gold, Coins, Tether, and Bitcoin.
     */
    suspend fun fetchRates(): MarketRatesState = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        var bourseItem: MarketRateItem? = null
        var oilItem: MarketRateItem? = null
        var rawUpdateStr = ""

        // 1. اولویت اول: دریافت لحظه‌ای شاخص بورس و نفت برنت از فید تیکر زنده صفحه https://www.tgju.org/coin
        var wtiFallbackObj: JSONObject? = null
        try {
            val jsonText = downloadText(
                "https://call.tgju.org/ajax.json?_t=$now",
                "https://www.tgju.org/coin"
            )
            if (!jsonText.isNullOrBlank()) {
                val currentObj = JSONObject(jsonText).optJSONObject("current")
                if (currentObj != null) {
                    val bourseObj = currentObj.optJSONObject("bourse")
                        ?: currentObj.optJSONObject("bourse_total")
                        ?: currentObj.optJSONObject("tse")
                    if (bourseObj != null) {
                        bourseItem = parseBourseItem(bourseObj)
                        rawUpdateStr = bourseObj.optString("ts", rawUpdateStr)
                    }

                    val brentObj = currentObj.optJSONObject("oil_brent")
                    if (brentObj != null) {
                        oilItem = parseOilItem(brentObj)
                        if (rawUpdateStr.isBlank()) {
                            rawUpdateStr = brentObj.optString("ts", "")
                        }
                    } else {
                        wtiFallbackObj = currentObj.optJSONObject("oil")
                    }
                }
            }
        } catch (_: Exception) {
        }

        // 2. اولویت دوم: ویجت زنده TGJU برای شاخص بورس و نفت برنت
        if (bourseItem == null || oilItem == null) {
            try {
                val widgetJson = downloadText(
                    "https://api.tgju.org/v1/widget/tmp?keys=oil_brent,bourse&_t=$now",
                    "https://www.tgju.org/coin"
                )
                if (!widgetJson.isNullOrBlank()) {
                    val indicators = JSONObject(widgetJson)
                        .optJSONObject("response")
                        ?.optJSONArray("indicators")
                    if (indicators != null) {
                        for (i in 0 until indicators.length()) {
                            val item = indicators.optJSONObject(i) ?: continue
                            when (item.optString("name")) {
                                "bourse", "bourse_total", "tse" -> {
                                    if (bourseItem == null) {
                                        bourseItem = parseBourseFromWidgetItem(item)
                                        if (rawUpdateStr.isBlank()) {
                                            rawUpdateStr = item.optString("updated_at", item.optString("t", ""))
                                        }
                                    }
                                }
                                "oil_brent" -> {
                                    if (oilItem == null) {
                                        oilItem = parseOilFromWidgetItem(item)
                                        if (rawUpdateStr.isBlank()) {
                                            rawUpdateStr = item.optString("updated_at", item.optString("t", ""))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }

        // 3. اولویت سوم: استخراج مستقیم از HTML صفحه https://www.tgju.org/coin در صورت در دسترس نبودن تیکر زنده
        if (bourseItem == null || oilItem == null) {
            try {
                val html = downloadText("https://www.tgju.org/coin?_t=$now", "https://www.tgju.org/coin")
                if (!html.isNullOrBlank()) {
                    val parsed = parseBourseAndOilFromTgjuCoinHtml(html)
                    if (bourseItem == null) bourseItem = parsed.first
                    if (oilItem == null) oilItem = parsed.second
                }
            } catch (_: Exception) {
            }
        }

        // 4. fallback آخر برای نفت اگر فقط oil عمومی موجود بود
        if (oilItem == null && wtiFallbackObj != null) {
            oilItem = parseOilItem(wtiFallbackObj)
        }

        saveTgjuBourseAndOil(bourseItem, oilItem, rawUpdateStr)
        buildCurrentState(
            baleNeedsLogin = !hasAnyCachedBaleRate()
        )
    }

    fun saveBaleRatesResult(result: BaleExtractedRates): MarketRatesState {
        val nowShamsi = formatToShamsiDateTime("")
        prefs.edit().apply {
            result.dollar?.let {
                putLong("bale_v4_dollar_toman", it.amount)
                putLong("bale_v4_dollar_diff", it.changeAmount)
                putFloat("bale_v4_dollar_pct", it.changePercent.toFloat())
                putBoolean("bale_v4_dollar_pos", it.isPositive)
            }
            result.euro?.let {
                putLong("bale_v4_euro_toman", it.amount)
                putLong("bale_v4_euro_diff", it.changeAmount)
                putFloat("bale_v4_euro_pct", it.changePercent.toFloat())
                putBoolean("bale_v4_euro_pos", it.isPositive)
            }
            result.goldOunceUsd?.let {
                putLong("bale_v4_ons_usd", it.amount)
                putFloat("bale_v4_ons_double", it.doubleAmount.toFloat())
                putLong("bale_v4_ons_diff", it.changeAmount)
                putFloat("bale_v4_ons_pct", it.changePercent.toFloat())
                putBoolean("bale_v4_ons_pos", it.isPositive)
            }
            result.goldKaratToman?.let {
                putLong("bale_v4_gold_karat_toman", it.amount)
                putString("bale_v4_gold_karat_title", result.goldKaratTitle)
                putLong("bale_v4_gold_karat_diff", it.changeAmount)
                putFloat("bale_v4_gold_karat_pct", it.changePercent.toFloat())
                putBoolean("bale_v4_gold_karat_pos", it.isPositive)
            }
            result.sekehEmami?.let {
                putLong("bale_v4_sekee_toman", it.amount)
                putLong("bale_v4_sekee_diff", it.changeAmount)
                putFloat("bale_v4_sekee_pct", it.changePercent.toFloat())
                putBoolean("bale_v4_sekee_pos", it.isPositive)
            }
            result.robSekeh?.let {
                putLong("bale_v4_rob_toman", it.amount)
                putLong("bale_v4_rob_diff", it.changeAmount)
                putFloat("bale_v4_rob_pct", it.changePercent.toFloat())
                putBoolean("bale_v4_rob_pos", it.isPositive)
            }
            result.tether?.let {
                putLong("bale_v4_tether_toman", it.amount)
                putLong("bale_v4_tether_diff", it.changeAmount)
                putFloat("bale_v4_tether_pct", it.changePercent.toFloat())
                putBoolean("bale_v4_tether_pos", it.isPositive)
            }
            result.bitcoinUsd?.let {
                putLong("bale_v4_btc_usd", it.amount)
                putLong("bale_v4_btc_diff", it.changeAmount)
                putFloat("bale_v4_btc_pct", it.changePercent.toFloat())
                putBoolean("bale_v4_btc_pos", it.isPositive)
            }
            putString("bale_v4_last_sync", nowShamsi)
            putString("last_update", nowShamsi)
            apply()
        }

        return buildCurrentState(
            baleNeedsLogin = false,
            baleStatusMessage = if (result.hasAnyRate()) {
                "نرخ‌ها با موفقیت از کانال‌های بله دریافت شد"
            } else {
                "قالب پیام در کانال بله یافت نشد"
            }
        )
    }

    fun getCachedOrDefault(err: String? = null): MarketRatesState {
        return buildCurrentState(
            baleNeedsLogin = !hasAnyCachedBaleRate(),
            errorMessage = err
        )
    }

    fun hasAnyCachedBaleRate(): Boolean {
        return prefs.getLong("bale_v4_dollar_toman", 0L) > 0L ||
            prefs.getLong("bale_v4_euro_toman", 0L) > 0L ||
            prefs.getLong("bale_v4_sekee_toman", 0L) > 0L ||
            prefs.getLong("bale_v4_gold_karat_toman", 0L) > 0L ||
            prefs.getLong("bale_v4_tether_toman", 0L) > 0L ||
            prefs.getLong("bale_v4_btc_usd", 0L) > 0L
    }

    private fun saveTgjuBourseAndOil(
        bourseItem: MarketRateItem?,
        oilItem: MarketRateItem?,
        rawUpdateStr: String
    ) {
        val shamsiUpdate = formatToShamsiDateTime(rawUpdateStr)
        prefs.edit().apply {
            bourseItem?.let {
                putLong("tgju_v3_bourse_pts", it.priceTomans)
                putLong("tgju_v3_bourse_diff", it.changeTomans)
                putFloat("tgju_v3_bourse_pct", it.changePercent.toFloat())
                putBoolean("tgju_v3_bourse_pos", it.isPositiveChange)
                putString("tgju_v3_bourse_price_str", it.displayPriceFormatted ?: "")
                putString("tgju_v3_bourse_diff_str", it.displayDiffFormatted ?: "")
            }
            oilItem?.let {
                putLong("tgju_v3_oil_usd", it.priceTomans)
                putLong("tgju_v3_oil_diff", it.changeTomans)
                putFloat("tgju_v3_oil_pct", it.changePercent.toFloat())
                putBoolean("tgju_v3_oil_pos", it.isPositiveChange)
                putString("tgju_v3_oil_price_str", it.displayPriceFormatted ?: "")
                putString("tgju_v3_oil_diff_str", it.displayDiffFormatted ?: "")
            }
            putString("last_update", shamsiUpdate)
            apply()
        }
    }

    private fun buildCurrentState(
        baleNeedsLogin: Boolean,
        baleStatusMessage: String? = null,
        errorMessage: String? = null
    ): MarketRatesState {
        val dollarPrice = prefs.getLong("bale_v4_dollar_toman", 0L)
        val euroPrice = prefs.getLong("bale_v4_euro_toman", 0L)
        val sekeePrice = prefs.getLong("bale_v4_sekee_toman", 0L)
        val robPrice = prefs.getLong("bale_v4_rob_toman", 0L)
        val onsUsd = prefs.getLong("bale_v4_ons_usd", 0L)
        val onsDouble = prefs.getFloat("bale_v4_ons_double", 0f).toDouble()
        val goldKaratPrice = prefs.getLong("bale_v4_gold_karat_toman", 0L)
        val goldKaratTitle = prefs.getString("bale_v4_gold_karat_title", "طلای 16 عیار") ?: "طلای 16 عیار"
        val tetherPrice = prefs.getLong("bale_v4_tether_toman", 0L)
        val btcUsd = prefs.getLong("bale_v4_btc_usd", 0L)

        val onsFormatted = when {
            onsUsd <= 0L -> null
            onsDouble > 0 && abs(onsDouble - onsUsd.toDouble()) >= 0.05 ->
                "$" + String.format(Locale.US, "%,.2f", onsDouble)
            else ->
                "$" + PersianDateHelper.formatCurrency(onsUsd, includeUnit = false)
        }

        val btcFormatted = if (btcUsd > 0L) {
            "$" + PersianDateHelper.formatCurrency(btcUsd, includeUnit = false)
        } else null

        val boursePts = prefs.getLong("tgju_v3_bourse_pts", 0L)
        val bourseDiff = prefs.getLong("tgju_v3_bourse_diff", 0L)
        val boursePos = prefs.getBoolean("tgju_v3_bourse_pos", true)
        val boursePct = prefs.getFloat("tgju_v3_bourse_pct", 0f).toDouble()
        val boursePriceStr = prefs.getString("tgju_v3_bourse_price_str", "")?.takeIf { it.isNotBlank() }
        val bourseDiffStr = prefs.getString("tgju_v3_bourse_diff_str", "")?.takeIf { it.isNotBlank() }

        val oilUsd = prefs.getLong("tgju_v3_oil_usd", 0L)
        val oilDiff = prefs.getLong("tgju_v3_oil_diff", 0L)
        val oilPos = prefs.getBoolean("tgju_v3_oil_pos", true)
        val oilPct = prefs.getFloat("tgju_v3_oil_pct", 0f).toDouble()
        val oilPriceStr = prefs.getString("tgju_v3_oil_price_str", "")?.takeIf { it.isNotBlank() }
        val oilDiffStr = prefs.getString("tgju_v3_oil_diff_str", "")?.takeIf { it.isNotBlank() }

        val lastUpdate = prefs.getString("bale_v4_last_sync", "")?.takeIf { it.isNotBlank() }
            ?: prefs.getString("last_update", "")
            ?: formatToShamsiDateTime("")

        return MarketRatesState(
            dollar = MarketRateItem(
                key = "dollar",
                title = "دلار",
                priceTomans = dollarPrice,
                changePercent = prefs.getFloat("bale_v4_dollar_pct", 0f).toDouble(),
                changeTomans = prefs.getLong("bale_v4_dollar_diff", 0L),
                isPositiveChange = prefs.getBoolean("bale_v4_dollar_pos", true),
                timeFormatted = "",
                sourceLabel = "بله (4827170321)"
            ),
            euro = MarketRateItem(
                key = "euro",
                title = "یورو",
                priceTomans = euroPrice,
                changePercent = prefs.getFloat("bale_v4_euro_pct", 0f).toDouble(),
                changeTomans = prefs.getLong("bale_v4_euro_diff", 0L),
                isPositiveChange = prefs.getBoolean("bale_v4_euro_pos", true),
                timeFormatted = "",
                sourceLabel = "بله (4827170321)"
            ),
            sekehEmami = MarketRateItem(
                key = "retail_sekee",
                title = "سکه امامی",
                priceTomans = sekeePrice,
                changePercent = prefs.getFloat("bale_v4_sekee_pct", 0f).toDouble(),
                changeTomans = prefs.getLong("bale_v4_sekee_diff", 0L),
                isPositiveChange = prefs.getBoolean("bale_v4_sekee_pos", true),
                timeFormatted = "",
                sourceLabel = "بله (5160361766)"
            ),
            robSekeh = MarketRateItem(
                key = "retail_rob",
                title = "ربع سکه",
                priceTomans = robPrice,
                changePercent = prefs.getFloat("bale_v4_rob_pct", 0f).toDouble(),
                changeTomans = prefs.getLong("bale_v4_rob_diff", 0L),
                isPositiveChange = prefs.getBoolean("bale_v4_rob_pos", true),
                timeFormatted = "",
                sourceLabel = "بله (5160361766)"
            ),
            goldOunce = MarketRateItem(
                key = "ons",
                title = "انس جهانی طلا",
                priceTomans = onsUsd,
                changePercent = prefs.getFloat("bale_v4_ons_pct", 0f).toDouble(),
                changeTomans = prefs.getLong("bale_v4_ons_diff", 0L),
                isPositiveChange = prefs.getBoolean("bale_v4_ons_pos", true),
                timeFormatted = "",
                unitLabel = "دلار",
                displayPriceFormatted = onsFormatted,
                displayDiffFormatted = null,
                sourceLabel = "بله (5160361766)"
            ),
            gold18k = MarketRateItem(
                key = "geram16",
                title = goldKaratTitle,
                priceTomans = goldKaratPrice,
                changePercent = prefs.getFloat("bale_v4_gold_karat_pct", 0f).toDouble(),
                changeTomans = prefs.getLong("bale_v4_gold_karat_diff", 0L),
                isPositiveChange = prefs.getBoolean("bale_v4_gold_karat_pos", true),
                timeFormatted = "",
                sourceLabel = "بله (5160361766)"
            ),
            bitcoin = MarketRateItem(
                key = "crypto-bitcoin",
                title = "بیت‌کوین",
                priceTomans = btcUsd,
                changePercent = prefs.getFloat("bale_v4_btc_pct", 0f).toDouble(),
                changeTomans = prefs.getLong("bale_v4_btc_diff", 0L),
                isPositiveChange = prefs.getBoolean("bale_v4_btc_pos", true),
                timeFormatted = "",
                unitLabel = "دلار",
                displayPriceFormatted = btcFormatted,
                displayDiffFormatted = null,
                sourceLabel = "بله (5138319664)"
            ),
            tether = MarketRateItem(
                key = "crypto-tether",
                title = "تتر (USDT)",
                priceTomans = tetherPrice,
                changePercent = prefs.getFloat("bale_v4_tether_pct", 0f).toDouble(),
                changeTomans = prefs.getLong("bale_v4_tether_diff", 0L),
                isPositiveChange = prefs.getBoolean("bale_v4_tether_pos", true),
                timeFormatted = "",
                unitLabel = "تومان",
                sourceLabel = "بله (5138319664)"
            ),
            bourse = MarketRateItem(
                key = "bourse",
                title = "شاخص کل بورس",
                priceTomans = boursePts,
                changePercent = boursePct,
                changeTomans = bourseDiff,
                isPositiveChange = boursePos,
                timeFormatted = "",
                unitLabel = "واحد",
                displayPriceFormatted = boursePriceStr,
                displayDiffFormatted = bourseDiffStr,
                sourceLabel = "TGJU (tgju.org/coin)"
            ),
            crudeOil = MarketRateItem(
                key = "oil_brent",
                title = "نفت برنت",
                priceTomans = oilUsd,
                changePercent = oilPct,
                changeTomans = oilDiff,
                isPositiveChange = oilPos,
                timeFormatted = "",
                unitLabel = "دلار",
                displayPriceFormatted = oilPriceStr,
                displayDiffFormatted = oilDiffStr,
                sourceLabel = "TGJU (tgju.org/coin)"
            ),
            lastUpdateTime = lastUpdate,
            isLoading = false,
            isSuccess = errorMessage == null,
            errorMessage = errorMessage,
            baleNeedsLogin = baleNeedsLogin,
            baleStatusMessage = baleStatusMessage
        )
    }

    private fun downloadText(urlString: String, referer: String): String? {
        val url = URL(urlString)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12000
            readTimeout = 12000
            useCaches = false
            setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
            setRequestProperty("Pragma", "no-cache")
            setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            )
            setRequestProperty("Referer", referer)
            setRequestProperty("Accept", "text/html,application/json,text/plain,*/*")
        }
        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            return null
        }
        return connection.inputStream.bufferedReader().use { it.readText() }
    }

    private fun parseBourseItem(obj: JSONObject): MarketRateItem? {
        val pStr = obj.optString("p", "").replace(",", "").trim()
        val ptsDouble = pStr.toDoubleOrNull() ?: return null
        val pts = ptsDouble.toLong()
        if (pts <= 0L) return null

        val dStr = obj.optString("d", "").replace(",", "").trim()
        val diffDouble = dStr.toDoubleOrNull() ?: 0.0
        val diffPts = abs(diffDouble).toLong()
        val dp = obj.optDouble("dp", 0.0)
        val dt = obj.optString("dt", "high")
        val isPositive = dt.equals("high", ignoreCase = true)

        return MarketRateItem(
            key = "bourse",
            title = "شاخص کل بورس",
            priceTomans = pts,
            changePercent = dp,
            changeTomans = diffPts,
            isPositiveChange = isPositive,
            timeFormatted = obj.optString("t", ""),
            unitLabel = "واحد",
            displayPriceFormatted = "${PersianDateHelper.formatCurrency(pts, includeUnit = false)}\u00A0واحد",
            displayDiffFormatted = if (diffPts > 0L) "${PersianDateHelper.formatCurrency(diffPts, includeUnit = false)}${if (isPositive) "+" else "-"}" else null,
            sourceLabel = "TGJU (tgju.org/coin)"
        )
    }

    private fun parseOilItem(obj: JSONObject): MarketRateItem? {
        val pStr = obj.optString("p", "").replace(",", "").trim()
        val pDouble = pStr.toDoubleOrNull() ?: return null
        if (pDouble <= 0.0) return null

        val dStr = obj.optString("d", "").replace(",", "").trim()
        val dp = obj.optDouble("dp", 0.0)
        val dt = obj.optString("dt", "high")
        val isPositive = dt.equals("high", ignoreCase = true)
        val dDouble = dStr.toDoubleOrNull() ?: (pDouble * (dp / 100.0))

        val oilPriceFormatted = String.format(Locale.US, "%.2f", pDouble)
        val oilDiffFormatted = if (abs(dDouble) >= 0.005) "$${String.format(Locale.US, "%.2f", abs(dDouble))}" else null

        return MarketRateItem(
            key = "oil_brent",
            title = "نفت برنت",
            priceTomans = pDouble.toLong(),
            changePercent = dp,
            changeTomans = abs(dDouble).toLong(),
            isPositiveChange = isPositive,
            timeFormatted = obj.optString("t", ""),
            unitLabel = "دلار",
            displayPriceFormatted = "$$oilPriceFormatted",
            displayDiffFormatted = oilDiffFormatted,
            sourceLabel = "TGJU (tgju.org/coin)"
        )
    }

    private fun parseBourseFromWidgetItem(item: JSONObject): MarketRateItem? {
        val pStr = item.optString("p", "").replace(",", "").trim()
        val pts = pStr.toDoubleOrNull()?.toLong() ?: return null
        if (pts <= 0L) return null

        val dStr = item.optString("d", "").replace(",", "").trim()
        val diffPts = abs(dStr.toDoubleOrNull() ?: 0.0).toLong()
        val dp = item.optDouble("dp", 0.0)
        val isPositive = item.optString("dt", "low").equals("high", ignoreCase = true)

        return MarketRateItem(
            key = "bourse",
            title = "شاخص کل بورس",
            priceTomans = pts,
            changePercent = dp,
            changeTomans = diffPts,
            isPositiveChange = isPositive,
            timeFormatted = item.optString("t", ""),
            unitLabel = "واحد",
            displayPriceFormatted = "${PersianDateHelper.formatCurrency(pts, includeUnit = false)}\u00A0واحد",
            displayDiffFormatted = if (diffPts > 0L) "${PersianDateHelper.formatCurrency(diffPts, includeUnit = false)}${if (isPositive) "+" else "-"}" else null,
            sourceLabel = "TGJU (tgju.org/coin)"
        )
    }

    private fun parseOilFromWidgetItem(item: JSONObject): MarketRateItem? {
        val pStr = item.optString("p", "").replace(",", "").trim()
        val pDouble = pStr.toDoubleOrNull() ?: return null
        if (pDouble <= 0.0) return null

        val dStr = item.optString("d", "").replace(",", "").trim()
        val dp = item.optDouble("dp", 0.0)
        val isPositive = item.optString("dt", "low").equals("high", ignoreCase = true)
        val dDouble = dStr.toDoubleOrNull() ?: (pDouble * (dp / 100.0))

        val oilPriceFormatted = String.format(Locale.US, "%.2f", pDouble)
        val oilDiffFormatted = if (abs(dDouble) >= 0.005) "$${String.format(Locale.US, "%.2f", abs(dDouble))}" else null

        return MarketRateItem(
            key = "oil_brent",
            title = "نفت برنت",
            priceTomans = pDouble.toLong(),
            changePercent = dp,
            changeTomans = abs(dDouble).toLong(),
            isPositiveChange = isPositive,
            timeFormatted = item.optString("t", ""),
            unitLabel = "دلار",
            displayPriceFormatted = "$$oilPriceFormatted",
            displayDiffFormatted = oilDiffFormatted,
            sourceLabel = "TGJU (tgju.org/coin)"
        )
    }

    companion object {
        /**
         * Extracts ONLY شاخص بورس (bourse) and نفت برنت (oil_brent) from https://www.tgju.org/coin HTML.
         */
        fun parseBourseAndOilFromTgjuCoinHtml(html: String): Pair<MarketRateItem?, MarketRateItem?> {
            val bourse = extractRowFromTgjuHtml(html, listOf("l-bourse", "data-market-row=\"bourse\"", "شاخص بورس"))?.let { (priceDouble, changeDouble, isPos) ->
                val pts = priceDouble.toLong()
                if (pts > 10_000L) {
                    val diffPts = abs(changeDouble).toLong()
                    MarketRateItem(
                        key = "bourse",
                        title = "شاخص کل بورس",
                        priceTomans = pts,
                        changePercent = 0.0,
                        changeTomans = diffPts,
                        isPositiveChange = isPos,
                        timeFormatted = "",
                        unitLabel = "واحد",
                        displayPriceFormatted = "${PersianDateHelper.formatCurrency(pts, includeUnit = false)}\u00A0واحد",
                        displayDiffFormatted = if (diffPts > 0L) "${PersianDateHelper.formatCurrency(diffPts, includeUnit = false)}${if (isPos) "+" else "-"}" else null,
                        sourceLabel = "TGJU (tgju.org/coin)"
                    )
                } else null
            }

            val oil = extractRowFromTgjuHtml(html, listOf("l-oil_brent", "data-market-row=\"oil_brent\"", "نفت برنت"))?.let { (priceDouble, changeDouble, isPos) ->
                if (priceDouble in 10.0..500.0) {
                    val oilPriceFormatted = String.format(Locale.US, "%.2f", priceDouble)
                    val oilDiffFormatted = if (abs(changeDouble) >= 0.005) "$${String.format(Locale.US, "%.2f", abs(changeDouble))}" else null
                    MarketRateItem(
                        key = "oil_brent",
                        title = "نفت برنت",
                        priceTomans = priceDouble.toLong(),
                        changePercent = 0.0,
                        changeTomans = abs(changeDouble).toLong(),
                        isPositiveChange = isPos,
                        timeFormatted = "",
                        unitLabel = "دلار",
                        displayPriceFormatted = "$$oilPriceFormatted",
                        displayDiffFormatted = oilDiffFormatted,
                        sourceLabel = "TGJU (tgju.org/coin)"
                    )
                } else null
            }

            return Pair(bourse, oil)
        }

        private fun extractRowFromTgjuHtml(
            html: String,
            markers: List<String>
        ): Triple<Double, Double, Boolean>? {
            for (marker in markers) {
                val idx = html.indexOf(marker, ignoreCase = true)
                if (idx >= 0) {
                    val liStart = html.lastIndexOf("<li", idx, ignoreCase = true)
                    val trStart = html.lastIndexOf("<tr", idx, ignoreCase = true)
                    val tagStart = maxOf(liStart, trStart).takeIf { it >= 0 && idx - it < 200 } ?: idx
                    val end = minOf(html.length, idx + 700)
                    val snippet = html.substring(tagStart, end)
                    val closeIdx = snippet.indexOf("</li>", ignoreCase = true).takeIf { it > 0 }
                        ?: snippet.indexOf("</tr>", ignoreCase = true).takeIf { it > 0 }
                        ?: snippet.length
                    val block = snippet.substring(0, closeIdx)

                    val isNegative = Regex("""class\s*=\s*["'][^"']*\blow\b[^"']*["']""", RegexOption.IGNORE_CASE)
                        .containsMatchIn(block) ||
                        Regex("""data-dt\s*=\s*["']low["']""", RegexOption.IGNORE_CASE).containsMatchIn(block)

                    // Try data-price attribute first if present
                    val dataPriceMatch = Regex("""data-price\s*=\s*["']([\d,\.]+)["']""").find(block)
                    val infoPriceMatches = Regex("""class\s*=\s*["'][^"']*\b(?:info-price|nf)\b[^"']*["'][^>]*>\s*([^<]+)<""").findAll(block)
                    val candidateStrings = buildList {
                        dataPriceMatch?.groupValues?.get(1)?.let { add(it) }
                        infoPriceMatches.forEach { add(it.groupValues[1]) }
                    }
                    for (rawPriceStr in candidateStrings) {
                        val cleaned = BaleChannelPriceParser.normalizePersianText(rawPriceStr)
                            .replace(",", "")
                            .replace("$", "")
                            .trim()
                        val priceVal = cleaned.toDoubleOrNull()
                        if (priceVal != null && priceVal > 0.0) {
                            val changeMatch = Regex("""class\s*=\s*["'][^"']*\b(?:info-change|change)\b[^"']*["'][^>]*>\s*([^<]+)<""").find(block)
                            val changeVal = changeMatch?.groupValues?.get(1)?.let {
                                val cNorm = BaleChannelPriceParser.normalizePersianText(it)
                                Regex("""([\d,\.]+)""").find(cNorm)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
                            } ?: 0.0
                            return Triple(priceVal, changeVal, !isNegative)
                        }
                    }
                }
            }
            return null
        }

        /**
         * Converts any raw Gregorian date/time into Solar Hijri (Shamsi) date format.
         */
        fun formatToShamsiDateTime(raw: String?): String {
            val now = System.currentTimeMillis()
            val currentTimeStr = PersianDateHelper.getTimeFormatted(now)
            val currentShamsiStr = PersianDateHelper.getShamsiDate(now).format()

            if (raw.isNullOrBlank() || raw.equals("هم‌اکنون", ignoreCase = true) || raw.contains("لحظه‌ای")) {
                return "$currentTimeStr  $currentShamsiStr"
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
                        return "$finalTime  $shamsiStr"
                    }
                }
            }

            val finalTime = if (timePart.isNotBlank()) timePart else currentTimeStr
            return "$finalTime  $currentShamsiStr"
        }
    }
}
