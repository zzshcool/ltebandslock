package com.ltebandslock.data.model

enum class AppThemeMode(val code: String, val titleZh: String, val titleEn: String) {
    DARK("dark", "深色主題", "Dark Mode"),
    LIGHT("light", "淺色主題", "Light Mode"),
    SYSTEM("system", "跟隨系統", "Follow System")
}

enum class AppLanguage(val code: String, val titleZh: String, val titleEn: String) {
    TRADITIONAL_CHINESE("zh_TW", "繁體中文", "Traditional Chinese"),
    ENGLISH("en", "英文", "English")
}

enum class SpeedUnit(val code: String, val label: String) {
    MBPS("mbps", "Mbps"),
    MB_S("mbs", "MB/s")
}

data class AppSettings(
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val language: AppLanguage = AppLanguage.TRADITIONAL_CHINESE,
    val speedUnit: SpeedUnit = SpeedUnit.MBPS
)
