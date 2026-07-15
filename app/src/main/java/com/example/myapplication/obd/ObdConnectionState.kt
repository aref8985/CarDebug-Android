package com.example.myapplication.obd

sealed class ObdConnectionState {
    object Disconnected : ObdConnectionState()
    object Connecting : ObdConnectionState()
    object Connected : ObdConnectionState()
    data class Error(val message: String) : ObdConnectionState()
}
