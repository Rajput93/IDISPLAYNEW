package com.app.idisplaynew.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.app.idisplaynew.data.remote.DeviceStatsProvider
import com.app.idisplaynew.data.repository.ScheduleRepository
import com.app.idisplaynew.ui.utils.DataStoreManager

class HomeViewModelFactory(
    private val scheduleRepository: ScheduleRepository,
    private val dataStoreManager: DataStoreManager,
    private val deviceStatsProvider: DeviceStatsProvider
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(scheduleRepository, dataStoreManager, deviceStatsProvider) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
