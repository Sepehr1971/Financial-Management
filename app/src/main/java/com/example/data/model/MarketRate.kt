package com.example.data.model

data class MarketRateItem(
    val key: String,
    val title: String,
    val priceTomans: Long,
    val changePercent: Double,
    val changeTomans: Long = 0L,
    val isPositiveChange: Boolean,
    val timeFormatted: String,
    val unitLabel: String = "تومان",
    val displayPriceFormatted: String? = null,
    val displayDiffFormatted: String? = null
)

data class MarketRatesState(
    val dollar: MarketRateItem = MarketRateItem(
        key = "dollar",
        title = "دلار",
        priceTomans = 266400L,
        changePercent = 0.35,
        changeTomans = 950L,
        isPositiveChange = true,
        timeFormatted = "به‌روزرسانی روز"
    ),
    val euro: MarketRateItem = MarketRateItem(
        key = "euro",
        title = "یورو",
        priceTomans = 289500L,
        changePercent = 0.25,
        changeTomans = 720L,
        isPositiveChange = true,
        timeFormatted = "به‌روزرسانی روز"
    ),
    val sekehEmami: MarketRateItem = MarketRateItem(
        key = "retail_sekee",
        title = "سکه امامی (تک‌فروشی)",
        priceTomans = 257400000L,
        changePercent = 0.30,
        changeTomans = 750000L,
        isPositiveChange = false,
        timeFormatted = "به‌روزرسانی روز"
    ),
    val robSekeh: MarketRateItem = MarketRateItem(
        key = "retail_rob",
        title = "ربع سکه (تک‌فروشی)",
        priceTomans = 71900000L,
        changePercent = 0.40,
        changeTomans = 300000L,
        isPositiveChange = false,
        timeFormatted = "به‌روزرسانی روز"
    ),
    val goldOunce: MarketRateItem = MarketRateItem(
        key = "ons",
        title = "انس جهانی طلا",
        priceTomans = 4069L,
        changePercent = 0.30,
        changeTomans = 12L,
        isPositiveChange = true,
        timeFormatted = "به‌روزرسانی روز",
        unitLabel = "دلار",
        displayPriceFormatted = "$۴,۰۶۹",
        displayDiffFormatted = "$۱۲"
    ),
    val gold18k: MarketRateItem = MarketRateItem(
        key = "geram18",
        title = "طلای ۱۸ عیار",
        priceTomans = 25252800L,
        changePercent = 0.45,
        changeTomans = 115000L,
        isPositiveChange = true,
        timeFormatted = "به‌روزرسانی روز"
    ),
    val bitcoin: MarketRateItem = MarketRateItem(
        key = "crypto-bitcoin",
        title = "بیت‌کوین",
        priceTomans = 75800L,
        changePercent = 0.40,
        changeTomans = 300L,
        isPositiveChange = true,
        timeFormatted = "به‌روزرسانی روز",
        unitLabel = "دلار",
        displayPriceFormatted = "$۷۵,۸۰۰",
        displayDiffFormatted = "$۳۰۰"
    ),
    val crudeOil: MarketRateItem = MarketRateItem(
        key = "oil_brent",
        title = "نفت برنت",
        priceTomans = 98L,
        changePercent = 0.50,
        changeTomans = 1L,
        isPositiveChange = true,
        timeFormatted = "به‌روزرسانی روز",
        unitLabel = "دلار",
        displayPriceFormatted = "$۹۸.۰",
        displayDiffFormatted = "$۰.۵"
    ),
    val lastUpdateTime: String = "لحظه‌ای (TGJU)",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = true,
    val errorMessage: String? = null
)
