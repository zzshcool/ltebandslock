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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ltebandslock.ui.MainViewModel
import com.ltebandslock.ui.components.DeviceInfoCard
import com.ltebandslock.ui.components.SignalBarsIndicator
import com.ltebandslock.ui.components.SignalMeterCard
import com.ltebandslock.ui.components.SpeedCard
import com.ltebandslock.ui.dialogs.AntennaAlignmentDialog
import com.ltebandslock.ui.dialogs.BandAdvisorDialog
import com.ltebandslock.ui.dialogs.BandLockDialog
import com.ltebandslock.ui.dialogs.CellInfoMapDialog
import com.ltebandslock.ui.dialogs.ConnectedDevicesDialog
import com.ltebandslock.ui.dialogs.ProfileManagerDialog
import com.ltebandslock.ui.dialogs.RebootConfirmDialog
import com.ltebandslock.ui.dialogs.SettingsDialog
import com.ltebandslock.ui.dialogs.SmsManagerDialog
import com.ltebandslock.ui.theme.CyanAccent
import com.ltebandslock.ui.theme.JoyConBlue
import com.ltebandslock.ui.theme.LocalAppStrings
import com.ltebandslock.ui.theme.LocalCustomColors
import com.ltebandslock.ui.theme.NintendoRed
import com.ltebandslock.ui.theme.SignalExcellent
import com.ltebandslock.ui.theme.SignalPoor

@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val colors = LocalCustomColors.current
    val strings = LocalAppStrings.current

    val appSettings by viewModel.appSettings.collectAsState()
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

    val antennaStatus by viewModel.antennaStatus.collectAsState()
    val connectedDevices by viewModel.connectedDevices.collectAsState()
    val signalHistory by viewModel.signalHistory.collectAsState()
    val benchmarkResults by viewModel.benchmarkResults.collectAsState()
    val isBenchmarking by viewModel.isBenchmarking.collectAsState()
    val benchmarkProgress by viewModel.benchmarkProgress.collectAsState()

    val smsMessages by viewModel.smsMessages.collectAsState()
    val smsCount by viewModel.smsCount.collectAsState()
    val isSmsLoading by viewModel.isSmsLoading.collectAsState()
    val isRebooting by viewModel.isRebooting.collectAsState()
    val rebootCountdown by viewModel.rebootCountdown.collectAsState()

    var showProfileDialog by remember { mutableStateOf(false) }
    var showBandLockDialog by remember { mutableStateOf(false) }
    var showRebootDialog by remember { mutableStateOf(false) }
    var showAntennaDialog by remember { mutableStateOf(false) }
    var showCellMapDialog by remember { mutableStateOf(false) }
    var showSmsDialog by remember { mutableStateOf(false) }
    var showAdvisorDialog by remember { mutableStateOf(false) }
    var showDevicesDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

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
        containerColor = colors.appBg,
        bottomBar = {
            // Prominent Bottom Band Lock Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.appBg.copy(alpha = 0.95f))
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Button(
                    onClick = { showBandLockDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanAccent,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.lockBandsAction,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
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
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title & Signal Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = strings.appTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.textPrimary,
                        letterSpacing = 0.5.sp
                    )

                    if (isLoggedIn) {
                        SignalBarsIndicator(
                            bars = signalInfo.signalBars,
                            maxBars = 5,
                            showLabel = false
                        )
                    }
                }

                // Action Buttons (Refresh & Settings)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.cardBgSubtle)
                            .border(1.dp, colors.cardBorder, RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = strings.refresh,
                            tint = CyanAccent,
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(if (isLoading) spinAngle else 0f)
                        )
                    }

                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.cardBgSubtle)
                            .border(1.dp, colors.cardBorder, RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = strings.settings,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Prominent Router Selector Card
            val modelName = if (deviceInfo.model.isNotEmpty() && deviceInfo.model != "Unknown Router" && deviceInfo.model != "Huawei Router") {
                deviceInfo.model
            } else {
                activeProfile?.name ?: "Huawei Router"
            }
            val ipText = activeProfile?.ipAddress ?: "192.168.8.1"

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, colors.cardBorder, RoundedCornerShape(12.dp))
                    .clickable { showProfileDialog = true },
                colors = CardDefaults.cardColors(containerColor = colors.cardBgSubtle),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CyanAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Router,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = modelName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (isLoggedIn) SignalExcellent else SignalPoor)
                                )
                                Text(
                                    text = if (isLoggedIn) "已連線 ($ipText)" else "未連線 ($ipText)",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyanAccent.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "切換裝置",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Switch",
                                tint = CyanAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Quick Tools Bar (2 rows of 3 spacious buttons)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Row 1: Antenna, Advisor, Devices
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QuickToolButton(
                        modifier = Modifier.weight(1f),
                        title = strings.toolAntenna,
                        subtitle = strings.toolAntennaSub,
                        icon = Icons.Default.Explore,
                        highlightColor = CyanAccent,
                        onClick = { showAntennaDialog = true }
                    )
                    QuickToolButton(
                        modifier = Modifier.weight(1f),
                        title = strings.toolAdvisor,
                        subtitle = strings.toolAdvisorSub,
                        icon = Icons.Default.AutoAwesome,
                        highlightColor = NintendoRed,
                        onClick = { showAdvisorDialog = true }
                    )
                    QuickToolButton(
                        modifier = Modifier.weight(1f),
                        title = strings.toolDevices,
                        subtitle = if (connectedDevices.isNotEmpty()) "${connectedDevices.size} 在線" else strings.toolDevicesSub,
                        icon = Icons.Default.Devices,
                        highlightColor = JoyConBlue,
                        onClick = { showDevicesDialog = true }
                    )
                }

                // Row 2: Cell Map, SMS, Reboot
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QuickToolButton(
                        modifier = Modifier.weight(1f),
                        title = strings.toolCellMap,
                        subtitle = strings.toolCellMapSub,
                        icon = Icons.Default.LocationOn,
                        onClick = { showCellMapDialog = true }
                    )
                    QuickToolButton(
                        modifier = Modifier.weight(1f),
                        title = strings.toolSms,
                        subtitle = if (smsCount.unread > 0) "${smsCount.unread} 未讀" else strings.toolSmsSub,
                        icon = Icons.Default.Email,
                        hasUnread = smsCount.unread > 0,
                        onClick = { showSmsDialog = true }
                    )
                    QuickToolButton(
                        modifier = Modifier.weight(1f),
                        title = strings.toolReboot,
                        subtitle = strings.toolRebootSub,
                        icon = Icons.Default.PowerSettingsNew,
                        highlightColor = SignalPoor,
                        onClick = { showRebootDialog = true }
                    )
                }
            }

            // 1. Primary Cellular Signal & CA Metrics Card
            SignalMeterCard(
                signalInfo = signalInfo,
                signalHistory = signalHistory
            )

            // 2. Real-Time Speed & Traffic Card
            SpeedCard(
                trafficInfo = trafficInfo,
                usedData = deviceInfo.usedData,
                speedUnit = appSettings.speedUnit
            )

            // 3. Cell & Network Identity Card
            DeviceInfoCard(
                deviceInfo = deviceInfo,
                onClickMap = { showCellMapDialog = true }
            )

            Spacer(modifier = Modifier.height(12.dp))
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

    // Reboot Router Confirmation & Progress Dialog
    if (showRebootDialog || isRebooting) {
        RebootConfirmDialog(
            isRebooting = isRebooting,
            rebootCountdown = rebootCountdown,
            onDismiss = { showRebootDialog = false },
            onConfirmReboot = { viewModel.rebootRouter() }
        )
    }

    // Antenna Alignment Dialog
    if (showAntennaDialog) {
        AntennaAlignmentDialog(
            signalInfo = signalInfo,
            antennaStatus = antennaStatus,
            onSelectAntennaMode = { mode -> viewModel.setAntennaMode(mode) },
            onDismiss = { showAntennaDialog = false }
        )
    }

    // Smart Band Advisor Benchmark Dialog
    if (showAdvisorDialog) {
        BandAdvisorDialog(
            isBenchmarking = isBenchmarking,
            progressText = benchmarkProgress,
            results = benchmarkResults,
            onStartBenchmark = { viewModel.runBandBenchmark() },
            onApplyBands = { bands ->
                viewModel.applyBandLock(bands)
                showAdvisorDialog = false
            },
            onDismiss = { showAdvisorDialog = false }
        )
    }

    // Connected Host Devices Dialog
    if (showDevicesDialog) {
        ConnectedDevicesDialog(
            devices = connectedDevices,
            onRefresh = { viewModel.loadConnectedDevices() },
            onBlockDevice = { mac, name -> viewModel.blockConnectedDevice(mac, name) },
            onDismiss = { showDevicesDialog = false }
        )
    }

    // Cell & eNodeB Map Lookup Dialog
    if (showCellMapDialog) {
        CellInfoMapDialog(
            deviceInfo = deviceInfo,
            signalInfo = signalInfo,
            onDismiss = { showCellMapDialog = false }
        )
    }

    // SMS Manager Dialog
    if (showSmsDialog) {
        SmsManagerDialog(
            smsMessages = smsMessages,
            smsCount = smsCount,
            isLoading = isSmsLoading,
            onDismiss = { showSmsDialog = false },
            onRefresh = { viewModel.loadSmsList() },
            onSendSms = { phone, content ->
                viewModel.sendSms(phone, content) { }
            },
            onDeleteSms = { index ->
                viewModel.deleteSms(index)
            }
        )
    }

    // Settings Preferences Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            settings = appSettings,
            onUpdateTheme = { viewModel.updateThemeMode(it) },
            onUpdateLanguage = { viewModel.updateLanguage(it) },
            onUpdateSpeedUnit = { viewModel.updateSpeedUnit(it) },
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
private fun QuickToolButton(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    hasUnread: Boolean = false,
    highlightColor: Color = CyanAccent,
    onClick: () -> Unit
) {
    val colors = LocalCustomColors.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(colors.cardBgSubtle)
            .border(
                1.dp,
                if (hasUnread) SignalPoor else colors.cardBorder,
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Box {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (hasUnread) SignalPoor else highlightColor,
                    modifier = Modifier.size(20.dp)
                )
                if (hasUnread) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(SignalPoor)
                            .align(Alignment.TopEnd)
                    )
                }
            }

            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                letterSpacing = 0.2.sp,
                maxLines = 1
            )

            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = if (hasUnread) SignalPoor else colors.textSecondary,
                maxLines = 1
            )
        }
    }
}
