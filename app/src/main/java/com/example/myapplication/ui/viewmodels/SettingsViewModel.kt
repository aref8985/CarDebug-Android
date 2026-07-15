package com.example.myapplication.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.CarDiagDatabase
import com.example.myapplication.data.settings.AppLanguage
import com.example.myapplication.data.settings.AppTheme
import com.example.myapplication.data.settings.SettingsRepository
import com.example.myapplication.data.settings.UnitSystem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository, private val application: Application) : AndroidViewModel(application) {

    val theme = repository.themeFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppTheme.SYSTEM)
    val language = repository.languageFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppLanguage.EN)
    val units = repository.unitsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UnitSystem.METRIC)
    val rpmThreshold = repository.rpmThresholdFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 6000)
    val tempThreshold = repository.tempThresholdFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 105)
    val autoConnect = repository.autoConnectFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { repository.updateTheme(theme) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { repository.updateLanguage(language) }
    }

    fun setUnits(units: UnitSystem) {
        viewModelScope.launch { repository.updateUnits(units) }
    }

    fun setRpmThreshold(threshold: Int) {
        viewModelScope.launch { repository.updateRpmThreshold(threshold) }
    }

    fun setTempThreshold(threshold: Int) {
        viewModelScope.launch { repository.updateTempThreshold(threshold) }
    }

    fun setAutoConnect(enabled: Boolean) {
        viewModelScope.launch { repository.updateAutoConnect(enabled) }
    }

    fun clearAllData() {
        viewModelScope.launch {
            val dao = CarDiagDatabase.getDatabase(application).carDiagDao()
            dao.deleteAllVehicles()
        }
    }
}
