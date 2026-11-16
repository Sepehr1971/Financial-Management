package com.example.utils

import android.content.Context
import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

/**
 * Available user-selectable font color palettes with high-contrast Light & Dark mode variants.
 */
enum class AppFontColorOption(
    val key: String,
    val label: String,
    val swatchColor: Color,
    val lightTextColor: Color,
    val darkTextColor: Color,
    val lightSecondaryColor: Color,
    val darkSecondaryColor: Color
) {
    DEFAULT(
        key = "default",
        label = "پیش‌فرض",
        swatchColor = Color(0xFF1E293B),
        lightTextColor = Color(0xFF0F172A),
        darkTextColor = Color(0xFFF8FAFC),
        lightSecondaryColor = Color(0xFF334155),
        darkSecondaryColor = Color(0xFFCBD5E1)
    ),
    PURE_BLACK(
        key = "black",
        label = "مشکی",
        swatchColor = Color(0xFF000000),
        lightTextColor = Color(0xFF000000),
        darkTextColor = Color(0xFFFFFFFF),
        lightSecondaryColor = Color(0xFF1E293B),
        darkSecondaryColor = Color(0xFFE2E8F0)
    ),
    NAVY(
        key = "navy",
        label = "سرمه‌ای",
        swatchColor = Color(0xFF1E3A8A),
        lightTextColor = Color(0xFF1E3A8A),
        darkTextColor = Color(0xFF93C5FD),
        lightSecondaryColor = Color(0xFF1D4ED8),
        darkSecondaryColor = Color(0xFFBFDBFE)
    ),
    EMERALD(
        key = "emerald",
        label = "سبز",
        swatchColor = Color(0xFF065F46),
        lightTextColor = Color(0xFF065F46),
        darkTextColor = Color(0xFF6EE7B7),
        lightSecondaryColor = Color(0xFF047857),
        darkSecondaryColor = Color(0xFFA7F3D0)
    ),
    PURPLE(
        key = "purple",
        label = "بنفش",
        swatchColor = Color(0xFF5B21B6),
        lightTextColor = Color(0xFF5B21B6),
        darkTextColor = Color(0xFFD8B4FE),
        lightSecondaryColor = Color(0xFF6D28D9),
        darkSecondaryColor = Color(0xFFE9D5FF)
    ),
    CRIMSON(
        key = "crimson",
        label = "زرشکی",
        swatchColor = Color(0xFF9F1239),
        lightTextColor = Color(0xFF9F1239),
        darkTextColor = Color(0xFFFDA4AF),
        lightSecondaryColor = Color(0xFFBE123C),
        darkSecondaryColor = Color(0xFFFECDD3)
    ),
    BROWN(
        key = "brown",
        label = "قهوه‌ای",
        swatchColor = Color(0xFF78350F),
        lightTextColor = Color(0xFF78350F),
        darkTextColor = Color(0xFFFDE68A),
        lightSecondaryColor = Color(0xFF92400E),
        darkSecondaryColor = Color(0xFFFEF3C7)
    ),
    TEAL(
        key = "teal",
        label = "فیروزه‌ای",
        swatchColor = Color(0xFF115E59),
        lightTextColor = Color(0xFF115E59),
        darkTextColor = Color(0xFF5EEAD4),
        lightSecondaryColor = Color(0xFF0F766E),
        darkSecondaryColor = Color(0xFF99F6E4)
    );

    companion object {
        fun fromKey(key: String?): AppFontColorOption {
            return values().firstOrNull { it.key == key } ?: DEFAULT
        }
    }
}

/**
 * Manages user-configurable UI font scaling and font color across the entire app.
 */
object FontScaleHelper {
    private const val PREFS_NAME = "app_display_prefs"
    private const val KEY_FONT_SCALE = "ui_font_scale"
    private const val KEY_FONT_COLOR = "ui_font_color_key"

    const val MIN_SCALE = 0.70f
    const val DEFAULT_SCALE = 1.00f
    const val MAX_SCALE = 1.70f
    const val STEP = 0.05f

    fun clampAndRound(rawScale: Float): Float {
        val clamped = rawScale.coerceIn(MIN_SCALE, MAX_SCALE)
        return (clamped * 20f).roundToInt() / 20f
    }

    fun getSavedFontScale(context: Context): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getFloat(KEY_FONT_SCALE, DEFAULT_SCALE)
        return clampAndRound(stored)
    }

    fun saveFontScale(context: Context, scale: Float): Float {
        val normalized = clampAndRound(scale)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat(KEY_FONT_SCALE, normalized).apply()
        return normalized
    }

    fun getSavedFontColor(context: Context): AppFontColorOption {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return AppFontColorOption.fromKey(prefs.getString(KEY_FONT_COLOR, AppFontColorOption.DEFAULT.key))
    }

    fun saveFontColor(context: Context, option: AppFontColorOption): AppFontColorOption {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_FONT_COLOR, option.key).apply()
        return option
    }

    fun getScaleLabel(scale: Float): String {
        val pct = (scale * 100f).roundToInt()
        val descriptor = when {
            pct <= 82 -> "خیلی کوچک"
            pct <= 94 -> "کوچک"
            pct <= 106 -> "متوسط (پیش‌فرض)"
            pct <= 125 -> "متوسط به بالا"
            pct <= 145 -> "بزرگ"
            else -> "خیلی بزرگ"
        }
        return "$pct% • $descriptor"
    }
}
