package com.app.idisplaynew.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.idisplaynew.data.model.ScheduleCurrentResponse
import com.app.idisplaynew.data.repository.ScheduleRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class HomeViewModel(
    private val scheduleRepository: ScheduleRepository
) : ViewModel() {

    private val _layout = MutableStateFlow<ScheduleCurrentResponse.ScheduleResult.Layout?>(null)
    val layout: StateFlow<ScheduleCurrentResponse.ScheduleResult.Layout?> = _layout.asStateFlow()

    private val _tickers = MutableStateFlow<List<ScheduleCurrentResponse.ScheduleResult.Ticker>>(emptyList())
    val tickers: StateFlow<List<ScheduleCurrentResponse.ScheduleResult.Ticker>> = _tickers.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _apiMessage = MutableStateFlow<String?>(null)
    val apiMessage: StateFlow<String?> = _apiMessage.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _mediaStoragePath = MutableStateFlow("")
    val mediaStoragePath: StateFlow<String> = _mediaStoragePath.asStateFlow()

    /** Path where downloaded images/videos are stored (visible on device). */
    fun getMediaStoragePath(): String = scheduleRepository.getMediaStoragePath()

    fun clearToast() {
        _toastMessage.value = null
    }

    private var syncJob: Job? = null
    private var commandsJob: Job? = null
    private var collectJob: Job? = null

    init {
        collectJob = viewModelScope.launch {
            scheduleRepository.getCurrentLayoutAndTickers()
                .catch { _error.value = it.message }
                .collect { (layout, tickers) ->
                    _layout.value = layout
                    _tickers.value = tickers
                    _isLoading.value = false
                }
        }
        // Poll schedule API every 5 sec
        syncJob = viewModelScope.launch {
            while (true) {
                scheduleRepository.syncFromApi()
                delay(5_000)
            }
        }
        // Poll device commands every 10 sec; if commands not empty, ack each then trigger schedule sync
        commandsJob = viewModelScope.launch {
            while (true) {
                scheduleRepository.fetchAndProcessCommands()
                delay(10_000)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        syncJob?.cancel()
        commandsJob?.cancel()
        collectJob?.cancel()
    }
}
