package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MarketRateItem
import com.example.data.model.MarketRatesState
import com.example.utils.PersianDateHelper

@Composable
fun MarketRatesCard(
    ratesState: MarketRatesState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, Color(0xFFF59E0B)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Title, Live Indicator, and Refresh Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFEF3C7),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CurrencyExchange,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "قیمت روز ارز و سکه (TGJU)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val displayUpdateTime = if (ratesState.lastUpdateTime.isNotBlank()) {
                            com.example.data.network.MarketRatesService.formatToShamsiDateTime(ratesState.lastUpdateTime)
                        } else {
                            ""
                        }
                        Text(
                            text = if (displayUpdateTime.isNotBlank()) "به‌روزرسانی: $displayUpdateTime" else "نرخ لحظه‌ای بازار",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(32.dp)
                ) {
                    if (ratesState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "به‌روزرسانی قیمت‌ها",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 1: Dollar (Fresh Emerald Mint) and Euro (Royal Azure Blue)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Dollar ($) - زمردی خنک و پرانرژی
                MarketRateColumnItem(
                    item = ratesState.dollar,
                    symbol = "$",
                    badgeColor = Color(0xFF047857),
                    gradientColors = listOf(Color(0xFFE8FAF2), Color(0xFFD1F4E6)),
                    borderColor = Color(0xFF6EE7B7),
                    titleColor = Color(0xFF065F46),
                    modifier = Modifier.weight(1f)
                )

                // Euro (€) - آبی لاجوردی و سلطنتی
                MarketRateColumnItem(
                    item = ratesState.euro,
                    symbol = "€",
                    badgeColor = Color(0xFF1D4ED8),
                    gradientColors = listOf(Color(0xFFEEF5FF), Color(0xFFDBEAFE)),
                    borderColor = Color(0xFF93C5FD),
                    titleColor = Color(0xFF1E40AF),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(6.5.dp))

            // Row 2: Sekeh Emami (Sunset Amber Gold) and Rob Sekeh (Luminous Coral Rose)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Sekeh Emami - کهربایی و عسلی شاهانه (نماد حذف شد تا عنوان کامل دیده شود)
                MarketRateColumnItem(
                    item = ratesState.sekehEmami,
                    symbol = "",
                    badgeColor = Color(0xFFD97706),
                    gradientColors = listOf(Color(0xFFFFFBEB), Color(0xFFFDE68A)),
                    borderColor = Color(0xFFFBBF24),
                    titleColor = Color(0xFF854D0E),
                    modifier = Modifier.weight(1f)
                )

                // Rob Sekeh - رزگلد و مرجانی فاخر (نماد حذف شد تا عنوان کامل دیده شود)
                MarketRateColumnItem(
                    item = ratesState.robSekeh,
                    symbol = "",
                    badgeColor = Color(0xFFE11D48),
                    gradientColors = listOf(Color(0xFFFFF1F2), Color(0xFFFFE4E6)),
                    borderColor = Color(0xFFFDA4AF),
                    titleColor = Color(0xFF9F1239),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(6.5.dp))

            // Row 3: Gold Ounce (Amethyst Royal Violet) and 18k Gold (Radiant Sunlight Gold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Global Gold Ounce - بنفش آمتیست لوکس (نماد حذف شد تا عنوان کامل دیده شود)
                MarketRateColumnItem(
                    item = ratesState.goldOunce,
                    symbol = "",
                    badgeColor = Color(0xFF7C3AED),
                    gradientColors = listOf(Color(0xFFF5F3FF), Color(0xFFEDE9FE)),
                    borderColor = Color(0xFFC4B5FD),
                    titleColor = Color(0xFF5B21B6),
                    modifier = Modifier.weight(1f)
                )

                // 18k Gold - طلایی درخشان (نماد حذف شد تا عنوان کامل دیده شود)
                MarketRateColumnItem(
                    item = ratesState.gold18k,
                    symbol = "",
                    badgeColor = Color(0xFFCA8A04),
                    gradientColors = listOf(Color(0xFFFEFCE8), Color(0xFFFEF08A)),
                    borderColor = Color(0xFFFACC15),
                    titleColor = Color(0xFF854D0E),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(6.5.dp))

            // Row 4: Bitcoin (Electric Crypto Orange) and Crude Oil (Deep Petrol Teal)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Bitcoin - نارنجی کریپتو شاد (نماد حذف شد تا عنوان کامل دیده شود)
                MarketRateColumnItem(
                    item = ratesState.bitcoin,
                    symbol = "",
                    badgeColor = Color(0xFFEA580C),
                    gradientColors = listOf(Color(0xFFFFF7ED), Color(0xFFFFEDD5)),
                    borderColor = Color(0xFFFDBA74),
                    titleColor = Color(0xFF9A3412),
                    modifier = Modifier.weight(1f)
                )

                // Crude Oil - سبز تیره نفتی و فیروزه‌ای براق (نماد بشکه حذف شد تا عنوان کامل "نفت برنت" به راحتی دیده شود)
                MarketRateColumnItem(
                    item = ratesState.crudeOil,
                    symbol = "",
                    badgeColor = Color(0xFF0F766E),
                    gradientColors = listOf(Color(0xFFF0FDFA), Color(0xFFCCFBF1)),
                    borderColor = Color(0xFF5EEAD4),
                    titleColor = Color(0xFF115E59),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(7.dp))

            // Source reference footer
            Text(
                text = "منبع: شبکه اطلاع‌رسانی طلا و ارز (tgju.org) • مقادیر تغییر بر حسب تومان و دلار است",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun MarketRateColumnItem(
    item: MarketRateItem,
    symbol: String,
    badgeColor: Color,
    gradientColors: List<Color>,
    borderColor: Color,
    titleColor: Color,
    modifier: Modifier = Modifier
) {
    val isGreen = item.isPositiveChange
    val changeColor = if (isGreen) Color(0xFF047857) else Color(0xFFB91C1C)
    val changeBg = if (isGreen) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)
    val changeBorder = if (isGreen) Color(0xFF10B981) else Color(0xFFEF4444)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .background(Brush.linearGradient(gradientColors))
            .border(BorderStroke(1.2.dp, borderColor), RoundedCornerShape(13.dp))
            .padding(horizontal = 9.dp, vertical = 7.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Line 1: Title (Gets full width of card so it is NEVER truncated!)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (symbol.isNotBlank()) {
                    Text(
                        text = symbol,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                    Spacer(modifier = Modifier.width(3.5.dp))
                }
                Text(
                    text = item.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = titleColor,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Spacer(modifier = Modifier.height(3.5.dp))

            // Line 2: Price (Full width, bold, large and clear, never truncated)
            val priceText = item.displayPriceFormatted
                ?: if (item.unitLabel == "دلار") {
                    "$" + PersianDateHelper.formatCurrency(item.priceTomans, includeUnit = false)
                } else {
                    PersianDateHelper.formatCurrency(item.priceTomans, includeUnit = true)
                }

            Text(
                text = priceText,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF0F172A),
                maxLines = 1,
                softWrap = false
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Line 3: Change Pill (Enlarged, bold, high-contrast; sign placed to the left of the number: e.g. ۱۰,۰۰۰- تومان)
            val sign = if (isGreen) "+" else "-"
            val changeDisplayText = if (item.unitLabel == "دلار") {
                val rawDiffStr = item.displayDiffFormatted?.replace("$", "")?.replace("+", "")?.replace("-", "")?.trim()
                val diffNumStr = if (!rawDiffStr.isNullOrBlank()) {
                    rawDiffStr
                } else {
                    val diff = if (item.changeTomans > 0) item.changeTomans else (item.priceTomans * (item.changePercent / 100.0)).toLong().coerceAtLeast(1L)
                    PersianDateHelper.formatCurrency(diff, includeUnit = false)
                }
                "$$diffNumStr$sign"
            } else {
                val diff = if (item.changeTomans > 0) {
                    item.changeTomans
                } else {
                    (item.priceTomans * (item.changePercent / 100.0)).toLong().coerceAtLeast(100L)
                }
                val numStr = PersianDateHelper.formatCurrency(diff, includeUnit = false)
                "$numStr$sign\u00A0تومان"
            }

            Surface(
                shape = RoundedCornerShape(7.dp),
                color = changeBg,
                border = BorderStroke(1.dp, changeBorder)
            ) {
                Text(
                    text = changeDisplayText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = changeColor,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                )
            }
        }
    }
}

