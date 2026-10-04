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
        var mask = java.math.BigInteger.ZERO
        for (band in selectedBands) {
            mask = mask.or(java.math.BigInteger.ONE.shiftLeft(band.bandNumber - 1))
        }
        return mask.toString(16).uppercase()
    }

    fun parseHexMask(hexString: String): List<LteBandInfo> {
        val clean = hexString.trim().removePrefix("0x").removePrefix("0X")
        if (clean.isEmpty() || clean == "0" || clean.equals(AUTO_MASK_HEX, ignoreCase = true)) {
            return ALL_BANDS
        }
        return try {
            val mask = java.math.BigInteger(clean, 16)
            val matched = ALL_BANDS.filter { band ->
                val bit = java.math.BigInteger.ONE.shiftLeft(band.bandNumber - 1)
                mask.and(bit) != java.math.BigInteger.ZERO
            }
            if (matched.isNotEmpty()) matched else ALL_BANDS
        } catch (e: Exception) {
            ALL_BANDS
        }
    }

    fun fromBandNames(bandNames: Collection<String>): List<LteBandInfo> {
        val cleanNumbers = bandNames.mapNotNull { name ->
            val numStr = name.trim().removePrefix("B").removePrefix("b").removePrefix("Band ").removePrefix("BAND ")
            numStr.toIntOrNull()
        }.toSet()
        val matched = ALL_BANDS.filter { cleanNumbers.contains(it.bandNumber) }
        return if (matched.isNotEmpty()) matched else ALL_BANDS
    }
}
