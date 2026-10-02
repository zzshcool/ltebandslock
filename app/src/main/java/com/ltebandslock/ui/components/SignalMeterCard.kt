package com.ltebandslock.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ltebandslock.data.model.SignalInfo
import com.ltebandslock.data.model.SignalQuality
import com.ltebandslock.ui.theme.CardBgDark
import com.ltebandslock.ui.theme.CardBgSubtle
import com.ltebandslock.ui.theme.CardBorderDark
import com.ltebandslock.ui.theme.CyanAccent
import com.ltebandslock.ui.theme.CyanGlow
import com.ltebandslock.ui.theme.SignalExcellent
import com.ltebandslock.ui.theme.SignalFair
import com.ltebandslock.ui.theme.SignalGood
import com.ltebandslock.ui.theme.SignalPoor
import com.ltebandslock.ui.theme.SignalUnknown
import com.ltebandslock.ui.theme.Slate200
import com.ltebandslock.ui.theme.Slate300
import com.ltebandslock.ui.theme.Slate400

@Composable
fun SignalMeterCard(
    signalInfo: SignalInfo,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CardBorderDark, RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CardBgDark)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row: Section Label + CA Status & Band
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CELLULAR RF",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                    letterSpacing = 0.5.sp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (signalInfo.primaryBand != "-") {
                        val cleanBw = signalInfo.bandwidth.replace(" ", "").replace("/20MHz", "")
                        Text(
                            text = "${signalInfo.primaryBand} ($cleanBw)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate300
                        )
                    }

                    if (signalInfo.aggregation) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CyanAccent.copy(alpha = 0.15f))
                                .border(1.dp, CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 1.5.dp)
                        ) {
                            Text(
                                text = signalInfo.caLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanGlow,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // 4 Signal Metrics Columns (Compact & High-Density)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                MetricColumn(
                    modifier = Modifier.weight(1f),
                    label = "RSRP",
                    value = signalInfo.rsrp?.toString() ?: "-",
                    unit = "dBm",
                    quality = signalInfo.rsrpQuality,
                    progress = calculateRsrpProgress(signalInfo.rsrp)
                )
                MetricColumn(
                    modifier = Modifier.weight(1f),
                    label = "RSRQ",
                    value = signalInfo.rsrq?.toString() ?: "-",
                    unit = "dB",
                    quality = signalInfo.rsrqQuality,
                    progress = calculateRsrqProgress(signalInfo.rsrq)
                )
                MetricColumn(
                    modifier = Modifier.weight(1f),
                    label = "SINR",
                    value = signalInfo.sinr?.toString() ?: "-",
                    unit = "dB",
                    quality = signalInfo.sinrQuality,
                    progress = calculateSinrProgress(signalInfo.sinr)
                )
                MetricColumn(
                    modifier = Modifier.weight(1f),
                    label = "RSSI",
                    value = signalInfo.rssi?.toString() ?: "-",
                    unit = "dBm",
                    quality = signalInfo.rssiQuality,
                    progress = calculateRssiProgress(signalInfo.rssi)
                )
            }

            HorizontalDivider(
                color = CardBorderDark,
                thickness = 0.8.dp
            )

            // Bottom Sub-Row: Active Bands, PCI, EARFCN
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Bands:",
                        fontSize = 10.5.sp,
                        color = Slate400
                    )
                    Text(
                        text = signalInfo.activeBands,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanGlow,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (signalInfo.pci != "-") {
                        Text(
                            text = "PCI: ${signalInfo.pci}",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate300
                        )
                    }
                    if (signalInfo.earfcn != "-") {
                        val earfcnDl = signalInfo.earfcn.replace("DL:", "").substringBefore(" ")
                        Text(
                            text = "EARFCN: $earfcnDl",
                            fontSize = 10.5.sp,
                            color = Slate400
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricColumn(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    unit: String,
    quality: SignalQuality,
    progress: Float
) {
    val qualityColor = when (quality) {
        SignalQuality.EXCELLENT -> SignalExcellent
        SignalQuality.GOOD -> SignalGood
        SignalQuality.FAIR -> SignalFair
        SignalQuality.POOR -> SignalPoor
        SignalQuality.UNKNOWN -> SignalUnknown
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0.05f, 1f),
        animationSpec = tween(durationMillis = 400),
        label = "progress"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(CardBgSubtle)
            .border(0.8.dp, CardBorderDark, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400
                )
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(qualityColor)
                )
            }

            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = value,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate200,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = unit,
                    fontSize = 8.5.sp,
                    color = Slate400,
                    modifier = Modifier.padding(bottom = 1.dp)
                )
            }

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .clip(RoundedCornerShape(1.dp)),
                color = qualityColor,
                trackColor = Color(0xFF1B2433)
            )
        }
    }
}

private fun calculateRsrpProgress(rsrp: Int?): Float {
    if (rsrp == null) return 0f
    return ((rsrp - (-120)) / 50f).coerceIn(0.05f, 1f)
}

private fun calculateRsrqProgress(rsrq: Int?): Float {
    if (rsrq == null) return 0f
    return ((rsrq - (-20)) / 15f).coerceIn(0.05f, 1f)
}

private fun calculateSinrProgress(sinr: Int?): Float {
    if (sinr == null) return 0f
    return ((sinr - (-5)) / 30f).coerceIn(0.05f, 1f)
}

private fun calculateRssiProgress(rssi: Int?): Float {
    if (rssi == null) return 0f
    return ((rssi - (-100)) / 50f).coerceIn(0.05f, 1f)
}
