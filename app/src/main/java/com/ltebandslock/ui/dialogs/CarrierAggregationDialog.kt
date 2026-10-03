package com.ltebandslock.ui.dialogs

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ltebandslock.data.model.SignalInfo
import com.ltebandslock.ui.theme.CardBgDark
import com.ltebandslock.ui.theme.CardBgSubtle
import com.ltebandslock.ui.theme.CardBorderDark
import com.ltebandslock.ui.theme.CyanAccent
import com.ltebandslock.ui.theme.CyanGlow
import com.ltebandslock.ui.theme.SignalExcellent
import com.ltebandslock.ui.theme.SignalGood
import com.ltebandslock.ui.theme.Slate200
import com.ltebandslock.ui.theme.Slate300
import com.ltebandslock.ui.theme.Slate400

@Composable
fun CarrierAggregationDialog(
    signalInfo: SignalInfo,
    onDismiss: () -> Unit
) {
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "4G+ 載波聚合 (CA) 分析",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate200
                        )
                        Text(
                            text = "Carrier Aggregation Multi-Carrier Status",
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

                // Current CA Status Badge Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardBgSubtle)
                        .border(1.dp, CyanAccent.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "當前載波狀態",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate400
                            )
                            Text(
                                text = if (signalInfo.aggregation) "${signalInfo.caCount}CA (LTE-Advanced 聚合中)" else "單載波 1CA (未啟用聚合)",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (signalInfo.aggregation) CyanGlow else Slate200
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CyanAccent.copy(alpha = 0.15f))
                                .border(1.dp, CyanAccent.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = signalInfo.caLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CyanGlow
                            )
                        }
                    }
                }

                // Carrier Breakdown List
                Text(
                    text = "載波與頻段分配明細",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400
                )

                // 1. Primary Component Carrier (PCC)
                CarrierItem(
                    role = "PCC (主載波)",
                    band = signalInfo.primaryBand,
                    bw = signalInfo.bandwidth,
                    desc = "主要控制信令與雙向上傳下載載波",
                    isPcc = true
                )

                // 2. Secondary Component Carriers (SCCs)
                val candidateBands = signalInfo.activeBands.split("+").filter { it != signalInfo.primaryBand && it.isNotEmpty() }
                val caCount = signalInfo.caCount

                if (caCount >= 2) {
                    val scc1Band = candidateBands.getOrNull(0) ?: "B3"
                    CarrierItem(
                        role = "SCC 1 (輔載波 1)",
                        band = scc1Band,
                        bw = "20 MHz",
                        desc = "動態下行頻寬疊加加速通道",
                        isPcc = false
                    )
                }

                if (caCount >= 3) {
                    val scc2Band = candidateBands.getOrNull(1) ?: "B7"
                    CarrierItem(
                        role = "SCC 2 (輔載波 2)",
                        band = scc2Band,
                        bw = "20 MHz",
                        desc = "高容量下行吞吐量通道 (達到 3CA)",
                        isPcc = false
                    )
                }

                if (caCount >= 4) {
                    val scc3Band = candidateBands.getOrNull(2) ?: "B28"
                    CarrierItem(
                        role = "SCC 3 (輔載波 3)",
                        band = scc3Band,
                        bw = "10~20 MHz",
                        desc = "第四載波聚合 (4CA 旗艦速率)",
                        isPcc = false
                    )
                }

                // Notice Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardBgSubtle)
                        .padding(9.dp)
                ) {
                    Text(
                        text = "提示：LTE 載波聚合為電信基地台動態調度（待機時通常僅維持 PCC，當啟動 Speedtest 或大檔下載時，基站會瞬間拉起 SCC1/SCC2 跑滿 2CA~4CA）。",
                        fontSize = 10.5.sp,
                        color = Slate400,
                        lineHeight = 15.sp
                    )
                }

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
                    Text(text = "了解並關閉", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun CarrierItem(
    role: String,
    band: String,
    bw: String,
    desc: String,
    isPcc: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CardBgSubtle)
            .border(0.8.dp, if (isPcc) SignalExcellent.copy(alpha = 0.4f) else CardBorderDark, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isPcc) SignalExcellent else CyanGlow)
                    )
                    Text(
                        text = role,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPcc) SignalExcellent else CyanGlow
                    )
                }

                Text(
                    text = "$band ($bw)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Slate200
                )
            }

            Text(
                text = desc,
                fontSize = 10.sp,
                color = Slate400
            )
        }
    }
}
