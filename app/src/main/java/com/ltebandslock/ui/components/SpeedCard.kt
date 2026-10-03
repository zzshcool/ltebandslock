package com.ltebandslock.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ltebandslock.data.model.SpeedUnit
import com.ltebandslock.data.model.TrafficInfo
import com.ltebandslock.ui.dialogs.SpeedTestDialog
import com.ltebandslock.ui.theme.CyanAccent
import com.ltebandslock.ui.theme.CyanGlow
import com.ltebandslock.ui.theme.LocalAppStrings
import com.ltebandslock.ui.theme.LocalCustomColors
import com.ltebandslock.ui.theme.SignalExcellent

@Composable
fun SpeedCard(
    trafficInfo: TrafficInfo,
    usedData: String = "",
    speedUnit: SpeedUnit = SpeedUnit.MBPS,
    modifier: Modifier = Modifier
) {
    var showSpeedTestDialog by remember { mutableStateOf(false) }
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
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            // Header Row: Section Label + Total Traffic
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.networkSpeedTitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary,
                    letterSpacing = 0.5.sp
                )

                if (usedData.isNotEmpty() && usedData != "-") {
                    Text(
                        text = "Total: $usedData",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary
                    )
                }
            }

            // Download & Upload Side-by-Side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SpeedCell(
                    modifier = Modifier.weight(1f),
                    label = "DOWNLOAD",
                    speed = trafficInfo.getFormattedDownloadSpeed(speedUnit),
                    icon = Icons.Default.ArrowDownward,
                    accentColor = SignalExcellent
                )

                SpeedCell(
                    modifier = Modifier.weight(1f),
                    label = "UPLOAD",
                    speed = trafficInfo.getFormattedUploadSpeed(speedUnit),
                    icon = Icons.Default.ArrowUpward,
                    accentColor = CyanAccent
                )
            }

            // Clean Dedicated Speedtest Action Button Strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.cardBgSubtle)
                    .border(0.8.dp, CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    .clickable { showSpeedTestDialog = true }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = CyanGlow,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "RUN SPEED TEST (即時測速與歷程曲線圖)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanGlow
                    )
                }
            }
        }
    }

    if (showSpeedTestDialog) {
        SpeedTestDialog(onDismiss = { showSpeedTestDialog = false })
    }
}

@Composable
private fun SpeedCell(
    modifier: Modifier = Modifier,
    label: String,
    speed: String,
    icon: ImageVector,
    accentColor: Color
) {
    val colors = LocalCustomColors.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(colors.cardBgSubtle)
            .border(0.8.dp, colors.cardBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = label,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary
                )
            }

            Text(
                text = speed,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
