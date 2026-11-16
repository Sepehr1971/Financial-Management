package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.luminance
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LocalTextStyle
import com.example.ui.components.BaleBackgroundSyncRunner
import com.example.ui.components.BaleInteractiveSyncDialog
import com.example.ui.components.InstallmentReminderCard
import com.example.ui.components.MarketRatesCard
import com.example.ui.components.ReceiptAttachmentToolCard
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.ScaledDialogContent
import com.example.ui.theme.appSecondaryTextColor
import com.example.ui.theme.appTextColor
import com.example.ui.theme.enDigits
import com.example.ui.theme.scaledDp
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.utils.PersianDateHelper
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

/**
 * Representation of individual tools available in the cascading selector.
 * Styled with vibrant, joyful colors that shine in both dark and light themes.
 */
enum class ToolSection(
    val title: String,
    val icon: ImageVector,
    val accentLight: Color,
    val accentDark: Color,
    val bgLight: Color,
    val bgDark: Color
) {
    CALCULATOR(
        title = "ماشین حساب مالی",
        icon = Icons.Default.Calculate,
        accentLight = Color(0xFF0284C7),
        accentDark = Color(0xFF38BDF8),
        bgLight = Color(0xFFE0F2FE),
        bgDark = Color(0xFF0C4A6E)
    ),
    LOAN_CALCULATOR(
        title = "محاسبه‌گر وام و بازپرداخت",
        icon = Icons.Default.AccountBalance,
        accentLight = Color(0xFF059669),
        accentDark = Color(0xFF34D399),
        bgLight = Color(0xFFD1FAE5),
        bgDark = Color(0xFF064E3B)
    ),
    INSTALLMENT_REMINDER(
        title = "یادآوری اقساط و وام‌ها",
        icon = Icons.Default.Receipt,
        accentLight = Color(0xFF7C3AED),
        accentDark = Color(0xFFA78BFA),
        bgLight = Color(0xFFF3E8FF),
        bgDark = Color(0xFF4C1D95)
    ),
    RECEIPT_ATTACHMENT(
        title = "پیوست تصویر رسید بانکی",
        icon = Icons.Default.AttachFile,
        accentLight = Color(0xFFDB2777),
        accentDark = Color(0xFFF472B6),
        bgLight = Color(0xFFFCE7F3),
        bgDark = Color(0xFF500724)
    );

    @Composable
    fun getAccentColor(): Color = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) accentDark else accentLight

    @Composable
    fun getBgColor(): Color = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) bgDark else bgLight
}

/**
 * Single item saved in calculation history.
 */
data class CalculationHistoryItem(
    val id: Long = System.currentTimeMillis(),
    val formula: String,
    val result: String,
    val rawAmount: Double,
    val timestamp: String
)

/**
 * Main Screen for Financial Tools ("امکانات مالی"), featuring a cascading/waterfall selector
 * between the Financial Calculator, Loan Calculator, and Installment Reminder.
 */
@Composable
fun ToolsScreen(
    viewModel: ExpenseViewModel,
    onAddExpenseWithAmount: (amount: Long) -> Unit,
    onAddExpenseWithReceipt: (receiptPath: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allExpandedExpenses by viewModel.allExpandedExpenses.collectAsStateWithLifecycle()
    val marketRatesState by viewModel.marketRates.collectAsStateWithLifecycle()
    var selectedTool by rememberSaveable { mutableStateOf<ToolSection?>(null) }
    var isDropdownExpanded by rememberSaveable { mutableStateOf(true) }
    var runSilentBaleSync by remember { mutableStateOf(false) }
    var openBaleDialogIfLoginNeeded by remember { mutableStateOf(false) }
    var showBaleInteractiveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!runSilentBaleSync) {
            openBaleDialogIfLoginNeeded = false
            runSilentBaleSync = true
        }
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val activeAccent = selectedTool?.getAccentColor() ?: if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val activeBg = selectedTool?.getBgColor() ?: if (isDark) Color(0xFF0C4A6E) else Color(0xFFE0F2FE)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = scaledDp(14.dp))
            .testTag("tools_screen"),
        verticalArrangement = Arrangement.spacedBy(scaledDp(14.dp))
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // 1. Cascading / Waterfall Dropdown Tool Selector (اولین کادر تب امکانات)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.4.dp, activeAccent),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(scaledDp(12.dp))) {
                    // Cascading Clickable Bar (Clean flat row: icon, text with weight, close/chevron)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = activeBg.copy(alpha = 0.5f),
                        border = BorderStroke(1.2.dp, activeAccent.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { isDropdownExpanded = !isDropdownExpanded }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = scaledDp(10.dp), vertical = scaledDp(10.dp)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = activeAccent,
                                modifier = Modifier.size(scaledDp(32.dp))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = selectedTool?.icon ?: Icons.Default.AccountBalance,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(scaledDp(18.dp))
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = selectedTool?.title ?: "انتخاب ابزار مالی (کلیک کنید)",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Black,
                                color = appTextColor(MaterialTheme.colorScheme.onSurface),
                                modifier = Modifier.weight(1f),
                                softWrap = true
                            )
                            if (selectedTool != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            selectedTool = null
                                            isDropdownExpanded = true
                                        }
                                        .padding(horizontal = scaledDp(7.dp), vertical = scaledDp(4.dp))
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "بستن ابزار",
                                            tint = appSecondaryTextColor(MaterialTheme.colorScheme.onSurfaceVariant),
                                            modifier = Modifier.size(scaledDp(13.dp))
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "بستن",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = appSecondaryTextColor(MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Icon(
                                imageVector = if (isDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = "باز کردن منوی آبشاری",
                                tint = activeAccent,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // Expanded Cascading Menu Options
                    AnimatedVisibility(visible = isDropdownExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ToolSection.entries.forEach { tool ->
                                val isCurrent = tool == selectedTool
                                val toolAccent = tool.getAccentColor()
                                val toolBg = tool.getBgColor()
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isCurrent) toolBg else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    border = BorderStroke(
                                        if (isCurrent) 1.8.dp else 1.dp,
                                        if (isCurrent) toolAccent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            selectedTool = tool
                                            isDropdownExpanded = false
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isCurrent) toolAccent else toolBg,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = tool.icon,
                                                    contentDescription = null,
                                                    tint = if (isCurrent) Color.White else toolAccent,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = tool.title,
                                            fontSize = 13.5.sp,
                                            fontWeight = if (isCurrent) FontWeight.Black else FontWeight.ExtraBold,
                                            color = if (isCurrent) toolAccent else appTextColor(MaterialTheme.colorScheme.onSurface),
                                            modifier = Modifier.weight(1f),
                                            softWrap = true
                                        )
                                        if (isCurrent) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = toolAccent,
                                                modifier = Modifier.size(18.dp)
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

        // 2. Active Tool Content (باز شدن دقیقاً زیر کادر کشویی انتخاب ابزار مالی)
        if (selectedTool != null) {
            item {
                when (selectedTool) {
                    ToolSection.CALCULATOR -> {
                        FinancialCalculatorCard(
                            onRegisterExpense = { calculatedAmount ->
                                if (calculatedAmount > 0) {
                                    onAddExpenseWithAmount(calculatedAmount)
                                } else {
                                    Toast.makeText(context, "مبلغ محاسبه‌شده باید بیشتر از صفر باشد", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                    ToolSection.LOAN_CALCULATOR -> {
                        LoanCalculatorCard(
                            onRegisterInstallmentExpense = { installmentAmount ->
                                if (installmentAmount > 0) {
                                    onAddExpenseWithAmount(installmentAmount)
                                } else {
                                    Toast.makeText(context, "مبلغ قسط باید بیشتر از صفر باشد", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                    ToolSection.INSTALLMENT_REMINDER -> {
                        InstallmentReminderCard(
                            onRegisterExpense = { installmentAmount ->
                                if (installmentAmount > 0) {
                                    onAddExpenseWithAmount(installmentAmount)
                                } else {
                                    Toast.makeText(context, "مبلغ قسط باید بیشتر از صفر باشد", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                    ToolSection.RECEIPT_ATTACHMENT -> {
                        ReceiptAttachmentToolCard(
                            expenses = allExpandedExpenses,
                            onAttachReceiptToExpense = { expenseId, receiptPath ->
                                viewModel.attachReceiptToExpense(expenseId, receiptPath)
                            },
                            onCreateExpenseWithReceipt = { receiptPath ->
                                onAddExpenseWithReceipt(receiptPath)
                            }
                        )
                    }
                    null -> {}
                }
            }
        }

        // 3. قیمت روز ارز، طلا، رمزارز (از بله) و بورس/نفت (از tgju.org/coin)
        item {
            MarketRatesCard(
                ratesState = marketRatesState,
                onRefresh = {
                    Toast.makeText(context, "در حال به‌روزرسانی بورس/نفت و نرخ‌های بله...", Toast.LENGTH_SHORT).show()
                    viewModel.refreshMarketRates()
                    openBaleDialogIfLoginNeeded = true
                    runSilentBaleSync = true
                },
                onConnectBale = {
                    showBaleInteractiveDialog = true
                }
            )
        }

        item { Spacer(modifier = Modifier.height(110.dp)) }
    }

    BaleBackgroundSyncRunner(
        active = runSilentBaleSync,
        onCompleted = { extracted ->
            runSilentBaleSync = false
            openBaleDialogIfLoginNeeded = false
            viewModel.applyBaleExtractedRates(extracted)
            Toast.makeText(context, "قیمت‌ها از کانال‌های بله به‌روزرسانی شد", Toast.LENGTH_SHORT).show()
        },
        onNeedsLogin = {
            runSilentBaleSync = false
            viewModel.markBaleLoginRequired(true)
            if (openBaleDialogIfLoginNeeded) {
                openBaleDialogIfLoginNeeded = false
                showBaleInteractiveDialog = true
            }
        },
        onFailed = {
            runSilentBaleSync = false
            openBaleDialogIfLoginNeeded = false
            viewModel.markBaleLoginRequired(false)
            Toast.makeText(context, "خطا در خواندن پیام کانال‌های بله", Toast.LENGTH_SHORT).show()
        }
    )

    if (showBaleInteractiveDialog) {
        BaleInteractiveSyncDialog(
            onDismiss = { showBaleInteractiveDialog = false },
            onRatesExtracted = { extracted ->
                showBaleInteractiveDialog = false
                viewModel.applyBaleExtractedRates(extracted)
                Toast.makeText(context, "قیمت‌های ارز، طلا و رمزارز از بله ثبت شد", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

/**
 * Advanced Financial Calculator with:
 * - English digits on keypad and formatted display
 * - Numbers strictly on the LEFT, operators strictly on the RIGHT
 * - Cascading/dropdown customizable Tax (%)
 * - Cascading/dropdown Discount in 5% increments (5% to 95%)
 * - 20-item Calculation History / Memory with selection
 * - Direct expense logging button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialCalculatorCard(
    onRegisterExpense: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Internal calculator states
    var display by remember { mutableStateOf("0") }
    var expression by remember { mutableStateOf("") }
    var lastOperator by remember { mutableStateOf<String?>(null) }
    var operandFirst by remember { mutableStateOf<Double?>(null) }
    var isNewInput by remember { mutableStateOf(true) }

    // Calculation history (stores up to 20 calculations)
    var calculationHistory by remember { mutableStateOf<List<CalculationHistoryItem>>(emptyList()) }
    var showHistorySheet by remember { mutableStateOf(false) }

    // Dialog state for customizable Tax
    var showTaxDialog by remember { mutableStateOf(false) }
    var customTaxInput by remember { mutableStateOf("9") }

    // Dialog/Dropdown state for 5%-increment Discounts
    var showDiscountSheet by remember { mutableStateOf(false) }
    var customDiscountInput by remember { mutableStateOf("10") }

    fun parseCurrentDisplay(): Double {
        val clean = display.replace(",", "").replace(" ", "").trim()
        return clean.toDoubleOrNull() ?: 0.0
    }

    fun formatEnglishCommas(num: Long): String {
        return NumberFormat.getNumberInstance(Locale.US).format(num)
    }

    fun formatNumber(num: Double): String {
        return if (num % 1.0 == 0.0) {
            formatEnglishCommas(num.toLong())
        } else {
            String.format(Locale.US, "%,.2f", num)
        }
    }

    fun addHistoryEntry(formula: String, resultStr: String, rawResult: Double) {
        val sdf = java.text.SimpleDateFormat("HH:mm", Locale.US)
        val timeNow = sdf.format(java.util.Date())
        val item = CalculationHistoryItem(
            formula = formula,
            result = resultStr,
            rawAmount = rawResult,
            timestamp = timeNow
        )
        calculationHistory = (listOf(item) + calculationHistory).take(20)
    }

    fun handleDigit(d: String) {
        if (isNewInput || display == "0") {
            display = d
            isNewInput = false
        } else {
            val currentRaw = display.replace(",", "")
            if (currentRaw.length < 13) {
                val newRaw = currentRaw + d
                val num = newRaw.toLongOrNull()
                display = if (num != null) formatEnglishCommas(num) else newRaw
            }
        }
    }

    fun handleThreeZeros() {
        if (isNewInput || display == "0") {
            display = "0"
        } else {
            val currentRaw = display.replace(",", "")
            if (currentRaw.length <= 10) {
                val newRaw = currentRaw + "000"
                val num = newRaw.toLongOrNull()
                if (num != null) {
                    display = formatEnglishCommas(num)
                }
            }
        }
    }

    fun handleDecimal() {
        if (isNewInput) {
            display = "0."
            isNewInput = false
        } else if (!display.contains(".")) {
            display += "."
        }
    }

    fun handleClear() {
        display = "0"
        expression = ""
        lastOperator = null
        operandFirst = null
        isNewInput = true
    }

    fun handleBackspace() {
        if (isNewInput) {
            display = "0"
            return
        }
        val currentRaw = display.replace(",", "")
        if (currentRaw.length <= 1) {
            display = "0"
            isNewInput = true
        } else {
            val newRaw = currentRaw.dropLast(1)
            val num = newRaw.toLongOrNull()
            display = if (num != null) formatEnglishCommas(num) else newRaw
        }
    }

    fun executeMath(op: String, a: Double, b: Double): Double {
        return when (op) {
            "+" -> a + b
            "−" -> a - b
            "×" -> a * b
            "÷" -> if (b != 0.0) a / b else 0.0
            else -> b
        }
    }

    fun handleOperator(op: String) {
        val currentVal = parseCurrentDisplay()
        if (operandFirst == null) {
            operandFirst = currentVal
            expression = "${formatNumber(currentVal)} $op"
        } else if (lastOperator != null && !isNewInput) {
            val result = executeMath(lastOperator!!, operandFirst!!, currentVal)
            operandFirst = result
            display = formatNumber(result)
            expression = "${formatNumber(result)} $op"
        } else {
            expression = "${formatNumber(operandFirst!!)} $op"
        }
        lastOperator = op
        isNewInput = true
    }

    fun handleEquals() {
        if (operandFirst != null && lastOperator != null) {
            val second = parseCurrentDisplay()
            val result = executeMath(lastOperator!!, operandFirst!!, second)
            val formula = "${formatNumber(operandFirst!!)} $lastOperator ${formatNumber(second)}"
            val resStr = formatNumber(result)
            expression = "$formula ="
            display = resStr
            addHistoryEntry(formula, resStr, result)
            operandFirst = null
            lastOperator = null
            isNewInput = true
        }
    }

    fun handlePercent() {
        val currentVal = parseCurrentDisplay()
        val res = if (operandFirst != null) {
            operandFirst!! * (currentVal / 100.0)
        } else {
            currentVal / 100.0
        }
        val formula = if (operandFirst != null) "${formatNumber(operandFirst!!)} × ${formatNumber(currentVal)}%" else "${formatNumber(currentVal)}%"
        val resStr = formatNumber(res)
        display = resStr
        expression = formula
        addHistoryEntry(formula, resStr, res)
        isNewInput = true
    }

    fun applyCustomTax(taxRate: Double) {
        val currentVal = parseCurrentDisplay()
        if (currentVal <= 0) return
        val taxAmount = currentVal * (taxRate / 100.0)
        val finalAmount = currentVal + taxAmount
        val formula = "${formatNumber(currentVal)} + ${taxRate.toInt()}% مالیات"
        val resStr = formatNumber(finalAmount)
        display = resStr
        expression = formula
        addHistoryEntry(formula, resStr, finalAmount)
        isNewInput = true
        Toast.makeText(context, "${taxRate.toInt()}% مالیات به مبلغ اضافه شد", Toast.LENGTH_SHORT).show()
    }

    fun applyCustomDiscount(discountRate: Double) {
        val currentVal = parseCurrentDisplay()
        if (currentVal <= 0) return
        val discountAmount = currentVal * (discountRate / 100.0)
        val finalAmount = currentVal - discountAmount
        val formula = "${formatNumber(currentVal)} - ${discountRate.toInt()}% تخفیف"
        val resStr = formatNumber(finalAmount)
        display = resStr
        expression = formula
        addHistoryEntry(formula, resStr, finalAmount)
        isNewInput = true
        Toast.makeText(context, "${discountRate.toInt()}% تخفیف اعمال شد", Toast.LENGTH_SHORT).show()
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val vibrantCyan = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.6.dp, vibrantCyan),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Title + Copy Button
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
                        imageVector = Icons.Default.Calculate,
                        contentDescription = null,
                        tint = vibrantCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ماشین حساب مالی",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = appTextColor(MaterialTheme.colorScheme.onSurface),
                        softWrap = true
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = vibrantCyan.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, vibrantCyan.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val clip = ClipData.newPlainText("Calculator Result", display)
                            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                            Toast.makeText(context, "مبلغ $display کپی شد", Toast.LENGTH_SHORT).show()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "کپی رقم",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "کپی رقم",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Display Box (English Numbers and Formula)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = scaledDp(14.dp), vertical = scaledDp(12.dp)),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = enDigits(if (expression.isNotBlank()) expression else " "),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = appSecondaryTextColor(MaterialTheme.colorScheme.onSurfaceVariant),
                        maxLines = 2,
                        softWrap = true,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "تومان",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            softWrap = false
                        )

                        Text(
                            text = display,
                            fontFamily = OutfitFontFamily,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = appTextColor(MaterialTheme.colorScheme.onSurface),
                            maxLines = 1,
                            softWrap = false,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Operations Row:
            // 1. مالیات کشویی
            // 2. تخفیف کشویی (۵٪ به ۵٪)
            // 3. حافظه و تاریخچه محاسبات (تا ۲۰ مورد)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 1. Tax Button (کشویی مالیات)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFEF3C7),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showTaxDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = Color(0xFFB45309),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "مالیات ▼",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // 2. Discount Button (کشویی تخفیف ۵٪ به ۵٪)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFE0F2FE),
                    border = BorderStroke(1.dp, Color(0xFF0284C7)),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showDiscountSheet = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalOffer,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "تخفیف ▼",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0369A1),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // 3. History Memory Button (حافظه ۲۰ مرحله محاسباتی)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF3E8FF),
                    border = BorderStroke(1.dp, Color(0xFFA855F7)),
                    modifier = Modifier
                        .weight(1.1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showHistorySheet = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = Color(0xFF7E22CE),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = enDigits("حافظه (${calculationHistory.size})"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6B21A8),
                            maxLines = 1,
                            softWrap = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(scaledDp(10.dp)))

            // Action Button: Direct Expense Registration
            Button(
                onClick = {
                    val amountLong = parseCurrentDisplay().toLong()
                    onRegisterExpense(amountLong)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = scaledDp(42.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ثبت مستقیم این مبلغ به عنوان هزینه",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        softWrap = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            Spacer(modifier = Modifier.height(12.dp))

            // KEYPAD:
            // Crucial user requirement:
            // "چیدمان ماشین حساب که اعداد سمت چپ قرار گیرد و علامتهای محاسباتی سمت راست"
            // "اعداد ماشین حساب را انگلیسی بکن"
            // We force LTR for the keypad so that Columns 1, 2, 3 (Numbers 7, 8, 9, 4, 5, 6, 1, 2, 3, 000, 0, .)
            // are positioned physically on the LEFT side of the screen,
            // and Column 4 (÷, ×, −, +, =) is positioned on the RIGHT side of the screen.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val operatorColor = Color(0xFF0284C7)
                    val actionBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    val numBg = MaterialTheme.colorScheme.surface
                    val numTextColor = appTextColor(MaterialTheme.colorScheme.onSurface)

                    // Row 1: C | Backspace | % | ÷
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CalcKey(label = "C", color = Color(0xFFE11D48), bgColor = Color(0xFFFFF1F2), modifier = Modifier.weight(1f)) { handleClear() }
                        CalcKey(icon = Icons.Default.Backspace, color = numTextColor, bgColor = actionBg, modifier = Modifier.weight(1f)) { handleBackspace() }
                        CalcKey(label = "%", color = operatorColor, bgColor = actionBg, modifier = Modifier.weight(1f)) { handlePercent() }
                        CalcKey(label = "÷", color = operatorColor, bgColor = actionBg, modifier = Modifier.weight(1f)) { handleOperator("÷") }
                    }

                    // Row 2: 7 | 8 | 9 | ×
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CalcKey(label = "7", color = numTextColor, bgColor = numBg, modifier = Modifier.weight(1f)) { handleDigit("7") }
                        CalcKey(label = "8", color = numTextColor, bgColor = numBg, modifier = Modifier.weight(1f)) { handleDigit("8") }
                        CalcKey(label = "9", color = numTextColor, bgColor = numBg, modifier = Modifier.weight(1f)) { handleDigit("9") }
                        CalcKey(label = "×", color = operatorColor, bgColor = actionBg, modifier = Modifier.weight(1f)) { handleOperator("×") }
                    }

                    // Row 3: 4 | 5 | 6 | −
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CalcKey(label = "4", color = numTextColor, bgColor = numBg, modifier = Modifier.weight(1f)) { handleDigit("4") }
                        CalcKey(label = "5", color = numTextColor, bgColor = numBg, modifier = Modifier.weight(1f)) { handleDigit("5") }
                        CalcKey(label = "6", color = numTextColor, bgColor = numBg, modifier = Modifier.weight(1f)) { handleDigit("6") }
                        CalcKey(label = "−", color = operatorColor, bgColor = actionBg, modifier = Modifier.weight(1f)) { handleOperator("−") }
                    }

                    // Row 4: 1 | 2 | 3 | +
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CalcKey(label = "1", color = numTextColor, bgColor = numBg, modifier = Modifier.weight(1f)) { handleDigit("1") }
                        CalcKey(label = "2", color = numTextColor, bgColor = numBg, modifier = Modifier.weight(1f)) { handleDigit("2") }
                        CalcKey(label = "3", color = numTextColor, bgColor = numBg, modifier = Modifier.weight(1f)) { handleDigit("3") }
                        CalcKey(label = "+", color = operatorColor, bgColor = actionBg, modifier = Modifier.weight(1f)) { handleOperator("+") }
                    }

                    // Row 5: 000 | 0 | . | =
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CalcKey(label = "000", color = Color(0xFF0D9488), bgColor = Color(0xFFF0FDFA), modifier = Modifier.weight(1f)) { handleThreeZeros() }
                        CalcKey(label = "0", color = numTextColor, bgColor = numBg, modifier = Modifier.weight(1f)) { handleDigit("0") }
                        CalcKey(label = ".", color = numTextColor, bgColor = numBg, modifier = Modifier.weight(1f)) { handleDecimal() }
                        CalcKey(label = "=", color = Color.White, bgColor = Color(0xFF0284C7), modifier = Modifier.weight(1f)) { handleEquals() }
                    }
                }
            }
        }
    }

    // Modal 1: Customizable Tax Dialog (کادر مالیات کشویی و قابل نوشتن)
    if (showTaxDialog) {
        val currentAmount = parseCurrentDisplay()
        val taxRate = customTaxInput.toDoubleOrNull() ?: 9.0
        val calculatedTaxAmount = currentAmount * (taxRate / 100.0)
        val finalWithTax = currentAmount + calculatedTaxAmount

        AlertDialog(
            onDismissRequest = { showTaxDialog = false },
            title = {
                ScaledDialogContent {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Receipt, contentDescription = null, tint = Color(0xFFB45309))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "محاسبه و افزودن مالیات", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            text = {
                ScaledDialogContent {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(scaledDp(10.dp))
                    ) {
                        Text(
                            text = "درصد مالیات را انتخاب کنید یا عدد دلخواه بنویسید:",
                            fontSize = 12.sp,
                            color = appSecondaryTextColor(MaterialTheme.colorScheme.onSurfaceVariant)
                        )

                        // Quick percentage presets chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(9, 10, 15, 20).forEach { rate ->
                                val isSelected = customTaxInput == rate.toString()
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFFF59E0B) else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { customTaxInput = rate.toString() }
                                ) {
                                    Text(
                                        text = "$rate%",
                                        fontFamily = OutfitFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else appTextColor(MaterialTheme.colorScheme.onSurface),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = scaledDp(6.dp))
                                    )
                                }
                            }
                        }

                        // Editable percentage input field
                        OutlinedTextField(
                            value = customTaxInput,
                            onValueChange = { customTaxInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = { Text(enDigits("درصد مالیات (مثلا 9)")) },
                            textStyle = LocalTextStyle.current.copy(fontFamily = OutfitFontFamily),
                            trailingIcon = { Text("%", fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Live calculation breakdown
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFEF3C7).copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(scaledDp(10.dp))) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("مبلغ جاری:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF78350F))
                                    Text(enDigits("${formatEnglishCommas(currentAmount.toLong())} تومان"), fontSize = 11.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF78350F))
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(enDigits("مبلغ مالیات (${taxRate.toInt()}%):"), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF78350F))
                                    Text(enDigits("+ ${formatEnglishCommas(calculatedTaxAmount.toLong())} تومان"), fontSize = 11.5.sp, fontWeight = FontWeight.Black, color = Color(0xFFB45309))
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFF59E0B).copy(alpha = 0.3f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("مبلغ نهایی با مالیات:", fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF78350F))
                                    Text(enDigits("${formatEnglishCommas(finalWithTax.toLong())} تومان"), fontSize = 13.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF92400E))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                ScaledDialogContent {
                    Button(
                        onClick = {
                            val parsed = customTaxInput.toDoubleOrNull() ?: 9.0
                            applyCustomTax(parsed)
                            showTaxDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                    ) {
                        Text("اعمال مالیات به مبلغ", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                ScaledDialogContent {
                    TextButton(onClick = { showTaxDialog = false }) {
                        Text("انصراف")
                    }
                }
            }
        )
    }

    // Modal 2: 5%-Increment Discount Bottom Sheet (کادر تخفیف کشویی 5 درصد 5 درصد)
    if (showDiscountSheet) {
        val currentAmount = parseCurrentDisplay()
        val discountPercentList = (5..95 step 5).toList()

        ModalBottomSheet(
            onDismissRequest = { showDiscountSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            ScaledDialogContent {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = scaledDp(16.dp), vertical = scaledDp(8.dp))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.LocalOffer, contentDescription = null, tint = Color(0xFF0284C7))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = enDigits("انتخاب درصد تخفیف (5% به 5%)"),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = appTextColor(MaterialTheme.colorScheme.onSurface),
                                softWrap = true
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = enDigits("مبلغ: ${formatEnglishCommas(currentAmount.toLong())}"),
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(scaledDp(10.dp)))

                    // Custom input field
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customDiscountInput,
                            onValueChange = { customDiscountInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = { Text(enDigits("درصد دلخواه (مثلاً 35 یا 75)")) },
                            textStyle = LocalTextStyle.current.copy(fontFamily = OutfitFontFamily),
                            trailingIcon = { Text("%", fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = {
                                val parsed = customDiscountInput.toDoubleOrNull() ?: 0.0
                                if (parsed in 0.0..100.0) {
                                    applyCustomDiscount(parsed)
                                    showDiscountSheet = false
                                } else {
                                    Toast.makeText(context, "درصد باید بین 0 تا 100 باشد", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                        ) {
                            Text("اعمال")
                        }
                    }

                    Spacer(modifier = Modifier.height(scaledDp(12.dp)))
                    Text(
                        text = "یا یک پله تخفیف را انتخاب کنید:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = appTextColor(MaterialTheme.colorScheme.onSurface)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Grid/List of 5% steps
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = scaledDp(320.dp)),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(discountPercentList) { percent ->
                            val discountValue = currentAmount * (percent / 100.0)
                            val payableValue = currentAmount - discountValue
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        applyCustomDiscount(percent.toDouble())
                                        showDiscountSheet = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = scaledDp(12.dp), vertical = scaledDp(8.dp)),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF0284C7),
                                            modifier = Modifier.widthIn(min = scaledDp(50.dp))
                                        ) {
                                            Text(
                                                text = "$percent%",
                                                fontFamily = OutfitFontFamily,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = enDigits("کسر: ${formatEnglishCommas(discountValue.toLong())} تومان"),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFB91C1C)
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = enDigits("${formatEnglishCommas(payableValue.toLong())} تومان"),
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF047857)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = Color(0xFF0284C7),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Modal 3: History Memory Bottom Sheet (حافظه 20 مرحله محاسباتی قبلی)
    if (showHistorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            ScaledDialogContent {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = scaledDp(16.dp), vertical = scaledDp(8.dp))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFF7E22CE))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = enDigits("حافظه و تاریخچه محاسبات (تا 20 مورد)"),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = appTextColor(MaterialTheme.colorScheme.onSurface),
                                softWrap = true
                            )
                        }

                        if (calculationHistory.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    calculationHistory = emptyList()
                                    Toast.makeText(context, "حافظه پاک شد", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFDC2626))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("پاک کردن", color = Color(0xFFDC2626), fontSize = 11.5.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(scaledDp(10.dp)))

                    if (calculationHistory.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = scaledDp(160.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(scaledDp(36.dp)))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("هنوز محاسبه‌ای در حافظه ثبت نشده است", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = appTextColor(MaterialTheme.colorScheme.onSurface))
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = scaledDp(340.dp)),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(calculationHistory) { item ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            display = item.result
                                            expression = item.formula
                                            isNewInput = true
                                            showHistorySheet = false
                                            Toast.makeText(context, "مبلغ ${item.result} فراخوانی شد", Toast.LENGTH_SHORT).show()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = scaledDp(12.dp), vertical = scaledDp(9.dp)),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = enDigits(item.formula),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = appTextColor(MaterialTheme.colorScheme.onSurface),
                                                maxLines = 2,
                                                softWrap = true
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = enDigits("= ${item.result} تومان"),
                                                fontSize = 14.5.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF6B21A8),
                                                maxLines = 2,
                                                softWrap = true
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF7E22CE).copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = "انتخاب رقم ❯",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF7E22CE),
                                                modifier = Modifier.padding(horizontal = scaledDp(8.dp), vertical = scaledDp(4.dp)),
                                                maxLines = 1,
                                                softWrap = true
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

/**
 * Loan and Repayment Calculator component with Iranian Banking Formula:
 * Monthly Installment = [Principal * Rate * (n + 1) / 2400 + Principal] / n
 */
@Composable
fun LoanCalculatorCard(
    onRegisterInstallmentExpense: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var principalText by remember { mutableStateOf("100,000,000") }
    var interestRateText by remember { mutableStateOf("23") }
    var monthsText by remember { mutableStateOf("24") }

    fun parseAmount(str: String): Long {
        return str.replace(",", "").replace(" ", "").toLongOrNull() ?: 0L
    }

    val principal = parseAmount(principalText)
    val rate = interestRateText.toDoubleOrNull() ?: 0.0
    val months = monthsText.toIntOrNull() ?: 1

    // Iranian banking loan calculations (CBI formula)
    val totalInterest = if (principal > 0 && months > 0 && rate > 0.0) {
        ((principal * rate * (months + 1)) / 2400.0).toLong()
    } else {
        0L
    }
    val totalRepayment = principal + totalInterest
    val monthlyInstallment = if (months > 0) totalRepayment / months else 0L

    fun formatNumber(num: Long): String {
        return NumberFormat.getNumberInstance(Locale.US).format(num)
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val vibrantEmerald = if (isDark) Color(0xFF34D399) else Color(0xFF059669)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.6.dp, vibrantEmerald),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
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
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = vibrantEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "محاسبه‌گر وام و بازپرداخت بانکی",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = appTextColor(MaterialTheme.colorScheme.onSurface),
                        softWrap = true
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = vibrantEmerald.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, vibrantEmerald.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val summary = "مبلغ وام: ${formatNumber(principal)} تومان\nقسط ماهانه: ${formatNumber(monthlyInstallment)} تومان\nکل سود: ${formatNumber(totalInterest)} تومان\nکل بازپرداخت: ${formatNumber(totalRepayment)} تومان ($months ماه - سود $rate%)"
                            val clip = ClipData.newPlainText("Loan Summary", summary)
                            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                            Toast.makeText(context, "خلاصه اقساط وام کپی شد", Toast.LENGTH_SHORT).show()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = scaledDp(8.dp), vertical = scaledDp(4.dp)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "کپی اقساط",
                            tint = Color(0xFF047857),
                            modifier = Modifier.size(scaledDp(13.dp))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "کپی اطلاعات",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF047857),
                            maxLines = 1,
                            softWrap = true
                        )
                    }
                }
            }

            // 1. Principal Input
            Text(
                text = "مبلغ اصل وام (تومان):",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = appSecondaryTextColor(MaterialTheme.colorScheme.onSurfaceVariant)
            )

            OutlinedTextField(
                value = principalText,
                onValueChange = { input ->
                    val clean = input.filter { it.isDigit() }
                    val num = clean.toLongOrNull() ?: 0L
                    principalText = if (num > 0) formatNumber(num) else clean
                },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontFamily = OutfitFontFamily),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = { Text("تومان", fontSize = 11.sp, modifier = Modifier.padding(end = 8.dp)) },
                modifier = Modifier.fillMaxWidth()
            )

            // Principal Quick Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    "50 م" to 50_000_000L,
                    "100 م" to 100_000_000L,
                    "200 م" to 200_000_000L,
                    "500 م" to 500_000_000L,
                    "1 م" to 1_000_000_000L
                ).forEach { (label, amt) ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { principalText = formatNumber(amt) }
                    ) {
                        Text(
                            text = enDigits(label),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = appTextColor(MaterialTheme.colorScheme.onSurface),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = scaledDp(4.dp)),
                            maxLines = 1,
                            softWrap = true
                        )
                    }
                }
            }

            // 2. Interest Rate & Duration in a Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "نرخ سود سالانه (%):",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = appSecondaryTextColor(MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = interestRateText,
                        onValueChange = { interestRateText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontFamily = OutfitFontFamily),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        trailingIcon = { Text("%", fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 6.dp)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "مدت اقساط (ماه):",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = appSecondaryTextColor(MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = monthsText,
                        onValueChange = { monthsText = it.filter { ch -> ch.isDigit() } },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontFamily = OutfitFontFamily),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        trailingIcon = { Text("ماه", fontSize = 11.sp, modifier = Modifier.padding(end = 6.dp)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Quick Duration Preset Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    "12 ماه" to "12",
                    "24 ماه" to "24",
                    "36 ماه" to "36",
                    "48 ماه" to "48",
                    "60 ماه" to "60"
                ).forEach { (lbl, m) ->
                    val isSelected = monthsText == m
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color(0xFF059669) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { monthsText = m }
                    ) {
                        Text(
                            text = enDigits(lbl),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else appTextColor(MaterialTheme.colorScheme.onSurface),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = scaledDp(4.dp)),
                            maxLines = 1,
                            softWrap = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Calculated Results Box
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isDark) Color(0xFF064E3B).copy(alpha = 0.5f) else Color(0xFFECFDF5).copy(alpha = 0.85f),
                border = BorderStroke(1.4.dp, vibrantEmerald),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(scaledDp(12.dp)),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Result 1: Monthly Installment (Main Highlight)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "مبلغ هر قسط ماهانه:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color(0xFFA7F3D0) else Color(0xFF065F46)
                        )
                        Text(
                            text = enDigits("${formatNumber(monthlyInstallment)} تومان"),
                            fontSize = 17.5.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857)
                        )
                    }

                    HorizontalDivider(color = vibrantEmerald.copy(alpha = 0.35f))

                    // Result 2: Total Interest
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "کل سود پرداختی به بانک:",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFA7F3D0) else Color(0xFF065F46)
                        )
                        Text(
                            text = enDigits("${formatNumber(totalInterest)} تومان"),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isDark) Color(0xFFFDE68A) else Color(0xFFB45309)
                        )
                    }

                    // Result 3: Total Repayment (Principal + Interest)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "کل مبلغ بازپرداخت:",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFA7F3D0) else Color(0xFF065F46)
                        )
                        Text(
                            text = enDigits("${formatNumber(totalRepayment)} تومان"),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF065F46)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Button: Direct Installment Logging into Expenses
            Button(
                onClick = { onRegisterInstallmentExpense(monthlyInstallment) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = scaledDp(44.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ثبت مستقیم این قسط در هزینه‌ها / تعهدات",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        softWrap = true
                    )
                }
            }
        }
    }
}

/**
 * Single reusable calculator button with minimum 48dp touch target.
 */
@Composable
private fun CalcKey(
    modifier: Modifier = Modifier,
    label: String? = null,
    icon: ImageVector? = null,
    color: Color,
    bgColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(scaledDp(52.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        shadowElevation = 1.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (label != null) {
                Text(
                    text = label,
                    fontFamily = OutfitFontFamily,
                    fontSize = if (label == "000") 14.5.sp else 18.5.sp,
                    fontWeight = FontWeight.Black,
                    color = color,
                    maxLines = 1,
                    softWrap = false
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(scaledDp(20.dp))
                )
            }
        }
    }
}
