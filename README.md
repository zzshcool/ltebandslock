# LTE Bands Lock

華為 4G/LTE 路由器（HiLink）Android 鎖頻、訊號監控與載波聚合管理工具。

## 支援機型

- **Huawei B618s-66d**（韌體 11.x，硬體支援至 3CA）
- **Huawei B818-263**（韌體 10.x / 11.x，硬體支援至 4CA/5CA）
- 採用 HiLink SCRAM-SHA256 挑戰認證之華為 4G/5G 路由器（如 B525、B535）

## 功能

- **頻段鎖定**：支援單頻或多頻組合鎖定（B1/B3/B7/B8/B28 等）及 AUTO 全頻段模式。
- **訊號與基站**：即時顯示 RSRP、RSRQ、SINR、RSSI、PCI、eNodeB ID、Cell ID、主頻段與頻寬。
- **載波聚合 (CA) 監測**：即時辨識 4G+ 狀態，並顯示 2CA / 3CA / 4CA 運行中標籤。
- **網速與流量**：即時下載/上傳速率，以及當期累計使用量與連線時間。
- **多設備切換**：支援儲存多台路由器 IP 與密碼，點擊切換連線。

## 常見問題：如何確認路由器正在跑 4CA / 3CA？

### 1. 基站動態調度機制
LTE 載波聚合 (CA) 是由電信基地台依傳輸負載即時動態調度：
- **待機狀態**：基站通常只維護 1 個主載波 (PCC)，不啟動額外載波以節省功率。
- **高速傳輸 / 測速時**：當下載流量激增時，基站會在數毫秒內自動啟用 1 至 3 個輔助載波 (SCC)，此時才會真正跑滿 2CA、3CA 或 4CA。

### 2. App 的識別方式
App 透過 HiLink 介面讀取以下指標：
- 狀態碼 `CurrentNetworkTypeEx=1011` 代表當前處於 LTE-A (4G+) 模式。
- 即時下行通道 `dl_mcs`（如偵測到 `mcsDownCarrier1` 至 `Carrier3/Carrier4`）。
- 依據當前聚合狀態在訊號卡片右上角標記 `2CA (4G+)`、`3CA (4G+)` 或 `4CA (4G+)`。

### 3. 機型硬體上限
- B618s-66d：Cat.11，硬體上限為 3CA。
- B818-263：Cat.19，硬體上限為 4CA/5CA。

## 建置與安裝

```bash
# 編譯 APK
.\gradlew.bat assembleDebug

# 透過 ADB 安裝至手機
adb install -r -t -d app\build\outputs\apk\debug\app-debug.apk
```
