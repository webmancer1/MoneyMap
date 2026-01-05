package com.example.moneymap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymap.data.preferences.SettingsPreferences
import com.example.moneymap.data.preferences.SettingsRepository
import com.example.moneymap.data.sync.SyncManager
import com.example.moneymap.data.sync.SyncResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isLoading: Boolean = true,
    val preferences: SettingsPreferences = SettingsPreferences()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val syncManager: SyncManager,
    private val exportManager: com.example.moneymap.data.export.ExportManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages = _messages.asSharedFlow()

    init {
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { preferences ->
                _uiState.value = SettingsUiState(isLoading = false, preferences = preferences)
            }
        }
    }

    fun toggleDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateDarkTheme(enabled)
        }
    }



    fun updateCurrency(currency: String) {
        viewModelScope.launch {
            settingsRepository.updateCurrency(currency)
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateNotificationsEnabled(enabled)
            // If master switch is off, maybe disable others or just use this as master
        }
    }

    fun toggleTransactionNotifications(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateNotificationsTransactions(enabled)
        }
    }

    fun toggleBudgetNotifications(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateNotificationsBudget(enabled)
        }
    }

    fun toggleSecurityNotifications(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateNotificationsSecurity(enabled)
        }
    }

    fun toggleTipsNotifications(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateNotificationsTips(enabled)
        }
    }

    fun toggleBiometricLock(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateBiometricLock(enabled)
        }
    }

    fun triggerPlaceholderMessage(message: String) {
        _messages.tryEmit(message)
    }

    suspend fun triggerManualSync(): SyncResult {
        return syncManager.triggerManualSync()
    }

    fun toggleAutoSync(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAutoSync(enabled)
            if (enabled) {
                syncManager.schedulePeriodicSync()
            } else {
                syncManager.cancelPeriodicSync()
            }
        }
    }


    suspend fun performExport(): SyncResult {
        triggerPlaceholderMessage("Starting export (CSV)...")
        return exportManager.exportData()
    }


    fun setPin(pin: String) {
        viewModelScope.launch {
            val hashedPin = hashPin(pin)
            settingsRepository.updatePin(hashedPin)
        }
    }

    fun removePin() {
        viewModelScope.launch {
            settingsRepository.updatePin(null)
        }
    }

    fun verifyPin(inputPin: String): Boolean {
        val currentPin = uiState.value.preferences.pin ?: return false
        return hashPin(inputPin) == currentPin
    }

    private fun hashPin(pin: String): String {
        val bytes = pin.toByteArray()
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}
