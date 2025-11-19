package com.example.moneymap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.auth.FirebaseAuth
import androidx.navigation.compose.rememberNavController
import com.example.moneymap.navigation.MoneyMapNavigation
import com.example.moneymap.navigation.NavRoutes
import com.example.moneymap.ui.theme.MoneyMapTheme
import com.example.moneymap.ui.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Check if user is already logged in
        val firebaseAuth = FirebaseAuth.getInstance()
        val startDestination = if (firebaseAuth.currentUser != null) {
            NavRoutes.HOME
        } else {
            NavRoutes.LOGIN
        }
        
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settingsState by settingsViewModel.uiState.collectAsState()

            MoneyMapTheme(
                darkTheme = settingsState.preferences.darkTheme,
                dynamicColor = settingsState.preferences.dynamicColor
            ) {
                val navController = rememberNavController()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Check for biometric lock on app start
                    LaunchedEffect(
                        firebaseAuth.currentUser,
                        settingsState.preferences.biometricLockEnabled,
                        startDestination
                    ) {
                        if (firebaseAuth.currentUser != null &&
                            settingsState.preferences.biometricLockEnabled &&
                            startDestination == NavRoutes.HOME
                        ) {
                            navController.navigate(NavRoutes.BIOMETRIC_LOCK) {
                                popUpTo(NavRoutes.HOME) { inclusive = true }
                            }
                        }
                    }

                    MoneyMapNavigation(
                        navController = navController,
                        startDestination = startDestination
                    )
                }
            }
        }
    }
}