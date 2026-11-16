package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ExpenseEntity
import com.example.data.model.ResolvedExpense
import com.example.ui.components.ExpenseItemCard
import com.example.ui.components.ReceiptPreviewDialog
import com.example.ui.theme.ScaledDialogContent
import com.example.ui.theme.appSecondaryTextColor
import com.example.ui.theme.appTextColor
import com.example.ui.theme.enDigits
import com.example.ui.theme.scaledDp
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.utils.PersianDateHelper
import com.example.utils.ReceiptImageHelper

@Composable
fun AnnualCommitmentsScreen(
    viewModel: ExpenseViewModel,
    onAddAnnualExpenseClick: () -> Unit,
    onEditAnnualExpense: (ExpenseEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val annualExpenses by viewModel.annualExpenses.collectAsStateWithLifecycle()
    val totalAnnual by viewModel.totalAnnualCommitments.collectAsStateWithLifecycle()
    val monthlyShare by viewModel.monthlyAmortizedShare.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("annual_commitments_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Header Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "هزینه‌های سالانه و تعهدات دوره‌ای",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = appTextColor(MaterialTheme.colorScheme.onSurface),
                        softWrap = true
                    )
                    Text(
                        text = enDigits("سرشکن شدن هزینه‌های بیمه به 12 ماه سال"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = appSecondaryTextColor(MaterialTheme.colorScheme.onSurfaceVariant),
                        softWrap = true
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFEF3C7),
                    modifier = Modifier.size(scaledDp(40f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(scaledDp(24f))
                        )
                    }
                }
            }
        }

        // Ultra-compact Cheerful Annual Summary & Logic Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFF59E0B)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    // ردیف اول: عنوان و نشان تقسیم بر ۱۲ ماه
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "کل تعهدات سالانه:",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEF3C7),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A))
                        ) {
                            Text(
                                text = enDigits("تقسیم بر 12 ماه"),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E),
                                modifier = Modifier.padding(horizontal = scaledDp(8f), vertical = scaledDp(3f)),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // خط زیر عنوان: عدد مبلغ کل تعهدات سالانه
                    Text(
                        text = enDigits(PersianDateHelper.formatCurrency(totalAnnual)),
                        fontSize = 17.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFB45309),
                        modifier = Modifier.padding(start = 25.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    androidx.compose.material3.HorizontalDivider(color = Color(0xFFFDE68A).copy(alpha = 0.6f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // عنوان سرشکن ماهانه در یک خط
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(scaledDp(16f))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "سرشکن ماهانه (سهم هر ماه):",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF065F46)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // عدد سرشکن ماهانه در خط زیر آن
                    Text(
                        text = enDigits("${PersianDateHelper.formatCurrency(monthlyShare)}/ماه"),
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF059669),
                        modifier = Modifier.padding(start = 22.dp)
                    )
                }
            }
        }

        // Add Button
        item {
            Button(
                onClick = onAddAnnualExpenseClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = scaledDp(48f)),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ثبت تعهد سالانه جدید (بیمه، خودرو، موتور...)",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    softWrap = true
                )
            }
        }

        // Items list
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "لیست تعهدات سالانه ثبت‌شده",
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Black,
                    color = appTextColor(MaterialTheme.colorScheme.onSurface),
                    softWrap = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = enDigits("${annualExpenses.size} مورد"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = appSecondaryTextColor(MaterialTheme.colorScheme.onSurfaceVariant),
                    softWrap = true
                )
            }
        }

        if (annualExpenses.isEmpty()) {
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
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "هنوز تعهد سالانه‌ای ثبت نکرده‌اید",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = appTextColor(MaterialTheme.colorScheme.onSurface)
                        )
                    }
                }
            }
        } else {
            items(annualExpenses) { entity ->
                AnnualCommitmentCard(
                    entity = entity,
                    onEditClick = { onEditAnnualExpense(entity) },
                    onDeleteClick = { viewModel.deleteExpense(entity) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(70.dp)) }
    }
}

/**
 * Cheerful, clear multi-line card for annual commitments.
 * Explicitly separates sentences (titles) from numbers and amounts across full-width rows.
 * Prevents any vertical letter/number wrapping (جلوگیری ۱۰۰٪ از هرگونه نوشته عمودی).
 */
@Composable
fun AnnualCommitmentCard(
    entity: ExpenseEntity,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cleanTitle = entity.title.replace(Regex("[\\r\\n]+"), " ").trim()
    val monthlyAmortized = entity.amount / 12L
    var showReceiptPreview by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onEditClick() }
            .testTag("annual_commitment_card_${entity.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFF59E0B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // سطر اول: جمله و عنوان تعهد سالانه کاملاً مجزا در سمت راست + دکمه‌های ویرایش و حذف در سمت چپ
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
                        color = Color(0xFFFEF3C7),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = cleanTitle,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black,
                        maxLines = 2,
                        lineHeight = 20.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // دکمه‌های عملیاتی: ویرایش (آبی فیروزه‌ای) و حذف (قرمز پررنگ روشن)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // ویرایش
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFE0F2FE),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBAE6FD)),
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable { onEditClick() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "ویرایش",
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // حذف
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFEE2E2),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .testTag("delete_annual_button_${entity.id}")
                            .clickable { showDeleteConfirmDialog = true }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "حذف",
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(9.dp))

            // سطر دوم: مبلغ کل سالانه (نوشته در خط اول، عدد در خط زیر آن به دستور کاربر)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFFEF3C7),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "مبلغ کل تعهد سالانه:",
                        fontSize = 12.5.sp,
                        color = Color(0xFF92400E),
                        fontWeight = FontWeight.Bold,
                        softWrap = true
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = enDigits(PersianDateHelper.formatCurrency(entity.amount)),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFB45309),
                        softWrap = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // سطر سوم: سرشکن ماهانه (سهم هر ماه) (نوشته در خط اول، عدد در خط زیر آن به دستور کاربر)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFECFDF5),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = scaledDp(12f), vertical = scaledDp(8f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(scaledDp(15f))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "سرشکن ماهانه (سهم هر ماه):",
                            fontSize = 12.5.sp,
                            color = Color(0xFF065F46),
                            fontWeight = FontWeight.Bold,
                            softWrap = true
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = enDigits("${PersianDateHelper.formatCurrency(monthlyAmortized)}/ماه"),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF047857),
                        modifier = Modifier.padding(start = 21.dp),
                        softWrap = true
                    )
                }
            }

            // سطر چهارم اختیاری: در صورتی که توضیحات یا یادداشتی ثبت شده باشد
            if (entity.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "یادداشت: ${entity.note}",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    maxLines = 2,
                    lineHeight = 16.sp
                )
            }

            if (!entity.receiptImagePath.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFCE7F3),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEC4899).copy(alpha = 0.65f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
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
                            Spacer(modifier = Modifier.width(4.dp))
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
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.65f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .testTag("share_annual_expense_${entity.id}")
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
                                contentDescription = "اشتراک‌گذاری تعهد و تصویر رسید",
                                tint = Color(0xFF047857),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "اشتراک",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46),
                                maxLines = 1,
                                softWrap = false
                            )
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
                        text = "تأیید حذف تعهد سالانه",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                ScaledDialogContent {
                    Text(
                        text = enDigits("آیا از حذف تعهد سالانه «$cleanTitle» به مبلغ ${PersianDateHelper.formatCurrency(entity.amount)} مطمئن هستید؟"),
                        fontSize = 13.sp,
                        lineHeight = 20.sp
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
                            .testTag("confirm_delete_annual_button")
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
                            .testTag("cancel_delete_annual_button")
                    ) {
                        Text("انصراف")
                    }
                }
            }
        )
    }
}

