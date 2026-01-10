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
        
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settingsState by settingsViewModel.uiState.collectAsState()

            MoneyMapTheme(
                darkTheme = settingsState.preferences.darkTheme
            ) {
                // Show splash/loading screen while settings are loading
                if (settingsState.isLoading) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        // You could add a logo or loading indicator here
                    }
                } else {
                    val startDestination = if (firebaseAuth.currentUser == null) {
                        NavRoutes.LOGIN
                    } else if (settingsState.preferences.biometricLockEnabled || settingsState.preferences.pin != null) {
                        NavRoutes.APP_LOCK
                    } else {
                        NavRoutes.HOME
                    }

                    val navController = rememberNavController()

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MoneyMapNavigation(
                            navController = navController,
                            startDestination = startDestination
                        )
                    }
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