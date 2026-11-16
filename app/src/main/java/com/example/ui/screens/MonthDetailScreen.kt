package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ExpenseEntity
import com.example.ui.components.CurrentMonthExpenseGrouper
import com.example.ui.components.ExpenseItemCard
import com.example.ui.components.GroupedExpenseDropdownCard
import com.example.ui.components.ModernBarChart
import androidx.compose.foundation.layout.heightIn
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.appSecondaryTextColor
import com.example.ui.theme.appTextColor
import com.example.ui.theme.enDigits
import com.example.ui.theme.scaledDp
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.utils.PersianDateHelper

@Composable
fun MonthDetailScreen(
    viewModel: ExpenseViewModel,
    onBackClick: () -> Unit,
    onAddExpenseInMonth: (monthIndex: Int) -> Unit,
    onEditExpense: (ExpenseEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBackClick() }

    val monthSummary by viewModel.selectedMonthDetail.collectAsStateWithLifecycle()
    val resolvedItems by viewModel.selectedMonthResolvedExpenses.collectAsStateWithLifecycle()
    val currentYear by viewModel.currentYear.collectAsStateWithLifecycle()

    var selectedDayFilter by remember { mutableStateOf<Int?>(null) }

    val monthName = monthSummary?.monthName ?: "ماه"
    val monthIndex = monthSummary?.monthIndex ?: 1

    val displayedItems = remember(resolvedItems, selectedDayFilter) {
        if (selectedDayFilter == null) resolvedItems
        else resolvedItems.filter { it.entity.shamsiDay == selectedDayFilter }
    }
    val groupedDisplayedItems = remember(displayedItems) {
        CurrentMonthExpenseGrouper.groupExpenses(displayedItems)
    }
    var expandedGroupKeys by rememberSaveable { mutableStateOf(listOf<String>()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("month_detail_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Top Navigation Bar
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onBackClick() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "بازگشت به تقویم 12 ماه",
                                tint = Color.White,
                                modifier = Modifier.size(scaledDp(20f))
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = enDigits("هزینه‌های ماه $monthName $currentYear"),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = appTextColor(MaterialTheme.colorScheme.onSurface),
                            softWrap = true
                        )
                        Text(
                            text = enDigits("← بازگشت به لیست 12 ماه سال"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Summary Card: Total, Direct, and Amortized Share
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(scaledDp(18f))) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "جمع کل هزینه این ماه:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = appTextColor(MaterialTheme.colorScheme.onSurface),
                            softWrap = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = enDigits(PersianDateHelper.formatCurrency(monthSummary?.totalExpense ?: 0L)),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Breakdown: Balanced columns with amounts below labels to avoid any overlap
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "هزینه‌های جاری مستقیم:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = appSecondaryTextColor(MaterialTheme.colorScheme.onSurfaceVariant),
                                softWrap = true
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = enDigits(PersianDateHelper.formatCurrency(monthSummary?.directExpenseTotal ?: 0L)),
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Black,
                                color = appTextColor(MaterialTheme.colorScheme.onSurface),
                                softWrap = true
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(scaledDp(14f))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "سهم هزینه‌های سالانه:",
                                    fontSize = 12.sp,
                                    color = Color(0xFF92400E),
                                    fontWeight = FontWeight.ExtraBold,
                                    softWrap = true
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = enDigits(PersianDateHelper.formatCurrency(monthSummary?.amortizedAnnualShare ?: 0L)),
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFB45309),
                                softWrap = true
                            )
                        }
                    }
                }
            }
        }

        // CRITICAL REQUIREMENT: 30-Day Monthly Calendar Grid with Weekdays Header Row
        item {
            val daysCount = PersianDateHelper.getDaysInMonth(currentYear, monthIndex)
            val startOffset = PersianDateHelper.getFirstDayOfWeekForShamsiMonth(currentYear, monthIndex)
            val totalCells = (0 until startOffset).map { null } + (1..daysCount).map { it }
            val weekDayNames = PersianDateHelper.SHAMSI_WEEK_DAY_NAMES
            val dayExpensesMap = remember(resolvedItems) { resolvedItems.groupBy { it.entity.shamsiDay } }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(scaledDp(14f))) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(scaledDp(18f))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = enDigits("تقویم 30 روزه ماه $monthName:"),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = appTextColor(MaterialTheme.colorScheme.onSurface)
                            )
                        }

                        if (selectedDayFilter != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedDayFilter = null }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = scaledDp(8f), vertical = scaledDp(3f)),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = enDigits("روز ${selectedDayFilter!!} • نمایش همه"),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "لغو فیلتر روز",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(scaledDp(12f))
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Weekdays Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        weekDayNames.forEachIndexed { idx, wName ->
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = enDigits(wName),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (idx == 6) Color(0xFFB91C1C) else appTextColor(MaterialTheme.colorScheme.onSurface)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Grid of Days
                    val rows = totalCells.chunked(7)
                    rows.forEach { rowCells ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (cellIndex in 0 until 7) {
                                val day = rowCells.getOrNull(cellIndex)
                                if (day == null) {
                                    Spacer(modifier = Modifier.weight(1f))
                                } else {
                                    val isSelected = selectedDayFilter == day
                                    val dayExpenses = dayExpensesMap[day] ?: emptyList()
                                    val hasExpenses = dayExpenses.isNotEmpty()
                                    val isFriday = cellIndex == 6

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 2.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                when {
                                                    isSelected -> MaterialTheme.colorScheme.primary
                                                    hasExpenses -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                                    isFriday -> Color(0xFFFEF2F2)
                                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                                }
                                            )
                                            .border(
                                                width = if (isSelected) 0.dp else 1.dp,
                                                color = when {
                                                    isSelected -> Color.Transparent
                                                    hasExpenses -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                                    else -> Color.Transparent
                                                },
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                selectedDayFilter = if (selectedDayFilter == day) null else day
                                            }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = day.toString(),
                                                fontFamily = OutfitFontFamily,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Black,
                                                color = when {
                                                    isSelected -> Color.White
                                                    isFriday -> Color(0xFFB91C1C)
                                                    hasExpenses -> MaterialTheme.colorScheme.primary
                                                    else -> appTextColor(MaterialTheme.colorScheme.onSurface)
                                                }
                                            )
                                            if (hasExpenses) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isSelected) Color.White else MaterialTheme.colorScheme.primary)
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

        // Dedicated Bar Chart for This Month's Folders
        item {
            val folders = monthSummary?.folderSummaries ?: emptyList()
            ModernBarChart(
                folders = folders,
                title = "نمودار میله‌ای هزینه‌های $monthName"
            )
        }

        // Folder breakdown details list
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "تفکیک مستقل جمع هر پوشه در $monthName:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = appTextColor(MaterialTheme.colorScheme.onSurface)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val folders = monthSummary?.folderSummaries ?: emptyList()
                    if (folders.isEmpty()) {
                        Text(
                            text = "پوشه‌ای برای نمایش وجود ندارد.",
                            fontSize = 12.sp,
                            color = appSecondaryTextColor(MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    } else {
                        folders.forEach { f ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                // Line 1: Color dot + Full folder title (no truncation or collision)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(9.dp)
                                            .clip(CircleShape)
                                            .background(Color(f.colorHex))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = f.folderName,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = appTextColor(MaterialTheme.colorScheme.onSurface),
                                        maxLines = 2,
                                        softWrap = true,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.5.dp))

                                // Line 2: Amount and percentage slightly below the label
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 17.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "مجموع هزینه:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = appSecondaryTextColor(MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = enDigits(PersianDateHelper.formatCurrency(f.totalAmount)),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = enDigits("(${f.percentage.toInt()}%)"),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = appSecondaryTextColor(MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                        }
                    }
                }
            }
        }

        // Action button to add an expense in this month
        item {
            Button(
                onClick = { onAddExpenseInMonth(monthIndex) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = scaledDp(48f)),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(scaledDp(18f))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ثبت هزینه جدید برای $monthName",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        // Direct Itemized Transactions in this month
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val headerTitle = if (selectedDayFilter != null)
                    "هزینه‌های روز ${selectedDayFilter!!} $monthName"
                else
                    "ریز هزینه‌های ثبت‌شده $monthName"

                Text(
                    text = enDigits(headerTitle),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = appTextColor(MaterialTheme.colorScheme.onSurface),
                    softWrap = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                val countLabel = if (groupedDisplayedItems.size == displayedItems.size) {
                    "${displayedItems.size} مورد"
                } else {
                    "${groupedDisplayedItems.size} دسته (${displayedItems.size} مورد)"
                }
                Text(
                    text = enDigits(countLabel),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = appSecondaryTextColor(MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }

        if (groupedDisplayedItems.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(scaledDp(20f)),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = appSecondaryTextColor(MaterialTheme.colorScheme.onSurfaceVariant),
                            modifier = Modifier.size(scaledDp(32f))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val emptyMsg = if (selectedDayFilter != null)
                            "در روز ${selectedDayFilter!!} $monthName هزینه‌ای ثبت نشده است"
                        else
                            "هزینه مستقیمی در این ماه ثبت نشده است"
                        Text(
                            text = enDigits(emptyMsg),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = appTextColor(MaterialTheme.colorScheme.onSurface)
                        )
                    }
                }
            }
        } else {
            items(
                items = groupedDisplayedItems,
                key = { group ->
                    if (!group.isAnnual && group.items.size == 1) "month_single_${group.items.first().entity.id}"
                    else "month_group_${group.groupKey}"
                }
            ) { group ->
                if (!group.isAnnual && group.items.size == 1) {
                    val singleItem = group.items.first()
                    ExpenseItemCard(
                        resolvedExpense = singleItem,
                        onDeleteClick = { viewModel.deleteExpense(singleItem.entity) },
                        onEditClick = { onEditExpense(singleItem.entity) }
                    )
                } else {
                    val isExpanded = expandedGroupKeys.contains(group.groupKey)
                    GroupedExpenseDropdownCard(
                        group = group,
                        isExpanded = isExpanded,
                        onToggleExpand = {
                            expandedGroupKeys = if (isExpanded) {
                                expandedGroupKeys - group.groupKey
                            } else {
                                expandedGroupKeys + group.groupKey
                            }
                        },
                        onEditItem = onEditExpense,
                        onDeleteItem = { entity -> viewModel.deleteExpense(entity) }
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(70.dp)) }
    }
}
