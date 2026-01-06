package com.example.moneymap.data.security

import androidx.activity.ComponentActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class BiometricHelper(private val activity: ComponentActivity) {
    
    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(activity)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }

    suspend fun authenticate(
        title: String = "Biometric Authentication",
        subtitle: String = "Use your fingerprint or face to unlock",
        negativeButtonText: String = "Cancel"
    ): BiometricResult = suspendCancellableCoroutine { continuation ->
        val executor = ContextCompat.getMainExecutor(activity)
        val fragmentActivity = activity as? FragmentActivity ?: run {
            continuation.resume(BiometricResult.Error("Activity is not a FragmentActivity"))
            return@suspendCancellableCoroutine
        }
        
        val biometricPrompt = BiometricPrompt(
            fragmentActivity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // Ignore user cancellation to avoid error loop if they want to use PIN
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        continuation.resume(BiometricResult.Failed)
                    } else {
                        continuation.resume(BiometricResult.Error(errString.toString()))
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    continuation.resume(BiometricResult.Success)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // This is called for soft failures (e.g. unrecognized fingerprint), do not resume yet, let them try again
                    // actually, the prompt stays up, so we don't need to do anything here usually.
                    // But if we want to handle it, we can. For now, let's let the prompt handle retries.
                    // If we resume here, the coroutine ends.
                    // BiometricPrompt handles retries internally for failed attempts.
                    // We only want to resume if it's a hard error or success.
                    // However, to match previous logic, if we need to notify UI, we might need a callback.
                    // But suspend function returns once.
                    // So we DON'T resume on AuthenticationFailed, waiting for Error or Success.
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()

        continuation.invokeOnCancellation {
            biometricPrompt.cancelAuthentication()
        }

        biometricPrompt.authenticate(promptInfo)
    }
}

sealed class BiometricResult {
    object Success : BiometricResult()
    data class Error(val message: String) : BiometricResult()
    object Failed : BiometricResult()
}

