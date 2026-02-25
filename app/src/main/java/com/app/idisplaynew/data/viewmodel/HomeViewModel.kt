package com.app.idisplaynew.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.idisplaynew.data.model.HeartbeatPayload
import com.app.idisplaynew.data.model.ScheduleCurrentResponse
import com.app.idisplaynew.data.remote.DeviceStatsProvider
import com.app.idisplaynew.data.repository.Repository
import com.app.idisplaynew.data.repository.ScheduleRepository
import com.app.idisplaynew.ui.utils.DataStoreManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HomeViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val dataStoreManager: DataStoreManager,
    private val deviceStatsProvider: DeviceStatsProvider
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
    private var heartbeatJob: Job? = null

    /** For heartbeat payload: currently displayed media (zoneId_mediaId or empty). */
    private val _currentDisplayedMediaId = MutableStateFlow<String>("")
    val currentDisplayedMediaId: StateFlow<String> = _currentDisplayedMediaId.asStateFlow()
    fun setCurrentDisplayedMediaId(zoneId: Int, mediaId: Int) {
        _currentDisplayedMediaId.value = "${zoneId}_$mediaId"
    }

    /** Last heartbeat API result: "success" if 200, else "failed". Sent in next payload. */
    private var lastHeartbeatStatus: String = "success"

    init {
        collectJob = viewModelScope.launch {
            scheduleRepository.getCurrentLayoutAndTickers()
                .catch { _error.value = it.message }
                .collect { (layout, tickers) ->
                    if (_layout.value != layout) _layout.value = layout
                    if (_tickers.value != tickers) _tickers.value = tickers
                    _isLoading.value = false
                }
        }
        // Poll schedule API every 5 sec; after sync set layout from DB snapshot so new downloads show (single → multiple)
        syncJob = viewModelScope.launch {
            while (true) {
                scheduleRepository.syncFromApi()
                delay(150) // let DB commits be visible
                val (layout, tickers) = scheduleRepository.getCurrentLayoutAndTickersSnapshot()
                if (_layout.value != layout) _layout.value = layout
                if (_tickers.value != tickers) _tickers.value = tickers
                scheduleRepository.notifyLayoutRefresh()
                delay(5_000)
            }
        }
        // Poll device commands every 10 sec; if commands not empty, ack each then trigger schedule sync
        commandsJob = viewModelScope.launch {
            while (true) {
                scheduleRepository.fetchAndProcessCommands()
                delay(150)
                val (layout, tickers) = scheduleRepository.getCurrentLayoutAndTickersSnapshot()
                if (_layout.value != layout) _layout.value = layout
                if (_tickers.value != tickers) _tickers.value = tickers
                scheduleRepository.notifyLayoutRefresh()
                delay(10_000)
            }
        }
        // Heartbeat every 10 sec (separate from commands); payload has device stats and status from previous call
        heartbeatJob = viewModelScope.launch {
            while (true) {
                val baseUrl = dataStoreManager.baseUrl.first() ?: ""
                val token = dataStoreManager.authToken.first() ?: ""
                if (baseUrl.isNotBlank() && token.isNotBlank()) {
                    val layoutId = _layout.value?.layoutId?.toString() ?: ""
                    val mediaId = _currentDisplayedMediaId.value
                    val payload = HeartbeatPayload(
                        status = lastHeartbeatStatus,
                        ipAddress = deviceStatsProvider.getIpAddress(),
                        temperature = deviceStatsProvider.getTemperature(),
                        cpuUsage = deviceStatsProvider.getCpuUsage(),
                        memoryUsage = deviceStatsProvider.getMemoryUsage(),
                        storageUsage = deviceStatsProvider.getStorageUsage(),
                        storageAvailableMb = deviceStatsProvider.getStorageAvailableMb(),
                        currentLayoutId = layoutId,
                        currentMediaId = mediaId,
                        appVersion = deviceStatsProvider.getAppVersion(),
                        osVersion = deviceStatsProvider.getOsVersion()
                    )
                    val result = Repository.postHeartbeat(baseUrl, token, payload)
                    lastHeartbeatStatus = if (result.isSuccess) "success" else "failed"
                }
                delay(10_000)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        syncJob?.cancel()
        commandsJob?.cancel()
        collectJob?.cancel()
        heartbeatJob?.cancel()
    }
}
