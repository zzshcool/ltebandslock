# LTE Bands Lock 開發與逆向工程維護指南 (CONTEXT.md)

本文件專為接手的 AI Agent 與後續維護人員撰寫，完整記錄針對華為 HiLink 路由器（以 B618s-66d 韌體 11.196.03.02.302 與 B818-263 為主要實測驗證目標）的逆向工程協議、Android 實作細節以及排查過程中所踩過的所有技術坑點。

---

## 1. 華為 HiLink 認證協議與核心陷阱

### 1.1 舊版 SHA256 認證失效
- 現象：舊版開源腳本或第三方 App 常使用 `/api/user/login` 搭配 `password_type=4`（username + SHA256(password) + token 的 base64 進行二次 SHA256）。
- 踩坑：新版韌體（如 B618 11.x、B818 全系列）已徹底關閉此介面，呼叫直接回傳錯誤碼 `108006` 或 `100003`。
- 解法：必須走標準 SCRAM-SHA256 挑戰認證流程（`/api/user/challenge_login` -> `/api/user/authentication_login`）。

### 1.2 陷阱：HTTP 200 實為錯誤回傳
- 踩坑：華為路由器 Web 伺服器在拒絕存取或認證失敗時，**HTTP Status Code 仍然回傳 200 OK**，但 Body 內容為：
  ```xml
  <?xml version="1.0" encoding="UTF-8"?>
  <error>
      <code>108006</code>
  </error>
  ```
- 後果：若在程式碼中使用 `response.isSuccessful` 或僅檢查 HTTP 狀態碼，會誤判為登入成功，導致 App 顯示連線綠燈，但後續取得資料全為空值或 `-`。
- 解法：必須嚴格解析 XML Body，若包含 `<error>` 標籤一律視為失敗，只有包含 `<response>`、`<rsan>` 或 `<response>OK</response>` 且無 `<error>` 時才算成功。

### 1.3 SCRAM-SHA256 加密細節與金鑰順序
挑戰認證步驟流程：
1. 向 `/api/webserver/token` 取得初始 Token：
   - 若回傳的 Token 長度大於 32 字元，必須取第 32 字元之後的子字串（即 `token.substring(32)`）。
2. 向 `/api/user/challenge_login` POST 包含隨機 Nonce 的 XML（Nonce 為 64 位的 16 進位隨機字串）：
   ```xml
   <?xml version="1.0" encoding="UTF-8"?>
   <request>
       <username>admin</username>
       <firstnonce>64位十六進位字串</firstnonce>
       <mode>1</mode>
   </request>
   ```
3. 路由器回傳 `servernonce`、`salt`（hex）與 `iterations`（通常為 100）。
4. 執行 SCRAM-SHA256 計算（順序極度關鍵，錯一步即認證失敗）：
   - `msg = clientNonce + "," + serverNonce + "," + serverNonce`（以 UTF-8 byte 陣列表示）。
   - `saltedPass = PBKDF2WithHmacSHA256(password, salt, iterations, 32)`（注意：密碼使用原始純文字，不可事先做 SHA256）。
   - `clientKey = HMAC_SHA256(key="Client Key", msg=saltedPass)`（重要：HMAC 的 Key 是固定字串 `"Client Key"` 的 UTF-8 byte，msg 才是 saltedPass）。
   - `storedKey = SHA256(clientKey)`。
   - `signature = HMAC_SHA256(key=msg, msg=storedKey)`（重要：Key 是 `msg`，資料是 `storedKey`）。
   - `clientProof = clientKey XOR signature`（逐 byte 異或運算後轉為十六進位字串）。
5. 向 `/api/user/authentication_login` POST 包含 clientproof 的 XML：
   ```xml
   <?xml version="1.0" encoding="UTF-8"?>
   <request>
       <clientproof>異或結果十六進位</clientproof>
       <finalnonce>servernonce</finalnonce>
   </request>
   ```

### 1.4 SessionID Cookie 覆蓋問題
- 踩坑：若僅在認證當下手動抓取 `Set-Cookie` 中的 `SessionID` 並以單一字串變數保存，後續發起 GET 請求時，若某個請求的回傳也帶有其他 `Set-Cookie`（不含 SessionID），手動賦值會將 `SessionID` 洗掉，導致路由器判定會話逾期（回傳 `100003`）。
- 解法：OkHttpClient 必須配置在記憶體中維護的 `CookieJar`（以 Map 保存各 Cookie 名稱），確保 `SessionID` 在連線生命週期內永久生效。

### 1.5 Verification Token 換票與「#」分隔符
- 踩坑：認證成功後，後續受保護請求所用的 Token 必須取自 Header `__RequestVerificationTokenone`（注意結尾的 `one`）。
- 踩坑：在部分端點（如 `/api/device/signal`），路由器回傳的 `__RequestVerificationToken` 是一串以 `#` 分隔的多組 Token（例如 `token1#token2#token3`）。如果未處理直接將整個字串帶入下一次 GET 請求的 Header，路由器會拒絕接受並報錯。
- 解法：讀取 Header Token 時，若包含 `#`，一律取第一個子字串（`token.split("#").first()`）。

---

## 2. API 端點分類與資料解析

### 2.1 端點存取權限
- 公開端點（無需登入）：
  - `/api/monitoring/traffic-statistics`：流量統計（TotalDownload, TotalUpload, CurrentDownloadRate, CurrentUploadRate）。
  - `/api/net/current-plmn`：當前電信業者代碼與名稱（FullName, ShortName）。
- 受保護端點（需通過 SCRAM 登入）：
  - `/api/device/signal`：完整 RF 訊號（rsrp, rsrq, sinr, rssi, band, dl_mcs, cell_id, pci 等）。
  - `/api/device/information`：型號（DeviceName）、WAN IP（WanIPAddress）、IMEI、軟韌體版本。
  - `/api/net/net-mode`：目前生效之網路模式與頻段遮罩（LTEBand）。

### 2.2 LTE 頻段 16 進位遮罩 (Hex Mask) 計算
`/api/net/net-mode` 的 `<LTEBand>` 標籤採用 16 進位 Bitmask。
計算公式：`Bit = 1 << (Band - 1)`

各主要頻段對應表：
- Band 1: `1 << 0 = 1` (Hex: `1`)
- Band 3: `1 << 2 = 4` (Hex: `4`)
- Band 5: `1 << 4 = 16` (Hex: `10`)
- Band 7: `1 << 6 = 64` (Hex: `40`)
- Band 8: `1 << 7 = 128` (Hex: `80`)
- Band 20: `1 << 19 = 524288` (Hex: `80000`)
- Band 28: `1 << 27 = 134217728` (Hex: `8000000`)
- Band 38: `1 << 37` (Hex: `2000000000`)
- Band 40: `1 << 39` (Hex: `8000000000`)
- Band 41: `1 << 40` (Hex: `10000000000`)

多頻聚合設定：
- 若鎖定 B1 + B3，Hex Mask = `1 + 4 = 5`。
- 若鎖定 B1 + B3 + B5 + B7 + B28，Hex Mask = `8000055`。
- AUTO 全頻段：傳入 `7fffffffffffffff` 且 `<NetworkMode>` 設為 `00`（指定特定頻段時 `<NetworkMode>` 設為 `03`）。

### 2.3 eNodeB ID 與 Sector/Cell 換算
`/api/device/signal` 回傳的 `<cell_id>` 為全球唯一小區識別碼（ECI，10 進位整數，如 `61784051`）。
換算邏輯：
- `eNodeB ID = cell_id / 256`（例如：`61784051 / 256 = 241343`）
- `Cell ID (Sector) = cell_id % 256`（例如：`61784051 % 256 = 243`）

### 2.4 載波聚合 (2CA / 3CA / 4CA) 判定邏輯
1. 是否啟用 4G+：
   - 檢查 `/api/monitoring/status` 的 `<CurrentNetworkTypeEx>`，值為 `1011` 表示處於 LTE-A (CA) 模式。
2. 正在跑幾 CA (2CA / 3CA / 4CA)：
   - LTE 載波聚合屬於「動態調度（Dynamic Scheduling）」，低負載待機時基站通常只維護主載波 (PCC)。
   - 當發生高吞吐流量時，基站會瞬間啟動 1 至 3 個輔助載波 (SCC)。
   - 在 `/api/device/signal` 中可讀取 `<dl_mcs>`：若內容包含 `Carrier4` 為 4CA，包含 `Carrier3` 為 3CA，包含 `Carrier2` 為 2CA。
   - 若為待機狀態，但鎖頻或當前配置包含多個生效頻段（如 B1+B3），App 亦會結合標明配置上限（如 `2CA (4G+)`）。

---

## 3. Android UI/UX 踩坑與介面適配

### 3.1 底部按鈕被系統導航列遮蓋 (WindowInsets)
- 踩坑：在未呼叫 `enableEdgeToEdge()` 的情況下，`navigationBarsPadding()` 可能回傳 0，但系統三鍵導航列仍會覆蓋在 Activity 上，導致最底部的 `LOCK BANDS (4G)` 按鈕被切成一半。
- 解法：
  1. `MainActivity.kt` 必須在 `super.onCreate()` 後立即呼叫 `enableEdgeToEdge()`。
  2. 底部動作列使用 `Surface` 包裹並套用 `Modifier.navigationBarsPadding()`。

### 3.2 文字截斷與強制斷詞 (DOWNLOA D / WAN IP 折行)
- 踩坑：在小螢幕或高顯示縮放比例下，Compose 的 `Text` 若未設定限制，遇到空白或寬度不足會自行折行，產生 `DOWNLOA \n D` 或 `10.59.229.2 \n 2` 等破版現象。
- 解法：所有數值與標籤一律加上 `maxLines = 1` 與 `softWrap = false`，適度調降字級（如標題 10.sp、數值 13.sp~17.sp），並在水平排版的 `Row` 中合理配置 `weight(1f)`，防止擠壓。

### 3.3 4G+ 標籤被擠成直排文字
- 踩坑：在 `SignalMeterCard` 標題 Row 中，若左側標題佔滿空間而右側 Badge 未設定 `maxLines = 1, softWrap = false`，Badge 寬度會被壓縮至極小，導致文字被擠壓成單字母縱向排列（如 `4 \n G \n + \n C \n A`）。
- 解法：左側 Row 設定 `Modifier.weight(1f, fill = false)`，右側 Badge 設定 `maxLines = 1, softWrap = false`。

### 3.4 解決「很 AI」之模板化膨脹（高資訊密度專業儀表板）
- **現象**：典型 AI 生成的 UI 習慣使用巨大圓角（20dp+）、大號圓圈帶霓虹漸層圖示（28dp~40dp）、誇張行距與多重卡片嵌套（Card 裡面套 Box 再套 Card），導致行動裝置一屏只能看見 2 個卡片，核心數值（RSRP/SINR/eNodeB）被擠到螢幕下半部必須不斷滾動才能看見。
- **解法**：
  1. **頂部整合**：將 App 標題、當前路由器狀態 Chip（附帶狀態燈號與點擊切換）以及刷新按鈕收攏為單一 32dp 水平列。
  2. **核心訊號優先**：將 `CELLULAR RF` 卡片置頂，4 個訊號指標（RSRP、RSRQ、SINR、RSSI）採用緊湊 4 欄並排，搭配精巧狀態燈與 2.5dp 迷你進度條，高密度呈現。
  3. **去嵌套化**：移除所有卡片內的裝飾性大型圓圈圖示與雙層嵌套 Card，改用單層 `RoundedCornerShape(10.dp)` 配合 `0.8dp` 深灰邊框。
  4. **全螢幕一覽**：上下傳速率、總流量、eNodeB、Cell ID、Sector、WAN IP、主頻段與 3CA/4CA 標籤，全部在第一屏完整呈現，無需滑動即可一眼掌握全貌。


---

## 4. 本地環境與指令參考

- 手機環境：realme RMX3850（序號 `ff940b68`）
- 路由器環境：Huawei B618s-66d（預設 IP `192.168.1.1`，韌體 `11.196.03.02.302`）
- ADB 路徑：`C:\Users\jasonX16\AppData\Local\Android\Sdk\platform-tools\adb.exe`
- JDK 路徑：`C:\Program Files\Android\Android Studio\jbr`（於 `gradle.properties` 設定 `org.gradle.java.home`）
- 編譯指令：
  ```bash
  .\gradlew.bat assembleDebug
  ```
- 安裝指令：
  ```bash
  & "C:\Users\jasonX16\AppData\Local\Android\Sdk\platform-tools\adb.exe" install -r -t -d app\build\outputs\apk\debug\app-debug.apk
  ```
- 啟動指令：
  ```bash
  & "C:\Users\jasonX16\AppData\Local\Android\Sdk\platform-tools\adb.exe" shell am start -S -n com.ltebandslock/.MainActivity
  ```
