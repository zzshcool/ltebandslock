package com.ltebandslock.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ltebandslock.data.api.HuaweiRouterApi
import com.ltebandslock.data.model.AntennaMode
import com.ltebandslock.data.model.AntennaStatus
import com.ltebandslock.data.model.BandBenchmarkResult
import com.ltebandslock.data.model.ConnectedDevice
import com.ltebandslock.data.model.DeviceInfo
import com.ltebandslock.data.model.LteBandInfo
import com.ltebandslock.data.model.LteBands
import com.ltebandslock.data.model.RouterProfile
import com.ltebandslock.data.model.SignalHistoryPoint
import com.ltebandslock.data.model.SignalInfo
import com.ltebandslock.data.model.SmsCount
import com.ltebandslock.data.model.SmsMessage
import com.ltebandslock.data.model.TrafficInfo
import com.ltebandslock.data.model.AppLanguage
import com.ltebandslock.data.model.AppSettings
import com.ltebandslock.data.model.AppThemeMode
import com.ltebandslock.data.model.SpeedUnit
import com.ltebandslock.data.preferences.SettingsManager
import com.ltebandslock.data.repository.ProfileRepository
import com.ltebandslock.ui.widget.LteWidgetProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProfileRepository(application)
    private val api = HuaweiRouterApi()
    private val settingsManager = SettingsManager(application)

    val appSettings: StateFlow<AppSettings> = settingsManager.settings

    fun updateThemeMode(theme: AppThemeMode) {
        settingsManager.setThemeMode(theme)
    }

    fun updateLanguage(lang: AppLanguage) {
        settingsManager.setLanguage(lang)
    }

    fun updateSpeedUnit(unit: SpeedUnit) {
        settingsManager.setSpeedUnit(unit)
    }

    private val _profiles = MutableStateFlow<List<RouterProfile>>(emptyList())
    val profiles: StateFlow<List<RouterProfile>> = _profiles.asStateFlow()

    private val _activeProfile = MutableStateFlow<RouterProfile?>(null)
    val activeProfile: StateFlow<RouterProfile?> = _activeProfile.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _infoMessage = MutableStateFlow<String?>(null)
    val infoMessage: StateFlow<String?> = _infoMessage.asStateFlow()

    private val _deviceInfo = MutableStateFlow(DeviceInfo())
    val deviceInfo: StateFlow<DeviceInfo> = _deviceInfo.asStateFlow()

    private val _signalInfo = MutableStateFlow(SignalInfo())
    val signalInfo: StateFlow<SignalInfo> = _signalInfo.asStateFlow()

    private val _trafficInfo = MutableStateFlow(TrafficInfo())
    val trafficInfo: StateFlow<TrafficInfo> = _trafficInfo.asStateFlow()

    private val _selectedBands = MutableStateFlow<List<LteBandInfo>>(LteBands.ALL_BANDS)
    val selectedBands: StateFlow<List<LteBandInfo>> = _selectedBands.asStateFlow()

    private val _antennaStatus = MutableStateFlow(AntennaStatus())
    val antennaStatus: StateFlow<AntennaStatus> = _antennaStatus.asStateFlow()

    private val _connectedDevices = MutableStateFlow<List<ConnectedDevice>>(emptyList())
    val connectedDevices: StateFlow<List<ConnectedDevice>> = _connectedDevices.asStateFlow()

    private val _signalHistory = MutableStateFlow<List<SignalHistoryPoint>>(emptyList())
    val signalHistory: StateFlow<List<SignalHistoryPoint>> = _signalHistory.asStateFlow()

    private val _benchmarkResults = MutableStateFlow<List<BandBenchmarkResult>>(emptyList())
    val benchmarkResults: StateFlow<List<BandBenchmarkResult>> = _benchmarkResults.asStateFlow()

    private val _isBenchmarking = MutableStateFlow(false)
    val isBenchmarking: StateFlow<Boolean> = _isBenchmarking.asStateFlow()

    private val _benchmarkProgress = MutableStateFlow("")
    val benchmarkProgress: StateFlow<String> = _benchmarkProgress.asStateFlow()

    private val _smsCount = MutableStateFlow(SmsCount())
    val smsCount: StateFlow<SmsCount> = _smsCount.asStateFlow()

    private val _smsMessages = MutableStateFlow<List<SmsMessage>>(emptyList())
    val smsMessages: StateFlow<List<SmsMessage>> = _smsMessages.asStateFlow()

    private val _isSmsLoading = MutableStateFlow(false)
    val isSmsLoading: StateFlow<Boolean> = _isSmsLoading.asStateFlow()

    private val _isRebooting = MutableStateFlow(false)
    val isRebooting: StateFlow<Boolean> = _isRebooting.asStateFlow()

    private val _rebootCountdown = MutableStateFlow(0)
    val rebootCountdown: StateFlow<Int> = _rebootCountdown.asStateFlow()

    private var pollingJob: Job? = null

    init {
        viewModelScope.launch {
            repository.profilesFlow.collect { profileList ->
                _profiles.value = profileList
                val activeId = repository.activeProfileIdFlow.first()
                val currentActive = profileList.find { it.id == activeId } ?: profileList.firstOrNull()
                if (currentActive != _activeProfile.value) {
                    _activeProfile.value = currentActive
                    currentActive?.let { autoLogin(it) }
                }
            }
        }
    }

    fun login() {
        val profile = _activeProfile.value ?: return
        autoLogin(profile)
    }

    private var isInitialBandsSynced = false

    private fun autoLogin(profile: RouterProfile) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            isInitialBandsSynced = false
            stopPolling()

            val loginResult = api.login(profile)
            if (loginResult.isSuccess) {
                _isLoggedIn.value = true
                _infoMessage.value = "Connected to ${profile.name}"
                fetchInitialData(profile.ipAddress)
                startPolling(profile.ipAddress)
            } else {
                _isLoggedIn.value = false
                _errorMessage.value = loginResult.exceptionOrNull()?.localizedMessage ?: "Failed to connect to ${profile.ipAddress}"
            }
            _isLoading.value = false
        }
    }

    private suspend fun fetchInitialData(ipAddress: String) {
        val devRes = api.getDeviceInfo(ipAddress)
        if (devRes.isSuccess) {
            _deviceInfo.value = devRes.getOrThrow()
        }

        val sigRes = api.getSignalInfo(ipAddress)
        if (sigRes.isSuccess) {
            val sig = sigRes.getOrThrow()
            _signalInfo.value = sig
            recordSignalHistory(sig)
            syncRouterBandsIfInitial(sig)
        }

        val trafRes = api.getTrafficInfo(ipAddress)
        if (trafRes.isSuccess) {
            _trafficInfo.value = trafRes.getOrThrow()
        }

        loadAntennaStatus()
        loadConnectedDevices()
        updateWidget()
    }

    private fun syncRouterBandsIfInitial(sig: SignalInfo) {
        if (!isInitialBandsSynced) {
            val targetBands = if (sig.configuredBands.isNotEmpty() && sig.configuredBands.size < LteBands.ALL_BANDS.size) {
                sig.configuredBands
            } else if (sig.activeBands.isNotEmpty() && sig.activeBands != "-") {
                LteBands.fromBandNames(sig.activeBands.split("+"))
            } else if (sig.configuredBands.isNotEmpty()) {
                sig.configuredBands
            } else {
                null
            }
            if (targetBands != null && targetBands.isNotEmpty()) {
                _selectedBands.value = targetBands
                isInitialBandsSynced = true
            }
        }
    }

    private fun startPolling(ipAddress: String) {
        stopPolling()
        pollingJob = viewModelScope.launch {
            var cycleCount = 0
            while (isActive) {
                delay(2000) // Poll every 2 seconds
                if (_isLoggedIn.value && !_isBenchmarking.value) {
                    val sigRes = api.getSignalInfo(ipAddress)
                    if (sigRes.isSuccess) {
                        val sig = sigRes.getOrThrow()
                        _signalInfo.value = sig
                        recordSignalHistory(sig)
                        syncRouterBandsIfInitial(sig)
                    }

                    val trafRes = api.getTrafficInfo(ipAddress)
                    if (trafRes.isSuccess) {
                        _trafficInfo.value = trafRes.getOrThrow()
                    }

                    val devRes = api.getDeviceInfo(ipAddress)
                    if (devRes.isSuccess) {
                        _deviceInfo.value = devRes.getOrThrow()
                    }

                    cycleCount++
                    if (cycleCount % 5 == 0) {
                        loadAntennaStatus()
                        loadConnectedDevices()
                    }

                    updateWidget()
                }
            }
        }
    }

    private fun recordSignalHistory(sig: SignalInfo) {
        val rsrp = sig.rsrp ?: return
        val sinr = sig.sinr ?: return
        val current = _signalHistory.value.toMutableList()
        current.add(SignalHistoryPoint(rsrp = rsrp, sinr = sinr, rsrq = sig.rsrq))
        if (current.size > 50) {
            current.removeAt(0)
        }
        _signalHistory.value = current
    }

    private fun updateWidget() {
        try {
            LteWidgetProvider.updateAllWidgets(
                getApplication(),
                _signalInfo.value,
                _deviceInfo.value,
                _trafficInfo.value
            )
        } catch (e: Exception) {
            // Widget update safeguard
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun loadAntennaStatus() {
        val profile = _activeProfile.value ?: return
        viewModelScope.launch {
            val res = api.getAntennaType(profile.ipAddress)
            if (res.isSuccess) {
                _antennaStatus.value = res.getOrThrow()
            }
        }
    }

    fun setAntennaMode(mode: AntennaMode) {
        val profile = _activeProfile.value ?: return
        // Optimistic UI state update: immediate tactile response
        _antennaStatus.value = _antennaStatus.value.copy(mode = mode)
        viewModelScope.launch {
            val res = api.setAntennaType(profile.ipAddress, mode)
            if (res.isSuccess) {
                _infoMessage.value = "天線已切換為: ${mode.title}"
                loadAntennaStatus()
                val sigRes = api.getSignalInfo(profile.ipAddress)
                if (sigRes.isSuccess) {
                    _signalInfo.value = sigRes.getOrThrow()
                }
            } else {
                _errorMessage.value = res.exceptionOrNull()?.localizedMessage ?: "天線設定失敗"
                // Rollback status on failure
                loadAntennaStatus()
            }
        }
    }

    fun loadConnectedDevices() {
        val profile = _activeProfile.value ?: return
        viewModelScope.launch {
            val res = api.getHostList(profile.ipAddress)
            if (res.isSuccess) {
                _connectedDevices.value = res.getOrThrow()
            }
        }
    }

    fun blockConnectedDevice(macAddress: String, hostName: String) {
        val profile = _activeProfile.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val res = api.blockDevice(profile.ipAddress, macAddress, hostName)
            if (res.isSuccess) {
                _infoMessage.value = "已將設備 $hostName 踢出下線"
                _connectedDevices.value = _connectedDevices.value.filter { it.macAddress != macAddress }
            } else {
                _errorMessage.value = res.exceptionOrNull()?.localizedMessage ?: "踢出設備失敗"
            }
            _isLoading.value = false
        }
    }

    fun runBandBenchmark(onFinished: (List<BandBenchmarkResult>) -> Unit = {}) {
        val profile = _activeProfile.value ?: return
        if (_isBenchmarking.value) return

        viewModelScope.launch {
            _isBenchmarking.value = true
            _benchmarkResults.value = emptyList()
            _infoMessage.value = "開始智慧頻段跑分測試..."

            val candidates = listOf(
                Pair("全頻聚合 (Auto CA)", LteBands.ALL_BANDS),
                Pair("4CA 四頻聚合 (B1+B3+B7+B8)", LteBands.ALL_BANDS.filter { it.bandNumber in listOf(1, 3, 7, 8) }),
                Pair("4CA 四頻聚合 (B1+B3+B7+B28)", LteBands.ALL_BANDS.filter { it.bandNumber in listOf(1, 3, 7, 28) }),
                Pair("B1+B3+B7 三頻聚合", LteBands.ALL_BANDS.filter { it.bandNumber == 1 || it.bandNumber == 3 || it.bandNumber == 7 }),
                Pair("B3+B7 雙頻聚合", LteBands.ALL_BANDS.filter { it.bandNumber == 3 || it.bandNumber == 7 }),
                Pair("單頻 Band 3 (1800M)", LteBands.ALL_BANDS.filter { it.bandNumber == 3 }),
                Pair("單頻 Band 7 (2600M)", LteBands.ALL_BANDS.filter { it.bandNumber == 7 }),
                Pair("單頻 Band 1 (2100M)", LteBands.ALL_BANDS.filter { it.bandNumber == 1 }),
                Pair("單頻 Band 8 (900M)", LteBands.ALL_BANDS.filter { it.bandNumber == 8 }),
                Pair("單頻 Band 28 (700M)", LteBands.ALL_BANDS.filter { it.bandNumber == 28 })
            ).filter { it.second.isNotEmpty() }

            val results = mutableListOf<BandBenchmarkResult>()
            val originalBands = _selectedBands.value

            for ((index, candidate) in candidates.withIndex()) {
                _benchmarkProgress.value = "[${index + 1}/${candidates.size}] 測試 ${candidate.first}..."
                api.setLteBands(profile.ipAddress, candidate.second)
                delay(3500)

                val sigRes = api.getSignalInfo(profile.ipAddress)
                if (sigRes.isSuccess) {
                    val sig = sigRes.getOrThrow()
                    val rsrp = sig.rsrp
                    val sinr = sig.sinr
                    val caCount = sig.caCount

                    val sinrPts = if (sinr != null) ((sinr + 10).coerceIn(0, 40) * (45.0 / 40.0)).toInt() else 0
                    val rsrpPts = if (rsrp != null) ((rsrp + 120).coerceIn(0, 50) * (35.0 / 50.0)).toInt() else 0
                    val caPts = when (caCount) {
                        4 -> 20
                        3 -> 18
                        2 -> 12
                        else -> 5
                    }
                    val totalScore = (sinrPts + rsrpPts + caPts).coerceIn(0, 100)

                    val item = BandBenchmarkResult(
                        bandName = candidate.first,
                        bands = candidate.second,
                        rsrp = rsrp,
                        sinr = sinr,
                        caCount = caCount,
                        activeBands = sig.activeBands.ifEmpty { sig.primaryBand },
                        score = totalScore,
                        isRecommended = false
                    )
                    results.add(item)
                    _benchmarkResults.value = results.sortedByDescending { it.score }
                }
            }

            // Restore original bands
            api.setLteBands(profile.ipAddress, originalBands)
            delay(1500)
            fetchInitialData(profile.ipAddress)

            val sorted = results.sortedByDescending { it.score }
            val finalResults = sorted.mapIndexed { idx, res ->
                if (idx == 0) res.copy(isRecommended = true) else res
            }
            _benchmarkResults.value = finalResults
            _isBenchmarking.value = false
            _benchmarkProgress.value = ""
            _infoMessage.value = "智慧頻段跑分完成！最高推薦: ${finalResults.firstOrNull()?.bandName ?: "無"}"
            onFinished(finalResults)
        }
    }


    fun applyBandLock(bands: List<LteBandInfo>) {
        val profile = _activeProfile.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = api.setLteBands(profile.ipAddress, bands)
            if (result.isSuccess) {
                _selectedBands.value = bands
                _infoMessage.value = "Bands lock updated successfully"
                delay(1000)
                fetchInitialData(profile.ipAddress)
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to update bands lock"
            }
            _isLoading.value = false
        }
    }

    fun switchProfile(profile: RouterProfile) {
        viewModelScope.launch {
            repository.setActiveProfileId(profile.id)
            _activeProfile.value = profile
            autoLogin(profile)
        }
    }

    fun saveProfile(profile: RouterProfile) {
        viewModelScope.launch {
            repository.addOrUpdateProfile(profile)
            _activeProfile.value = profile
            autoLogin(profile)
        }
    }

    fun deleteProfile(profileId: String) {
        viewModelScope.launch {
            repository.deleteProfile(profileId)
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _infoMessage.value = null
    }

    fun loadSmsList() {
        val profile = _activeProfile.value ?: return
        viewModelScope.launch {
            _isSmsLoading.value = true
            val countRes = api.getSmsCount(profile.ipAddress)
            if (countRes.isSuccess) {
                _smsCount.value = countRes.getOrThrow()
            }
            val listRes = api.getSmsList(profile.ipAddress, page = 1, maxCount = 30)
            if (listRes.isSuccess) {
                _smsMessages.value = listRes.getOrThrow()
            } else {
                _errorMessage.value = listRes.exceptionOrNull()?.localizedMessage ?: "Failed to load SMS"
            }
            _isSmsLoading.value = false
        }
    }

    fun sendSms(phone: String, content: String, onComplete: (Boolean) -> Unit) {
        val profile = _activeProfile.value ?: return
        viewModelScope.launch {
            _isSmsLoading.value = true
            val res = api.sendSms(profile.ipAddress, phone, content)
            if (res.isSuccess) {
                _infoMessage.value = "SMS sent successfully"
                onComplete(true)
                delay(1000)
                loadSmsList()
            } else {
                _errorMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to send SMS"
                onComplete(false)
            }
            _isSmsLoading.value = false
        }
    }

    fun deleteSms(index: Long) {
        val profile = _activeProfile.value ?: return
        viewModelScope.launch {
            val res = api.deleteSms(profile.ipAddress, index)
            if (res.isSuccess) {
                _infoMessage.value = "SMS deleted"
                _smsMessages.value = _smsMessages.value.filter { it.index != index }
                val countRes = api.getSmsCount(profile.ipAddress)
                if (countRes.isSuccess) {
                    _smsCount.value = countRes.getOrThrow()
                }
            } else {
                _errorMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to delete SMS"
            }
        }
    }

    fun rebootRouter() {
        val profile = _activeProfile.value ?: return
        viewModelScope.launch {
            _isRebooting.value = true
            _rebootCountdown.value = 60
            stopPolling()

            val res = api.rebootRouter(profile.ipAddress)
            if (res.isSuccess) {
                _infoMessage.value = "Router is rebooting..."
                _isLoggedIn.value = false

                // Countdown 60 seconds
                for (i in 60 downTo 1) {
                    _rebootCountdown.value = i
                    delay(1000)
                }
                _rebootCountdown.value = 0
                _isRebooting.value = false
                _infoMessage.value = "Reboot complete. Reconnecting..."
                autoLogin(profile)
            } else {
                _isRebooting.value = false
                _errorMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to send reboot command"
                startPolling(profile.ipAddress)
            }
        }
    }
}
