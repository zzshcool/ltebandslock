package com.ltebandslock.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.ltebandslock.data.model.AppLanguage
import com.ltebandslock.data.model.AppThemeMode

data class CustomAppColors(
    val isDark: Boolean,
    val appBg: Color,
    val cardBg: Color,
    val cardBgSubtle: Color,
    val cardBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val primaryAccent: Color = NintendoRed,
    val secondaryAccent: Color = JoyConBlue
)

val DarkCustomColors = CustomAppColors(
    isDark = true,
    appBg = AppBgDark,
    cardBg = CardBgDark,
    cardBgSubtle = CardBgSubtle,
    cardBorder = CardBorderDark,
    textPrimary = Slate200,
    textSecondary = Slate400
)

val LightCustomColors = CustomAppColors(
    isDark = false,
    appBg = Color(0xFFF1F5F9),
    cardBg = Color(0xFFFFFFFF),
    cardBgSubtle = Color(0xFFF8FAFC),
    cardBorder = Color(0xFFE2E8F0),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF64748B)
)

val LocalCustomColors = compositionLocalOf { DarkCustomColors }

private val DarkColorScheme = darkColorScheme(
    primary = NintendoRed,
    secondary = JoyConBlue,
    tertiary = JoyConRed,
    background = AppBgDark,
    surface = CardBgDark,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Slate200,
    onSurface = Slate200,
    surfaceVariant = CardBgSubtle
)

private val LightColorScheme = lightColorScheme(
    primary = NintendoRed,
    secondary = JoyConBlue,
    tertiary = JoyConRed,
    background = Color(0xFFF1F5F9),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF8FAFC)
)

@Composable
fun LTEBandsLockTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    language: AppLanguage = AppLanguage.TRADITIONAL_CHINESE,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    val customColors = if (isDark) DarkCustomColors else LightCustomColors
    val appStrings = getAppStrings(language)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    CompositionLocalProvider(
        LocalCustomColors provides customColors,
        LocalAppStrings provides appStrings
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
