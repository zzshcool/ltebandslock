package com.ltebandslock.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ltebandslock.data.api.HuaweiRouterApi
import com.ltebandslock.data.model.DeviceInfo
import com.ltebandslock.data.model.LteBandInfo
import com.ltebandslock.data.model.LteBands
import com.ltebandslock.data.model.RouterProfile
import com.ltebandslock.data.model.SignalInfo
import com.ltebandslock.data.model.TrafficInfo
import com.ltebandslock.data.repository.ProfileRepository
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

    private fun autoLogin(profile: RouterProfile) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
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
            _signalInfo.value = sigRes.getOrThrow()
        }

        val trafRes = api.getTrafficInfo(ipAddress)
        if (trafRes.isSuccess) {
            _trafficInfo.value = trafRes.getOrThrow()
        }
    }

    private fun startPolling(ipAddress: String) {
        stopPolling()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(2000) // Poll every 2 seconds
                if (_isLoggedIn.value) {
                    val sigRes = api.getSignalInfo(ipAddress)
                    if (sigRes.isSuccess) {
                        _signalInfo.value = sigRes.getOrThrow()
                    }

                    val trafRes = api.getTrafficInfo(ipAddress)
                    if (trafRes.isSuccess) {
                        _trafficInfo.value = trafRes.getOrThrow()
                    }

                    val devRes = api.getDeviceInfo(ipAddress)
                    if (devRes.isSuccess) {
                        _deviceInfo.value = devRes.getOrThrow()
                    }
                }
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
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
}
