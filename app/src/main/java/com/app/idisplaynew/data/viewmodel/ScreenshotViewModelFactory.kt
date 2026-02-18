package com.app.idisplaynew.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.app.idisplaynew.data.remote.ScreenshotUploader
import com.app.idisplaynew.ui.utils.DataStoreManager

class ScreenshotViewModelFactory(
    private val dataStoreManager: DataStoreManager,
    private val uploader: ScreenshotUploader = ScreenshotUploader()
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScreenshotViewModel::class.java)) {
            return ScreenshotViewModel(dataStoreManager, uploader) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
