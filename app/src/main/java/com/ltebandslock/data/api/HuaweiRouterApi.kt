package com.ltebandslock.data.api

import android.util.Base64
import android.util.Log
import com.ltebandslock.data.model.AntennaMode
import com.ltebandslock.data.model.AntennaStatus
import com.ltebandslock.data.model.ConnectedDevice
import com.ltebandslock.data.model.DeviceInfo
import com.ltebandslock.data.model.LteBandInfo
import com.ltebandslock.data.model.LteBands
import com.ltebandslock.data.model.RouterProfile
import com.ltebandslock.data.model.SignalInfo
import com.ltebandslock.data.model.SmsCount
import com.ltebandslock.data.model.SmsMessage
import com.ltebandslock.data.model.TrafficInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class HuaweiRouterApi {

    companion object {
        private const val TAG = "HuaweiRouterApi"
    }

    private val cookieStore = ConcurrentHashMap<String, String>()

    private val client = OkHttpClient.Builder()
        .cookieJar(object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                for (c in cookies) {
                    cookieStore[c.name] = c.value
                }
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                val result = mutableListOf<Cookie>()
                for ((name, value) in cookieStore) {
                    result.add(
                        Cookie.Builder()
                            .domain(url.host)
                            .path("/")
                            .name(name)
                            .value(value)
                            .build()
                    )
                }
                return result
            }
        })
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private var sessionCookie: String? = null
    private var verificationToken: String? = null

    suspend fun login(profile: RouterProfile): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            cookieStore.clear()
            val baseUrl = sanitizeUrl(profile.ipAddress)
            Log.d(TAG, "Attempting login to $baseUrl as ${profile.username}")

            // Step 1: Initialize session
            val initReq = Request.Builder().url("$baseUrl/").get().build()
            client.newCall(initReq).execute()

            // Step 2: Retrieve initial verification token
            val initialToken = getInitialToken(baseUrl)

            // Step 3: Try SCRAM-SHA256 Challenge Login (B818-263, B618 newer firmware, B535, etc.)
            val scramResult = tryScramLogin(baseUrl, profile, initialToken)
            if (scramResult.isSuccess) {
                Log.d(TAG, "SCRAM login succeeded!")
                return@withContext Result.success(true)
            } else {
                Log.d(TAG, "SCRAM login failed: ${scramResult.exceptionOrNull()?.message}, trying SHA256 fallback")
            }

            // Step 4: Fallback to traditional password_type=4 SHA256 login (B618 older firmware)
            val sha256Result = trySha256Login(baseUrl, profile, initialToken)
            if (sha256Result.isSuccess) {
                Log.d(TAG, "SHA256 login succeeded!")
                return@withContext Result.success(true)
            }

            Result.failure(Exception("Login failed: ${scramResult.exceptionOrNull()?.message ?: "Authentication rejected"}"))
        } catch (e: Exception) {
            Log.e(TAG, "Login error", e)
            Result.failure(Exception("Connection error: ${e.localizedMessage}"))
        }
    }

    private fun getInitialToken(baseUrl: String): String {
        return try {
            val tokenReq = Request.Builder().url("$baseUrl/api/webserver/token").get().build()
            val tokenResp = client.newCall(tokenReq).execute()
            val tokenXml = tokenResp.body?.string() ?: ""
            val rawToken = parseXmlTag(tokenXml, "token")

            // Capture initial Set-Cookie if any
            tokenResp.header("Set-Cookie")?.let { cookie ->
                sessionCookie = cookie.split(";")[0]
            }

            if (rawToken.isNotEmpty()) {
                if (rawToken.length > 32) rawToken.substring(32) else rawToken
            } else {
                val sesTokReq = Request.Builder().url("$baseUrl/api/webserver/SesTokInfo").get().build()
                val sesTokResp = client.newCall(sesTokReq).execute()
                sesTokResp.header("Set-Cookie")?.let { cookie ->
                    sessionCookie = cookie.split(";")[0]
                }
                parseXmlTag(sesTokResp.body?.string() ?: "", "TokInfo")
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun tryScramLogin(baseUrl: String, profile: RouterProfile, initialToken: String): Result<Boolean> {
        return try {
            val clientNonce = generateRandomHex(32) // 64 hex characters
            val challengeXml = """<?xml version="1.0" encoding="UTF-8"?>
<request>
    <username>${profile.username}</username>
    <firstnonce>$clientNonce</firstnonce>
    <mode>1</mode>
</request>""".trimIndent()

            val chalBuilder = Request.Builder()
                .url("$baseUrl/api/user/challenge_login")
                .post(challengeXml.toRequestBody("text/html".toMediaType()))
                .header("__RequestVerificationToken", initialToken)

            sessionCookie?.let { chalBuilder.header("Cookie", it) }

            val chalResp = client.newCall(chalBuilder.build()).execute()
            val chalBody = chalResp.body?.string() ?: ""

            // Capture cookie and token
            chalResp.header("Set-Cookie")?.let { cookie ->
                sessionCookie = cookie.split(";")[0]
            }
            val chalToken = chalResp.header("__RequestVerificationToken") ?: initialToken

            val saltHex = parseXmlTag(chalBody, "salt")
            val serverNonce = parseXmlTag(chalBody, "servernonce")
            val iterations = parseXmlTag(chalBody, "iterations").toIntOrNull() ?: 100

            if (saltHex.isEmpty() || serverNonce.isEmpty()) {
                return Result.failure(Exception("Not a SCRAM router or invalid challenge response"))
            }

            // Correct Huawei SCRAM calculation:
            // msg = clientnonce,servernonce,servernonce
            // saltedPass = PBKDF2(rawPassword, salt, iterations, 32)
            // clientKey = HMAC(key="Client Key", msg=saltedPass)
            // storedKey = SHA256(clientKey)
            // signature = HMAC(key=msg, msg=storedKey)
            // clientProof = clientKey XOR signature
            val msg = "$clientNonce,$serverNonce,$serverNonce".toByteArray(Charsets.UTF_8)
            val salt = hexToBytes(saltHex)

            val saltedPass = pbkdf2HmacSha256(profile.password, salt, iterations, 32)
            val clientKey = hmacSha256("Client Key".toByteArray(Charsets.UTF_8), saltedPass)
            val storedKey = sha256Bytes(clientKey)
            val signature = hmacSha256(msg, storedKey)
            val clientProof = xorBytes(clientKey, signature)
            val clientProofHex = bytesToHex(clientProof)

            val authXml = """<?xml version="1.0" encoding="UTF-8"?>
<request>
    <clientproof>$clientProofHex</clientproof>
    <finalnonce>$serverNonce</finalnonce>
</request>""".trimIndent()

            val authBuilder = Request.Builder()
                .url("$baseUrl/api/user/authentication_login")
                .post(authXml.toRequestBody("application/x-www-form-urlencoded; charset=UTF-8".toMediaType()))
                .header("__RequestVerificationToken", chalToken)

            val authResp = client.newCall(authBuilder.build()).execute()
            val authBody = authResp.body?.string() ?: ""

            // The final token for all subsequent requests is in __RequestVerificationTokenone or __RequestVerificationToken
            val finalTok = authResp.header("__RequestVerificationTokenone")
                ?: authResp.header("__RequestVerificationToken")?.split("#")?.firstOrNull()
                ?: chalToken
            verificationToken = finalTok

            if ((authBody.contains("<response>") || authBody.contains("<rsan>") || authBody.contains("OK")) && !authBody.contains("<error>")) {
                Log.d(TAG, "SCRAM Authentication confirmed success")
                Result.success(true)
            } else {
                val errorCode = parseXmlTag(authBody, "code")
                Result.failure(Exception("Authentication rejected (code: $errorCode)"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun trySha256Login(baseUrl: String, profile: RouterProfile, token: String): Result<Boolean> {
        return try {
            val pwdSha256Base64 = sha256Base64(profile.password)
            val authInput = profile.username + pwdSha256Base64 + token
            val finalAuthHash = sha256Base64(authInput)

            val loginXml = """<?xml version="1.0" encoding="UTF-8"?>
<request>
    <username>${profile.username}</username>
    <password>$finalAuthHash</password>
    <password_type>4</password_type>
</request>""".trimIndent()

            val reqBuilder = Request.Builder()
                .url("$baseUrl/api/user/login")
                .post(loginXml.toRequestBody("application/xml; charset=utf-8".toMediaType()))
                .header("__RequestVerificationToken", token)

            val resp = client.newCall(reqBuilder.build()).execute()
            val finalTok = resp.header("__RequestVerificationTokenone")
                ?: resp.header("__RequestVerificationToken")?.split("#")?.firstOrNull()
                ?: token
            verificationToken = finalTok

            val body = resp.body?.string() ?: ""
            if (body.contains("<response>OK</response>") && !body.contains("<error>")) {
                Result.success(true)
            } else {
                val code = parseXmlTag(body, "code")
                Result.failure(Exception("SHA256 Login failed (code: $code)"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSignalInfo(ipAddress: String): Result<SignalInfo> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = sanitizeUrl(ipAddress)
            val xml = makeGetRequest("$baseUrl/api/device/signal")
            if (xml.isEmpty() || xml.contains("<error>")) {
                return@withContext Result.failure(Exception("Failed to get signal details"))
            }

            val rsrp = parseXmlTag(xml, "rsrp").replace("dBm", "").trim().toIntOrNull()
            val rsrq = parseXmlTag(xml, "rsrq").replace("dB", "").trim().toIntOrNull()
            val sinr = parseXmlTag(xml, "sinr").replace("dB", "").trim().toIntOrNull()
            val rssi = parseXmlTag(xml, "rssi").replace("dBm", "").trim().toIntOrNull()

            val pci = parseXmlTag(xml, "pci")
            val earfcn = parseXmlTag(xml, "earfcn")
            val rawBand = parseXmlTag(xml, "band")
            val dlBw = parseXmlTag(xml, "dlbandwidth")
            val ulBw = parseXmlTag(xml, "ulbandwidth")

            val primaryBand = if (rawBand.isNotEmpty()) "B$rawBand" else "-"
            val bandwidth = if (dlBw.isNotEmpty() && ulBw.isNotEmpty()) "$dlBw / $ulBw" else dlBw.ifEmpty { ulBw.ifEmpty { "-" } }

            // Fetch net-mode for configured/active LTE bands mask
            val netModeXml = makeGetRequest("$baseUrl/api/net/net-mode")
            val lteBandHex = parseXmlTag(netModeXml, "LTEBand")
            val decodedBands = decodeLteBandMask(lteBandHex)

            // Check Carrier Aggregation
            val statusXml = makeGetRequest("$baseUrl/api/monitoring/status")
            val netTypeEx = parseXmlTag(statusXml, "CurrentNetworkTypeEx")
            val netType = parseXmlTag(statusXml, "CurrentNetworkType")
            val isCa = netTypeEx == "1011" || netType == "101" || decodedBands.size > 1

            // Detect CA carrier count from active MCS channels or configured bands (Cat.11/Cat.19 B818/B618)
            val dlMcs = parseXmlTag(xml, "dl_mcs")

            // Method 1: Count distinct Carrier indexes in dl_mcs (0-based: Carrier0=PCC, Carrier1=SCC1, Carrier2=SCC2, Carrier3=SCC3 -> 4CA)
            val carrierRegex = "(?i)Carrier([0-9]+)".toRegex()
            val carrierMatches = carrierRegex.findAll(dlMcs).mapNotNull { it.groupValues[1].toIntOrNull() }.toList()
            val mcsCarrierCount = if (carrierMatches.isNotEmpty()) {
                val maxIdx = carrierMatches.maxOrNull() ?: 0
                maxIdx + 1 // e.g. max index 3 means 4 carriers (0,1,2,3) = 4CA
            } else if (dlMcs.contains("Carrier4", ignoreCase = true)) {
                5
            } else if (dlMcs.contains("Carrier3", ignoreCase = true)) {
                4
            } else if (dlMcs.contains("Carrier2", ignoreCase = true)) {
                3
            } else if (dlMcs.contains("Carrier1", ignoreCase = true)) {
                2
            } else {
                0
            }

            // Method 2: Count secondary cell XML blocks (<secondary_cell>, <sec_cell>, <scc>)
            val secCellCount = listOf(
                "(?i)<secondary_cell[\\s>]".toRegex().findAll(xml).count(),
                "(?i)<sec_cell[\\s>]".toRegex().findAll(xml).count(),
                "(?i)<scc[1-9]_".toRegex().findAll(xml).distinctBy { it.value }.count()
            ).maxOrNull() ?: 0
            val sccCarrierCount = if (secCellCount > 0) secCellCount + 1 else 0

            // Method 3: Parse distinct SCC bands in XML (e.g. scc1_band, scc2_band, scc3_band)
            val sccBands = mutableListOf<String>()
            for (i in 1..4) {
                val b = parseXmlTag(xml, "scc${i}_band").ifEmpty {
                    parseXmlTag(xml, "secondary_cell_band$i")
                }
                if (b.isNotEmpty()) sccBands.add("B$b")
            }
            val parsedSccCount = if (sccBands.isNotEmpty()) sccBands.size + 1 else 0

            // Determine final caCount
            val detectedCa = maxOf(mcsCarrierCount, sccCarrierCount, parsedSccCount)
            val caCount = when {
                detectedCa in 2..5 -> detectedCa
                isCa && decodedBands.size in 2..4 -> decodedBands.size
                isCa -> 2
                else -> 1
            }

            val caLabel = if (isCa && caCount > 1) "${caCount}CA (4G+)" else if (isCa) "4G+ CA" else "4G"

            val activeBandsStr = if (decodedBands.isNotEmpty()) {
                decodedBands.joinToString("+")
            } else if (sccBands.isNotEmpty()) {
                (listOf(primaryBand) + sccBands).distinct().joinToString("+")
            } else {
                primaryBand
            }

            // Parse Signal Bars (0..5) from router hardware status or calculate from RSRP/SINR
            val rawSignalIcon = parseXmlTag(statusXml, "SignalIcon").toIntOrNull()
                ?: parseXmlTag(statusXml, "signalicon").toIntOrNull()
                ?: parseXmlTag(statusXml, "SignalStrength").toIntOrNull()
                ?: parseXmlTag(statusXml, "signalstrength").toIntOrNull()

            val calculatedBars = when {
                rsrp == null -> 0
                rsrp >= -85 && (sinr == null || sinr >= 10) -> 5
                rsrp >= -95 && (sinr == null || sinr >= 5) -> 4
                rsrp >= -105 && (sinr == null || sinr >= 0) -> 3
                rsrp >= -115 -> 2
                rsrp >= -125 -> 1
                else -> 0
            }
            val signalBars = (rawSignalIcon ?: calculatedBars).coerceIn(0, 5)

            val configuredBandsList = if (lteBandHex.isNotEmpty()) {
                val parsed = LteBands.parseHexMask(lteBandHex)
                if (parsed.isNotEmpty() && parsed.size < LteBands.ALL_BANDS.size) {
                    parsed
                } else if (decodedBands.isNotEmpty()) {
                    LteBands.fromBandNames(decodedBands)
                } else {
                    parsed
                }
            } else if (decodedBands.isNotEmpty()) {
                LteBands.fromBandNames(decodedBands)
            } else {
                emptyList()
            }

            Result.success(
                SignalInfo(
                    rsrp = rsrp,
                    rsrq = rsrq,
                    sinr = sinr,
                    rssi = rssi,
                    primaryBand = primaryBand,
                    activeBands = activeBandsStr,
                    configuredBands = configuredBandsList,
                    bandwidth = bandwidth,
                    aggregation = isCa,
                    caCount = caCount,
                    caLabel = caLabel,
                    pci = pci.ifEmpty { "-" },
                    earfcn = earfcn.ifEmpty { "-" },
                    signalBars = signalBars
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDeviceInfo(ipAddress: String): Result<DeviceInfo> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = sanitizeUrl(ipAddress)

            val infoXml = makeGetRequest("$baseUrl/api/device/information")
            val modelName = parseXmlTag(infoXml, "DeviceName").ifEmpty {
                parseXmlTag(infoXml, "Classname").ifEmpty {
                    parseXmlTag(infoXml, "workmode").ifEmpty { "Huawei Router" }
                }
            }
            val wanIp = parseXmlTag(infoXml, "WanIPAddress")

            val statusXml = makeGetRequest("$baseUrl/api/monitoring/status")
            val netTypeEx = parseXmlTag(statusXml, "CurrentNetworkTypeEx")
            val rawNetType = parseXmlTag(statusXml, "CurrentNetworkType")
            val networkTypeStr = when {
                netTypeEx == "1011" || rawNetType == "101" -> "4G+"
                rawNetType == "19" -> "4G"
                rawNetType == "9" -> "3G"
                else -> if (netTypeEx.isNotEmpty() || rawNetType.isNotEmpty()) "4G" else "-"
            }

            val plmnXml = makeGetRequest("$baseUrl/api/net/current-plmn")
            val carrier = parseXmlTag(plmnXml, "FullName").ifEmpty {
                parseXmlTag(plmnXml, "ShortName").ifEmpty { "LTE Network" }
            }
            val rawNumeric = parseXmlTag(plmnXml, "Numeric")
            val mcc = if (rawNumeric.length >= 3) rawNumeric.substring(0, 3) else "466"
            val mnc = if (rawNumeric.length > 3) rawNumeric.substring(3) else "89"

            val signalXml = makeGetRequest("$baseUrl/api/device/signal")
            val fullCellId = parseXmlTag(signalXml, "cell_id")
            val pci = parseXmlTag(signalXml, "pci")
            val tac = parseXmlTag(signalXml, "tac").ifEmpty {
                parseXmlTag(signalXml, "TAC").ifEmpty { "-" }
            }

            var eNodeB = "-"
            var cellDisplay = "-"
            if (fullCellId.isNotEmpty() && fullCellId != "-") {
                try {
                    val cellDec = fullCellId.toLong()
                    val eNodeBId = cellDec / 256
                    val cellIdLocal = cellDec % 256
                    eNodeB = "$eNodeBId"
                    cellDisplay = "$cellDec (eNodeB: $eNodeBId, Cell: $cellIdLocal, PCI: $pci)"
                } catch (e: Exception) {
                    cellDisplay = fullCellId
                }
            }

            val trafficXml = makeGetRequest("$baseUrl/api/monitoring/traffic-statistics")
            val totalDl = parseXmlTag(trafficXml, "TotalDownload").toLongOrNull() ?: 0L
            val totalUl = parseXmlTag(trafficXml, "TotalUpload").toLongOrNull() ?: 0L
            val totalBytes = totalDl + totalUl
            val usedDataFormatted = formatDataUsage(totalBytes)

            Result.success(
                DeviceInfo(
                    model = modelName,
                    networkType = networkTypeStr,
                    carrier = carrier,
                    cellId = cellDisplay,
                    eNodeBId = eNodeB,
                    wanIp = wanIp.ifEmpty { "-" },
                    usedData = usedDataFormatted,
                    tac = tac,
                    plmn = rawNumeric.ifEmpty { "$mcc$mnc" },
                    mcc = mcc,
                    mnc = mnc
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTrafficInfo(ipAddress: String): Result<TrafficInfo> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = sanitizeUrl(ipAddress)
            val xml = makeGetRequest("$baseUrl/api/monitoring/traffic-statistics")

            val downloadRate = parseXmlTag(xml, "CurrentDownloadRate").toLongOrNull() ?: 0L
            val uploadRate = parseXmlTag(xml, "CurrentUploadRate").toLongOrNull() ?: 0L
            val totalDl = parseXmlTag(xml, "TotalDownload").toLongOrNull() ?: 0L
            val totalUl = parseXmlTag(xml, "TotalUpload").toLongOrNull() ?: 0L
            val connectTime = parseXmlTag(xml, "CurrentConnectTime").toLongOrNull() ?: 0L

            Result.success(
                TrafficInfo(
                    downloadRateBytes = downloadRate,
                    uploadRateBytes = uploadRate,
                    totalDownloadBytes = totalDl,
                    totalUploadBytes = totalUl,
                    connectTimeSeconds = connectTime
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setLteBands(ipAddress: String, selectedBands: List<LteBandInfo>): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = sanitizeUrl(ipAddress)

            val hexMask = LteBands.calculateHexMask(selectedBands)
            val networkMode = if (selectedBands.isEmpty() || selectedBands.size == LteBands.ALL_BANDS.size) "00" else "03"

            val xmlPayload = """<?xml version="1.0" encoding="UTF-8"?>
<request>
    <NetworkMode>$networkMode</NetworkMode>
    <NetworkBand>3FFFFFFF</NetworkBand>
    <LTEBand>$hexMask</LTEBand>
</request>""".trimIndent()

            val requestBuilder = Request.Builder()
                .url("$baseUrl/api/net/net-mode")
                .post(xmlPayload.toRequestBody("application/xml; charset=utf-8".toMediaType()))

            sessionCookie?.let { requestBuilder.header("Cookie", it) }
            verificationToken?.let { requestBuilder.header("__RequestVerificationToken", it) }

            val response = client.newCall(requestBuilder.build()).execute()
            val bodyString = response.body?.string() ?: ""

            // Update token if returned
            response.header("__RequestVerificationToken")?.let { verificationToken = it }
            response.header("Set-Cookie")?.let { sessionCookie = it.split(";")[0] }

            if (bodyString.contains("<response>OK</response>") && !bodyString.contains("<error>")) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to update bands: ${parseXmlTag(bodyString, "code")}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun makeGetRequest(url: String): String {
        return try {
            val builder = Request.Builder().url(url).get()
            verificationToken?.let { builder.header("__RequestVerificationToken", it) }

            val response = client.newCall(builder.build()).execute()

            // Update verification token if router returns refreshed token
            response.header("__RequestVerificationTokenone")?.let {
                verificationToken = it
            } ?: response.header("__RequestVerificationToken")?.let {
                val tok = it.split("#").firstOrNull() ?: it
                if (tok.isNotEmpty()) verificationToken = tok
            }

            response.body?.string() ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "GET error for $url: ${e.message}")
            ""
        }
    }

    private fun decodeLteBandMask(hexStr: String): List<String> {
        val result = mutableListOf<String>()
        if (hexStr.isEmpty()) return result
        try {
            val value = hexStr.toLong(16)
            for (band in listOf(1, 3, 7, 8, 20, 28, 32, 38, 40, 41, 42, 43)) {
                val bit = 1L shl (band - 1)
                if ((value and bit) != 0L) {
                    result.add("B$band")
                }
            }
        } catch (e: Exception) {
            // ignore
        }
        return result
    }

    private fun formatDataUsage(bytes: Long): String {
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        return if (gb >= 1000.0) {
            String.format("%.2f TB", gb / 1024.0)
        } else {
            String.format("%.2f GB", gb)
        }
    }

    private fun sanitizeUrl(ip: String): String {
        var cleanIp = ip.trim()
        if (!cleanIp.startsWith("http://") && !cleanIp.startsWith("https://")) {
            cleanIp = "http://$cleanIp"
        }
        return cleanIp.removeSuffix("/")
    }

    private fun parseXmlTag(xml: String, tag: String): String {
        val pattern = "<$tag>(.*?)</$tag>".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        val match = pattern.find(xml)
        return match?.groupValues?.get(1)?.trim() ?: ""
    }

    private fun sha256Base64(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(digest, Base64.NO_WRAP)
    }

    private fun generateRandomHex(byteCount: Int): String {
        val bytes = ByteArray(byteCount)
        SecureRandom().nextBytes(bytes)
        return bytesToHex(bytes)
    }

    private fun pbkdf2HmacSha256(passwordStr: String, salt: ByteArray, iterations: Int, keyLengthBytes: Int): ByteArray {
        val spec = PBEKeySpec(passwordStr.toCharArray(), salt, iterations, keyLengthBytes * 8)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    private fun sha256Bytes(data: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(data)
    }

    private fun xorBytes(a: ByteArray, b: ByteArray): ByteArray {
        val result = ByteArray(a.size)
        for (i in a.indices) {
            result[i] = (a[i].toInt() xor b[i].toInt()).toByte()
        }
        return result
    }

    private fun hexToBytes(hex: String): ByteArray {
        val cleanHex = if (hex.length % 2 != 0) "0$hex" else hex
        val result = ByteArray(cleanHex.length / 2)
        for (i in result.indices) {
            val index = i * 2
            result[i] = cleanHex.substring(index, index + 2).toInt(16).toByte()
        }
        return result
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    suspend fun rebootRouter(ipAddress: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = sanitizeUrl(ipAddress)
            val xmlPayload = """<?xml version="1.0" encoding="UTF-8"?>
<request>
    <Control>1</Control>
</request>""".trimIndent()
            val requestBuilder = Request.Builder()
                .url("$baseUrl/api/device/control")
                .post(xmlPayload.toRequestBody("application/xml; charset=utf-8".toMediaType()))

            sessionCookie?.let { requestBuilder.header("Cookie", it) }
            verificationToken?.let { requestBuilder.header("__RequestVerificationToken", it) }

            val response = client.newCall(requestBuilder.build()).execute()
            response.header("__RequestVerificationToken")?.let { verificationToken = it }
            val body = response.body?.string() ?: ""
            if (body.contains("<response>OK</response>") || response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("Reboot failed: $body"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSmsCount(ipAddress: String): Result<SmsCount> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = sanitizeUrl(ipAddress)
            val xml = makeGetRequest("$baseUrl/api/sms/sms-count")
            val total = parseXmlTag(xml, "LocalInbox").toIntOrNull() ?: 0
            val unread = parseXmlTag(xml, "LocalUnread").toIntOrNull() ?: 0
            Result.success(SmsCount(total = total, unread = unread))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSmsList(ipAddress: String, page: Int = 1, maxCount: Int = 20): Result<List<SmsMessage>> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = sanitizeUrl(ipAddress)
            val xmlPayload = """<?xml version="1.0" encoding="UTF-8"?>
<request>
    <PageIndex>$page</PageIndex>
    <ReadCount>$maxCount</ReadCount>
    <BoxType>1</BoxType>
    <SortType>0</SortType>
    <Ascending>0</Ascending>
    <UnreadPreferred>0</UnreadPreferred>
</request>""".trimIndent()

            val requestBuilder = Request.Builder()
                .url("$baseUrl/api/sms/sms-list")
                .post(xmlPayload.toRequestBody("application/xml; charset=utf-8".toMediaType()))

            sessionCookie?.let { requestBuilder.header("Cookie", it) }
            verificationToken?.let { requestBuilder.header("__RequestVerificationToken", it) }

            val response = client.newCall(requestBuilder.build()).execute()
            response.header("__RequestVerificationToken")?.let { verificationToken = it }
            val body = response.body?.string() ?: ""
            Log.d(TAG, "sms-list response (code=${response.code}): $body")

            val messages = mutableListOf<SmsMessage>()
            val msgPattern = "<message>(.*?)</message>".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
            val matches = msgPattern.findAll(body)
            for (match in matches) {
                val block = match.groupValues[1]
                val index = parseXmlTag(block, "Index").toLongOrNull()
                    ?: parseXmlTag(block, "index").toLongOrNull() ?: 0L
                val phone = parseXmlTag(block, "Phone").ifEmpty { parseXmlTag(block, "phone") }
                val rawContent = parseXmlTag(block, "Content").ifEmpty { parseXmlTag(block, "content") }
                val content = try {
                    android.text.Html.fromHtml(rawContent, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
                } catch (e: Exception) {
                    rawContent
                }
                val date = parseXmlTag(block, "Date").ifEmpty { parseXmlTag(block, "date") }
                val smstat = parseXmlTag(block, "smstat").ifEmpty {
                    parseXmlTag(block, "Smstype").ifEmpty {
                        parseXmlTag(block, "SmsType")
                    }
                }
                messages.add(
                    SmsMessage(
                        index = index,
                        phone = phone,
                        content = content,
                        date = date,
                        isUnread = smstat == "0"
                    )
                )
            }
            Result.success(messages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendSms(ipAddress: String, phone: String, content: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = sanitizeUrl(ipAddress)
            val xmlPayload = """<?xml version="1.0" encoding="UTF-8"?>
<request>
    <Index>-1</Index>
    <Phones>
        <Phone>$phone</Phone>
    </Phones>
    <Sca></Sca>
    <Content>$content</Content>
    <Length>-1</Length>
    <Reserved>1</Reserved>
    <Date>-1</Date>
</request>""".trimIndent()

            val requestBuilder = Request.Builder()
                .url("$baseUrl/api/sms/send-sms")
                .post(xmlPayload.toRequestBody("application/xml; charset=utf-8".toMediaType()))

            sessionCookie?.let { requestBuilder.header("Cookie", it) }
            verificationToken?.let { requestBuilder.header("__RequestVerificationToken", it) }

            val response = client.newCall(requestBuilder.build()).execute()
            response.header("__RequestVerificationToken")?.let { verificationToken = it }
            val body = response.body?.string() ?: ""
            if (body.contains("<response>OK</response>")) {
                Result.success(true)
            } else {
                Result.failure(Exception("Send SMS failed: ${parseXmlTag(body, "code")}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSms(ipAddress: String, index: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = sanitizeUrl(ipAddress)
            val xmlPayload = """<?xml version="1.0" encoding="UTF-8"?>
<request>
    <Index>$index</Index>
</request>""".trimIndent()

            val requestBuilder = Request.Builder()
                .url("$baseUrl/api/sms/delete-sms")
                .post(xmlPayload.toRequestBody("application/xml; charset=utf-8".toMediaType()))

            sessionCookie?.let { requestBuilder.header("Cookie", it) }
            verificationToken?.let { requestBuilder.header("__RequestVerificationToken", it) }

            val response = client.newCall(requestBuilder.build()).execute()
            response.header("__RequestVerificationToken")?.let { verificationToken = it }
            val body = response.body?.string() ?: ""
            if (body.contains("<response>OK</response>")) {
                Result.success(true)
            } else {
                Result.failure(Exception("Delete SMS failed: ${parseXmlTag(body, "code")}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAntennaType(ipAddress: String): Result<AntennaStatus> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = sanitizeUrl(ipAddress)
            val xml = makeGetRequest("$baseUrl/api/device/antenna_type")
            Log.d(TAG, "antenna_type response: $xml")

            if (xml.isEmpty() || xml.contains("<error>")) {
                val errCode = parseXmlTag(xml, "code")
                return@withContext Result.failure(Exception("Failed to get antenna type: code $errCode"))
            }

            val rawType = parseXmlTag(xml, "antennatype").toIntOrNull()
                ?: parseXmlTag(xml, "antennasettype").toIntOrNull()
            val antenna1Val = parseXmlTag(xml, "antenna1type").toIntOrNull()
            val antenna2Val = parseXmlTag(xml, "antenna2type").toIntOrNull()

            val mode = if (rawType != null) {
                AntennaMode.fromValue(rawType)
            } else if (antenna1Val != null && antenna2Val != null) {
                when {
                    antenna1Val == 0 && antenna2Val == 0 -> AntennaMode.AUTO
                    antenna1Val == 1 && (antenna2Val == 1 || antenna2Val == 0) -> AntennaMode.INTERNAL
                    antenna1Val == 2 && (antenna2Val == 2 || antenna2Val == 0) -> AntennaMode.EXTERNAL
                    (antenna1Val == 1 && antenna2Val == 2) || (antenna1Val == 2 && antenna2Val == 1) -> AntennaMode.MIXED
                    else -> AntennaMode.fromValue(antenna1Val)
                }
            } else {
                AntennaMode.AUTO
            }

            Result.success(
                AntennaStatus(
                    mode = mode,
                    antenna1Type = antenna1Val,
                    antenna2Type = antenna2Val,
                    rawXml = xml
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "getAntennaType error", e)
            Result.failure(e)
        }
    }

    suspend fun setAntennaType(ipAddress: String, mode: AntennaMode): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = sanitizeUrl(ipAddress)
            val a1 = when (mode) {
                AntennaMode.AUTO -> 0
                AntennaMode.INTERNAL -> 1
                AntennaMode.EXTERNAL -> 2
                AntennaMode.MIXED -> 1
            }
            val a2 = when (mode) {
                AntennaMode.AUTO -> 0
                AntennaMode.INTERNAL -> 0
                AntennaMode.EXTERNAL -> 2
                AntennaMode.MIXED -> 2
            }

            // Dual compatibility payload: provides both antennasettype and antenna1/2type
            val xmlPayload = """<?xml version="1.0" encoding="UTF-8"?>
<request>
    <antennasettype>${mode.value}</antennasettype>
    <antenna1type>$a1</antenna1type>
    <antenna2type>$a2</antenna2type>
</request>""".trimIndent()

            val requestBuilder = Request.Builder()
                .url("$baseUrl/api/device/antenna_set_type")
                .post(xmlPayload.toRequestBody("application/xml; charset=utf-8".toMediaType()))

            sessionCookie?.let { requestBuilder.header("Cookie", it) }
            verificationToken?.let { requestBuilder.header("__RequestVerificationToken", it) }

            val response = client.newCall(requestBuilder.build()).execute()
            response.header("__RequestVerificationToken")?.let { verificationToken = it }
            val body = response.body?.string() ?: ""
            Log.d(TAG, "antenna_set_type response: $body")

            if (body.contains("<response>OK</response>")) {
                Result.success(true)
            } else {
                // Fallback attempt: single antennasettype tag
                val fallbackPayload = """<?xml version="1.0" encoding="UTF-8"?>
<request>
    <antennasettype>${mode.value}</antennasettype>
</request>""".trimIndent()
                val fallbackReq = Request.Builder()
                    .url("$baseUrl/api/device/antenna_set_type")
                    .post(fallbackPayload.toRequestBody("application/xml; charset=utf-8".toMediaType()))
                sessionCookie?.let { fallbackReq.header("Cookie", it) }
                verificationToken?.let { fallbackReq.header("__RequestVerificationToken", it) }
                val fallbackResp = client.newCall(fallbackReq.build()).execute()
                val fallbackBody = fallbackResp.body?.string() ?: ""

                if (fallbackBody.contains("<response>OK</response>")) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("Failed to set antenna type: ${parseXmlTag(body, "code")}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "setAntennaType error", e)
            Result.failure(e)
        }
    }

    suspend fun blockDevice(ipAddress: String, macAddress: String, hostName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = sanitizeUrl(ipAddress)

            // Attempt 1: Multi-macfilter blacklist
            val cleanHostName = hostName.ifEmpty { "Blocked_Device" }
            val xmlPayload = """<?xml version="1.0" encoding="UTF-8"?>
<request>
    <policy>0</policy>
    <wifihostnames>
        <wifihostname>
            <Hostname>$cleanHostName</Hostname>
            <MacAddress>$macAddress</MacAddress>
        </wifihostname>
    </wifihostnames>
</request>""".trimIndent()

            val requestBuilder = Request.Builder()
                .url("$baseUrl/api/wlan/multi-macfilter-settings")
                .post(xmlPayload.toRequestBody("application/xml; charset=utf-8".toMediaType()))

            sessionCookie?.let { requestBuilder.header("Cookie", it) }
            verificationToken?.let { requestBuilder.header("__RequestVerificationToken", it) }

            val response = client.newCall(requestBuilder.build()).execute()
            response.header("__RequestVerificationToken")?.let { verificationToken = it }
            val body = response.body?.string() ?: ""

            if (body.contains("<response>OK</response>")) {
                return@withContext Result.success(true)
            }

            // Attempt 2: Simple mac-filter
            val simplePayload = """<?xml version="1.0" encoding="UTF-8"?>
<request>
    <filter_mac>$macAddress</filter_mac>
</request>""".trimIndent()

            val simpleReq = Request.Builder()
                .url("$baseUrl/api/wlan/mac-filter")
                .post(simplePayload.toRequestBody("application/xml; charset=utf-8".toMediaType()))
            sessionCookie?.let { simpleReq.header("Cookie", it) }
            verificationToken?.let { simpleReq.header("__RequestVerificationToken", it) }

            val simpleResp = client.newCall(simpleReq.build()).execute()
            val simpleBody = simpleResp.body?.string() ?: ""

            if (simpleBody.contains("<response>OK</response>")) {
                Result.success(true)
            } else {
                Result.failure(Exception("Router rejected block request: ${parseXmlTag(body, "code")}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "blockDevice error", e)
            Result.failure(e)
        }
    }

    suspend fun getHostList(ipAddress: String): Result<List<ConnectedDevice>> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = sanitizeUrl(ipAddress)
            val xml = makeGetRequest("$baseUrl/api/wlan/host-list")
            Log.d(TAG, "host-list response: $xml")

            val devices = mutableListOf<ConnectedDevice>()
            val hostPattern = "<Host>(.*?)</Host>".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
            val matches = hostPattern.findAll(xml)

            for (match in matches) {
                val block = match.groupValues[1]
                val hostName = parseXmlTag(block, "HostName").ifEmpty {
                    parseXmlTag(block, "hostname").ifEmpty { "Unknown Device" }
                }
                val ip = parseXmlTag(block, "IpAddress").ifEmpty {
                    parseXmlTag(block, "ipaddress")
                }
                val mac = parseXmlTag(block, "MacAddress").ifEmpty {
                    parseXmlTag(block, "macaddress")
                }
                val associateTime = parseXmlTag(block, "AssociatedTime").toLongOrNull()
                    ?: parseXmlTag(block, "AssociateTime").toLongOrNull()
                    ?: parseXmlTag(block, "associatedtime").toLongOrNull()
                    ?: parseXmlTag(block, "associatetime").toLongOrNull() ?: 0L

                devices.add(
                    ConnectedDevice(
                        hostName = hostName,
                        ipAddress = ip,
                        macAddress = mac,
                        associateTime = associateTime
                    )
                )
            }
            Result.success(devices)
        } catch (e: Exception) {
            Log.e(TAG, "getHostList error", e)
            Result.failure(e)
        }
    }
}

