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
        
        testFirebaseConnection()
        
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
                darkTheme = settingsState.preferences.darkTheme
            ) {
                val navController = rememberNavController()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Check for security lock on app start
                    LaunchedEffect(
                        firebaseAuth.currentUser,
                        settingsState.preferences.biometricLockEnabled,
                        settingsState.preferences.pin,
                        settingsState.isLoading,
                        startDestination
                    ) {
                        if (!settingsState.isLoading &&
                            firebaseAuth.currentUser != null &&
                            (settingsState.preferences.biometricLockEnabled || settingsState.preferences.pin != null) &&
                            startDestination == NavRoutes.HOME
                        ) {
                            navController.navigate(NavRoutes.APP_LOCK) {
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

    private fun testFirebaseConnection() {
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val testData = hashMapOf(
            "timestamp" to com.google.firebase.Timestamp.now(),
            "status" to "Health Check"
        )

        db.collection("_connection_test")
            .add(testData)
            .addOnSuccessListener { documentReference ->
                android.util.Log.d("FIREBASE_TEST", "Connection successful! DocumentSnapshot added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("FIREBASE_TEST", "Connection failed", e)
            }
    }
}