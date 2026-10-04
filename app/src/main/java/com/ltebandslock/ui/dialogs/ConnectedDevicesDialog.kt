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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ltebandslock.data.model.ConnectedDevice
import com.ltebandslock.ui.theme.CyanAccent
import com.ltebandslock.ui.theme.JoyConBlue
import com.ltebandslock.ui.theme.LocalAppStrings
import com.ltebandslock.ui.theme.LocalCustomColors
import com.ltebandslock.ui.theme.NintendoRed
import com.ltebandslock.ui.theme.SignalGood
import com.ltebandslock.ui.theme.SignalPoor
import com.ltebandslock.ui.theme.Slate200
import com.ltebandslock.ui.theme.Slate400

@Composable
fun ConnectedDevicesDialog(
    devices: List<ConnectedDevice>,
    onRefresh: () -> Unit,
    onBlockDevice: (macAddress: String, hostName: String) -> Unit = { _, _ -> },
    onDismiss: () -> Unit
) {
    val colors = LocalCustomColors.current
    val strings = LocalAppStrings.current

    var deviceToBlock by remember { mutableStateOf<ConnectedDevice?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.cardBg)
                .border(1.5.dp, colors.cardBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
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
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "HOSTS",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "連線設備管理",
                            color = colors.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(JoyConBlue.copy(alpha = 0.2f))
                                .border(1.dp, JoyConBlue, CircleShape)
                                .padding(horizontal = 7.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "${devices.size} 台",
                                color = JoyConBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row {
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = strings.refresh,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = strings.close,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (devices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.cardBgSubtle),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Devices,
                                contentDescription = null,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "目前無連線中的用戶端設備",
                                color = colors.textSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(devices) { device ->
                            DeviceItemRow(
                                device = device,
                                onKickClick = { deviceToBlock = device }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Close Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.cardBorder,
                        contentColor = colors.textPrimary
                    )
                ) {
                    Text(
                        text = strings.close,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }

    // Confirmation Dialog for Kicking/Blocking Device
    deviceToBlock?.let { target ->
        AlertDialog(
            onDismissRequest = { deviceToBlock = null },
            title = {
                Text(
                    text = "確認踢出設備？",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "確定要將此設備踢下線並加入路由器黑名單阻擋連線嗎？",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "設備名稱: ${target.hostName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "MAC: ${target.macAddress}",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyanAccent
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onBlockDevice(target.macAddress, target.hostName)
                        deviceToBlock = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NintendoRed)
                ) {
                    Text(text = "確認踢出", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deviceToBlock = null }) {
                    Text(text = strings.cancel, color = colors.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun DeviceItemRow(
    device: ConnectedDevice,
    onKickClick: () -> Unit
) {
    val colors = LocalCustomColors.current

    // Parse IPv4 and IPv6 if multiple addresses present (separated by semicolon)
    val ipList = device.ipAddress.split(";").map { it.trim() }.filter { it.isNotEmpty() }
    val primaryIp = ipList.firstOrNull { !it.contains(":") } ?: ipList.firstOrNull() ?: "-"
    val secondaryIp = ipList.firstOrNull { it != primaryIp }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.cardBgSubtle)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Row 1: Status Dot + HostName + Uptime + Kick Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(SignalGood)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = device.hostName,
                        color = colors.textPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = device.formattedUptime,
                            color = Slate200,
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Kick / Block Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(NintendoRed.copy(alpha = 0.2f))
                            .border(0.8.dp, NintendoRed.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .clickable { onKickClick() }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = "踢下線",
                                tint = NintendoRed,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "踢出",
                                color = NintendoRed,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 2: IPv4 Address (Dedicated full width line to prevent IPv6 overflow)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "IP: ",
                    color = colors.textSecondary,
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = primaryIp,
                    color = CyanAccent,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (secondaryIp != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "($secondaryIp)",
                        color = colors.textSecondary,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Row 3: MAC Address
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "MAC: ",
                    color = colors.textSecondary,
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = device.macAddress,
                    color = colors.textSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.5.sp,
                    maxLines = 1
                )
            }
        }
    }
}
