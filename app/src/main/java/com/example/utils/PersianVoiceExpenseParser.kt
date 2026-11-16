package com.example.utils

import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToLong

/**
 * Offline Persian speech-to-expense parser.
 * Extracts monetary amount (in Tomans), clean expense title, relative date (امروز / دیروز / پریروز),
 * and annual commitment detection (بیمه / عوارض / سالانه) from natural Persian transcripts.
 */
object PersianVoiceExpenseParser {

    data class ParsedVoiceExpense(
        val title: String,
        val amountTomans: Long,
        val shamsiDateString: String,
        val isAnnual: Boolean,
        val rawTranscript: String
    )

    private val numberWords = mapOf(
        "صفر" to 0.0,
        "یک" to 1.0, "یه" to 1.0,
        "دو" to 2.0,
        "سه" to 3.0,
        "چهار" to 4.0, "چار" to 4.0,
        "پنج" to 5.0,
        "شش" to 6.0, "شیش" to 6.0,
        "هفت" to 7.0,
        "هشت" to 8.0,
        "نه" to 9.0,
        "ده" to 10.0,
        "یازده" to 11.0,
        "دوازده" to 12.0,
        "سیزده" to 13.0,
        "چهارده" to 14.0,
        "پانزده" to 15.0, "پونزده" to 15.0,
        "شانزده" to 16.0, "شونزده" to 16.0,
        "هفده" to 17.0, "هیفده" to 17.0,
        "هجده" to 18.0, "هیجده" to 18.0,
        "نوزده" to 19.0,
        "بیست" to 20.0,
        "سی" to 30.0,
        "چهل" to 40.0,
        "پنجاه" to 50.0,
        "شصت" to 60.0,
        "هفتاد" to 70.0,
        "هشتاد" to 80.0,
        "نود" to 90.0,
        "صد" to 100.0, "یکصد" to 100.0,
        "دویست" to 200.0,
        "سیصد" to 300.0,
        "چهارصد" to 400.0, "چارصد" to 400.0,
        "پانصد" to 500.0, "پونصد" to 500.0,
        "ششصد" to 600.0, "شیشصد" to 600.0,
        "هفتصد" to 700.0,
        "هشتصد" to 800.0,
        "نهصد" to 900.0
    )

    private val scaleWords = mapOf(
        "هزار" to 1_000L,
        "هزاری" to 1_000L,
        "میلیون" to 1_000_000L,
        "ملیون" to 1_000_000L,
        "میلیارد" to 1_000_000_000L,
        "ملیارد" to 1_000_000_000L
    )

    private val tomanCurrencyWords = setOf("تومان", "تومن", "تومانی", "تومنی")
    private val rialCurrencyWords = setOf("ریال", "ریالی")

    private val fillerWords = setOf(
        "بابت", "برای", "واسه", "خرج", "هزینه", "پرداخت", "کردم", "دادم",
        "گرفتم", "خریدم", "شدم", "شد", "بود", "مبلغ", "میشه", "می‌شه", "به", "از"
    )

    private val relativeDateOffsets = mapOf(
        "امروز" to 0,
        "دیروز" to -1,
        "پریروز" to -2
    )

    private data class CandidateAmount(
        val startIndex: Int,
        val endIndexExclusive: Int,
        val consumedIndices: Set<Int>,
        val amountTomans: Long,
        val hasCurrency: Boolean,
        val hasScale: Boolean
    ) {
        fun priorityScore(): Int {
            var score = 0
            if (hasCurrency) score += 100
            if (hasScale) score += 50
            if (amountTomans >= 1000L) score += 20
            if (amountTomans >= 100L) score += 10
            return score
        }
    }

    fun normalizePersianText(raw: String): String {
        var text = raw
            .replace('ي', 'ی')
            .replace('ك', 'ک')
            .replace('ة', 'ه')
            .replace('ۀ', 'ه')
            .replace('٫', '.')
            .trim()
        text = PersianDateHelper.toEnglishDigits(text)
        // Remove thousand separators such as 250,000 or 250،000
        while (true) {
            val updated = text.replace(Regex("(\\d)[,،](\\d{3})"), "$1$2")
            if (updated == text) break
            text = updated
        }
        // Convert fractional slash between digits followed by scale (e.g., 2/5 میلیون -> 2.5 میلیون)
        text = text.replace(Regex("(\\d+)/(\\d+)(\\s*(?:هزار|میلیون|ملیون|میلیارد|ملیارد))"), "$1.$2$3")
        // Separate digits stuck to Persian letters (e.g., 500هزار or 250تومان)
        text = text.replace(Regex("(\\d)([\\u0600-\\u06FF])"), "$1 $2")
        text = text.replace(Regex("([\\u0600-\\u06FF])(\\d)"), "$1 $2")
        // Collapse extra spaces
        return text.replace(Regex("\\s+"), " ").trim()
    }

    private fun cleanLookupToken(token: String): String {
        return token
            .replace("\u200C", "")
            .trim(',', '،', '.', '؛', ':', '!', '؟', '?', '"', '\'', '«', '»', '(', ')')
            .lowercase(Locale.ROOT)
    }

    private fun isDecimalOrIntToken(cleanToken: String): Double? {
        if (cleanToken.isEmpty()) return null
        return cleanToken.toDoubleOrNull()
    }

    private fun isNumberPartToken(cleanToken: String): Boolean {
        return cleanToken == "نیم" ||
            numberWords.containsKey(cleanToken) ||
            scaleWords.containsKey(cleanToken) ||
            isDecimalOrIntToken(cleanToken) != null
    }

    fun parse(
        rawTranscript: String,
        referenceTimeMillis: Long = System.currentTimeMillis()
    ): ParsedVoiceExpense {
        val normalized = normalizePersianText(rawTranscript)
        if (normalized.isBlank()) {
            val today = PersianDateHelper.getShamsiDate(referenceTimeMillis).format()
            return ParsedVoiceExpense(
                title = "",
                amountTomans = 0L,
                shamsiDateString = today,
                isAnnual = false,
                rawTranscript = rawTranscript
            )
        }

        val rawTokens = normalized.split(" ").filter { it.isNotBlank() }
        val cleanTokens = rawTokens.map { cleanLookupToken(it) }

        // 1. Detect relative date token (امروز / دیروز / پریروز)
        var dayOffset = 0
        val dateTokenIndices = mutableSetOf<Int>()
        for (i in cleanTokens.indices) {
            val offset = relativeDateOffsets[cleanTokens[i]]
            if (offset != null) {
                dayOffset = offset
                dateTokenIndices.add(i)
            }
        }
        val cal = Calendar.getInstance().apply {
            timeInMillis = referenceTimeMillis
            add(Calendar.DAY_OF_YEAR, dayOffset)
        }
        val shamsiDateString = PersianDateHelper.getShamsiDate(cal.timeInMillis).format()

        // 2. Scan candidate numeric/amount expressions
        val candidates = mutableListOf<CandidateAmount>()
        var idx = 0
        while (idx < cleanTokens.size) {
            val tok = cleanTokens[idx]
            if (isDecimalOrIntToken(tok) != null || numberWords.containsKey(tok) || scaleWords.containsKey(tok)) {
                val candidate = parseAmountSpan(cleanTokens, idx)
                if (candidate != null && candidate.amountTomans > 0L) {
                    candidates.add(candidate)
                    idx = candidate.endIndexExclusive
                    continue
                }
            }
            idx++
        }

        val bestCandidate = candidates.maxWithOrNull(
            compareBy<CandidateAmount> { it.priorityScore() }.thenBy { it.amountTomans }
        )

        val amountTomans = bestCandidate?.amountTomans ?: 0L
        val consumedForAmount = bestCandidate?.consumedIndices ?: emptySet()

        // 3. Reconstruct clean title by filtering out amount, date, and filler tokens
        val remainingIndices = rawTokens.indices.filter { i ->
            i !in consumedForAmount && i !in dateTokenIndices
        }.toMutableList()

        // Trim leading/trailing filler words and orphan "و"
        while (remainingIndices.isNotEmpty()) {
            val firstClean = cleanTokens[remainingIndices.first()]
            if (firstClean in fillerWords || firstClean == "و") {
                remainingIndices.removeAt(0)
            } else {
                break
            }
        }
        while (remainingIndices.isNotEmpty()) {
            val lastClean = cleanTokens[remainingIndices.last()]
            if (lastClean in fillerWords || lastClean == "و") {
                remainingIndices.removeAt(remainingIndices.lastIndex)
            } else {
                break
            }
        }

        val titleTokens = remainingIndices
            .filter { i -> cleanTokens[i] !in setOf("بابت", "واسه", "مبلغ", "پرداخت", "کردم", "دادم", "شدم") }
            .map { rawTokens[it] }

        // Clean any remaining leading/trailing "و" after inner filler removal
        val cleanedTitleList = titleTokens.toMutableList()
        while (cleanedTitleList.isNotEmpty() && cleanLookupToken(cleanedTitleList.first()) == "و") {
            cleanedTitleList.removeAt(0)
        }
        while (cleanedTitleList.isNotEmpty() && cleanLookupToken(cleanedTitleList.last()) == "و") {
            cleanedTitleList.removeAt(cleanedTitleList.lastIndex)
        }

        val extractedTitle = cleanedTitleList.joinToString(" ").trim()
        val isAnnual = extractedTitle.contains("بیمه") ||
            extractedTitle.contains("عوارض") ||
            extractedTitle.contains("سالانه") ||
            normalized.contains("بیمه") ||
            normalized.contains("عوارض") ||
            normalized.contains("سالانه")

        return ParsedVoiceExpense(
            title = extractedTitle,
            amountTomans = amountTomans,
            shamsiDateString = shamsiDateString,
            isAnnual = isAnnual,
            rawTranscript = rawTranscript
        )
    }

    private fun parseAmountSpan(cleanTokens: List<String>, startIndex: Int): CandidateAmount? {
        var total = 0.0
        var currentGroup = 0.0
        var lastScale = 1L
        var hasScale = false
        var hasCurrency = false
        var isRial = false
        var i = startIndex
        val consumed = mutableSetOf<Int>()

        while (i < cleanTokens.size) {
            val token = cleanTokens[i]
            val numericVal = isDecimalOrIntToken(token)
            val wordVal = numberWords[token]
            val scaleVal = scaleWords[token]

            when {
                numericVal != null -> {
                    currentGroup += numericVal
                    consumed.add(i)
                    i++
                }
                wordVal != null -> {
                    currentGroup += wordVal
                    consumed.add(i)
                    i++
                }
                token == "نیم" -> {
                    if (currentGroup > 0.0) {
                        currentGroup += 0.5
                    } else if (lastScale > 1L) {
                        total += 0.5 * lastScale
                    } else {
                        currentGroup += 0.5
                    }
                    consumed.add(i)
                    i++
                }
                scaleVal != null -> {
                    hasScale = true
                    val factor = if (currentGroup == 0.0) 1.0 else currentGroup
                    total += factor * scaleVal
                    currentGroup = 0.0
                    lastScale = scaleVal
                    consumed.add(i)
                    i++
                }
                token == "و" -> {
                    val nextClean = cleanTokens.getOrNull(i + 1)
                    if (nextClean != null && isNumberPartToken(nextClean)) {
                        consumed.add(i)
                        i++
                    } else {
                        break
                    }
                }
                token in tomanCurrencyWords -> {
                    hasCurrency = true
                    consumed.add(i)
                    i++
                    break
                }
                token in rialCurrencyWords -> {
                    hasCurrency = true
                    isRial = true
                    consumed.add(i)
                    i++
                    break
                }
                else -> break
            }
        }

        if (consumed.isEmpty()) return null

        // Handle colloquial "دو میلیون و پونصد" -> 2,500,000 when trailing group < 1000 after میلیون
        if (currentGroup > 0.0) {
            if (lastScale == 1_000_000L && currentGroup < 1000.0 && currentGroup >= 1.0) {
                total += currentGroup * 1_000L
            } else {
                total += currentGroup
            }
        }

        var finalAmount = total.roundToLong()
        if (isRial && finalAmount >= 10L) {
            finalAmount /= 10L
        }

        return CandidateAmount(
            startIndex = startIndex,
            endIndexExclusive = i,
            consumedIndices = consumed,
            amountTomans = finalAmount,
            hasCurrency = hasCurrency,
            hasScale = hasScale
        )
    }
}
