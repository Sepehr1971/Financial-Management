package com.example.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Utility for Jalali (Shamsi) and Gregorian calendar conversion, formatting,
 * and Persian numerals.
 */
object PersianDateHelper {

    val PERSIAN_MONTH_NAMES = listOf(
        "فروردین", "اردیبهشت", "خرداد",
        "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر",
        "دی", "بهمن", "اسفند"
    )

    fun getMonthName(month: Int): String {
        return if (month in 1..12) PERSIAN_MONTH_NAMES[month - 1] else ""
    }

    private val PERSIAN_WEEK_DAYS = listOf(
        "یک‌شنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه", "شنبه"
    )

    data class ShamsiDate(
        val year: Int,
        val month: Int, // 1..12
        val day: Int,   // 1..31
        val dayOfWeek: String = "",
        val monthName: String = if (month in 1..12) PERSIAN_MONTH_NAMES[month - 1] else ""
    ) {
        fun format(separator: String = "/"): String {
            val m = if (month < 10) "0$month" else "$month"
            val d = if (day < 10) "0$day" else "$day"
            return "$year$separator$m$separator$d"
        }

        fun toReadablePersian(): String {
            return "${dayOfWeek} $day $monthName $year"
        }
    }

    /**
     * Convert Gregorian date to Jalali (Solar Hijri)
     */
    fun gregorianToJalali(gYear: Int, gMonth: Int, gDay: Int): ShamsiDate {
        val gDm = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val gy2 = if (gMonth > 2) gYear + 1 else gYear
        var days = 355666 + (365 * gYear) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) + ((gy2 + 399) / 400) + gDay + gDm[gMonth - 1]
        var jy = -1595 + (33 * (days / 12053))
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm = if (days < 186) 1 + (days / 31) else 7 + ((days - 186) / 30)
        val jd = 1 + (if (days < 186) (days % 31) else ((days - 186) % 30))

        val cal = Calendar.getInstance()
        cal.set(gYear, gMonth - 1, gDay)
        val dayOfWeekIndex = cal.get(Calendar.DAY_OF_WEEK) - 1
        val dayOfWeek = PERSIAN_WEEK_DAYS[dayOfWeekIndex]

        return ShamsiDate(jy, jm, jd, dayOfWeek)
    }

    /**
     * Convert Jalali (Shamsi) to Gregorian
     */
    fun jalaliToGregorian(jYear: Int, jMonth: Int, jDay: Int): Triple<Int, Int, Int> {
        val jy2 = jYear + 1595
        var days = -355668 + (365 * jy2) + (jy2 / 33) * 8 + ((jy2 % 33 + 3) / 4) + jDay + (if (jMonth < 7) (jMonth - 1) * 31 else ((jMonth - 7) * 30 + 186))
        var gy = 400 * (days / 146097)
        days %= 146097
        if (days > 36524) {
            gy += 100 * (--days / 36524)
            days %= 36524
            if (days >= 365) gy++
        }
        gy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            gy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val gd = days + 1
        val isLeap = (gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0)
        val salG = intArrayOf(0, 31, if (isLeap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var gm = 0
        var d = gd
        while (gm < 12 && d > salG[gm + 1]) {
            gm++
            d -= salG[gm]
        }
        gm++
        return Triple(gy, gm, d)
    }

    /**
     * Convert timestamp to ShamsiDate
     */
    fun getShamsiDate(timestamp: Long = System.currentTimeMillis()): ShamsiDate {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        return gregorianToJalali(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    val SHAMSI_WEEK_DAY_NAMES = listOf("شنبه", "۱شنبه", "۲شنبه", "۳شنبه", "۴شنبه", "۵شنبه", "جمعه")
    val SHAMSI_WEEK_DAY_FULL_NAMES = listOf("شنبه", "یک‌شنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه")

    /**
     * Day of week for the 1st of a Shamsi month:
     * 0: شنبه, 1: یک‌شنبه, 2: دوشنبه, 3: سه‌شنبه, 4: چهارشنبه, 5: پنج‌شنبه, 6: جمعه
     */
    fun getFirstDayOfWeekForShamsiMonth(year: Int, month: Int): Int {
        val (gy, gm, gd) = jalaliToGregorian(year, month, 1)
        val cal = Calendar.getInstance()
        cal.set(gy, gm - 1, gd)
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> 0
            Calendar.SUNDAY -> 1
            Calendar.MONDAY -> 2
            Calendar.TUESDAY -> 3
            Calendar.WEDNESDAY -> 4
            Calendar.THURSDAY -> 5
            Calendar.FRIDAY -> 6
            else -> 0
        }
    }

    val GREGORIAN_MONTH_NAMES_FA = listOf(
        "ژانویه", "فوریه", "مارس", "آوریل", "مه", "ژوئن",
        "ژوئیه", "اوت", "سپتامبر", "اکتبر", "نوامبر", "دسامبر"
    )

    fun getGregorianPersianMonthDay(timestamp: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        val monthIdx = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val monthName = GREGORIAN_MONTH_NAMES_FA.getOrElse(monthIdx) { "" }
        return "$day $monthName"
    }

    fun getGregorianNumericDate(timestamp: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.US)
        return sdf.format(Date(timestamp))
    }

    /**
     * Formats Gregorian Date with full month name (e.g. October instead of Oct)
     */
    fun getGregorianFormatted(timestamp: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH)
        return sdf.format(Date(timestamp))
    }

    /**
     * Live time string HH:mm:ss
     */
    fun getTimeFormatted(timestamp: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)
        return sdf.format(Date(timestamp))
    }

    /**
     * Convert Persian and Arabic digits to standard ASCII English digits (0-9)
     */
    fun toEnglishDigits(input: String): String {
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        var result = input
        for (i in 0..9) {
            result = result.replace(persianDigits[i], ('0' + i)).replace(arabicDigits[i], ('0' + i))
        }
        return result
    }

    /**
     * Convert English digits to Persian digits
     */
    fun toPersianDigits(input: String): String {
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val sb = StringBuilder()
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(persianDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun toPersianDigits(number: Long): String {
        return toPersianDigits(number.toString())
    }

    fun toPersianDigits(number: Int): String {
        return toPersianDigits(number.toString())
    }

    /**
     * Format currency amount with commas and "تومان" (using non-breaking space to prevent vertical wrapping)
     */
    fun formatCurrency(amount: Long, includeUnit: Boolean = true): String {
        val str = "%,d".format(Locale.US, amount)
        val persianStr = toPersianDigits(str)
        return if (includeUnit) "$persianStr\u00A0تومان" else persianStr
    }

    /**
     * Convert Jalali date to approximate start of day timestamp
     */
    fun jalaliToTimestamp(year: Int, month: Int, day: Int): Long {
        val (gy, gm, gd) = jalaliToGregorian(year, month, day)
        val cal = Calendar.getInstance()
        cal.set(gy, gm - 1, gd, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /**
     * Get number of days in a Shamsi month
     */
    fun getDaysInMonth(year: Int, month: Int): Int {
        return when {
            month in 1..6 -> 31
            month in 7..11 -> 30
            month == 12 -> if (isShamsiLeapYear(year)) 30 else 29
            else -> 30
        }
    }

    /**
     * Accurate Shamsi leap year check
     */
    fun isShamsiLeapYear(year: Int): Boolean {
        val r = year % 33
        return r == 1 || r == 5 || r == 9 || r == 13 || r == 17 || r == 22 || r == 26 || r == 30
    }
}
