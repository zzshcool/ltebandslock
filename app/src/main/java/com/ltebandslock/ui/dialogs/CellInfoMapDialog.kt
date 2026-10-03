package com.ltebandslock.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ltebandslock.data.model.DeviceInfo
import com.ltebandslock.data.model.SignalInfo
import com.ltebandslock.ui.theme.CardBgDark
import com.ltebandslock.ui.theme.CardBgSubtle
import com.ltebandslock.ui.theme.CardBorderDark
import com.ltebandslock.ui.theme.CyanAccent
import com.ltebandslock.ui.theme.CyanGlow
import com.ltebandslock.ui.theme.SignalExcellent
import com.ltebandslock.ui.theme.Slate200
import com.ltebandslock.ui.theme.Slate400

@Composable
fun CellInfoMapDialog(
    deviceInfo: DeviceInfo,
    signalInfo: SignalInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val cleanCellId = extractCleanCellId(deviceInfo.cellId)
    val sector = extractSector(deviceInfo.cellId)
    val mcc = deviceInfo.mcc.ifEmpty { "466" }
    val mnc = deviceInfo.mnc.ifEmpty { "89" }

    fun openBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "無法開啟瀏覽器: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyCellInfo() {
        val text = buildString {
            append("電信業者: ${deviceInfo.carrier} (${deviceInfo.plmn})\n")
            append("eNodeB ID: ${deviceInfo.eNodeBId}\n")
            append("Cell ID (ECI): $cleanCellId\n")
            append("Sector: $sector\n")
            append("PCI: ${signalInfo.pci}\n")
            append("TAC: ${deviceInfo.tac}\n")
            append("EARFCN: ${signalInfo.earfcn}\n")
            append("主頻段: ${signalInfo.primaryBand} (${signalInfo.bandwidth})\n")
            append("CA 聚合: ${signalInfo.caLabel} [${signalInfo.activeBands}]")
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("LTE Cell Info", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "已複製基站資訊至剪貼簿", Toast.LENGTH_SHORT).show()
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
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CELL IDENTIFIER & MAP",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "4G 實體基地台識別與地圖反查",
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

                // Operator & Mode Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardBgSubtle)
                        .border(0.8.dp, CardBorderDark, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "連線電信業者 (CARRIER)", fontSize = 10.sp, color = Slate400)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = deviceInfo.carrier,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate200
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "PLMN (MCC-MNC)", fontSize = 10.sp, color = Slate400)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$mcc-$mnc",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CyanGlow,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Detailed Cell Parameters Table
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardBgSubtle)
                        .border(0.8.dp, CardBorderDark, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "基地台詳細參數 (RF IDENTITY)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate400
                        )

                        Row(modifier = Modifier.fillMaxWidth()) {
                            ParamItem(modifier = Modifier.weight(1f), label = "eNodeB ID", value = deviceInfo.eNodeBId, highlight = true)
                            ParamItem(modifier = Modifier.weight(1f), label = "Cell ID (ECI)", value = cleanCellId)
                        }

                        Row(modifier = Modifier.fillMaxWidth()) {
                            ParamItem(modifier = Modifier.weight(1f), label = "扇區 (Sector)", value = sector)
                            ParamItem(modifier = Modifier.weight(1f), label = "實體小區 (PCI)", value = signalInfo.pci)
                        }

                        Row(modifier = Modifier.fillMaxWidth()) {
                            ParamItem(modifier = Modifier.weight(1f), label = "追蹤區 (TAC)", value = deviceInfo.tac)
                            ParamItem(modifier = Modifier.weight(1f), label = "中心頻點 (EARFCN)", value = signalInfo.earfcn)
                        }

                        Row(modifier = Modifier.fillMaxWidth()) {
                            ParamItem(modifier = Modifier.weight(1f), label = "主頻段 / 頻寬", value = "${signalInfo.primaryBand} (${signalInfo.bandwidth})")
                            ParamItem(modifier = Modifier.weight(1f), label = "載波聚合 (CA)", value = "${signalInfo.caLabel} [${signalInfo.activeBands}]")
                        }
                    }
                }

                // Map & Lookup Action Buttons
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // CellMapper Lookup Button
                    Button(
                        onClick = {
                            val url = "https://www.cellmapper.net/map?MCC=$mcc&MNC=$mnc&type=LTE"
                            openBrowser(url)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanAccent,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "在 CellMapper 查詢基站位置 (MCC=$mcc, MNC=$mnc)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // OpenCelliD Button
                        OutlinedButton(
                            onClick = { openBrowser("https://www.opencellid.org/") },
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = Slate200,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "OpenCelliD", fontSize = 11.sp, color = Slate200)
                        }

                        // Copy All Info Button
                        OutlinedButton(
                            onClick = { copyCellInfo() },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = Slate200,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(text = "複製基站資訊", fontSize = 10.5.sp, color = Slate200)
                        }
                    }
                }

                // Help Note
                Text(
                    text = "提示：點擊「在 CellMapper 查詢」會開啟地圖，並自動帶入當前電信商代碼。您可在搜尋框輸入 eNodeB ID (${deviceInfo.eNodeBId})，即可快速定位發射基地台之具體鐵塔方位與涵蓋範圍。",
                    fontSize = 10.sp,
                    color = Slate400,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ParamItem(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    highlight: Boolean = false
) {
    Column(
        modifier = modifier.padding(vertical = 2.dp)
    ) {
        Text(text = label, fontSize = 10.sp, color = Slate400)
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.SemiBold,
            color = if (highlight) CyanAccent else Slate200,
            fontFamily = FontFamily.Monospace
        )
    }
}

private fun extractCleanCellId(rawCellId: String): String {
    val clean = rawCellId.split("(").firstOrNull()?.trim() ?: rawCellId
    return clean.ifEmpty { "-" }
}

private fun extractSector(rawCellId: String): String {
    val pattern = "Cell:\\s*([0-9]+)".toRegex()
    val match = pattern.find(rawCellId)
    if (match != null) return match.groupValues[1]

    val clean = extractCleanCellId(rawCellId)
    val num = clean.toLongOrNull() ?: return "-"
    return "${num % 256}"
}
