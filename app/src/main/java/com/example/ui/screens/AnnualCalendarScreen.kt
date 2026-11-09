package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ExpenseEntity
import com.example.data.model.MonthSummary
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.utils.PersianDateHelper

@Composable
fun AnnualCalendarScreen(
    viewModel: ExpenseViewModel,
    onAddExpenseInMonth: (monthIndex: Int) -> Unit,
    onEditExpense: (ExpenseEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMonthIndex by remember { mutableStateOf<Int?>(null) }

    if (selectedMonthIndex != null) {
        MonthDetailScreen(
            viewModel = viewModel,
            onBackClick = { selectedMonthIndex = null },
            onAddExpenseInMonth = onAddExpenseInMonth,
            onEditExpense = onEditExpense
        )
    } else {
        val currentYear by viewModel.currentYear.collectAsStateWithLifecycle()
        val monthSummaries by viewModel.yearMonthSummaries.collectAsStateWithLifecycle()
        val clockState by viewModel.clockState.collectAsStateWithLifecycle()
        val currentMonth = clockState.shamsiDate.month

        val highestMonthExpense = monthSummaries.maxOfOrNull { it.totalExpense }?.coerceAtLeast(1L) ?: 1L
        val totalYearExpenses = monthSummaries.sumOf { it.totalExpense }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(vertical = 4.dp)
                .testTag("annual_calendar_screen")
        ) {
            // Year Navigation Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF10B981)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous Year Button (Opposing Arrow on the right)
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable { viewModel.setYear(currentYear - 1) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "سال قبل",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // Current Year Title & Annual Total
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "سال ${PersianDateHelper.toPersianDigits(currentYear)}",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = PersianDateHelper.formatCurrency(totalYearExpenses),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Next Year Button (Opposing Arrow on the left)
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable { viewModel.setYear(currentYear + 1) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "سال بعد",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick Selectable Year Pills (allow jumping to any past year easily)
                    val currentCalendarYear = clockState.shamsiDate.year
                    val quickYears = listOf(
                        currentCalendarYear - 3,
                        currentCalendarYear - 2,
                        currentCalendarYear - 1,
                        currentCalendarYear,
                        currentCalendarYear + 1
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        quickYears.forEach { yr ->
                            val isSelected = yr == currentYear
                            val isThisYear = yr == currentCalendarYear
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            isThisYear -> MaterialTheme.colorScheme.primaryContainer
                                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        }
                                    )
                                    .clickable { viewModel.setYear(yr) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isThisYear && !isSelected) "${PersianDateHelper.toPersianDigits(yr)} (امسال)" else PersianDateHelper.toPersianDigits(yr),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = when {
                                        isSelected -> Color.White
                                        isThisYear -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 12 Persian Months Grid - 3 columns x 4 rows fits completely on screen
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                items(monthSummaries) { monthSummary ->
                    val isCurrent = monthSummary.monthIndex == currentMonth && currentYear == clockState.shamsiDate.year
                    val progress = (monthSummary.totalExpense.toFloat() / highestMonthExpense.toFloat()).coerceIn(0f, 1f)

                    CompactMonthCard(
                        summary = monthSummary,
                        isCurrent = isCurrent,
                        relativeProgress = progress,
                        onClick = {
                            viewModel.setSelectedMonth(monthSummary.monthIndex)
                            selectedMonthIndex = monthSummary.monthIndex
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CompactMonthCard(
    summary: MonthSummary,
    isCurrent: Boolean,
    relativeProgress: Float,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .clickable { onClick() }
            .testTag("month_card_${summary.monthIndex}"),
        shape = RoundedCornerShape(13.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.surface
        ),
        border = if (isCurrent) {
            androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF10B981))
        } else if (summary.totalExpense > 0) {
            androidx.compose.foundation.BorderStroke(1.4.dp, Color(0xFF0284C7))
        } else {
            androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF94A3B8))
        },
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 2.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 7.dp)
        ) {
            // Month name & number
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${PersianDateHelper.toPersianDigits(summary.monthIndex)}. ${summary.monthName}",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false
                )

                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "جاری",
                            color = Color.White,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Total amount
            Text(
                text = PersianDateHelper.formatCurrency(summary.totalExpense),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (summary.totalExpense > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Transaction count
            Text(
                text = "${PersianDateHelper.toPersianDigits(summary.transactionCount)} تراکنش",
                fontSize = 9.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Mini progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (relativeProgress > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(relativeProgress)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isCurrent) MaterialTheme.colorScheme.primary else Color(0xFF10B981))
                    )
                }
            }
        }
    }
}
