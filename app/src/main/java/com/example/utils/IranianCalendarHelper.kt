package com.example.utils

import java.util.Calendar
import kotlin.math.floor

data class HijriDate(
    val year: Int,
    val month: Int, // 1..12
    val day: Int,   // 1..30
    val monthName: String
) {
    fun formatNumeric(): String {
        val m = if (month < 10) "0$month" else "$month"
        val d = if (day < 10) "0$day" else "$day"
        return "$year/$m/$d"
    }

    fun formatReadablePersian(): String {
        return "${PersianDateHelper.toPersianDigits(day)} $monthName ${PersianDateHelper.toPersianDigits(year)}"
    }

    fun formatDayMonthPersian(): String {
        return "${PersianDateHelper.toPersianDigits(day)} $monthName"
    }
}

data class HolidayReasonEntry(
    val shamsiDay: Int,
    val shamsiMonth: Int,
    val hijriLabel: String,
    val reason: String,
    val isFriday: Boolean = false
)

data class CalendarDayCell(
    val shamsiYear: Int,
    val shamsiMonth: Int,
    val shamsiDay: Int,
    val gregorianYear: Int,
    val gregorianMonth: Int,
    val gregorianDay: Int,
    val hijriDate: HijriDate,
    val isToday: Boolean,
    val isFriday: Boolean,
    val isOfficialHoliday: Boolean,
    val holidayReasons: List<String>
)

data class MonthlyCalendarUiData(
    val shamsiYear: Int,
    val shamsiMonth: Int,
    val monthName: String,
    val firstDayOfWeekOffset: Int, // 0=شنبه .. 6=جمعه
    val days: List<CalendarDayCell>,
    val officialHolidayEntries: List<HolidayReasonEntry>
)

/**
 * تقویم رسمی ایران و تعطیلات رسمی (منطبق با تقویم باد صبا)
 * شامل تبدیل دقیق تاریخ هجری قمری رسمی ایران و استخراج تعطیلات ثابت شمسی و قمری
 */
object IranianCalendarHelper {

    val HIJRI_MONTH_NAMES = listOf(
        "محرم", "صفر", "ربیع‌الاول", "ربیع‌الثانی",
        "جمادی‌الاول", "جمادی‌الثانی", "رجب", "شعبان",
        "رمضان", "شوال", "ذی‌القعده", "ذی‌الحجه"
    )

    private data class HijriMonthAnchor(
        val jYear: Int,
        val jMonth: Int,
        val jDay: Int,
        val hYear: Int,
        val hMonth: Int,
        val lengthDays: Int
    ) {
        val startTimestampDayIndex: Long by lazy {
            shamsiDayIndex(jYear, jMonth, jDay)
        }
    }

    /**
     * تقویم رسمی هجری قمری ایران منطبق با باد صبا (۱۴۴۷ تا ۱۴۴۸ هـ.ق / ۱۴۰۴ تا ۱۴۰۶ هـ.ش)
     */
    private val OFFICIAL_IRAN_HIJRI_ANCHORS = listOf(
        // سال ۱۴۴۷ قمری (اواخر ۱۴۰۴ و ابتدای ۱۴۰۵)
        HijriMonthAnchor(1404, 11, 30, 1447, 9, 30),   // ۱ رمضان ۱۴۴۷
        HijriMonthAnchor(1405, 1, 1, 1447, 10, 29),    // ۱ شوال ۱۴۴۷ (عید فطر)
        HijriMonthAnchor(1405, 1, 30, 1447, 11, 29),   // ۱ ذی‌القعده ۱۴۴۷
        HijriMonthAnchor(1405, 2, 28, 1447, 12, 29),   // ۱ ذی‌الحجه ۱۴۴۷ (۱۰ ذی‌الحجه = ۶ خرداد)

        // سال ۱۴۴۸ قمری (خرداد ۱۴۰۵ تا خرداد ۱۴۰۶)
        HijriMonthAnchor(1405, 3, 26, 1448, 1, 30),    // ۱ محرم ۱۴۴۸ (۹ محرم = ۳ تیر، ۱۰ محرم = ۴ تیر)
        HijriMonthAnchor(1405, 4, 25, 1448, 2, 29),    // ۱ صفر ۱۴۴۸ (۲۰ صفر = ۱۳ مرداد، ۲۹ صفر = ۲۲ مرداد)
        HijriMonthAnchor(1405, 5, 23, 1448, 3, 30),    // ۱ ربیع‌الاول ۱۴۴۸ (۸ = ۳۰ مرداد، ۱۷ = ۸ شهریور)
        HijriMonthAnchor(1405, 6, 22, 1448, 4, 29),    // ۱ ربیع‌الثانی ۱۴۴۸
        HijriMonthAnchor(1405, 7, 20, 1448, 5, 30),    // ۱ جمادی‌الاول ۱۴۴۸
        HijriMonthAnchor(1405, 8, 20, 1448, 6, 30),    // ۱ جمادی‌الثانی ۱۴۴۸ (۳ جمادی‌الثانی = ۲۲ آبان ۱۴۰۵)
        HijriMonthAnchor(1405, 9, 20, 1448, 7, 30),    // ۱ رجب ۱۴۴۸ (۱۳ رجب = ۲ دی، ۲۷ رجب = ۱۶ دی)
        HijriMonthAnchor(1405, 10, 20, 1448, 8, 30),   // ۱ شعبان ۱۴۴۸ (۱۵ شعبان = ۴ بهمن)
        HijriMonthAnchor(1405, 11, 20, 1448, 9, 29),   // ۱ رمضان ۱۴۴۸ (۲۱ رمضان = ۱۰ اسفند)
        HijriMonthAnchor(1405, 12, 19, 1448, 10, 30),  // ۱ شوال ۱۴۴۸ (۱۹ اسفند ۱۴۰۵)
        HijriMonthAnchor(1406, 1, 19, 1448, 11, 29),   // ۱ ذی‌القعده ۱۴۴۸
        HijriMonthAnchor(1406, 2, 18, 1448, 12, 30),   // ۱ ذی‌الحجه ۱۴۴۸
        HijriMonthAnchor(1406, 3, 17, 1449, 1, 30)     // ۱ محرم ۱۴۴۹
    )

    private val SHAMSI_FIXED_HOLIDAYS: Map<Pair<Int, Int>, String> = mapOf(
        (1 to 1) to "جشن نوروز",
        (1 to 2) to "عید نوروز",
        (1 to 3) to "عید نوروز",
        (1 to 4) to "عید نوروز",
        (1 to 12) to "روز جمهوری اسلامی ایران",
        (1 to 13) to "روز طبیعت (سیزده‌به‌در)",
        (3 to 14) to "رحلت حضرت امام خمینی (ره)",
        (3 to 15) to "قیام خونین ۱۵ خرداد",
        (11 to 22) to "پیروزی انقلاب اسلامی ایران",
        (12 to 29) to "روز ملی شدن صنعت نفت ایران"
    )

    private fun getHijriHolidayReason(hMonth: Int, hDay: Int, monthLength: Int = 30): String? {
        return when (hMonth to hDay) {
            (1 to 9) -> "تاسوعای حسینی"
            (1 to 10) -> "عاشورای حسینی"
            (2 to 20) -> "اربعین حسینی"
            (2 to 28) -> "رحلت حضرت رسول اکرم (ص) و شهادت امام حسن مجتبی (ع)"
            (2 to 29) -> if (monthLength == 29) "شهادت امام رضا (ع)" else null
            (2 to 30) -> if (monthLength >= 30) "شهادت امام رضا (ع)" else null
            (3 to 8) -> "شهادت امام حسن عسکری (ع) و آغاز امامت حضرت ولی‌عصر (عج)"
            (3 to 17) -> "ولادت حضرت رسول اکرم (ص) و امام جعفر صادق (ع)"
            (6 to 3) -> "شهادت حضرت فاطمه زهرا (س)"
            (7 to 13) -> "ولادت امام علی (ع) و روز پدر"
            (7 to 27) -> "مبعث حضرت رسول اکرم (ص)"
            (8 to 15) -> "ولادت حضرت قائم (عج) / نیمه شعبان"
            (9 to 21) -> "شهادت حضرت علی (ع)"
            (10 to 1) -> "عید سعید فطر"
            (10 to 2) -> "تعطیل به مناسبت عید سعید فطر"
            (10 to 25) -> "شهادت امام جعفر صادق (ع)"
            (12 to 10) -> "عید سعید قربان"
            (12 to 18) -> "عید سعید غدیر خم"
            else -> null
        }
    }

    fun shamsiToHijri(jYear: Int, jMonth: Int, jDay: Int): Pair<HijriDate, Int> {
        val targetIndex = shamsiDayIndex(jYear, jMonth, jDay)

        for (anchor in OFFICIAL_IRAN_HIJRI_ANCHORS) {
            val startIdx = anchor.startTimestampDayIndex
            val diff = (targetIndex - startIdx).toInt()
            if (diff in 0 until anchor.lengthDays) {
                val hDay = diff + 1
                val mName = HIJRI_MONTH_NAMES.getOrElse(anchor.hMonth - 1) { "" }
                return HijriDate(anchor.hYear, anchor.hMonth, hDay, mName) to anchor.lengthDays
            }
        }

        val (gy, gm, gd) = PersianDateHelper.jalaliToGregorian(jYear, jMonth, jDay)
        val hijri = gregorianToHijriAlgorithmic(gy, gm, gd)
        val defaultLen = if (hijri.month % 2 == 1) 30 else 29
        return hijri to defaultLen
    }

    fun getHijriDate(timestamp: Long = System.currentTimeMillis()): HijriDate {
        val shamsi = PersianDateHelper.getShamsiDate(timestamp)
        return shamsiToHijri(shamsi.year, shamsi.month, shamsi.day).first
    }

    fun getMonthlyCalendar(
        shamsiYear: Int,
        shamsiMonth: Int,
        todayShamsi: PersianDateHelper.ShamsiDate = PersianDateHelper.getShamsiDate()
    ): MonthlyCalendarUiData {
        val daysCount = PersianDateHelper.getDaysInMonth(shamsiYear, shamsiMonth)
        val firstDayOffset = PersianDateHelper.getFirstDayOfWeekForShamsiMonth(shamsiYear, shamsiMonth)
        val monthName = PersianDateHelper.getMonthName(shamsiMonth)

        val dayCells = ArrayList<CalendarDayCell>(daysCount)
        val holidayEntries = ArrayList<HolidayReasonEntry>()

        for (day in 1..daysCount) {
            val dayOfWeekOffset = (firstDayOffset + day - 1) % 7
            val isFriday = dayOfWeekOffset == 6
            val isToday = shamsiYear == todayShamsi.year &&
                shamsiMonth == todayShamsi.month &&
                day == todayShamsi.day

            val (hijri, hMonthLen) = shamsiToHijri(shamsiYear, shamsiMonth, day)
            val reasons = mutableListOf<String>()

            SHAMSI_FIXED_HOLIDAYS[shamsiMonth to day]?.let { reasons.add(it) }
            getHijriHolidayReason(hijri.month, hijri.day, hMonthLen)?.let { hijriReason ->
                if (reasons.none { it == hijriReason }) {
                    reasons.add(hijriReason)
                }
            }

            val isOfficialHoliday = reasons.isNotEmpty()
            if (isOfficialHoliday) {
                holidayEntries.add(
                    HolidayReasonEntry(
                        shamsiDay = day,
                        shamsiMonth = shamsiMonth,
                        hijriLabel = hijri.formatDayMonthPersian(),
                        reason = reasons.joinToString(" / "),
                        isFriday = isFriday
                    )
                )
            }

            val (gy, gm, gd) = PersianDateHelper.jalaliToGregorian(shamsiYear, shamsiMonth, day)

            dayCells.add(
                CalendarDayCell(
                    shamsiYear = shamsiYear,
                    shamsiMonth = shamsiMonth,
                    shamsiDay = day,
                    gregorianYear = gy,
                    gregorianMonth = gm,
                    gregorianDay = gd,
                    hijriDate = hijri,
                    isToday = isToday,
                    isFriday = isFriday,
                    isOfficialHoliday = isOfficialHoliday,
                    holidayReasons = reasons
                )
            )
        }

        return MonthlyCalendarUiData(
            shamsiYear = shamsiYear,
            shamsiMonth = shamsiMonth,
            monthName = monthName,
            firstDayOfWeekOffset = firstDayOffset,
            days = dayCells,
            officialHolidayEntries = holidayEntries
        )
    }

    private fun shamsiDayIndex(jYear: Int, jMonth: Int, jDay: Int): Long {
        val (gy, gm, gd) = PersianDateHelper.jalaliToGregorian(jYear, jMonth, jDay)
        return gregorianToJulianDayNumber(gy, gm, gd)
    }

    private fun gregorianToJulianDayNumber(year: Int, month: Int, day: Int): Long {
        val a = (14 - month) / 12
        val y = year + 4800 - a
        val m = month + 12 * a - 3
        return day.toLong() + (153L * m + 2) / 5 + 365L * y + y / 4 - y / 100 + y / 400 - 32045L
    }

    /**
     * Fallback Kuwaiti / Tabular Hijri algorithm for dates outside the anchored Iranian official years.
     */
    private fun gregorianToHijriAlgorithmic(gYear: Int, gMonth: Int, gDay: Int): HijriDate {
        val jdn = gregorianToJulianDayNumber(gYear, gMonth, gDay)
        var l = jdn - 1948440L + 10632L
        val n = (l - 1L) / 10631L
        l = l - 10631L * n + 354L
        val j = ((10985L - l) / 5316L) * ((50L * l) / 17719L) +
            (l / 5670L) * ((43L * l) / 15238L)
        l = l - ((30L - j) / 15L) * ((17719L * j) / 50L) -
            (j / 16L) * ((15238L * j) / 43L) + 29L
        val m = ((24L * l) / 709L).toInt()
        val d = (l - (709L * m) / 24L).toInt()
        val y = (30L * n + j - 30L).toInt()
        val clampedMonth = m.coerceIn(1, 12)
        val mName = HIJRI_MONTH_NAMES.getOrElse(clampedMonth - 1) { "" }
        return HijriDate(y, clampedMonth, d, mName)
    }
}
