package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TableChart
import com.example.utils.ReportTableExporter
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ExpenseEntity
import com.example.data.model.FolderSummary
import com.example.data.repository.FolderMatcher
import com.example.ui.components.ExpenseItemCard
import com.example.ui.components.ExpensePieChartCard
import com.example.ui.components.ModernBarChart
import com.example.ui.components.MonthlyTrendsBarChart
import com.example.ui.components.PersianDatePickerDialog
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.ui.viewmodel.MonthlyBreakdownItem
import com.example.utils.PersianDateHelper

enum class ReportsTabSection(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    FOLDER_REPORTS("گزارش پوشه‌ها و فیلتر", Icons.Default.FilterList),
    ANNUAL_CALENDAR("تقویم سالانه ۱۲ ماه", Icons.Default.CalendarMonth)
}

@Composable
fun ReportsScreen(
    viewModel: ExpenseViewModel,
    onEditExpense: (ExpenseEntity) -> Unit,
    onAddExpenseInMonth: (monthIndex: Int) -> Unit = {},
    initialSection: ReportsTabSection = ReportsTabSection.FOLDER_REPORTS,
    modifier: Modifier = Modifier
) {
    var currentSection by remember(initialSection) { mutableStateOf(initialSection) }
    val context = LocalContext.current
    val filterState by viewModel.reportsFilter.collectAsStateWithLifecycle()
    val selectedFolder by viewModel.selectedReportFolder.collectAsStateWithLifecycle()
    val totalAmount by viewModel.reportTotalAmount.collectAsStateWithLifecycle()
    val folderTotalAmount by viewModel.folderReportTotalAmount.collectAsStateWithLifecycle()
    val folderExpenses by viewModel.folderReportExpenses.collectAsStateWithLifecycle()
    val monthlyBreakdown by viewModel.folderMonthlyBreakdown.collectAsStateWithLifecycle()
    val averageMonthlyAmount by viewModel.reportAverageMonthlyAmount.collectAsStateWithLifecycle()
    val customFoldersList by viewModel.customFolders.collectAsStateWithLifecycle()
    val folderSummaries by viewModel.reportFolderSummaries.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val allExpandedExpenses by viewModel.allExpandedExpenses.collectAsStateWithLifecycle()
    val yearMonthSummaries by viewModel.yearMonthSummaries.collectAsStateWithLifecycle()

    val filteredSearchResults = remember(searchQuery, allExpandedExpenses) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            val q = PersianDateHelper.toEnglishDigits(searchQuery)
                .lowercase()
                .replace("ي", "ی")
                .replace("ك", "ک")
                .replace("ة", "ه")
                .replace("آ", "ا")
                .replace("أ", "ا")
                .replace("إ", "ا")
                .trim()
            allExpandedExpenses.filter { item ->
                val titleNorm = item.entity.title.lowercase()
                    .replace("ي", "ی")
                    .replace("ك", "ک")
                    .replace("ة", "ه")
                    .replace("آ", "ا")
                    .replace("أ", "ا")
                    .replace("إ", "ا")
                val folderNorm = item.effectiveFolder.lowercase()
                    .replace("ي", "ی")
                    .replace("ك", "ک")
                    .replace("ة", "ه")
                    .replace("آ", "ا")
                    .replace("أ", "ا")
                    .replace("إ", "ا")
                val customFolderNorm = (item.entity.customFolder ?: "").lowercase()
                    .replace("ي", "ی")
                    .replace("ك", "ک")
                    .replace("ة", "ه")
                    .replace("آ", "ا")
                    .replace("أ", "ا")
                    .replace("إ", "ا")
                val noteNorm = item.entity.note.lowercase()
                    .replace("ي", "ی")
                    .replace("ك", "ک")
                    .replace("ة", "ه")
                    .replace("آ", "ا")
                    .replace("أ", "ا")
                    .replace("إ", "ا")

                titleNorm.contains(q) ||
                folderNorm.contains(q) ||
                customFolderNorm.contains(q) ||
                noteNorm.contains(q) ||
                item.entity.shamsiDateString.contains(q)
            }.sortedWith(
                compareByDescending<com.example.data.model.ResolvedExpense> { it.entity.shamsiDateString }
                    .thenByDescending { it.entity.timestamp }
            )
        }
    }
    val searchResultsTotal = remember(filteredSearchResults) {
        filteredSearchResults.sumOf { it.entity.amount }
    }

    val activeFolderSummaries = remember(folderSummaries) {
        folderSummaries.filter { fs ->
            val k = FolderMatcher.toKey(fs.folderName)
            !(k.contains("نگهدارىماشین") || k.contains("نگهدارىخودرو") ||
                (k.contains("نگهداری") && (k.contains("ماشین") || k.contains("خودرو"))) ||
                FolderMatcher.getCanonicalGroupId(fs.folderName) == "CAR_MAINTENANCE") &&
                fs.totalAmount > 0
        }.sortedByDescending { it.totalAmount }
    }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val presets = listOf(
        "این ماه",
        "ماه گذشته",
        "۳ ماه اخیر",
        "۴ ماه اخیر",
        "۵ ماه اخیر",
        "۶ ماه اخیر",
        "کل سال جاری"
    )

    // Combine distinct folder names from custom folders and folder summaries without duplicate tabs,
    // and sort them intelligently: Highest expense first, then descending, and folders with 0 expense at the end!
    val availableFolderNames = remember(customFoldersList, folderSummaries) {
        val rawNames = mutableListOf<String>()
        rawNames.addAll(customFoldersList.map { it.name })
        folderSummaries.forEach { fs ->
            rawNames.add(fs.folderName)
        }
        if ("اینترنت" !in rawNames) rawNames.add("اینترنت")

        // Map each canonical group to its total expense amount in the current period
        val groupAmounts = mutableMapOf<String, Long>()
        for (fs in folderSummaries) {
            val grp = FolderMatcher.getCanonicalGroupId(fs.folderName)
            groupAmounts[grp] = (groupAmounts[grp] ?: 0L) + fs.totalAmount
        }

        val seenGroups = mutableSetOf<String>()
        val distinctNames = mutableListOf<String>()
        for (name in rawNames) {
            val k = FolderMatcher.toKey(name)
            // Exclude "هزینه های نگهداری ماشین" / "هزینه های نگهداری خودرو" as requested by user
            if (k.contains("نگهدارىماشین") || k.contains("نگهدارىخودرو") ||
                (k.contains("نگهداری") && (k.contains("ماشین") || k.contains("خودرو"))) ||
                FolderMatcher.getCanonicalGroupId(name) == "CAR_MAINTENANCE"
            ) {
                continue
            }

            val group = FolderMatcher.getCanonicalGroupId(name)
            if (group !in seenGroups) {
                seenGroups.add(group)
                val displayName = if (group == "MOTOR" || k.contains("موتور")) {
                    FolderMatcher.UNIFIED_MOTOR_FOLDER_NAME
                } else {
                    val cleanedName = FolderMatcher.cleanFolderName(name)
                    if (cleanedName.isNotBlank() && FolderMatcher.getCanonicalGroupId(name) == FolderMatcher.getCanonicalGroupId(cleanedName)) {
                        cleanedName
                    } else {
                        name
                    }
                }
                distinctNames.add(displayName)
            }
        }

        // مرتب‌سازی هوشمند: هر پوشه‌ای که در این بازه هزینه بیشتری دارد اول، و پوشه‌های بدون هزینه در آخر
        distinctNames.sortedWith(
            compareByDescending<String> { name ->
                val grp = FolderMatcher.getCanonicalGroupId(name)
                groupAmounts[grp] ?: 0L
            }.thenBy { it }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
            .testTag("reports_screen")
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // Screen Title & Export Action (Export button placed horizontally underneath the main title)
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "گزارش‌گیری و تحلیل هزینه‌ها",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (currentSection == ReportsTabSection.FOLDER_REPORTS) "گزارش مستقل هر پوشه و نمودارها" else "تقویم سالانه ۱۲ ماه و تحلیل ماهانه",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 🔍 کادر جستجوی سریع هزینه‌ها (بالای کادرهای خروجی اکسل و PDF)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = {
                    Text(
                        text = "جستجوی هزینه‌ها",
                        fontSize = 13.sp,
                        maxLines = 1,
                        softWrap = false,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "جستجو",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "پاک کردن",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = Color(0xFF059669),
                    unfocusedBorderColor = Color(0xFF10B981).copy(alpha = 0.8f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp)
                    .testTag("reports_global_search_input")
            )

            // Buttons for Excel (.xlsx) and PDF Report Exports matching the reference accounting table
            if (currentSection == ReportsTabSection.FOLDER_REPORTS) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val exportExpenses = if (searchQuery.isNotBlank()) filteredSearchResults else folderExpenses
                    val exportFolderTitle = if (searchQuery.isNotBlank()) "جستجوی «$searchQuery»" else selectedFolder

                    // 1. خروجی اکسل (Excel .xlsx)
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = Color(0xFFE8FAF2),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF6EE7B7)),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .clickable {
                                if (exportExpenses.isEmpty()) {
                                    Toast.makeText(context, "هیچ هزینه‌ای برای خروجی گرفتن وجود ندارد", Toast.LENGTH_SHORT).show()
                                    return@clickable
                                }
                                Toast.makeText(context, "در حال ایجاد فایل اکسل...", Toast.LENGTH_SHORT).show()
                                val reportData = ReportTableExporter.processExpenses(
                                    expenses = exportExpenses,
                                    startDate = filterState.startDate,
                                    endDate = filterState.endDate,
                                    selectedFolder = exportFolderTitle
                                )
                                val xlsxBytes = ReportTableExporter.createExcelWorkbook(reportData)
                                val cleanStart = filterState.startDate.replace("/", "-")
                                val cleanEnd = filterState.endDate.replace("/", "-")
                                val fileName = if (searchQuery.isNotBlank()) {
                                    "Gozaresh_Hazineha_Jostojoo_${searchQuery.trim().replace(" ", "_")}.xlsx"
                                } else {
                                    "Gozaresh_Hazineha_${cleanStart}_ta_${cleanEnd}.xlsx"
                                }
                                ReportTableExporter.shareGeneratedFile(
                                    context = context,
                                    fileBytes = xlsxBytes,
                                    fileName = fileName,
                                    mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    chooserTitle = "اشتراک‌گذاری و باز کردن فایل اکسل"
                                )
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TableChart,
                                contentDescription = "خروجی Excel",
                                tint = Color(0xFF047857),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "اکسل نتایج (${filteredSearchResults.size})" else "خروجی Excel",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // 2. خروجی پی‌دی‌اف (PDF) با فونت B Mitra و خطوط مشخص جدول
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = Color(0xFFFFF1F2),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFFDA4AF)),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .clickable {
                                if (exportExpenses.isEmpty()) {
                                    Toast.makeText(context, "هیچ هزینه‌ای برای خروجی گرفتن وجود ندارد", Toast.LENGTH_SHORT).show()
                                    return@clickable
                                }
                                Toast.makeText(context, "در حال ایجاد فایل PDF...", Toast.LENGTH_SHORT).show()
                                val reportData = ReportTableExporter.processExpenses(
                                    expenses = exportExpenses,
                                    startDate = filterState.startDate,
                                    endDate = filterState.endDate,
                                    selectedFolder = exportFolderTitle
                                )
                                val pdfBytes = ReportTableExporter.createPdfDocument(context, reportData)
                                val cleanStart = filterState.startDate.replace("/", "-")
                                val cleanEnd = filterState.endDate.replace("/", "-")
                                val fileName = if (searchQuery.isNotBlank()) {
                                    "Gozaresh_Hazineha_Jostojoo_${searchQuery.trim().replace(" ", "_")}.pdf"
                                } else {
                                    "Gozaresh_Hazineha_${cleanStart}_ta_${cleanEnd}.pdf"
                                }
                                ReportTableExporter.shareGeneratedFile(
                                    context = context,
                                    fileBytes = pdfBytes,
                                    fileName = fileName,
                                    mimeType = "application/pdf",
                                    chooserTitle = "اشتراک‌گذاری و باز کردن فایل PDF"
                                )
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "خروجی PDF",
                                tint = Color(0xFFE11D48),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "PDF نتایج (${filteredSearchResults.size})" else "خروجی PDF",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF9F1239),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Section Switcher: [📊 گزارش پوشه‌ها و فیلتر] | [📅 تقویم سالانه ۱۲ ماه]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Tab 1: Folder Reports
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (currentSection == ReportsTabSection.FOLDER_REPORTS) MaterialTheme.colorScheme.primary
                        else Color.Transparent
                    )
                    .clickable { currentSection = ReportsTabSection.FOLDER_REPORTS }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        tint = if (currentSection == ReportsTabSection.FOLDER_REPORTS) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "گزارش پوشه‌ها و فیلتر",
                        fontSize = 11.5.sp,
                        fontWeight = if (currentSection == ReportsTabSection.FOLDER_REPORTS) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (currentSection == ReportsTabSection.FOLDER_REPORTS) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Tab 2: Annual Calendar
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (currentSection == ReportsTabSection.ANNUAL_CALENDAR) MaterialTheme.colorScheme.primary
                        else Color.Transparent
                    )
                    .clickable { currentSection = ReportsTabSection.ANNUAL_CALENDAR }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = if (currentSection == ReportsTabSection.ANNUAL_CALENDAR) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "تقویم سالانه ۱۲ ماه",
                        fontSize = 11.5.sp,
                        fontWeight = if (currentSection == ReportsTabSection.ANNUAL_CALENDAR) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (currentSection == ReportsTabSection.ANNUAL_CALENDAR) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        when (currentSection) {
            ReportsTabSection.FOLDER_REPORTS -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (searchQuery.isNotBlank()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                                border = androidx.compose.foundation.BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "نتایج جستجو برای: «$searchQuery»",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${PersianDateHelper.toPersianDigits(filteredSearchResults.size)} مورد هزینه یافت شد • جمع مبلغ: ${PersianDateHelper.formatCurrency(searchResultsTotal)}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    TextButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Text("پاک کردن", fontSize = 12.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        if (filteredSearchResults.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(28.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "هیچ هزینه‌ای منطبق با «$searchQuery» یافت نشد",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "می‌توانید کلمه دیگری مانند نام شخص، نام پوشه یا عنوان هزینه را جستجو کنید",
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        } else {
                            items(filteredSearchResults) { item ->
                                ExpenseItemCard(
                                    resolvedExpense = item,
                                    onDeleteClick = { viewModel.deleteExpense(item.entity) },
                                    onEditClick = { onEditExpense(item.entity) }
                                )
                            }
                        }

                        item { Spacer(modifier = Modifier.height(70.dp)) }
                    } else {
                        // Folder Selector Card (Half Thickness)
                        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, Color(0xFF0EA5E9)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "انتخاب پوشه برای گزارش‌گیری مستقل:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (selectedFolder != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { viewModel.selectReportFolder(null) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "لغو فیلتر",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "همه",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // "All Folders" chip
                        item {
                            val isAllSelected = selectedFolder == null
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isAllSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { viewModel.selectReportFolder(null) }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "📁 همه پوشه‌ها",
                                    fontSize = 11.sp,
                                    fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isAllSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Specific Folder chips (e.g. اینترنت، سوپرمارکت، بنزین، ...)
                        items(availableFolderNames) { fName ->
                            val isSelected = selectedFolder != null && FolderMatcher.isSameFolder(selectedFolder!!, fName)
                            val folderColor = customFoldersList.find { FolderMatcher.isSameFolder(it.name, fName) }?.let { Color(it.colorHex) }
                                ?: MaterialTheme.colorScheme.primary

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { viewModel.selectReportFolder(fName) }
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color.White else folderColor)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = fName,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Date Range Selector Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, Color(0xFF8B5CF6)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تعیین بازه زمانی گزارش:",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Preset chips (Includes ۴ ماه اخیر, ۵ ماه اخیر as requested!)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(presets) { preset ->
                            val isSelected = filterState.presetName == preset
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { viewModel.setDateRangePreset(preset) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = preset,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Custom Date Pickers Row (از تاریخ ... تا تاریخ ...)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Start Date Card
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showStartDatePicker = true },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                            border = BorderStroke(1.2.dp, Color(0xFF8B5CF6).copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "از تاریخ (شمسی):",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = PersianDateHelper.toPersianDigits(filterState.startDate),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // End Date Card
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showEndDatePicker = true },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                            border = BorderStroke(1.2.dp, Color(0xFF8B5CF6).copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "تا تاریخ (شمسی):",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = PersianDateHelper.toPersianDigits(filterState.endDate),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Summary Card for this Report (Compact & Proportionate)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                ),
                border = BorderStroke(1.5.dp, Color(0xFF10B981)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val title = if (selectedFolder != null) "گزارش اختصاصی «$selectedFolder»" else "جمع کل در این بازه:"
                        Text(
                            text = title,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val currentTotal = if (selectedFolder != null) folderTotalAmount else totalAmount
                        Text(
                            text = PersianDateHelper.formatCurrency(currentTotal),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${PersianDateHelper.toPersianDigits(folderExpenses.size)} تراکنش",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (selectedFolder != null) {
                            Text(
                                text = "میانگین: ${PersianDateHelper.formatCurrency(averageMonthlyAmount)}/ماه",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = "${PersianDateHelper.toPersianDigits(folderSummaries.size)} پوشه فعال",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // 📊 نمودار میله‌ای روند مخارج ماهانه و توزیع بودجه (Recharts-style Bar Chart)
        item {
            MonthlyTrendsBarChart(
                monthlyBreakdown = monthlyBreakdown,
                yearMonthSummaries = yearMonthSummaries,
                allExpenses = allExpandedExpenses,
                onEditExpense = onEditExpense,
                title = if (selectedFolder != null) "روند مخارج ماهانه «$selectedFolder»" else "نمودار میله‌ای روند مخارج ماهانه",
                subtitle = if (selectedFolder != null) "توزیع بودجه ماه به ماه پوشه انتخابی (با کلیک روی میله جزئیات ماه باز می‌شود)" else "توزیع بودجه و روند تغییرات ماه به ماه (با کلیک روی میله جزئیات ماه باز می‌شود)",
                modifier = Modifier.fillMaxWidth()
            )
        }

        // CRITICAL REQUIREMENT: Month-by-month breakdown (تفکیک ماه به ماه)
        // Shows e.g. how much Internet cost was in each month of the last 4-5 months!
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, Color(0xFFF43F5E)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ShowChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (selectedFolder != null) "تفکیک ماه به ماه پوشه «$selectedFolder»:" else "تفکیک ماه به ماه هزینه‌ها در این بازه:",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "${PersianDateHelper.toPersianDigits(monthlyBreakdown.size)} ماه",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (monthlyBreakdown.isEmpty()) {
                        Text(
                            text = "هزینه‌ای برای نمایش در این ماه‌ها ثبت نشده است",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        monthlyBreakdown.forEach { mb ->
                            MonthBreakdownRow(item = mb)
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }
        }

        if (selectedFolder == null) {
            // وقتی همه پوشه‌ها انتخاب است: هزینه هر پوشه + نمودار میله‌ای مقایسه مخارج پوشه‌ها
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, Color(0xFF0D9488)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "هزینه به تفکیک هر پوشه:",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "مجموع مخارج ثبت‌شده در هر پوشه (برای مشاهده ریز جزئیات روی پوشه بزنید)",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (activeFolderSummaries.isEmpty()) {
                            Text(
                                text = "هیچ هزینه‌ای در این بازه زمانی برای پوشه‌ها ثبت نشده است",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                activeFolderSummaries.forEach { folder ->
                                    val folderColor = Color(folder.colorHex)
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable { viewModel.selectReportFolder(folder.folderName) },
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.5.dp, folderColor),
                                        shape = RoundedCornerShape(14.dp),
                                        shadowElevation = 1.dp
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 11.dp)
                                        ) {
                                            // ردیف ۱: عنوان پوشه با دایره رنگی در سمت راست + فلش باز کردن در سمت چپ
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(12.dp)
                                                            .clip(CircleShape)
                                                            .background(folderColor)
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Text(
                                                        text = folder.folderName,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }

                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription = null,
                                                    tint = folderColor,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            // ردیف ۲ (یک خط زیر عنوان): مبلغ کل درشت و مشخص + تعداد تراکنش و درصد
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = PersianDateHelper.formatCurrency(folder.totalAmount),
                                                    fontSize = 14.5.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = folderColor,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )

                                                Text(
                                                    text = "${PersianDateHelper.toPersianDigits(folder.itemCount)} تراکنش • ${PersianDateHelper.toPersianDigits(folder.percentage.toInt())}٪ از کل",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    softWrap = false
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

            // نمودار میله‌ای مقایسه مخارج پوشه‌ها (بیشترین تا کمترین با یک نگاه)
            item {
                ModernBarChart(
                    folders = activeFolderSummaries,
                    title = "نمودار میله‌ای مقایسه مخارج پوشه‌ها (از بیشترین به کمترین)",
                    onFolderClick = { folder ->
                        viewModel.selectReportFolder(folder.folderName)
                    }
                )
            }
        } else {
            // وقتی یک پوشه خاص انتخاب شده است: نمایش ریز مخارج آن پوشه همراه با دکمه افقی در زیر عنوان اصلی
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "ریز مخارج پوشه «$selectedFolder» (${PersianDateHelper.toPersianDigits(folderExpenses.size)} تراکنش):",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // دکمه بازگشت افقی و خوانا در زیر عنوان اصلی (جلوگیری ۱۰۰٪ از شکستن عمودی حروف)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.selectReportFolder(null) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "بازگشت به همه پوشه‌ها",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "← بازگشت به نمایش همه پوشه‌ها و نمودار",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            // List of transactions in this specific folder
            if (folderExpenses.isEmpty()) {
                item {
                    Text(
                        text = "هیچ هزینه‌ای برای این پوشه در این بازه زمانی ثبت نشده است",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                items(folderExpenses) { item ->
                    ExpenseItemCard(
                        resolvedExpense = item,
                        onDeleteClick = { viewModel.deleteExpense(item.entity) },
                        onEditClick = { onEditExpense(item.entity) }
                    )
                }
            }
        }

        // پای‌چارت دایره‌ای (Pie Chart) در انتهای تب گزارش‌ها
        item {
            Spacer(modifier = Modifier.height(8.dp))
            ExpensePieChartCard(
                folderSummaries = activeFolderSummaries,
                totalAmount = totalAmount
            )
        }

        item { Spacer(modifier = Modifier.height(70.dp)) }
    }
}
}
ReportsTabSection.ANNUAL_CALENDAR -> {
    AnnualCalendarScreen(
        viewModel = viewModel,
        onAddExpenseInMonth = onAddExpenseInMonth,
        onEditExpense = onEditExpense,
        modifier = Modifier.fillMaxSize()
    )
}
}
}

    if (showStartDatePicker) {
        val sClean = PersianDateHelper.toEnglishDigits(filterState.startDate)
        val parts = sClean.split("/")
        val y = parts.getOrNull(0)?.toIntOrNull() ?: PersianDateHelper.getShamsiDate().year
        val m = parts.getOrNull(1)?.toIntOrNull() ?: PersianDateHelper.getShamsiDate().month
        val d = parts.getOrNull(2)?.toIntOrNull() ?: 1

        PersianDatePickerDialog(
            initialYear = y,
            initialMonth = m,
            initialDay = d,
            onDateSelected = { year, month, day, formatted ->
                viewModel.setCustomDateRange(formatted, filterState.endDate)
                showStartDatePicker = false
            },
            onDismissRequest = { showStartDatePicker = false }
        )
    }

    if (showEndDatePicker) {
        val eClean = PersianDateHelper.toEnglishDigits(filterState.endDate)
        val parts = eClean.split("/")
        val y = parts.getOrNull(0)?.toIntOrNull() ?: PersianDateHelper.getShamsiDate().year
        val m = parts.getOrNull(1)?.toIntOrNull() ?: PersianDateHelper.getShamsiDate().month
        val d = parts.getOrNull(2)?.toIntOrNull() ?: PersianDateHelper.getShamsiDate().day

        PersianDatePickerDialog(
            initialYear = y,
            initialMonth = m,
            initialDay = d,
            onDateSelected = { year, month, day, formatted ->
                viewModel.setCustomDateRange(filterState.startDate, formatted)
                showEndDatePicker = false
            },
            onDismissRequest = { showEndDatePicker = false }
        )
    }
}

@Composable
private fun MonthBreakdownRow(item: MonthlyBreakdownItem) {
    val progress = (item.percentageOfTotal / 100f).coerceIn(0f, 1f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.3.dp, Color(0xFFF43F5E).copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = item.yearMonthLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "${PersianDateHelper.toPersianDigits(item.transactionCount)} تراکنش",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = PersianDateHelper.formatCurrency(item.totalAmount),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (item.totalAmount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )
                if (item.totalAmount > 0) {
                    Text(
                        text = "${PersianDateHelper.toPersianDigits(item.percentageOfTotal.toInt())}٪ از کل بازه",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Visual progress bar of this month's share
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = progress.coerceAtLeast(0.02f))
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (item.totalAmount > 0) MaterialTheme.colorScheme.primary else Color.Transparent)
            )
        }
    }
}
}

private fun shareCsv(context: Context, csvContent: String) {
    try {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, csvContent)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "اشتراک‌گذاری گزارش هزینه‌ها")
        context.startActivity(shareIntent)
    } catch (e: Exception) {
        Toast.makeText(context, "خطا در اشتراک‌گذاری گزارش", Toast.LENGTH_SHORT).show()
    }
}
