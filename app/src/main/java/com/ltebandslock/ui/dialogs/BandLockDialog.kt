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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.ltebandslock.ui.theme.CardBgDark
import com.ltebandslock.ui.theme.CyanAccent
import com.ltebandslock.ui.theme.Slate200
import com.ltebandslock.ui.theme.Slate400
import com.ltebandslock.ui.theme.Slate900

@Composable
fun BandLockDialog(
    currentlySelectedBands: List<LteBandInfo>,
    activeBands: String = "-",
    onDismiss: () -> Unit,
    onApplyBands: (List<LteBandInfo>) -> Unit
) {
    var selectedBands by remember { mutableStateOf(currentlySelectedBands.toSet()) }
    val isAuto = selectedBands.size == LteBands.ALL_BANDS.size || selectedBands.isEmpty()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyanAccent.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBgDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                            tint = CyanAccent
                        )
                        Text(
                            text = "4G BANDS LOCK",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate200
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Slate400
                        )
                    }
                }

                // Current Active Bands Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F243A))
                        .border(1.dp, CyanAccent.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ACTIVE IN ROUTER:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate400
                        )
                        Text(
                            text = activeBands.ifEmpty { "-" },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CyanAccent
                        )
                    }
                }

                // AUTO toggle master row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF131F2E))
                        .clickable {
                            selectedBands = if (isAuto) {
                                emptySet()
                            } else {
                                LteBands.ALL_BANDS.toSet()
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "AUTO (ALL BANDS)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = CyanAccent
                        )
                        Text(
                            text = "Enable all frequency bands automatically",
                            fontSize = 11.sp,
                            color = Slate400
                        )
                    }

                    Switch(
                        checked = isAuto,
                        onCheckedChange = { checked ->
                            selectedBands = if (checked) LteBands.ALL_BANDS.toSet() else emptySet()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Slate900,
                            checkedTrackColor = CyanAccent
                        )
                    )
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF162436))
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
                                    color = Slate200
                                )
                                Text(
                                    text = band.frequencyLabel,
                                    fontSize = 12.sp,
                                    color = Slate400
                                )
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
                                    checkedThumbColor = Slate900,
                                    checkedTrackColor = CyanAccent
                                )
                            )
                        }
                    }
                }

                // Apply Button
                Button(
                    onClick = {
                        onApplyBands(selectedBands.toList())
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                ) {
                    Text(
                        text = "APPLY BANDS LOCK",
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
