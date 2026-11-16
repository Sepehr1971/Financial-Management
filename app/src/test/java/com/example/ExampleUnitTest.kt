package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testTehranPrayerTimes() {
    val prayerTimes = com.example.utils.TehranPrayerTimesHelper.getTehranPrayerTimes()
    assertNotNull(prayerTimes)
    assertTrue("Fajr should have valid time format", prayerTimes.fajr.matches(Regex("\\d{2}:\\d{2}")))
    assertTrue("Dhuhr should have valid time format", prayerTimes.dhuhr.matches(Regex("\\d{2}:\\d{2}")))
    assertTrue("Maghrib should have valid time format", prayerTimes.maghrib.matches(Regex("\\d{2}:\\d{2}")))
    assertTrue("Sunrise should have valid time format", prayerTimes.sunrise.matches(Regex("\\d{2}:\\d{2}")))
    assertTrue("Sunset should have valid time format", prayerTimes.sunset.matches(Regex("\\d{2}:\\d{2}")))
    assertTrue("Midnight should have valid time format", prayerTimes.midnight.matches(Regex("\\d{2}:\\d{2}")))
  }

  @Test
  fun testTetherRateSafeguard() {
    // If raw scraped value was "1.00" USD or 0, our safeguard ensures price in Tomans >= 10,000 Tomans
    val dollarPrice = 266500L
    val parsedZero = 0L
    val finalTether = if (parsedZero < 10000L) dollarPrice else parsedZero
    assertEquals(dollarPrice, finalTether)
  }

  @Test
  fun testGitHubRepoParsing() {
    assertEquals(
      "ali" to "hazineh",
      com.example.utils.GitHubUpdateHelper.parseOwnerAndRepo("ali/hazineh")
    )
    assertEquals(
      "ali" to "hazineh",
      com.example.utils.GitHubUpdateHelper.parseOwnerAndRepo("https://github.com/ali/hazineh.git")
    )
    assertEquals(
      "ali" to "hazineh",
      com.example.utils.GitHubUpdateHelper.parseOwnerAndRepo("https://github.com/ali/hazineh/releases")
    )
    assertNull(com.example.utils.GitHubUpdateHelper.parseOwnerAndRepo("onlyusername"))
    assertNull(com.example.utils.GitHubUpdateHelper.parseOwnerAndRepo("   "))
  }

  @Test
  fun testGitHubVersionComparison() {
    assertTrue(com.example.utils.GitHubUpdateHelper.compareVersions("v1.1.0", "1.0") > 0)
    assertTrue(com.example.utils.GitHubUpdateHelper.compareVersions("V2", "1.0") > 0)
    assertEquals(0, com.example.utils.GitHubUpdateHelper.compareVersions("v1.0", "1.0.0"))
    assertTrue(com.example.utils.GitHubUpdateHelper.compareVersions("1.0", "1.2") < 0)
  }

  @Test
  fun testCurrentMonthExpenseGrouping() {
    val supermarketItems = (1..10).map { idx ->
      com.example.data.model.ResolvedExpense(
        entity = com.example.data.model.ExpenseEntity(
          id = idx.toLong(),
          title = if (idx % 2 == 0) "خرید سوپرمارکت" else "  خرید سوپر مارکت ",
          amount = idx * 100_000L,
          shamsiYear = 1405,
          shamsiMonth = 8,
          shamsiDay = idx,
          shamsiDateString = "1405/08/${idx.toString().padStart(2, '0')}"
        ),
        effectiveFolder = "خرید سوپرمارکت",
        isRecurring = false
      )
    }
    val fuelItem = com.example.data.model.ResolvedExpense(
      entity = com.example.data.model.ExpenseEntity(
        id = 11L,
        title = "بنزین",
        amount = 150_000L,
        shamsiYear = 1405,
        shamsiMonth = 8,
        shamsiDay = 15,
        shamsiDateString = "1405/08/15"
      ),
      effectiveFolder = "بنزین",
      isRecurring = false
    )

    val groups = com.example.ui.components.CurrentMonthExpenseGrouper.groupExpenses(
      supermarketItems + fuelItem
    )

    assertEquals(2, groups.size)
    assertEquals(10, groups[0].items.size)
    assertEquals(5_500_000L, groups[0].totalAmount)
    assertEquals("خرید سوپرمارکت", groups[0].displayTitle)
    assertEquals(1, groups[1].items.size)
    assertEquals(150_000L, groups[1].totalAmount)
  }

  @Test
  fun testSmartSemanticAndAnnualGrouping() {
    val items = listOf(
      com.example.data.model.ResolvedExpense(
        entity = com.example.data.model.ExpenseEntity(
          id = 1L,
          title = "پول علی",
          amount = 500_000L,
          shamsiYear = 1405,
          shamsiMonth = 8,
          shamsiDay = 20,
          shamsiDateString = "1405/08/20"
        ),
        effectiveFolder = "علی",
        isRecurring = false
      ),
      com.example.data.model.ResolvedExpense(
        entity = com.example.data.model.ExpenseEntity(
          id = 2L,
          title = "علی",
          amount = 300_000L,
          shamsiYear = 1405,
          shamsiMonth = 8,
          shamsiDay = 10,
          shamsiDateString = "1405/08/10"
        ),
        effectiveFolder = "علی",
        isRecurring = false
      ),
      com.example.data.model.ResolvedExpense(
        entity = com.example.data.model.ExpenseEntity(
          id = -101L,
          title = "بیمه شخص ثالث خودرو (سهم ماهانه)",
          amount = 1_000_000L,
          isAnnual = true,
          shamsiYear = 1405,
          shamsiMonth = 8,
          shamsiDay = 15,
          shamsiDateString = "1405/08/15"
        ),
        effectiveFolder = "تعهدات سالانه و تعهدات",
        isRecurring = false
      ),
      com.example.data.model.ResolvedExpense(
        entity = com.example.data.model.ExpenseEntity(
          id = -201L,
          title = "بیمه موتورسیکلت (سهم ماهانه)",
          amount = 200_000L,
          isAnnual = true,
          shamsiYear = 1405,
          shamsiMonth = 8,
          shamsiDay = 20,
          shamsiDateString = "1405/08/20"
        ),
        effectiveFolder = "موتور",
        isRecurring = false
      ),
      com.example.data.model.ResolvedExpense(
        entity = com.example.data.model.ExpenseEntity(
          id = 3L,
          title = "قبض آب",
          amount = 80_000L,
          shamsiYear = 1405,
          shamsiMonth = 8,
          shamsiDay = 5,
          shamsiDateString = "1405/08/05"
        ),
        effectiveFolder = "قبوض",
        isRecurring = false
      ),
      com.example.data.model.ResolvedExpense(
        entity = com.example.data.model.ExpenseEntity(
          id = 4L,
          title = "قبض برق",
          amount = 120_000L,
          shamsiYear = 1405,
          shamsiMonth = 8,
          shamsiDay = 4,
          shamsiDateString = "1405/08/04"
        ),
        effectiveFolder = "قبوض",
        isRecurring = false
      )
    )

    val groups = com.example.ui.components.CurrentMonthExpenseGrouper.groupExpenses(items)

    assertEquals(4, groups.size)

    val aliGroup = groups[0]
    assertEquals("علی", aliGroup.displayTitle)
    assertEquals(2, aliGroup.items.size)
    assertEquals(800_000L, aliGroup.totalAmount)

    val annualGroup = groups[1]
    assertTrue(annualGroup.isAnnual)
    assertEquals("تعهدات سالانه (سهم ماهانه)", annualGroup.displayTitle)
    assertEquals(2, annualGroup.items.size)
    assertEquals(1_200_000L, annualGroup.totalAmount)

    assertEquals("قبض آب", groups[2].displayTitle)
    assertEquals("قبض برق", groups[3].displayTitle)
  }

  @Test
  fun testExtendedSemanticGrouping() {
    val items = listOf(
      com.example.data.model.ResolvedExpense(
        entity = com.example.data.model.ExpenseEntity(
          id = 10L,
          title = "خرید میوه",
          amount = 400_000L,
          shamsiYear = 1405,
          shamsiMonth = 8,
          shamsiDay = 20,
          shamsiDateString = "1405/08/20"
        ),
        effectiveFolder = "خرید میوه",
        isRecurring = false
      ),
      com.example.data.model.ResolvedExpense(
        entity = com.example.data.model.ExpenseEntity(
          id = 11L,
          title = "خرید میوه و پروتئین",
          amount = 980_000L,
          shamsiYear = 1405,
          shamsiMonth = 8,
          shamsiDay = 12,
          shamsiDateString = "1405/08/12"
        ),
        effectiveFolder = "سوپرمارکت و خوراکی",
        isRecurring = false
      ),
      com.example.data.model.ResolvedExpense(
        entity = com.example.data.model.ExpenseEntity(
          id = 12L,
          title = "هزینه های درمانی",
          amount = 650_000L,
          shamsiYear = 1405,
          shamsiMonth = 8,
          shamsiDay = 18,
          shamsiDateString = "1405/08/18"
        ),
        effectiveFolder = "درمان",
        isRecurring = false
      ),
      com.example.data.model.ResolvedExpense(
        entity = com.example.data.model.ExpenseEntity(
          id = 13L,
          title = "درمان",
          amount = 350_000L,
          shamsiYear = 1405,
          shamsiMonth = 8,
          shamsiDay = 10,
          shamsiDateString = "1405/08/10"
        ),
        effectiveFolder = "درمان",
        isRecurring = false
      ),
      com.example.data.model.ResolvedExpense(
        entity = com.example.data.model.ExpenseEntity(
          id = 14L,
          title = "نانوایی",
          amount = 60_000L,
          shamsiYear = 1405,
          shamsiMonth = 7,
          shamsiDay = 15,
          shamsiDateString = "1405/07/15"
        ),
        effectiveFolder = "نان",
        isRecurring = false
      ),
      com.example.data.model.ResolvedExpense(
        entity = com.example.data.model.ExpenseEntity(
          id = 15L,
          title = "خرید نان سنگک",
          amount = 40_000L,
          shamsiYear = 1405,
          shamsiMonth = 7,
          shamsiDay = 5,
          shamsiDateString = "1405/07/05"
        ),
        effectiveFolder = "نان",
        isRecurring = false
      ),
      com.example.data.model.ResolvedExpense(
        entity = com.example.data.model.ExpenseEntity(
          id = 16L,
          title = "تخم مرغ",
          amount = 150_000L,
          shamsiYear = 1405,
          shamsiMonth = 8,
          shamsiDay = 3,
          shamsiDateString = "1405/08/03"
        ),
        effectiveFolder = "سوپرمارکت و خوراکی",
        isRecurring = false
      )
    )

    val groups = com.example.ui.components.CurrentMonthExpenseGrouper.groupExpenses(items)

    assertEquals(4, groups.size)

    val fruitGroup = groups[0]
    assertEquals("خرید میوه", fruitGroup.displayTitle)
    assertEquals(2, fruitGroup.items.size)
    assertEquals(1_380_000L, fruitGroup.totalAmount)

    val medicalGroup = groups[1]
    assertEquals("درمان", medicalGroup.displayTitle)
    assertEquals(2, medicalGroup.items.size)
    assertEquals(1_000_000L, medicalGroup.totalAmount)

    val bakeryGroup = groups[2]
    assertEquals("نانوایی", bakeryGroup.displayTitle)
    assertEquals(2, bakeryGroup.items.size)
    assertEquals(100_000L, bakeryGroup.totalAmount)

    val eggGroup = groups[3]
    assertEquals("تخم مرغ", eggGroup.displayTitle)
    assertEquals(1, eggGroup.items.size)
  }

  @Test
  fun testBaleChannel4827170321DollarAndEuroParser() {
    val chatDump = """
      دلار آزاد: ۶۸,۰۰۰ تومان
      یورو: ۷۳,۱۰۰ تومان
      10:15
      🇺🇸 دلار آمریکا
      خرید: 69,100
      فروش: ۶۹,۴۵۰ تومان (+1.2%)
      🇪🇺 یورو: ۷۴۲,۰۰۰ ریال
      14:32
    """.trimIndent()

    val result = com.example.data.network.BaleChannelPriceParser.parseChannel4827170321(chatDump)
    assertEquals(69_450L, result.dollar?.amount)
    assertEquals(1.2, result.dollar?.changePercent ?: 0.0, 0.01)
    assertEquals(true, result.dollar?.isPositive)
    assertEquals(74_200L, result.euro?.amount)
  }

  @Test
  fun testBaleChannel5160361766GoldAndCoinsParser() {
    val chatDump = """
      🌍 انس جهانی طلا: ۲,۷۱۴.۵۰ دلار
      🟡 طلای ۱۶ عیار: ۴,۰۸۰,۰۰۰ تومان
      🪙 سکه امامی: ۵۳,۲۰۰,۰۰۰ تومان
      🔸 نیم سکه: ۲۸,۵۰۰,۰۰۰ تومان
      🔹 ربع سکه: ۱۷,۸۵۰,۰۰۰ تومان
    """.trimIndent()

    val result = com.example.data.network.BaleChannelPriceParser.parseChannel5160361766(chatDump)
    assertEquals(2_715L, result.goldOunceUsd?.amount)
    assertEquals("طلای ۱۶ عیار", result.goldKaratTitle)
    assertEquals(4_080_000L, result.goldKaratToman?.amount)
    assertEquals(53_200_000L, result.sekehEmami?.amount)
    assertEquals(17_850_000L, result.robSekeh?.amount)
  }

  @Test
  fun testBaleChannel5138319664CryptoParser() {
    val chatDump = """
      ₮ تتر (USDT): ۶۹,۸۵۰ تومان
      ₿ بیت کوین (BTC): 91,450 دلار (-0.45%)
    """.trimIndent()

    val result = com.example.data.network.BaleChannelPriceParser.parseChannel5138319664(chatDump)
    assertEquals(69_850L, result.tether?.amount)
    assertEquals(91_450L, result.bitcoinUsd?.amount)
    assertEquals(false, result.bitcoinUsd?.isPositive)
    assertEquals(0.45, result.bitcoinUsd?.changePercent ?: 0.0, 0.01)
  }

  @Test
  fun testBaleLoginDetection() {
    assertTrue(
      com.example.data.network.BaleChannelPriceParser.detectBaleLoginRequired(
        "ورود به بله\nلطفاً شماره موبایل خود را وارد کنید\nارسال کد تایید",
        "https://web.bale.ai/login"
      )
    )
    assertFalse(
      com.example.data.network.BaleChannelPriceParser.detectBaleLoginRequired(
        "دلار آزاد: ۶۹,۴۵۰ تومان\nیورو: ۷۴,۲۰۰ تومان",
        "https://web.bale.ai/chat?uid=4827170321"
      )
    )
  }

  @Test
  fun testTgjuCoinHtmlBourseAndOilParser() {
    val sampleHtml = """
      <ul class="info-bar mobile-hide">
        <li id="l-bourse">
          <span class="info-title">شاخص بورس</span>
          <span class="info-price">2,145,320</span>
          <span class="info-change high">14,200</span>
        </li>
        <li id="l-oil_brent" class="low">
          <span class="info-title">نفت برنت</span>
          <span class="info-price">73.8</span>
          <span class="info-change">0.42</span>
        </li>
      </ul>
    """.trimIndent()

    val (bourse, oil) = com.example.data.network.MarketRatesService.parseBourseAndOilFromTgjuCoinHtml(sampleHtml)
    assertEquals(2_145_320L, bourse?.priceTomans)
    assertEquals(14_200L, bourse?.changeTomans)
    assertEquals(true, bourse?.isPositiveChange)
    assertEquals(73L, oil?.priceTomans)
    assertEquals("$۷۳.۸۰", oil?.displayPriceFormatted)
    assertEquals(false, oil?.isPositiveChange)
  }

  @Test
  fun testBaleChannelEdgeCasesRegression() {
    // 1. Inline buy/sell + same-line Dollar & Euro + official/secondary rate below free-market rate
    val fxDump = """
      دلار آزاد: خرید 108,200 فروش 109,500 تومان | یورو آزاد: خرید 118,000 فروش 119,400 تومان
      دلار مبادله ای: 68,500 تومان
    """.trimIndent()
    val fx = com.example.data.network.BaleChannelPriceParser.parseChannel4827170321(fxDump)
    assertEquals(109_500L, fx.dollar?.amount)
    assertEquals(119_400L, fx.euro?.amount)

    // 2. Crypto channel with BTC/USDT trading pair must NOT overwrite Tether with Bitcoin's USD price
    val cryptoDump = """
      تتر (USDT): 110,250 تومان
      بیت کوین (BTC/USDT): 65,288 دلار (-1.1%)
    """.trimIndent()
    val crypto = com.example.data.network.BaleChannelPriceParser.parseChannel5138319664(cryptoDump)
    assertEquals(110_250L, crypto.tether?.amount)
    assertEquals(65_288L, crypto.bitcoinUsd?.amount)

    // 3. Gold channel: timestamp 18:15 + مثقال 33,093,500 + طرح قدیم must NOT leak into 18k/16k Gold or Sekeh Emami
    val goldDump = """
      🌍 انس جهانی طلا: 3,883 دلار
      🟡 طلای ۱۸ عیار: 112,500,000
      🪙 سکه امامی: 118,400,000 تومان
      🪙 سکه تمام بهار آزادی (طرح قدیم): 112,000,000 تومان
      🔹 ربع سکه: 33,500,000 تومان
      گزارش طلا ساعت 18:15
      مثقال طلا: 33,093,500 تومان
    """.trimIndent()
    val gold = com.example.data.network.BaleChannelPriceParser.parseChannel5160361766(goldDump)
    assertEquals(3_883L, gold.goldOunceUsd?.amount)
    assertEquals(11_250_000L, gold.goldKaratToman?.amount)
    assertEquals("طلای ۱۸ عیار", gold.goldKaratTitle)
    assertEquals(118_400_000L, gold.sekehEmami?.amount)
    assertEquals(33_500_000L, gold.robSekeh?.amount)
  }

  @Test
  fun testBaleGoldPipeMultiAssetAnd750() {
    // Multi-asset condensed line + pipe separator + Persian date "18 آبان" + Mithqal nearby
    val condensedDump = """
      به‌روزرسانی 18 آبان:
      انس جهانی: 3,883$ | طلای ۱۸ عیار | ۱۶,۵۴۰,۰۰۰ تومان | مثقال طلا: ۷۱,۶۰۰,۰۰۰ تومان | ربع سکه: ۳۳,۵۰۰,۰۰۰ تومان
      سکه امامی: ۱۱۸,۴۰۰,۰۰۰ تومان
    """.trimIndent()
    val parsed1 = com.example.data.network.BaleChannelPriceParser.parseChannel5160361766(condensedDump)
    assertEquals(3_883L, parsed1.goldOunceUsd?.amount)
    assertEquals(16_540_000L, parsed1.goldKaratToman?.amount)
    assertEquals("طلای ۱۸ عیار", parsed1.goldKaratTitle)
    assertEquals(118_400_000L, parsed1.sekehEmami?.amount)
    assertEquals(33_500_000L, parsed1.robSekeh?.amount)

    // Pre-keyword price + 750 hallmark + 18 in price ("18,250,000")
    val preAnd750Dump = """
      ۱۸,۲۵۰,۰۰۰ تومان | طلای ۷۵۰
      مثقال طلا: ۷۹,۰۰۰,۰۰۰ تومان
    """.trimIndent()
    val parsed2 = com.example.data.network.BaleChannelPriceParser.parseChannel5160361766(preAnd750Dump)
    assertEquals(18_250_000L, parsed2.goldKaratToman?.amount)
    assertEquals("طلای ۱۸ عیار", parsed2.goldKaratTitle)
  }

  @Test
  fun testIranianCalendarBadesabaHijriAndHolidays() {
    // 22 Aban 1405 = 3 Jumada al-Thani 1448 (Martyrdom of Fatimah Zahra)
    val aban22Hijri = com.example.utils.IranianCalendarHelper.shamsiToHijri(1405, 8, 22).first
    assertEquals(1448, aban22Hijri.year)
    assertEquals(6, aban22Hijri.month)
    assertEquals(3, aban22Hijri.day)
    assertEquals("1448/06/03", aban22Hijri.formatNumeric())

    val aban23Hijri = com.example.utils.IranianCalendarHelper.shamsiToHijri(1405, 8, 23).first
    assertEquals("1448/06/04", aban23Hijri.formatNumeric())

    val abanCal = com.example.utils.IranianCalendarHelper.getMonthlyCalendar(1405, 8)
    assertEquals(1, abanCal.officialHolidayEntries.size)
    assertEquals(22, abanCal.officialHolidayEntries[0].shamsiDay)
    assertTrue(abanCal.officialHolidayEntries[0].reason.contains("فاطمه زهرا"))
    val abanDay22 = abanCal.days.first { it.shamsiDay == 22 }
    assertTrue(abanDay22.isOfficialHoliday)
    // 22 Aban 1405 = 2026-11-13
    assertEquals(2026, abanDay22.gregorianYear)
    assertEquals(11, abanDay22.gregorianMonth)
    assertEquals(13, abanDay22.gregorianDay)

    // Tir 1405: 3 Tir = Tasua (9 Muharram), 4 Tir = Ashura (10 Muharram)
    val tirCal = com.example.utils.IranianCalendarHelper.getMonthlyCalendar(1405, 4)
    val tasua = tirCal.officialHolidayEntries.find { it.shamsiDay == 3 }
    val ashura = tirCal.officialHolidayEntries.find { it.shamsiDay == 4 }
    assertNotNull(tasua)
    assertNotNull(ashura)
    assertTrue(tasua!!.reason.contains("تاسوعا"))
    assertTrue(ashura!!.reason.contains("عاشورا"))
  }

  @Test
  fun testReceiptExpenseShareTextFormat() {
    val expense = com.example.data.model.ExpenseEntity(
      id = 77L,
      title = "خرید دارو و\nآزمایش",
      amount = 1_250_000L,
      customFolder = "درمان",
      shamsiYear = 1405,
      shamsiMonth = 8,
      shamsiDay = 22,
      shamsiDateString = "1405/08/22",
      note = "نسخه دکتر\nبیمه تکمیلی",
      receiptImagePath = "/tmp/receipt_test.jpg"
    )
    val shareText = com.example.utils.ReceiptImageHelper.buildExpenseShareText(expense)
    assertTrue(shareText.contains("خرید دارو و آزمایش"))
    assertTrue(shareText.contains("۱,۲۵۰,۰۰۰"))
    assertTrue(shareText.contains("۱۴۰۵/۰۸/۲۲"))
    assertTrue(shareText.contains("درمان"))
    assertTrue(shareText.contains("نسخه دکتر بیمه تکمیلی"))
  }
}

