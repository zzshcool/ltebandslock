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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ltebandslock.data.model.RouterProfile
import com.ltebandslock.ui.theme.CardBgDark
import com.ltebandslock.ui.theme.CyanAccent
import com.ltebandslock.ui.theme.SignalExcellent
import com.ltebandslock.ui.theme.SignalPoor
import com.ltebandslock.ui.theme.Slate200
import com.ltebandslock.ui.theme.Slate400
import com.ltebandslock.ui.theme.Slate900

@Composable
fun ProfileManagerDialog(
    profiles: List<RouterProfile>,
    activeProfileId: String?,
    onDismiss: () -> Unit,
    onSelectProfile: (RouterProfile) -> Unit,
    onSaveProfile: (RouterProfile) -> Unit,
    onDeleteProfile: (String) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var profileToEdit by remember { mutableStateOf<RouterProfile?>(null) }

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
                // Header
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
                            imageVector = Icons.Default.Router,
                            contentDescription = null,
                            tint = CyanAccent
                        )
                        Text(
                            text = if (isEditing) "EDIT ROUTER PROFILE" else "ROUTER PROFILES",
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

                if (isEditing) {
                    EditProfileForm(
                        initialProfile = profileToEdit,
                        onCancel = {
                            isEditing = false
                            profileToEdit = null
                        },
                        onSave = { updatedProfile ->
                            onSaveProfile(updatedProfile)
                            isEditing = false
                            profileToEdit = null
                        }
                    )
                } else {
                    // Profile List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(profiles) { profile ->
                            val isActive = profile.id == activeProfileId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isActive) Color(0xFF1B334B) else Color(0xFF162436))
                                    .border(
                                        1.dp,
                                        if (isActive) CyanAccent else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onSelectProfile(profile) }
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = profile.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Slate200
                                        )
                                        if (isActive) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Active",
                                                tint = SignalExcellent,
                                                modifier = Modifier.height(16.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${profile.ipAddress} (${profile.username})",
                                        fontSize = 12.sp,
                                        color = Slate400
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(onClick = {
                                        profileToEdit = profile
                                        isEditing = true
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = CyanAccent
                                        )
                                    }

                                    if (profiles.size > 1) {
                                        IconButton(onClick = { onDeleteProfile(profile.id) }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = SignalPoor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Add Profile Button
                    Button(
                        onClick = {
                            profileToEdit = null
                            isEditing = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Slate900
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ADD NEW ROUTER PROFILE",
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditProfileForm(
    initialProfile: RouterProfile?,
    onCancel: () -> Unit,
    onSave: (RouterProfile) -> Unit
) {
    var name by remember { mutableStateOf(initialProfile?.name ?: "") }
    var ipAddress by remember { mutableStateOf(initialProfile?.ipAddress ?: "192.168.8.1") }
    var username by remember { mutableStateOf(initialProfile?.username ?: "admin") }
    var password by remember { mutableStateOf(initialProfile?.password ?: "") }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Profile Name (e.g. B818 Home)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyanAccent,
                unfocusedBorderColor = Slate400,
                focusedLabelColor = CyanAccent,
                unfocusedLabelColor = Slate400,
                focusedTextColor = Slate200,
                unfocusedTextColor = Slate200
            )
        )

        OutlinedTextField(
            value = ipAddress,
            onValueChange = { ipAddress = it },
            label = { Text("IP Address (e.g. 192.168.8.1)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyanAccent,
                unfocusedBorderColor = Slate400,
                focusedLabelColor = CyanAccent,
                unfocusedLabelColor = Slate400,
                focusedTextColor = Slate200,
                unfocusedTextColor = Slate200
            )
        )

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyanAccent,
                unfocusedBorderColor = Slate400,
                focusedLabelColor = CyanAccent,
                unfocusedLabelColor = Slate400,
                focusedTextColor = Slate200,
                unfocusedTextColor = Slate200
            )
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyanAccent,
                unfocusedBorderColor = Slate400,
                focusedLabelColor = CyanAccent,
                unfocusedLabelColor = Slate400,
                focusedTextColor = Slate200,
                unfocusedTextColor = Slate200
            )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancel) {
                Text("Cancel", color = Slate400)
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    val finalProfile = (initialProfile ?: RouterProfile(name = name)).copy(
                        name = name.ifEmpty { "Huawei Router" },
                        ipAddress = ipAddress.ifEmpty { "192.168.8.1" },
                        username = username.ifEmpty { "admin" },
                        password = password
                    )
                    onSave(finalProfile)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
            ) {
                Text("Save Profile", color = Slate900, fontWeight = FontWeight.Bold)
            }
        }
    }
}
