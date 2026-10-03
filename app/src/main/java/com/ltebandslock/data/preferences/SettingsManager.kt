package com.ltebandslock.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.ltebandslock.data.model.AppLanguage
import com.ltebandslock.data.model.AppSettings
import com.ltebandslock.data.model.AppThemeMode
import com.ltebandslock.data.model.SpeedUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        val themeCode = prefs.getString(KEY_THEME_MODE, AppThemeMode.DARK.code) ?: AppThemeMode.DARK.code
        val langCode = prefs.getString(KEY_LANGUAGE, AppLanguage.TRADITIONAL_CHINESE.code) ?: AppLanguage.TRADITIONAL_CHINESE.code
        val unitCode = prefs.getString(KEY_SPEED_UNIT, SpeedUnit.MBPS.code) ?: SpeedUnit.MBPS.code

        val theme = AppThemeMode.entries.find { it.code == themeCode } ?: AppThemeMode.DARK
        val lang = AppLanguage.entries.find { it.code == langCode } ?: AppLanguage.TRADITIONAL_CHINESE
        val unit = SpeedUnit.entries.find { it.code == unitCode } ?: SpeedUnit.MBPS

        return AppSettings(
            themeMode = theme,
            language = lang,
            speedUnit = unit
        )
    }

    fun setThemeMode(theme: AppThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, theme.code).apply()
        _settings.value = _settings.value.copy(themeMode = theme)
    }

    fun setLanguage(lang: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, lang.code).apply()
        _settings.value = _settings.value.copy(language = lang)
    }

    fun setSpeedUnit(unit: SpeedUnit) {
        prefs.edit().putString(KEY_SPEED_UNIT, unit.code).apply()
        _settings.value = _settings.value.copy(speedUnit = unit)
    }

    companion object {
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_LANGUAGE = "key_language"
        private const val KEY_SPEED_UNIT = "key_speed_unit"
    }
}
