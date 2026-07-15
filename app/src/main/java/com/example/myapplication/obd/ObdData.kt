package com.example.myapplication.obd

data class ObdData(
    val rpm: Int = 0,
    val speed: Int = 0,
    val fuelTrim: Float = 0f,
    val coolantTemp: Int = 0,
    val load: Float = 0f,
    val voltage: Float = 0f,
    val intakeTemp: Int = 0,
    val throttle: Float = 0f,
    val timingAdvance: Float = 0f,
    // پارامترهای محاسباتی جدید
    val tripDistance: Double = 0.0,       // مسافت سفر فعلی (کیلومتر)
    val instantFuelConsumption: Float = 0f // مصرف سوخت لحظه‌ای (L/100km)
)
