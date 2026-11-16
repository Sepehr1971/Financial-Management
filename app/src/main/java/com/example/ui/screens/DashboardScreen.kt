package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.model.FolderSummary
import com.example.ui.components.CurrentMonthExpenseGrouper
import com.example.ui.components.ExpenseItemCard
import com.example.ui.components.GroupedExpenseDropdownCard
import com.example.ui.components.MonthlyExpenseOverviewCard
import com.example.ui.components.PersianDailyCalendarCard
import com.example.ui.theme.appSecondaryTextColor
import com.example.ui.theme.appTextColor
import com.example.ui.theme.enDigits
import com.example.ui.theme.scaledDp
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.utils.PersianDateHelper

@Composable
fun DashboardScreen(
    viewModel: ExpenseViewModel,
    onNavigateToCalendar: () -> Unit,
    onNavigateToFolders: () -> Unit,
    onNavigateToAnnual: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onEditExpense: (ExpenseEntity) -> Unit,
    onAddExpenseForDate: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val clockState by viewModel.clockState.collectAsStateWithLifecycle()
    val currentMonthSummary by viewModel.currentMonthSummary.collectAsStateWithLifecycle()
    val currentMonthResolved by viewModel.currentMonthResolvedExpenses.collectAsStateWithLifecycle()
    // Explicitly sorted from latest expense to earliest expense by expenditure date and creation time
    val sortedCurrentMonthExpenses = remember(currentMonthResolved) {
        currentMonthResolved.sortedWith(
            compareByDescending<com.example.data.model.ResolvedExpense> { viewModel.extractExpenseDateNum(it.entity) }
                .thenByDescending { it.entity.timestamp }
                .thenByDescending { it.entity.id }
        )
    }
    val groupedCurrentMonthExpenses = remember(sortedCurrentMonthExpenses) {
        CurrentMonthExpenseGrouper.groupExpenses(sortedCurrentMonthExpenses)
    }
    var expandedGroupKeys by rememberSaveable { mutableStateOf(listOf<String>()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // 1. تقویم روز (شمس، میلادی، ساعت زنده)
        item {
            PersianDailyCalendarCard(
                clockState = clockState
            )
        }

        // 2. هزینه کل ماه و هزینه‌های جاری
        item {
            MonthlyExpenseOverviewCard(
                clockState = clockState,
                currentMonthSummary = currentMonthSummary
            )
        }

        // Persian Annual Calendar Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 78.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onNavigateToCalendar() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, Color(0xFF10B981)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = scaledDp(14f), vertical = scaledDp(12f)),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(scaledDp(44f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(scaledDp(24f))
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = enDigits("تقویم سالانه ${clockState.shamsiDate.year}"),
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Black,
                                color = appTextColor(MaterialTheme.colorScheme.onPrimaryContainer)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = enDigits("مشاهده هزینه‌ها و تفکیک هر 12 ماه سال"),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = appSecondaryTextColor(MaterialTheme.colorScheme.onPrimaryContainer)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مشاهده تقویم ❯",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Recent Expenses List (Grouped by expense title with expandable dropdown details)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ریز هزینه‌های ماه جاری",
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Black,
                    color = appTextColor(MaterialTheme.colorScheme.onSurface),
                    softWrap = true,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                val countLabel = if (groupedCurrentMonthExpenses.size == sortedCurrentMonthExpenses.size) {
                    "${sortedCurrentMonthExpenses.size} مورد ثبت شده"
                } else {
                    "${groupedCurrentMonthExpenses.size} دسته (${sortedCurrentMonthExpenses.size} مورد)"
                }

                Text(
                    text = enDigits(countLabel),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = appSecondaryTextColor(MaterialTheme.colorScheme.onSurfaceVariant),
                    softWrap = true
                )
            }
        }

        if (groupedCurrentMonthExpenses.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "در این ماه هنوز هزینه‌ای ثبت نکرده‌اید",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(onClick = onAddExpenseClick) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ثبت اولین هزینه", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        } else {
            items(
                items = groupedCurrentMonthExpenses,
                key = { group ->
                    if (!group.isAnnual && group.items.size == 1) "single_${group.items.first().entity.id}" else "group_${group.groupKey}"
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

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun FolderMiniBadge(
    folder: FolderSummary,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(folder.colorHex))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = folder.folderName,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 4,
                    softWrap = true
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = enDigits(PersianDateHelper.formatCurrency(folder.totalAmount)),
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                softWrap = true
            )
        }
    }
}
