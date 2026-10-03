package com.ltebandslock.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
import kotlin.math.cos
import kotlin.math.sin

enum class MetricType(
    val title: String,
    val fullName: String,
    val unit: String,
    val description: String,
    val minVal: Float,
    val maxVal: Float,
    val ranges: List<ThresholdRange>
) {
    RSRP(
        title = "RSRP",
        fullName = "Reference Signal Received Power",
        unit = "dBm",
        description = "LTE 最核心的訊號接收功率指標。數值愈接近 0 代表基地台接收訊號愈強，低於 -105 dBm 時容易出現封包遺失與降速。",
        minVal = -130f,
        maxVal = -60f,
        ranges = listOf(
            ThresholdRange("極佳 (Excellent)", ">= -80 dBm", SignalExcellent),
            ThresholdRange("良好 (Good)", "-80 ~ -95 dBm", SignalGood),
            ThresholdRange("普通 (Fair)", "-95 ~ -105 dBm", SignalFair),
            ThresholdRange("不良 (Poor)", "< -105 dBm", SignalPoor)
        )
    ),
    RSRQ(
        title = "RSRQ",
        fullName = "Reference Signal Received Quality",
        unit = "dB",
        description = "LTE 訊號品質指標，反映信號受干擾與相鄰基地台負載情況。低於 -15 dB 代表頻道干擾嚴重。",
        minVal = -20f,
        maxVal = -3f,
        ranges = listOf(
            ThresholdRange("極佳 (Excellent)", ">= -10 dB", SignalExcellent),
            ThresholdRange("良好 (Good)", "-10 ~ -15 dB", SignalGood),
            ThresholdRange("普通 (Fair)", "-15 ~ -20 dB", SignalFair),
            ThresholdRange("不良 (Poor)", "< -20 dB", SignalPoor)
        )
    ),
    SINR(
        title = "SINR",
        fullName = "Signal to Interference plus Noise Ratio",
        unit = "dB",
        description = "訊噪干擾比，代表純淨訊號與噪聲干擾的比例。愈高愈能解調高階 QAM（例如 256-QAM），是決定下載極速的核心關鍵。",
        minVal = -10f,
        maxVal = 30f,
        ranges = listOf(
            ThresholdRange("極佳 (Excellent)", ">= 20 dB", SignalExcellent),
            ThresholdRange("良好 (Good)", "13 ~ 20 dB", SignalGood),
            ThresholdRange("普通 (Fair)", "0 ~ 13 dB", SignalFair),
            ThresholdRange("不良 (Poor)", "< 0 dB", SignalPoor)
        )
    ),
    RSSI(
        title = "RSSI",
        fullName = "Received Signal Strength Indicator",
        unit = "dBm",
        description = "頻段內全體射頻功率總合（含主訊號、熱噪聲與干擾）。數值高但 RSRP 低通常代表環境存在同頻干擾。",
        minVal = -105f,
        maxVal = -45f,
        ranges = listOf(
            ThresholdRange("極佳 (Excellent)", ">= -65 dBm", SignalExcellent),
            ThresholdRange("良好 (Good)", "-65 ~ -75 dBm", SignalGood),
            ThresholdRange("普通 (Fair)", "-75 ~ -85 dBm", SignalFair),
            ThresholdRange("不良 (Poor)", "< -85 dBm", SignalPoor)
        )
    )
}

data class ThresholdRange(
    val level: String,
    val condition: String,
    val color: Color
)

@Composable
fun SignalGaugeDialog(
    metricType: MetricType,
    currentValue: Int?,
    quality: SignalQuality,
    onDismiss: () -> Unit
) {
    val qualityColor = when (quality) {
        SignalQuality.EXCELLENT -> SignalExcellent
        SignalQuality.GOOD -> SignalGood
        SignalQuality.FAIR -> SignalFair
        SignalQuality.POOR -> SignalPoor
        SignalQuality.UNKNOWN -> SignalUnknown
    }

    // Normalized progress: 0f = worst (left), 1f = best (right)
    val normalizedProgress = if (currentValue != null) {
        ((currentValue.toFloat() - metricType.minVal) / (metricType.maxVal - metricType.minVal)).coerceIn(0f, 1f)
    } else {
        0f
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorderDark, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBgDark)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${metricType.title} 指標詳情",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate200
                        )
                        Text(
                            text = metricType.fullName,
                            fontSize = 10.sp,
                            color = Slate400
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Slate400,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Speedtest-style Gauge Dial with Needle
                SignalGaugeMeterView(
                    progress = normalizedProgress,
                    valueText = currentValue?.let { "$it" } ?: "-",
                    unit = metricType.unit,
                    qualityLabel = quality.name,
                    qualityColor = qualityColor
                )

                // Metric Description
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardBgSubtle)
                        .border(0.8.dp, CardBorderDark, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = metricType.description,
                        fontSize = 11.sp,
                        color = Slate300,
                        lineHeight = 16.sp
                    )
                }

                // Reference Threshold Scale Table
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "評等標準參考表（左差 -> 右優）",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate400
                    )

                    metricType.ranges.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(CardBgSubtle)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(item.color)
                                )
                                Text(
                                    text = item.level,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Slate200
                                )
                            }
                            Text(
                                text = item.condition,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = item.color
                            )
                        }
                    }
                }

                // Done Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanAccent,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "關閉",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SignalGaugeMeterView(
    progress: Float,
    valueText: String,
    unit: String,
    qualityLabel: String,
    qualityColor: Color,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "gaugeNeedle"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(width = 220.dp, height = 130.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Canvas(modifier = Modifier.size(220.dp, 130.dp)) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                val strokeWidth = 14.dp.toPx()
                val radius = (canvasWidth / 2f) - strokeWidth
                val centerOffset = Offset(canvasWidth / 2f, canvasHeight * 0.92f)

                val startAngle = 180f
                val sweepAngle = 180f

                // 1. Background Arc Track
                drawArc(
                    color = Color(0xFF1B2535),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // 2. Colored Multi-Segment Quality Track (Poor -> Fair -> Good -> Excellent)
                // Poor (Red, 0% ~ 25%)
                drawArc(
                    color = SignalPoor,
                    startAngle = 180f,
                    sweepAngle = sweepAngle * 0.25f,
                    useCenter = false,
                    topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )
                // Fair (Amber, 25% ~ 50%)
                drawArc(
                    color = SignalFair,
                    startAngle = 180f + (sweepAngle * 0.25f),
                    sweepAngle = sweepAngle * 0.25f,
                    useCenter = false,
                    topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )
                // Good (Cobalt Blue, 50% ~ 75%)
                drawArc(
                    color = SignalGood,
                    startAngle = 180f + (sweepAngle * 0.50f),
                    sweepAngle = sweepAngle * 0.25f,
                    useCenter = false,
                    topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )
                // Excellent (Emerald, 75% ~ 100%)
                drawArc(
                    color = SignalExcellent,
                    startAngle = 180f + (sweepAngle * 0.75f),
                    sweepAngle = sweepAngle * 0.25f,
                    useCenter = false,
                    topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )

                // 3. Dynamic Needle (Pointer)
                val needleAngle = startAngle + (sweepAngle * animatedProgress)
                val needleRad = Math.toRadians(needleAngle.toDouble())
                val needleLength = radius * 0.78f

                val needleTip = Offset(
                    x = (centerOffset.x + (needleLength * cos(needleRad))).toFloat(),
                    y = (centerOffset.y + (needleLength * sin(needleRad))).toFloat()
                )

                // Needle Line
                drawLine(
                    color = Slate200,
                    start = centerOffset,
                    end = needleTip,
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Needle Center Pivot Pin
                drawCircle(
                    color = CyanAccent,
                    radius = 6.dp.toPx(),
                    center = centerOffset
                )
                drawCircle(
                    color = CardBgDark,
                    radius = 2.5.dp.toPx(),
                    center = centerOffset
                )
            }

            // Digital Value readout below needle pivot
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = valueText,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Slate200
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = unit,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate400,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(qualityColor.copy(alpha = 0.15f))
                        .border(1.dp, qualityColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = qualityLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = qualityColor
                    )
                }
            }
        }

        // Left / Right Indicator Scale Label
        Row(
            modifier = Modifier
                .width(200.dp)
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "POOR (差)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SignalPoor)
            Text(text = "EXCELLENT (優)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SignalExcellent)
        }
    }
}
