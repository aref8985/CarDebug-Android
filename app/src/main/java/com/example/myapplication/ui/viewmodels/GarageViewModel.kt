package com.example.myapplication.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.CarDiagDatabase
import com.example.myapplication.data.local.Vehicle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GarageViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = CarDiagDatabase.getDatabase(application).carDiagDao()

    val allVehicles: StateFlow<List<Vehicle>> = dao.getAllVehicles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeVehicle: StateFlow<Vehicle?> = dao.getActiveVehicle()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun addVehicle(brand: String, model: String, vin: String?, year: String?, mileage: Int) {
        viewModelScope.launch {
            dao.insertVehicle(
                Vehicle(
                    name = "$brand $model",
                    brand = brand,
                    model = model,
                    vin = vin,
                    year = year,
                    mileage = mileage.toDouble(),
                    engineType = detectEngineType(model)
                )
            )
        }
    }

    private fun detectEngineType(model: String): String {
        return when {
            model.contains("پژو ۲۰۶") || model.contains("پژو ۲۰۷") || model.contains("رانا") -> "TU5 / TU3"
            model.contains("سمند") || model.contains("دنا") -> "EF7 / XU7"
            model.contains("پراید") || model.contains("تیبا") || model.contains("ساینا") -> "M13 / M15"
            model.contains("تارا") || model.contains("شاهین") -> "TU5P / M15TC"
            else -> "استاندارد"
        }
    }

    fun deleteVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            dao.deleteVehicle(vehicle)
        }
    }

    fun selectVehicle(vehicleId: Int) {
        viewModelScope.launch {
            dao.clearActiveStatus()
            dao.setActive(vehicleId)
        }
    }

    fun syncOdometer(vehicleId: Int, currentMileage: Int) {
        viewModelScope.launch {
            val vehicles = allVehicles.value
            val vehicle = vehicles.find { it.id == vehicleId } ?: return@launch
            dao.updateVehicle(vehicle.copy(mileage = currentMileage.toDouble()))
        }
    }
}
