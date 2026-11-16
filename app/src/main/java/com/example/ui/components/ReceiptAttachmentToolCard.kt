package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.model.ExpenseEntity
import com.example.data.model.ResolvedExpense
import com.example.ui.theme.appSecondaryTextColor
import com.example.ui.theme.appTextColor
import com.example.ui.theme.enDigits
import com.example.ui.theme.scaledDp
import com.example.utils.PersianDateHelper
import com.example.utils.ReceiptImageHelper
import java.io.File

/**
 * Tool section for capturing/picking a bank or shopping receipt photo via Camera or Photo Picker,
 * and attaching it to any registered expense or creating a new expense with the receipt attached.
 */
@Composable
fun ReceiptAttachmentToolCard(
    expenses: List<ResolvedExpense>,
    onAttachReceiptToExpense: (expenseId: Long, receiptPath: String) -> Unit,
    onCreateExpenseWithReceipt: (receiptPath: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val accentColor = if (isDark) Color(0xFFF472B6) else Color(0xFFDB2777)
    val accentBg = if (isDark) Color(0xFF500724) else Color(0xFFFCE7F3)

    var selectedReceiptPath by remember { mutableStateOf<String?>(null) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var previewExpense by remember { mutableStateOf<ExpenseEntity?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val file = pendingCameraFile
        if (success && file != null && file.exists() && file.length() > 0L) {
            selectedReceiptPath = file.absolutePath
            Toast.makeText(context, "تصویر رسید با دوربین ثبت شد", Toast.LENGTH_SHORT).show()
        } else {
            file?.delete()
        }
        pendingCameraFile = null
        pendingCameraUri = null
    }

    fun startCameraCapture() {
        try {
            val (file, uri) = ReceiptImageHelper.createCameraCaptureTarget(context)
            pendingCameraFile = file
            pendingCameraUri = uri
            takePictureLauncher.launch(uri)
        } catch (_: Exception) {
            Toast.makeText(context, "امکان باز کردن دوربین وجود ندارد", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCameraCapture()
        } else {
            Toast.makeText(context, "برای عکسبرداری از رسید، مجوز دوربین لازم است", Toast.LENGTH_LONG).show()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val savedPath = ReceiptImageHelper.copyUriToInternalReceiptFile(context, uri)
            if (savedPath != null) {
                selectedReceiptPath = savedPath
                Toast.makeText(context, "تصویر رسید از گالری انتخاب شد", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "خطا در خواندن تصویر انتخاب‌شده", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Deduplicate annual amortized synthetic entries (show only real/unique original expenses sorted newest first)
    val baseExpenses = remember(expenses, searchQuery) {
        val unique = expenses
            .filter { it.entity.id >= 0L }
            .sortedByDescending { it.entity.timestamp }
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) {
            unique.take(25)
        } else {
            unique.filter {
                it.entity.title.lowercase().contains(q) ||
                    it.effectiveFolder.lowercase().contains(q) ||
                    it.entity.shamsiDateString.contains(q)
            }.take(25)
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.6.dp, accentColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.5.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("receipt_attachment_tool_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(scaledDp(14f))
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = accentBg,
                    modifier = Modifier.size(scaledDp(38f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(scaledDp(21f))
                        )
                    }
                }
                Spacer(modifier = Modifier.width(scaledDp(10f)))
                Text(
                    text = "پیوست تصویر رسید بانکی",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = appTextColor(MaterialTheme.colorScheme.onSurface),
                    softWrap = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Camera & Gallery Selection Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) {
                            startCameraCapture()
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFFDB2777) else Color(0xFFEC4899),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = scaledDp(48f))
                        .testTag("receipt_tool_camera_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(scaledDp(17f))
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "عکسبرداری با دوربین",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp,
                        softWrap = true
                    )
                }

                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    border = BorderStroke(1.3.dp, accentColor),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = scaledDp(48f))
                        .testTag("receipt_tool_gallery_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "انتخاب از گالری",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp,
                        softWrap = true
                    )
                }
            }

            // Selected Image Preview + Create New Expense Option
            AnimatedVisibility(visible = !selectedReceiptPath.isNullOrBlank()) {
                val path = selectedReceiptPath.orEmpty()
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = accentBg.copy(alpha = if (isDark) 0.4f else 0.55f),
                        border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    AsyncImage(
                                        model = File(path),
                                        contentDescription = "پیش‌نمایش رسید انتخابی",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "تصویر رسید آماده الصاق است",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = accentColor,
                                            softWrap = true
                                        )
                                        Text(
                                            text = "هزینه مورد نظر را از لیست زیر انتخاب کنید یا هزینه جدید بسازید",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            lineHeight = 16.sp,
                                            softWrap = true
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { selectedReceiptPath = null },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "حذف تصویر انتخابی",
                                        tint = Color(0xFFDC2626)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = { onCreateExpenseWithReceipt(path) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("receipt_create_new_expense_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ثبت هزینه جدید با این رسید",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    softWrap = true
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "انتخاب هزینه برای پیوست رسید:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("جستجوی عنوان خرید (مثال: سوپرمارکت)", fontSize = 12.sp) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("receipt_tool_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (baseExpenses.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "هزینه‌ای یافت نشد. ابتدا هزینه‌ای ثبت کنید یا گزینه ثبت هزینه جدید با رسید را بزنید.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    baseExpenses.forEach { resolved ->
                        val item = resolved.entity
                        val hasExistingReceipt = !item.receiptImagePath.isNullOrBlank()
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
                            border = BorderStroke(
                                1.dp,
                                if (hasExistingReceipt) Color(0xFF10B981).copy(alpha = 0.6f)
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = enDigits(item.title),
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = appTextColor(MaterialTheme.colorScheme.onSurface),
                                            softWrap = true
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = enDigits("${PersianDateHelper.formatCurrency(item.amount)} • ${item.shamsiDateString}"),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = appSecondaryTextColor(MaterialTheme.colorScheme.onSurfaceVariant),
                                            softWrap = true
                                        )
                                    }

                                    if (hasExistingReceipt) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFD1FAE5),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable { previewExpense = item }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AttachFile,
                                                    contentDescription = "دارای رسید پیوست",
                                                    tint = Color(0xFF047857),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "دارای رسید",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF047857),
                                                    softWrap = true
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val canAttach = !selectedReceiptPath.isNullOrBlank()
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (canAttach) accentColor else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = scaledDp(48f))
                                            .clip(RoundedCornerShape(10.dp))
                                            .testTag("attach_receipt_expense_${item.id}")
                                            .clickable {
                                                val path = selectedReceiptPath
                                                if (!path.isNullOrBlank()) {
                                                    onAttachReceiptToExpense(item.id, path)
                                                    Toast.makeText(
                                                        context,
                                                        "تصویر رسید به «${item.title}» پیوست شد",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        "ابتدا از دوربین یا گالری تصویر رسید را انتخاب کنید",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = if (hasExistingReceipt && canAttach) Icons.Default.Check else Icons.Default.ReceiptLong,
                                                contentDescription = "الصاق رسید به هزینه",
                                                tint = if (canAttach) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = if (hasExistingReceipt) "جایگزینی رسید" else "الصاق رسید به این هزینه",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (canAttach) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                softWrap = true
                                            )
                                        }
                                    }

                                    if (hasExistingReceipt) {
                                        OutlinedButton(
                                            onClick = { previewExpense = item },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                            modifier = Modifier.heightIn(min = 48.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Visibility,
                                                contentDescription = "مشاهده تصویر رسید",
                                                tint = Color(0xFF0284C7),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "مشاهده",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0284C7),
                                                maxLines = 1
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                ReceiptImageHelper.shareExpenseWithReceipt(context, item)
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, Color(0xFF10B981)),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .testTag("share_tool_receipt_expense_${item.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = "اشتراک‌گذاری هزینه و رسید",
                                                tint = Color(0xFF059669),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "اشتراک",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF059669),
                                                maxLines = 1
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
    }

    previewExpense?.let { exp ->
        ReceiptPreviewDialog(
            expense = exp,
            onDismissRequest = { previewExpense = null }
        )
    }
}
