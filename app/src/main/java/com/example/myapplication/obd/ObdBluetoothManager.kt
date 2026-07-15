package com.example.myapplication.obd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.example.myapplication.data.local.CarDiagDatabase
import com.example.myapplication.data.local.Vehicle
import com.github.pires.obd.commands.protocol.*
import com.github.pires.obd.enums.ObdProtocols
import com.github.pires.obd.commands.engine.RPMCommand
import com.github.pires.obd.commands.SpeedCommand
import com.github.pires.obd.commands.fuel.FuelTrimCommand
import com.github.pires.obd.commands.engine.LoadCommand
import com.github.pires.obd.commands.engine.ThrottlePositionCommand
import com.github.pires.obd.commands.temperature.AirIntakeTemperatureCommand
import com.github.pires.obd.commands.temperature.EngineCoolantTemperatureCommand
import com.github.pires.obd.commands.control.ModuleVoltageCommand
import com.github.pires.obd.commands.control.TimingAdvanceCommand
import com.github.pires.obd.enums.FuelTrim
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.util.UUID
import kotlin.random.Random

class ObdBluetoothManager(private val context: Context) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val dao = CarDiagDatabase.getDatabase(context).carDiagDao()

    private val obdMutex = Mutex()
    private var isDataLoopPaused = false

    private val _connectionState = MutableStateFlow<ObdConnectionState>(ObdConnectionState.Disconnected)
    val connectionState: StateFlow<ObdConnectionState> = _connectionState.asStateFlow()

    private val _obdData = MutableStateFlow(ObdData())
    val obdData: StateFlow<ObdData> = _obdData.asStateFlow()

    private val _detectedVin = MutableStateFlow<String?>(null)
    val detectedVin: StateFlow<String?> = _detectedVin.asStateFlow()

    private var socket: BluetoothSocket? = null
    private var lastConnectedDevice: BluetoothDevice? = null
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    
    var isSimulationMode = false
        private set
        
    private var simulationJob: Job? = null
    private var dataJob: Job? = null
    private var reconnectJob: Job? = null

    // Tracking for Distance calculation
    private var totalTripDistance = 0.0
    private var lastDistanceUpdateTime = 0L

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        return bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
    }

    @SuppressLint("MissingPermission")
    suspend fun connect(device: BluetoothDevice) {
        disconnect(isManual = true)
        lastConnectedDevice = device
        isSimulationMode = false
        performConnection(device)
    }

    @SuppressLint("MissingPermission")
    private suspend fun performConnection(device: BluetoothDevice) {
        withContext(Dispatchers.IO) {
            _connectionState.value = ObdConnectionState.Connecting
            try {
                socket = try {
                    device.createRfcommSocketToServiceRecord(SPP_UUID)
                } catch (e: Exception) {
                    val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                    method.invoke(device, 1) as BluetoothSocket
                }

                bluetoothAdapter?.cancelDiscovery()
                withTimeout(15000) { socket?.connect() }

                if (socket?.isConnected == true) {
                    initializeObdPro()
                } else {
                    throw IOException("Socket connection failed")
                }
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: "خطا در اتصال"
                _connectionState.value = ObdConnectionState.Error(errorMsg)
                closeConnection()
                scheduleReconnect()
            }
        }
    }

    private fun scheduleReconnect() {
        if (isSimulationMode || reconnectJob?.isActive == true) return
        reconnectJob = CoroutineScope(Dispatchers.IO).launch {
            delay(8000)
            lastConnectedDevice?.let { performConnection(it) }
        }
    }

    private suspend fun initializeObdPro() {
        try {
            val initCommands = listOf("AT Z", "AT E0", "AT L0", "AT S0", "AT AL", "AT ST FF", "AT SP 0")
            for (cmd in initCommands) {
                delay(250)
                val response = sendRawCommand(cmd)
                if (response.contains("UNABLE TO CONNECT", ignoreCase = true)) {
                    _connectionState.value = ObdConnectionState.Error("سوییچ خودرو را باز کنید")
                    return
                }
            }
            sendRawCommandNoResponse("AT CAF 0")
            
            delay(500)
            val rawVin = sendRawCommand("0902")
            val vin = formatRawVin(rawVin)
            
            if (vin.length >= 10) {
                _detectedVin.value = vin
                handleAutoVehicleDetection(vin)
            } else {
                handleAutoVehicleDetection("LEGACY_VEHICLE_${System.currentTimeMillis() % 10000}")
            }

            _connectionState.value = ObdConnectionState.Connected
            startDataLoop()
        } catch (e: Exception) {
            _connectionState.value = ObdConnectionState.Error("خطا در تنظیمات دانگل: ${e.message}")
            scheduleReconnect()
        }
    }

    private suspend fun handleAutoVehicleDetection(vin: String) {
        val existingVehicle = dao.getVehicleByVin(vin)
        if (existingVehicle != null) {
            dao.clearActiveStatus()
            dao.setActive(existingVehicle.id)
        } else {
            val isLegacy = vin.startsWith("LEGACY")
            val (brand, model) = when {
                vin.startsWith("LAL") || vin.startsWith("L9A") -> "ایران خودرو" to "پژو / سمند / دنا"
                vin.startsWith("NAP") -> "سایپا" to "پراید / کوییک / شاهین"
                vin.startsWith("PL7") -> "پارس خودرو" to "ال ۹۰ / ساندرو"
                isLegacy -> "خودروی استاندارد" to "پروتکل قدیمی"
                else -> "خودروی خارجی" to "شناسایی شده"
            }
            val newVehicle = Vehicle(
                name = "$brand $model",
                brand = brand,
                model = model,
                vin = if (isLegacy) null else vin,
                isActive = true,
                engineType = if (brand == "ایران خودرو") "EF7 / TU5" else if (brand == "سایپا") "M13 / M15" else "استاندارد"
            )
            dao.clearActiveStatus()
            dao.insertVehicle(newVehicle)
        }
    }

    private suspend fun startDataLoop() {
        dataJob = CoroutineScope(Dispatchers.IO).launch {
            val s = socket ?: return@launch
            val rpmCmd = RPMCommand()
            val speedCmd = SpeedCommand()
            val coolantCmd = EngineCoolantTemperatureCommand()
            val voltageCmd = ModuleVoltageCommand()
            val loadCmd = LoadCommand()
            val throttleCmd = ThrottlePositionCommand()
            val intakeTempCmd = AirIntakeTemperatureCommand()
            val fuelTrimCmd = FuelTrimCommand(FuelTrim.SHORT_TERM_BANK_1)
            val timingCmd = TimingAdvanceCommand()

            var cycleCount = 0
            totalTripDistance = 0.0
            lastDistanceUpdateTime = System.currentTimeMillis()

            while (isActive && _connectionState.value == ObdConnectionState.Connected) {
                if (isDataLoopPaused) {
                    delay(500)
                    continue
                }

                obdMutex.withLock {
                    try {
                        rpmCmd.run(s.inputStream, s.outputStream)
                        speedCmd.run(s.inputStream, s.outputStream)
                        
                        val now = System.currentTimeMillis()
                        val deltaT = (now - lastDistanceUpdateTime) / (1000.0 * 60.0 * 60.0)
                        if (deltaT > 0 && speedCmd.metricSpeed > 0) {
                            val addedKm = speedCmd.metricSpeed * deltaT
                            totalTripDistance += addedKm
                            if (cycleCount % 50 == 0) updatePersistedMileage(addedKm)
                        }
                        lastDistanceUpdateTime = now
                        
                        if (cycleCount % 3 == 0) {
                            loadCmd.run(s.inputStream, s.outputStream)
                            throttleCmd.run(s.inputStream, s.outputStream)
                            try { timingCmd.run(s.inputStream, s.outputStream) } catch (e: Exception) {}
                        }

                        if (cycleCount % 10 == 0) {
                            voltageCmd.run(s.inputStream, s.outputStream)
                            coolantCmd.run(s.inputStream, s.outputStream)
                            try { intakeTempCmd.run(s.inputStream, s.outputStream) } catch (e: Exception) {}
                            try { fuelTrimCmd.run(s.inputStream, s.outputStream) } catch (e: Exception) {}
                        }

                        _obdData.value = ObdData(
                            rpm = rpmCmd.rpm,
                            speed = speedCmd.metricSpeed,
                            tripDistance = totalTripDistance,
                            timingAdvance = try { timingCmd.percentage } catch (e: Exception) { 0f },
                            load = if (cycleCount % 3 == 0) loadCmd.percentage else _obdData.value.load,
                            throttle = if (cycleCount % 3 == 0) throttleCmd.percentage else _obdData.value.throttle,
                            voltage = if (cycleCount % 10 == 0) voltageCmd.voltage.toFloat() else _obdData.value.voltage,
                            coolantTemp = if (cycleCount % 10 == 0) coolantCmd.temperature.toInt() else _obdData.value.coolantTemp,
                            intakeTemp = if (cycleCount % 10 == 0) intakeTempCmd.temperature.toInt() else _obdData.value.intakeTemp,
                            fuelTrim = if (cycleCount % 10 == 0) fuelTrimCmd.value else _obdData.value.fuelTrim,
                            instantFuelConsumption = calculateFuel(rpmCmd.rpm, loadCmd.percentage, speedCmd.metricSpeed)
                        )
                        cycleCount = (cycleCount + 1) % 100
                    } catch (e: Exception) {
                        if (e is IOException || e.message?.contains("STOPPED") == true) {
                            _connectionState.value = ObdConnectionState.Error("ارتباط با خودرو قطع شد")
                            closeConnection()
                            scheduleReconnect()
                        }
                    }
                }
                delay(150)
            }
        }
    }

    fun pauseDataLoop(pause: Boolean) {
        isDataLoopPaused = pause
    }

    private suspend fun updatePersistedMileage(addedKm: Double) {
        val vehicle = dao.getActiveVehicleSync() ?: return
        dao.updateVehicle(vehicle.copy(mileage = vehicle.mileage + addedKm))
    }

    private fun calculateFuel(rpm: Int, load: Float, speed: Int): Float {
        if (rpm < 500) return 0f
        val fuelPerHour = (rpm * load) / 14000f 
        return if (speed > 5) (fuelPerHour / speed) * 100 else fuelPerHour
    }

    suspend fun startDeveloperTest() {
        disconnect(isManual = true)
        isSimulationMode = true
        _connectionState.value = ObdConnectionState.Connecting
        delay(1000)
        val vin = "LAL_DEV_TEST_9999"
        _detectedVin.value = vin
        handleAutoVehicleDetection(vin)
        _connectionState.value = ObdConnectionState.Connected
        simulationJob = CoroutineScope(Dispatchers.IO).launch {
            var simRpm = 800f
            var simSpeed = 0f
            while (isActive && isSimulationMode) {
                simRpm += Random.nextInt(-20, 30)
                if (simRpm < 750) simRpm = 800f
                if (simSpeed < 120) simSpeed += 1.2f else simSpeed = 0f

                _obdData.value = ObdData(
                    rpm = simRpm.toInt(),
                    speed = simSpeed.toInt(),
                    coolantTemp = 85 + Random.nextInt(10),
                    voltage = 13.8f + Random.nextFloat(),
                    load = 20f + Random.nextFloat() * 10f,
                    throttle = 15f + Random.nextFloat() * 5f,
                    intakeTemp = 35 + Random.nextInt(5),
                    fuelTrim = Random.nextFloat() * 2 - 1f,
                    timingAdvance = 12.5f + Random.nextFloat(),
                    tripDistance = simSpeed.toDouble() / 10.0,
                    instantFuelConsumption = 6.5f + Random.nextFloat()
                )
                delay(200)
            }
        }
    }

    fun disconnect(isManual: Boolean = false) {
        _connectionState.value = ObdConnectionState.Disconnected
        if (isManual) {
            lastConnectedDevice = null
            reconnectJob?.cancel()
        }
        isSimulationMode = false
        simulationJob?.cancel()
        dataJob?.cancel()
        closeConnection()
    }

    fun getInputStream() = socket?.inputStream
    fun getOutputStream() = socket?.outputStream

    private fun closeConnection() {
        try { socket?.close() } catch (e: Exception) {}
        socket = null
    }

    private fun sendRawCommandNoResponse(command: String) {
        try {
            socket?.outputStream?.write("$command\r".toByteArray())
            socket?.outputStream?.flush()
        } catch (e: Exception) {}
    }

    suspend fun sendRawCommand(command: String): String {
        if (isSimulationMode) return "49 02 01 4C 41 4C 5F 44 45 56"
        return obdMutex.withLock {
            withContext(Dispatchers.IO) {
                try {
                    val os = socket?.outputStream ?: return@withContext ""
                    val `is` = socket?.inputStream ?: return@withContext ""
                    os.write("$command\r".toByteArray())
                    os.flush()
                    val response = StringBuilder()
                    withTimeout(4000) {
                        var c: Int
                        while (true) {
                            c = `is`.read()
                            if (c == -1 || c.toChar() == '>') break
                            response.append(c.toChar())
                        }
                    }
                    response.toString().replace(Regex("[^\\p{Print}]"), "").trim()
                } catch (e: Exception) { "" }
            }
        }
    }

    private fun formatRawVin(raw: String): String {
        return try {
            val hex = raw.replace(" ", "").replace(">", "").substringAfter("4902")
            val sb = StringBuilder()
            for (i in 0 until (hex.length - 1) step 2) {
                val charCode = hex.substring(i, i + 2).toInt(16).toChar()
                if (charCode.isLetterOrDigit()) sb.append(charCode)
            }
            sb.toString()
        } catch (e: Exception) { "" }
    }
}
