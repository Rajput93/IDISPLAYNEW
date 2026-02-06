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

    /** Path where downloaded images/videos are stored (e.g. for display). */
    fun getMediaStoragePath(): String = scheduleRepository.getMediaStoragePath()

    fun clearToast() {
        _toastMessage.value = null
    }

    private var syncJob: Job? = null
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
        syncJob = viewModelScope.launch {
            while (true) {
                val result = scheduleRepository.syncFromApi()
                if (result != null && (result.downloadedImages > 0 || result.downloadedVideos > 0)) {
                    val parts = mutableListOf<String>()
                    if (result.downloadedImages > 0) parts.add("${result.downloadedImages} image(s)")
                    if (result.downloadedVideos > 0) parts.add("${result.downloadedVideos} video(s)")
                    _toastMessage.value = "Downloaded: ${parts.joinToString(", ")}\nStorage: ${result.storagePath}"
                }
                delay(1000)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        syncJob?.cancel()
        collectJob?.cancel()
    }
}
