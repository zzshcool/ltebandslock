package com.ltebandslock.ui.dialogs

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ltebandslock.data.model.LteBandInfo
import com.ltebandslock.data.model.LteBands
import com.ltebandslock.ui.theme.JoyConBlue
import com.ltebandslock.ui.theme.LocalCustomColors
import com.ltebandslock.ui.theme.NintendoRed
import com.ltebandslock.ui.theme.Slate400
import com.ltebandslock.ui.theme.Slate900

@Composable
fun BandLockDialog(
    currentlySelectedBands: List<LteBandInfo>,
    activeBands: String = "-",
    configuredBands: List<LteBandInfo> = emptyList(),
    onDismiss: () -> Unit,
    onApplyBands: (List<LteBandInfo>) -> Unit
) {
    val customColors = LocalCustomColors.current

    val activeBandNumbers = remember(activeBands) {
        activeBands.split("+")
            .mapNotNull { it.trim().removePrefix("B").removePrefix("b").toIntOrNull() }
            .toSet()
    }

    // 優先順序：
    // 1. 若 configuredBands 有具體鎖頻（小於全頻段），代表路由器內部已鎖定該頻段組合
    // 2. 若 activeBands 有具體數值（例如 B1+B3+B7+B28），解析為對應頻段清單
    // 3. 若 currentlySelectedBands < ALL_BANDS.size，使用先前選取
    // 4. 否則若 configuredBands 非空，使用 configuredBands
    // 5. 最後若 activeBands 能解析出頻段，使用 activeBands
    val initialSelection = remember(currentlySelectedBands, configuredBands, activeBands) {
        val fromActive = if (activeBandNumbers.isNotEmpty()) {
            LteBands.ALL_BANDS.filter { activeBandNumbers.contains(it.bandNumber) }
        } else emptyList()

        when {
            configuredBands.isNotEmpty() && configuredBands.size < LteBands.ALL_BANDS.size -> {
                configuredBands.toSet()
            }
            fromActive.isNotEmpty() && fromActive.size < LteBands.ALL_BANDS.size -> {
                fromActive.toSet()
            }
            currentlySelectedBands.isNotEmpty() && currentlySelectedBands.size < LteBands.ALL_BANDS.size -> {
                currentlySelectedBands.toSet()
            }
            fromActive.isNotEmpty() -> {
                fromActive.toSet()
            }
            configuredBands.isNotEmpty() -> {
                configuredBands.toSet()
            }
            else -> currentlySelectedBands.toSet()
        }
    }

    var selectedBands by remember { mutableStateOf(initialSelection) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, customColors.cardBorder, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = customColors.cardBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = NintendoRed,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "4G 頻段鎖定",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = customColors.textPrimary
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = customColors.textSecondary
                        )
                    }
                }

                // Current Active Bands in Router Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (customColors.isDark) Color(0xFF0F243A) else Color(0xFFE0F2FE))
                        .border(1.dp, JoyConBlue.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
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
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Text(
                                text = "目前路由器生效頻段",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = customColors.textSecondary
                            )
                        }
                        Text(
                            text = activeBands.ifEmpty { "-" },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = JoyConBlue
                        )
                    }
                }

                // Quick Selection & Status Toolbar (No unnecessary AUTO toggle)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "已選取: ${selectedBands.size} / ${LteBands.ALL_BANDS.size}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedBands.isEmpty()) NintendoRed else customColors.textPrimary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Quick: Select All
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (customColors.isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                            modifier = Modifier.clickable {
                                selectedBands = LteBands.ALL_BANDS.toSet()
                            }
                        ) {
                            Text(
                                text = "全選",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = customColors.textPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        // Quick: Clear
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (customColors.isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                            modifier = Modifier.clickable {
                                selectedBands = emptySet()
                            }
                        ) {
                            Text(
                                text = "清空",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = customColors.textPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        // Quick: Reset to current active bands
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = JoyConBlue.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, JoyConBlue.copy(alpha = 0.4f)),
                            modifier = Modifier.clickable {
                                selectedBands = initialSelection
                            }
                        ) {
                            Text(
                                text = "還原鎖頻",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = JoyConBlue,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Individual Band Toggles
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(LteBands.ALL_BANDS) { band ->
                        val isSelected = selectedBands.contains(band)
                        val isCurrentActive = activeBandNumbers.contains(band.bandNumber)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) {
                                        if (customColors.isDark) Color(0xFF162B44) else Color(0xFFEFF6FF)
                                    } else {
                                        customColors.cardBgSubtle
                                    }
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) JoyConBlue.copy(alpha = 0.6f) else customColors.cardBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    selectedBands = if (isSelected) {
                                        selectedBands - band
                                    } else {
                                        selectedBands + band
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = band.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isSelected) customColors.textPrimary else customColors.textSecondary
                                )
                                Text(
                                    text = band.frequencyLabel,
                                    fontSize = 12.sp,
                                    color = customColors.textSecondary
                                )
                                if (isCurrentActive) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF10B981).copy(alpha = 0.2f))
                                            .border(1.dp, Color(0xFF10B981), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "運作中",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981)
                                        )
                                    }
                                }
                            }

                            Switch(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    selectedBands = if (checked) {
                                        selectedBands + band
                                    } else {
                                        selectedBands - band
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = NintendoRed,
                                    uncheckedThumbColor = Slate400,
                                    uncheckedTrackColor = if (customColors.isDark) Slate900 else Color(0xFFCBD5E1)
                                )
                            )
                        }
                    }
                }

                // Apply Button
                Button(
                    onClick = {
                        if (selectedBands.isNotEmpty()) {
                            onApplyBands(selectedBands.toList())
                            onDismiss()
                        }
                    },
                    enabled = selectedBands.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NintendoRed,
                        disabledContainerColor = Slate400.copy(alpha = 0.3f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedBands.isEmpty()) "請至少勾選 1 個頻段" else "套用頻段鎖定 (${selectedBands.size} 頻段)",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
