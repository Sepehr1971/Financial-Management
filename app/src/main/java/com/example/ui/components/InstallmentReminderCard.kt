package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.utils.LoanNotificationHelper
import com.example.utils.PersianDateHelper
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.Brush
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.ScaledDialogContent
import com.example.ui.theme.appSecondaryTextColor
import com.example.ui.theme.appTextColor
import com.example.ui.theme.enDigits
import com.example.ui.theme.scaledDp

/**
 * Data Model for a Loan / Installment Tracker Item with Notification Reminder.
 */
data class InstallmentItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val bankOrLender: String = "",
    val totalLoanAmount: Long = 0L,         // مبلغ کل وام به تومان
    val monthlyAmount: Long,                // مبلغ قسط هر ماه به تومان
    val dueDayOfMonth: Int,                 // روز سررسید در ماه (۱ تا ۳۱)
    val reminderOffsetHours: Int = 24,      // زمان یادآوری بر حسب ساعت قبل از سررسید (مثلاً ۱، ۳، ۶، ۱۲ یا ۲۴، ۴۸...)
    val reminderDaysBefore: Int = 1,        // حفظ سازگاری
    val isNotificationEnabled: Boolean = true,
    val totalInstallments: Int,
    val paidInstallments: Int = 0,
    val note: String = ""
)

/**
 * Helper to format reminder timing in human-readable Persian (ساعت یا روز قبل سررسید).
 */
fun formatReminderTimingLabel(item: InstallmentItem): String {
    val h = if (item.reminderOffsetHours > 0 || item.reminderDaysBefore == 0) item.reminderOffsetHours else item.reminderDaysBefore * 24
    return when (h) {
        0 -> "سر موعد سررسید"
        1 -> "1 ساعت قبل"
        3 -> "3 ساعت قبل"
        6 -> "6 ساعت قبل"
        12 -> "12 ساعت قبل"
        24 -> "1 روز قبل"
        48 -> "2 روز قبل"
        72 -> "3 روز قبل"
        120 -> "5 روز قبل"
        168 -> "1 هفته قبل"
        else -> if (h < 24) "$h ساعت قبل" else "${h / 24} روز قبل"
    }
}

/**
 * Installment & Loan Reminder component (یادآوری اقساط و وام‌ها)
 * Includes direct bank addition, total loan amount, monthly installment amount,
 * notification scheduling, and direct expense logging.
 */
@Composable
fun InstallmentReminderCard(
    onRegisterExpense: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("installment_reminders_prefs", Context.MODE_PRIVATE) }

    var installmentsList by remember {
        mutableStateOf(loadInstallmentsFromPrefs(prefs))
    }

    var showAddEditDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<InstallmentItem?>(null) }
    var itemToDelete by remember { mutableStateOf<InstallmentItem?>(null) }

    // Request Notification permission for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "مجوز اعلان‌ها فعال شد.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        LoanNotificationHelper.createNotificationChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    fun saveAndRefresh(newList: List<InstallmentItem>) {
        installmentsList = newList
        saveInstallmentsToPrefs(prefs, newList)
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val vibrantPurple = if (isDark) Color(0xFFC084FC) else Color(0xFF7C3AED)
    val bgPurple = if (isDark) Color(0xFF3B0764) else Color(0xFFF5F3FF)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("installment_reminder_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.6.dp, vibrantPurple),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.5.dp)
    ) {
        Column(modifier = Modifier.padding(scaledDp(14f))) {
            // Header Row: Clean Title without lengthy subtitle description
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = bgPurple,
                        modifier = Modifier.size(scaledDp(38f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = null,
                                tint = vibrantPurple,
                                modifier = Modifier.size(scaledDp(22f))
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "یادآوری اقساط و وام‌ها",
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Black,
                        color = appTextColor(MaterialTheme.colorScheme.onSurface),
                        softWrap = true
                    )
                }

                // Dedicated Quick Add Bank/Loan Button (Joyful, energetic button)
                Button(
                    onClick = {
                        itemToEdit = null
                        showAddEditDialog = true
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFFA855F7) else Color(0xFF7C3AED)),
                    contentPadding = PaddingValues(horizontal = scaledDp(10f), vertical = scaledDp(6f)),
                    modifier = Modifier.heightIn(min = scaledDp(36f))
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        modifier = Modifier.size(scaledDp(16f))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "+ بانک / وام جدید",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Total Overview Stats Row with Lively Cheerful Theme Colors
            val totalMonthlyBurden = installmentsList.sumOf { it.monthlyAmount }
            val totalPrincipalLoans = installmentsList.sumOf { if (it.totalLoanAmount > 0) it.totalLoanAmount else (it.monthlyAmount * it.totalInstallments) }
            val totalRemainingDebt = installmentsList.sumOf {
                val remainingCount = (it.totalInstallments - it.paidInstallments).coerceAtLeast(0)
                remainingCount * it.monthlyAmount
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Monthly Burden Card (Vibrant Royal Violet)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF4C1D95) else Color(0xFFF3E8FF),
                    border = BorderStroke(1.4.dp, if (isDark) Color(0xFFA855F7) else Color(0xFFC084FC)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(scaledDp(10f))) {
                        Text(
                            text = "قسط ماهانه:",
                            fontSize = 10.5.sp,
                            color = if (isDark) Color(0xFFE9D5FF) else Color(0xFF6B21A8),
                            fontWeight = FontWeight.Bold,
                            softWrap = true
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = enDigits(PersianDateHelper.formatCurrency(totalMonthlyBurden)),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isDark) Color(0xFFFFFFFF) else Color(0xFF581C87),
                            softWrap = true
                        )
                    }
                }

                // Total Loans Principal Card (Vibrant Joyful Cyan / Ocean Sky)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF0369A1) else Color(0xFFE0F2FE),
                    border = BorderStroke(1.4.dp, if (isDark) Color(0xFF38BDF8) else Color(0xFF7DD3FC)),
                    modifier = Modifier.weight(1.1f)
                ) {
                    Column(modifier = Modifier.padding(scaledDp(10f))) {
                        Text(
                            text = "مجموع مبالغ وام‌ها:",
                            fontSize = 10.5.sp,
                            color = if (isDark) Color(0xFFE0F2FE) else Color(0xFF0284C7),
                            fontWeight = FontWeight.Bold,
                            softWrap = true
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = enDigits(PersianDateHelper.formatCurrency(totalPrincipalLoans)),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isDark) Color(0xFFFFFFFF) else Color(0xFF075985),
                            softWrap = true
                        )
                    }
                }

                // Total Remaining Debt Card (Vibrant Radiant Coral / Rose)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFFBE123C) else Color(0xFFFFE4E6),
                    border = BorderStroke(1.4.dp, if (isDark) Color(0xFFFB7185) else Color(0xFFFDA4AF)),
                    modifier = Modifier.weight(1.1f)
                ) {
                    Column(modifier = Modifier.padding(scaledDp(10f))) {
                        Text(
                            text = "کل بدهی مانده:",
                            fontSize = 10.5.sp,
                            color = if (isDark) Color(0xFFFFE4E6) else Color(0xFFE11D48),
                            fontWeight = FontWeight.Bold,
                            softWrap = true
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = enDigits(PersianDateHelper.formatCurrency(totalRemainingDebt)),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isDark) Color(0xFFFFFFFF) else Color(0xFF9F1239),
                            softWrap = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Installments List
            if (installmentsList.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "هنوز هیچ بانک یا وامی ثبت نکرده‌اید",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "برای شروع و تنظیم یادآوری نوتیفیکیشن، روی «+ بانک / وام جدید» بزنید.",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    installmentsList.forEach { item ->
                        InstallmentItemRow(
                            item = item,
                            onRegisterDirectExpense = {
                                onRegisterExpense(item.monthlyAmount)
                                val updated = item.copy(paidInstallments = (item.paidInstallments + 1).coerceAtMost(item.totalInstallments))
                                saveAndRefresh(installmentsList.map { if (it.id == item.id) updated else it })
                                Toast.makeText(context, "قسط «${item.title}» در هزینه‌ها ثبت و تعداد تسویه به‌روز شد.", Toast.LENGTH_SHORT).show()
                            },
                            onTestNotification = {
                                LoanNotificationHelper.showTestNotification(context, item.title, item.monthlyAmount)
                                Toast.makeText(context, "اعلان آزمایشی برای وام «${item.title}» ارسال شد.", Toast.LENGTH_SHORT).show()
                            },
                            onIncrementPaid = {
                                if (item.paidInstallments < item.totalInstallments) {
                                    val updated = item.copy(paidInstallments = item.paidInstallments + 1)
                                    saveAndRefresh(installmentsList.map { if (it.id == item.id) updated else it })
                                }
                            },
                            onDecrementPaid = {
                                if (item.paidInstallments > 0) {
                                    val updated = item.copy(paidInstallments = item.paidInstallments - 1)
                                    saveAndRefresh(installmentsList.map { if (it.id == item.id) updated else it })
                                }
                            },
                            onEdit = {
                                itemToEdit = item
                                showAddEditDialog = true
                            },
                            onDelete = {
                                itemToDelete = item
                            }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Dialog
    if (showAddEditDialog) {
        AddEditInstallmentDialog(
            initialItem = itemToEdit,
            onDismiss = {
                showAddEditDialog = false
                itemToEdit = null
            },
            onSave = { newItem ->
                if (itemToEdit != null) {
                    saveAndRefresh(installmentsList.map { if (it.id == newItem.id) newItem else it })
                    Toast.makeText(context, "اطلاعات وام و یادآوری به‌روزرسانی شد", Toast.LENGTH_SHORT).show()
                } else {
                    saveAndRefresh(listOf(newItem) + installmentsList)
                    Toast.makeText(context, "وام جدید از بانک «${newItem.bankOrLender}» با موفقیت ثبت شد", Toast.LENGTH_SHORT).show()
                }

                // Schedule notification reminder if enabled
                if (newItem.isNotificationEnabled) {
                    LoanNotificationHelper.scheduleLoanReminder(context, newItem)
                } else {
                    LoanNotificationHelper.cancelLoanReminder(context, newItem.id)
                }

                showAddEditDialog = false
                itemToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    itemToDelete?.let { deleting ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = {
                ScaledDialogContent {
                    Text(
                        text = "حذف وام «${deleting.title}»",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp
                    )
                }
            },
            text = {
                ScaledDialogContent {
                    Text(
                        text = "آیا از حذف این وام و سررسیدهای آن اطمینان دارید؟",
                        fontSize = 12.5.sp
                    )
                }
            },
            confirmButton = {
                ScaledDialogContent {
                    Button(
                        onClick = {
                            LoanNotificationHelper.cancelLoanReminder(context, deleting.id)
                            saveAndRefresh(installmentsList.filter { it.id != deleting.id })
                            itemToDelete = null
                            Toast.makeText(context, "وام حذف شد", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Text("حذف شود", color = Color.White)
                    }
                }
            },
            dismissButton = {
                ScaledDialogContent {
                    OutlinedButton(onClick = { itemToDelete = null }) {
                        Text("انصراف")
                    }
                }
            }
        )
    }
}

@Composable
private fun InstallmentItemRow(
    item: InstallmentItem,
    onRegisterDirectExpense: () -> Unit,
    onTestNotification: () -> Unit,
    onIncrementPaid: () -> Unit,
    onDecrementPaid: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val remainingCount = (item.totalInstallments - item.paidInstallments).coerceAtLeast(0)
    val remainingAmount = remainingCount * item.monthlyAmount
    val progress = if (item.totalInstallments > 0) {
        (item.paidInstallments.toFloat() / item.totalInstallments.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val isFullyPaid = item.paidInstallments >= item.totalInstallments

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardBorder = when {
        isFullyPaid -> if (isDark) Color(0xFF34D399) else Color(0xFF10B981)
        else -> if (isDark) Color(0xFFA78BFA) else Color(0xFFC084FC)
    }
    val cardBg = if (isDark) {
        if (isFullyPaid) Color(0xFF064E3B).copy(alpha = 0.55f) else Color(0xFF2E1065).copy(alpha = 0.45f)
    } else {
        if (isFullyPaid) Color(0xFFECFDF5) else Color(0xFFFAF5FF)
    }
    val bankBadgeBg = if (isDark) Color(0xFF7C3AED) else Color(0xFFEDE9FE)
    val bankBadgeText = if (isDark) Color.White else Color(0xFF6D28D9)
    val totalLoanColor = if (isDark) Color(0xFF7DD3FC) else Color(0xFF0284C7)
    val monthlyAmountColor = if (isDark) Color(0xFF6EE7B7) else Color(0xFF059669)
    val reminderBg = if (isDark) Color(0xFF92400E) else Color(0xFFFEF3C7)
    val reminderBorder = if (isDark) Color(0xFFFBBF24) else Color(0xFFFDE68A)
    val reminderTextColor = if (isDark) Color(0xFFFEF3C7) else Color(0xFF92400E)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(1.5.dp, cardBorder),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(scaledDp(12f))) {
            // Line 1: Title, Bank badge, Notification status and Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.title,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = appTextColor(MaterialTheme.colorScheme.onSurface),
                        softWrap = true,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (item.bankOrLender.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = bankBadgeBg
                        ) {
                            Text(
                                text = item.bankOrLender,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = bankBadgeText,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                softWrap = true
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Test notification bell button
                    IconButton(
                        onClick = onTestNotification,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "تست اعلان",
                            tint = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "ویرایش",
                            tint = if (isDark) Color(0xFF60A5FA) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف",
                            tint = if (isDark) Color(0xFFFB7185) else Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Line 2: Total Loan Amount + Monthly Installment Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (item.totalLoanAmount > 0) {
                        Column {
                            Text(
                                text = "مبلغ کل وام:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = appSecondaryTextColor(MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Text(
                                text = enDigits(PersianDateHelper.formatCurrency(item.totalLoanAmount)),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Black,
                                color = totalLoanColor,
                                softWrap = true
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Column {
                        Text(
                            text = "قسط ماهانه:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = appSecondaryTextColor(MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = enDigits(PersianDateHelper.formatCurrency(item.monthlyAmount)),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Black,
                            color = monthlyAmountColor,
                            softWrap = true
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Due Day & Reminder Badge
                val reminderLabel = formatReminderTimingLabel(item)

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = reminderBg,
                    border = BorderStroke(1.dp, reminderBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (item.isNotificationEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                contentDescription = null,
                                tint = if (item.isNotificationEnabled) (if (isDark) Color(0xFFFCD34D) else Color(0xFFD97706)) else Color(0xFF9CA3AF),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = enDigits("سررسید: ${item.dueDayOfMonth} هر ماه"),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = reminderTextColor,
                                softWrap = true
                            )
                        }
                        if (item.isNotificationEnabled) {
                            Text(
                                text = enDigits("یادآوری: $reminderLabel"),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = reminderTextColor,
                                softWrap = true
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (isFullyPaid) Color(0xFF10B981) else (if (isDark) Color(0xFFC084FC) else Color(0xFF7C3AED)),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(5.dp))

            // Progress text and remaining debt
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = enDigits("${item.paidInstallments} از ${item.totalInstallments} قسط پرداخت شده (${(progress * 100).toInt()}%)"),
                    fontSize = 10.5.sp,
                    color = if (isFullyPaid) (if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857)) else appSecondaryTextColor(MaterialTheme.colorScheme.onSurfaceVariant),
                    fontWeight = FontWeight.Bold,
                    softWrap = true,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = enDigits(if (isFullyPaid) "تسویه کامل وام ✓" else "باقی‌مانده: ${PersianDateHelper.formatCurrency(remainingAmount)}"),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isFullyPaid) (if (isDark) Color(0xFF34D399) else Color(0xFF059669)) else (if (isDark) Color(0xFFFDA4AF) else Color(0xFFE11D48)),
                    softWrap = true
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Button 1: "+ ثبت مستقیم در هزینه‌ها"
                Button(
                    onClick = onRegisterDirectExpense,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .heightIn(min = scaledDp(40f)),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF10B981) else Color(0xFF059669))
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(scaledDp(14f))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ثبت در هزینه‌ها",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        softWrap = true
                    )
                }

                // Button 2: Quick Paid count toggler (+1 and -1)
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0xFF7C3AED) else Color(0xFFEDE9FE),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFFA78BFA) else Color(0xFFDDD6FE)),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = scaledDp(36f))
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onIncrementPaid() }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = scaledDp(6f), vertical = scaledDp(6f))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "پرداخت شد",
                                    tint = if (isDark) Color.White else Color(0xFF6D28D9),
                                    modifier = Modifier.size(scaledDp(13f))
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "قسط دادم",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF6D28D9)
                                )
                            }
                        }
                    }

                    if (item.paidInstallments > 0) {
                        IconButton(
                            onClick = onDecrementPaid,
                            modifier = Modifier.size(scaledDp(34f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "کاهش",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(scaledDp(15f))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddEditInstallmentDialog(
    initialItem: InstallmentItem?,
    onDismiss: () -> Unit,
    onSave: (InstallmentItem) -> Unit
) {
    var title by remember { mutableStateOf(initialItem?.title ?: "") }
    var bank by remember { mutableStateOf(initialItem?.bankOrLender ?: "") }
    var totalLoanStr by remember { mutableStateOf(if ((initialItem?.totalLoanAmount ?: 0L) > 0) initialItem?.totalLoanAmount.toString() else "") }
    var amountStr by remember { mutableStateOf(initialItem?.monthlyAmount?.toString() ?: "") }
    var dueDay by remember { mutableIntStateOf(initialItem?.dueDayOfMonth ?: 25) }

    val initialOffsetHours = initialItem?.let {
        if (it.reminderOffsetHours > 0 || it.reminderDaysBefore == 0) it.reminderOffsetHours else it.reminderDaysBefore * 24
    } ?: 24
    var reminderOffsetHours by remember { mutableIntStateOf(initialOffsetHours) }
    // Custom unit: true = hours, false = days
    var useHoursUnit by remember { mutableStateOf(initialOffsetHours in 1..23) }
    var customOffsetStr by remember {
        val v = if (initialOffsetHours in 1..23) initialOffsetHours else (initialOffsetHours / 24)
        mutableStateOf(v.toString())
    }

    var notificationEnabled by remember { mutableStateOf(initialItem?.isNotificationEnabled ?: true) }
    var totalCountStr by remember { mutableStateOf(initialItem?.totalInstallments?.toString() ?: "24") }
    var paidCountStr by remember { mutableStateOf(initialItem?.paidInstallments?.toString() ?: "0") }
    var note by remember { mutableStateOf(initialItem?.note ?: "") }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            ScaledDialogContent {
                Text(
                    text = if (initialItem != null) "ویرایش اطلاعات وام و یادآوری" else "افزودن بانک و وام جدید",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            }
        },
        text = {
            ScaledDialogContent {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(scaledDp(9f))
            ) {
                // Bank or Lender
                OutlinedTextField(
                    value = bank,
                    onValueChange = { bank = it },
                    label = { Text("نام بانک یا موسسه (مثلاً: بانک رسالت، مسکن، ملی)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان وام (مثلاً: وام مسکن، خرید خودرو، جهیزیه)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Total Loan Amount
                OutlinedTextField(
                    value = totalLoanStr,
                    onValueChange = { input ->
                        totalLoanStr = PersianDateHelper.toEnglishDigits(input).filter { it.isDigit() }
                    },
                    label = { Text("مبلغ کل وام (تومان)") },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = OutfitFontFamily),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                if (totalLoanStr.isNotBlank()) {
                    val amt = totalLoanStr.toLongOrNull() ?: 0L
                    Text(
                        text = enDigits("مبلغ وام: ${PersianDateHelper.formatCurrency(amt)}"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF7DD3FC) else Color(0xFF0284C7)
                    )
                }

                // Monthly Installment Amount
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { input ->
                        amountStr = PersianDateHelper.toEnglishDigits(input).filter { it.isDigit() }
                    },
                    label = { Text("مبلغ قسط هر ماه (تومان)") },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = OutfitFontFamily),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                if (amountStr.isNotBlank()) {
                    val amt = amountStr.toLongOrNull() ?: 0L
                    Text(
                        text = enDigits("مبلغ هر قسط: ${PersianDateHelper.formatCurrency(amt)}"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF059669)
                    )
                }

                // Due Day of Month
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = enDigits("روز سررسید قسط در هر ماه: $dueDay"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row {
                        IconButton(
                            onClick = { if (dueDay > 1) dueDay-- },
                            modifier = Modifier.size(scaledDp(32f))
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(scaledDp(16f)))
                        }
                        IconButton(
                            onClick = { if (dueDay < 31) dueDay++ },
                            modifier = Modifier.size(scaledDp(32f))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(scaledDp(16f)))
                        }
                    }
                }

                // Notification Reminder Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = Color(0xFF8B5CF6),
                            modifier = Modifier.size(scaledDp(18f))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "یادآوری نوتیفیکیشن",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Switch(
                        checked = notificationEnabled,
                        onCheckedChange = { notificationEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF8B5CF6))
                    )
                }

                // Flexible Notification Offset: Quick Hours & Days + Custom input
                if (notificationEnabled) {
                    Text(
                        text = "زمان ارسال اعلان قبل از سررسید (ساعت یا روز):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = appSecondaryTextColor(MaterialTheme.colorScheme.onSurfaceVariant)
                    )

                    val presets = listOf(
                        0 to "سر موعد",
                        1 to "1 ساعت قبل",
                        3 to "3 ساعت قبل",
                        6 to "6 ساعت قبل",
                        12 to "12 ساعت قبل",
                        24 to "1 روز قبل",
                        48 to "2 روز قبل",
                        72 to "3 روز قبل",
                        120 to "5 روز قبل"
                    )

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(presets) { (hours, label) ->
                            val isSel = reminderOffsetHours == hours
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) Color(0xFF7C3AED) else (if (isDark) Color(0xFF4C1D95) else Color(0xFFEDE9FE)),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSel) Color(0xFFA855F7) else Color(0xFFC4B5FD).copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        reminderOffsetHours = hours
                                        if (hours == 0) {
                                            customOffsetStr = "0"
                                        } else if (hours < 24) {
                                            useHoursUnit = true
                                            customOffsetStr = hours.toString()
                                        } else {
                                            useHoursUnit = false
                                            customOffsetStr = (hours / 24).toString()
                                        }
                                    }
                            ) {
                                Text(
                                    text = enDigits(label),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else (if (isDark) Color(0xFFE9D5FF) else Color(0xFF6D28D9)),
                                    modifier = Modifier.padding(horizontal = scaledDp(10f), vertical = scaledDp(6f)),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    // Custom input row: [عدد دلخواه] + [ساعت قبل / روز قبل]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customOffsetStr,
                            onValueChange = { input ->
                                val digits = PersianDateHelper.toEnglishDigits(input).filter { it.isDigit() }
                                customOffsetStr = digits
                                val num = (digits.toIntOrNull() ?: 0).coerceIn(0, 720)
                                reminderOffsetHours = if (useHoursUnit) num else num * 24
                            },
                            label = { Text("مقدار دلخواه", fontSize = 11.sp) },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = OutfitFontFamily),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )

                        // Toggle between Hours and Days
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (useHoursUnit) Color(0xFF0284C7) else (if (isDark) Color(0xFF1E3A8A) else Color(0xFFE0F2FE)),
                            modifier = Modifier
                                .heightIn(min = scaledDp(46f))
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    useHoursUnit = true
                                    val num = (customOffsetStr.toIntOrNull() ?: 1).coerceIn(0, 720)
                                    reminderOffsetHours = num
                                }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = scaledDp(12f), vertical = scaledDp(8f))
                            ) {
                                Text(
                                    text = "ساعت قبل",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (useHoursUnit) Color.White else (if (isDark) Color(0xFFBAE6FD) else Color(0xFF0369A1))
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (!useHoursUnit) Color(0xFF059669) else (if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5)),
                            modifier = Modifier
                                .heightIn(min = scaledDp(46f))
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    useHoursUnit = false
                                    val num = (customOffsetStr.toIntOrNull() ?: 1).coerceIn(0, 30)
                                    reminderOffsetHours = num * 24
                                }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = scaledDp(12f), vertical = scaledDp(8f))
                            ) {
                                Text(
                                    text = "روز قبل",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!useHoursUnit) Color.White else (if (isDark) Color(0xFFA7F3D0) else Color(0xFF047857))
                                )
                            }
                        }
                    }
                }

                // Total installments and Paid installments
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = totalCountStr,
                        onValueChange = { input ->
                            totalCountStr = PersianDateHelper.toEnglishDigits(input).filter { it.isDigit() }
                        },
                        label = { Text("کل اقساط") },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = OutfitFontFamily),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = paidCountStr,
                        onValueChange = { input ->
                            paidCountStr = PersianDateHelper.toEnglishDigits(input).filter { it.isDigit() }
                        },
                        label = { Text("پرداخت شده") },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = OutfitFontFamily),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            }
        },
        confirmButton = {
            ScaledDialogContent {
                Button(
                    onClick = {
                        val cleanAmt = amountStr.toLongOrNull() ?: 0L
                        val cleanTotalLoan = totalLoanStr.toLongOrNull() ?: (cleanAmt * (totalCountStr.toIntOrNull() ?: 24))
                        val cleanTotal = totalCountStr.toIntOrNull() ?: 24
                        val cleanPaid = paidCountStr.toIntOrNull() ?: 0
                        val finalTitle = if (title.isNotBlank()) title.trim() else if (bank.isNotBlank()) "وام $bank" else "وام بانکی"
                        val computedDays = reminderOffsetHours / 24
                        val item = (initialItem?.copy(
                            title = finalTitle,
                            bankOrLender = bank.trim(),
                            totalLoanAmount = cleanTotalLoan,
                            monthlyAmount = cleanAmt,
                            dueDayOfMonth = dueDay,
                            reminderOffsetHours = reminderOffsetHours,
                            reminderDaysBefore = computedDays,
                            isNotificationEnabled = notificationEnabled,
                            totalInstallments = cleanTotal,
                            paidInstallments = cleanPaid.coerceIn(0, cleanTotal),
                            note = note.trim()
                        )) ?: InstallmentItem(
                            title = finalTitle,
                            bankOrLender = bank.trim(),
                            totalLoanAmount = cleanTotalLoan,
                            monthlyAmount = cleanAmt,
                            dueDayOfMonth = dueDay,
                            reminderOffsetHours = reminderOffsetHours,
                            reminderDaysBefore = computedDays,
                            isNotificationEnabled = notificationEnabled,
                            totalInstallments = cleanTotal,
                            paidInstallments = cleanPaid.coerceIn(0, cleanTotal),
                            note = note.trim()
                        )
                        onSave(item)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                ) {
                    Text("ذخیره وام و یادآوری")
                }
            }
        },
        dismissButton = {
            ScaledDialogContent {
                OutlinedButton(onClick = onDismiss) {
                    Text("انصراف")
                }
            }
        }
    )
}

/**
 * Persists installments list into SharedPreferences as JSON.
 */
private fun saveInstallmentsToPrefs(prefs: android.content.SharedPreferences, list: List<InstallmentItem>) {
    val array = JSONArray()
    for (item in list) {
        val obj = JSONObject().apply {
            put("id", item.id)
            put("title", item.title)
            put("bank", item.bankOrLender)
            put("totalLoan", item.totalLoanAmount)
            put("amount", item.monthlyAmount)
            put("dueDay", item.dueDayOfMonth)
            put("reminderOffsetHours", item.reminderOffsetHours)
            put("reminderDays", item.reminderDaysBefore)
            put("notifEnabled", item.isNotificationEnabled)
            put("total", item.totalInstallments)
            put("paid", item.paidInstallments)
            put("note", item.note)
        }
        array.put(obj)
    }
    prefs.edit().putString("saved_installments_list", array.toString()).apply()
}

/**
 * Loads installments list from SharedPreferences or preloads default practical samples.
 */
private fun loadInstallmentsFromPrefs(prefs: android.content.SharedPreferences): List<InstallmentItem> {
    val raw = prefs.getString("saved_installments_list", null)
    if (!raw.isNullOrBlank()) {
        try {
            val array = JSONArray(raw)
            val list = mutableListOf<InstallmentItem>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val legacyDays = obj.optInt("reminderDays", 1)
                val offsetHours = if (obj.has("reminderOffsetHours")) {
                    obj.optInt("reminderOffsetHours", legacyDays * 24)
                } else {
                    legacyDays * 24
                }
                list.add(
                    InstallmentItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = obj.optString("title", "وام"),
                        bankOrLender = obj.optString("bank", ""),
                        totalLoanAmount = obj.optLong("totalLoan", 100_000_000L),
                        monthlyAmount = obj.optLong("amount", 0L),
                        dueDayOfMonth = obj.optInt("dueDay", 25),
                        reminderOffsetHours = offsetHours,
                        reminderDaysBefore = legacyDays,
                        isNotificationEnabled = obj.optBoolean("notifEnabled", true),
                        totalInstallments = obj.optInt("total", 12),
                        paidInstallments = obj.optInt("paid", 0),
                        note = obj.optString("note", "")
                    )
                )
            }
            if (list.isNotEmpty()) {
                return list
            }
        } catch (_: Exception) {
        }
    }

    // Default sample loans with bank names and total amounts
    val defaults = listOf(
        InstallmentItem(
            title = "وام قرض‌الحسنه کارگشایی",
            bankOrLender = "بانک رسالت",
            totalLoanAmount = 150_000_000L,
            monthlyAmount = 6_500_000L,
            dueDayOfMonth = 10,
            reminderOffsetHours = 24,
            reminderDaysBefore = 1,
            isNotificationEnabled = true,
            totalInstallments = 24,
            paidInstallments = 8
        ),
        InstallmentItem(
            title = "خرید اقساطی کالا",
            bankOrLender = "دیجی‌پی / بانک تجارت",
            totalLoanAmount = 23_400_000L,
            monthlyAmount = 1_950_000L,
            dueDayOfMonth = 25,
            reminderOffsetHours = 48,
            reminderDaysBefore = 2,
            isNotificationEnabled = true,
            totalInstallments = 12,
            paidInstallments = 3
        )
    )
    saveInstallmentsToPrefs(prefs, defaults)
    return defaults
}
