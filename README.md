# LTE Band Manager

華為 4G/LTE 路由器（HiLink）Android 鎖頻、訊號監控與載波聚合管理工具。

## 支援機型

- **Huawei B818-263**（Cat.19，硬體支援至 4CA/5CA）
- **Huawei B618s-66d**（Cat.11，硬體支援至 3CA）
- 採用 HiLink API 之華為路由器（如 B525、B535、B715）

## 主要功能

- **鎖頻功能**：支援單頻或多頻組合鎖定（B1/B3/B7/B8/B20/B28/B38/B40/B41/B42/B43）與 AUTO 全頻段模式。
- **訊號與基站**：即時顯示 RSRP、RSRQ、SINR、RSSI、PCI、EARFCN、eNodeB ID、Cell ID、主頻段與頻寬。
- **載波聚合 (CA) 監測**：即時辨識 4G+ / 2CA / 3CA / 4CA 狀態與運行中頻段組合。
- **即時網速與流量**：即時下載/上傳速率，當期累計流量與連線時間。
- **多裝置登入管理**：支援儲存多台路由器 IP 與密碼，頂部選單一鍵切換。
- **進階工具列**：
  - 天線調校 (Antenna Tuning)
  - 頻段測速建議 (Band Advisor)
  - 在線裝置列表 (Connected Devices)
  - 基地台地圖查詢 (Cell Map)
  - 簡訊收發管理 (SMS Inbox)
  - 路由器遠端重啟 (Reboot Device)

## 常見問題：如何確認正在跑 4CA / 3CA？

1. **基站動態調度**：待機時基站僅維持 1 個主載波 (PCC)，高流量傳輸或測速時才會動態啟用 1 至 3 個輔助載波 (SCC)。
2. **硬體限制**：B618s-66d 上限 3CA；B818-263 上限 4CA/5CA。

## 建置方法

```bash
# 編譯 APK
.\gradlew.bat assembleDebug

# 安裝 APK
adb install -r -t -d app\build\outputs\apk\debug\app-debug.apk
```
