package com.example.moneymap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymap.data.repository.AuthRepository
import com.example.moneymap.data.sync.SyncManager
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val user: FirebaseUser? = null,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        _uiState.value = _uiState.value.copy(
            user = authRepository.currentUser,
            isLoggedIn = authRepository.isUserLoggedIn
        )
    }

    fun signIn(email: String, password: String) {
        if (!isValidEmail(email)) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a valid email address")
            return
        }
        if (password.isBlank()) {
             _uiState.value = _uiState.value.copy(errorMessage = "Please enter your password")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = authRepository.signInWithEmailAndPassword(email, password)
            result.onSuccess { user ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    user = user,
                    isLoggedIn = true,
                    errorMessage = null
                )
                // Trigger sync after successful login
                syncManager.triggerManualSync()
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = exception.message ?: "Sign in failed",
                    isLoggedIn = false
                )
            }
        }
    }

    fun signUp(email: String, password: String) {
        if (!isValidEmail(email)) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a valid email address")
            return
        }
        if (!isValidPassword(password)) {
            _uiState.value = _uiState.value.copy(errorMessage = "Password must be at least 8 characters, contain an uppercase letter and a number")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = authRepository.signUpWithEmailAndPassword(email, password)
            result.onSuccess { user ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    user = user,
                    isLoggedIn = true,
                    errorMessage = null
                )
                // Trigger sync after successful signup
                syncManager.triggerManualSync()
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = exception.message ?: "Sign up failed",
                    isLoggedIn = false
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.value = AuthUiState()
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = authRepository.sendPasswordResetEmail(email)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Password reset email sent"
                )
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = exception.message ?: "Failed to send password reset email"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = authRepository.signInWithGoogle(idToken)
            result.onSuccess { user ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    user = user,
                    isLoggedIn = true,
                    errorMessage = null
                )
                // Trigger sync after successful login
                syncManager.triggerManualSync()
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = exception.message ?: "Google sign in failed",
                    isLoggedIn = false
                )
            }
        }
    }

    fun updateProfile(displayName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = authRepository.updateProfile(displayName)
            result.onSuccess {
                // Refresh user data
                checkAuthState()
                _uiState.value = _uiState.value.copy(isLoading = false)
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = exception.message ?: "Failed to update profile"
                )
            }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = authRepository.deleteAccount()
            result.onSuccess {
                _uiState.value = AuthUiState() // Reset state
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = exception.message ?: "Failed to delete account"
                )
            }
        }
    }

    fun updatePassword(password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = authRepository.updatePassword(password)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Password updated successfully" // Using errorMessage for success msg for now, or consider adding a successMessage field
                )
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = exception.message ?: "Failed to update password"
                )
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\$".toRegex()
        return email.matches(emailRegex)
    }

    private fun isValidPassword(password: String): Boolean {
        if (password.length < 8) return false
        val hasUpperCase = password.any { it.isUpperCase() }
        val hasNumber = password.any { it.isDigit() }
        return hasUpperCase && hasNumber
    }
}

