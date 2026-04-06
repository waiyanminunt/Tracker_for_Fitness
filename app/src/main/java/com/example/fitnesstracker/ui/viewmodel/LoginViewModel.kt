package com.example.fitnesstracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnesstracker.data.DataRepository
import com.example.fitnesstracker.data.network.LoginRequest
import com.example.fitnesstracker.data.network.LoginResponse
import com.example.fitnesstracker.data.network.RegisterRequest
import com.example.fitnesstracker.data.network.RegisterResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(private val repository: DataRepository) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loginResult = MutableStateFlow<Result<LoginResponse>?>(null)
    val loginResult: StateFlow<Result<LoginResponse>?> = _loginResult.asStateFlow()

    private val _registerResult = MutableStateFlow<Result<RegisterResponse>?>(null)
    val registerResult: StateFlow<Result<RegisterResponse>?> = _registerResult.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                repository.login(LoginRequest(email, password))
                    .onSuccess { _loginResult.value = Result.success(it) }
                    .onFailure { _error.value = it.localizedMessage ?: "Login failed" }
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Unexpected error during login"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(name: String, email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                repository.register(RegisterRequest(name, email, password))
                    .onSuccess { _registerResult.value = Result.success(it) }
                    .onFailure { _error.value = it.localizedMessage ?: "Registration failed" }
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Unexpected error during registration"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearResults() {
        _loginResult.value = null
        _registerResult.value = null
        _error.value = null
    }
}
