package com.example.myapplication.navigation

import kotlinx.serialization.Serializable
import androidx.navigation3.runtime.NavKey

@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object Dashboard : Route

    @Serializable
    data object Diagnostics : Route

    @Serializable
    data object Garage : Route

    @Serializable
    data object Performance : Route

    @Serializable
    data class VehicleDetail(val vehicleId: Int) : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object Terminal : Route
}
