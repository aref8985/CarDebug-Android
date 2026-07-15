package com.example.myapplication.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dtcs")
data class Dtc(
    @PrimaryKey val code: String,
    val descriptionEn: String,
    val descriptionFa: String,
    val brand: String? = null // e.g., "IKCO", "SAIPA", "GENERIC"
)
