package com.ltebandslock.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ltebandslock.data.model.SmsCount
import com.ltebandslock.data.model.SmsMessage
import com.ltebandslock.ui.theme.AppBgDark
import com.ltebandslock.ui.theme.CardBgDark
import com.ltebandslock.ui.theme.CardBgSubtle
import com.ltebandslock.ui.theme.CardBorderDark
import com.ltebandslock.ui.theme.CyanAccent
import com.ltebandslock.ui.theme.SignalFair
import com.ltebandslock.ui.theme.SignalPoor
import com.ltebandslock.ui.theme.Slate200
import com.ltebandslock.ui.theme.Slate300
import com.ltebandslock.ui.theme.Slate400
import com.ltebandslock.ui.theme.Slate700
import com.ltebandslock.ui.theme.Slate800

@Composable
fun SmsManagerDialog(
    smsMessages: List<SmsMessage>,
    smsCount: SmsCount,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onSendSms: (phone: String, content: String) -> Unit,
    onDeleteSms: (index: Long) -> Unit
) {
    val context = LocalContext.current
    var showComposeDialog by remember { mutableStateOf(false) }
    var messageToDelete by remember { mutableStateOf<SmsMessage?>(null) }

    LaunchedEffect(Unit) {
        onRefresh()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(14.dp))
                .background(CardBgDark)
                .border(1.dp, CardBorderDark, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "SMS MESSAGES",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent,
                                letterSpacing = 0.5.sp
                            )
                            if (smsCount.unread > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SignalPoor)
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "${smsCount.unread} 未讀",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        Text(
                            text = "4G 路由器 SIM 卡簡訊收發 (共 ${smsMessages.size} 則)",
                            fontSize = 11.sp,
                            color = Slate400
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = Slate200,
                                modifier = Modifier.size(16.dp)
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
                }

                // Action Bar: Compose New SMS Button
                Button(
                    onClick = { showComposeDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "發送新簡訊 (COMPOSE SMS)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Message List Area
                if (isLoading && smsMessages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = CyanAccent,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                } else if (smsMessages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = Slate700,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "收件匣無任何簡訊",
                                fontSize = 12.sp,
                                color = Slate400
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(smsMessages, key = { it.index }) { msg ->
                            SmsItemCard(
                                msg = msg,
                                onDelete = { messageToDelete = msg }
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    messageToDelete?.let { msg ->
        AlertDialog(
            onDismissRequest = { messageToDelete = null },
            title = {
                Text(
                    text = "確認刪除簡訊",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Slate200
                )
            },
            text = {
                Text(
                    text = "確定要刪除來自「${msg.phone.ifEmpty { "未知號碼" }}」的這則簡訊嗎？\n此操作無法復原。",
                    fontSize = 13.sp,
                    color = Slate300
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSms(msg.index)
                        messageToDelete = null
                    },
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SignalPoor)
                ) {
                    Text("確認刪除", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { messageToDelete = null },
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
                ) {
                    Text("取消", color = Slate400, fontSize = 12.sp)
                }
            },
            containerColor = CardBgDark,
            shape = RoundedCornerShape(12.dp)
        )
    }

    // Compose SMS Sub-Dialog
    if (showComposeDialog) {
        ComposeSmsDialog(
            onDismiss = { showComposeDialog = false },
            onSend = { phone, content ->
                onSendSms(phone, content)
                showComposeDialog = false
            }
        )
    }
}

@Composable
private fun SmsItemCard(
    msg: SmsMessage,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }

    val otpMatch = remember(msg.content) {
        // Detect 4 to 8 digit OTP verification code
        val pattern = "(?:驗證碼|code|密碼|PIN)[^0-9]*([0-9]{4,8})".toRegex(RegexOption.IGNORE_CASE)
        val match = pattern.find(msg.content)
        match?.groupValues?.get(1) ?: run {
            // General 4-6 standalone digit code
            val simplePattern = "\\b([0-9]{4,6})\\b".toRegex()
            simplePattern.find(msg.content)?.groupValues?.get(1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CardBgSubtle)
            .border(
                0.8.dp,
                if (msg.isUnread) CyanAccent.copy(alpha = 0.5f) else CardBorderDark,
                RoundedCornerShape(8.dp)
            )
            .clickable { isExpanded = !isExpanded }
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Top Row: Sender & Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (msg.isUnread) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(CyanAccent)
                        )
                    }
                    Text(
                        text = msg.phone.ifEmpty { "未知號碼" },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate200,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = msg.date,
                    fontSize = 10.sp,
                    color = Slate400,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Message Content
            Text(
                text = msg.content,
                fontSize = 12.sp,
                color = Slate200,
                lineHeight = 17.sp,
                maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis
            )

            // Action Toolbar (Always visible or when expanded)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Quick Copy OTP button if available
                    if (otpMatch != null) {
                        OutlinedButton(
                            onClick = {
                                val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cb.setPrimaryClip(ClipData.newPlainText("OTP Code", otpMatch))
                                Toast.makeText(context, "已複製驗證碼: $otpMatch", Toast.LENGTH_SHORT).show()
                            },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(24.dp),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(text = "複製驗證碼 ($otpMatch)", fontSize = 10.sp, color = CyanAccent, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Copy Full Content Button
                    OutlinedButton(
                        onClick = {
                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cb.setPrimaryClip(ClipData.newPlainText("SMS Content", msg.content))
                            Toast.makeText(context, "已複製簡訊內文", Toast.LENGTH_SHORT).show()
                        },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(24.dp),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = "複製", fontSize = 10.sp, color = Slate400)
                    }
                }

                // Delete Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = SignalPoor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ComposeSmsDialog(
    onDismiss: () -> Unit,
    onSend: (phone: String, content: String) -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }
    var messageText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CardBgDark)
                .border(1.dp, CardBorderDark, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "撰寫新簡訊 (SEND SMS)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate200
                )

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("收件人電話號碼", fontSize = 11.sp) },
                    placeholder = { Text("例如 0912345678", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor = Slate200,
                        unfocusedTextColor = Slate200
                    )
                )

                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    label = { Text("簡訊內文", fontSize = 11.sp) },
                    placeholder = { Text("輸入要傳送的訊息內容...", fontSize = 11.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor = Slate200,
                        unfocusedTextColor = Slate200
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "字數: ${messageText.length}",
                        fontSize = 10.sp,
                        color = Slate400
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(text = "取消", fontSize = 11.sp, color = Slate400)
                        }

                        Button(
                            onClick = {
                                if (phoneNumber.isNotBlank() && messageText.isNotBlank()) {
                                    onSend(phoneNumber.trim(), messageText.trim())
                                }
                            },
                            enabled = phoneNumber.isNotBlank() && messageText.isNotBlank(),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanAccent,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "傳送", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
