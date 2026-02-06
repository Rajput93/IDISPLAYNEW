package com.app.idisplaynew.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.app.idisplaynew.data.repository.ScheduleRepository

class HomeViewModelFactory(
    private val scheduleRepository: ScheduleRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(scheduleRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
