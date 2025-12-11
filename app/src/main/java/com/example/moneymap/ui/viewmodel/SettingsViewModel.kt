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
    private val syncManager: SyncManager
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
}
