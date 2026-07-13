package com.example.apzolife.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.apzolife.data.repository.AuthRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val rememberMe: Boolean = false,
    val savedEmail: String = AuthRepository.getSavedEmail()
)

class AuthViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            AuthRepository.login(email, password, _uiState.value.rememberMe)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Login failed") }
                }
        }
    }

    fun signup(fullName: String, email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            AuthRepository.signup(fullName, email, password)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, message = "Account created! Please verify your email.") }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Signup failed") }
                }
        }
    }

    fun sendReset(email: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            AuthRepository.sendReset(email)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, message = "Reset link sent to your email") }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to send reset email") }
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            AuthRepository.logout()
        }
    }

    fun setRememberMe(value: Boolean) {
        _uiState.update { it.copy(rememberMe = value) }
    }
}