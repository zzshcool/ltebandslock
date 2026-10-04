package com.ltebandslock.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ltebandslock.data.model.SignalHistoryPoint
import com.ltebandslock.data.model.SignalInfo
import com.ltebandslock.data.model.SignalQuality
import com.ltebandslock.ui.dialogs.CarrierAggregationDialog
import com.ltebandslock.ui.theme.CardBgDark
import com.ltebandslock.ui.theme.CardBgSubtle
import com.ltebandslock.ui.theme.CardBorderDark
import com.ltebandslock.ui.theme.CyanAccent
import com.ltebandslock.ui.theme.CyanGlow
import com.ltebandslock.ui.theme.JoyConBlue
import com.ltebandslock.ui.theme.NintendoRed
import com.ltebandslock.ui.theme.SignalExcellent
import com.ltebandslock.ui.theme.SignalFair
import com.ltebandslock.ui.theme.SignalGood
import com.ltebandslock.ui.theme.SignalPoor
import com.ltebandslock.ui.theme.SignalUnknown
import com.ltebandslock.ui.theme.Slate200
import com.ltebandslock.ui.theme.Slate300
import com.ltebandslock.ui.theme.Slate400
import com.ltebandslock.ui.theme.Slate700
import com.ltebandslock.ui.theme.Slate800
import com.ltebandslock.ui.theme.LocalAppStrings
import com.ltebandslock.ui.theme.LocalCustomColors

@Composable
fun SignalMeterCard(
    signalInfo: SignalInfo,
    signalHistory: List<SignalHistoryPoint> = emptyList(),
    modifier: Modifier = Modifier
) {
    var selectedMetricForGauge by remember { mutableStateOf<MetricType?>(null) }
    var showCaDialog by remember { mutableStateOf(false) }
    var showTrendChart by remember { mutableStateOf(true) }

    val colors = LocalCustomColors.current
    val strings = LocalAppStrings.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, colors.cardBorder, RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBg)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row: Section Label + Compact Bars + Primary Band & CA Badge (No Truncation)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Clean Section Label with Compact Bars
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "CELLULAR RF",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary,
                        letterSpacing = 0.5.sp
                    )

                    SignalBarsIndicator(
                        bars = signalInfo.signalBars,
                        maxBars = 5,
                        showLabel = false
                    )
                }

                // Right: Primary Band + Dedicated CA Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (signalInfo.primaryBand != "-") {
                        val cleanBw = signalInfo.bandwidth.replace(" ", "").replace("/20MHz", "")
                        Text(
                            text = if (cleanBw.isNotEmpty() && cleanBw != "-") "${signalInfo.primaryBand} ($cleanBw)" else signalInfo.primaryBand,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                            maxLines = 1
                        )
                    }

                    if (signalInfo.aggregation) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CyanAccent.copy(alpha = 0.2f))
                                .border(1.dp, CyanAccent.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                .clickable { showCaDialog = true }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = signalInfo.caLabel,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CyanGlow,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // 4 Signal Metrics Columns (Interactive - Click to open Gauge Dialog)
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
                    progress = calculateRsrpProgress(signalInfo.rsrp),
                    onClick = { selectedMetricForGauge = MetricType.RSRP }
                )
                MetricColumn(
                    modifier = Modifier.weight(1f),
                    label = "RSRQ",
                    value = signalInfo.rsrq?.toString() ?: "-",
                    unit = "dB",
                    quality = signalInfo.rsrqQuality,
                    progress = calculateRsrqProgress(signalInfo.rsrq),
                    onClick = { selectedMetricForGauge = MetricType.RSRQ }
                )
                MetricColumn(
                    modifier = Modifier.weight(1f),
                    label = "SINR",
                    value = signalInfo.sinr?.toString() ?: "-",
                    unit = "dB",
                    quality = signalInfo.sinrQuality,
                    progress = calculateSinrProgress(signalInfo.sinr),
                    onClick = { selectedMetricForGauge = MetricType.SINR }
                )
                MetricColumn(
                    modifier = Modifier.weight(1f),
                    label = "RSSI",
                    value = signalInfo.rssi?.toString() ?: "-",
                    unit = "dBm",
                    quality = signalInfo.rssiQuality,
                    progress = calculateRssiProgress(signalInfo.rssi),
                    onClick = { selectedMetricForGauge = MetricType.RSSI }
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
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { showCaDialog = true }
                        .padding(vertical = 1.dp),
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

            if (signalHistory.size >= 3) {
                HorizontalDivider(
                    color = CardBorderDark,
                    thickness = 0.8.dp
                )

                // Signal Real-time Trend Sparkline
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShowChart,
                                contentDescription = null,
                                tint = Slate400,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "SIGNAL TREND",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate400,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(JoyConBlue)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "RSRP ${signalInfo.rsrp?.let { "$it" } ?: "-"}",
                                    fontSize = 10.sp,
                                    color = JoyConBlue,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(NintendoRed)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "SINR ${signalInfo.sinr?.let { "$it" } ?: "-"}",
                                    fontSize = 10.sp,
                                    color = NintendoRed,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    SignalTrendChart(
                        history = signalHistory,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    )
                }
            }
        }
    }

    // Metric Gauge Dialog when tapped
    selectedMetricForGauge?.let { metric ->
        val value = when (metric) {
            MetricType.RSRP -> signalInfo.rsrp
            MetricType.RSRQ -> signalInfo.rsrq
            MetricType.SINR -> signalInfo.sinr
            MetricType.RSSI -> signalInfo.rssi
        }
        val quality = when (metric) {
            MetricType.RSRP -> signalInfo.rsrpQuality
            MetricType.RSRQ -> signalInfo.rsrqQuality
            MetricType.SINR -> signalInfo.sinrQuality
            MetricType.RSSI -> signalInfo.rssiQuality
        }
        SignalGaugeDialog(
            metricType = metric,
            currentValue = value,
            quality = quality,
            onDismiss = { selectedMetricForGauge = null }
        )
    }

    // Carrier Aggregation Details Dialog when tapped
    if (showCaDialog) {
        CarrierAggregationDialog(
            signalInfo = signalInfo,
            onDismiss = { showCaDialog = false }
        )
    }
}

@Composable
private fun MetricColumn(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    unit: String,
    quality: SignalQuality,
    progress: Float,
    onClick: () -> Unit
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
            .clickable(onClick = onClick)
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

@Composable
private fun SignalTrendChart(
    history: List<SignalHistoryPoint>,
    modifier: Modifier = Modifier
) {
    if (history.size < 2) return

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(CardBgSubtle)
            .border(1.dp, CardBorderDark, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        val width = size.width
        val height = size.height

        // Background reference lines
        drawLine(
            color = Slate800,
            start = Offset(0f, height * 0.25f),
            end = Offset(width, height * 0.25f),
            strokeWidth = 1f
        )
        drawLine(
            color = Slate800,
            start = Offset(0f, height * 0.5f),
            end = Offset(width, height * 0.5f),
            strokeWidth = 1f
        )
        drawLine(
            color = Slate800,
            start = Offset(0f, height * 0.75f),
            end = Offset(width, height * 0.75f),
            strokeWidth = 1f
        )

        val pointCount = history.size
        val stepX = width / (pointCount - 1).coerceAtLeast(1)

        val rsrpPath = Path()
        val sinrPath = Path()

        var lastRsrpOffset = Offset.Zero
        var lastSinrOffset = Offset.Zero

        for (i in 0 until pointCount) {
            val pt = history[i]
            val x = i * stepX

            // RSRP map: -125 (bottom) to -65 (top)
            val rsrpNorm = ((pt.rsrp - (-125f)) / ((-65f) - (-125f))).coerceIn(0f, 1f)
            val yRsrp = height - (rsrpNorm * height)

            // SINR map: -10 (bottom) to 30 (top)
            val sinrNorm = ((pt.sinr - (-10f)) / (30f - (-10f))).coerceIn(0f, 1f)
            val ySinr = height - (sinrNorm * height)

            if (i == 0) {
                rsrpPath.moveTo(x, yRsrp)
                sinrPath.moveTo(x, ySinr)
            } else {
                rsrpPath.lineTo(x, yRsrp)
                sinrPath.lineTo(x, ySinr)
            }

            if (i == pointCount - 1) {
                lastRsrpOffset = Offset(x, yRsrp)
                lastSinrOffset = Offset(x, ySinr)
            }
        }

        // Draw RSRP line (Joy-Con Blue)
        drawPath(
            path = rsrpPath,
            color = JoyConBlue.copy(alpha = 0.9f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
        drawCircle(
            color = JoyConBlue,
            radius = 3.dp.toPx(),
            center = lastRsrpOffset
        )

        // Draw SINR line (Nintendo Red)
        drawPath(
            path = sinrPath,
            color = NintendoRed.copy(alpha = 0.9f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
        drawCircle(
            color = NintendoRed,
            radius = 3.dp.toPx(),
            center = lastSinrOffset
        )
    }
}

