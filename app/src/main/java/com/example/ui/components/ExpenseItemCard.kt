package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ResolvedExpense
import com.example.ui.theme.ScaledDialogContent
import com.example.ui.theme.appSecondaryTextColor
import com.example.ui.theme.appTextColor
import com.example.ui.theme.enDigits
import com.example.ui.theme.scaledDp
import com.example.utils.PersianDateHelper
import com.example.utils.ReceiptImageHelper

/**
 * Modern Expense Item Card with full text visibility and amounts positioned cleanly underneath.
 * Follows the user's explicit layout rule:
 * - Persian titles and texts are NEVER truncated or shortened with ellipsis (...).
 * - Amounts (اعداد و ارقام) are placed directly UNDER the text.
 * - Generous spacing, clean folder badge, full note visibility, and accessible action buttons.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpenseItemCard(
    resolvedExpense: ResolvedExpense,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    onEditClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val entity = resolvedExpense.entity
    val isAnnual = entity.isAnnual
    val isRecurring = resolvedExpense.isRecurring
    val cleanTitle = entity.title.replace(Regex("[\\r\\n]+"), " ").trim()
    val cleanNote = entity.note.replace(Regex("[\\r\\n]+"), " ").trim()
    val hasReceipt = !entity.receiptImagePath.isNullOrBlank()
    var showReceiptPreview by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val cardBorderColor = when {
        isAnnual -> Color(0xFFF59E0B) // Cheerful Amber Gold
        isRecurring -> Color(0xFF0284C7) // Vivid Sky Blue
        resolvedExpense.effectiveFolder.isNotBlank() -> {
            val f = resolvedExpense.effectiveFolder
            when {
                f.contains("بنزین") || f.contains("ماشین") || f.contains("خودرو") || f.contains("موتور") -> Color(0xFFF97316) // Cheerful Orange
                f.contains("اینترنت") || f.contains("شارژ") || f.contains("قبوض") -> Color(0xFF8B5CF6) // Cheerful Purple
                f.contains("سوپرمارکت") || f.contains("خرید") || f.contains("میوه") -> Color(0xFF10B981) // Cheerful Emerald
                f.contains("درمان") || f.contains("دارو") || f.contains("پزشک") -> Color(0xFFEC4899) // Cheerful Pink / Rose
                f.contains("اجاره") || f.contains("خانه") || f.contains("مسکن") -> Color(0xFFEAB308) // Cheerful Yellow / Amber
                else -> Color(0xFF10B981) // Cheerful Emerald
            }
        }
        else -> Color(0xFF10B981) // Vibrant Emerald
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = onEditClick != null) { onEditClick?.invoke() }
            .testTag("expense_item_${entity.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.4.dp, cardBorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(scaledDp(14f))
        ) {
            // Line 1: Category Icon + Full Title (100% visible, softWrap = true, NO ellipsis) + Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.Top
                ) {
                    // Category icon indicator
                    Surface(
                        shape = CircleShape,
                        color = if (isAnnual) Color(0xFFFEF3C7)
                        else if (isRecurring) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(scaledDp(36f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isAnnual) Icons.Default.Shield
                                else if (isRecurring) Icons.Default.Repeat
                                else Icons.Default.Folder,
                                contentDescription = null,
                                tint = if (isAnnual) Color(0xFFD97706)
                                else if (isRecurring) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(scaledDp(19f))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Title: Takes the full width needed, wraps naturally, never truncated with "..."
                    Text(
                        text = enDigits(cleanTitle),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = appTextColor(MaterialTheme.colorScheme.onSurface),
                        lineHeight = 22.sp,
                        softWrap = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Action buttons: Edit & Delete at top corner with clear, vivid, cheerful containers
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(start = 6.dp)
                ) {
                    if (onEditClick != null) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFE0F2FE),
                            border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable { onEditClick() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "ویرایش هزینه",
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }

                    // Delete button: bright, high-contrast, vivid red pill (حل ۱۰۰٪ مشکل تیره بودن سطل آشغال)
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFEE2E2),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .testTag("delete_expense_button_${entity.id}")
                            .clickable { showDeleteConfirmDialog = true }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "حذف هزینه",
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Line 2: Amount placed UNDER the text (اعداد و ارقام در زیر نوشته‌ها) + Folder Badge (wraps cleanly with zero clipping)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Amount in crisp, high-contrast emerald/amber
                Text(
                    text = enDigits(PersianDateHelper.formatCurrency(entity.amount)),
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isAnnual) Color(0xFFB45309) else MaterialTheme.colorScheme.primary,
                    softWrap = true
                )

                // Folder Badge and annual share indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isAnnual) Color(0xFFFEF3C7)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .padding(horizontal = scaledDp(8f), vertical = scaledDp(4f))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = if (isAnnual) Color(0xFF78350F) else appTextColor(MaterialTheme.colorScheme.onSurface),
                                modifier = Modifier.size(scaledDp(13f))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = enDigits(resolvedExpense.effectiveFolder),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAnnual) Color(0xFF78350F) else appTextColor(MaterialTheme.colorScheme.onSurface),
                                softWrap = true
                            )
                        }
                    }

                    if (isAnnual) {
                        Spacer(modifier = Modifier.width(6.dp))
                        val label = if (entity.id < 0) {
                            "(سهم ماهانه 1/12)"
                        } else {
                            "(سرشکن: ${PersianDateHelper.formatCurrency(entity.amount / 12)}/ماه)"
                        }
                        Text(
                            text = enDigits(label),
                            fontSize = 11.sp,
                            color = Color(0xFFB45309),
                            fontWeight = FontWeight.Bold,
                            softWrap = true
                        )
                    }
                }
            }

            // Line 3: Note / Description if present (Full text, wrapped, never cut off with "...")
            if (cleanNote.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = enDigits(cleanNote),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = appTextColor(MaterialTheme.colorScheme.onSurface),
                        lineHeight = 18.sp,
                        softWrap = true,
                        modifier = Modifier.padding(horizontal = scaledDp(10f), vertical = scaledDp(6f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Line 4: Date + Receipt Attachment Indicator Badge (if attached)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = appSecondaryTextColor(MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.size(scaledDp(13f))
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = enDigits(entity.shamsiDateString),
                        fontSize = 12.sp,
                        color = appTextColor(MaterialTheme.colorScheme.onSurface),
                        fontWeight = FontWeight.Bold
                    )
                }

                if (hasReceipt) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFCE7F3),
                            border = BorderStroke(1.dp, Color(0xFFEC4899).copy(alpha = 0.65f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .testTag("expense_receipt_badge_${entity.id}")
                                .clickable { showReceiptPreview = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = "دارای پیوست عکس رسید",
                                    tint = Color(0xFFBE185D),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "دارای پیوست رسید",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF9D174D),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFECFDF5),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.7f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .testTag("share_expense_button_${entity.id}")
                                .clickable {
                                    ReceiptImageHelper.shareExpenseWithReceipt(context, entity)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "اشتراک‌گذاری هزینه و رسید",
                                    tint = Color(0xFF047857),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "اشتراک",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF047857),
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

    if (showReceiptPreview) {
        ReceiptPreviewDialog(
            expense = entity,
            onDismissRequest = { showReceiptPreview = false }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = {
                ScaledDialogContent {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFDC2626)
                    )
                }
            },
            title = {
                ScaledDialogContent {
                    Text(
                        text = "تأیید حذف هزینه",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = appTextColor(MaterialTheme.colorScheme.onSurface)
                    )
                }
            },
            text = {
                ScaledDialogContent {
                    Text(
                        text = enDigits("آیا از حذف هزینه «$cleanTitle» به مبلغ ${PersianDateHelper.formatCurrency(entity.amount)} مطمئن هستید؟"),
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = appTextColor(MaterialTheme.colorScheme.onSurface)
                    )
                }
            },
            confirmButton = {
                ScaledDialogContent {
                    Button(
                        onClick = {
                            showDeleteConfirmDialog = false
                            onDeleteClick()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        modifier = Modifier
                            .heightIn(min = scaledDp(48f))
                            .testTag("confirm_delete_expense_button")
                    ) {
                        Text("بله، حذف شود", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                ScaledDialogContent {
                    OutlinedButton(
                        onClick = { showDeleteConfirmDialog = false },
                        modifier = Modifier
                            .heightIn(min = scaledDp(48f))
                            .testTag("cancel_delete_expense_button")
                    ) {
                        Text("انصراف", color = appTextColor(MaterialTheme.colorScheme.primary))
                    }
                }
            }
        )
    }
}
