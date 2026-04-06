package com.example.fitnesstracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnesstracker.data.DataRepository
import com.example.fitnesstracker.data.network.ActivityRequest
import com.example.fitnesstracker.data.network.ActivityResponse
import com.example.fitnesstracker.data.network.ChangePasswordRequest
import com.example.fitnesstracker.data.network.ChangePasswordResponse
import com.example.fitnesstracker.data.network.User
import com.example.fitnesstracker.data.network.UserResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class FitnessViewModel(private val repository: DataRepository) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _saveResult = MutableStateFlow<Result<ActivityResponse>?>(null)
    val saveResult: StateFlow<Result<ActivityResponse>?> = _saveResult.asStateFlow()

    private val _updateResult = MutableStateFlow<Result<UserResponse>?>(null)
    val updateResult: StateFlow<Result<UserResponse>?> = _updateResult.asStateFlow()

    private val _passwordResult = MutableStateFlow<Result<ChangePasswordResponse>?>(null)
    val passwordResult: StateFlow<Result<ChangePasswordResponse>?> = _passwordResult.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile.asStateFlow()

    /**
     * Fetches the authenticated user's profile from the backend.
     *
     * Guards:
     * - Immediately fails with a clear error if [userId] is 0 (un-authenticated session).
     * - Times out after 5 seconds so the spinner never hangs indefinitely.
     */
    fun fetchProfile(userId: Int) {
        if (userId <= 0) {
            _error.value = "Session error: please log in again."
            _isLoading.value = false
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = withTimeoutOrNull(5_000L) {
                    repository.getUserProfile(userId)
                }
                when {
                    result == null -> {
                        // Timeout path — spinner must stop
                        _error.value = "Profile load timed out. Check your connection."
                    }
                    else -> result
                        .onSuccess { response ->
                            if (response.success && response.user != null) {
                                _userProfile.value = response.user
                            } else {
                                _error.value = response.message.ifBlank { "Could not load profile." }
                            }
                        }
                        .onFailure { t ->
                            _userProfile.value = null
                            _error.value = t.localizedMessage ?: "Failed to load profile."
                        }
                }
            } catch (e: Exception) {
                _error.value = "Connection error: ${e.localizedMessage}"
            } finally {
                // Guaranteed: spinner ALWAYS stops
                _isLoading.value = false
            }
        }
    }

    fun updateProfile(userId: Int, name: String, email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                repository.updateUser(userId, name, email)
                    .onSuccess { response ->
                        if (response.success && response.user != null) {
                            _userProfile.value = response.user
                            _updateResult.value = Result.success(response)
                        } else {
                            _error.value = response.message
                        }
                    }
                    .onFailure { t ->
                        _error.value = t.localizedMessage ?: "Failed to update profile"
                    }
            } catch (e: Exception) {
                _error.value = "Connection error: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun changePassword(userId: Int, current: String, new: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val request = ChangePasswordRequest(userId, current, new)
            try {
                repository.changePassword(request)
                    .onSuccess { response ->
                        if (response.success) {
                            _passwordResult.value = Result.success(response)
                        } else {
                            _error.value = response.message
                        }
                    }
                    .onFailure { t ->
                        _error.value = t.localizedMessage ?: "Failed to change password"
                    }
            } catch (e: Exception) {
                _error.value = "Connection error: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveActivity(request: ActivityRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.saveActivity(request).onSuccess {
                _saveResult.value = Result.success(it)
                _isLoading.value = false
            }.onFailure {
                _error.value = it.localizedMessage ?: "Failed to save activity"
                _isLoading.value = false
            }
        }
    }

    fun clearResults() {
        _saveResult.value = null
        _updateResult.value = null
        _passwordResult.value = null
        _error.value = null
    }
}
