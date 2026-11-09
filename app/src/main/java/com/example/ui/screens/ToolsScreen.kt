package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.utils.PersianDateHelper

/**
 * Screen for Tools & Utilities ("امکانات"), featuring a fast, intuitive financial calculator
 * with large accessible touch targets, Persian number formatting, and direct expense logging.
 */
@Composable
fun ToolsScreen(
    viewModel: ExpenseViewModel,
    onAddExpenseWithAmount: (amount: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
            .testTag("tools_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Screen Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE0F2FE),
                    border = BorderStroke(1.2.dp, Color(0xFF38BDF8)),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "امکانات و ابزارهای مالی",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "ماشین حساب محاسبات سریع و ابزارهای کاربردی مخارج",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // The Financial Calculator Card
        item {
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

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

/**
 * Calculator component with Persian number display, comma separation,
 * quick financial presets, and standard operations.
 */
@Composable
fun FinancialCalculatorCard(
    onRegisterExpense: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Calculator internal state
    var display by remember { mutableStateOf("0") }
    var expression by remember { mutableStateOf("") }
    var lastOperator by remember { mutableStateOf<String?>(null) }
    var operandFirst by remember { mutableStateOf<Double?>(null) }
    var isNewInput by remember { mutableStateOf(true) }

    fun parseCurrentDisplay(): Double {
        val clean = PersianDateHelper.toEnglishDigits(display)
            .replace(",", "")
            .replace("،", "")
            .trim()
        return clean.toDoubleOrNull() ?: 0.0
    }

    fun formatWithCommas(num: Long): String {
        return PersianDateHelper.formatCurrency(num, includeUnit = false)
    }

    fun formatNumber(num: Double): String {
        return if (num % 1.0 == 0.0) {
            val longVal = num.toLong()
            formatWithCommas(longVal)
        } else {
            val formatted = String.format(java.util.Locale.US, "%.2f", num)
            PersianDateHelper.toPersianDigits(formatted)
        }
    }

    fun handleDigit(d: String) {
        if (isNewInput || display == "0") {
            display = d
            isNewInput = false
        } else {
            if (display.replace(",", "").length < 13) {
                val currentRaw = PersianDateHelper.toEnglishDigits(display).replace(",", "")
                val newRaw = currentRaw + d
                val num = newRaw.toLongOrNull()
                display = if (num != null) {
                    formatWithCommas(num)
                } else {
                    newRaw
                }
            }
        }
    }

    fun handleThreeZeros() {
        if (isNewInput || display == "0") {
            display = "0"
        } else {
            val currentRaw = PersianDateHelper.toEnglishDigits(display).replace(",", "")
            if (currentRaw.length <= 10) {
                val newRaw = currentRaw + "000"
                val num = newRaw.toLongOrNull()
                if (num != null) {
                    display = formatWithCommas(num)
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

    fun handleOperator(op: String) {
        val currentVal = parseCurrentDisplay()
        if (operandFirst == null) {
            operandFirst = currentVal
        } else if (!isNewInput && lastOperator != null) {
            // Compute intermediate
            val res = when (lastOperator) {
                "+" -> operandFirst!! + currentVal
                "−" -> operandFirst!! - currentVal
                "×" -> operandFirst!! * currentVal
                "÷" -> if (currentVal != 0.0) operandFirst!! / currentVal else 0.0
                else -> currentVal
            }
            operandFirst = res
            display = formatNumber(res)
        }
        lastOperator = op
        expression = "${formatNumber(operandFirst!!)} $op"
        isNewInput = true
    }

    fun handleEquals() {
        if (operandFirst != null && lastOperator != null) {
            val currentVal = parseCurrentDisplay()
            val res = when (lastOperator) {
                "+" -> operandFirst!! + currentVal
                "−" -> operandFirst!! - currentVal
                "×" -> operandFirst!! * currentVal
                "÷" -> if (currentVal != 0.0) operandFirst!! / currentVal else 0.0
                else -> currentVal
            }
            expression = "${formatNumber(operandFirst!!)} $lastOperator ${formatNumber(currentVal)} ="
            display = formatNumber(res)
            operandFirst = null
            lastOperator = null
            isNewInput = true
        }
    }

    fun handleClear() {
        display = "0"
        expression = ""
        operandFirst = null
        lastOperator = null
        isNewInput = true
    }

    fun handleBackspace() {
        if (isNewInput) {
            display = "0"
            return
        }
        val currentRaw = PersianDateHelper.toEnglishDigits(display).replace(",", "")
        if (currentRaw.length <= 1) {
            display = "0"
            isNewInput = true
        } else {
            val newRaw = currentRaw.dropLast(1)
            val num = newRaw.toLongOrNull()
            display = if (num != null) {
                formatWithCommas(num)
            } else {
                newRaw
            }
        }
    }

    fun handlePercent() {
        val currentVal = parseCurrentDisplay()
        val res = if (operandFirst != null) {
            operandFirst!! * (currentVal / 100.0)
        } else {
            currentVal / 100.0
        }
        display = formatNumber(res)
        isNewInput = true
    }

    // Quick financial helper presets
    fun applyVat() {
        val currentVal = parseCurrentDisplay()
        val withVat = currentVal * 1.09 // +9%
        expression = "${formatNumber(currentVal)} + ۹٪ مالیات ="
        display = formatNumber(withVat)
        isNewInput = true
    }

    fun applyDiscount(percent: Double) {
        val currentVal = parseCurrentDisplay()
        val discounted = currentVal * (1.0 - percent / 100.0)
        val pPersian = PersianDateHelper.toPersianDigits(percent.toInt())
        expression = "${formatNumber(currentVal)} - $pPersian٪ تخفیف ="
        display = formatNumber(discounted)
        isNewInput = true
    }

    fun splitEvenly(persons: Int) {
        val currentVal = parseCurrentDisplay()
        if (persons > 0 && currentVal > 0) {
            val share = currentVal / persons
            val pPersian = PersianDateHelper.toPersianDigits(persons)
            expression = "${formatNumber(currentVal)} ÷ $pPersian دانگ ="
            display = formatNumber(share)
            isNewInput = true
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("financial_calculator_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, Color(0xFF0284C7)), // Lively Cyan Border
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Calculator Header Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ماشین حساب محاسبات مالی",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                // Copy amount icon button
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val rawDigits = PersianDateHelper.toEnglishDigits(display).replace(",", "")
                            val clip = ClipData.newPlainText("مبلغ", rawDigits)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "مبلغ $display در حافظه کپی شد", Toast.LENGTH_SHORT).show()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "کپی مبلغ",
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

            // Calculator Display Box
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    // Formula expression line
                    Text(
                        text = if (expression.isNotBlank()) expression else " ",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Main formatted result line
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "تومان",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            maxLines = 1,
                            softWrap = false
                        )

                        Text(
                            text = display,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick financial shortcut buttons row (مالیات، تخفیف، دُنگ)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // +9% VAT
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFEF3C7),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { applyVat() }
                ) {
                    Text(
                        text = "+۹٪ مالیات",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB45309),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 5.dp),
                        maxLines = 1,
                        softWrap = false
                    )
                }

                // -10% Discount
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFE0F2FE),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { applyDiscount(10.0) }
                ) {
                    Text(
                        text = "-۱۰٪ تخفیف",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0284C7),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 5.dp),
                        maxLines = 1,
                        softWrap = false
                    )
                }

                // ÷2 Split (دانگ ۲ نفره)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF3E8FF),
                    border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { splitEvenly(2) }
                ) {
                    Text(
                        text = "÷ ۲ دُنگ",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7E22CE),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 5.dp),
                        maxLines = 1,
                        softWrap = false
                    )
                }

                // ÷4 Split (دانگ ۴ نفره)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFECFDF5),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { splitEvenly(4) }
                ) {
                    Text(
                        text = "÷ ۴ دُنگ",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF047857),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 5.dp),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Direct "Register as Expense" Action Button
            Button(
                onClick = {
                    val amountLong = parseCurrentDisplay().toLong()
                    onRegisterExpense(amountLong)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
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
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))

            // Calculator Keypad Grid (Standard 4x5 Layout with Persian & Math Keys)
            val operatorColor = Color(0xFF0284C7)
            val actionBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            val numBg = MaterialTheme.colorScheme.surface

            // Row 1: C, ⌫, %, ÷
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalcKey(label = "C", color = Color(0xFFE11D48), bgColor = Color(0xFFFFF1F2), modifier = Modifier.weight(1f)) { handleClear() }
                CalcKey(icon = Icons.Default.Backspace, color = MaterialTheme.colorScheme.onSurface, bgColor = actionBg, modifier = Modifier.weight(1f)) { handleBackspace() }
                CalcKey(label = "٪", color = operatorColor, bgColor = actionBg, modifier = Modifier.weight(1f)) { handlePercent() }
                CalcKey(label = "÷", color = operatorColor, bgColor = actionBg, modifier = Modifier.weight(1f)) { handleOperator("÷") }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: 7, 8, 9, ×
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalcKey(label = "۷", color = MaterialTheme.colorScheme.onSurface, bgColor = numBg, modifier = Modifier.weight(1f)) { handleDigit("7") }
                CalcKey(label = "۸", color = MaterialTheme.colorScheme.onSurface, bgColor = numBg, modifier = Modifier.weight(1f)) { handleDigit("8") }
                CalcKey(label = "۹", color = MaterialTheme.colorScheme.onSurface, bgColor = numBg, modifier = Modifier.weight(1f)) { handleDigit("9") }
                CalcKey(label = "×", color = operatorColor, bgColor = actionBg, modifier = Modifier.weight(1f)) { handleOperator("×") }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 3: 4, 5, 6, −
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalcKey(label = "۴", color = MaterialTheme.colorScheme.onSurface, bgColor = numBg, modifier = Modifier.weight(1f)) { handleDigit("4") }
                CalcKey(label = "۵", color = MaterialTheme.colorScheme.onSurface, bgColor = numBg, modifier = Modifier.weight(1f)) { handleDigit("5") }
                CalcKey(label = "۶", color = MaterialTheme.colorScheme.onSurface, bgColor = numBg, modifier = Modifier.weight(1f)) { handleDigit("6") }
                CalcKey(label = "−", color = operatorColor, bgColor = actionBg, modifier = Modifier.weight(1f)) { handleOperator("−") }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 4: 1, 2, 3, +
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalcKey(label = "۱", color = MaterialTheme.colorScheme.onSurface, bgColor = numBg, modifier = Modifier.weight(1f)) { handleDigit("1") }
                CalcKey(label = "۲", color = MaterialTheme.colorScheme.onSurface, bgColor = numBg, modifier = Modifier.weight(1f)) { handleDigit("2") }
                CalcKey(label = "۳", color = MaterialTheme.colorScheme.onSurface, bgColor = numBg, modifier = Modifier.weight(1f)) { handleDigit("3") }
                CalcKey(label = "+", color = operatorColor, bgColor = actionBg, modifier = Modifier.weight(1f)) { handleOperator("+") }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 5: 000 (سه صفر), 0, ., =
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalcKey(label = "۰۰۰", color = Color(0xFF0D9488), bgColor = Color(0xFFF0FDFA), modifier = Modifier.weight(1f)) { handleThreeZeros() }
                CalcKey(label = "۰", color = MaterialTheme.colorScheme.onSurface, bgColor = numBg, modifier = Modifier.weight(1f)) { handleDigit("0") }
                CalcKey(label = "•", color = MaterialTheme.colorScheme.onSurface, bgColor = numBg, modifier = Modifier.weight(1f)) { handleDecimal() }
                CalcKey(label = "=", color = Color.White, bgColor = Color(0xFF0284C7), modifier = Modifier.weight(1f)) { handleEquals() }
            }
        }
    }
}

/**
 * Individual reusable button on the calculator keypad with minimum 48dp touch target.
 */
@Composable
private fun CalcKey(
    modifier: Modifier = Modifier,
    label: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    color: Color,
    bgColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(52.dp)
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
                    fontSize = if (label == "۰۰۰") 14.sp else 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    maxLines = 1,
                    softWrap = false
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
