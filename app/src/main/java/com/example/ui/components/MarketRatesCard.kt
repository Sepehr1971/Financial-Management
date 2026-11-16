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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.ShowChart
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MarketRateItem
import com.example.data.model.MarketRatesState
import com.example.ui.theme.LocalAppFontScale
import com.example.ui.theme.appTextColor
import com.example.ui.theme.enDigits
import com.example.ui.theme.scaledDp
import com.example.utils.PersianDateHelper

@Composable
fun MarketRatesCard(
    ratesState: MarketRatesState,
    onRefresh: () -> Unit,
    onConnectBale: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var activeChart by remember { mutableStateOf<MarketChartTarget?>(null) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, Color(0xFFF59E0B)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(scaledDp(14f))) {
            // Header: Title, Live Indicator, and Refresh Button
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
                        modifier = Modifier.size(scaledDp(34f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CurrencyExchange,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(scaledDp(18f))
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = enDigits("قیمت روز ارز، طلا و رمزارز"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = appTextColor(MaterialTheme.colorScheme.onSurface)
                        )
                        val displayUpdateTime = if (ratesState.lastUpdateTime.isNotBlank()) {
                            com.example.data.network.MarketRatesService.formatToShamsiDateTime(ratesState.lastUpdateTime)
                        } else {
                            ""
                        }
                        Text(
                            text = enDigits(if (displayUpdateTime.isNotBlank()) "به‌روزرسانی: $displayUpdateTime" else "نرخ لحظه‌ای بازار"),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = appTextColor(MaterialTheme.colorScheme.onSurface)
                        )
                    }
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(40.dp)
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

            val anyBaleMissing = ratesState.baleNeedsLogin ||
                ratesState.dollar.priceTomans <= 0L ||
                ratesState.gold18k.priceTomans <= 0L ||
                ratesState.tether.priceTomans <= 0L

            if (anyBaleMissing) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFECFDF5),
                    border = BorderStroke(1.2.dp, Color(0xFF10B981)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onConnectBale() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "اتصال به کانال‌های بله (دریافت دلار، طلا، سکه و رمزارز)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF065F46),
                            softWrap = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF059669)
                        ) {
                            Text(
                                text = "ورود و دریافت",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 1: Dollar (Fresh Emerald Mint) and Euro (Royal Azure Blue)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Dollar ($) - زمردی خنک و پرانرژی (چارت زنده از iranbroker.net)
                MarketRateColumnItem(
                    item = ratesState.dollar,
                    symbol = "$",
                    badgeColor = Color(0xFF047857),
                    gradientColors = listOf(Color(0xFFE8FAF2), Color(0xFFD1F4E6)),
                    borderColor = Color(0xFF6EE7B7),
                    titleColor = Color(0xFF065F46),
                    chartTestTag = "market_rate_dollar_chart",
                    onChartClick = {
                        activeChart = MarketChartTarget(
                            title = "نمودار لحظه‌ای دلار",
                            url = "https://iranbroker.net/symbol/usd"
                        )
                    },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )

                // Euro (€) - آبی لاجوردی و سلطنتی (چارت زنده از iranbroker.net)
                MarketRateColumnItem(
                    item = ratesState.euro,
                    symbol = "€",
                    badgeColor = Color(0xFF1D4ED8),
                    gradientColors = listOf(Color(0xFFEEF5FF), Color(0xFFDBEAFE)),
                    borderColor = Color(0xFF93C5FD),
                    titleColor = Color(0xFF1E40AF),
                    chartTestTag = "market_rate_euro_chart",
                    onChartClick = {
                        activeChart = MarketChartTarget(
                            title = "نمودار لحظه‌ای یورو",
                            url = "https://iranbroker.net/symbol/eur"
                        )
                    },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }

            Spacer(modifier = Modifier.height(5.5.dp))

            // Row 2: Sekeh Emami (Sunset Amber Gold) and Rob Sekeh (Luminous Coral Rose)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
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
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )

                // Rob Sekeh - رزگلد و مرجانی فاخر (نماد حذف شد تا عنوان کامل دیده شود)
                MarketRateColumnItem(
                    item = ratesState.robSekeh,
                    symbol = "",
                    badgeColor = Color(0xFFE11D48),
                    gradientColors = listOf(Color(0xFFFFF1F2), Color(0xFFFFE4E6)),
                    borderColor = Color(0xFFFDA4AF),
                    titleColor = Color(0xFF9F1239),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }

            Spacer(modifier = Modifier.height(5.5.dp))

            // Row 3: Gold Ounce (Amethyst Royal Violet) and 18k/16k Gold (Radiant Sunlight Gold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Global Gold Ounce - بنفش آمتیست لوکس
                MarketRateColumnItem(
                    item = ratesState.goldOunce,
                    symbol = "",
                    badgeColor = Color(0xFF7C3AED),
                    gradientColors = listOf(Color(0xFFF5F3FF), Color(0xFFEDE9FE)),
                    borderColor = Color(0xFFC4B5FD),
                    titleColor = Color(0xFF5B21B6),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )

                // 18k/16k Gold - طلایی درخشان (چارت زنده از moj3.ir)
                MarketRateColumnItem(
                    item = ratesState.gold18k,
                    symbol = "",
                    badgeColor = Color(0xFFCA8A04),
                    gradientColors = listOf(Color(0xFFFEFCE8), Color(0xFFFEF08A)),
                    borderColor = Color(0xFFFACC15),
                    titleColor = Color(0xFF854D0E),
                    chartTestTag = "market_rate_gold_18k_chart",
                    onChartClick = {
                        activeChart = MarketChartTarget(
                            title = PersianDateHelper.normalizeToEnglishNumbers(
                                "نمودار ${ratesState.gold18k.title.ifBlank { "طلای 18 عیار" }}"
                            ),
                            url = "https://moj3.ir/price/chart"
                        )
                    },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }

            Spacer(modifier = Modifier.height(5.5.dp))

            // Row 4: Bitcoin (Electric Crypto Orange) and Tether USDT (Vibrant Crypto Mint Teal)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Bitcoin - نارنجی کریپتو شاد
                MarketRateColumnItem(
                    item = ratesState.bitcoin,
                    symbol = "",
                    badgeColor = Color(0xFFEA580C),
                    gradientColors = listOf(Color(0xFFFFF7ED), Color(0xFFFFEDD5)),
                    borderColor = Color(0xFFFDBA74),
                    titleColor = Color(0xFF9A3412),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )

                // Tether USDT - فیروزه‌ای و سبز زمردی کریپتو تتر
                MarketRateColumnItem(
                    item = ratesState.tether,
                    symbol = "₮",
                    badgeColor = Color(0xFF0D9488),
                    gradientColors = listOf(Color(0xFFE6FFFA), Color(0xFFCCFBF1)),
                    borderColor = Color(0xFF2DD4BF),
                    titleColor = Color(0xFF0F766E),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }

            Spacer(modifier = Modifier.height(5.5.dp))

            // Row 5: Tehran Stock Exchange (Royal Sapphire Blue) and Crude Oil (Deep Petrol Teal)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // شاخص کل بورس تهران
                MarketRateColumnItem(
                    item = ratesState.bourse,
                    symbol = "",
                    badgeColor = Color(0xFF2563EB),
                    gradientColors = listOf(Color(0xFFEFF6FF), Color(0xFFDBEAFE)),
                    borderColor = Color(0xFF93C5FD),
                    titleColor = Color(0xFF1E40AF),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )

                // Crude Oil - نفت برنت دریای شمال
                MarketRateColumnItem(
                    item = ratesState.crudeOil,
                    symbol = "",
                    badgeColor = Color(0xFF0F766E),
                    gradientColors = listOf(Color(0xFFF0FDFA), Color(0xFFCCFBF1)),
                    borderColor = Color(0xFF5EEAD4),
                    titleColor = Color(0xFF115E59),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }
    }

    activeChart?.let { target ->
        MarketChartDialog(
            target = target,
            onDismiss = { activeChart = null }
        )
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
    modifier: Modifier = Modifier,
    chartTestTag: String? = null,
    onChartClick: (() -> Unit)? = null
) {
    val uiScale = LocalAppFontScale.current
    val isGreen = item.isPositiveChange
    val changeColor = if (isGreen) Color(0xFF065F46) else Color(0xFF991B1B)
    val changeBg = if (isGreen) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)
    val changeBorder = if (isGreen) Color(0xFF10B981) else Color(0xFFEF4444)

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = scaledDp(68f))
            .clip(RoundedCornerShape(13.dp))
            .background(Brush.linearGradient(gradientColors))
            .border(BorderStroke(1.3.dp, borderColor), RoundedCornerShape(13.dp))
            .then(
                if (onChartClick != null) {
                    Modifier
                        .then(if (chartTestTag != null) Modifier.testTag(chartTestTag) else Modifier)
                        .clickable { onChartClick() }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = scaledDp(9f), vertical = (7f * uiScale).coerceIn(5f, 12f).dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            // Line 1: Title + optional Chart indicator pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (symbol.isNotBlank()) {
                        Text(
                            text = symbol,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = badgeColor
                        )
                        Spacer(modifier = Modifier.width(3.5.dp))
                    }
                    Text(
                        text = enDigits(item.title),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = titleColor,
                        softWrap = true
                    )
                }

                if (onChartClick != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor.copy(alpha = 0.16f),
                        border = BorderStroke(0.9.dp, badgeColor.copy(alpha = 0.55f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShowChart,
                                contentDescription = "مشاهده چارت ${item.title}",
                                tint = badgeColor,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "نمودار",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Black,
                                color = badgeColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            val isPendingSource = item.priceTomans <= 0L && item.displayPriceFormatted.isNullOrBlank()
            val isBaleSource = item.sourceLabel.contains("بله")

            // Line 2: Price (Full width, large, bold, high-contrast black)
            val priceText = when {
                isPendingSource -> if (isBaleSource) "نیاز به اتصال بله" else "در حال دریافت..."
                item.displayPriceFormatted != null -> item.displayPriceFormatted.replace(" واحد", "\u00A0واحد")
                item.unitLabel == "دلار" -> "$" + PersianDateHelper.formatCurrency(item.priceTomans, includeUnit = false)
                else -> PersianDateHelper.formatCurrency(item.priceTomans, includeUnit = false) + "\u00A0تومان"
            }

            Text(
                text = enDigits(priceText),
                fontSize = if (isPendingSource) 11.5.sp else 14.sp,
                fontWeight = FontWeight.Black,
                color = if (isPendingSource) Color(0xFF334155) else Color.Black,
                softWrap = true
            )

            // Line 3: Only show pending status OR live delta pill (removed "منبع: بله" / "منبع: TGJU" to reduce card height)
            if (isPendingSource) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Text(
                        text = enDigits(item.sourceLabel.ifBlank { "در انتظار" }),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E293B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = scaledDp(7f), vertical = scaledDp(2f))
                    )
                }
            } else {
                val sign = if (isGreen) "+" else "-"
                val hasDelta = item.changeTomans > 0L || item.changePercent > 0.0 || !item.displayDiffFormatted.isNullOrBlank()
                if (hasDelta) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val changeDisplayText = when {
                        item.changeTomans == 0L && item.displayDiffFormatted.isNullOrBlank() && item.changePercent > 0.0 -> {
                            val pctStr = String.format(java.util.Locale.US, "%.2f", item.changePercent)
                            "$pctStr%$sign"
                        }
                        item.unitLabel == "دلار" -> {
                            val rawDiffStr = item.displayDiffFormatted?.replace("$", "")?.replace("+", "")?.replace("-", "")?.trim()
                            val diffNumStr = if (!rawDiffStr.isNullOrBlank()) {
                                rawDiffStr
                            } else {
                                PersianDateHelper.formatCurrency(item.changeTomans, includeUnit = false)
                            }
                            "$$diffNumStr$sign"
                        }
                        item.unitLabel == "واحد" -> {
                            val rawNum = item.displayDiffFormatted?.replace("+", "")?.replace("-", "")?.trim()
                                ?: PersianDateHelper.formatCurrency(item.changeTomans, includeUnit = false)
                            "$rawNum$sign"
                        }
                        else -> {
                            val numStr = PersianDateHelper.formatCurrency(item.changeTomans, includeUnit = false)
                            "$numStr$sign\u00A0تومان"
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(7.dp),
                        color = changeBg,
                        border = BorderStroke(1.dp, changeBorder)
                    ) {
                        Text(
                            text = enDigits(changeDisplayText),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black,
                            color = changeColor,
                            maxLines = 2,
                            softWrap = true,
                            modifier = Modifier.padding(horizontal = scaledDp(7f), vertical = scaledDp(2f))
                        )
                    }
                }
            }
        }
    }
}

