package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.ScaledDialogContent
import com.example.ui.theme.appSecondaryTextColor
import com.example.ui.theme.appTextColor
import com.example.ui.theme.enDigits
import com.example.ui.theme.scaledDp
import com.example.utils.AppFontColorOption
import com.example.utils.FontScaleHelper
import com.example.utils.GitHubUpdateHelper
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsDialog(
    onDismissRequest: () -> Unit,
    onResetData: () -> Unit,
    onBackupData: () -> Unit,
    onRestoreData: () -> Unit,
    totalExpensesCount: Int,
    fontScale: Float = FontScaleHelper.DEFAULT_SCALE,
    onFontScaleChange: (Float) -> Unit = {},
    fontColorOption: AppFontColorOption = AppFontColorOption.DEFAULT,
    onFontColorChange: (AppFontColorOption) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    var showConfirmResetDialog by remember { mutableStateOf(false) }

    var repoInput by remember { mutableStateOf(GitHubUpdateHelper.getSavedRepoInput(context)) }
    var autoCheckAndDownload by remember {
        mutableStateOf(GitHubUpdateHelper.isAutoCheckAndDownloadEnabled(context))
    }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateResult by remember { mutableStateOf<GitHubUpdateHelper.UpdateCheckResult?>(null) }
    var autoDownloadTriggeredThisSession by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (autoCheckAndDownload && GitHubUpdateHelper.parseOwnerAndRepo(repoInput) != null) {
            isCheckingUpdate = true
            val result = GitHubUpdateHelper.checkForUpdate(repoInput)
            updateResult = result
            isCheckingUpdate = false
            if (result.isUpdateAvailable && !autoDownloadTriggeredThisSession && result.downloadUrl.isNotBlank()) {
                autoDownloadTriggeredThisSession = true
                GitHubUpdateHelper.openDownloadUrl(context, result.downloadUrl)
                Toast.makeText(
                    context,
                    "نسخه جدید (${result.latestVersion}) یافت شد و دانلود آغاز گردید",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ScaledDialogContent(
            explicitScale = fontScale,
            explicitColorOption = fontColorOption
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .heightIn(max = 700.dp)
                    .testTag("settings_dialog"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.8.dp, Color(0xFF10B981)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(scaledDp(18f))
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header: Title & Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(scaledDp(38f))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(scaledDp(20f))
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "تنظیمات برنامه",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = appTextColor(MaterialTheme.colorScheme.onSurface)
                            )
                        }

                        IconButton(onClick = onDismissRequest) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "بستن",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // First Feature in Settings: تنظیم اندازه و رنگ فونت و ابعاد کادرها
                    Text(
                        text = "تنظیم اندازه و رنگ فونت و کادرها:",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Black,
                        color = appTextColor(MaterialTheme.colorScheme.onSurface)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val fontCardBg = if (isDark) Color(0xFF0F2942) else Color(0xFFF0FDFA)
                    val fontCardBorder = if (isDark) Color(0xFF2DD4BF) else Color(0xFF0D9488)
                    val fontTitleColor = appTextColor(if (isDark) Color(0xFF99F6E4) else Color(0xFF115E59))
                    val fontSubColor = appSecondaryTextColor(if (isDark) Color(0xFFCCFBF1) else Color(0xFF0F766E))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("font_scale_settings_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = fontCardBg),
                        border = BorderStroke(1.5.dp, fontCardBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(scaledDp(12f))
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
                                    Surface(
                                        shape = CircleShape,
                                        color = fontCardBorder.copy(alpha = 0.18f),
                                        modifier = Modifier.size(scaledDp(36f))
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.FormatSize,
                                                contentDescription = "تنظیم اندازه فونت",
                                                tint = fontCardBorder,
                                                modifier = Modifier.size(scaledDp(20f))
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "بزرگ‌نمایی فونت و کادرها",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = fontTitleColor,
                                            softWrap = true
                                        )
                                        Text(
                                            text = enDigits(FontScaleHelper.getScaleLabel(fontScale)),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = fontSubColor,
                                            softWrap = true
                                        )
                                    }
                                }

                                if (abs(fontScale - FontScaleHelper.DEFAULT_SCALE) > 0.01f ||
                                    fontColorOption != AppFontColorOption.DEFAULT
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(9.dp),
                                        color = fontCardBorder.copy(alpha = 0.16f),
                                        border = BorderStroke(1.dp, fontCardBorder),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(9.dp))
                                            .testTag("font_scale_reset_button")
                                            .clickable {
                                                onFontScaleChange(FontScaleHelper.DEFAULT_SCALE)
                                                onFontColorChange(AppFontColorOption.DEFAULT)
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(
                                                horizontal = scaledDp(8f),
                                                vertical = scaledDp(5f)
                                            ),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "بازگشت به پیش‌فرض",
                                                tint = fontTitleColor,
                                                modifier = Modifier.size(scaledDp(13f))
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "پیش‌فرض",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = fontTitleColor
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Minus / Slider / Plus Controls (70% .. 170%)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (fontScale > FontScaleHelper.MIN_SCALE + 0.01f) Color(0xFF0D9488) else Color(0xFFCBD5E1),
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .testTag("font_scale_decrease_button")
                                        .clickable(enabled = fontScale > FontScaleHelper.MIN_SCALE + 0.01f) {
                                            onFontScaleChange(fontScale - FontScaleHelper.STEP)
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Remove,
                                            contentDescription = "کوچک‌تر کردن فونت",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Slider(
                                    value = fontScale,
                                    onValueChange = { onFontScaleChange(it) },
                                    valueRange = FontScaleHelper.MIN_SCALE..FontScaleHelper.MAX_SCALE,
                                    steps = 19,
                                    colors = SliderDefaults.colors(
                                        thumbColor = fontCardBorder,
                                        activeTrackColor = fontCardBorder,
                                        inactiveTrackColor = fontCardBorder.copy(alpha = 0.28f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("font_scale_slider")
                                )

                                Surface(
                                    shape = CircleShape,
                                    color = if (fontScale < FontScaleHelper.MAX_SCALE - 0.01f) Color(0xFF0D9488) else Color(0xFFCBD5E1),
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .testTag("font_scale_increase_button")
                                        .clickable(enabled = fontScale < FontScaleHelper.MAX_SCALE - 0.01f) {
                                            onFontScaleChange(fontScale + FontScaleHelper.STEP)
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "بزرگ‌تر کردن فونت",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Quick Preset Chips (clearly distinct sizes)
                            val presets = listOf(
                                0.80f to "کوچک",
                                1.00f to "متوسط",
                                1.30f to "بزرگ",
                                1.60f to "خیلی بزرگ"
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                presets.forEach { (presetScale, label) ->
                                    val isSelected = abs(fontScale - presetScale) < 0.03f
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) fontCardBorder else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) fontCardBorder else fontCardBorder.copy(alpha = 0.45f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = scaledDp(48f, strength = 0.35f, minFactor = 0.9f))
                                            .clip(RoundedCornerShape(10.dp))
                                            .testTag("font_scale_preset_${(presetScale * 100).toInt()}")
                                            .clickable { onFontScaleChange(presetScale) }
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(
                                                horizontal = scaledDp(4f),
                                                vertical = scaledDp(6f)
                                            )
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                                color = if (isSelected) Color.White else fontTitleColor,
                                                textAlign = TextAlign.Center,
                                                softWrap = true
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = fontCardBorder.copy(alpha = 0.25f))
                            Spacer(modifier = Modifier.height(10.dp))

                            // Font Color Selector inside the same Font Card
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "انتخاب رنگ فونت",
                                    tint = fontCardBorder,
                                    modifier = Modifier.size(scaledDp(18f))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "رنگ فونت‌های برنامه:",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = fontTitleColor
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(${fontColorOption.label})",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = fontSubColor
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AppFontColorOption.values().forEach { option ->
                                    val isColorSelected = fontColorOption == option
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isColorSelected) option.swatchColor.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(
                                            if (isColorSelected) 1.8.dp else 1.dp,
                                            if (isColorSelected) option.swatchColor else fontCardBorder.copy(alpha = 0.35f)
                                        ),
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .testTag("font_color_${option.key}")
                                            .clickable { onFontColorChange(option) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(
                                                horizontal = scaledDp(8f),
                                                vertical = scaledDp(6f)
                                            ),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(scaledDp(20f))
                                                    .clip(CircleShape)
                                                    .background(option.swatchColor)
                                                    .border(1.dp, Color.White, CircleShape)
                                            ) {
                                                if (isColorSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "انتخاب شده",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(scaledDp(13f))
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = option.label,
                                                fontSize = 11.sp,
                                                fontWeight = if (isColorSelected) FontWeight.Black else FontWeight.Bold,
                                                color = if (isDark) option.darkTextColor else option.lightTextColor
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Live Adaptive Preview Box (responds immediately to both fontScale and fontColorOption!)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.2.dp, fontCardBorder.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("font_live_preview_box")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = scaledDp(12f),
                                            vertical = scaledDp(10f)
                                        ),
                                    verticalArrangement = Arrangement.spacedBy(scaledDp(6f))
                                ) {
                                    Text(
                                        text = "پیش‌نمایش کادر و فونت:",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = appSecondaryTextColor(MaterialTheme.colorScheme.onSurfaceVariant),
                                        softWrap = true
                                    )
                                    Text(
                                        text = enDigits("1,250,000 تومان • نمونه متن و اعداد 24"),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = appTextColor(MaterialTheme.colorScheme.onSurface),
                                        softWrap = true
                                    )
                                }
                            }
                        }
                    }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 2: پشتیبان‌گیری، بازیابی و مدیریت داده‌ها
                Text(
                    text = "مدیریت داده‌ها و پشتیبان‌گیری:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 1. پشتیبان‌گیری کامل از اطلاعات (بکاپ)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            onBackupData()
                            onDismissRequest()
                        },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2FF)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF4F46E5))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFE0E7FF),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = "پشتیبان‌گیری",
                                    tint = Color(0xFF4338CA),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "پشتیبان‌گیری کامل از اطلاعات (بکاپ)",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1E1B4B)
                            )
                            Text(
                                text = "ذخیره تمامی هزینه‌ها، تعهدات سالانه، پوشه‌ها و ساختار در فایل بکاپ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3730A3)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 2. بازیابی اطلاعات (بازگردانی بکاپ)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            onRestoreData()
                            onDismissRequest()
                        },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F3FF)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF7C3AED))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEDE9FE),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = "بازیابی اطلاعات",
                                    tint = Color(0xFF7C3AED),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "بازیابی اطلاعات (بازگردانی بکاپ)",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF4C1D95)
                            )
                            Text(
                                text = "انتخاب فایل پشتیبان قبلی و بازگردانی تمامی داده‌ها و ساختار به برنامه",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6D28D9)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 3. ریست کردن و پاکسازی برنامه (قرار گرفته در زیر کادر بازیابی اطلاعات طبق درخواست کاربر)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showConfirmResetDialog = true },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFE11D48))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFEE2E2),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteForever,
                                        contentDescription = null,
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "ریست کردن و پاکسازی برنامه",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF991B1B)
                                )
                                Text(
                                    text = "حذف تمامی هزینه‌ها و شروع مجدد از صفر",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF7F1D1D)
                                )
                            }
                        }

                        Text(
                            text = "ریست",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFDC2626)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 2: به‌روزرسانی خودکار برنامه از گیتهاب
                Text(
                    text = "به‌روزرسانی برنامه از گیتهاب:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                val updateCardBg = if (isDark) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFECFDF5)
                val updateBorder = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
                val updateTitleColor = if (isDark) Color(0xFF6EE7B7) else Color(0xFF065F46)
                val updateSubtitleColor = if (isDark) Color(0xFFA7F3D0) else Color(0xFF047857)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = updateCardBg),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, updateBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = updateBorder.copy(alpha = 0.18f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.SystemUpdate,
                                        contentDescription = "به‌روزرسانی برنامه",
                                        tint = updateBorder,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "بررسی و دانلود خودکار آپدیت",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = updateTitleColor
                                )
                                Text(
                                    text = enDigits("نسخه فعلی: V2 (2.0) • اتصال مستقیم به گیتهاب"),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = updateSubtitleColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = repoInput,
                            onValueChange = { newValue ->
                                repoInput = newValue
                                GitHubUpdateHelper.saveRepoInput(context, newValue)
                            },
                            label = { Text("آدرس مخزن گیتهاب", fontSize = 11.5.sp) },
                            placeholder = { Text(GitHubUpdateHelper.DEFAULT_GITHUB_REPO_URL, fontSize = 11.5.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("github_repo_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "بررسی و دانلود خودکار هنگام ورود به تنظیمات",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = updateTitleColor,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = autoCheckAndDownload,
                                onCheckedChange = { checked ->
                                    autoCheckAndDownload = checked
                                    GitHubUpdateHelper.setAutoCheckAndDownloadEnabled(context, checked)
                                },
                                colors = SwitchDefaults.colors(checkedTrackColor = updateBorder),
                                modifier = Modifier.testTag("github_auto_update_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                val targetRepo = repoInput.trim().ifBlank {
                                    GitHubUpdateHelper.DEFAULT_GITHUB_REPO_URL.also {
                                        repoInput = it
                                        GitHubUpdateHelper.saveRepoInput(context, it)
                                    }
                                }
                                isCheckingUpdate = true
                                scope.launch {
                                    val result = GitHubUpdateHelper.checkForUpdate(targetRepo)
                                    updateResult = result
                                    isCheckingUpdate = false
                                    if (result.isUpdateAvailable && autoCheckAndDownload && result.downloadUrl.isNotBlank()) {
                                        GitHubUpdateHelper.openDownloadUrl(context, result.downloadUrl)
                                        Toast.makeText(
                                            context,
                                            "نسخه جدید (${result.latestVersion}) یافت شد و دانلود آغاز گردید",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            },
                            enabled = !isCheckingUpdate,
                            colors = ButtonDefaults.buttonColors(containerColor = updateBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("github_check_update_button")
                        ) {
                            if (isCheckingUpdate) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("در حال بررسی گیتهاب...", color = Color.White, fontSize = 12.5.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("بررسی آپدیت از گیتهاب", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        updateResult?.let { result ->
                            Spacer(modifier = Modifier.height(10.dp))
                            val statusBg = when {
                                !result.isSuccess -> if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.5f) else Color(0xFFFEF2F2)
                                result.isUpdateAvailable -> if (isDark) Color(0xFF065F46).copy(alpha = 0.7f) else Color(0xFFD1FAE5)
                                else -> if (isDark) Color(0xFF1E293B) else Color.White
                            }
                            val statusText = when {
                                !result.isSuccess -> if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
                                else -> updateTitleColor
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = statusBg,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = result.message,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = statusText
                                    )
                                    if (result.releaseNotes.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = result.releaseNotes,
                                            fontSize = 11.sp,
                                            color = statusText.copy(alpha = 0.85f),
                                            maxLines = 4
                                        )
                                    }
                                    if (result.isUpdateAvailable && result.downloadUrl.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                val opened = GitHubUpdateHelper.openDownloadUrl(context, result.downloadUrl)
                                                if (!opened) {
                                                    Toast.makeText(
                                                        context,
                                                        "امکان باز کردن لینک دانلود وجود ندارد",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 48.dp)
                                                .testTag("github_download_update_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "دانلود نسخه جدید (${result.latestVersion})",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 3: درباره برنامه (با نسخه V1، نام سازنده محمد عظیمی در زیر عنوان، و تاریخ ساخت 1405/07/10)
                Text(
                    text = "درباره برنامه:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF0284C7)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(15.dp)
                    ) {
                        // سربرگ: لوگو + عنوان برنامه + نشان نسخه
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFE0F2FE),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBAE6FD)),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "درباره برنامه",
                                            tint = Color(0xFF0284C7),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "مدیریت مالی و هزینه‌ها",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF0C4A6E)
                                    )
                                    Text(
                                        text = "سیستم هوشمند حسابداری و تقویم شمسی",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0369A1)
                                    )
                                }
                            }

                            // Version Badge: V2
                            Surface(
                                shape = RoundedCornerShape(9.dp),
                                color = Color(0xFF0284C7),
                                modifier = Modifier.padding(start = 6.dp)
                            ) {
                                Text(
                                    text = enDigits("ورژن V2"),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color(0xFFBAE6FD).copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // بخش ۱: نام سازنده برنامه
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFE0F2FE).copy(alpha = 0.55f),
                            border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF0284C7).copy(alpha = 0.14f),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = Color(0xFF0284C7),
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "نام سازنده برنامه:",
                                        fontSize = 11.5.sp,
                                        color = appTextColor(Color(0xFF0C4A6E)),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "محمد عظیمی",
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = appTextColor(Color(0xFF0369A1))
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // بخش ۲: تاریخ ساخت برنامه
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFE0F2FE).copy(alpha = 0.55f),
                            border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF0284C7).copy(alpha = 0.14f),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            tint = Color(0xFF0284C7),
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "تاریخ ساخت برنامه:",
                                        fontSize = 11.5.sp,
                                        color = appTextColor(Color(0xFF0C4A6E)),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = enDigits("1405/07/10 (10 مهر 1405)"),
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = appTextColor(Color(0xFF0369A1))
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(12.dp))

                // Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = enDigits("اپلیکیشن مدیریت هزینه • نسخه V2 • سازنده: محمد عظیمی"),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = appSecondaryTextColor(MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        }
        }
    }

    // Double Confirmation Dialog for Reset
    if (showConfirmResetDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmResetDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFDC2626)
                )
            },
            title = {
                ScaledDialogContent(explicitScale = fontScale, explicitColorOption = fontColorOption) {
                    Text(
                        text = "آیا از پاکسازی تمام اطلاعات مطمئن هستید؟",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                ScaledDialogContent(explicitScale = fontScale, explicitColorOption = fontColorOption) {
                    Text(
                        text = enDigits("با تأیید این عملیات، تمامی $totalExpensesCount هزینه ثبت‌شده به طور کامل حذف خواهند شد و برنامه از ابتدا آغاز می‌شود. این عملیات غیرقابل بازگشت است مگر اینکه قبلاً فایل پشتیبان تهیه کرده باشید."),
                        fontSize = 12.5.sp,
                        lineHeight = 20.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmResetDialog = false
                        onResetData()
                        onDismissRequest()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("بله، پاکسازی کامل شود")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmResetDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }
}
