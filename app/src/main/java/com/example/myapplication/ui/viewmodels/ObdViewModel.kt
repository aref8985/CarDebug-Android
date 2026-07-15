package com.example.myapplication.ui.viewmodels

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.CarDiagDatabase
import com.example.myapplication.data.local.Vehicle
import com.example.myapplication.obd.ObdBluetoothManager
import com.example.myapplication.obd.ObdConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ObdViewModel(private val obdBluetoothManager: ObdBluetoothManager, application: android.app.Application) : ViewModel() {
    private val dao = CarDiagDatabase.getDatabase(application).carDiagDao()

    val connectionState = obdBluetoothManager.connectionState
    val obdData = obdBluetoothManager.obdData
    
    val activeVehicle: StateFlow<Vehicle?> = dao.getActiveVehicle()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _rpmHistory = MutableStateFlow<List<Int>>(emptyList())
    val rpmHistory: StateFlow<List<Int>> = _rpmHistory.asStateFlow()

    private val _maxRpm = MutableStateFlow(0)
    val maxRpm: StateFlow<Int> = _maxRpm.asStateFlow()

    private val _maxSpeed = MutableStateFlow(0)
    val maxSpeed: StateFlow<Int> = _maxSpeed.asStateFlow()

    init {
        viewModelScope.launch {
            obdData.collect { data ->
                val currentList = _rpmHistory.value.toMutableList()
                currentList.add(data.rpm)
                // Memory Safety: Keep only last 100 points
                if (currentList.size > 100) currentList.removeAt(0)
                _rpmHistory.value = currentList

                if (data.rpm > _maxRpm.value) _maxRpm.value = data.rpm
                if (data.speed > _maxSpeed.value) _maxSpeed.value = data.speed
            }
        }

        viewModelScope.launch {
            connectionState.collect { state ->
                if (state !is ObdConnectionState.Connected) {
                    resetMaxValues()
                }
            }
        }
    }

    fun resetMaxValues() {
        _maxRpm.value = 0
        _maxSpeed.value = 0
    }

    fun getPairedDevices() = obdBluetoothManager.getPairedDevices()

    fun connect(device: BluetoothDevice) {
        viewModelScope.launch {
            obdBluetoothManager.connect(device)
        }
    }

    fun disconnect() {
        obdBluetoothManager.disconnect(isManual = true)
    }
}
