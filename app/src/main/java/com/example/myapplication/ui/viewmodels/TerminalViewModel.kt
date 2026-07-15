package com.example.myapplication.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.obd.ObdBluetoothManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TerminalLine(val text: String, val type: LineType)
enum class LineType { COMMAND, RESPONSE, ERROR }

class TerminalViewModel(private val obdBluetoothManager: ObdBluetoothManager) : ViewModel() {
    private val _history = MutableStateFlow<List<TerminalLine>>(emptyList())
    val history: StateFlow<List<TerminalLine>> = _history.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    fun sendCommand(command: String) {
        if (command.isBlank()) return
        
        viewModelScope.launch {
            _isSending.value = true
            _history.value = _history.value + TerminalLine("> $command", LineType.COMMAND)
            
            val response = obdBluetoothManager.sendRawCommand(command)
            val type = if (response.startsWith("Error:")) LineType.ERROR else LineType.RESPONSE
            _history.value = _history.value + TerminalLine(response, type)
            
            _isSending.value = false
        }
    }

    fun clearHistory() {
        _history.value = emptyList()
    }
}
