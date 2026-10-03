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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.window.DialogProperties
import com.ltebandslock.ui.theme.CardBgDark
import com.ltebandslock.ui.theme.CardBgSubtle
import com.ltebandslock.ui.theme.CardBorderDark
import com.ltebandslock.ui.theme.CyanAccent
import com.ltebandslock.ui.theme.SignalFair
import com.ltebandslock.ui.theme.SignalPoor
import com.ltebandslock.ui.theme.Slate200
import com.ltebandslock.ui.theme.Slate400

@Composable
fun RebootConfirmDialog(
    isRebooting: Boolean,
    rebootCountdown: Int,
    onDismiss: () -> Unit,
    onConfirmReboot: () -> Unit
) {
    Dialog(
        onDismissRequest = {
            if (!isRebooting) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = !isRebooting,
            dismissOnClickOutside = !isRebooting
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CardBgDark)
                .border(1.dp, CardBorderDark, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            if (!isRebooting) {
                // Confirmation State
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SignalFair.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = SignalFair,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "重啟路由器",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate200
                        )
                    }

                    Text(
                        text = "確定要立即重新啟動 4G 路由器嗎？\n\n重啟過程約需 60 至 90 秒，期間 Wi-Fi 連線與網際網路將暫時中斷，重啟完成後 App 將自動重新嘗試連線。",
                        fontSize = 12.sp,
                        color = Slate400,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(text = "取消", fontSize = 12.sp, color = Slate400)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = onConfirmReboot,
                            colors = ButtonDefaults.buttonColors(containerColor = SignalPoor),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "確認重啟",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            } else {
                // Rebooting In-Progress State
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyanAccent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(26.dp),
                            color = CyanAccent,
                            strokeWidth = 2.5.dp
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "路由器重啟中",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate200
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "正在重新開機，倒數 $rebootCountdown 秒",
                            fontSize = 12.sp,
                            color = CyanAccent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    val progress = ((60 - rebootCountdown).coerceAtLeast(0)) / 60f
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = CyanAccent,
                        trackColor = CardBgSubtle,
                    )

                    Text(
                        text = "設備正在初始化數據機射頻與系統服務，請保持手機開啟 Wi-Fi...",
                        fontSize = 11.sp,
                        color = Slate400,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
