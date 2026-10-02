package com.ltebandslock.data.api

import android.util.Base64
import android.util.Log
import com.ltebandslock.data.model.DeviceInfo
import com.ltebandslock.data.model.LteBandInfo
import com.ltebandslock.data.model.LteBands
import com.ltebandslock.data.model.RouterProfile
import com.ltebandslock.data.model.SignalInfo
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

            // Detect CA carrier count from active MCS channels or configured bands
            val dlMcs = parseXmlTag(xml, "dl_mcs")
            val caCount = when {
                dlMcs.contains("Carrier4") -> 4
                dlMcs.contains("Carrier3") -> 3
                dlMcs.contains("Carrier2") -> 2
                isCa && decodedBands.size >= 2 -> decodedBands.size
                isCa -> 2
                else -> 1
            }

            val caLabel = if (isCa && caCount > 1) "${caCount}CA (4G+)" else if (isCa) "4G+ CA" else "4G"

            val activeBandsStr = if (decodedBands.isNotEmpty()) {
                decodedBands.joinToString("+")
            } else {
                primaryBand
            }

            Result.success(
                SignalInfo(
                    rsrp = rsrp,
                    rsrq = rsrq,
                    sinr = sinr,
                    rssi = rssi,
                    primaryBand = primaryBand,
                    activeBands = activeBandsStr,
                    bandwidth = bandwidth,
                    aggregation = isCa,
                    caCount = caCount,
                    caLabel = caLabel,
                    pci = pci.ifEmpty { "-" },
                    earfcn = earfcn.ifEmpty { "-" }
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

            val signalXml = makeGetRequest("$baseUrl/api/device/signal")
            val fullCellId = parseXmlTag(signalXml, "cell_id")
            val pci = parseXmlTag(signalXml, "pci")

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
                    usedData = usedDataFormatted
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
        val pattern = "<$tag>(.*?)</$tag>".toRegex(RegexOption.DOT_MATCHES_ALL)
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
}
