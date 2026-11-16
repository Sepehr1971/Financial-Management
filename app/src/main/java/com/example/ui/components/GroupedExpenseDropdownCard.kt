package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExpenseEntity
import com.example.data.model.ResolvedExpense
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.ScaledDialogContent
import com.example.ui.theme.appSecondaryTextColor
import com.example.ui.theme.appTextColor
import com.example.ui.theme.enDigits
import com.example.ui.theme.scaledDp
import com.example.utils.PersianDateHelper
import com.example.utils.ReceiptImageHelper

data class GroupedCurrentMonthExpense(
    val groupKey: String,
    val displayTitle: String,
    val effectiveFolder: String,
    val isAnnual: Boolean,
    val isRecurring: Boolean,
    val totalAmount: Long,
    val items: List<ResolvedExpense>,
    val latestTimestamp: Long,
    val receiptCount: Int
)

object CurrentMonthExpenseGrouper {

    private val leadingPrefixes = listOf(
        "هزینه های شخصی", "هزینه‌های شخصی",
        "کارت به کارت به", "کارت به کارت برای", "کارت به کارت",
        "هزینه های", "هزینه‌های", "هزینه ها", "هزینهٔ", "هزینه",
        "خرید های", "خریدهای", "خرید از", "خرید",
        "واریز به", "واریز برای", "واریز",
        "پرداخت به", "پرداخت بابت", "پرداخت برای", "پرداخت",
        "انتقال به", "انتقال برای", "انتقال",
        "کرایه ی", "کرایهٔ", "کرایه",
        "پولِ", "پول",
        "خرجِ", "خرج",
        "سهمِ", "سهم",
        "بابتِ", "بابت",
        "برایِ", "برای",
        "مبلغِ", "مبلغ",
        "قسطِ", "قسط"
    )

    private val trailingCurrencyNoise = listOf("تومان", "تومن", "ریال")

    private val bakeryExactWords = setOf(
        "نان", "نون", "نانوا", "نانوایی", "سنگک", "سنگکی",
        "بربری", "لواش", "تافتون", "باگت"
    )

    private val medicalExactWords = setOf(
        "درمان", "درمانی", "پزشک", "پزشکی", "دکتر", "دارو", "دارویی",
        "داروخانه", "بیمارستان", "کلینیک", "درمانگاه", "آزمایشگاه",
        "آزمایش", "ویزیت", "دندانپزشک", "دندانپزشکی", "سونوگرافی", "فیزیوتراپی"
    )

    private val fruitExactWords = setOf(
        "میوه", "میوهجات", "ترهبار", "سبزی", "سبزیجات"
    )

    private val supermarketExactWords = setOf(
        "سوپرمارکت", "سوپر", "سوپری", "خواربار", "هایپرمارکت", "هایپر"
    )

    private val fuelExactWords = setOf("بنزین", "سوخت", "گازوئیل")

    private val internetExactWords = setOf(
        "اینترنت", "وایفای", "مودم", "آسیاتک", "مبیننت", "شاتل"
    )

    private val transitExactWords = setOf(
        "اسنپ", "تپسی", "تاکسی", "آژانس", "مترو", "اتوبوس"
    )

    private val proteinExactWords = setOf(
        "مرغ", "گوشت", "ماهی", "پروتئین", "پروتین"
    )

    private val clothingExactWords = setOf(
        "پوشاک", "لباس", "شلوار", "پیراهن", "تیشرت", "کفش",
        "مانتو", "پالتو", "کاپشن", "جوراب"
    )

    private val restaurantExactWords = setOf(
        "رستوران", "کافه", "فستفود", "پیتزا", "ساندویچ", "ناهار", "شام"
    )

    fun normalizeSpacesAndChars(rawTitle: String): String {
        var s = rawTitle
            .replace(Regex("[\\u064B-\\u065F\\u0670]"), "")
            .replace('\u064A', '\u06CC') // Arabic Yeh -> Persian Yeh
            .replace('\u0643', '\u06A9') // Arabic Kaf -> Persian Kaf
            .replace('\u0629', '\u0647') // Teh Marbuta -> Heh
            .replace('\u0623', '\u0627') // Alef Hamza Above -> Alef
            .replace('\u0625', '\u0627') // Alef Hamza Below -> Alef
            .replace('\u200C', ' ')       // ZWNJ -> space
            .replace(Regex("[\\r\\n\\s]+"), " ")
            .trim()
        for (noise in trailingCurrencyNoise) {
            if (s.endsWith(" $noise")) {
                val trimmed = s.removeSuffix(" $noise").trim()
                if (trimmed.length >= 2) {
                    s = trimmed
                }
            }
        }
        return s
    }

    /**
     * Strips leading Persian expense prefixes ("پول", "خرید", "هزینه", "بابت", ...)
     * so "پول علی" and "علی" or "خرید سوپر مارکت" and "سوپرمارکت" map to the same core concept.
     */
    fun extractCoreTitle(rawTitle: String): String {
        var s = normalizeSpacesAndChars(rawTitle)
        var changed = true
        while (changed) {
            changed = false
            for (prefix in leadingPrefixes) {
                if (s.startsWith("$prefix ")) {
                    val remainder = s.substring(prefix.length).trim()
                    val nonSpaceCount = remainder.count { !it.isWhitespace() }
                    if (nonSpaceCount >= 2) {
                        s = remainder
                        changed = true
                        break
                    }
                }
            }
        }
        return s
    }

    private fun tokenize(text: String): List<String> {
        return normalizeSpacesAndChars(text)
            .split(Regex("[\\s\\u200c،,_\\-/]+"))
            .filter { it.isNotBlank() }
    }

    fun normalizeTitleKey(rawTitle: String): String {
        val core = extractCoreTitle(rawTitle)
        val tokens = tokenize(core).map { it.lowercase() }
        val compact = tokens.joinToString("")

        return when {
            // 1. Bakery ("نانوایی", "نان", "نون", "نان سنگک", ...) - safe against "دندان"
            tokens.any {
                it in bakeryExactWords || it.startsWith("نانوا") ||
                    (it.startsWith("نان") && it != "نانو")
            } -> "SEMANTIC::BAKERY"

            // 2. Medical ("هزینه های درمانی", "درمان", "پزشک", "دارو", ...)
            tokens.any {
                it in medicalExactWords || it.startsWith("دندانپزشک") ||
                    it.startsWith("داروخان") || it.startsWith("بیمارستان")
            } -> "SEMANTIC::MEDICAL"

            // 3. Fruit ("خرید میوه", "خرید میوه و پروتئین", "میوه و تره‌بار", ...)
            tokens.any { it in fruitExactWords } ||
                (tokens.contains("تره") && tokens.contains("بار")) ||
                compact.contains("میوه") -> "SEMANTIC::FRUIT"

            // 4. Supermarket ("خرید سوپرمارکت", "سوپر مارکت", "سوپری", ...)
            tokens.any { it in supermarketExactWords } ||
                compact.contains("سوپرمارکت") ||
                compact.contains("سوپرماركت") -> "SEMANTIC::SUPERMARKET"

            // 5. Fuel ("بنزین", "پمپ بنزین", "سوخت")
            tokens.any { it in fuelExactWords } -> "SEMANTIC::FUEL"

            // 6. Internet ("اینترنت", "بسته اینترنت همراه اول", "شارژ اینترنت شاتل", "مودم")
            tokens.any { it in internetExactWords } ||
                compact.contains("اینترنت") ||
                compact.contains("وایفای") ||
                compact.contains("مبیننت") -> "SEMANTIC::INTERNET"

            // 7. Taxi & Transit ("اسنپ", "تپسی", "تاکسی", "مترو", "حمل و نقل")
            tokens.any { it in transitExactWords } ||
                (tokens.contains("حمل") && tokens.contains("نقل")) -> "SEMANTIC::TRANSIT"

            // 8. Protein ("گوشت", "مرغ", "ماهی") - exclude "تخم مرغ"
            !compact.contains("تخممرغ") &&
                tokens.any { it in proteinExactWords } -> "SEMANTIC::PROTEIN"

            // 9. Clothing ("پوشاک", "لباس", "کفش", ...)
            tokens.any { it in clothingExactWords } -> "SEMANTIC::CLOTHING"

            // 10. Restaurant & Cafe ("رستوران", "کافه", "پیتزا", ...)
            tokens.any { it in restaurantExactWords } ||
                (tokens.contains("فست") && tokens.contains("فود")) -> "SEMANTIC::RESTAURANT"

            // 11. General normalized core title (e.g. "پول علی" + "علی" -> "علی")
            else -> compact
        }
    }

    private fun resolveDisplayTitle(
        groupKey: String,
        list: List<ResolvedExpense>
    ): String {
        if (list.any { it.entity.isAnnual }) {
            return "تعهدات سالانه (سهم ماهانه)"
        }
        val normalizedOriginals = list.map { normalizeSpacesAndChars(it.entity.title) }
        val compactOriginals = normalizedOriginals.map { it.replace(" ", "") }.distinct()
        if (compactOriginals.size == 1) {
            return normalizedOriginals.groupingBy { it }.eachCount()
                .entries.maxWithOrNull(
                    compareBy<Map.Entry<String, Int>> { it.value }.thenBy { -it.key.length }
                )?.key ?: normalizedOriginals.first()
        }

        val cores = normalizedOriginals.map { extractCoreTitle(it) }.filter { it.isNotBlank() }
        val allTokens = cores.flatMap { tokenize(it) }

        return when (groupKey) {
            "SEMANTIC::BAKERY" -> {
                if (allTokens.any { it.contains("نانوا") }) "نانوایی" else "نان"
            }
            "SEMANTIC::MEDICAL" -> {
                val hasTreatment = allTokens.any { it == "درمان" || it == "درمانی" }
                val hasMedicine = allTokens.any { it.startsWith("دارو") }
                val hasDoctor = allTokens.any { it == "پزشک" || it == "پزشکی" || it == "دکتر" || it == "ویزیت" }
                val hasOther = allTokens.any {
                    it !in setOf("درمان", "درمانی", "دارو", "دارویی", "داروخانه", "پزشک", "پزشکی", "دکتر", "ویزیت")
                }
                when {
                    hasTreatment && !hasMedicine && !hasDoctor && !hasOther -> "درمان"
                    hasMedicine && !hasTreatment && !hasDoctor && !hasOther -> "دارو"
                    hasDoctor && !hasTreatment && !hasMedicine && !hasOther -> "پزشک"
                    else -> "درمان و پزشکی"
                }
            }
            "SEMANTIC::FRUIT" -> {
                val allStartWithBuyFruit = normalizedOriginals.all { it.startsWith("خرید میوه") }
                if (allStartWithBuyFruit) "خرید میوه" else "میوه"
            }
            "SEMANTIC::SUPERMARKET" -> {
                if (normalizedOriginals.all { it.startsWith("خرید سوپر") }) "خرید سوپرمارکت" else "سوپرمارکت"
            }
            "SEMANTIC::FUEL" -> "بنزین"
            "SEMANTIC::INTERNET" -> "اینترنت"
            "SEMANTIC::TRANSIT" -> "حمل‌ونقل و تاکسی"
            "SEMANTIC::PROTEIN" -> {
                val distinctCores = cores.map { it.replace(" ", "") }.distinct()
                if (distinctCores.size == 1) cores.first() else "گوشت و مرغ"
            }
            "SEMANTIC::CLOTHING" -> "پوشاک"
            "SEMANTIC::RESTAURANT" -> "رستوران و غذا"
            else -> cores.minByOrNull { it.length } ?: normalizedOriginals.first()
        }
    }

    /**
     * Groups sorted expenses intelligently:
     * - All annual commitments (`isAnnual == true`) are combined into one unified dropdown group.
     * - Regular expenses sharing the same semantic core or category (e.g. "پول علی" and "علی",
     *   "خرید میوه" and "خرید میوه و پروتئین", "هزینه های درمانی" and "درمان", multiple "نانوایی")
     *   are merged and summed under a unified title.
     * Preserves the relative order of first appearance (latest group first).
     */
    fun groupExpenses(sortedExpenses: List<ResolvedExpense>): List<GroupedCurrentMonthExpense> {
        if (sortedExpenses.isEmpty()) return emptyList()
        val grouped = LinkedHashMap<String, MutableList<ResolvedExpense>>()
        for (item in sortedExpenses) {
            val key = if (item.entity.isAnnual) {
                "annual::ALL_ANNUAL_COMMITMENTS"
            } else {
                normalizeTitleKey(item.entity.title).let { normalized ->
                    if (normalized.startsWith("SEMANTIC::")) normalized else "regular::$normalized"
                }
            }
            grouped.getOrPut(key) { mutableListOf() }.add(item)
        }
        return grouped.entries.map { (key, list) ->
            val first = list.first()
            val isAnnualGroup = list.any { it.entity.isAnnual }
            val unifiedTitle = resolveDisplayTitle(key, list)
            val unifiedFolder = if (isAnnualGroup) {
                "تعهدات سالانه و تعهدات"
            } else {
                first.effectiveFolder
            }
            GroupedCurrentMonthExpense(
                groupKey = key,
                displayTitle = unifiedTitle,
                effectiveFolder = unifiedFolder,
                isAnnual = isAnnualGroup,
                isRecurring = list.any { it.isRecurring },
                totalAmount = list.sumOf { it.entity.amount },
                items = list,
                latestTimestamp = list.maxOf { it.entity.timestamp },
                receiptCount = list.count { !it.entity.receiptImagePath.isNullOrBlank() }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GroupedExpenseDropdownCard(
    group: GroupedCurrentMonthExpense,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onEditItem: (ExpenseEntity) -> Unit,
    onDeleteItem: (ExpenseEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    var previewExpense by remember { mutableStateOf<ExpenseEntity?>(null) }
    var pendingDeleteExpense by remember { mutableStateOf<ExpenseEntity?>(null) }

    val cardBorderColor = when {
        group.isAnnual -> Color(0xFFF59E0B)
        group.isRecurring -> Color(0xFF0284C7)
        else -> {
            val f = group.effectiveFolder
            when {
                f.contains("بنزین") || f.contains("ماشین") || f.contains("خودرو") || f.contains("موتور") -> Color(0xFFF97316)
                f.contains("اینترنت") || f.contains("شارژ") || f.contains("قبوض") -> Color(0xFF8B5CF6)
                f.contains("سوپرمارکت") || f.contains("خرید") || f.contains("میوه") -> Color(0xFF10B981)
                f.contains("درمان") || f.contains("دارو") || f.contains("پزشک") -> Color(0xFFEC4899)
                f.contains("اجاره") || f.contains("خانه") || f.contains("مسکن") -> Color(0xFFEAB308)
                else -> Color(0xFF10B981)
            }
        }
    }

    val countBadgeBg = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.55f) else Color(0xFFDBEAFE)
    val countBadgeText = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onToggleExpand() }
            .testTag("grouped_expense_card_${group.groupKey}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.5.dp),
        border = BorderStroke(1.6.dp, cardBorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(scaledDp(14f))
        ) {
            // Header row: Category Icon + Full Title + Count Pill + Expand Arrow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (group.isAnnual) Color(0xFFFEF3C7)
                        else if (group.isRecurring) MaterialTheme.colorScheme.primaryContainer
                        else cardBorderColor.copy(alpha = 0.14f),
                        modifier = Modifier.size(scaledDp(38f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (group.isAnnual) Icons.Default.Shield
                                else if (group.isRecurring) Icons.Default.Repeat
                                else Icons.Default.Folder,
                                contentDescription = null,
                                tint = if (group.isAnnual) Color(0xFFD97706) else cardBorderColor,
                                modifier = Modifier.size(scaledDp(20f))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = enDigits(group.displayTitle),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = appTextColor(MaterialTheme.colorScheme.onSurface),
                            lineHeight = 22.sp,
                            softWrap = true
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = countBadgeBg
                        ) {
                            val badgeLabel = if (group.isAnnual) {
                                "مجموع ${group.items.size} تعهد سالانه"
                            } else {
                                "مجموع ${group.items.size} مورد ثبت‌شده"
                            }
                            Text(
                                text = enDigits(badgeLabel),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = countBadgeText,
                                softWrap = true,
                                modifier = Modifier.padding(horizontal = scaledDp(7f), vertical = scaledDp(2f))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = cardBorderColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, cardBorderColor.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onToggleExpand() }
                        .testTag("toggle_group_expand_${group.groupKey}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isExpanded) "بستن ریز" else "ریز مخارج",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = cardBorderColor,
                            softWrap = true
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "بستن جزئیات" else "مشاهده ریز مخارج",
                            tint = cardBorderColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Total summed amount + folder & receipt indicators
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = enDigits(PersianDateHelper.formatCurrency(group.totalAmount)),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = if (group.isAnnual) Color(0xFFB45309) else MaterialTheme.colorScheme.primary,
                    softWrap = true
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (group.isAnnual) Color(0xFFFEF3C7)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .padding(horizontal = scaledDp(8f), vertical = scaledDp(4f))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = if (group.isAnnual) Color(0xFF78350F) else appTextColor(MaterialTheme.colorScheme.onSurface),
                                modifier = Modifier.size(scaledDp(13f))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = enDigits(group.effectiveFolder),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (group.isAnnual) Color(0xFF78350F) else appTextColor(MaterialTheme.colorScheme.onSurface),
                                softWrap = true
                            )
                        }
                    }

                    if (group.receiptCount > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFCE7F3),
                            border = BorderStroke(1.dp, Color(0xFFEC4899).copy(alpha = 0.55f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = scaledDp(7f), vertical = scaledDp(4f)),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = "تعداد رسیدهای پیوست‌شده",
                                    tint = Color(0xFFBE185D),
                                    modifier = Modifier.size(scaledDp(12f))
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = enDigits("${group.receiptCount} رسید"),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF9D174D),
                                    softWrap = true
                                )
                            }
                        }
                    }
                }
            }

            // Expandable Dropdown List of Individual Expenses
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = cardBorderColor.copy(alpha = 0.35f))

                    group.items.forEachIndexed { index, resolved ->
                        val item = resolved.entity
                        val cleanItemTitle = item.title.replace(Regex("[\\r\\n]+"), " ").trim()
                        val cleanNote = item.note.replace(Regex("[\\r\\n]+"), " ").trim()
                        val hasReceipt = !item.receiptImagePath.isNullOrBlank()

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onEditItem(item) }
                                .testTag("grouped_sub_item_${item.id}")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = cardBorderColor.copy(alpha = 0.16f),
                                            modifier = Modifier.size(scaledDp(22f))
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${index + 1}",
                                                    fontFamily = OutfitFontFamily,
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = cardBorderColor
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = enDigits(cleanItemTitle),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = appTextColor(MaterialTheme.colorScheme.onSurface),
                                            lineHeight = 19.sp,
                                            softWrap = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .testTag("edit_grouped_sub_expense_${item.id}")
                                                .clickable { onEditItem(item) }
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFFE0F2FE),
                                                border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "ویرایش هزینه",
                                                        tint = Color(0xFF0284C7),
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .testTag("delete_grouped_sub_expense_${item.id}")
                                                .clickable { pendingDeleteExpense = item }
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFFFEE2E2),
                                                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "حذف هزینه",
                                                        tint = Color(0xFFDC2626),
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = enDigits(PersianDateHelper.formatCurrency(item.amount)),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (item.isAnnual) Color(0xFFB45309) else MaterialTheme.colorScheme.primary,
                                        softWrap = true
                                    )

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = appSecondaryTextColor(MaterialTheme.colorScheme.onSurfaceVariant),
                                            modifier = Modifier.size(scaledDp(12f))
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = enDigits(item.shamsiDateString),
                                            fontSize = 11.5.sp,
                                            color = appTextColor(MaterialTheme.colorScheme.onSurface),
                                            fontWeight = FontWeight.Bold,
                                            softWrap = true
                                        )
                                    }

                                    if (hasReceipt) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(7.dp),
                                                color = Color(0xFFFCE7F3),
                                                border = BorderStroke(1.dp, Color(0xFFEC4899).copy(alpha = 0.65f)),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(7.dp))
                                                    .clickable { previewExpense = item }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.AttachFile,
                                                        contentDescription = "مشاهده تصویر رسید",
                                                        tint = Color(0xFFBE185D),
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text(
                                                        text = "دارای پیوست رسید",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF9D174D),
                                                        softWrap = true
                                                    )
                                                }
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(7.dp),
                                                color = Color(0xFFECFDF5),
                                                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.65f)),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(7.dp))
                                                    .testTag("share_grouped_sub_expense_${item.id}")
                                                    .clickable {
                                                        ReceiptImageHelper.shareExpenseWithReceipt(context, item)
                                                    }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Share,
                                                        contentDescription = "اشتراک‌گذاری هزینه و تصویر رسید",
                                                        tint = Color(0xFF047857),
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text(
                                                        text = "اشتراک",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF065F46),
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (cleanNote.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(5.dp))
                                    Text(
                                        text = enDigits(cleanNote),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = appTextColor(MaterialTheme.colorScheme.onSurface),
                                        lineHeight = 17.sp,
                                        softWrap = true
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    previewExpense?.let { exp ->
        ReceiptPreviewDialog(
            expense = exp,
            onDismissRequest = { previewExpense = null }
        )
    }

    pendingDeleteExpense?.let { target ->
        val cleanTargetTitle = target.title.replace(Regex("[\\r\\n]+"), " ").trim()
        AlertDialog(
            onDismissRequest = { pendingDeleteExpense = null },
            icon = {
                ScaledDialogContent {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFDC2626)
                    )
                }
            },
            title = {
                ScaledDialogContent {
                    Text(
                        text = "تأیید حذف هزینه",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = appTextColor(MaterialTheme.colorScheme.onSurface)
                    )
                }
            },
            text = {
                ScaledDialogContent {
                    Text(
                        text = enDigits("آیا از حذف هزینه «$cleanTargetTitle» به مبلغ ${PersianDateHelper.formatCurrency(target.amount)} در تاریخ ${target.shamsiDateString} مطمئن هستید؟"),
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = appTextColor(MaterialTheme.colorScheme.onSurface)
                    )
                }
            },
            confirmButton = {
                ScaledDialogContent {
                    Button(
                        onClick = {
                            pendingDeleteExpense = null
                            onDeleteItem(target)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        modifier = Modifier
                            .heightIn(min = scaledDp(48f))
                            .testTag("confirm_delete_grouped_sub_button")
                    ) {
                        Text("بله، حذف شود", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                ScaledDialogContent {
                    OutlinedButton(
                        onClick = { pendingDeleteExpense = null },
                        modifier = Modifier
                            .heightIn(min = scaledDp(48f))
                            .testTag("cancel_delete_grouped_sub_button")
                    ) {
                        Text("انصراف", color = appTextColor(MaterialTheme.colorScheme.primary))
                    }
                }
            }
        )
    }
}
