package com.example.myapplication

import android.app.Application
import com.example.myapplication.obd.ObdBluetoothManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CarDiagApp : Application() {
    // Lazy initialization or background initialization to prevent blocking the main thread
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    lateinit var obdBluetoothManager: ObdBluetoothManager
        private set

    override fun onCreate() {
        super.onCreate()
        // Initialize Bluetooth manager on the main thread is generally fast, 
        // but the internal adapter setup is done inside the manager.
        obdBluetoothManager = ObdBluetoothManager(this)
        
        // Background initialization for heavy tasks if any
        appScope.launch {
            // E.g. Warm up database or other non-UI services
        }
    }
}
