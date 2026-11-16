package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExpenseEntity
import com.example.data.model.ResolvedExpense
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.appSecondaryTextColor
import com.example.ui.theme.appTextColor
import com.example.ui.theme.enDigits
import com.example.ui.theme.scaledDp
import com.example.utils.PersianDateHelper

/**
 * Persian Monthly Expense Calendar Card for Home (Dashboard) Tab.
 * Displays all days of the current Shamsi month with registered daily expenses,
 * daily amounts, and an expandable detail pane upon tapping any day.
 */
@Composable
fun PersianMonthlyExpenseCalendarCard(
    currentShamsiYear: Int,
    currentShamsiMonth: Int,
    currentShamsiDay: Int,
    allExpenses: List<ResolvedExpense>,
    onExpenseClick: (ExpenseEntity) -> Unit,
    onAddExpenseForDate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var displayYear by remember { mutableIntStateOf(currentShamsiYear) }
    var displayMonth by remember { mutableIntStateOf(currentShamsiMonth) }
    var selectedDay by remember { mutableStateOf<Int?>(currentShamsiDay) }

    val monthName = PersianDateHelper.getMonthName(displayMonth)
    val daysCount = PersianDateHelper.getDaysInMonth(displayYear, displayMonth)
    val startOffset = PersianDateHelper.getFirstDayOfWeekForShamsiMonth(displayYear, displayMonth)

    // Filter expenses belonging to this year and month
    val monthExpenses = remember(allExpenses, displayYear, displayMonth) {
        allExpenses.filter { it.entity.shamsiYear == displayYear && it.entity.shamsiMonth == displayMonth }
    }

    val dayExpensesMap = remember(monthExpenses) {
        monthExpenses.groupBy { it.entity.shamsiDay }
    }

    val totalMonthExpense = remember(monthExpenses) {
        monthExpenses.sumOf { it.entity.amount }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("persian_monthly_calendar_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, Color(0xFF10B981)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.5.dp)
    ) {
        Column(modifier = Modifier.padding(scaledDp(14f))) {
            // Header: Month title, Navigator (Previous/Next Month), and Month Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFD1FAE5),
                        modifier = Modifier.size(scaledDp(34f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(scaledDp(18f))
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = enDigits("تقویم ماهانه مخارج ($monthName $displayYear)"),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = appTextColor(MaterialTheme.colorScheme.onSurface)
                        )
                        Text(
                            text = enDigits("مجموع ماه: ${PersianDateHelper.formatCurrency(totalMonthExpense)}"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF059669)
                        )
                    }
                }

                // Month Switcher Arrows
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Next Month
                    IconButton(
                        onClick = {
                            if (displayMonth < 12) {
                                displayMonth++
                            } else {
                                displayMonth = 1
                                displayYear++
                            }
                            selectedDay = null
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "ماه بعد",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Reset to Today button
                    if (displayYear != currentShamsiYear || displayMonth != currentShamsiMonth) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    displayYear = currentShamsiYear
                                    displayMonth = currentShamsiMonth
                                    selectedDay = currentShamsiDay
                                }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "امروز",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Previous Month
                    IconButton(
                        onClick = {
                            if (displayMonth > 1) {
                                displayMonth--
                            } else {
                                displayMonth = 12
                                displayYear--
                            }
                            selectedDay = null
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "ماه قبل",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            Spacer(modifier = Modifier.height(8.dp))

            // Weekday Names Header Row (Sat to Fri)
            val weekDays = listOf("شنبه", "1ش", "2ش", "3ش", "4ش", "5ش", "جمعه")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekDays.forEachIndexed { index, name ->
                    val isFriday = index == 6
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = enDigits(name),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isFriday) Color(0xFFEF4444) else appSecondaryTextColor(MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 7-Column Days Grid
            val totalCells = (0 until startOffset).map { null } + (1..daysCount).map { it }
            val rows = totalCells.chunked(7)

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                rows.forEach { rowCells ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (colIndex in 0 until 7) {
                            val day = rowCells.getOrNull(colIndex)
                            val isFriday = colIndex == 6
                            if (day == null) {
                                Spacer(modifier = Modifier.weight(1f))
                            } else {
                                val isToday = displayYear == currentShamsiYear && displayMonth == currentShamsiMonth && day == currentShamsiDay
                                val isSelected = selectedDay == day
                                val dayExpenses = dayExpensesMap[day] ?: emptyList()
                                val hasExpenses = dayExpenses.isNotEmpty()
                                val dayTotal = if (hasExpenses) dayExpenses.sumOf { it.entity.amount } else 0L

                                val cellBg = when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    isToday -> Color(0xFFE0F2FE)
                                    hasExpenses -> Color(0xFFECFDF5)
                                    else -> Color.Transparent
                                }

                                val cellBorder = when {
                                    isSelected -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                    isToday -> BorderStroke(1.5.dp, Color(0xFF0284C7))
                                    hasExpenses -> BorderStroke(1.dp, Color(0xFF34D399))
                                    else -> null
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(scaledDp(46f, strength = 0.7f, minFactor = 0.85f, maxFactor = 1.65f))
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(cellBg)
                                        .then(if (cellBorder != null) Modifier.border(cellBorder, RoundedCornerShape(8.dp)) else Modifier)
                                        .clickable {
                                            selectedDay = if (selectedDay == day) null else day
                                        }
                                        .padding(vertical = 3.dp, horizontal = 1.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = day.toString(),
                                            fontFamily = OutfitFontFamily,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected || isToday || hasExpenses) FontWeight.ExtraBold else FontWeight.Medium,
                                            color = when {
                                                isSelected -> Color.White
                                                isFriday -> Color(0xFFDC2626)
                                                isToday -> Color(0xFF0369A1)
                                                hasExpenses -> Color(0xFF065F46)
                                                else -> appTextColor(MaterialTheme.colorScheme.onSurface)
                                            }
                                        )

                                        if (hasExpenses) {
                                            val compactAmount = formatCompactDailyAmount(dayTotal)
                                            Text(
                                                text = enDigits(compactAmount),
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color(0xFFFDE047) else Color(0xFF059669),
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        } else {
                                            // subtle empty dot spacer
                                            Spacer(modifier = Modifier.height(scaledDp(10f)))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Expandable Day Detail Drawer
            AnimatedVisibility(
                visible = selectedDay != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                selectedDay?.let { day ->
                    val dayExpenses = dayExpensesMap[day] ?: emptyList()
                    val daySum = dayExpenses.sumOf { it.entity.amount }
                    val formattedDate = "$displayYear/${String.format("%02d", displayMonth)}/${String.format("%02d", day)}"

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        // Title and Close
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = enDigits("هزینه‌های روز $day $monthName $displayYear"),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = appTextColor(MaterialTheme.colorScheme.onSurface)
                                )
                                Text(
                                    text = enDigits(
                                        if (dayExpenses.isNotEmpty()) {
                                            "مجموع روز: ${PersianDateHelper.formatCurrency(daySum)} (${dayExpenses.size} تراکنش)"
                                        } else {
                                            "هیچ هزینه‌ای برای این روز ثبت نشده است"
                                        }
                                    ),
                                    fontSize = 11.sp,
                                    color = if (dayExpenses.isNotEmpty()) Color(0xFF059669) else appSecondaryTextColor(MaterialTheme.colorScheme.onSurfaceVariant),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            IconButton(
                                onClick = { selectedDay = null },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "بستن",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        if (dayExpenses.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                dayExpenses.forEach { resolved ->
                                    val expense = resolved.entity
                                    val folderName = resolved.effectiveFolder
                                    val folderColor = Color(0xFF10B981)

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onExpenseClick(expense) }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 7.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(folderColor)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Column {
                                                    Text(
                                                        text = expense.title,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = folderName,
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            Text(
                                                text = enDigits(PersianDateHelper.formatCurrency(expense.amount)),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF047857),
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick Add Button for this specific day
                        Button(
                            onClick = { onAddExpenseForDate(formattedDate) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(scaledDp(16f))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = enDigits("ثبت هزینه برای روز $day $monthName"),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Formats daily amount concisely for calendar cell display (e.g. 50k -> ۵۰ هزار, 1.2M -> ۱.۲ م).
 */
private fun formatCompactDailyAmount(amount: Long): String {
    return when {
        amount >= 1_000_000_000L -> {
            val b = amount / 1_000_000_000.0
            "${String.format(java.util.Locale.US, "%.1f", b)} م"
        }
        amount >= 1_000_000L -> {
            val m = amount / 1_000_000.0
            "${String.format(java.util.Locale.US, "%.1f", m)} م"
        }
        amount >= 1_000L -> {
            val k = amount / 1_000L
            "$k هـ"
        }
        else -> amount.toString()
    }
}
