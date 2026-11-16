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
    val displayDiffFormatted: String? = null,
    val sourceLabel: String = ""
)

data class MarketRatesState(
    val dollar: MarketRateItem = MarketRateItem(
        key = "dollar",
        title = "دلار",
        priceTomans = 0L,
        changePercent = 0.0,
        changeTomans = 0L,
        isPositiveChange = true,
        timeFormatted = "",
        sourceLabel = "بله"
    ),
    val euro: MarketRateItem = MarketRateItem(
        key = "euro",
        title = "یورو",
        priceTomans = 0L,
        changePercent = 0.0,
        changeTomans = 0L,
        isPositiveChange = true,
        timeFormatted = "",
        sourceLabel = "بله"
    ),
    val sekehEmami: MarketRateItem = MarketRateItem(
        key = "retail_sekee",
        title = "سکه امامی",
        priceTomans = 0L,
        changePercent = 0.0,
        changeTomans = 0L,
        isPositiveChange = true,
        timeFormatted = "",
        sourceLabel = "بله"
    ),
    val robSekeh: MarketRateItem = MarketRateItem(
        key = "retail_rob",
        title = "ربع سکه",
        priceTomans = 0L,
        changePercent = 0.0,
        changeTomans = 0L,
        isPositiveChange = true,
        timeFormatted = "",
        sourceLabel = "بله"
    ),
    val goldOunce: MarketRateItem = MarketRateItem(
        key = "ons",
        title = "انس جهانی طلا",
        priceTomans = 0L,
        changePercent = 0.0,
        changeTomans = 0L,
        isPositiveChange = true,
        timeFormatted = "",
        unitLabel = "دلار",
        displayPriceFormatted = null,
        displayDiffFormatted = null,
        sourceLabel = "بله"
    ),
    val gold18k: MarketRateItem = MarketRateItem(
        key = "geram16",
        title = "طلای 16 عیار",
        priceTomans = 0L,
        changePercent = 0.0,
        changeTomans = 0L,
        isPositiveChange = true,
        timeFormatted = "",
        sourceLabel = "بله"
    ),
    val bitcoin: MarketRateItem = MarketRateItem(
        key = "crypto-bitcoin",
        title = "بیت‌کوین",
        priceTomans = 0L,
        changePercent = 0.0,
        changeTomans = 0L,
        isPositiveChange = true,
        timeFormatted = "",
        unitLabel = "دلار",
        displayPriceFormatted = null,
        displayDiffFormatted = null,
        sourceLabel = "بله"
    ),
    val tether: MarketRateItem = MarketRateItem(
        key = "crypto-tether",
        title = "تتر (USDT)",
        priceTomans = 0L,
        changePercent = 0.0,
        changeTomans = 0L,
        isPositiveChange = true,
        timeFormatted = "",
        unitLabel = "تومان",
        sourceLabel = "بله"
    ),
    val crudeOil: MarketRateItem = MarketRateItem(
        key = "oil_brent",
        title = "نفت برنت",
        priceTomans = 0L,
        changePercent = 0.0,
        changeTomans = 0L,
        isPositiveChange = true,
        timeFormatted = "",
        unitLabel = "دلار",
        displayPriceFormatted = null,
        displayDiffFormatted = null,
        sourceLabel = "TGJU"
    ),
    val bourse: MarketRateItem = MarketRateItem(
        key = "bourse",
        title = "شاخص کل بورس",
        priceTomans = 0L,
        changePercent = 0.0,
        changeTomans = 0L,
        isPositiveChange = true,
        timeFormatted = "",
        unitLabel = "واحد",
        displayPriceFormatted = null,
        displayDiffFormatted = null,
        sourceLabel = "TGJU"
    ),
    val lastUpdateTime: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = true,
    val errorMessage: String? = null,
    val baleNeedsLogin: Boolean = false,
    val baleStatusMessage: String? = null
)
