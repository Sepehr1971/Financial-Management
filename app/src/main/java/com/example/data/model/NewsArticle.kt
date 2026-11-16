package com.example.data.model

enum class NewsSource(val displayName: String, val websiteUrl: String, val colorHex: Long) {
    TABNAK("تابناک", "https://www.tabnak.ir", 0xFFDC2626),
    ZOOMIT("زومیت", "https://www.zoomit.ir", 0xFF2563EB),
    FARARU("فرارو", "https://fararu.com", 0xFFEA580C),
    KHABARONLINE("خبرآنلاین", "https://www.khabaronline.ir", 0xFF0284C7),
    EGHTESADONLINE("اقتصاد آنلاین", "https://www.eghtesadonline.com", 0xFF059669),
    AFTAB("آفتاب نیوز", "https://aftabnews.ir", 0xFFD97706),
    SAHAM("سهام نیوز", "https://sahamnews.org", 0xFF7C3AED)
}

enum class NewsCategory(val displayName: String) {
    ALL("همه اخبار"),
    ECONOMY("اقتصادی"),
    POLITICS("سیاسی"),
    TECHNOLOGY("تکنولوژی"),
    SCIENCE("علمی"),
    CULTURAL("فرهنگی"),
    SOCIAL("اجتماعی"),
    HEALTH("سلامت")
}

data class NewsArticle(
    val id: String,
    val title: String,
    val summary: String,
    val fullContent: String = "",
    val source: NewsSource,
    val category: NewsCategory,
    val url: String,
    val fetchTime: String,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUrl: String? = null,
    val videoUrl: String? = null
)
