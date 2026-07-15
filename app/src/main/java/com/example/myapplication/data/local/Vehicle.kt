package com.example.myapplication.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val brand: String,
    val model: String,
    val year: String? = null,
    val vin: String? = null,
    val obdProtocol: String = "AUTO",
    val connectionType: String = "BLUETOOTH",
    val lastScanDate: Long? = null,
    val isActive: Boolean = false,
    val mileage: Double = 0.0,
    val engineType: String? = null,
    val hasErrors: Boolean = false,
    val errorCount: Int = 0,
    val lastOilChange: Int = 0,
    val lastBeltChange: Int = 0
)
