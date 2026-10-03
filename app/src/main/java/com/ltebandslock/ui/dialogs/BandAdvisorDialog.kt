package com.ltebandslock.ui.dialogs

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ltebandslock.data.model.BandBenchmarkResult
import com.ltebandslock.data.model.LteBandInfo
import com.ltebandslock.ui.theme.CardBgDark
import com.ltebandslock.ui.theme.CardBgSubtle
import com.ltebandslock.ui.theme.CardBorderDark
import com.ltebandslock.ui.theme.CyanAccent
import com.ltebandslock.ui.theme.JoyConBlue
import com.ltebandslock.ui.theme.NintendoRed
import com.ltebandslock.ui.theme.SignalExcellent
import com.ltebandslock.ui.theme.SignalFair
import com.ltebandslock.ui.theme.SignalGood
import com.ltebandslock.ui.theme.Slate200
import com.ltebandslock.ui.theme.Slate400

@Composable
fun BandAdvisorDialog(
    isBenchmarking: Boolean,
    progressText: String,
    results: List<BandBenchmarkResult>,
    onStartBenchmark: () -> Unit,
    onApplyBands: (List<LteBandInfo>) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = {
            if (!isBenchmarking) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = !isBenchmarking,
            dismissOnClickOutside = !isBenchmarking,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(20.dp))
                .background(CardBgDark)
                .border(1.5.dp, CardBorderDark, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NintendoRed)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "ADVISOR",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "智慧頻段跑分評估",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (!isBenchmarking) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Slate400,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Status & Description Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CardBgSubtle)
                        .padding(12.dp)
                ) {
                    if (isBenchmarking) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = NintendoRed,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = progressText.ifEmpty { "測試頻段中，請稍候..." },
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = NintendoRed,
                                trackColor = CardBorderDark
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "正在輪流鎖定不同頻段組合並量測 SINR 與載波聚合狀態...",
                                color = Slate400,
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        Column {
                            Text(
                                text = "自動依序測試台灣主流頻段（B1, B3, B7, B8, B28 及 CA 組合），量測即時雜訊比與訊號強度，計算出當前地點最佳鎖頻方案。",
                                color = Slate200,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onStartBenchmark,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NintendoRed,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (results.isEmpty()) "開始一鍵跑分評估" else "重新測試跑分",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Results List
                Text(
                    text = "頻段品質排行 (${results.size})",
                    color = Slate400,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (results.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CardBgSubtle),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "尚未執行測試，點擊上方按鈕開始評估",
                            color = Slate400,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(results) { res ->
                            BenchmarkResultRow(
                                item = res,
                                onApply = { onApplyBands(res.bands) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (!isBenchmarking) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CardBgSubtle,
                            contentColor = Slate200
                        )
                    ) {
                        Text(
                            text = "關閉",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BenchmarkResultRow(
    item: BandBenchmarkResult,
    onApply: () -> Unit
) {
    val scoreColor = when {
        item.score >= 80 -> SignalExcellent
        item.score >= 60 -> SignalGood
        item.score >= 40 -> SignalFair
        else -> Slate400
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CardBgSubtle)
            .border(
                1.dp,
                if (item.isRecommended) NintendoRed else CardBorderDark,
                RoundedCornerShape(10.dp)
            )
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.isRecommended) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(NintendoRed)
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "首選推薦",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = item.bandName,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "RSRP: ${item.rsrp?.let { "$it dBm" } ?: "-"}",
                        color = JoyConBlue,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "SINR: ${item.sinr?.let { "$it dB" } ?: "-"}",
                        color = CyanAccent,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                    if (item.caCount > 1) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(JoyConBlue.copy(alpha = 0.2f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "${item.caCount}CA",
                                color = JoyConBlue,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${item.score} 分",
                        color = scoreColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (item.isRecommended) NintendoRed else JoyConBlue)
                        .clickable { onApply() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "套用",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
