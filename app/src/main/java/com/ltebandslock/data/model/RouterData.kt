package com.ltebandslock.data.model

data class DeviceInfo(
    val model: String = "Unknown Router",
    val networkType: String = "4G",
    val carrier: String = "Unknown Carrier",
    val cellId: String = "-",
    val eNodeBId: String = "-",
    val wanIp: String = "-",
    val usedData: String = "-",
    val uptime: String = "-",
    val tac: String = "-",
    val plmn: String = "-",
    val mcc: String = "466",
    val mnc: String = "89"
)

data class SignalInfo(
    val rsrp: Int? = null,           // e.g. -92 dBm
    val rsrq: Int? = null,           // e.g. -12 dB
    val sinr: Int? = null,           // e.g. -3 dB
    val rssi: Int? = null,           // e.g. -61 dBm
    val primaryBand: String = "-",    // e.g. "B1" or "Band 1"
    val activeBands: String = "-",     // e.g. "B1+B3"
    val bandwidth: String = "-",       // e.g. "20MHz/20MHz"
    val aggregation: Boolean = false,  // Carrier Aggregation (CA)
    val caCount: Int = 1,              // 1 = single, 2 = 2CA, 3 = 3CA, 4 = 4CA
    val caLabel: String = "4G",        // e.g. "4G", "2CA (4G+)", "3CA (4G+)", "4CA (4G+)"
    val pci: String = "-",
    val earfcn: String = "-",
    val signalBars: Int = 0            // 0..5 bars
) {
    val rsrpQuality: SignalQuality get() = evaluateRsrp(rsrp)
    val rsrqQuality: SignalQuality get() = evaluateRsrq(rsrq)
    val sinrQuality: SignalQuality get() = evaluateSinr(sinr)
    val rssiQuality: SignalQuality get() = evaluateRssi(rssi)

    private fun evaluateRsrp(value: Int?): SignalQuality = when {
        value == null -> SignalQuality.UNKNOWN
        value >= -80 -> SignalQuality.EXCELLENT
        value >= -95 -> SignalQuality.GOOD
        value >= -105 -> SignalQuality.FAIR
        else -> SignalQuality.POOR
    }

    private fun evaluateRsrq(value: Int?): SignalQuality = when {
        value == null -> SignalQuality.UNKNOWN
        value >= -10 -> SignalQuality.EXCELLENT
        value >= -15 -> SignalQuality.GOOD
        value >= -20 -> SignalQuality.FAIR
        else -> SignalQuality.POOR
    }

    private fun evaluateSinr(value: Int?): SignalQuality = when {
        value == null -> SignalQuality.UNKNOWN
        value >= 20 -> SignalQuality.EXCELLENT
        value >= 13 -> SignalQuality.GOOD
        value >= 0 -> SignalQuality.FAIR
        else -> SignalQuality.POOR
    }

    private fun evaluateRssi(value: Int?): SignalQuality = when {
        value == null -> SignalQuality.UNKNOWN
        value >= -65 -> SignalQuality.EXCELLENT
        value >= -75 -> SignalQuality.GOOD
        value >= -85 -> SignalQuality.FAIR
        else -> SignalQuality.POOR
    }
}

enum class SignalQuality {
    EXCELLENT, GOOD, FAIR, POOR, UNKNOWN
}

data class TrafficInfo(
    val downloadRateBytes: Long = 0L,  // bytes per second
    val uploadRateBytes: Long = 0L,    // bytes per second
    val totalDownloadBytes: Long = 0L,
    val totalUploadBytes: Long = 0L,
    val connectTimeSeconds: Long = 0L
) {
    fun getFormattedDownloadSpeed(unit: SpeedUnit = SpeedUnit.MBPS): String =
        formatSpeed(downloadRateBytes, unit)

    fun getFormattedUploadSpeed(unit: SpeedUnit = SpeedUnit.MBPS): String =
        formatSpeed(uploadRateBytes, unit)

    val formattedDownloadSpeed: String
        get() = formatSpeed(downloadRateBytes, SpeedUnit.MBPS)

    val formattedUploadSpeed: String
        get() = formatSpeed(uploadRateBytes, SpeedUnit.MBPS)

    val formattedTotalDownload: String
        get() = formatBytes(totalDownloadBytes)

    private fun formatSpeed(bytesPerSec: Long, unit: SpeedUnit = SpeedUnit.MBPS): String {
        return if (unit == SpeedUnit.MB_S) {
            val mbPerSec = bytesPerSec / (1024.0 * 1024.0)
            val kbPerSec = bytesPerSec / 1024.0
            when {
                mbPerSec >= 1.0 -> String.format("%.2f MB/s", mbPerSec)
                kbPerSec >= 1.0 -> String.format("%.1f KB/s", kbPerSec)
                else -> "$bytesPerSec B/s"
            }
        } else {
            val bitsPerSec = bytesPerSec * 8
            when {
                bitsPerSec >= 1_000_000_000 -> String.format("%.2f Gbps", bitsPerSec / 1_000_000_000.0)
                bitsPerSec >= 1_000_000 -> String.format("%.2f Mbps", bitsPerSec / 1_000_000.0)
                bitsPerSec >= 1_000 -> String.format("%.1f Kbps", bitsPerSec / 1_000.0)
                else -> "$bitsPerSec bps"
            }
        }
    }

    private fun formatBytes(bytes: Long): String {
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        return if (gb >= 1.0) {
            String.format("%.2f GB", gb)
        } else {
            val mb = bytes / (1024.0 * 1024.0)
            String.format("%.1f MB", mb)
        }
    }
}

enum class AntennaMode(val value: Int, val title: String, val description: String) {
    AUTO(0, "自動 (Auto)", "路由器依訊號演算法自動切換內建或外接"),
    INTERNAL(1, "內建天線 (Internal)", "強制僅使用路由器內部內建天線"),
    EXTERNAL(2, "外接天線 (External)", "強制啟用後方 TS-9 外接天線接孔"),
    MIXED(3, "混合模式 (Mixed)", "同時啟用內建與外接天線混合接收");

    companion object {
        fun fromValue(value: Int?): AntennaMode = entries.find { it.value == value } ?: AUTO
    }
}

data class AntennaStatus(
    val mode: AntennaMode = AntennaMode.AUTO,
    val antenna1Type: Int? = null,
    val antenna2Type: Int? = null,
    val rawXml: String = ""
)

data class ConnectedDevice(
    val hostName: String = "Unknown Device",
    val ipAddress: String = "-",
    val macAddress: String = "-",
    val associateTime: Long = 0L
) {
    val formattedUptime: String get() {
        if (associateTime <= 0) return "剛連線"
        val hours = associateTime / 3600
        val minutes = (associateTime % 3600) / 60
        return if (hours > 0) "${hours}小時 ${minutes}分" else "${minutes}分鐘"
    }
}

data class SignalHistoryPoint(
    val timestamp: Long = System.currentTimeMillis(),
    val rsrp: Int,
    val sinr: Int,
    val rsrq: Int? = null
)

data class BandBenchmarkResult(
    val bandName: String,
    val bands: List<LteBandInfo>,
    val rsrp: Int? = null,
    val sinr: Int? = null,
    val caCount: Int = 1,
    val activeBands: String = "-",
    val score: Int = 0,
    val isRecommended: Boolean = false
)

