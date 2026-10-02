package com.ltebandslock.data.model

data class DeviceInfo(
    val model: String = "Unknown Router",
    val networkType: String = "4G",
    val carrier: String = "Unknown Carrier",
    val cellId: String = "-",
    val eNodeBId: String = "-",
    val wanIp: String = "-",
    val usedData: String = "-",
    val uptime: String = "-"
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
    val earfcn: String = "-"
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
    val formattedDownloadSpeed: String
        get() = formatSpeed(downloadRateBytes)

    val formattedUploadSpeed: String
        get() = formatSpeed(uploadRateBytes)

    val formattedTotalDownload: String
        get() = formatBytes(totalDownloadBytes)

    private fun formatSpeed(bytesPerSec: Long): String {
        val bitsPerSec = bytesPerSec * 8
        return when {
            bitsPerSec >= 1_000_000_000 -> String.format("%.2f Gbps", bitsPerSec / 1_000_000_000.0)
            bitsPerSec >= 1_000_000 -> String.format("%.2f Mbps", bitsPerSec / 1_000_000.0)
            bitsPerSec >= 1_000 -> String.format("%.1f Kbps", bitsPerSec / 1_000.0)
            else -> "$bitsPerSec bps"
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
