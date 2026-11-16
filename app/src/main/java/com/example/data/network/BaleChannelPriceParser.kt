package com.example.data.network

import com.example.utils.PersianDateHelper
import kotlin.math.abs
import kotlin.math.roundToLong

data class ParsedBaleValue(
    val amount: Long,
    val doubleAmount: Double = amount.toDouble(),
    val changeAmount: Long = 0L,
    val changePercent: Double = 0.0,
    val isPositive: Boolean = true
)

data class Channel4827170321Result(
    val dollar: ParsedBaleValue? = null,
    val euro: ParsedBaleValue? = null
)

data class Channel5160361766Result(
    val goldOunceUsd: ParsedBaleValue? = null,
    val goldKaratToman: ParsedBaleValue? = null,
    val goldKaratTitle: String = "طلای 16 عیار",
    val sekehEmami: ParsedBaleValue? = null,
    val robSekeh: ParsedBaleValue? = null
)

data class Channel5138319664Result(
    val tether: ParsedBaleValue? = null,
    val bitcoinUsd: ParsedBaleValue? = null
)

data class BaleExtractedRates(
    val dollar: ParsedBaleValue? = null,
    val euro: ParsedBaleValue? = null,
    val goldOunceUsd: ParsedBaleValue? = null,
    val goldKaratToman: ParsedBaleValue? = null,
    val goldKaratTitle: String = "طلای 16 عیار",
    val sekehEmami: ParsedBaleValue? = null,
    val robSekeh: ParsedBaleValue? = null,
    val tether: ParsedBaleValue? = null,
    val bitcoinUsd: ParsedBaleValue? = null,
    val extractedAtMillis: Long = System.currentTimeMillis()
) {
    fun hasAnyRate(): Boolean {
        return dollar != null || euro != null || goldOunceUsd != null ||
            goldKaratToman != null || sekehEmami != null || robSekeh != null ||
            tether != null || bitcoinUsd != null
    }
}

object BaleChannelPriceParser {

    const val URL_FX_CHANNEL = "https://web.bale.ai/chat?uid=4827170321"
    const val URL_GOLD_CHANNEL = "https://web.bale.ai/chat?uid=5160361766"
    const val URL_CRYPTO_CHANNEL = "https://web.bale.ai/chat?uid=5138319664"
    const val URL_TGJU_COIN = "https://www.tgju.org/coin"

    private val DATE_REGEX = Regex("""\b\d{4}/\d{1,2}/\d{1,2}\b""")
    private val TIME_REGEX = Regex("""\b\d{1,2}:\d{2}(:\d{2})?\b""")
    private val NUMBER_REGEX = Regex("""(\d[\d,\.]*)""")

    private val PERSIAN_MONTH_DATE_REGEX = Regex(
        """\b\d{1,2}\s+(?:فروردین|اردیبهشت|خرداد|تیر|مرداد|شهریور|مهر|آبان|آذر|دی|بهمن|اسفند)\b"""
    )

    private val ALL_ASSET_BOUNDARY_KEYWORDS = listOf(
        "یورو", "پوند", "درهم", "لیر", "یوان", "دینار", "روبل", "فرانک", "منات",
        "سکه امامی", "نیم سکه", "ربع سکه", "سکه بهار", "سکه گرمی", "سکه تمام", "طرح جدید", "طرح قدیم",
        "مثقال", "مظنه", "آبشده", "آب شده", "انس", "اونس",
        "طلای 24", "طلا 24", "24 عیار", "22 عیار", "21 عیار", "طلای 16", "طلای 18", "طلای 750", "طلای",
        "تتر", "بیت کوین", "بیتکوین", "اتریوم", "سولانا", "ریپل", "تون", "دوج", "بایننس",
        "bitcoin", "eth", "xau", "eur"
    )

    private fun findFirstKeywordRange(cleanLine: String, keywordPatterns: List<String>): IntRange? {
        for (kw in keywordPatterns) {
            if (kw.all { it in 'a'..'z' || it in '0'..'9' }) {
                val match = Regex("""(?<![a-z0-9])${Regex.escape(kw)}(?![a-z0-9])""").find(cleanLine)
                if (match != null) return match.range
            } else {
                val idx = cleanLine.indexOf(kw)
                if (idx >= 0) return idx until (idx + kw.length)
            }
        }
        return null
    }

    private fun isolateKeywordSegment(cleanLine: String, keywordPatterns: List<String>): String {
        if (keywordPatterns.isEmpty()) return cleanLine
        val range = findFirstKeywordRange(cleanLine, keywordPatterns) ?: return cleanLine
        val startIdx = range.first
        val searchFrom = range.last + 1
        var endIdx = cleanLine.length

        // Break on another asset keyword only if it isn't part of a quote pair (like BTC/USDT)
        // and isn't the unit word "دلار" right after a number (like "65,288 دلار").
        val checkKeywords = ALL_ASSET_BOUNDARY_KEYWORDS + listOf("دلار", "usd", "usdt", "btc", "طلا")
        for (otherKw in checkKeywords) {
            if (keywordPatterns.any { it.contains(otherKw) || otherKw.contains(it) }) continue
            var from = searchFrom
            while (from < endIdx) {
                val oIdx = if (otherKw.all { it in 'a'..'z' }) {
                    Regex("""\b${Regex.escape(otherKw)}\b""").find(cleanLine, startIndex = from)?.range?.first ?: -1
                } else {
                    cleanLine.indexOf(otherKw, startIndex = from)
                }
                if (oIdx !in (searchFrom + 1) until endIdx) break

                val beforeChar = cleanLine[oIdx - 1]
                val isSlashPair = (otherKw == "usdt" || otherKw == "usd" || otherKw == "btc") &&
                    (beforeChar == '/' || beforeChar == '-')
                val prefixBefore = cleanLine.substring(searchFrom, oIdx).trimEnd()
                val isDollarUnitAfterNumber = (otherKw == "دلار" || otherKw == "usd") &&
                    prefixBefore.lastOrNull()?.isDigit() == true

                if (!isSlashPair && !isDollarUnitAfterNumber) {
                    endIdx = oIdx
                    break
                }
                from = oIdx + otherKw.length
            }
        }

        return cleanLine.substring(startIdx, endIdx).trim()
    }

    private fun isolatePreKeywordSegment(cleanLine: String, keywordPatterns: List<String>): String? {
        if (keywordPatterns.isEmpty()) return null
        val range = findFirstKeywordRange(cleanLine, keywordPatterns) ?: return null
        val kwStart = range.first
        if (kwStart <= 0) return null

        val prefix = cleanLine.substring(0, kwStart)
            .trimEnd(' ', '\t', '|', '•', '؛', ':', '-', '—', '–')
        if (prefix.isBlank()) return null

        var sliceStart = 0
        val boundaryTokens = ALL_ASSET_BOUNDARY_KEYWORDS + listOf("|", "•", "؛")
        for (tok in boundaryTokens) {
            val lastIdx = prefix.lastIndexOf(tok)
            if (lastIdx >= 0) {
                val candidateStart = lastIdx + tok.length
                if (candidateStart > sliceStart && candidateStart < prefix.length) {
                    sliceStart = candidateStart
                }
            }
        }
        val slice = prefix.substring(sliceStart).trim()
        return slice.takeIf { it.isNotBlank() && NUMBER_REGEX.containsMatchIn(it) }
    }

    fun normalizePersianText(input: String): String {
        val englishDigits = PersianDateHelper.toEnglishDigits(input)
        return englishDigits
            .replace(Regex("""[\u0640\u064B-\u065F\u0670]"""), "")
            .replace('ي', 'ی')
            .replace('ك', 'ک')
            .replace('ة', 'ه')
            .replace('ۀ', 'ه')
            .replace("\u200C", " ")
            .replace("\u200E", "")
            .replace("\u200F", "")
            .replace("\u202A", "")
            .replace("\u202B", "")
            .replace("\u202C", "")
            .replace('٬', ',')
            .replace('٫', '.')
    }

    fun detectBaleLoginRequired(rawText: String, currentUrl: String?): Boolean {
        val url = (currentUrl ?: "").lowercase()
        if (url.contains("/login") || url.contains("/auth") || url.contains("sso")) {
            return true
        }
        val norm = normalizePersianText(rawText).lowercase()
        if (norm.isBlank()) return false

        val hasPriceKeyword = norm.contains("دلار") || norm.contains("یورو") ||
            norm.contains("سکه") || norm.contains("طلا") ||
            norm.contains("انس") || norm.contains("اونس") ||
            norm.contains("تتر") || norm.contains("بیت") ||
            Regex("""\busdt\b""").containsMatchIn(norm) ||
            Regex("""\bbtc\b""").containsMatchIn(norm)

        if (hasPriceKeyword && Regex("""\d[\d,\.]{2,}""").containsMatchIn(norm)) {
            return false
        }

        val loginSignals = listOf(
            "شماره موبایل",
            "شماره تلفن",
            "ورود به بله",
            "ورود یا ثبت",
            "کد تایید",
            "ارسال کد",
            "پیامک",
            "شماره همراه",
            "enter your phone",
            "sign in to bale"
        )
        return loginSignals.any { norm.contains(it) }
    }

    /**
     * Parses https://web.bale.ai/chat?uid=4827170321 -> Dollar & Euro (in Tomans)
     */
    fun parseChannel4827170321(rawText: String): Channel4827170321Result {
        val lines = splitMeaningfulLines(rawText)
        var dollar: ParsedBaleValue? = null
        var dollarFallback: ParsedBaleValue? = null
        var euro: ParsedBaleValue? = null
        var euroFallback: ParsedBaleValue? = null

        val fxStopKeywords = listOf(
            "درهم", "پوند", "لیر", "یوان", "دینار", "روبل", "فرانک", "منات",
            "سکه", "طلا", "انس", "اونس", "تتر", "بیت", "ریال عربستان", "کانادا", "استرالیا"
        )

        for (i in lines.indices.reversed()) {
            val clean = stripDatesAndTimes(lines[i].lowercase())
            if (clean.isBlank()) continue

            if (dollar == null && isDollarLine(clean)) {
                val isOfficialOrSecondary = isSecondaryRateLine(clean)
                val parsed = extractNumberFromWindow(
                    lines = lines,
                    startIndex = i,
                    maxLookAhead = 2,
                    minValid = 30_000.0,
                    maxValid = 500_000.0,
                    assumeTomanThreshold = 500_000L,
                    keywordPatterns = listOf("دلار آزاد", "دلار نقدی", "دلار آمریکا", "دلار تهران", "دلار", "usd"),
                    rejectUsdSymbol = true,
                    stopIfMatches = { next ->
                        isEuroLine(next) || fxStopKeywords.any { next.contains(it) }
                    }
                )
                if (parsed != null) {
                    if (!isOfficialOrSecondary) {
                        dollar = parsed
                    } else if (dollarFallback == null) {
                        dollarFallback = parsed
                    }
                }
            }

            if (euro == null && isEuroLine(clean)) {
                val isOfficialOrSecondary = isSecondaryRateLine(clean)
                val parsed = extractNumberFromWindow(
                    lines = lines,
                    startIndex = i,
                    maxLookAhead = 2,
                    minValid = 30_000.0,
                    maxValid = 600_000.0,
                    assumeTomanThreshold = 600_000L,
                    keywordPatterns = listOf("یورو آزاد", "یورو نقدی", "یورو", "eur"),
                    rejectUsdSymbol = true,
                    stopIfMatches = { next ->
                        isDollarLine(next) || fxStopKeywords.any { next.contains(it) }
                    }
                )
                if (parsed != null) {
                    if (!isOfficialOrSecondary) {
                        euro = parsed
                    } else if (euroFallback == null) {
                        euroFallback = parsed
                    }
                }
            }

            if (dollar != null && euro != null) break
        }

        return Channel4827170321Result(
            dollar = dollar ?: dollarFallback,
            euro = euro ?: euroFallback
        )
    }

    /**
     * Parses https://web.bale.ai/chat?uid=5160361766 ->
     * Gold Ounce (USD), Gold 16k (or 18k fallback if channel posts 18k), Sekeh Emami, Rob Sekeh
     */
    fun parseChannel5160361766(rawText: String): Channel5160361766Result {
        val lines = splitMeaningfulLines(rawText)
        var goldOunce: ParsedBaleValue? = null
        var gold16k: ParsedBaleValue? = null
        var gold18k: ParsedBaleValue? = null
        var goldGeneric: ParsedBaleValue? = null
        var sekehEmami: ParsedBaleValue? = null
        var sekehFallback: ParsedBaleValue? = null
        var robSekeh: ParsedBaleValue? = null

        val goldOtherStop = listOf(
            "نیم سکه", "سکه نیم", "ربع سکه", "سکه گرمی", "گرمی",
            "بهار آزادی", "طرح قدیم", "مثقال", "مظنه", "آبشده", "آب شده",
            "دست دوم", "نقره", "پلاتین", "24 عیار", "۲۴ عیار", "حباب"
        )

        for (i in lines.indices.reversed()) {
            val clean = stripDatesAndTimes(lines[i].lowercase())
            if (clean.isBlank()) continue

            if (goldOunce == null && isOunceLine(clean)) {
                val parsed = extractNumberFromWindow(
                    lines = lines,
                    startIndex = i,
                    maxLookAhead = 2,
                    minValid = 1_000.0,
                    maxValid = 15_000.0,
                    isUsdAsset = true,
                    keywordPatterns = listOf("انس جهانی طلا", "انس جهانی", "اونس جهانی", "انس طلا", "انس", "اونس", "xau", "ons"),
                    stopIfMatches = { next ->
                        isGold16Line(next) || isGold18Line(next) || isSekehEmamiStrictLine(next) ||
                            isRobSekehLine(next) || goldOtherStop.any { next.contains(it) }
                    }
                )
                if (parsed != null) goldOunce = parsed
            }

            if (gold16k == null && isGold16Line(clean)) {
                val parsed = extractNumberFromWindow(
                    lines = lines,
                    startIndex = i,
                    maxLookAhead = 3,
                    minValid = 1_000_000.0,
                    maxValid = 35_000_000.0,
                    assumeTomanThreshold = 35_000_000L,
                    excludeLiteralNumbers = setOf(16L),
                    keywordPatterns = listOf(
                        "طلای 16 عیار", "طلا 16 عیار", "گرم طلای 16",
                        "16 عیار", "عیار 16", "شانزده عیار", "طلای 16", "طلا 16"
                    ),
                    rejectUsdSymbol = true,
                    stopIfMatches = { next ->
                        isOunceLine(next) || isGold18Line(next) || isSekehEmamiStrictLine(next) ||
                            isRobSekehLine(next) || goldOtherStop.any { next.contains(it) }
                    }
                )
                if (parsed != null) gold16k = parsed
            }

            if (gold18k == null && isGold18Line(clean)) {
                val parsed = extractNumberFromWindow(
                    lines = lines,
                    startIndex = i,
                    maxLookAhead = 3,
                    minValid = 1_000_000.0,
                    maxValid = 35_000_000.0,
                    assumeTomanThreshold = 35_000_000L,
                    excludeLiteralNumbers = setOf(18L, 750L),
                    keywordPatterns = listOf(
                        "طلای 18 عیار", "طلا 18 عیار", "گرم طلای 18",
                        "طلای 750", "طلا 750", "750 عیار", "عیار 750",
                        "18 عیار", "عیار 18", "طلای 18", "طلا 18",
                        "طلای هجده", "طلای هیجده", "گرم 18"
                    ),
                    rejectUsdSymbol = true,
                    stopIfMatches = { next ->
                        isOunceLine(next) || isGold16Line(next) || isSekehEmamiStrictLine(next) ||
                            isRobSekehLine(next) || goldOtherStop.any { next.contains(it) }
                    }
                )
                if (parsed != null) gold18k = parsed
            } else if (gold18k == null && goldGeneric == null && isGenericGramGoldLine(clean)) {
                val parsed = extractNumberFromWindow(
                    lines = lines,
                    startIndex = i,
                    maxLookAhead = 3,
                    minValid = 1_000_000.0,
                    maxValid = 35_000_000.0,
                    assumeTomanThreshold = 35_000_000L,
                    excludeLiteralNumbers = setOf(18L, 16L, 750L),
                    keywordPatterns = listOf("هر گرم طلا", "گرم طلا", "طلای خام"),
                    rejectUsdSymbol = true,
                    stopIfMatches = { next ->
                        isOunceLine(next) || isGold16Line(next) || isSekehEmamiStrictLine(next) ||
                            isRobSekehLine(next) || goldOtherStop.any { next.contains(it) }
                    }
                )
                if (parsed != null) goldGeneric = parsed
            }

            if (robSekeh == null && isRobSekehLine(clean)) {
                val parsed = extractNumberFromWindow(
                    lines = lines,
                    startIndex = i,
                    maxLookAhead = 2,
                    minValid = 8_000_000.0,
                    maxValid = 150_000_000.0,
                    assumeTomanThreshold = 150_000_000L,
                    keywordPatterns = listOf("ربع سکه", "سکه ربع", "ربع"),
                    rejectUsdSymbol = true,
                    stopIfMatches = { next ->
                        isOunceLine(next) || isGold16Line(next) || isGold18Line(next) ||
                            isSekehEmamiStrictLine(next) || next.contains("نیم") || next.contains("گرمی") || next.contains("مثقال")
                    }
                )
                if (parsed != null) robSekeh = parsed
            }

            if (sekehEmami == null && isSekehEmamiStrictLine(clean)) {
                val parsed = extractNumberFromWindow(
                    lines = lines,
                    startIndex = i,
                    maxLookAhead = 2,
                    minValid = 20_000_000.0,
                    maxValid = 350_000_000.0,
                    assumeTomanThreshold = 350_000_000L,
                    keywordPatterns = listOf("سکه امامی", "طرح جدید", "امامی"),
                    rejectUsdSymbol = true,
                    stopIfMatches = { next ->
                        isOunceLine(next) || isGold16Line(next) || isGold18Line(next) ||
                            isRobSekehLine(next) || goldOtherStop.any { next.contains(it) }
                    }
                )
                if (parsed != null) sekehEmami = parsed
            } else if (sekehEmami == null && sekehFallback == null && isSekehTamamFallbackLine(clean)) {
                val parsed = extractNumberFromWindow(
                    lines = lines,
                    startIndex = i,
                    maxLookAhead = 2,
                    minValid = 20_000_000.0,
                    maxValid = 350_000_000.0,
                    assumeTomanThreshold = 350_000_000L,
                    keywordPatterns = listOf("سکه تمام"),
                    rejectUsdSymbol = true,
                    stopIfMatches = { next ->
                        isOunceLine(next) || isGold16Line(next) || isGold18Line(next) ||
                            isRobSekehLine(next) || goldOtherStop.any { next.contains(it) }
                    }
                )
                if (parsed != null) sekehFallback = parsed
            }

            if (goldOunce != null && (gold16k != null || gold18k != null) && sekehEmami != null && robSekeh != null) {
                break
            }
        }

        val resolvedGold = gold16k ?: gold18k ?: goldGeneric
        val resolvedTitle = when {
            gold16k != null -> "طلای 16 عیار"
            gold18k != null || goldGeneric != null -> "طلای 18 عیار"
            else -> "طلای 16 عیار"
        }

        return Channel5160361766Result(
            goldOunceUsd = goldOunce,
            goldKaratToman = resolvedGold,
            goldKaratTitle = resolvedTitle,
            sekehEmami = sekehEmami ?: sekehFallback,
            robSekeh = robSekeh
        )
    }

    /**
     * Parses https://web.bale.ai/chat?uid=5138319664 -> Tether (Tomans) & Bitcoin (USD)
     */
    fun parseChannel5138319664(rawText: String): Channel5138319664Result {
        val lines = splitMeaningfulLines(rawText)
        var tether: ParsedBaleValue? = null
        var bitcoin: ParsedBaleValue? = null

        val cryptoStopKeywords = listOf(
            "اتریوم", "ethereum", "سولانا", "solana", "ریپل", "دوج", "تون",
            "کاردانو", "بایننس", "شیبا", "ترون", "لایت کوین"
        )

        for (i in lines.indices.reversed()) {
            val clean = stripDatesAndTimes(lines[i].lowercase())
            if (clean.isBlank()) continue

            if (tether == null && isTetherLine(clean)) {
                val parsed = extractNumberFromWindow(
                    lines = lines,
                    startIndex = i,
                    maxLookAhead = 2,
                    minValid = 30_000.0,
                    maxValid = 500_000.0,
                    assumeTomanThreshold = 500_000L,
                    keywordPatterns = listOf("تتر", "usdt"),
                    rejectUsdSymbol = true,
                    stopIfMatches = { next ->
                        isBitcoinLine(next) || cryptoStopKeywords.any { next.contains(it) }
                    }
                )
                if (parsed != null) tether = parsed
            }

            if (bitcoin == null && isBitcoinLine(clean)) {
                val parsed = extractNumberFromWindow(
                    lines = lines,
                    startIndex = i,
                    maxLookAhead = 2,
                    minValid = 1_000.0,
                    maxValid = 1_000_000.0,
                    isUsdAsset = true,
                    keywordPatterns = listOf("بیت کوین", "بیتکوین", "bitcoin", "btc"),
                    rejectTomanKeywords = true,
                    stopIfMatches = { next ->
                        isTetherLine(next) || cryptoStopKeywords.any { next.contains(it) }
                    }
                )
                if (parsed != null) bitcoin = parsed
            }

            if (tether != null && bitcoin != null) break
        }

        return Channel5138319664Result(tether = tether, bitcoinUsd = bitcoin)
    }

    private fun splitMeaningfulLines(rawText: String): List<String> {
        val normalized = normalizePersianText(rawText)
        return normalized.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private fun stripDatesAndTimes(lower: String): String {
        return lower
            .replace(DATE_REGEX, " ")
            .replace(TIME_REGEX, " ")
            .replace(PERSIAN_MONTH_DATE_REGEX, " ")
            .replace(Regex("""[+-]?\s*\d+(?:\.\d+)?\s*%"""), " ")
            .trim()
    }

    private fun isSecondaryRateLine(clean: String): Boolean {
        return clean.contains("مبادله") || clean.contains("نیما") ||
            clean.contains("توافقی") || clean.contains("سنا") ||
            clean.contains("رسمی") || clean.contains("حواله") ||
            clean.contains("بانکی")
    }

    private fun isDollarLine(clean: String): Boolean {
        val hasDollar = clean.contains("دلار") || Regex("""\busd\b""").containsMatchIn(clean)
        if (!hasDollar) return false
        val excluded = listOf(
            "کانادا", "استرالیا", "سنگاپور", "نیوزیلند", "هنگ",
            "تتر", "انس", "اونس", "نفت", "بیت", "برابری", "نسبت"
        )
        return excluded.none { clean.contains(it) }
    }

    private fun isEuroLine(clean: String): Boolean {
        if (clean.contains("برابری") || clean.contains("eur/usd") || clean.contains("eurusd")) return false
        return clean.contains("یورو") || Regex("""\beur\b""").containsMatchIn(clean)
    }

    private fun isOunceLine(clean: String): Boolean {
        return clean.contains("انس") || clean.contains("اونس") ||
            Regex("""\b(?:xau|ons)\b""").containsMatchIn(clean)
    }

    private fun hasGoldExclusionInSegment(segment: String): Boolean {
        val hasWageExclusion = segment.contains("اجرت") && !segment.contains("بدون اجرت")
        return hasWageExclusion ||
            segment.contains("انس") || segment.contains("اونس") ||
            segment.contains("مثقال") || segment.contains("مظنه") ||
            segment.contains("آبشده") || segment.contains("آب شده") ||
            segment.contains("سکه") || segment.contains("نیم") ||
            segment.contains("ربع") || segment.contains("حباب") ||
            segment.contains("دست دوم") || segment.contains("کارکرده") ||
            segment.contains("شکسته") || segment.contains("شمش") ||
            segment.contains("نقره") || segment.contains("پلاتین") ||
            Regex("""(?<![\d,.])(?:24|22|21|995|999)(?![\d,.])""").containsMatchIn(segment)
    }

    private fun hasGoldPrefixExclusion(clean: String, keywordPatterns: List<String>): Boolean {
        val range = findFirstKeywordRange(clean, keywordPatterns) ?: return false
        if (range.first <= 0) return false
        val prefix = clean.substring(maxOf(0, range.first - 22), range.first)
        return prefix.contains("مثقال") || prefix.contains("مظنه") ||
            prefix.contains("آبشده") || prefix.contains("آب شده") ||
            prefix.contains("حباب") || prefix.contains("سکه") ||
            prefix.contains("نیم") || prefix.contains("ربع") ||
            prefix.contains("دست دوم") || prefix.contains("کارکرده") ||
            prefix.contains("شمش")
    }

    private fun isGold16Line(clean: String): Boolean {
        val kws = listOf(
            "طلای 16 عیار", "طلا 16 عیار", "گرم طلای 16",
            "16 عیار", "عیار 16", "شانزده عیار", "طلای 16", "طلا 16"
        )
        val hasKarat16 = Regex("""(?<![\d,.])16(?![\d,.])""").containsMatchIn(clean) || clean.contains("شانزده")
        if (!(clean.contains("طلا") || clean.contains("گرم") || clean.contains("عیار")) || !hasKarat16) {
            return false
        }
        val segment = isolateKeywordSegment(clean, kws)
        return !hasGoldPrefixExclusion(clean, kws) && !hasGoldExclusionInSegment(segment)
    }

    private fun isGold18Line(clean: String): Boolean {
        val kws = listOf(
            "طلای 18 عیار", "طلا 18 عیار", "گرم طلای 18",
            "طلای 750", "طلا 750", "750 عیار", "عیار 750",
            "18 عیار", "عیار 18", "طلای 18", "طلا 18",
            "طلای هجده", "طلای هیجده", "گرم 18"
        )
        val hasKarat18 = Regex("""(?<![\d,.])(?:18|750)(?![\d,.])""").containsMatchIn(clean) ||
            clean.contains("هجده") || clean.contains("هیجده")
        if (!(clean.contains("طلا") || clean.contains("گرم") || clean.contains("عیار")) || !hasKarat18) {
            return false
        }
        val segment = isolateKeywordSegment(clean, kws)
        if (Regex("""(?<![\d,.])16(?![\d,.])""").containsMatchIn(segment) || segment.contains("شانزده")) return false
        return !hasGoldPrefixExclusion(clean, kws) && !hasGoldExclusionInSegment(segment)
    }

    private fun isGenericGramGoldLine(clean: String): Boolean {
        val kws = listOf("هر گرم طلا", "گرم طلا", "طلای خام")
        if (!kws.any { clean.contains(it) }) return false
        val segment = isolateKeywordSegment(clean, kws)
        if (Regex("""(?<![\d,.])16(?![\d,.])""").containsMatchIn(segment) || segment.contains("شانزده")) return false
        return !hasGoldPrefixExclusion(clean, kws) && !hasGoldExclusionInSegment(segment)
    }

    private fun isRobSekehLine(clean: String): Boolean {
        if (clean.contains("حباب") || clean.contains("تورم")) return false
        return clean.contains("ربع سکه") || clean.contains("سکه ربع") ||
            (clean.contains("ربع") && !clean.contains("امامی") && !clean.contains("نیم"))
    }

    private fun isSekehEmamiStrictLine(clean: String): Boolean {
        if (clean.contains("ربع") || clean.contains("نیم") || clean.contains("گرمی") ||
            clean.contains("پارسیان") || clean.contains("قدیم") || clean.contains("حباب")
        ) {
            return false
        }
        return clean.contains("سکه امامی") || clean.contains("امامی") || clean.contains("طرح جدید")
    }

    private fun isSekehTamamFallbackLine(clean: String): Boolean {
        if (clean.contains("ربع") || clean.contains("نیم") || clean.contains("گرمی") ||
            clean.contains("پارسیان") || clean.contains("قدیم") || clean.contains("حباب")
        ) {
            return false
        }
        return clean.contains("سکه تمام")
    }

    private fun isTetherLine(clean: String): Boolean {
        // Reject trading pairs like BTC/USDT, ETH/USDT, BTCUSDT
        if (Regex("""[a-z]{2,6}\s*[/\-]\s*usdt""").containsMatchIn(clean) ||
            Regex("""[a-z]{2,6}usdt\b""").containsMatchIn(clean)
        ) {
            // Only allow if there is STILL an isolated "تتر" segment with Toman/Rial
            if (!clean.contains("تتر")) return false
        }
        val hasTetherWord = clean.contains("تتر") || Regex("""\busdt\b""").containsMatchIn(clean)
        if (!hasTetherWord) return false

        // If line is purely a Bitcoin line like "بیت کوین به تتر: 65288", reject as Tether unless an explicit تتر price segment exists
        val mentionsOtherCoin = isBitcoinLine(clean) || clean.contains("اتریوم") || Regex("""\beth\b""").containsMatchIn(clean)
        if (mentionsOtherCoin) {
            val tetherSeg = isolateKeywordSegment(clean, listOf("تتر", "usdt"))
            return tetherSeg.contains("تومان") || tetherSeg.contains("تومن") || tetherSeg.contains("ریال")
        }
        return true
    }

    private fun isBitcoinLine(clean: String): Boolean {
        return clean.contains("بیت کوین") || clean.contains("بیتکوین") ||
            Regex("""\bbitcoin\b""").containsMatchIn(clean) ||
            Regex("""\bbtc\b""").containsMatchIn(clean)
    }

    private fun extractNumberFromWindow(
        lines: List<String>,
        startIndex: Int,
        maxLookAhead: Int,
        minValid: Double,
        maxValid: Double,
        isUsdAsset: Boolean = false,
        assumeTomanThreshold: Long = Long.MAX_VALUE,
        excludeLiteralNumbers: Set<Long> = emptySet(),
        keywordPatterns: List<String> = emptyList(),
        rejectUsdSymbol: Boolean = false,
        rejectTomanKeywords: Boolean = false,
        stopIfMatches: (String) -> Boolean = { false }
    ): ParsedBaleValue? {
        val endIndex = minOf(lines.lastIndex, startIndex + maxLookAhead)
        var bestBuyFallback: ParsedBaleValue? = null

        // Check nearby lines (±3) for message-level unit declaration ("بر حسب ریال" / "به ریال")
        val blockStart = maxOf(0, startIndex - 2)
        val blockEnd = minOf(lines.lastIndex, startIndex + 3)
        var blockHasRialHint = false
        var blockHasTomanHint = false
        for (b in blockStart..blockEnd) {
            val bl = lines[b].lowercase()
            if (bl.contains("به ریال") || bl.contains("بر حسب ریال") || bl.contains("نرخ ریال")) {
                blockHasRialHint = true
            }
            if (bl.contains("به تومان") || bl.contains("بر حسب تومان")) {
                blockHasTomanHint = true
            }
        }

        for (idx in startIndex..endIndex) {
            val rawLower = lines[idx].lowercase()
            val lineClean = stripDatesAndTimes(rawLower)
            if (lineClean.isBlank()) continue

            if (idx > startIndex && stopIfMatches(lineClean)) {
                break
            }

            // Skip delta / bubble / metadata lines unless they also explicitly carry قیمت/فروش/نقدی
            if (isNonPriceMetadataLine(lineClean)) {
                continue
            }

            // For the header/keyword line itself, isolate the segment matching this specific asset
            val segment = if (idx == startIndex) {
                isolateKeywordSegment(lineClean, keywordPatterns)
            } else {
                lineClean
            }

            val hasSell = segment.contains("فروش")
            val hasBuy = segment.contains("خرید")

            // If BOTH خرید and فروش are on the SAME segment, first parse specifically from "فروش" onward!
            if (hasSell && hasBuy) {
                val sellIdx = segment.indexOf("فروش")
                val buyIdx = segment.indexOf("خرید")
                val sellSlice = if (sellIdx > buyIdx) {
                    segment.substring(sellIdx)
                } else {
                    segment.substring(sellIdx, buyIdx)
                }
                val sellVal = extractBestNumberFromSegment(
                    segment = sellSlice,
                    fullLineForDelta = rawLower,
                    minValid = minValid,
                    maxValid = maxValid,
                    isUsdAsset = isUsdAsset,
                    assumeTomanThreshold = assumeTomanThreshold,
                    excludeLiteralNumbers = excludeLiteralNumbers,
                    blockHasRialHint = blockHasRialHint,
                    blockHasTomanHint = blockHasTomanHint,
                    rejectUsdSymbol = rejectUsdSymbol,
                    rejectTomanKeywords = rejectTomanKeywords
                )
                if (sellVal != null) return sellVal
            }

            val parsed = extractBestNumberFromSegment(
                segment = segment,
                fullLineForDelta = rawLower,
                minValid = minValid,
                maxValid = maxValid,
                isUsdAsset = isUsdAsset,
                assumeTomanThreshold = assumeTomanThreshold,
                excludeLiteralNumbers = excludeLiteralNumbers,
                blockHasRialHint = blockHasRialHint,
                blockHasTomanHint = blockHasTomanHint,
                rejectUsdSymbol = rejectUsdSymbol,
                rejectTomanKeywords = rejectTomanKeywords
            )

            if (parsed != null) {
                if (hasSell) {
                    return parsed
                } else if (hasBuy) {
                    if (bestBuyFallback == null) bestBuyFallback = parsed
                } else {
                    return parsed
                }
            }

            // Fallback when price appears BEFORE the asset keyword on the same line (e.g. "16,540,000 | طلای 18 عیار")
            if (idx == startIndex && bestBuyFallback == null) {
                val preSlice = isolatePreKeywordSegment(lineClean, keywordPatterns)
                if (preSlice != null) {
                    val preParsed = extractBestNumberFromSegment(
                        segment = preSlice,
                        fullLineForDelta = rawLower,
                        minValid = minValid,
                        maxValid = maxValid,
                        isUsdAsset = isUsdAsset,
                        assumeTomanThreshold = assumeTomanThreshold,
                        excludeLiteralNumbers = excludeLiteralNumbers,
                        blockHasRialHint = blockHasRialHint,
                        blockHasTomanHint = blockHasTomanHint,
                        rejectUsdSymbol = rejectUsdSymbol,
                        rejectTomanKeywords = rejectTomanKeywords
                    )
                    if (preParsed != null) return preParsed
                }
            }
        }

        return bestBuyFallback
    }

    private fun isNonPriceMetadataLine(lineClean: String): Boolean {
        val metaHints = listOf(
            "تغییر", "نوسان", "حباب", "اختلاف", "بازدید", "تعداد", "شماره", "آیدی", "کد"
        )
        val hasMetaHint = metaHints.any { lineClean.contains(it) }
        if (!hasMetaHint) return false
        val hasPriceLabel = lineClean.contains("قیمت") || lineClean.contains("فروش") ||
            lineClean.contains("نرخ") || lineClean.contains("نقدی")
        return !hasPriceLabel
    }

    private fun extractBestNumberFromSegment(
        segment: String,
        fullLineForDelta: String,
        minValid: Double,
        maxValid: Double,
        isUsdAsset: Boolean,
        assumeTomanThreshold: Long,
        excludeLiteralNumbers: Set<Long>,
        blockHasRialHint: Boolean,
        blockHasTomanHint: Boolean,
        rejectUsdSymbol: Boolean,
        rejectTomanKeywords: Boolean
    ): ParsedBaleValue? {
        val hasToman = segment.contains("تومان") || segment.contains("تومن") || (!segment.contains("ریال") && blockHasTomanHint)
        val hasRial = segment.contains("ریال") || (!hasToman && blockHasRialHint)

        if (rejectTomanKeywords && (hasToman || segment.contains("ریال"))) {
            return null
        }

        val matches = NUMBER_REGEX.findAll(segment).toList()
        for (match in matches) {
            val startIdx = match.range.first
            val endIdx = match.range.last + 1

            val beforeContext = segment.substring(maxOf(0, startIdx - 14), startIdx)
            val beforeTight = beforeContext.trimEnd()
            val afterContext = segment.substring(endIdx, minOf(segment.length, endIdx + 16))
            val afterTight = afterContext.trimStart()

            // Reject numbers prefixed by + or - (those are change deltas like +12,500)
            if (beforeTight.endsWith("+") || beforeTight.endsWith("-")) {
                continue
            }
            // Reject change/bubble/rank numbers right after keywords like "تغییر: 12,000"
            if (beforeContext.contains("تغییر") || beforeContext.contains("نوسان") ||
                beforeContext.contains("حباب") || beforeContext.contains("اختلاف")
            ) {
                continue
            }
            // Reject percentages
            if (afterTight.startsWith("%")) {
                continue
            }
            // Reject USD amounts when parsing Toman-only asset (e.g. Tether must not grab "$65,288" or "65,288 دلار")
            if (rejectUsdSymbol && !hasToman && !hasRial) {
                if (beforeTight.endsWith("$") || afterTight.startsWith("$") ||
                    afterTight.startsWith("دلار") || afterTight.startsWith("usd")
                ) {
                    continue
                }
            }

            var parsedDouble = parseFlexibleNumber(match.value) ?: continue
            if (afterTight.startsWith("میلیون")) {
                parsedDouble *= 1_000_000.0
            }

            val rawRounded = parsedDouble.roundToLong()
            if (rawRounded in excludeLiteralNumbers) continue

            val convertedDouble: Double
            val convertedLong: Long
            if (isUsdAsset) {
                convertedDouble = parsedDouble
                convertedLong = rawRounded
            } else if (hasRial) {
                convertedDouble = parsedDouble / 10.0
                convertedLong = (parsedDouble / 10.0).roundToLong()
            } else if (!hasToman && rawRounded >= assumeTomanThreshold) {
                convertedDouble = parsedDouble / 10.0
                convertedLong = (parsedDouble / 10.0).roundToLong()
            } else {
                convertedDouble = parsedDouble
                convertedLong = rawRounded
            }

            if (convertedDouble in minValid..maxValid) {
                val changeInfo = extractChangeInfo(fullLineForDelta)
                return ParsedBaleValue(
                    amount = convertedLong,
                    doubleAmount = convertedDouble,
                    changeAmount = changeInfo.first,
                    changePercent = changeInfo.second,
                    isPositive = changeInfo.third
                )
            }
        }
        return null
    }

    private fun extractChangeInfo(line: String): Triple<Long, Double, Boolean> {
        val percentMatch = Regex("""([+-]?)\s*(\d+(?:\.\d+)?)\s*%""").find(line)
        val hasDownHint = line.contains("▼") || line.contains("کاهش") || line.contains("منفی")
        val hasUpHint = line.contains("▲") || line.contains("افزایش") || line.contains("مثبت")
        if (percentMatch != null) {
            val sign = percentMatch.groupValues[1]
            val pct = percentMatch.groupValues[2].toDoubleOrNull() ?: 0.0
            val positive = when {
                sign == "-" || hasDownHint -> false
                sign == "+" || hasUpHint -> true
                else -> true
            }
            return Triple(0L, abs(pct), positive)
        }
        return Triple(0L, 0.0, !hasDownHint)
    }

    fun parseFlexibleNumber(token: String): Double? {
        val clean = token.trim().trimEnd('.', ',')
        if (clean.isBlank()) return null

        return when {
            clean.contains(',') && clean.contains('.') -> {
                clean.replace(",", "").toDoubleOrNull()
            }
            clean.contains(',') -> {
                clean.replace(",", "").toDoubleOrNull()
            }
            clean.contains('.') -> {
                val parts = clean.split('.')
                if (parts.size == 2 && parts[1].length != 3) {
                    clean.toDoubleOrNull()
                } else {
                    clean.replace(".", "").toDoubleOrNull()
                }
            }
            else -> clean.toDoubleOrNull()
        }
    }
}
