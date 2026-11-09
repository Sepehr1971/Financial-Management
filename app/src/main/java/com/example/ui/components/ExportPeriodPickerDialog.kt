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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ResolvedExpense
import com.example.utils.PersianDateHelper

enum class ExportPeriodPreset(val label: String, val subtitle: String) {
    FULL_CURRENT_YEAR("کل سال جاری", "تمام ماه‌های سال جاری"),
    CURRENT_MONTH("این ماه", "هزینه‌های ماه جاری"),
    PREVIOUS_MONTH("ماه گذشته", "هزینه‌های ماه قبل"),
    LAST_3_MONTHS("۳ ماه اخیر", "بازه ۳ ماهه منتهی به امروز"),
    ALL_TIME("کل هزینه‌ها (از ابتدا تا کنون)", "تمام تراکنش‌های ثبت‌شده از ابتدا"),
    CUSTOM_RANGE("بازه دلخواه (انتخاب تاریخ)", "تعیین دقیق تاریخ شروع و پایان")
}

@Composable
fun ExportPeriodPickerDialog(
    exportFormatTitle: String,
    exportFormatIcon: ImageVector,
    exportFormatColor: Color,
    allExpenses: List<ResolvedExpense>,
    onDismissRequest: () -> Unit,
    onConfirmExport: (startDate: String, endDate: String, periodLabel: String) -> Unit
) {
    var selectedPreset by remember { mutableStateOf(ExportPeriodPreset.ALL_TIME) }
    val today = remember { PersianDateHelper.getShamsiDate() }

    // Date calculations
    val (defaultStart, defaultEnd) = remember(today) {
        val y = today.year
        val m = today.month
        val mStr = if (m < 10) "0$m" else "$m"
        val d = today.day
        val dStr = if (d < 10) "0$d" else "$d"

        val allDates = allExpenses.map { it.entity.shamsiDateString }.filter { it.isNotBlank() }.sorted()
        val earliest = allDates.firstOrNull() ?: "$y/01/01"
        val latest = allDates.lastOrNull() ?: "$y/$mStr/$dStr"
        Pair(earliest, latest)
    }

    var customStartDate by remember { mutableStateOf(defaultStart) }
    var customEndDate by remember { mutableStateOf(defaultEnd) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    // Calculate active start and end dates based on selected preset
    val effectiveDateRange by remember(selectedPreset, customStartDate, customEndDate, allExpenses) {
        derivedStateOf {
            val y = today.year
            val m = today.month
            val mStr = if (m < 10) "0$m" else "$m"
            val lastDayCurrentMonth = if (m <= 6) "31" else if (m <= 11) "30" else "29"

            when (selectedPreset) {
                ExportPeriodPreset.FULL_CURRENT_YEAR -> Pair("$y/01/01", "$y/12/29")
                ExportPeriodPreset.CURRENT_MONTH -> Pair("$y/$mStr/01", "$y/$mStr/$lastDayCurrentMonth")
                ExportPeriodPreset.PREVIOUS_MONTH -> {
                    var prevM = m - 1
                    var prevY = y
                    if (prevM <= 0) {
                        prevM = 12
                        prevY -= 1
                    }
                    val prevMStr = if (prevM < 10) "0$prevM" else "$prevM"
                    val lastDayPrev = if (prevM <= 6) "31" else if (prevM <= 11) "30" else "29"
                    Pair("$prevY/$prevMStr/01", "$prevY/$prevMStr/$lastDayPrev")
                }
                ExportPeriodPreset.LAST_3_MONTHS -> {
                    var startM = m - 2
                    var startY = y
                    if (startM <= 0) {
                        startM += 12
                        startY -= 1
                    }
                    val startMStr = if (startM < 10) "0$startM" else "$startM"
                    Pair("$startY/$startMStr/01", "$y/$mStr/$lastDayCurrentMonth")
                }
                ExportPeriodPreset.ALL_TIME -> {
                    val allDates = allExpenses.map { it.entity.shamsiDateString }.filter { it.isNotBlank() }.sorted()
                    val earliest = allDates.firstOrNull() ?: "$y/01/01"
                    val latest = allDates.lastOrNull() ?: "$y/12/29"
                    Pair(earliest, latest)
                }
                ExportPeriodPreset.CUSTOM_RANGE -> Pair(customStartDate, customEndDate)
            }
        }
    }

    // Matching items count and sum
    val matchingExpensesCount by remember(effectiveDateRange, allExpenses) {
        derivedStateOf {
            val (s, e) = effectiveDateRange
            allExpenses.count {
                it.entity.shamsiDateString.isNotBlank() &&
                        it.entity.shamsiDateString >= s &&
                        it.entity.shamsiDateString <= e
            }
        }
    }

    val matchingExpensesSum by remember(effectiveDateRange, allExpenses) {
        derivedStateOf {
            val (s, e) = effectiveDateRange
            allExpenses.filter {
                it.entity.shamsiDateString.isNotBlank() &&
                        it.entity.shamsiDateString >= s &&
                        it.entity.shamsiDateString <= e
            }.sumOf { it.entity.amount }
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 680.dp)
                .testTag("export_period_picker_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = exportFormatColor.copy(alpha = 0.15f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = exportFormatIcon,
                                    contentDescription = null,
                                    tint = exportFormatColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "انتخاب دوره گزارش $exportFormatTitle",
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "دوره زمانی مورد نظر برای صدور فایل را مشخص کنید",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))

                // Presets
                Text(
                    text = "دوره‌های پیش‌فرض گزارش:",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    ExportPeriodPreset.values().forEach { preset ->
                        val isSelected = selectedPreset == preset
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) exportFormatColor.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) exportFormatColor else Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedPreset = preset }
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
                                        text = preset.label,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) exportFormatColor else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = preset.subtitle,
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (isSelected) {
                                    Surface(
                                        shape = CircleShape,
                                        color = exportFormatColor,
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // If Custom Range is selected, show date picker fields
                if (selectedPreset == ExportPeriodPreset.CUSTOM_RANGE) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "تنظیم تاریخ دلخواه شروع و پایان:",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Start date button
                                OutlinedButton(
                                    onClick = { showStartDatePicker = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(text = "از تاریخ", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = PersianDateHelper.toPersianDigits(customStartDate), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // End date button
                                OutlinedButton(
                                    onClick = { showEndDatePicker = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(text = "تا تاریخ", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = PersianDateHelper.toPersianDigits(customEndDate), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Summary info of the selected period
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF0FDF4),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "از ${PersianDateHelper.toPersianDigits(effectiveDateRange.first)} تا ${PersianDateHelper.toPersianDigits(effectiveDateRange.second)}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D)
                                )
                                Text(
                                    text = "${PersianDateHelper.toPersianDigits(matchingExpensesCount)} تراکنش یافت شد",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF166534)
                                )
                            }
                        }

                        Text(
                            text = PersianDateHelper.formatCurrency(matchingExpensesSum),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF15803D)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions: Cancel & Confirm
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "انصراف", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            val (s, e) = effectiveDateRange
                            onConfirmExport(s, e, selectedPreset.label)
                            onDismissRequest()
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = exportFormatColor)
                    ) {
                        Icon(
                            imageVector = exportFormatIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تأیید و دریافت خروجی",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    if (showStartDatePicker) {
        val sClean = PersianDateHelper.toEnglishDigits(customStartDate)
        val parts = sClean.split("/")
        val y = parts.getOrNull(0)?.toIntOrNull() ?: today.year
        val m = parts.getOrNull(1)?.toIntOrNull() ?: today.month
        val d = parts.getOrNull(2)?.toIntOrNull() ?: 1

        PersianDatePickerDialog(
            initialYear = y,
            initialMonth = m,
            initialDay = d,
            onDateSelected = { _, _, _, formatted ->
                customStartDate = formatted
                showStartDatePicker = false
            },
            onDismissRequest = { showStartDatePicker = false }
        )
    }

    if (showEndDatePicker) {
        val eClean = PersianDateHelper.toEnglishDigits(customEndDate)
        val parts = eClean.split("/")
        val y = parts.getOrNull(0)?.toIntOrNull() ?: today.year
        val m = parts.getOrNull(1)?.toIntOrNull() ?: today.month
        val d = parts.getOrNull(2)?.toIntOrNull() ?: today.day

        PersianDatePickerDialog(
            initialYear = y,
            initialMonth = m,
            initialDay = d,
            onDateSelected = { _, _, _, formatted ->
                customEndDate = formatted
                showEndDatePicker = false
            },
            onDismissRequest = { showEndDatePicker = false }
        )
    }
}
