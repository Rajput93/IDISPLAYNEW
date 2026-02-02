package com.app.idisplaynew.data.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.idisplaynew.data.model.LoginPayload
import com.app.idisplaynew.data.model.LoginResponse
import com.app.idisplaynew.data.repository.Repository
import com.app.idisplaynew.ui.utils.DataStoreManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException

data class LoginUiState(
    val baseUrl: String = "https://idisplay-backend.digitalnoticeboard.biz/api/",
    val clientId: String = ""
)

class LoginViewModel(private val repository: Repository, private val dataStoreManager: DataStoreManager) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _registerResponse = MutableLiveData<LoginResponse?>()
    val registerResponse: LiveData<LoginResponse?> get() = _registerResponse

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    fun updateBaseUrl(value: String) {
        _uiState.value = _uiState.value.copy(baseUrl = value)
    }

    fun updateClientId(value: String) {
        _uiState.value = _uiState.value.copy(clientId = value)
    }

    fun register() {
        val baseUrl = _uiState.value.baseUrl.trim().let { if (it.endsWith("/")) it else "$it/" }
        val clientId = _uiState.value.clientId.trim()
        if (baseUrl.isBlank()) {
            _error.value = "Base URL is required"
            return
        }
        if (clientId.isBlank()) {
            _error.value = "Client ID is required"
            return
        }
        val request = LoginPayload(
            deviceId = clientId,
            name = "DisplayHub Device",
            ipAddress = "0.0.0.0",
            resolution = "1920x1080",
            orientation = "landscape",
            appVersion = "1.0",
            osVersion = "Android",
            registrationCode = "254565"
        )
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = repository.register(baseUrl, request)
                if (response.isSuccess) {
                    _registerResponse.value = response
                    _error.value = null
                } else {
                    _error.value = response.message.takeIf { it.isNotBlank() }
                        ?: response.errors.flatMap { (key, msgs) -> msgs.map { msg -> "$key: $msg" } }.joinToString(" ")
                }
            } catch (e: Exception) {
                when (e) {
                    is SocketTimeoutException -> {
                        _error.value = "⚠️ Your internet is slow. Please try again."
                    }
                    is IOException -> {
                        _error.value = "⚠️ Your internet connection appears to be slow. Please check your connection and try again."
                    }
                    else -> {
                        _error.value = "Error: ${e.message}"
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearRegisterResponse() {
        _registerResponse.value = null
    }
}

