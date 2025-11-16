package com.example.moneymap.ui.lifecycle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import com.example.moneymap.data.preferences.SettingsRepository
import com.example.moneymap.navigation.NavRoutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun BiometricLifecycleObserver(
    navController: NavController,
    settingsRepository: SettingsRepository,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
) {
    var shouldCheckBiometric by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    shouldCheckBiometric = true
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(shouldCheckBiometric) {
        if (shouldCheckBiometric) {
            CoroutineScope(Dispatchers.Main).launch {
                val preferences = settingsRepository.settingsFlow.first { true }
                val currentRoute = navController.currentBackStackEntry?.destination?.route
                if (preferences.biometricLockEnabled && 
                    currentRoute != NavRoutes.BIOMETRIC_LOCK && 
                    currentRoute != NavRoutes.LOGIN && 
                    currentRoute != NavRoutes.REGISTER) {
                    navController.navigate(NavRoutes.BIOMETRIC_LOCK)
                }
            }
        }
        onDispose {}
    }
}

