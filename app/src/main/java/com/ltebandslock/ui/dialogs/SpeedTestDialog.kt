package com.ltebandslock.ui.dialogs

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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ltebandslock.data.speedtest.SpeedTestEngine
import com.ltebandslock.data.speedtest.SpeedTestPhase
import com.ltebandslock.ui.theme.CardBgDark
import com.ltebandslock.ui.theme.CardBgSubtle
import com.ltebandslock.ui.theme.CardBorderDark
import com.ltebandslock.ui.theme.CyanAccent
import com.ltebandslock.ui.theme.CyanGlow
import com.ltebandslock.ui.theme.SignalExcellent
import com.ltebandslock.ui.theme.SignalFair
import com.ltebandslock.ui.theme.SignalGood
import com.ltebandslock.ui.theme.SignalPoor
import com.ltebandslock.ui.theme.Slate200
import com.ltebandslock.ui.theme.Slate300
import com.ltebandslock.ui.theme.Slate400
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

@Composable
fun SpeedTestDialog(
    onDismiss: () -> Unit
) {
    val engine = remember { SpeedTestEngine() }
    val state by engine.state.collectAsState()
    val scope = rememberCoroutineScope()
    var runningJob = remember { mutableListOf<Job>() }

    DisposableEffect(Unit) {
        onDispose {
            runningJob.forEach { it.cancel() }
            engine.reset()
        }
    }

    Dialog(onDismissRequest = {
        runningJob.forEach { it.cancel() }
        onDismiss()
    }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorderDark, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBgDark)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "LTE 網速測試 (Speedtest)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate200
                        )
                        Text(
                            text = "即時下行/上行 Throughput 監測",
                            fontSize = 10.sp,
                            color = Slate400
                        )
                    }

                    IconButton(
                        onClick = {
                            runningJob.forEach { it.cancel() }
                            onDismiss()
                        },
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

                // Ping & Jitter Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LatencyBox(
                        modifier = Modifier.weight(1f),
                        label = "PING",
                        value = if (state.pingMs > 0) "${state.pingMs} ms" else "-"
                    )
                    LatencyBox(
                        modifier = Modifier.weight(1f),
                        label = "JITTER",
                        value = if (state.jitterMs > 0) "${state.jitterMs} ms" else "-"
                    )
                }

                // 1. Semi-Circular Speedometer Gauge with Needle
                val maxGaugeScale = max(100f, max(state.downloadPeakMbps, state.uploadPeakMbps) * 1.15f)
                val currentGaugeProgress = (state.currentSpeedMbps / maxGaugeScale).coerceIn(0f, 1f)

                val phaseBadgeText = when (state.phase) {
                    SpeedTestPhase.IDLE -> "READY"
                    SpeedTestPhase.PING -> "TESTING LATENCY..."
                    SpeedTestPhase.DOWNLOAD -> "TESTING DOWNLOAD..."
                    SpeedTestPhase.UPLOAD -> "TESTING UPLOAD..."
                    SpeedTestPhase.COMPLETED -> "TEST COMPLETED"
                    SpeedTestPhase.ERROR -> "TEST FAILED"
                }

                val phaseBadgeColor = when (state.phase) {
                    SpeedTestPhase.DOWNLOAD -> SignalExcellent
                    SpeedTestPhase.UPLOAD -> CyanGlow
                    SpeedTestPhase.COMPLETED -> SignalGood
                    SpeedTestPhase.ERROR -> SignalPoor
                    else -> Slate400
                }

                SpeedometerView(
                    progress = currentGaugeProgress,
                    speedValue = String.format("%.1f", state.currentSpeedMbps),
                    phaseText = phaseBadgeText,
                    badgeColor = phaseBadgeColor,
                    maxScale = maxGaugeScale.toInt()
                )

                // 2. Real-Time Curve / Line Chart (曲線圖)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "即時傳輸歷程曲線圖",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate400
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            LegendItem(label = "Download", color = SignalExcellent)
                            LegendItem(label = "Upload", color = CyanGlow)
                        }
                    }

                    SpeedLineChartView(
                        downloadPoints = state.downloadHistory,
                        uploadPoints = state.uploadHistory,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(85.dp)
                    )
                }

                // 3. Result Summary Matrix (DL / UL)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SpeedResultBox(
                        modifier = Modifier.weight(1f),
                        label = "DOWNLOAD AVG",
                        value = if (state.downloadAvgMbps > 0) String.format("%.1f Mbps", state.downloadAvgMbps) else "-",
                        accentColor = SignalExcellent
                    )
                    SpeedResultBox(
                        modifier = Modifier.weight(1f),
                        label = "UPLOAD AVG",
                        value = if (state.uploadAvgMbps > 0) String.format("%.1f Mbps", state.uploadAvgMbps) else "-",
                        accentColor = CyanGlow
                    )
                }

                // Progress Bar during test
                if (state.phase != SpeedTestPhase.IDLE && state.phase != SpeedTestPhase.COMPLETED) {
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp)),
                        color = CyanAccent,
                        trackColor = Color(0xFF1B2433)
                    )
                }

                // Action Buttons
                val isTesting = state.phase == SpeedTestPhase.PING ||
                        state.phase == SpeedTestPhase.DOWNLOAD ||
                        state.phase == SpeedTestPhase.UPLOAD

                if (isTesting) {
                    Button(
                        onClick = {
                            runningJob.forEach { it.cancel() }
                            runningJob.clear()
                            engine.reset()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SignalPoor,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("中斷測試", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                } else {
                    Button(
                        onClick = {
                            runningJob.forEach { it.cancel() }
                            runningJob.clear()
                            val job = scope.launch {
                                engine.runTest()
                            }
                            runningJob.add(job)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanAccent,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = if (state.phase == SpeedTestPhase.COMPLETED) Icons.Default.Refresh else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (state.phase == SpeedTestPhase.COMPLETED) "再次測試" else "開始測速",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeedometerView(
    progress: Float,
    speedValue: String,
    phaseText: String,
    badgeColor: Color,
    maxScale: Int
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 250),
        label = "speedNeedle"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(width = 220.dp, height = 120.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Canvas(modifier = Modifier.size(220.dp, 120.dp)) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val strokeWidth = 12.dp.toPx()
                val radius = (canvasWidth / 2f) - strokeWidth
                val centerOffset = Offset(canvasWidth / 2f, canvasHeight * 0.95f)

                val startAngle = 180f
                val sweepAngle = 180f

                // Track Background
                drawArc(
                    color = Color(0xFF182333),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Active Gradient Track up to needle
                if (animatedProgress > 0.01f) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(Color(0xFF0284C7), CyanGlow, SignalExcellent)
                        ),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle * animatedProgress,
                        useCenter = false,
                        topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                        size = Size(radius * 2f, radius * 2f),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // Needle
                val needleAngle = startAngle + (sweepAngle * animatedProgress)
                val needleRad = Math.toRadians(needleAngle.toDouble())
                val needleLength = radius * 0.8f

                val needleTip = Offset(
                    x = (centerOffset.x + (needleLength * cos(needleRad))).toFloat(),
                    y = (centerOffset.y + (needleLength * sin(needleRad))).toFloat()
                )

                drawLine(
                    color = Slate200,
                    start = centerOffset,
                    end = needleTip,
                    strokeWidth = 2.8.dp.toPx(),
                    cap = StrokeCap.Round
                )

                drawCircle(color = CyanAccent, radius = 5.dp.toPx(), center = centerOffset)
                drawCircle(color = CardBgDark, radius = 2.dp.toPx(), center = centerOffset)
            }

            // Digital value
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = speedValue,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Slate200
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Mbps",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanGlow,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeColor.copy(alpha = 0.15f))
                        .border(0.8.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 1.5.dp)
                ) {
                    Text(
                        text = phaseText,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .width(200.dp)
                .padding(top = 1.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "0", fontSize = 8.5.sp, color = Slate400)
            Text(text = "${maxScale / 2}", fontSize = 8.5.sp, color = Slate400)
            Text(text = "$maxScale Mbps", fontSize = 8.5.sp, color = Slate400)
        }
    }
}

@Composable
fun SpeedLineChartView(
    downloadPoints: List<Float>,
    uploadPoints: List<Float>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CardBgSubtle)
            .border(0.8.dp, CardBorderDark, RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(73.dp)) {
            val width = size.width
            val height = size.height

            // Calculate max value for y-axis
            val maxDl = downloadPoints.maxOrNull() ?: 10f
            val maxUl = uploadPoints.maxOrNull() ?: 10f
            val maxY = max(30f, max(maxDl, maxUl) * 1.15f)

            // Draw 3 horizontal grid reference lines
            val gridStep = height / 3f
            for (i in 1..2) {
                val y = i * gridStep
                drawLine(
                    color = Color(0xFF1E2B3E),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Draw Download Curve
            if (downloadPoints.size >= 2) {
                val dlPath = Path()
                val dlFillPath = Path()
                val stepX = width / (max(downloadPoints.size - 1, 1).toFloat())

                dlPath.moveTo(0f, height - ((downloadPoints[0] / maxY) * height))
                dlFillPath.moveTo(0f, height)
                dlFillPath.lineTo(0f, height - ((downloadPoints[0] / maxY) * height))

                for (i in 1 until downloadPoints.size) {
                    val prevX = (i - 1) * stepX
                    val prevY = height - ((downloadPoints[i - 1] / maxY) * height)
                    val currX = i * stepX
                    val currY = height - ((downloadPoints[i] / maxY) * height)

                    val midX = (prevX + currX) / 2f
                    dlPath.cubicTo(midX, prevY, midX, currY, currX, currY)
                    dlFillPath.cubicTo(midX, prevY, midX, currY, currX, currY)
                }

                dlFillPath.lineTo((downloadPoints.size - 1) * stepX, height)
                dlFillPath.close()

                // Fill gradient
                drawPath(
                    path = dlFillPath,
                    brush = Brush.verticalGradient(
                        listOf(SignalExcellent.copy(alpha = 0.25f), Color.Transparent)
                    )
                )

                // Stroke line
                drawPath(
                    path = dlPath,
                    color = SignalExcellent,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Draw Upload Curve
            if (uploadPoints.size >= 2) {
                val ulPath = Path()
                val ulFillPath = Path()
                val stepX = width / (max(uploadPoints.size - 1, 1).toFloat())

                ulPath.moveTo(0f, height - ((uploadPoints[0] / maxY) * height))
                ulFillPath.moveTo(0f, height)
                ulFillPath.lineTo(0f, height - ((uploadPoints[0] / maxY) * height))

                for (i in 1 until uploadPoints.size) {
                    val prevX = (i - 1) * stepX
                    val prevY = height - ((uploadPoints[i - 1] / maxY) * height)
                    val currX = i * stepX
                    val currY = height - ((uploadPoints[i] / maxY) * height)

                    val midX = (prevX + currX) / 2f
                    ulPath.cubicTo(midX, prevY, midX, currY, currX, currY)
                    ulFillPath.cubicTo(midX, prevY, midX, currY, currX, currY)
                }

                ulFillPath.lineTo((uploadPoints.size - 1) * stepX, height)
                ulFillPath.close()

                drawPath(
                    path = ulFillPath,
                    brush = Brush.verticalGradient(
                        listOf(CyanGlow.copy(alpha = 0.2f), Color.Transparent)
                    )
                )

                drawPath(
                    path = ulPath,
                    color = CyanGlow,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
    }
}

@Composable
private fun LatencyBox(modifier: Modifier, label: String, value: String) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(CardBgSubtle)
            .border(0.8.dp, CardBorderDark, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Slate400)
            Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate200)
        }
    }
}

@Composable
private fun SpeedResultBox(modifier: Modifier, label: String, value: String, accentColor: Color) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(CardBgSubtle)
            .border(0.8.dp, CardBorderDark, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(text = label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Slate400)
            Text(text = value, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = accentColor)
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(text = label, fontSize = 9.sp, color = Slate400)
    }
}
