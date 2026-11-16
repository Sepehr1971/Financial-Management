package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ExpenseEntity
import com.example.data.model.MonthSummary
import com.example.data.model.ResolvedExpense
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.ScaledDialogContent
import com.example.ui.theme.appSecondaryTextColor
import com.example.ui.theme.appTextColor
import com.example.ui.theme.enDigits
import com.example.ui.theme.scaledDp
import com.example.ui.viewmodel.MonthlyBreakdownItem
import com.example.utils.PersianDateHelper
import com.example.utils.ReceiptImageHelper

/**
 * Data item representing a single month bar in the trend chart.
 */
data class MonthlyTrendBarData(
    val id: String,
    val year: Int,
    val month: Int,
    val monthName: String,
    val yearMonthLabel: String,
    val amount: Long,
    val transactionCount: Int,
    val percentageOfTotal: Float,
    val isPeak: Boolean = false,
    val isCurrentMonth: Boolean = false,
    val transactions: List<ResolvedExpense> = emptyList()
)

/**
 * Recharts-inspired Vertical Bar Chart for Monthly Spending Trends with Interactive Modal.
 * Visualizes budget distribution across months with:
 * - Animated vertical bars with top rounded corners and vibrant gradients
 * - Horizontal Cartesian reference lines (0%, 50%, 100%)
 * - Interactive tap selection on bars opening a detailed modal popup with full transactions
 * - Key metric badges (Average monthly spend, Peak month, Period Total)
 * - Cheerful, vibrant border styling adhering to user theme requirements
 */
@Composable
fun MonthlyTrendsBarChart(
    monthlyBreakdown: List<MonthlyBreakdownItem>,
    yearMonthSummaries: List<MonthSummary> = emptyList(),
    allExpenses: List<ResolvedExpense> = emptyList(),
    onEditExpense: ((ExpenseEntity) -> Unit)? = null,
    modifier: Modifier = Modifier,
    title: String = "نمودار میله‌ای روند مخارج ماهانه",
    subtitle: String = "توزیع بودجه و روند تغییرات ماه به ماه",
    onMonthClick: ((year: Int, month: Int) -> Unit)? = null
) {
    // Mode switcher: Current Filter Months vs Full 12-Month Shamsi Year
    var viewMode by remember { mutableStateOf(0) } // 0 = Filter Range, 1 = 12 Months of Year

    // Modal State: when not null, shows MonthExpensesDetailModal
    var selectedModalMonth by remember { mutableStateOf<MonthlyTrendBarData?>(null) }

    val currentShamsiMonth = remember { PersianDateHelper.getShamsiDate().month }
    val currentShamsiYear = remember { PersianDateHelper.getShamsiDate().year }

    // Prepare chart data based on active view mode
    val chartItems = remember(monthlyBreakdown, yearMonthSummaries, allExpenses, viewMode) {
        if (viewMode == 0 || yearMonthSummaries.isEmpty()) {
            val total = monthlyBreakdown.sumOf { it.totalAmount }.coerceAtLeast(1L)
            val maxAmount = monthlyBreakdown.maxOfOrNull { it.totalAmount } ?: 0L
            monthlyBreakdown.map { item ->
                MonthlyTrendBarData(
                    id = "${item.year}_${item.month}",
                    year = item.year,
                    month = item.month,
                    monthName = item.monthName,
                    yearMonthLabel = item.yearMonthLabel,
                    amount = item.totalAmount,
                    transactionCount = item.transactionCount,
                    percentageOfTotal = if (total > 0) (item.totalAmount.toFloat() / total.toFloat() * 100f) else 0f,
                    isPeak = item.totalAmount > 0 && item.totalAmount == maxAmount,
                    isCurrentMonth = item.year == currentShamsiYear && item.month == currentShamsiMonth,
                    transactions = item.transactions
                )
            }
        } else {
            val total = yearMonthSummaries.sumOf { it.totalExpense }.coerceAtLeast(1L)
            val maxAmount = yearMonthSummaries.maxOfOrNull { it.totalExpense } ?: 0L
            yearMonthSummaries.map { item ->
                val monthTransactions = allExpenses.filter {
                    it.entity.shamsiYear == item.year && it.entity.shamsiMonth == item.monthIndex
                }
                MonthlyTrendBarData(
                    id = "${item.year}_${item.monthIndex}",
                    year = item.year,
                    month = item.monthIndex,
                    monthName = item.monthName,
                    yearMonthLabel = "${item.monthName} ${PersianDateHelper.toEnglishDigits(item.year.toString())}",
                    amount = item.totalExpense,
                    transactionCount = item.transactionCount,
                    percentageOfTotal = if (total > 0) (item.totalExpense.toFloat() / total.toFloat() * 100f) else 0f,
                    isPeak = item.totalExpense > 0 && item.totalExpense == maxAmount,
                    isCurrentMonth = item.year == currentShamsiYear && item.monthIndex == currentShamsiMonth,
                    transactions = monthTransactions
                )
            }
        }
    }

    var selectedBarId by remember(chartItems) {
        mutableStateOf(
            chartItems.firstOrNull { it.isCurrentMonth && it.amount > 0 }?.id
                ?: chartItems.firstOrNull { it.isPeak }?.id
                ?: chartItems.lastOrNull { it.amount > 0 }?.id
                ?: chartItems.firstOrNull()?.id
        )
    }

    val selectedItem = remember(chartItems, selectedBarId) {
        chartItems.find { it.id == selectedBarId } ?: chartItems.firstOrNull()
    }

    var animationPlayed by remember { mutableStateOf(false) }
    LaunchedEffect(chartItems) {
        animationPlayed = true
    }

    val grandTotal = remember(chartItems) { chartItems.sumOf { it.amount } }
    val maxSpend = remember(chartItems) { (chartItems.maxOfOrNull { it.amount } ?: 0L).coerceAtLeast(1L) }
    val averageSpend = remember(chartItems) {
        val count = chartItems.size.coerceAtLeast(1)
        grandTotal / count
    }
    val peakItem = remember(chartItems) { chartItems.maxByOrNull { it.amount } }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_trends_bar_chart"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, Color(0xFF0284C7)), // Lively Cyan/Sky Blue border
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row 1: Icon + Full Title (never truncated!)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFE0F2FE),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                    modifier = Modifier.size(scaledDp(36f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(scaledDp(22f))
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = appTextColor(MaterialTheme.colorScheme.onSurface),
                    softWrap = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Header Row 2: Subtitle + View Mode Switcher Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = appSecondaryTextColor(MaterialTheme.colorScheme.onSurfaceVariant),
                    softWrap = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))

                // Switcher pill if 12-month data is available
                if (yearMonthSummaries.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.5f))
                    ) {
                        Row(modifier = Modifier.padding(scaledDp(2f))) {
                            // Option 0: Filter range
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (viewMode == 0) Color(0xFF0284C7) else Color.Transparent)
                                    .clickable { viewMode = 0 }
                                    .padding(horizontal = scaledDp(7f), vertical = scaledDp(3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "بازه فیلتر",
                                    fontSize = 10.sp,
                                    fontWeight = if (viewMode == 0) FontWeight.Black else FontWeight.Bold,
                                    color = if (viewMode == 0) Color.White else appTextColor(MaterialTheme.colorScheme.onSurface),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            // Option 1: 12 Months
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (viewMode == 1) Color(0xFF0284C7) else Color.Transparent)
                                    .clickable { viewMode = 1 }
                                    .padding(horizontal = scaledDp(7f), vertical = scaledDp(3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = enDigits("12 ماه سال"),
                                    fontSize = 10.sp,
                                    fontWeight = if (viewMode == 1) FontWeight.Black else FontWeight.Bold,
                                    color = if (viewMode == 1) Color.White else appTextColor(MaterialTheme.colorScheme.onSurface),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Selected Month Detail Banner (Interactive Tooltip / Recharts Hero Box)
            selectedItem?.let { item ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.2.dp, if (item.isPeak) Color(0xFFF59E0B) else Color(0xFF0284C7)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp)
                    ) {
                        // Row 1: Month Name & Stats Pill + Modal Detail Trigger Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Text(
                                    text = enDigits("ماه انتخابی: ${item.yearMonthLabel}"),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = appTextColor(MaterialTheme.colorScheme.onSurface),
                                    softWrap = true
                                )
                                if (item.isPeak) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFFEF3C7)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocalFireDepartment,
                                                contentDescription = null,
                                                tint = Color(0xFFB45309),
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = "بیشترین",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFB45309),
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Horizontal Stats Pill: "X تراکنش • Y% از کل"
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = enDigits("${item.transactionCount} تراکنش • ${item.percentageOfTotal.toInt()}%"),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = scaledDp(7f), vertical = scaledDp(3f))
                                    )
                                }

                                // Interactive Button to open full Month Details Modal directly
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF0284C7),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { selectedModalMonth = item }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = scaledDp(7f), vertical = scaledDp(3f)),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ReceiptLong,
                                            contentDescription = "مشاهده جزئیات",
                                            tint = Color.White,
                                            modifier = Modifier.size(scaledDp(12f))
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "ریز جزئیات",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(5.dp))

                        // Row 2: Amount Label on start, Formatted Currency on end
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مجموع هزینه این ماه (روی میله یا دکمه بزنید):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = appSecondaryTextColor(MaterialTheme.colorScheme.onSurfaceVariant),
                                softWrap = true,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = enDigits(PersianDateHelper.formatCurrency(item.amount)),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = if (item.isPeak) Color(0xFFD97706) else MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (chartItems.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "هزینه‌ای برای نمایش در این ماه‌ها ثبت نشده است",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Chart Area with Canvas Grid & Vertical Bars
                val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                val averageLineColor = Color(0xFFF59E0B).copy(alpha = 0.7f)
                val avgRatio = (averageSpend.toFloat() / maxSpend.toFloat()).coerceIn(0.05f, 1f)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(178.dp)
                ) {
                    // Background Cartesian Grid (Dotted horizontal lines at 100%, 50%, 0%)
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 18.dp, bottom = 24.dp, start = 4.dp, end = 4.dp)
                    ) {
                        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

                        // 100% Top Grid line
                        drawLine(
                            color = gridLineColor,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = pathEffect
                        )

                        // 50% Mid Grid line
                        val midY = size.height * 0.5f
                        drawLine(
                            color = gridLineColor,
                            start = Offset(0f, midY),
                            end = Offset(size.width, midY),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = pathEffect
                        )

                        // Average Line
                        val avgY = size.height * (1f - avgRatio)
                        drawLine(
                            color = averageLineColor,
                            start = Offset(0f, avgY),
                            end = Offset(size.width, avgY),
                            strokeWidth = 1.2.dp.toPx(),
                            pathEffect = pathEffect
                        )

                        // Base Axis line (0%)
                        drawLine(
                            color = gridLineColor.copy(alpha = 0.8f),
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }

                    // Vertical Bars Row
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(if (chartItems.size > 8) 2.dp else 6.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        chartItems.forEachIndexed { index, item ->
                            val isSelected = item.id == selectedBarId
                            val targetRatio = if (item.amount <= 0L) 0.03f else (item.amount.toFloat() / maxSpend.toFloat()).coerceIn(0.05f, 1f)
                            val animatedProgress by animateFloatAsState(
                                targetValue = if (animationPlayed) targetRatio else 0f,
                                animationSpec = tween(
                                    durationMillis = 600 + (index * 40).coerceAtMost(400),
                                    easing = FastOutSlowInEasing
                                ),
                                label = "trend_bar_${item.id}"
                            )

                            // Gradient styling
                            val barBrush = remember(isSelected, item.isPeak) {
                                when {
                                    isSelected -> Brush.verticalGradient(
                                        colors = listOf(Color(0xFF6366F1), Color(0xFF4F46E5)) // Vibrant Indigo
                                    )
                                    item.isPeak -> Brush.verticalGradient(
                                        colors = listOf(Color(0xFFF59E0B), Color(0xFFD97706)) // Radiant Amber/Orange
                                    )
                                    item.isCurrentMonth -> Brush.verticalGradient(
                                        colors = listOf(Color(0xFF10B981), Color(0xFF059669)) // Emerald Green
                                    )
                                    else -> Brush.verticalGradient(
                                        colors = listOf(Color(0xFF38BDF8), Color(0xFF0284C7)) // Sky to Deep Cyan
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable {
                                        selectedBarId = item.id
                                        selectedModalMonth = item // 🚀 Opens detailed Modal popup on click!
                                        onMonthClick?.invoke(item.year, item.month)
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                // Spent amount label always shown above each bar (compact, horizontal)
                                val topAmountLabel = when {
                                    item.amount == 0L -> "0"
                                    item.amount >= 1_000_000L -> {
                                        val millions = item.amount / 1_000_000L
                                        val tenth = (item.amount % 1_000_000L) / 100_000L
                                        if (tenth > 0 && millions < 100) {
                                            "$millions.${tenth}M"
                                        } else {
                                            "${millions}M"
                                        }
                                    }
                                    item.amount >= 1_000L -> "${item.amount / 1_000L}K"
                                    else -> item.amount.toString()
                                }

                                Text(
                                    text = topAmountLabel,
                                    fontFamily = OutfitFontFamily,
                                    fontSize = if (chartItems.size > 8) 7.5.sp else 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = when {
                                        item.isPeak -> Color(0xFFB45309)
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        item.amount == 0L -> appSecondaryTextColor(MaterialTheme.colorScheme.onSurfaceVariant)
                                        else -> appTextColor(MaterialTheme.colorScheme.onSurface)
                                    },
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Spacer(modifier = Modifier.height(2.dp))

                                // The Vertical Bar Pillar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f, fill = false)
                                        .height((115.dp * animatedProgress).coerceAtLeast(6.dp))
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(barBrush)
                                        .then(
                                            if (isSelected) {
                                                Modifier.border(
                                                    1.5.dp,
                                                    Color.White,
                                                    RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                                )
                                            } else {
                                                Modifier
                                            }
                                        )
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // X-Axis Month Label
                                val shortMonthName = when (item.monthName) {
                                    "فروردین" -> "فرور"
                                    "اردیبهشت" -> "اردی"
                                    "خرداد" -> "خرد"
                                    "تیر" -> "تیر"
                                    "مرداد" -> "مرد"
                                    "شهریور" -> "شهر"
                                    "مهر" -> "مهر"
                                    "آبان" -> "آبان"
                                    "آذر" -> "آذر"
                                    "دی" -> "دی"
                                    "بهمن" -> "بهمن"
                                    "اسفند" -> "اسف"
                                    else -> item.monthName.take(4)
                                }

                                Text(
                                    text = if (chartItems.size > 8) shortMonthName else item.monthName,
                                    fontSize = if (chartItems.size > 8) 9.sp else 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    maxLines = 1,
                                    softWrap = false,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom Reference Metrics Cards (Clean 3-Column stats)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // 1. Average Monthly Spend
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(scaledDp(7f))) {
                            Text(
                                text = "میانگین ماهانه:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = appSecondaryTextColor(MaterialTheme.colorScheme.onSurfaceVariant),
                                softWrap = true
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = enDigits(PersianDateHelper.formatCurrency(averageSpend)),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0369A1),
                                softWrap = true
                            )
                        }
                    }

                    // 2. Peak Month
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFEF3C7).copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(scaledDp(7f))) {
                            Text(
                                text = "اوج مخارج:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E),
                                softWrap = true
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = peakItem?.monthName ?: "-",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFB45309),
                                softWrap = true
                            )
                        }
                    }

                    // 3. Total Period Budget
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(scaledDp(7f))) {
                            Text(
                                text = "کل مخارج دوره:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = appSecondaryTextColor(MaterialTheme.colorScheme.onSurfaceVariant),
                                softWrap = true
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = enDigits(PersianDateHelper.formatCurrency(grandTotal)),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF047857),
                                softWrap = true
                            )
                        }
                    }
                }
            }
        }
    }

    // 🚀 Interactive Month Expenses Detail Modal (پنجره کوچک جزئیات هزینه‌های ماه هنگام کلیک روی میله)
    selectedModalMonth?.let { monthData ->
        MonthExpensesDetailModal(
            monthData = monthData,
            onDismiss = { selectedModalMonth = null },
            onEditExpense = { entity ->
                selectedModalMonth = null
                onEditExpense?.invoke(entity)
            }
        )
    }
}

/**
 * Recharts-style Interactive Modal showing the itemized expenses and folder breakdown for a clicked month.
 */
@Composable
fun MonthExpensesDetailModal(
    monthData: MonthlyTrendBarData,
    onDismiss: () -> Unit,
    onEditExpense: ((ExpenseEntity) -> Unit)? = null
) {
    val context = LocalContext.current
    var previewExpense by remember { mutableStateOf<ExpenseEntity?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ScaledDialogContent {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = scaledDp(560f)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, Color(0xFF0284C7)), // Lively Cyan border
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(scaledDp(16f))
            ) {
                // 1. Header: Icon + Title + Close Button
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
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFE0F2FE),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                            modifier = Modifier.size(scaledDp(36f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(scaledDp(20f))
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = enDigits("جزئیات هزینه‌های «${monthData.yearMonthLabel}»"),
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Black,
                                color = appTextColor(MaterialTheme.colorScheme.onSurface),
                                softWrap = true
                            )
                            Text(
                                text = enDigits("${monthData.transactionCount} تراکنش ثبت‌شده در این ماه"),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = appSecondaryTextColor(MaterialTheme.colorScheme.onSurfaceVariant),
                                softWrap = true
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Summary Card: Total Amount + Percentage + Peak Badge
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.2.dp, if (monthData.isPeak) Color(0xFFF59E0B) else Color(0xFF0284C7).copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "مجموع مخارج ماه:",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = appSecondaryTextColor(MaterialTheme.colorScheme.onSurfaceVariant),
                                softWrap = true
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = enDigits(PersianDateHelper.formatCurrency(monthData.amount)),
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Black,
                                color = if (monthData.isPeak) Color(0xFFD97706) else MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            if (monthData.isPeak) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFFEF3C7)
                                ) {
                                    Text(
                                        text = "🔥 اوج مخارج",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB45309),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                            }
                            Text(
                                text = enDigits("${monthData.percentageOfTotal.toInt()}% از بودجه دوره"),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Folder Breakdown Chips for this Month (if multiple folders exist)
                val monthFolders = remember(monthData.transactions) {
                    monthData.transactions.groupBy { it.effectiveFolder }
                        .map { (folder, items) -> folder to items.sumOf { it.entity.amount } }
                        .sortedByDescending { it.second }
                }

                if (monthFolders.size > 1) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(monthFolders) { (folderName, sum) ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = folderName,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = enDigits(PersianDateHelper.formatCurrency(sum)),
                                        fontSize = 10.5.sp,
                                        color = appTextColor(MaterialTheme.colorScheme.onSurface),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 4. Scrollable List of Expenses in this Month
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(6.dp))

                if (monthData.transactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "هیچ هزینه‌ای برای این ماه ثبت نشده است",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    val groupedTransactions = remember(monthData.transactions) {
                        CurrentMonthExpenseGrouper.groupExpenses(monthData.transactions)
                    }
                    var expandedModalGroups by remember(monthData) { mutableStateOf(setOf<String>()) }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(
                            items = groupedTransactions,
                            key = { g ->
                                if (!g.isAnnual && g.items.size == 1) "modal_s_${g.items.first().entity.id}"
                                else "modal_g_${g.groupKey}"
                            }
                        ) { group ->
                            if (!group.isAnnual && group.items.size == 1) {
                                val item = group.items.first()
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable(enabled = onEditExpense != null) {
                                            onEditExpense?.invoke(item.entity)
                                            onDismiss()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.entity.title,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = appTextColor(MaterialTheme.colorScheme.onSurface),
                                                softWrap = true
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant
                                                ) {
                                                    Text(
                                                        text = item.effectiveFolder,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = appTextColor(MaterialTheme.colorScheme.onSurface),
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                                        softWrap = true
                                                    )
                                                }
                                                if (item.entity.shamsiDateString.isNotBlank()) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = PersianDateHelper.toEnglishDigits(item.entity.shamsiDateString),
                                                        fontFamily = OutfitFontFamily,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = appSecondaryTextColor(MaterialTheme.colorScheme.onSurfaceVariant)
                                                    )
                                                }
                                                if (!item.entity.receiptImagePath.isNullOrBlank()) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(5.dp),
                                                        color = Color(0xFFFCE7F3),
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(5.dp))
                                                            .clickable { previewExpense = item.entity }
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.AttachFile,
                                                                contentDescription = "دارای پیوست رسید",
                                                                tint = Color(0xFFBE185D),
                                                                modifier = Modifier.size(11.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(2.dp))
                                                            Text(
                                                                text = "رسید",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFF9D174D)
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(5.dp),
                                                        color = Color(0xFFECFDF5),
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(5.dp))
                                                            .testTag("share_modal_expense_${item.entity.id}")
                                                            .clickable {
                                                                ReceiptImageHelper.shareExpenseWithReceipt(context, item.entity)
                                                            }
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Share,
                                                                contentDescription = "اشتراک‌گذاری هزینه و تصویر رسید",
                                                                tint = Color(0xFF047857),
                                                                modifier = Modifier.size(11.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(2.dp))
                                                            Text(
                                                                text = "اشتراک",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFF065F46)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = enDigits(PersianDateHelper.formatCurrency(item.entity.amount)),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            } else {
                                val isGroupExpanded = expandedModalGroups.contains(group.groupKey)
                                val borderAccent = if (group.isAnnual) Color(0xFFF59E0B) else MaterialTheme.colorScheme.primary
                                Surface(
                                    shape = RoundedCornerShape(11.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.2.dp, borderAccent.copy(alpha = 0.7f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(11.dp))
                                        .clickable {
                                            expandedModalGroups = if (isGroupExpanded) {
                                                expandedModalGroups - group.groupKey
                                            } else {
                                                expandedModalGroups + group.groupKey
                                            }
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = scaledDp(10f), vertical = scaledDp(9f))
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = group.displayTitle,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = appTextColor(MaterialTheme.colorScheme.onSurface),
                                                    softWrap = true
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = enDigits("${group.items.size} مورد • مجموع ${PersianDateHelper.formatCurrency(group.totalAmount)}"),
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (group.isAnnual) Color(0xFFD97706) else MaterialTheme.colorScheme.primary,
                                                    softWrap = true
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = if (isGroupExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                contentDescription = if (isGroupExpanded) "بستن" else "باز کردن ریز مخارج",
                                                tint = borderAccent,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

                                        AnimatedVisibility(visible = isGroupExpanded) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 8.dp),
                                                verticalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                HorizontalDivider(color = borderAccent.copy(alpha = 0.3f))
                                                group.items.forEachIndexed { idx, subItem ->
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .clickable(enabled = onEditExpense != null) {
                                                                onEditExpense?.invoke(subItem.entity)
                                                                onDismiss()
                                                            }
                                                    ) {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(
                                                                    text = enDigits("${idx + 1}. ${subItem.entity.title}"),
                                                                    fontSize = 11.5.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = appTextColor(MaterialTheme.colorScheme.onSurface),
                                                                    softWrap = true
                                                                )
                                                                if (subItem.entity.shamsiDateString.isNotBlank()) {
                                                                    Text(
                                                                        text = PersianDateHelper.toEnglishDigits(subItem.entity.shamsiDateString),
                                                                        fontFamily = OutfitFontFamily,
                                                                        fontSize = 10.sp,
                                                                        color = appSecondaryTextColor(MaterialTheme.colorScheme.onSurfaceVariant)
                                                                    )
                                                                }
                                                            }
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                if (!subItem.entity.receiptImagePath.isNullOrBlank()) {
                                                                    Surface(
                                                                        shape = RoundedCornerShape(5.dp),
                                                                        color = Color(0xFFFCE7F3),
                                                                        modifier = Modifier
                                                                            .padding(end = 4.dp)
                                                                            .clip(RoundedCornerShape(5.dp))
                                                                            .clickable { previewExpense = subItem.entity }
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = Icons.Default.AttachFile,
                                                                            contentDescription = "مشاهده رسید",
                                                                            tint = Color(0xFFBE185D),
                                                                            modifier = Modifier.padding(4.dp).size(12.dp)
                                                                        )
                                                                    }
                                                                    Surface(
                                                                        shape = RoundedCornerShape(5.dp),
                                                                        color = Color(0xFFECFDF5),
                                                                        modifier = Modifier
                                                                            .padding(end = 6.dp)
                                                                            .clip(RoundedCornerShape(5.dp))
                                                                            .testTag("share_modal_sub_expense_${subItem.entity.id}")
                                                                            .clickable {
                                                                                ReceiptImageHelper.shareExpenseWithReceipt(context, subItem.entity)
                                                                            }
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = Icons.Default.Share,
                                                                            contentDescription = "اشتراک‌گذاری هزینه و رسید",
                                                                            tint = Color(0xFF047857),
                                                                            modifier = Modifier.padding(4.dp).size(12.dp)
                                                                        )
                                                                    }
                                                                }
                                                                Text(
                                                                    text = enDigits(PersianDateHelper.formatCurrency(subItem.entity.amount)),
                                                                    fontSize = 11.5.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = if (subItem.entity.isAnnual) Color(0xFFD97706) else MaterialTheme.colorScheme.primary
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 5. Close Button (Solid, Prominent, Non-wrapping)
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = scaledDp(42f))
                ) {
                    Text(
                        text = "بستن",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
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
}
