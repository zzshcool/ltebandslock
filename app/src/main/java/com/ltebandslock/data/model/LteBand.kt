package com.ltebandslock.data.model

data class LteBandInfo(
    val bandNumber: Int,
    val frequency: String,
    val bitMask: Long,
    val isTdd: Boolean = false
) {
    val name: String get() = "BAND $bandNumber"
    val frequencyLabel: String get() = "($frequency)"
}

object LteBands {
    val ALL_BANDS = listOf(
        LteBandInfo(1, "2100MHz", 1L shl 0),
        LteBandInfo(3, "1800MHz", 1L shl 2),
        LteBandInfo(7, "2600MHz", 1L shl 6),
        LteBandInfo(8, "900MHz", 1L shl 7),
        LteBandInfo(20, "800MHz", 1L shl 19),
        LteBandInfo(28, "700MHz", 1L shl 27),
        LteBandInfo(32, "1500MHz", 1L shl 31),
        LteBandInfo(38, "2600MHz TDD", 1L shl 37, isTdd = true),
        LteBandInfo(40, "2300MHz TDD", 1L shl 39, isTdd = true),
        LteBandInfo(41, "2500MHz TDD", 1L shl 40, isTdd = true),
        LteBandInfo(42, "3500MHz TDD", 1L shl 41, isTdd = true),
        LteBandInfo(43, "3700MHz TDD", 1L shl 42, isTdd = true)
    )

    const val AUTO_MASK_HEX = "7FFFFFFFFFFFFFFF"

    fun calculateHexMask(selectedBands: List<LteBandInfo>): String {
        if (selectedBands.isEmpty() || selectedBands.size == ALL_BANDS.size) {
            return AUTO_MASK_HEX
        }
        var mask = 0L
        for (band in selectedBands) {
            mask = mask or band.bitMask
        }
        return mask.toString(16).uppercase()
    }

    fun parseHexMask(hexString: String): List<LteBandInfo> {
        if (hexString.equals(AUTO_MASK_HEX, ignoreCase = true) || hexString.isEmpty() || hexString == "0") {
            return ALL_BANDS
        }
        return try {
            val mask = hexString.toLong(16)
            ALL_BANDS.filter { (it.bitMask and mask) != 0L }
        } catch (e: Exception) {
            ALL_BANDS
        }
    }
}
