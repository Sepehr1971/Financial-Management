package com.example

import com.example.utils.PersianDateHelper
import com.example.utils.PersianVoiceExpenseParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class PersianVoiceExpenseParserTest {

    // Fixed reference timestamp: 2026-10-25 12:00:00 local
    private fun referenceMillis(): Long {
        return Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 25, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun parsesSpokenWordsWithThousandsAndToman() {
        val result = PersianVoiceExpenseParser.parse(
            "دویست و پنجاه هزار تومان خرید نان و پنیر",
            referenceMillis()
        )
        assertEquals(250_000L, result.amountTomans)
        assertEquals("خرید نان و پنیر", result.title)
        assertFalse(result.isAnnual)
    }

    @Test
    fun parsesHalfMillionAndYesterdayDate() {
        val ref = referenceMillis()
        val calYesterday = Calendar.getInstance().apply {
            timeInMillis = ref
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val expectedYesterday = PersianDateHelper.getShamsiDate(calYesterday.timeInMillis).format()

        val result = PersianVoiceExpenseParser.parse(
            "سه و نیم میلیون اجاره مسکن دیروز",
            ref
        )
        assertEquals(3_500_000L, result.amountTomans)
        assertEquals("اجاره مسکن", result.title)
        assertEquals(expectedYesterday, result.shamsiDateString)
    }

    @Test
    fun parsesPersianDigitsWithColloquialToman() {
        val result = PersianVoiceExpenseParser.parse(
            "۵۰۰ هزار تومن بنزین",
            referenceMillis()
        )
        assertEquals(500_000L, result.amountTomans)
        assertEquals("بنزین", result.title)
    }

    @Test
    fun parsesAnnualInsuranceExpense() {
        val result = PersianVoiceExpenseParser.parse(
            "دو میلیون و پانصد هزار تومان بیمه ماشین",
            referenceMillis()
        )
        assertEquals(2_500_000L, result.amountTomans)
        assertEquals("بیمه ماشین", result.title)
        assertTrue(result.isAnnual)
    }

    @Test
    fun convertsRialsToTomans() {
        val result = PersianVoiceExpenseParser.parse(
            "یک میلیون و دویست هزار ریال قبض برق",
            referenceMillis()
        )
        assertEquals(120_000L, result.amountTomans)
        assertEquals("قبض برق", result.title)
    }

    @Test
    fun prioritizesCurrencySpanOverQuantityNumber() {
        val result = PersianVoiceExpenseParser.parse(
            "۲ کیلو پرتقال ۱۸۰ هزار تومان",
            referenceMillis()
        )
        assertEquals(180_000L, result.amountTomans)
        assertEquals("2 کیلو پرتقال", result.title)
    }

    @Test
    fun parsesDecimalMillionsAndTwoDaysAgo() {
        val ref = referenceMillis()
        val calTwoDaysAgo = Calendar.getInstance().apply {
            timeInMillis = ref
            add(Calendar.DAY_OF_YEAR, -2)
        }
        val expectedDate = PersianDateHelper.getShamsiDate(calTwoDaysAgo.timeInMillis).format()

        val result = PersianVoiceExpenseParser.parse(
            "۲/۵ میلیون تعمیر موتور پریروز",
            ref
        )
        assertEquals(2_500_000L, result.amountTomans)
        assertEquals("تعمیر موتور", result.title)
        assertEquals(expectedDate, result.shamsiDateString)
    }

    @Test
    fun stripsFillerWordsAroundTitle() {
        val result = PersianVoiceExpenseParser.parse(
            "بابت خرید میوه ۸۵ هزار تومان پرداخت کردم",
            referenceMillis()
        )
        assertEquals(85_000L, result.amountTomans)
        assertEquals("خرید میوه", result.title)
    }
}
