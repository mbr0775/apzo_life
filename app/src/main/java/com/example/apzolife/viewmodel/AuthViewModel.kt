package com.example.apzolife.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.apzolife.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = AuthRepository.isLoggedIn(),
    val error: String? = null,
    val message: String? = null,
    val rememberMe: Boolean = false,
    val savedEmail: String = AuthRepository.getSavedEmail()
)

class AuthViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun setRememberMe(value: Boolean) {
        _uiState.value = _uiState.value.copy(rememberMe = value)
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, message = null)
            val result = AuthRepository.login(
                email = email,
                password = password,
                rememberMe = _uiState.value.rememberMe
            )
            _uiState.value = if (result.isSuccess) {
                _uiState.value.copy(isLoading = false, isLoggedIn = true, message = "Login successful")
            } else {
                _uiState.value.copy(isLoading = false, error = result.exceptionOrNull()?.message ?: "Login failed")
            }
        }
    }

    fun signup(fullName: String, email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, message = null)
            val result = AuthRepository.signup(fullName, email, password)
            _uiState.value = if (result.isSuccess) {
                _uiState.value.copy(
                    isLoading = false,
                    message = "Account created successfully. Check your email if confirmation is enabled."
                )
            } else {
                _uiState.value.copy(isLoading = false, error = result.exceptionOrNull()?.message ?: "Signup failed")
            }
        }
    }

    fun sendReset(email: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, message = null)
            val result = AuthRepository.sendReset(email)
            _uiState.value = if (result.isSuccess) {
                _uiState.value.copy(isLoading = false, message = "Password reset email sent")
            } else {
                _uiState.value.copy(isLoading = false, error = result.exceptionOrNull()?.message ?: "Failed to send reset email")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            AuthRepository.logout()
            _uiState.value = AuthUiState(isLoggedIn = false, savedEmail = "")
        }
    }

    fun clearStatus() {
        _uiState.value = _uiState.value.copy(error = null, message = null)
    }
}