package com.example.moneymap.ui.screen.auth

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.moneymap.data.security.BiometricHelper
import com.example.moneymap.data.security.BiometricResult
import com.example.moneymap.ui.viewmodel.SettingsViewModel

@Composable
fun AppLockScreen(
    onUnlock: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val uiState by viewModel.uiState.collectAsState()
    
    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isBiometricLoading by remember { mutableStateOf(false) }

    // Try biometric immediately if enabled
    LaunchedEffect(uiState.preferences.biometricLockEnabled) {
        if (uiState.preferences.biometricLockEnabled && activity != null) {
            val helper = BiometricHelper(activity)
            if (helper.isBiometricAvailable()) {
                isBiometricLoading = true
                when (helper.authenticate()) {
                    is BiometricResult.Success -> onUnlock()
                    is BiometricResult.Error -> {
                        errorMessage = "Biometric error"
                        isBiometricLoading = false
                    }
                    is BiometricResult.Failed -> {
                        errorMessage = "Biometric failed"
                        isBiometricLoading = false
                    }
                }
            }
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.padding(top = 48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "MoneyMap Locked",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                if (uiState.preferences.pin != null) {
                    Text(
                        text = "Enter PIN to unlock",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    PinDots(length = 4, filledCount = pin.length)
                }

                if (isBiometricLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }

                errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (uiState.preferences.pin != null) {
                NumberPad(
                    onNumberClick = { number ->
                        errorMessage = null
                        if (pin.length < 4) {
                            pin += number
                            if (pin.length == 4) {
                                if (viewModel.verifyPin(pin)) {
                                    onUnlock()
                                } else {
                                    errorMessage = "Incorrect PIN"
                                    pin = ""
                                }
                            }
                        }
                    },
                    onDeleteClick = {
                        errorMessage = null
                        if (pin.isNotEmpty()) {
                            pin = pin.dropLast(1)
                        }
                    }
                )
            } else if (!uiState.preferences.biometricLockEnabled) {
                // Fallback if no security is enabled but we ended up here (shouldn't happen ideally)
                Button(onClick = onUnlock) {
                    Text("Unlock")
                }
            }
        }
    }
}
