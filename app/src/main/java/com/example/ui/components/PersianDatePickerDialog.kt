package com.example.ui.components

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.PersianDateHelper

@Composable
fun PersianDatePickerDialog(
    initialYear: Int = PersianDateHelper.getShamsiDate().year,
    initialMonth: Int = PersianDateHelper.getShamsiDate().month,
    initialDay: Int = PersianDateHelper.getShamsiDate().day,
    onDateSelected: (year: Int, month: Int, day: Int, formatted: String) -> Unit,
    onDismissRequest: () -> Unit
) {
    var selectedYear by remember { mutableIntStateOf(initialYear) }
    var selectedMonth by remember { mutableIntStateOf(initialMonth) }
    var selectedDay by remember { mutableIntStateOf(initialDay) }

    val daysCount = PersianDateHelper.getDaysInMonth(selectedYear, selectedMonth)
    if (selectedDay > daysCount) {
        selectedDay = daysCount
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.testTag("persian_date_picker_dialog"),
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "انتخاب تاریخ شمسی",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                val mStr = if (selectedMonth < 10) "0$selectedMonth" else "$selectedMonth"
                val dStr = if (selectedDay < 10) "0$selectedDay" else "$selectedDay"
                Text(
                    text = PersianDateHelper.toPersianDigits("$selectedYear/$mStr/$dStr (${PersianDateHelper.PERSIAN_MONTH_NAMES[selectedMonth - 1]})"),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Year Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedYear++ }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "سال بعد")
                    }

                    Text(
                        text = "سال ${PersianDateHelper.toPersianDigits(selectedYear)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(onClick = { selectedYear-- }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "سال قبل")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Months scrollable tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedMonth - 1,
                    edgePadding = 8.dp,
                    containerColor = Color.Transparent,
                    divider = {}
                ) {
                    PersianDateHelper.PERSIAN_MONTH_NAMES.forEachIndexed { index, name ->
                        val isSelected = selectedMonth == index + 1
                        Tab(
                            selected = isSelected,
                            onClick = { selectedMonth = index + 1 },
                            text = {
                                Text(
                                    text = name,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Days Grid with Week Day Names Header
                val weekDayNames = PersianDateHelper.SHAMSI_WEEK_DAY_NAMES
                val startOffset = PersianDateHelper.getFirstDayOfWeekForShamsiMonth(selectedYear, selectedMonth)
                val totalCells = (0 until startOffset).map { null } + (1..daysCount).map { it }

                // Week days header row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    weekDayNames.forEachIndexed { index, wName ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = wName,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (index == 6) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(totalCells) { day ->
                        if (day == null) {
                            Box(modifier = Modifier.size(32.dp))
                        } else {
                            val isSelected = selectedDay == day
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else Color.Transparent
                                    )
                                    .border(
                                        width = if (isSelected) 0.dp else 1.dp,
                                        color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                        shape = CircleShape
                                    )
                                    .clickable { selectedDay = day },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = PersianDateHelper.toPersianDigits(day),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val m = if (selectedMonth < 10) "0$selectedMonth" else "$selectedMonth"
                    val d = if (selectedDay < 10) "0$selectedDay" else "$selectedDay"
                    onDateSelected(selectedYear, selectedMonth, selectedDay, "$selectedYear/$m/$d")
                },
                modifier = Modifier.testTag("confirm_date_button")
            ) {
                Text("تأیید تاریخ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("انصراف")
            }
        }
    )
}
