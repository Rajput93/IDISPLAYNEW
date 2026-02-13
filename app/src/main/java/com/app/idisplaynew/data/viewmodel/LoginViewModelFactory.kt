package com.app.idisplaynew.data.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.app.idisplaynew.data.repository.Repository
import com.app.idisplaynew.ui.utils.DataStoreManager

class LoginViewModelFactory(
    private val repository: Repository,
    private val dataStoreManager: DataStoreManager,
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(repository, dataStoreManager, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}