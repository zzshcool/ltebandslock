package com.ltebandslock.ui.dialogs

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ltebandslock.data.model.AntennaMode
import com.ltebandslock.data.model.AntennaStatus
import com.ltebandslock.data.model.SignalInfo
import com.ltebandslock.ui.theme.AppBgDark
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
import com.ltebandslock.ui.theme.Slate200
import com.ltebandslock.ui.theme.Slate400
import com.ltebandslock.ui.theme.Slate700
import com.ltebandslock.ui.theme.Slate800
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AntennaAlignmentDialog(
    signalInfo: SignalInfo,
    antennaStatus: AntennaStatus = AntennaStatus(),
    onSelectAntennaMode: (AntennaMode) -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Orientation Compass State
    var azimuthDegrees by remember { mutableFloatStateOf(0f) }
    var hasCompassSensor by remember { mutableStateOf(true) }

    // Audio Feedback State
    var isAudioEnabled by remember { mutableStateOf(false) }

    // Peak Hold Tracking
    var peakSinr by remember { mutableStateOf<Int?>(null) }
    var peakSinrAzimuth by remember { mutableFloatStateOf(0f) }
    var peakRsrp by remember { mutableStateOf<Int?>(null) }
    var peakRsrpAzimuth by remember { mutableFloatStateOf(0f) }

    // Vibrator Helper
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    // Compass Sensor Registration
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ORIENTATION)

        if (rotationSensor == null) {
            hasCompassSensor = false
        }

        val listener = object : SensorEventListener {
            val rotationMatrix = FloatArray(9)
            val orientationAngles = FloatArray(3)

            override fun onSensorChanged(event: SensorEvent?) {
                event ?: return
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    val degrees = (Math.toDegrees(orientationAngles[0].toDouble()).toFloat() + 360f) % 360f
                    azimuthDegrees = degrees
                } else if (event.sensor.type == Sensor.TYPE_ORIENTATION) {
                    azimuthDegrees = (event.values[0] + 360f) % 360f
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        rotationSensor?.let {
            sensorManager?.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    // Update Peak Hold & Haptic
    LaunchedEffect(signalInfo.sinr, signalInfo.rsrp) {
        val curSinr = signalInfo.sinr
        if (curSinr != null) {
            if (peakSinr == null || curSinr > peakSinr!!) {
                peakSinr = curSinr
                peakSinrAzimuth = azimuthDegrees
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(40)
                    }
                } catch (e: Exception) {
                    // Ignore haptic error
                }
            }
        }

        val curRsrp = signalInfo.rsrp
        if (curRsrp != null) {
            if (peakRsrp == null || curRsrp > peakRsrp!!) {
                peakRsrp = curRsrp
                peakRsrpAzimuth = azimuthDegrees
            }
        }
    }

    // Audio Feedback Tone Loop
    LaunchedEffect(isAudioEnabled, signalInfo.sinr) {
        if (!isAudioEnabled) return@LaunchedEffect
        var toneGen: ToneGenerator? = null
        try {
            toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 65)
            while (isActive && isAudioEnabled) {
                val sinrVal = signalInfo.sinr ?: -5
                // Higher SINR -> Higher tone & faster beeps
                val toneType = when {
                    sinrVal >= 18 -> ToneGenerator.TONE_PROP_BEEP2
                    sinrVal >= 12 -> ToneGenerator.TONE_PROP_BEEP
                    sinrVal >= 5 -> ToneGenerator.TONE_PROP_ACK
                    sinrVal >= 0 -> ToneGenerator.TONE_CDMA_KEYPAD_VOLUME_KEY_LITE
                    else -> ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE
                }

                val intervalMs = when {
                    sinrVal >= 18 -> 180L
                    sinrVal >= 12 -> 300L
                    sinrVal >= 5 -> 500L
                    sinrVal >= 0 -> 800L
                    else -> 1200L
                }

                toneGen.startTone(toneType, 60)
                delay(intervalMs)
            }
        } catch (e: Exception) {
            // Ignore audio generation error
        } finally {
            try {
                toneGen?.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(14.dp))
                .background(CardBgDark)
                .border(1.dp, CardBorderDark, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ANTENNA ALIGNMENT TOOL",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "外接對數 / 定向天線校準輔助",
                            fontSize = 11.sp,
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

                // Audio Tone & Sensor Controls Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardBgSubtle)
                        .border(0.8.dp, CardBorderDark, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = if (isAudioEnabled) CyanAccent else Slate400,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "聲音輔助提示音 (訊號越佳頻率越高)",
                            fontSize = 11.sp,
                            color = if (isAudioEnabled) Slate200 else Slate400,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Switch(
                        checked = isAudioEnabled,
                        onCheckedChange = { isAudioEnabled = it },
                        modifier = Modifier.size(36.dp, 20.dp),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyanAccent,
                            checkedTrackColor = CyanAccent.copy(alpha = 0.35f),
                            uncheckedThumbColor = Slate400,
                            uncheckedTrackColor = Slate800
                        )
                    )
                }

                // Compass & Azimuth Radar View
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AppBgDark)
                        .border(1.dp, CardBorderDark, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    CompassRadarView(
                        azimuth = azimuthDegrees,
                        peakAzimuth = peakSinrAzimuth,
                        hasPeak = peakSinr != null
                    )

                    // Overlay Heading Label
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${azimuthDegrees.toInt()}°",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate200,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = getHeadingDirection(azimuthDegrees),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CyanAccent
                        )
                    }
                }

                // Primary Metrics Grid: SINR (Hero) & RSRP
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // SINR Hero Card
                    val curSinr = signalInfo.sinr
                    val sinrColor = when {
                        curSinr == null -> Slate400
                        curSinr >= 13 -> SignalExcellent
                        curSinr >= 5 -> SignalGood
                        curSinr >= 0 -> SignalFair
                        else -> SignalPoor
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CardBgSubtle)
                            .border(1.dp, sinrColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SINR (雜訊比)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate400
                                )
                                Text(
                                    text = "最關鍵",
                                    fontSize = 9.sp,
                                    color = sinrColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = curSinr?.let { if (it > 0) "+$it" else "$it" } ?: "-",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = sinrColor,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "dB",
                                    fontSize = 12.sp,
                                    color = Slate400,
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                            }
                        }
                    }

                    // RSRP Hero Card
                    val curRsrp = signalInfo.rsrp
                    val rsrpColor = when {
                        curRsrp == null -> Slate400
                        curRsrp >= -85 -> SignalExcellent
                        curRsrp >= -95 -> SignalGood
                        curRsrp >= -105 -> SignalFair
                        else -> SignalPoor
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CardBgSubtle)
                            .border(1.dp, rsrpColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "RSRP 強度",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate400
                                )
                                Text(
                                    text = "功率",
                                    fontSize = 9.sp,
                                    color = rsrpColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = curRsrp?.toString() ?: "-",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = rsrpColor,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "dBm",
                                    fontSize = 12.sp,
                                    color = Slate400,
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                            }
                        }
                    }
                }

                // Peak Hold History Card (Allows rotating back to optimal direction)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardBgSubtle)
                        .border(0.8.dp, CardBorderDark, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "最佳紀錄方位 (PEAK HOLD)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanGlow
                            )

                            Button(
                                onClick = {
                                    peakSinr = null
                                    peakRsrp = null
                                },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.height(24.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Slate700),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reset Peak",
                                    modifier = Modifier.size(11.dp),
                                    tint = Slate200
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(text = "重設", fontSize = 10.sp, color = Slate200)
                            }
                        }

                        val peakSinrText = peakSinr?.let { "${if (it > 0) "+$it" else "$it"} dB @ ${peakSinrAzimuth.toInt()}° (${getHeadingDirection(peakSinrAzimuth)})" } ?: "尚未記錄"
                        val peakRsrpText = peakRsrp?.let { "$it dBm @ ${peakRsrpAzimuth.toInt()}° (${getHeadingDirection(peakRsrpAzimuth)})" } ?: "尚未記錄"

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "最高 SINR 方位:", fontSize = 11.sp, color = Slate400)
                            Text(text = peakSinrText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SignalExcellent)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "最高 RSRP 方位:", fontSize = 11.sp, color = Slate400)
                            Text(text = peakRsrpText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyanAccent)
                        }
                    }
                }

                // Antenna Hardware Mode Selector
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBgSubtle)
                        .border(1.dp, CardBorderDark, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(NintendoRed)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "ANTENNA",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "天線硬體選擇",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = antennaStatus.mode.title,
                                color = JoyConBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 4 Antenna Mode Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AntennaMode.entries.forEach { mode ->
                                val isSelected = antennaStatus.mode == mode
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) NintendoRed else Color.Black.copy(alpha = 0.35f))
                                        .border(
                                            1.dp,
                                            if (isSelected) NintendoRed else CardBorderDark,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { onSelectAntennaMode(mode) }
                                        .padding(vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = when (mode) {
                                            AntennaMode.AUTO -> "自動"
                                            AntennaMode.INTERNAL -> "內建"
                                            AntennaMode.EXTERNAL -> "外接"
                                            AntennaMode.MIXED -> "混合"
                                        },
                                        color = if (isSelected) Color.White else Slate400,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = antennaStatus.mode.description,
                            color = Slate400,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                }

                // Alignment Guidance Notes
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "校準技巧：",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate200
                    )
                    Text(
                        text = "1. 請將手機背面平行貼合於天線背面，沿水平方向緩慢 360 度旋轉天線。\n" +
                               "2. 觀察聲音頻率或峰值記錄，當 SINR 達到最高（通常大於 15dB）時鎖定方位角。\n" +
                               "3. SINR（雜訊比）比 RSRP（強度）對上網速率與延遲更為關鍵。",
                        fontSize = 10.sp,
                        color = Slate400,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CompassRadarView(
    azimuth: Float,
    peakAzimuth: Float,
    hasPeak: Boolean
) {
    Canvas(modifier = Modifier.size(160.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f - 12.dp.toPx()

        // Outer Rings
        drawCircle(
            color = Slate800,
            radius = radius,
            center = center,
            style = Stroke(width = 1.5.dp.toPx())
        )
        drawCircle(
            color = Slate800.copy(alpha = 0.5f),
            radius = radius * 0.65f,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
        drawCircle(
            color = Slate800.copy(alpha = 0.3f),
            radius = radius * 0.35f,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )

        // Draw Peak Azimuth Indicator (Yellow/Orange Arc Pointer)
        if (hasPeak) {
            val peakRad = Math.toRadians((peakAzimuth - 90).toDouble())
            val peakX = center.x + (radius - 2.dp.toPx()) * cos(peakRad).toFloat()
            val peakY = center.y + (radius - 2.dp.toPx()) * sin(peakRad).toFloat()
            drawCircle(
                color = SignalFair,
                radius = 4.5.dp.toPx(),
                center = Offset(peakX, peakY)
            )
        }

        // Draw 12 Cardinal Ticks
        for (i in 0 until 12) {
            val angleDeg = i * 30 - 90
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val tickLen = if (i % 3 == 0) 10.dp.toPx() else 5.dp.toPx()
            val outerX = center.x + radius * cos(angleRad).toFloat()
            val outerY = center.y + radius * sin(angleRad).toFloat()
            val innerX = center.x + (radius - tickLen) * cos(angleRad).toFloat()
            val innerY = center.y + (radius - tickLen) * sin(angleRad).toFloat()

            val tickColor = if (i % 3 == 0) CyanAccent else Slate700
            drawLine(
                color = tickColor,
                start = Offset(innerX, innerY),
                end = Offset(outerX, outerY),
                strokeWidth = if (i % 3 == 0) 2.dp.toPx() else 1.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Current Heading Pointer (Cyan Arrow pointing outward)
        val curRad = Math.toRadians((azimuth - 90).toDouble())
        val headX = center.x + (radius - 6.dp.toPx()) * cos(curRad).toFloat()
        val headY = center.y + (radius - 6.dp.toPx()) * sin(curRad).toFloat()

        drawLine(
            color = CyanAccent,
            start = center,
            end = Offset(headX, headY),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round
        )

        drawCircle(
            color = CyanAccent,
            radius = 4.dp.toPx(),
            center = center
        )
    }
}

private fun getHeadingDirection(degrees: Float): String {
    val norm = (degrees % 360f + 360f) % 360f
    return when {
        norm >= 337.5 || norm < 22.5 -> "正北 (N)"
        norm < 67.5 -> "東北 (NE)"
        norm < 112.5 -> "正東 (E)"
        norm < 157.5 -> "東南 (SE)"
        norm < 202.5 -> "正南 (S)"
        norm < 247.5 -> "西南 (SW)"
        norm < 292.5 -> "正西 (W)"
        else -> "西北 (NW)"
    }
}
