package com.ltebandslock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.ltebandslock.ui.theme.JoyConBlue
import com.ltebandslock.ui.theme.LocalAppStrings
import com.ltebandslock.ui.theme.LocalCustomColors
import com.ltebandslock.ui.theme.SignalExcellent

@Composable
fun DeviceInfoCard(
    deviceInfo: DeviceInfo,
    onClickMap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
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
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header Row: Section Label + Carrier & Mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.routerInfoTitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary,
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
                            color = colors.textPrimary
                        )
                    }

                    val is4GPlus = deviceInfo.networkType.contains("+")
                    val badgeColor = if (is4GPlus) JoyConBlue else SignalExcellent
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeColor.copy(alpha = 0.15f))
                            .border(1.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
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
                    highlight = true,
                    onClick = onClickMap
                )
                InfoCell(
                    modifier = Modifier.weight(1f),
                    label = "CELL ID",
                    value = extractCleanCellId(deviceInfo.cellId),
                    onClick = onClickMap
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
    highlight: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val colors = LocalCustomColors.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(colors.cardBgSubtle)
            .border(0.8.dp, colors.cardBorder, RoundedCornerShape(6.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 7.dp, vertical = 5.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary
                )
                if (onClick != null) {
                    Text(
                        text = "->",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary
                    )
                }
            }
            Text(
                text = value.ifEmpty { "-" },
                fontSize = 12.5.sp,
                fontWeight = if (highlight) FontWeight.Bold else FontWeight.SemiBold,
                color = if (highlight) JoyConBlue else colors.textPrimary,
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
