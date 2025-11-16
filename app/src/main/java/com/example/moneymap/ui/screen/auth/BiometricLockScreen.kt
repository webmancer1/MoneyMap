package com.example.moneymap.ui.screen.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.activity.ComponentActivity
import com.example.moneymap.data.security.BiometricHelper
import com.example.moneymap.data.security.BiometricResult

@Composable
fun BiometricLockScreen(
    onAuthenticationSuccess: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isAuthenticating by remember { mutableStateOf(false) }

    LaunchedEffect(activity) {
        if (activity != null) {
            val helper = BiometricHelper(activity)
            if (helper.isBiometricAvailable()) {
                isAuthenticating = true
                when (val result = helper.authenticate()) {
                    is BiometricResult.Success -> {
                        onAuthenticationSuccess()
                    }
                    is BiometricResult.Error -> {
                        errorMessage = result.message
                        isAuthenticating = false
                    }
                    is BiometricResult.Failed -> {
                        errorMessage = "Authentication failed. Please try again."
                        isAuthenticating = false
                    }
                }
            } else {
                errorMessage = "Biometric authentication is not available on this device."
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Fingerprint,
            contentDescription = "Biometric",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Unlock MoneyMap",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Use your fingerprint or face to continue",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        if (isAuthenticating) {
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator()
        }
        
        errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedButton(onClick = onSkip) {
            Text("Skip")
        }
    }
}

