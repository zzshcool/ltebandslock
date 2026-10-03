package com.ltebandslock.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import com.ltebandslock.data.model.AppLanguage

data class AppStrings(
    // Common & Actions
    val appTitle: String,
    val settings: String,
    val refresh: String,
    val cancel: String,
    val confirm: String,
    val apply: String,
    val close: String,
    val save: String,
    val ok: String,
    val success: String,
    val failed: String,

    // Settings Screen / Dialog
    val settingsTitle: String,
    val themeSection: String,
    val themeDark: String,
    val themeLight: String,
    val themeSystem: String,
    val languageSection: String,
    val langZh: String,
    val langEn: String,
    val speedUnitSection: String,
    val unitMbpsDesc: String,
    val unitMbsDesc: String,

    // Quick Tools
    val toolAntenna: String,
    val toolAntennaSub: String,
    val toolAdvisor: String,
    val toolAdvisorSub: String,
    val toolDevices: String,
    val toolDevicesSub: String,
    val toolCellMap: String,
    val toolCellMapSub: String,
    val toolSms: String,
    val toolSmsSub: String,
    val toolReboot: String,
    val toolRebootSub: String,

    // Bottom Bar
    val lockBandsAction: String,

    // Cards & Sections
    val cellularRfTitle: String,
    val networkSpeedTitle: String,
    val routerInfoTitle: String,

    // Signal Quality Labels (RF metrics like RSRP/SINR remain English)
    val qualityExcellent: String,
    val qualityGood: String,
    val qualityFair: String,
    val qualityPoor: String,
    val qualityUnknown: String,

    // Signal Bars
    val signalBarsFull: String,
    val signalBarsStrong: String,
    val signalBarsMedium: String,
    val signalBarsWeak: String,

    // Antenna Modes
    val antennaModeAuto: String,
    val antennaModeInternal: String,
    val antennaModeExternal: String,
    val antennaModeMixed: String,
    val antennaSelectionTitle: String
)

val StringsZh = AppStrings(
    appTitle = "LTE 鎖頻管家",
    settings = "設定",
    refresh = "重新整理",
    cancel = "取消",
    confirm = "確定",
    apply = "套用",
    close = "關閉",
    save = "儲存",
    ok = "完成",
    success = "成功",
    failed = "失敗",

    settingsTitle = "個人化設定",
    themeSection = "外觀風格",
    themeDark = "深色主題",
    themeLight = "淺色主題",
    themeSystem = "跟隨系統",
    languageSection = "語言設定",
    langZh = "繁體中文",
    langEn = "English",
    speedUnitSection = "網速單位",
    unitMbpsDesc = "Mbps (電信頻寬標準)",
    unitMbsDesc = "MB/s (檔案傳輸習慣)",

    toolAntenna = "ANTENNA",
    toolAntennaSub = "天線/對準",
    toolAdvisor = "ADVISOR",
    toolAdvisorSub = "頻段跑分",
    toolDevices = "DEVICES",
    toolDevicesSub = "設備清單",
    toolCellMap = "CELL MAP",
    toolCellMapSub = "基站反查",
    toolSms = "SMS",
    toolSmsSub = "簡訊管理",
    toolReboot = "REBOOT",
    toolRebootSub = "重啟設備",

    lockBandsAction = "LOCK BANDS (4G)",

    cellularRfTitle = "CELLULAR RF 訊號",
    networkSpeedTitle = "TRAFFIC 網路速度",
    routerInfoTitle = "ROUTER 設備資訊",

    qualityExcellent = "極佳",
    qualityGood = "良好",
    qualityFair = "普通",
    qualityPoor = "微弱",
    qualityUnknown = "未知",

    signalBarsFull = "滿格",
    signalBarsStrong = "強",
    signalBarsMedium = "中等",
    signalBarsWeak = "微弱",

    antennaModeAuto = "自動 (Auto)",
    antennaModeInternal = "內建天線 (Internal)",
    antennaModeExternal = "外接天線 (External)",
    antennaModeMixed = "混合模式 (Mixed)",
    antennaSelectionTitle = "天線硬體選擇"
)

val StringsEn = AppStrings(
    appTitle = "LTE Band Manager",
    settings = "Settings",
    refresh = "Refresh",
    cancel = "Cancel",
    confirm = "Confirm",
    apply = "Apply",
    close = "Close",
    save = "Save",
    ok = "OK",
    success = "Success",
    failed = "Failed",

    settingsTitle = "Preferences",
    themeSection = "Appearance Theme",
    themeDark = "Dark Mode",
    themeLight = "Light Mode",
    themeSystem = "System Default",
    languageSection = "Language",
    langZh = "繁體中文",
    langEn = "English",
    speedUnitSection = "Speed Metric Unit",
    unitMbpsDesc = "Mbps (Telecom standard)",
    unitMbsDesc = "MB/s (Download file rate)",

    toolAntenna = "ANTENNA",
    toolAntennaSub = "Antenna Tuning",
    toolAdvisor = "ADVISOR",
    toolAdvisorSub = "Band Advisor",
    toolDevices = "DEVICES",
    toolDevicesSub = "Host List",
    toolCellMap = "CELL MAP",
    toolCellMapSub = "Tower Lookup",
    toolSms = "SMS",
    toolSmsSub = "SMS Inbox",
    toolReboot = "REBOOT",
    toolRebootSub = "Reboot Device",

    lockBandsAction = "LOCK BANDS (4G)",

    cellularRfTitle = "CELLULAR RF",
    networkSpeedTitle = "TRAFFIC & SPEED",
    routerInfoTitle = "ROUTER INFO",

    qualityExcellent = "EXCELLENT",
    qualityGood = "GOOD",
    qualityFair = "FAIR",
    qualityPoor = "POOR",
    qualityUnknown = "UNKNOWN",

    signalBarsFull = "Full",
    signalBarsStrong = "Strong",
    signalBarsMedium = "Fair",
    signalBarsWeak = "Weak",

    antennaModeAuto = "Auto",
    antennaModeInternal = "Internal",
    antennaModeExternal = "External",
    antennaModeMixed = "Mixed",
    antennaSelectionTitle = "Antenna Hardware Mode"
)

val LocalAppStrings = compositionLocalOf { StringsZh }

@Composable
fun getAppStrings(language: AppLanguage): AppStrings {
    return when (language) {
        AppLanguage.TRADITIONAL_CHINESE -> StringsZh
        AppLanguage.ENGLISH -> StringsEn
    }
}
