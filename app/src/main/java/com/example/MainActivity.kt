package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ExpenseEntity
import com.example.ui.components.AddExpenseBottomSheet
import com.example.ui.components.SettingsDialog
import com.example.ui.screens.AnnualCalendarScreen
import com.example.ui.screens.AnnualCommitmentsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.MonthDetailScreen
import com.example.ui.screens.NewsScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.ReportsTabSection
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ExpenseViewModel
import androidx.compose.material.icons.filled.Calculate
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.graphics.Color
import com.example.ui.components.ExportPeriodPickerDialog
import com.example.utils.BackupRestoreHelper
import com.example.utils.PersianDateHelper
import com.example.utils.ReportTableExporter

enum class ScreenTab(val title: String, val icon: ImageVector) {
    DASHBOARD("خانه", Icons.Default.Home),
    REPORTS("گزارش‌ها", Icons.Default.QueryStats),
    TOOLS("امکانات", Icons.Default.Calculate),
    ANNUAL("تعهدات سالانه", Icons.Default.Shield),
    NEWS("اخبار", Icons.Default.Newspaper)
}

class MainActivity : ComponentActivity() {

    private val viewModel: ExpenseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.decorView.isVerticalScrollBarEnabled = false
        window.decorView.isHorizontalScrollBarEnabled = false

        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDarkMode by remember { mutableStateOf(systemDark) }

            MyApplicationTheme(darkTheme = isDarkMode) {
                // Persian RTL Direction Layout Provider
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    ExpenseTrackerApp(
                        viewModel = viewModel,
                        isDarkMode = isDarkMode,
                        onToggleTheme = { isDarkMode = !isDarkMode }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseTrackerApp(
    viewModel: ExpenseViewModel,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit
) {
    val context = LocalContext.current
    val allExpenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val customFolders by viewModel.customFolders.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(ScreenTab.DASHBOARD) }
    var reportsInitialSubTab by remember { mutableStateOf(ReportsTabSection.FOLDER_REPORTS) }
    var showAddBottomSheet by remember { mutableStateOf(false) }
    var isAnnualPreselected by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<com.example.data.model.ExpenseEntity?>(null) }
    var preselectedDateForAdd by remember { mutableStateOf<String?>(null) }
    var preselectedAmountForAdd by remember { mutableStateOf<Long?>(null) }

    var showExportPeriodFor by remember { mutableStateOf<String?>(null) }
    var pendingRestoreData by remember { mutableStateOf<BackupRestoreHelper.BackupData?>(null) }

    val restoreFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val jsonContent = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader(Charsets.UTF_8).readText()
                }
                if (!jsonContent.isNullOrBlank()) {
                    val backup = BackupRestoreHelper.parseBackupJson(jsonContent)
                    if (backup != null && (backup.expenses.isNotEmpty() || backup.customFolders.isNotEmpty())) {
                        pendingRestoreData = backup
                    } else {
                        Toast.makeText(context, "فایل انتخاب‌شده معتبر نیست یا اطلاعاتی در آن یافت نشد", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "خطا در خواندن فایل پشتیبان: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "مدیریت مالی و هزینه‌ها",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    // Settings button with reset option
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "تنظیمات",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Theme toggle button
                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier.testTag("toggle_theme_button")
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "تغییر تم",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            )
        },
        bottomBar = {
            androidx.compose.foundation.layout.Column {
                androidx.compose.material3.HorizontalDivider(
                    color = Color(0xFF10B981).copy(alpha = 0.35f),
                    thickness = 1.dp
                )
                NavigationBar(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("main_bottom_nav"),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                ScreenTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (tab == ScreenTab.REPORTS && currentTab != ScreenTab.REPORTS) {
                                reportsInitialSubTab = ReportsTabSection.FOLDER_REPORTS
                            }
                            currentTab = tab
                        },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingExpense = null
                    preselectedDateForAdd = PersianDateHelper.getShamsiDate().format()
                    showAddBottomSheet = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_expense_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "ثبت هزینه جدید",
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                ScreenTab.DASHBOARD -> {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToCalendar = {
                            reportsInitialSubTab = ReportsTabSection.ANNUAL_CALENDAR
                            currentTab = ScreenTab.REPORTS
                        },
                        onNavigateToFolders = {
                            currentTab = ScreenTab.NEWS
                        },
                        onNavigateToAnnual = {
                            currentTab = ScreenTab.ANNUAL
                        },
                        onAddExpenseClick = {
                            editingExpense = null
                            preselectedDateForAdd = PersianDateHelper.getShamsiDate().format()
                            showAddBottomSheet = true
                        },
                        onEditExpense = { entity ->
                            editingExpense = viewModel.getOriginalExpenseForEdit(entity)
                            showAddBottomSheet = true
                        }
                    )
                }
                ScreenTab.REPORTS -> {
                    ReportsScreen(
                        viewModel = viewModel,
                        onEditExpense = { entity ->
                            editingExpense = viewModel.getOriginalExpenseForEdit(entity)
                            showAddBottomSheet = true
                        },
                        onAddExpenseInMonth = { monthIndex ->
                            editingExpense = null
                            val currentYear = PersianDateHelper.getShamsiDate().year
                            val mStr = if (monthIndex < 10) "0$monthIndex" else "$monthIndex"
                            preselectedDateForAdd = "$currentYear/$mStr/01"
                            showAddBottomSheet = true
                        },
                        initialSection = reportsInitialSubTab
                    )
                }
                ScreenTab.TOOLS -> {
                    ToolsScreen(
                        viewModel = viewModel,
                        onAddExpenseWithAmount = { amount ->
                            editingExpense = null
                            preselectedAmountForAdd = amount
                            preselectedDateForAdd = PersianDateHelper.getShamsiDate().format()
                            showAddBottomSheet = true
                        }
                    )
                }
                ScreenTab.ANNUAL -> {
                    AnnualCommitmentsScreen(
                        viewModel = viewModel,
                        onAddAnnualExpenseClick = {
                            editingExpense = null
                            isAnnualPreselected = true
                            preselectedDateForAdd = PersianDateHelper.getShamsiDate().format()
                            showAddBottomSheet = true
                        },
                        onEditAnnualExpense = { entity ->
                            editingExpense = viewModel.getOriginalExpenseForEdit(entity)
                            isAnnualPreselected = true
                            showAddBottomSheet = true
                        }
                    )
                }
                ScreenTab.NEWS -> {
                    NewsScreen(viewModel = viewModel)
                }
            }
        }
    }

    if (showAddBottomSheet) {
        AddExpenseBottomSheet(
            onDismissRequest = {
                showAddBottomSheet = false
                isAnnualPreselected = false
                editingExpense = null
                preselectedAmountForAdd = null
            },
            initialShamsiDate = preselectedDateForAdd ?: PersianDateHelper.getShamsiDate().format(),
            expenseToEdit = editingExpense,
            initialIsAnnual = isAnnualPreselected || (editingExpense?.isAnnual == true),
            initialAmount = preselectedAmountForAdd,
            onAddExpense = { title, amount, isAnnual, customFolder, dateStr, note ->
                viewModel.addExpense(
                    title = title,
                    amount = amount,
                    isAnnual = isAnnual,
                    customFolder = customFolder,
                    shamsiDateString = dateStr,
                    note = note
                )
            },
            onUpdateExpense = { updated ->
                viewModel.updateExpense(updated)
                editingExpense = null
            },
            availableFolders = customFolders.map { it.name }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            onDismissRequest = { showSettingsDialog = false },
            onResetData = {
                viewModel.clearAllData()
                Toast.makeText(context, "تمامی داده‌ها پاک و برنامه ریست شد", Toast.LENGTH_SHORT).show()
            },
            onBackupData = {
                val jsonBackup = viewModel.createFullBackupJson()
                BackupRestoreHelper.shareBackupFile(context, jsonBackup)
            },
            onRestoreData = {
                try {
                    restoreFileLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                } catch (_: Exception) {
                    restoreFileLauncher.launch(arrayOf("*/*"))
                }
            },
            totalExpensesCount = allExpenses.size
        )
    }

    if (showExportPeriodFor != null) {
        val isExcel = showExportPeriodFor == "excel"
        ExportPeriodPickerDialog(
            exportFormatTitle = if (isExcel) "اکسل (Excel .xlsx)" else "پی‌دی‌اف (PDF)",
            exportFormatIcon = if (isExcel) Icons.Default.TableChart else Icons.Default.PictureAsPdf,
            exportFormatColor = if (isExcel) Color(0xFF047857) else Color(0xFFE11D48),
            allExpenses = viewModel.allExpandedExpenses.collectAsStateWithLifecycle().value,
            onDismissRequest = { showExportPeriodFor = null },
            onConfirmExport = { startDate, endDate, periodLabel ->
                val allItems = viewModel.allExpandedExpenses.value
                val filtered = allItems.filter {
                    it.entity.shamsiDateString.isNotBlank() &&
                            it.entity.shamsiDateString >= startDate &&
                            it.entity.shamsiDateString <= endDate
                }
                if (filtered.isEmpty()) {
                    Toast.makeText(context, "هیچ هزینه‌ای در دوره $periodLabel ($startDate تا $endDate) یافت نشد", Toast.LENGTH_LONG).show()
                } else {
                    val reportData = ReportTableExporter.processExpenses(
                        expenses = filtered,
                        startDate = startDate,
                        endDate = endDate,
                        selectedFolder = null
                    )
                    val cleanStart = startDate.replace("/", "-")
                    val cleanEnd = endDate.replace("/", "-")
                    if (isExcel) {
                        Toast.makeText(context, "در حال ایجاد فایل اکسل برای دوره $periodLabel...", Toast.LENGTH_SHORT).show()
                        val xlsxBytes = ReportTableExporter.createExcelWorkbook(reportData)
                        ReportTableExporter.shareGeneratedFile(
                            context = context,
                            fileBytes = xlsxBytes,
                            fileName = "Gozaresh_Hazineha_${cleanStart}_ta_${cleanEnd}.xlsx",
                            mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            chooserTitle = "اشتراک‌گذاری و باز کردن فایل اکسل"
                        )
                    } else {
                        Toast.makeText(context, "در حال ایجاد فایل PDF برای دوره $periodLabel...", Toast.LENGTH_SHORT).show()
                        val pdfBytes = ReportTableExporter.createPdfDocument(context, reportData)
                        ReportTableExporter.shareGeneratedFile(
                            context = context,
                            fileBytes = pdfBytes,
                            fileName = "Gozaresh_Hazineha_${cleanStart}_ta_${cleanEnd}.pdf",
                            mimeType = "application/pdf",
                            chooserTitle = "اشتراک‌گذاری و باز کردن فایل PDF"
                        )
                    }
                }
            }
        )
    }

    if (pendingRestoreData != null) {
        val backup = pendingRestoreData!!
        AlertDialog(
            onDismissRequest = { pendingRestoreData = null },
            title = {
                Text(
                    text = "تأیید بازیابی اطلاعات (Restore)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "فایل پشتیبان با موفقیت خوانده شد:\n" +
                            "• ${PersianDateHelper.toPersianDigits(backup.expenses.size)} هزینه ثبت‌شده\n" +
                            "• ${PersianDateHelper.toPersianDigits(backup.customFolders.size)} پوشه و ساختار برنامه\n" +
                            "• تاریخ فایل پشتیبان: ${PersianDateHelper.toPersianDigits(backup.backupDateShamsi)}\n\n" +
                            "آیا مایلید تمام داده‌های فوق بازیابی شوند؟ (اطلاعات فعلی با داده‌های این فایل پشتیبان جایگزین خواهند شد)",
                    fontSize = 12.5.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreBackup(
                            expenses = backup.expenses,
                            customFolders = backup.customFolders,
                            replaceExisting = true
                        ) { success, expCount, foldCount ->
                            if (success) {
                                Toast.makeText(context, "اطلاعات با موفقیت بازیابی شد ($expCount هزینه و $foldCount پوشه)", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "خطا در بازیابی اطلاعات", Toast.LENGTH_LONG).show()
                            }
                        }
                        pendingRestoreData = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                ) {
                    Text("بله، بازیابی شود")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingRestoreData = null }) {
                    Text("انصراف")
                }
            }
        )
    }
}
