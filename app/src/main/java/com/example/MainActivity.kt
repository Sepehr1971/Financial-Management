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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Mic
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
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.example.ui.components.ExportPeriodPickerDialog
import com.example.ui.components.VoiceExpenseDialog
import com.example.ui.theme.LocalAppFontColorOption
import com.example.ui.theme.LocalAppFontScale
import com.example.ui.theme.ScaledDialogContent
import com.example.ui.theme.enDigits
import com.example.utils.AppFontColorOption
import com.example.utils.BackupRestoreHelper
import com.example.utils.FontScaleHelper
import com.example.utils.GitHubUpdateHelper
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
            var isDarkMode by rememberSaveable { mutableStateOf(false) }
            var fontScale by remember { mutableFloatStateOf(FontScaleHelper.getSavedFontScale(this)) }
            var fontColorOption by remember { mutableStateOf(FontScaleHelper.getSavedFontColor(this)) }
            val baseDensity = LocalDensity.current
            val scaledDensity = remember(baseDensity.density, baseDensity.fontScale, fontScale) {
                Density(
                    density = baseDensity.density,
                    fontScale = baseDensity.fontScale * fontScale
                )
            }

            MyApplicationTheme(
                darkTheme = isDarkMode,
                fontColorOption = fontColorOption
            ) {
                // Persian RTL Direction Layout Provider + Proportional Font & Container Scaling + Font Color
                CompositionLocalProvider(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                    LocalDensity provides scaledDensity,
                    LocalAppFontScale provides fontScale,
                    LocalAppFontColorOption provides fontColorOption
                ) {
                    ExpenseTrackerApp(
                        viewModel = viewModel,
                        isDarkMode = isDarkMode,
                        onToggleTheme = { isDarkMode = !isDarkMode },
                        fontScale = fontScale,
                        onFontScaleChange = { newScale ->
                            fontScale = FontScaleHelper.saveFontScale(this, newScale)
                        },
                        fontColorOption = fontColorOption,
                        onFontColorChange = { newColor ->
                            fontColorOption = FontScaleHelper.saveFontColor(this, newColor)
                        }
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
    onToggleTheme: () -> Unit,
    fontScale: Float = FontScaleHelper.DEFAULT_SCALE,
    onFontScaleChange: (Float) -> Unit = {},
    fontColorOption: AppFontColorOption = AppFontColorOption.DEFAULT,
    onFontColorChange: (AppFontColorOption) -> Unit = {}
) {
    val context = LocalContext.current
    val allExpenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val customFolders by viewModel.customFolders.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(ScreenTab.DASHBOARD) }
    var reportsInitialSubTab by rememberSaveable { mutableStateOf(ReportsTabSection.FOLDER_REPORTS) }
    var showAddBottomSheet by remember { mutableStateOf(false) }
    var showVoiceExpenseDialog by remember { mutableStateOf(false) }
    var isAnnualPreselected by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<com.example.data.model.ExpenseEntity?>(null) }
    var preselectedDateForAdd by remember { mutableStateOf<String?>(null) }
    var preselectedAmountForAdd by remember { mutableStateOf<Long?>(null) }
    var preselectedTitleForAdd by remember { mutableStateOf<String?>(null) }
    var preselectedReceiptPathForAdd by remember { mutableStateOf<String?>(null) }

    var showExportPeriodFor by remember { mutableStateOf<String?>(null) }
    var pendingRestoreData by remember { mutableStateOf<BackupRestoreHelper.BackupData?>(null) }

    LaunchedEffect(Unit) {
        val savedRepo = GitHubUpdateHelper.getSavedRepoInput(context)
        if (GitHubUpdateHelper.isAutoCheckAndDownloadEnabled(context) &&
            GitHubUpdateHelper.parseOwnerAndRepo(savedRepo) != null
        ) {
            val result = GitHubUpdateHelper.checkForUpdate(savedRepo)
            if (result.isUpdateAvailable && result.downloadUrl.isNotBlank()) {
                GitHubUpdateHelper.openDownloadUrl(context, result.downloadUrl)
                Toast.makeText(
                    context,
                    "نسخه جدید (${result.latestVersion}) در گیتهاب یافت شد و دانلود آغاز گردید",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

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
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = com.example.ui.theme.appTextColor(MaterialTheme.colorScheme.onBackground)
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
            val navGradient = if (isDarkMode) {
                listOf(
                    Color(0xFF0F172A),
                    Color(0xFF1E1B4B),
                    Color(0xFF134E4A)
                )
            } else {
                listOf(
                    Color(0xFF312E81), // Royal Indigo
                    Color(0xFF1E40AF), // Deep Sapphire Blue
                    Color(0xFF0E7490)  // Luminous Ocean Teal
                )
            }
            val topHighlightBrush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                listOf(
                    Color(0xFF34D399),
                    Color(0xFF38BDF8),
                    Color(0xFFFBBF24),
                    Color(0xFFA78BFA)
                )
            )
            androidx.compose.material3.Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_bottom_nav"),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                shadowElevation = 12.dp,
                color = Color.Transparent
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(androidx.compose.ui.graphics.Brush.horizontalGradient(navGradient))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .background(topHighlightBrush)
                    )
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val uiScale = LocalAppFontScale.current
                        ScreenTab.values().forEach { tab ->
                            val isSelected = currentTab == tab
                            val activePillColor = when (tab) {
                                ScreenTab.DASHBOARD -> Color(0xFF10B981) // Emerald
                                ScreenTab.REPORTS -> Color(0xFF0EA5E9)   // Sky Cyan
                                ScreenTab.TOOLS -> Color(0xFFF59E0B)     // Radiant Gold
                                ScreenTab.ANNUAL -> Color(0xFFA855F7)    // Amethyst Violet
                                ScreenTab.NEWS -> Color(0xFFF43F5E)      // Coral Rose
                            }
                            val activeContentColor = if (tab == ScreenTab.TOOLS) Color(0xFF1E1B4B) else Color.White
                            val itemInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                            val navMinHeight = (56f * uiScale).coerceAtLeast(48f).dp
                            val navIconSize = ((if (isSelected) 23f else 21f) * uiScale).coerceIn(18f, 28f).dp
                            val navVerticalPad = (6f * uiScale).coerceIn(4f, 10f).dp

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = navMinHeight)
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                                    .then(
                                        if (isSelected) {
                                            Modifier
                                                .background(activePillColor)
                                                .border(
                                                    1.2.dp,
                                                    Color.White.copy(alpha = 0.75f),
                                                    androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                                                )
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable(
                                        interactionSource = itemInteraction,
                                        indication = androidx.compose.material3.ripple(color = Color.White),
                                        onClick = {
                                            if (tab == ScreenTab.REPORTS && currentTab != ScreenTab.REPORTS) {
                                                reportsInitialSubTab = ReportsTabSection.FOLDER_REPORTS
                                            }
                                            currentTab = tab
                                        }
                                    )
                                    .padding(horizontal = 3.dp, vertical = navVerticalPad)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.title,
                                        tint = if (isSelected) activeContentColor else Color(0xFFE0E7FF),
                                        modifier = Modifier.size(navIconSize)
                                    )
                                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = tab.title,
                                        fontSize = if (tab == ScreenTab.ANNUAL) 10.sp else 11.sp,
                                        lineHeight = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.ExtraBold,
                                        color = if (isSelected) activeContentColor else Color.White,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = { showVoiceExpenseDialog = true },
                    containerColor = Color(0xFF0EA5E9),
                    contentColor = Color.White,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("voice_expense_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "ثبت صوتی هزینه",
                        modifier = Modifier.size(22.dp)
                    )
                }

                FloatingActionButton(
                    onClick = {
                        editingExpense = null
                        preselectedTitleForAdd = null
                        preselectedAmountForAdd = null
                        preselectedReceiptPathForAdd = null
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
                            preselectedReceiptPathForAdd = null
                            preselectedDateForAdd = PersianDateHelper.getShamsiDate().format()
                            showAddBottomSheet = true
                        },
                        onAddExpenseForDate = { dateStr ->
                            editingExpense = null
                            preselectedReceiptPathForAdd = null
                            preselectedDateForAdd = dateStr
                            showAddBottomSheet = true
                        },
                        onEditExpense = { entity ->
                            preselectedReceiptPathForAdd = null
                            editingExpense = viewModel.getOriginalExpenseForEdit(entity)
                            showAddBottomSheet = true
                        }
                    )
                }
                ScreenTab.REPORTS -> {
                    ReportsScreen(
                        viewModel = viewModel,
                        onEditExpense = { entity ->
                            preselectedReceiptPathForAdd = null
                            editingExpense = viewModel.getOriginalExpenseForEdit(entity)
                            showAddBottomSheet = true
                        },
                        onAddExpenseInMonth = { monthIndex ->
                            editingExpense = null
                            preselectedReceiptPathForAdd = null
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
                            preselectedReceiptPathForAdd = null
                            preselectedAmountForAdd = amount
                            preselectedDateForAdd = PersianDateHelper.getShamsiDate().format()
                            showAddBottomSheet = true
                        },
                        onAddExpenseWithReceipt = { receiptPath ->
                            editingExpense = null
                            preselectedTitleForAdd = null
                            preselectedAmountForAdd = null
                            preselectedReceiptPathForAdd = receiptPath
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
                            preselectedReceiptPathForAdd = null
                            isAnnualPreselected = true
                            preselectedDateForAdd = PersianDateHelper.getShamsiDate().format()
                            showAddBottomSheet = true
                        },
                        onEditAnnualExpense = { entity ->
                            preselectedReceiptPathForAdd = null
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

    if (showVoiceExpenseDialog) {
        VoiceExpenseDialog(
            onDismissRequest = { showVoiceExpenseDialog = false },
            onDirectSaveExpense = { parsed ->
                viewModel.addExpense(
                    title = parsed.title,
                    amount = parsed.amountTomans,
                    isAnnual = parsed.isAnnual,
                    customFolder = null,
                    shamsiDateString = parsed.shamsiDateString,
                    note = ""
                )
                Toast.makeText(
                    context,
                    "هزینه «${parsed.title}» (${PersianDateHelper.formatCurrency(parsed.amountTomans)}) ثبت شد",
                    Toast.LENGTH_LONG
                ).show()
                showVoiceExpenseDialog = false
            },
            onApplyToForm = { parsed ->
                editingExpense = null
                preselectedReceiptPathForAdd = null
                preselectedTitleForAdd = parsed.title.takeIf { it.isNotBlank() }
                preselectedAmountForAdd = parsed.amountTomans.takeIf { it > 0L }
                preselectedDateForAdd = parsed.shamsiDateString
                isAnnualPreselected = parsed.isAnnual
                showVoiceExpenseDialog = false
                showAddBottomSheet = true
            }
        )
    }

    if (showAddBottomSheet) {
        AddExpenseBottomSheet(
            onDismissRequest = {
                showAddBottomSheet = false
                isAnnualPreselected = false
                editingExpense = null
                preselectedAmountForAdd = null
                preselectedTitleForAdd = null
                preselectedReceiptPathForAdd = null
            },
            initialShamsiDate = preselectedDateForAdd ?: PersianDateHelper.getShamsiDate().format(),
            expenseToEdit = editingExpense,
            initialIsAnnual = isAnnualPreselected || (editingExpense?.isAnnual == true),
            initialAmount = preselectedAmountForAdd,
            initialTitle = preselectedTitleForAdd,
            initialReceiptPath = preselectedReceiptPathForAdd,
            onAddExpense = { title, amount, isAnnual, customFolder, dateStr, note, receiptImagePath ->
                viewModel.addExpense(
                    title = title,
                    amount = amount,
                    isAnnual = isAnnual,
                    customFolder = customFolder,
                    shamsiDateString = dateStr,
                    note = note,
                    receiptImagePath = receiptImagePath
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
            totalExpensesCount = allExpenses.size,
            fontScale = fontScale,
            onFontScaleChange = onFontScaleChange,
            fontColorOption = fontColorOption,
            onFontColorChange = onFontColorChange
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
                ScaledDialogContent {
                    Text(
                        text = "تأیید بازیابی اطلاعات (Restore)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                ScaledDialogContent {
                    Text(
                        text = enDigits(
                            "فایل پشتیبان با موفقیت خوانده شد:\n" +
                                "• ${backup.expenses.size} هزینه ثبت‌شده\n" +
                                "• ${backup.customFolders.size} پوشه و ساختار برنامه\n" +
                                "• تاریخ فایل پشتیبان: ${PersianDateHelper.toEnglishDigits(backup.backupDateShamsi)}\n\n" +
                                "آیا مایلید تمام داده‌های فوق بازیابی شوند؟ (اطلاعات فعلی با داده‌های این فایل پشتیبان جایگزین خواهند شد)"
                        ),
                        fontSize = 12.5.sp,
                        lineHeight = 20.sp
                    )
                }
            },
            confirmButton = {
                ScaledDialogContent {
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
                }
            },
            dismissButton = {
                ScaledDialogContent {
                    OutlinedButton(onClick = { pendingRestoreData = null }) {
                        Text("انصراف")
                    }
                }
            }
        )
    }
}
