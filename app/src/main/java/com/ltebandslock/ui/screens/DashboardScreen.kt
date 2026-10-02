package com.ltebandslock.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ltebandslock.ui.MainViewModel
import com.ltebandslock.ui.components.DeviceInfoCard
import com.ltebandslock.ui.components.SignalMeterCard
import com.ltebandslock.ui.components.SpeedCard
import com.ltebandslock.ui.dialogs.BandLockDialog
import com.ltebandslock.ui.dialogs.ProfileManagerDialog
import com.ltebandslock.ui.theme.AppBgDark
import com.ltebandslock.ui.theme.CardBorderDark
import com.ltebandslock.ui.theme.CyanAccent
import com.ltebandslock.ui.theme.SignalExcellent
import com.ltebandslock.ui.theme.SignalPoor
import com.ltebandslock.ui.theme.Slate200
import com.ltebandslock.ui.theme.Slate400
import com.ltebandslock.ui.theme.Slate800

@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val profiles by viewModel.profiles.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val infoMessage by viewModel.infoMessage.collectAsState()

    val deviceInfo by viewModel.deviceInfo.collectAsState()
    val signalInfo by viewModel.signalInfo.collectAsState()
    val trafficInfo by viewModel.trafficInfo.collectAsState()
    val selectedBands by viewModel.selectedBands.collectAsState()

    var showProfileDialog by remember { mutableStateOf(false) }
    var showBandLockDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage, infoMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        infoMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = AppBgDark,
        bottomBar = {
            // Compact Docked Bottom Action Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppBgDark.copy(alpha = 0.95f))
                    .navigationBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Button(
                    onClick = { showBandLockDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanAccent,
                        contentColor = Color(0xFF0F172A)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LOCK BANDS (4G)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Unified Compact Header Bar (Fits on one line)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Title
                Text(
                    text = "LTE Lock",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate200,
                    letterSpacing = 0.3.sp
                )

                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Active Profile Chip (Clickable to switch router)
                    val routerLabel = if (deviceInfo.model.isNotEmpty() && deviceInfo.model != "Unknown Router" && deviceInfo.model != "Huawei Router") {
                        "${deviceInfo.model} (${activeProfile?.ipAddress ?: "-"})"
                    } else {
                        "${activeProfile?.name ?: "Router"} (${activeProfile?.ipAddress ?: "-"})"
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Slate800)
                            .border(0.8.dp, CardBorderDark, RoundedCornerShape(6.dp))
                            .clickable { showProfileDialog = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isLoggedIn) SignalExcellent else SignalPoor)
                            )
                            Text(
                                text = routerLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate200,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Switch Router",
                                tint = Slate400,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    // Refresh Button (Compact 28dp)
                    val infiniteTransition = rememberInfiniteTransition(label = "spin")
                    val spinAngle by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 800, easing = LinearEasing)
                        ),
                        label = "spinAngle"
                    )

                    IconButton(
                        onClick = { viewModel.login() },
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Slate800)
                            .border(0.8.dp, CardBorderDark, RoundedCornerShape(6.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = CyanAccent,
                            modifier = Modifier
                                .size(15.dp)
                                .rotate(if (isLoading) spinAngle else 0f)
                        )
                    }
                }
            }

            // 1. Primary Cellular Signal & CA Metrics Card
            SignalMeterCard(signalInfo = signalInfo)

            // 2. Real-Time Speed & Traffic Card
            SpeedCard(
                trafficInfo = trafficInfo,
                usedData = deviceInfo.usedData
            )

            // 3. Cell & Network Identity Card
            DeviceInfoCard(deviceInfo = deviceInfo)

            // Spacer to guarantee clearance
            Spacer(modifier = Modifier.height(10.dp))
        }
    }

    // Router Profiles Management Dialog
    if (showProfileDialog) {
        ProfileManagerDialog(
            profiles = profiles,
            activeProfileId = activeProfile?.id,
            onDismiss = { showProfileDialog = false },
            onSelectProfile = { profile ->
                viewModel.switchProfile(profile)
                showProfileDialog = false
            },
            onSaveProfile = { profile ->
                viewModel.saveProfile(profile)
            },
            onDeleteProfile = { profileId ->
                viewModel.deleteProfile(profileId)
            }
        )
    }

    // Band Lock Dialog
    if (showBandLockDialog) {
        BandLockDialog(
            currentlySelectedBands = selectedBands,
            activeBands = signalInfo.activeBands,
            onDismiss = { showBandLockDialog = false },
            onApplyBands = { newBands ->
                viewModel.applyBandLock(newBands)
                showBandLockDialog = false
            }
        )
    }
}
