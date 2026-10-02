package com.ltebandslock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ltebandslock.data.model.DeviceInfo
import com.ltebandslock.ui.theme.CardBgDark
import com.ltebandslock.ui.theme.CardBgSubtle
import com.ltebandslock.ui.theme.CardBorderDark
import com.ltebandslock.ui.theme.CyanGlow
import com.ltebandslock.ui.theme.SignalExcellent
import com.ltebandslock.ui.theme.Slate200
import com.ltebandslock.ui.theme.Slate400

@Composable
fun DeviceInfoCard(
    deviceInfo: DeviceInfo,
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
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header Row: Section Label + Carrier & Mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CELL & NETWORK IDENTITY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                    letterSpacing = 0.5.sp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (deviceInfo.carrier.isNotEmpty() && deviceInfo.carrier != "-") {
                        Text(
                            text = deviceInfo.carrier,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate200
                        )
                    }

                    val is4GPlus = deviceInfo.networkType.contains("+")
                    val badgeColor = if (is4GPlus) CyanGlow else SignalExcellent
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeColor.copy(alpha = 0.12f))
                            .border(1.dp, badgeColor.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.5.dp)
                    ) {
                        Text(
                            text = deviceInfo.networkType,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = badgeColor
                        )
                    }
                }
            }

            // Key-Value Grid: Row 1 (eNodeB, Cell ID, Sector)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                InfoCell(
                    modifier = Modifier.weight(1f),
                    label = "eNodeB ID",
                    value = deviceInfo.eNodeBId,
                    highlight = true
                )
                InfoCell(
                    modifier = Modifier.weight(1f),
                    label = "CELL ID",
                    value = extractCleanCellId(deviceInfo.cellId)
                )
                InfoCell(
                    modifier = Modifier.weight(1f),
                    label = "SECTOR",
                    value = extractSector(deviceInfo.cellId)
                )
            }

            // Key-Value Grid: Row 2 (WAN IP & Device Model)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                InfoCell(
                    modifier = Modifier.weight(1.3f),
                    label = "WAN IP",
                    value = deviceInfo.wanIp
                )
                InfoCell(
                    modifier = Modifier.weight(1f),
                    label = "ROUTER MODEL",
                    value = deviceInfo.model
                )
            }
        }
    }
}

@Composable
private fun InfoCell(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    highlight: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(CardBgSubtle)
            .border(0.8.dp, CardBorderDark, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 5.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Slate400
            )
            Text(
                text = value.ifEmpty { "-" },
                fontSize = 12.5.sp,
                fontWeight = if (highlight) FontWeight.Bold else FontWeight.SemiBold,
                color = if (highlight) CyanGlow else Slate200,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

private fun extractCleanCellId(fullCellText: String): String {
    if (fullCellText.isEmpty() || fullCellText == "-") return "-"
    return fullCellText.substringBefore(" ")
}

private fun extractSector(fullCellText: String): String {
    if (!fullCellText.contains("Cell:")) return "-"
    return fullCellText.substringAfter("Cell:").substringBefore(",").trim()
}
