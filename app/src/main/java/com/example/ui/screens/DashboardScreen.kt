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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ExpenseEntity
import com.example.data.model.FolderSummary
import com.example.ui.components.ExpenseItemCard
import com.example.ui.components.MarketRatesCard
import com.example.ui.components.ModernBarChart
import com.example.ui.components.MonthlyExpenseOverviewCard
import com.example.ui.components.PersianDailyCalendarCard
import com.example.ui.components.PersianLiveClockHeader
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clockState by viewModel.clockState.collectAsStateWithLifecycle()
    val currentMonthSummary by viewModel.currentMonthSummary.collectAsStateWithLifecycle()
    val currentMonthResolved by viewModel.currentMonthResolvedExpenses.collectAsStateWithLifecycle()
    val annualExpenses by viewModel.annualExpenses.collectAsStateWithLifecycle()
    val allTimeFolders by viewModel.allTimeFolderSummaries.collectAsStateWithLifecycle()
    val marketRatesState by viewModel.marketRates.collectAsStateWithLifecycle()

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

        // 2. هزینه کل ماه و هزینه‌های جاری (بالاتر از قیمت ارزها و سکه‌ها)
        item {
            MonthlyExpenseOverviewCard(
                clockState = clockState,
                currentMonthSummary = currentMonthSummary
            )
        }

        // 3. قیمت روز ارز و سکه (TGJU)
        item {
            MarketRatesCard(
                ratesState = marketRatesState,
                onRefresh = {
                    Toast.makeText(context, "در حال به‌روزرسانی نرخ‌های بازار...", Toast.LENGTH_SHORT).show()
                    viewModel.refreshMarketRates {
                        Toast.makeText(context, "قیمت‌های ارز و سکه به‌روزرسانی شد", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        // Persian Annual Calendar Card (Increased to 3x height: 78dp)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(78.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onNavigateToCalendar() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, Color(0xFF10B981)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
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
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "تقویم سالانه ${PersianDateHelper.toPersianDigits(clockState.shamsiDate.year)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "مشاهده هزینه‌ها و تفکیک هر ۱۲ ماه سال",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
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

        // Recent Expenses List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ریز هزینه‌های ماه جاری",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "${PersianDateHelper.toPersianDigits(currentMonthResolved.size)} مورد ثبت شده",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (currentMonthResolved.isEmpty()) {
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
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(onClick = onAddExpenseClick) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ثبت اولین هزینه")
                        }
                    }
                }
            }
        } else {
            items(
                items = currentMonthResolved,
                key = { it.entity.id }
            ) { item ->
                ExpenseItemCard(
                    resolvedExpense = item,
                    onDeleteClick = { viewModel.deleteExpense(item.entity) },
                    onEditClick = { onEditExpense(item.entity) }
                )
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
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    softWrap = true
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = PersianDateHelper.formatCurrency(folder.totalAmount),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                softWrap = true
            )
        }
    }
}
