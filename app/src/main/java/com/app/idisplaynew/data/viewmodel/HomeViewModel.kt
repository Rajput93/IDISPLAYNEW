package com.app.idisplaynew.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.idisplaynew.data.model.ScheduleCurrentResponse
import com.app.idisplaynew.data.repository.Repository
import com.app.idisplaynew.ui.utils.DataStoreManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: Repository,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _layout = MutableStateFlow<ScheduleCurrentResponse.ScheduleResult.Layout?>(null)
    val layout: StateFlow<ScheduleCurrentResponse.ScheduleResult.Layout?> = _layout.asStateFlow()

    private val _tickers = MutableStateFlow<List<ScheduleCurrentResponse.ScheduleResult.Ticker>>(emptyList())
    val tickers: StateFlow<List<ScheduleCurrentResponse.ScheduleResult.Ticker>> = _tickers.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadSchedule() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val baseUrl = dataStoreManager.baseUrl.first() ?: run {
                    _error.value = "Base URL not found. Please login again."
                    _isLoading.value = false
                    return@launch
                }
                val token = dataStoreManager.authToken.first() ?: run {
                    _error.value = "Token not found. Please login again."
                    _isLoading.value = false
                    return@launch
                }
                val response = repository.getScheduleCurrent(baseUrl, token)
                if (response.isSuccess && response.result?.layout != null) {
                    _layout.value = response.result.layout
                    _tickers.value = response.result.tickers
                    _error.value = null
                } else {
                    _error.value = response.message.ifBlank { "Failed to load schedule" }
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
