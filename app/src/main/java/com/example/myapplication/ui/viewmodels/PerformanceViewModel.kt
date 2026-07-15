package com.example.myapplication.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.obd.ObdBluetoothManager
import com.example.myapplication.obd.ObdConnectionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class TimerState { READY, RUNNING, FINISHED }

data class PerformanceMetrics(
    val time0to60: Long = 0L,
    val time0to100: Long = 0L,
    val currentGForce: Float = 0f,
    val speedHistory: List<Pair<Long, Int>> = emptyList()
)

class PerformanceViewModel(private val obdBluetoothManager: ObdBluetoothManager) : ViewModel() {
    
    private val _timerState = MutableStateFlow(TimerState.READY)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()
    
    private val _currentTime = MutableStateFlow(0L)
    val currentTime: StateFlow<Long> = _currentTime.asStateFlow()
    
    private val _metrics = MutableStateFlow(PerformanceMetrics())
    val metrics: StateFlow<PerformanceMetrics> = _metrics.asStateFlow()

    private val _personalBest = MutableStateFlow(0L)
    val personalBest: StateFlow<Long> = _personalBest.asStateFlow()

    val connectionState = obdBluetoothManager.connectionState

    private var timerJob: Job? = null
    private var startTime = 0L
    private var lastSpeed = 0
    private var lastUpdateTime = 0L
    private var gForceBuffer = mutableListOf<Float>()

    init {
        viewModelScope.launch {
            obdBluetoothManager.obdData.collect { data ->
                handleSpeedUpdate(data.speed)
            }
        }

        viewModelScope.launch {
            connectionState.collect { state ->
                if (state !is ObdConnectionState.Connected) {
                    resetTimer()
                }
            }
        }
    }

    private fun handleSpeedUpdate(speed: Int) {
        val now = System.currentTimeMillis()
        
        if (lastUpdateTime != 0L && now > lastUpdateTime) {
            val deltaV = (speed - lastSpeed) / 3.6f
            val deltaT = (now - lastUpdateTime) / 1000f
            if (deltaT > 0) {
                val acceleration = deltaV / deltaT
                val gForce = acceleration / 9.81f
                
                gForceBuffer.add(gForce)
                if (gForceBuffer.size > 5) gForceBuffer.removeAt(0)
                val smoothedG = gForceBuffer.average().toFloat()
                
                _metrics.value = _metrics.value.copy(currentGForce = smoothedG)
            }
        }
        lastSpeed = speed
        lastUpdateTime = now

        when (_timerState.value) {
            TimerState.READY -> {
                if (speed > 2) {
                    startTimer()
                }
            }
            TimerState.RUNNING -> {
                val elapsed = now - startTime
                
                if (speed >= 60 && _metrics.value.time0to60 == 0L) {
                    _metrics.value = _metrics.value.copy(time0to60 = elapsed)
                }
                
                val currentHistory = _metrics.value.speedHistory.toMutableList()
                currentHistory.add(Pair(elapsed, speed))
                // Performance safeguard: Limit history points
                if (currentHistory.size > 200) currentHistory.removeAt(0)
                _metrics.value = _metrics.value.copy(speedHistory = currentHistory)

                if (speed >= 100) {
                    stopTimer(elapsed)
                } else if (speed == 0 && elapsed > 5000) {
                    resetTimer()
                }
            }
            TimerState.FINISHED -> {
                if (speed == 0) {
                    _timerState.value = TimerState.READY
                }
            }
        }
    }

    private fun startTimer() {
        startTime = System.currentTimeMillis()
        _timerState.value = TimerState.RUNNING
        _metrics.value = PerformanceMetrics()
        timerJob = viewModelScope.launch {
            while (true) {
                _currentTime.value = System.currentTimeMillis() - startTime
                delay(16)
            }
        }
    }

    private fun stopTimer(finalTime: Long) {
        timerJob?.cancel()
        _timerState.value = TimerState.FINISHED
        _currentTime.value = finalTime
        _metrics.value = _metrics.value.copy(time0to100 = finalTime)
        
        if (_personalBest.value == 0L || finalTime < _personalBest.value) {
            _personalBest.value = finalTime
        }
    }

    fun resetTimer() {
        timerJob?.cancel()
        _timerState.value = TimerState.READY
        _currentTime.value = 0L
        _metrics.value = PerformanceMetrics()
        gForceBuffer.clear()
    }
}
