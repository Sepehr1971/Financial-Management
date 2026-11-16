package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.utils.AppFontColorOption
import com.example.utils.PersianDateHelper

val LocalAppFontScale = compositionLocalOf { 1.0f }
val LocalAppFontColorOption = compositionLocalOf { AppFontColorOption.DEFAULT }

private val NUMERIC_RUN_REGEX = Regex("""[$+-]?\d+(?:[.,:/-]\d+)*[%+-]?""")

/**
 * Converts all digits to English ASCII (0-9), converts '٪' to '%',
 * and styles numeric runs using OutfitFontFamily while leaving Persian letters in VazirmatnFontFamily.
 */
fun enDigits(text: String): AnnotatedString {
    val normalized = PersianDateHelper.normalizeToEnglishNumbers(text)
    return buildAnnotatedString {
        append(normalized)
        NUMERIC_RUN_REGEX.findAll(normalized).forEach { match ->
            addStyle(
                style = SpanStyle(fontFamily = OutfitFontFamily),
                start = match.range.first,
                end = match.range.last + 1
            )
        }
    }
}

/**
 * Proportionally scales a dp dimension alongside the user's font scale
 * so boxes, paddings, and icon containers expand and shrink harmoniously.
 */
@Composable
fun scaledDp(baseDp: Float, strength: Float = 0.55f, minFactor: Float = 0.78f, maxFactor: Float = 1.42f): Dp {
    val scale = LocalAppFontScale.current
    val factor = (1f + (scale - 1f) * strength).coerceIn(minFactor, maxFactor)
    return (baseDp * factor).dp
}

@Composable
fun scaledDp(baseDp: Dp, strength: Float = 0.55f, minFactor: Float = 0.78f, maxFactor: Float = 1.42f): Dp =
    scaledDp(baseDp.value, strength, minFactor, maxFactor)

/**
 * Returns the active primary font color override if the user picked a custom font color,
 * or falls back to [defaultColor] when using the default theme palette.
 */
@Composable
fun appTextColor(defaultColor: Color): Color {
    val customOption = LocalAppFontColorOption.current
    if (customOption == AppFontColorOption.DEFAULT) return defaultColor
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return if (isDark) customOption.darkTextColor else customOption.lightTextColor
}

/**
 * Returns the active secondary font color override if the user picked a custom font color,
 * or falls back to [defaultColor].
 */
@Composable
fun appSecondaryTextColor(defaultColor: Color): Color {
    val customOption = LocalAppFontColorOption.current
    if (customOption == AppFontColorOption.DEFAULT) return defaultColor
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return if (isDark) customOption.darkSecondaryColor else customOption.lightSecondaryColor
}

/**
 * Ensures Compose Dialog, AlertDialog, and ModalBottomSheet windows inherit
 * the user's selected font scale, font color, and RTL layout direction
 * (since Dialog/BottomSheet windows reset LocalDensity at their root ComposeView).
 */
@Composable
fun ScaledDialogContent(
    explicitScale: Float? = null,
    explicitColorOption: AppFontColorOption? = null,
    content: @Composable () -> Unit
) {
    val effectiveScale = explicitScale ?: LocalAppFontScale.current
    val effectiveColor = explicitColorOption ?: LocalAppFontColorOption.current
    val baseDensity = LocalDensity.current
    val scaledDensity = remember(baseDensity.density, baseDensity.fontScale, effectiveScale) {
        Density(
            density = baseDensity.density,
            fontScale = baseDensity.fontScale * effectiveScale
        )
    }
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalDensity provides scaledDensity,
        LocalAppFontScale provides effectiveScale,
        LocalAppFontColorOption provides effectiveColor
    ) {
        content()
    }
}

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color.Black,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = Color(0xFFECFDF5),
    secondary = GoldAccent,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF78350F),
    onSecondaryContainer = Color(0xFFFEF3C7),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = Color(0xFF2B3A52),
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = OnEmeraldContainer,
    secondary = GoldAccent,
    onSecondary = Color.Black,
    secondaryContainer = GoldContainer,
    onSecondaryContainer = OnGoldContainer,
    background = SlateBackground,
    onBackground = SlateTextPrimary,
    surface = SlateCard,
    onSurface = SlateTextPrimary,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = SlateTextSecondary,
    outline = SlateBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    fontColorOption: AppFontColorOption = AppFontColorOption.DEFAULT,
    content: @Composable () -> Unit
) {
    val baseScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val colorScheme = if (fontColorOption == AppFontColorOption.DEFAULT) {
        baseScheme
    } else {
        val primaryText = if (darkTheme) fontColorOption.darkTextColor else fontColorOption.lightTextColor
        val secondaryText = if (darkTheme) fontColorOption.darkSecondaryColor else fontColorOption.lightSecondaryColor
        baseScheme.copy(
            onBackground = primaryText,
            onSurface = primaryText,
            onSurfaceVariant = secondaryText,
            onPrimaryContainer = primaryText,
            onSecondaryContainer = primaryText
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
