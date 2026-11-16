package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MonthSummary
import com.example.ui.theme.LalezarFontFamily
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.VazirmatnFontFamily
import com.example.ui.viewmodel.LiveClockState
import com.example.utils.CalendarDayCell
import com.example.utils.IranianCalendarHelper
import com.example.utils.PersianDateHelper

/**
 * 1. کادر تقویم روز (طراحی شاد و رنگارنگ با هدر نیلی-آسمانی، باکس‌های رنگی شفاف و تقویم ماهانه باد صبا)
 */
@Composable
fun PersianDailyCalendarCard(
    clockState: LiveClockState,
    modifier: Modifier = Modifier
) {
    var monthOffset by remember { mutableIntStateOf(0) }
    var selectedCell by remember { mutableStateOf<CalendarDayCell?>(null) }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val uiScale = com.example.ui.theme.LocalAppFontScale.current
    val daySquircleSize = (46f * (1f + (uiScale - 1f) * 0.65f)).coerceAtLeast(36f).dp
    val calendarCellHeight = (55f * (1f + (uiScale - 1f) * 0.72f)).coerceAtLeast(44f).dp
    val calendarCellPad = (5f * (1f + (uiScale - 1f) * 0.45f)).coerceIn(3f, 9f).dp
    val stripPillHPad = (6f * (1f + (uiScale - 1f) * 0.4f)).coerceIn(4f, 10f).dp

    val (displayYear, displayMonth) = remember(clockState.shamsiDate.year, clockState.shamsiDate.month, monthOffset) {
        val totalMonths = clockState.shamsiDate.year * 12 + (clockState.shamsiDate.month - 1) + monthOffset
        val y = totalMonths.floorDiv(12)
        val m = totalMonths.mod(12) + 1
        y to m
    }

    val calendarData = remember(displayYear, displayMonth, clockState.shamsiDate) {
        IranianCalendarHelper.getMonthlyCalendar(displayYear, displayMonth, clockState.shamsiDate)
    }

    val cardShellGradient = if (isDark) {
        listOf(Color(0xFF0F172A), Color(0xFF1E293B))
    } else {
        listOf(Color(0xFFF8FAFF), Color(0xFFFFFFFF), Color(0xFFF0F9FF))
    }

    val cardBorderBrush = Brush.linearGradient(
        listOf(
            Color(0xFF6366F1), // Vivid Indigo
            Color(0xFF0EA5E9), // Sky Blue
            Color(0xFF10B981)  // Emerald Mint
        )
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_calendar_card"),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.5.dp, cardBorderBrush),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(cardShellGradient))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Hero Section: Luminous Royal Indigo -> Sky Cyan Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            colors = if (isDark) {
                                listOf(
                                    Color(0xFF312E81),
                                    Color(0xFF3730A3),
                                    Color(0xFF0369A1)
                                )
                            } else {
                                listOf(
                                    Color(0xFF4F46E5), // Royal Indigo
                                    Color(0xFF6366F1), // Lively Violet-Indigo
                                    Color(0xFF0EA5E9)  // Sunny Sky Blue
                                )
                            }
                        )
                    )
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFDE047).copy(alpha = 0.28f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.08f, size.height * 0.12f),
                                radius = size.width * 0.45f
                            ),
                            radius = size.width * 0.45f,
                            center = Offset(size.width * 0.08f, size.height * 0.12f)
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF38BDF8).copy(alpha = 0.30f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.92f, size.height * 0.85f),
                                radius = size.width * 0.45f
                            ),
                            radius = size.width * 0.45f,
                            center = Offset(size.width * 0.92f, size.height * 0.85f)
                        )
                    }
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Hero Row: Golden Day Squircle + Weekday/Month + Live Clock Capsule
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
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFFEF08A),
                                border = BorderStroke(1.5.dp, Color.White),
                                shadowElevation = 3.dp,
                                modifier = Modifier.size(daySquircleSize)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = PersianDateHelper.toPersianDigits(clockState.shamsiDate.day),
                                        style = TextStyle(
                                            fontFamily = LalezarFontFamily,
                                            fontSize = 23.sp,
                                            color = Color(0xFF312E81),
                                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                                        ),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = clockState.shamsiDate.dayOfWeek,
                                    style = TextStyle(
                                        fontFamily = LalezarFontFamily,
                                        fontSize = 18.sp,
                                        color = Color.White,
                                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                                    ),
                                    softWrap = true
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${clockState.shamsiDate.monthName} ${PersianDateHelper.toPersianDigits(clockState.shamsiDate.year)}",
                                    fontFamily = VazirmatnFontFamily,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    softWrap = true
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Live Clock Capsule
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF1E1B4B).copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4ADE80))
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = Color(0xFFFDE047),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                AnimatedContent(
                                    targetState = clockState.timeString,
                                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                                    label = "live_clock_time"
                                ) { time ->
                                    Text(
                                        text = PersianDateHelper.toPersianDigits(time),
                                        style = TextStyle(
                                            fontFamily = LalezarFontFamily,
                                            fontSize = 15.sp,
                                            color = Color.White,
                                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                                        ),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Jewel-toned Frosted Strip for Gregorian, Shamsi & Hijri Dates
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF1E1B4B).copy(alpha = 0.30f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 9.dp, vertical = 8.dp)
                        ) {
                            // Row 2: 2026/11/15   15 نوامبر   1405/08/25
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(7.dp),
                                        color = Color(0xFF0284C7).copy(alpha = 0.72f)
                                    ) {
                                        Text(
                                            text = clockState.gregorianNumeric,
                                            fontFamily = OutfitFontFamily,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = stripPillHPad, vertical = 3.dp)
                                        )
                                    }

                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                        Text(
                                            text = clockState.gregorianPersianMonthDay,
                                            fontFamily = VazirmatnFontFamily,
                                            color = Color.White,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Black,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(7.dp),
                                        color = Color(0xFF059669).copy(alpha = 0.85f)
                                    ) {
                                        Text(
                                            text = clockState.shamsiDate.format(),
                                            fontFamily = VazirmatnFontFamily,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = stripPillHPad, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            HorizontalDivider(color = Color.White.copy(alpha = 0.18f))
                            Spacer(modifier = Modifier.height(6.dp))

                            // Row 3: Hijri date in the exact same clean layout
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("hijri_date_row"),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(7.dp),
                                        color = Color(0xFFD97706).copy(alpha = 0.85f)
                                    ) {
                                        Text(
                                            text = clockState.hijriDate.formatNumeric(),
                                            fontFamily = OutfitFontFamily,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = stripPillHPad, vertical = 3.dp)
                                        )
                                    }

                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                        Text(
                                            text = "${PersianDateHelper.toPersianDigits(clockState.hijriDate.day)} ${clockState.hijriDate.monthName}",
                                            fontFamily = VazirmatnFontFamily,
                                            color = Color.White,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Black,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(7.dp),
                                        color = Color(0xFF7C3AED).copy(alpha = 0.88f)
                                    ) {
                                        Text(
                                            text = "هجری قمری",
                                            fontFamily = VazirmatnFontFamily,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = stripPillHPad, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Monthly Calendar Section: Clean Bright Canvas with Vibrant Jewel Tiles
            val calendarOnSurface = if (isDark) Color.White else Color.Black

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp)
                    .testTag("monthly_calendar_container")
            ) {
                // Calendar Header: Indigo Icon + Month Title + Badesaba Pill + Circular Nav Buttons
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
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDark) Color(0xFF312E81) else Color(0xFFEEF2FF),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF6366F1) else Color(0xFFC7D2FE)),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4F46E5),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تقویم ${calendarData.monthName} ${PersianDateHelper.toPersianDigits(calendarData.shamsiYear)}",
                            style = TextStyle(
                                fontFamily = LalezarFontFamily,
                                fontSize = 16.5.sp,
                                color = calendarOnSurface,
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            ),
                            softWrap = true
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) Color(0xFF064E3B) else Color(0xFFECFDF5),
                            border = BorderStroke(0.9.dp, Color(0xFF34D399))
                        ) {
                            Text(
                                text = "باد صبا",
                                fontFamily = VazirmatnFontFamily,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFA7F3D0) else Color(0xFF047857),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (monthOffset != 0) {
                            Surface(
                                shape = RoundedCornerShape(9.dp),
                                color = Color(0xFF4F46E5),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(9.dp))
                                    .clickable {
                                        monthOffset = 0
                                        selectedCell = null
                                    }
                                    .testTag("calendar_reset_today_button")
                            ) {
                                Text(
                                    text = "امروز",
                                    fontFamily = VazirmatnFontFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(2.dp))
                        }

                        IconButton(
                            onClick = {
                                monthOffset -= 1
                                selectedCell = null
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("calendar_prev_month_button")
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isDark) Color(0xFF312E81) else Color(0xFFEEF2FF),
                                border = BorderStroke(0.8.dp, if (isDark) Color(0xFF6366F1) else Color(0xFFC7D2FE)),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                        contentDescription = "ماه قبل",
                                        tint = if (isDark) Color(0xFFC7D2FE) else Color(0xFF4338CA),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = {
                                monthOffset += 1
                                selectedCell = null
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("calendar_next_month_button")
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isDark) Color(0xFF312E81) else Color(0xFFEEF2FF),
                                border = BorderStroke(0.8.dp, if (isDark) Color(0xFF6366F1) else Color(0xFFC7D2FE)),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "ماه بعد",
                                        tint = if (isDark) Color(0xFFC7D2FE) else Color(0xFF4338CA),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Weekday Header Strip (شنبه .. جمعه)
                val weekHeaders = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFEEF2FF),
                    border = BorderStroke(0.8.dp, if (isDark) Color(0xFF334155) else Color(0xFFC7D2FE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp, horizontal = 3.dp)
                    ) {
                        weekHeaders.forEachIndexed { idx, label ->
                            val isFriHeader = idx == 6
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isFriHeader) {
                                    Surface(
                                        shape = RoundedCornerShape(7.dp),
                                        color = if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.6f) else Color(0xFFFFE4E6)
                                    ) {
                                        Text(
                                            text = label,
                                            fontFamily = VazirmatnFontFamily,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFE11D48),
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 1.dp)
                                        )
                                    }
                                } else {
                                    Text(
                                        text = label,
                                        fontFamily = VazirmatnFontFamily,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4338CA),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Monthly Day Grid (54dp cells with TopCenter/BottomCenter slots so English numbers sit cleanly inside)
                val leadingEmpty = List<CalendarDayCell?>(calendarData.firstDayOfWeekOffset) { null }
                val rawCells: List<CalendarDayCell?> = leadingEmpty + calendarData.days
                val trailingEmptyCount = (7 - (rawCells.size % 7)) % 7
                val paddedCells = rawCells + List<CalendarDayCell?>(trailingEmptyCount) { null }
                val weeks = paddedCells.chunked(7)

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    weeks.forEach { week ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            week.forEach { cell ->
                                if (cell == null) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(calendarCellHeight)
                                    )
                                } else {
                                    val isRedHoliday = cell.isOfficialHoliday || cell.isFriday
                                    val isSelected = selectedCell?.shamsiDay == cell.shamsiDay &&
                                        selectedCell?.shamsiMonth == cell.shamsiMonth &&
                                        selectedCell?.shamsiYear == cell.shamsiYear

                                    val cellBrush = when {
                                        cell.isToday && !isRedHoliday -> Brush.linearGradient(
                                            listOf(Color(0xFF047857), Color(0xFF0284C7))
                                        )
                                        cell.isToday && isRedHoliday -> Brush.linearGradient(
                                            listOf(Color(0xFFE11D48), Color(0xFFEA580C))
                                        )
                                        cell.isOfficialHoliday -> Brush.linearGradient(
                                            listOf(
                                                if (isDark) Color(0xFF881337).copy(alpha = 0.8f) else Color(0xFFFECDD3),
                                                if (isDark) Color(0xFF9F1239).copy(alpha = 0.7f) else Color(0xFFFDA4AF)
                                            )
                                        )
                                        cell.isFriday -> Brush.linearGradient(
                                            listOf(
                                                if (isDark) Color(0xFF4C0519).copy(alpha = 0.65f) else Color(0xFFFEE2E2),
                                                if (isDark) Color(0xFF881337).copy(alpha = 0.45f) else Color(0xFFFECACA)
                                            )
                                        )
                                        isSelected -> Brush.linearGradient(
                                            listOf(
                                                if (isDark) Color(0xFF312E81) else Color(0xFFC7D2FE),
                                                if (isDark) Color(0xFF3730A3) else Color(0xFFA5B4FC)
                                            )
                                        )
                                        else -> Brush.linearGradient(
                                            listOf(
                                                if (isDark) Color(0xFF334155) else Color(0xFFDCE3EE),
                                                if (isDark) Color(0xFF1E293B) else Color(0xFFCBD5E1)
                                            )
                                        )
                                    }

                                    val shamsiTextColor = when {
                                        cell.isToday -> Color.White
                                        isRedHoliday -> if (isDark) Color(0xFFFECDD3) else Color(0xFF991B1B)
                                        isSelected -> if (isDark) Color.White else Color(0xFF1E1B4B)
                                        else -> if (isDark) Color.White else Color.Black
                                    }

                                    val gregorianSubColor = when {
                                        cell.isToday -> Color(0xFFFEF08A)
                                        isRedHoliday -> if (isDark) Color(0xFFFDA4AF) else Color(0xFFB91C1C)
                                        isSelected -> if (isDark) Color(0xFFE0E7FF) else Color(0xFF1E1B4B)
                                        else -> if (isDark) Color(0xFFE2E8F0) else Color.Black
                                    }

                                    val borderStroke = when {
                                        cell.isToday && isRedHoliday -> BorderStroke(2.dp, Color(0xFF34D399))
                                        cell.isToday -> BorderStroke(1.8.dp, Color(0xFFFEF08A))
                                        cell.isOfficialHoliday -> BorderStroke(1.3.dp, Color(0xFFE11D48))
                                        cell.isFriday -> BorderStroke(1.dp, if (isDark) Color(0xFF9F1239) else Color(0xFFF87171))
                                        isSelected -> BorderStroke(1.5.dp, Color(0xFF4338CA))
                                        else -> BorderStroke(1.dp, if (isDark) Color(0xFF475569) else Color(0xFF94A3B8))
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(calendarCellHeight)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(cellBrush)
                                            .border(borderStroke, RoundedCornerShape(12.dp))
                                            .clickable {
                                                selectedCell = if (isSelected) null else cell
                                            }
                                    ) {
                                        Text(
                                            text = PersianDateHelper.toPersianDigits(cell.shamsiDay),
                                            style = TextStyle(
                                                fontFamily = VazirmatnFontFamily,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Black,
                                                lineHeight = 18.sp,
                                                color = shamsiTextColor,
                                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                                            ),
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier
                                                .align(Alignment.TopCenter)
                                                .padding(top = calendarCellPad)
                                        )
                                        Text(
                                            text = cell.gregorianDay.toString(),
                                            style = TextStyle(
                                                fontFamily = OutfitFontFamily,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Black,
                                                lineHeight = 14.sp,
                                                color = gregorianSubColor,
                                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                                            ),
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(bottom = calendarCellPad)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Selected day detail banner when user taps any day
                selectedCell?.let { sel ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(11.dp),
                        color = if (sel.isOfficialHoliday || sel.isFriday) Color(0xFFFFF1F2) else Color(0xFFEEF2FF),
                        border = BorderStroke(
                            1.dp,
                            if (sel.isOfficialHoliday || sel.isFriday) Color(0xFFFDA4AF) else Color(0xFFA5B4FC)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val reasonPart = if (sel.holidayReasons.isNotEmpty()) {
                            " — ${sel.holidayReasons.joinToString(" / ")}"
                        } else if (sel.isFriday) {
                            " — جمعه (تعطیل پایان هفته)"
                        } else ""
                        val gregStr = "${sel.gregorianYear}/${sel.gregorianMonth.toString().padStart(2, '0')}/${sel.gregorianDay.toString().padStart(2, '0')}"
                        Text(
                            text = "${PersianDateHelper.toPersianDigits(sel.shamsiDay)} ${calendarData.monthName} • $gregStr • ${sel.hijriDate.formatReadablePersian()}$reasonPart",
                            fontFamily = VazirmatnFontFamily,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (sel.isOfficialHoliday || sel.isFriday) Color(0xFFBE123C) else Color(0xFF3730A3),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            softWrap = true
                        )
                    }
                }

                // Official Holidays list directly below the calendar (without header sentence)
                if (calendarData.officialHolidayEntries.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("official_holidays_reasons_section"),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        calendarData.officialHolidayEntries.forEach { entry ->
                            Surface(
                                shape = RoundedCornerShape(11.dp),
                                color = if (isDark) Color(0xFF4C0519).copy(alpha = 0.72f) else Color(0xFFFFF1F2),
                                border = BorderStroke(1.dp, if (isDark) Color(0xFFE11D48) else Color(0xFFFDA4AF)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE11D48))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${PersianDateHelper.toPersianDigits(entry.shamsiDay)} ${calendarData.monthName} (${entry.hijriLabel}): ${entry.reason}",
                                        fontFamily = VazirmatnFontFamily,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFFFDA4AF) else Color(0xFFBE123C),
                                        lineHeight = 17.sp,
                                        softWrap = true
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "در این ماه به جز جمعه‌ها، تعطیل رسمی دیگری وجود ندارد.",
                        fontFamily = VazirmatnFontFamily,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B),
                        modifier = Modifier.testTag("official_holidays_reasons_section")
                    )
                }
            }
        }
    }
}

/**
 * 3. کادر هزینه کل ماه
 * کادر مستقل با نمایش کامل جمع کل هزینه ماه جاری، هزینه‌های جاری، و سهم هزینه‌های سالانه؛
 * هیچ متنی به دلیل کمبود جا به صورت اختصاری نوشته نمی‌شود و چیدمان ستونی بدون تداخل متن و عدد است.
 */
@Composable
fun MonthlyExpenseOverviewCard(
    clockState: LiveClockState,
    currentMonthSummary: MonthSummary?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_expense_overview_card"),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.5.dp, Color(0xFF10B981)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = com.example.ui.theme.scaledDp(14f),
                    vertical = com.example.ui.theme.scaledDp(12f)
                )
        ) {
            // Header + Total Amount stacked vertically so text and numbers NEVER overlap or clip
            val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "هزینه‌های ${clockState.shamsiDate.monthName} ماه با احتساب هزینه‌های ثابت سالانه:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = com.example.ui.theme.appTextColor(MaterialTheme.colorScheme.onSurface),
                    lineHeight = 20.sp,
                    softWrap = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // High-visibility Amount Badge cleanly underneath the title
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) Color(0xFF064E3B) else Color(0xFFECFDF5),
                    border = BorderStroke(1.2.dp, Color(0xFF10B981)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = com.example.ui.theme.enDigits(
                            PersianDateHelper.formatCurrency(currentMonthSummary?.totalExpense ?: 0L)
                        ),
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Black,
                        color = com.example.ui.theme.appTextColor(
                            if (isDark) Color(0xFFECFDF5) else Color(0xFF022C22)
                        ),
                        softWrap = true,
                        modifier = Modifier.padding(
                            horizontal = com.example.ui.theme.scaledDp(12f),
                            vertical = com.example.ui.theme.scaledDp(7f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(10.dp))

            // هزینه‌های جاری ماه (سازگار با هر دو تم روشن و تاریک)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = if (isDark) Color(0xFF0C2A44) else Color(0xFFF0F9FF),
                border = BorderStroke(1.3.dp, if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = com.example.ui.theme.scaledDp(12f),
                            vertical = com.example.ui.theme.scaledDp(9f)
                        )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                            modifier = Modifier.size(com.example.ui.theme.scaledDp(16f))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "هزینه‌های جاری ماه:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = com.example.ui.theme.appTextColor(
                                if (isDark) Color(0xFFE0F2FE) else Color(0xFF0C4A6E)
                            ),
                            softWrap = true
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = com.example.ui.theme.enDigits(
                            PersianDateHelper.formatCurrency(
                                currentMonthSummary?.directExpenseTotal ?: (currentMonthSummary?.totalExpense ?: 0L)
                            )
                        ),
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Black,
                        color = com.example.ui.theme.appTextColor(
                            if (isDark) Color.White else Color(0xFF082F49)
                        ),
                        modifier = Modifier.padding(start = 22.dp),
                        softWrap = true
                    )
                }
            }
        }
    }
}

/**
 * Backward-compatible Combined Header
 */
@Composable
fun PersianLiveClockHeader(
    clockState: LiveClockState,
    currentMonthSummary: MonthSummary?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PersianDailyCalendarCard(clockState = clockState)
        MonthlyExpenseOverviewCard(
            clockState = clockState,
            currentMonthSummary = currentMonthSummary
        )
    }
}
