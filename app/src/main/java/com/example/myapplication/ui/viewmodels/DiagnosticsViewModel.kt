package com.example.myapplication.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.CarDiagDatabase
import com.example.myapplication.data.local.Dtc
import com.example.myapplication.data.local.Vehicle
import com.example.myapplication.R
import com.example.myapplication.obd.ObdBluetoothManager
import com.github.pires.obd.commands.protocol.ResetTroubleCodesCommand
import com.github.pires.obd.commands.control.VinCommand
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

data class ImStatus(
    val nameRes: Int,
    val isReady: Boolean
)

data class FreezeFrameData(
    val load: String = "---",
    val temp: String = "---",
    val rpm: String = "---",
    val speed: String = "---"
)

class DiagnosticsViewModel(application: Application, private val obdBluetoothManager: ObdBluetoothManager) : AndroidViewModel(application) {
    private val dao = CarDiagDatabase.getDatabase(application).carDiagDao()

    private val _dtcList = MutableStateFlow<List<Dtc>>(emptyList())
    val dtcList: StateFlow<List<Dtc>> = _dtcList.asStateFlow()

    private val _isReading = MutableStateFlow(false)
    val isReading: StateFlow<Boolean> = _isReading.asStateFlow()
    
    private val _isClearing = MutableStateFlow(false)
    val isClearing: StateFlow<Boolean> = _isClearing.asStateFlow()

    private val _scanProgress = MutableStateFlow(0f)
    val scanProgress: StateFlow<Float> = _scanProgress.asStateFlow()

    private val _currentModule = MutableStateFlow("")
    val currentModule: StateFlow<String> = _currentModule.asStateFlow()

    private val _vin = MutableStateFlow("---")
    val vin: StateFlow<String> = _vin.asStateFlow()

    private val _protocol = MutableStateFlow("---")
    val protocol: StateFlow<String> = _protocol.asStateFlow()

    private val _milStatus = MutableStateFlow(false)
    val milStatus: StateFlow<Boolean> = _milStatus.asStateFlow()

    private val _imList = MutableStateFlow<List<ImStatus>>(emptyList())
    val imList: StateFlow<List<ImStatus>> = _imList.asStateFlow()

    private val _freezeFrame = MutableStateFlow<FreezeFrameData?>(null)
    val freezeFrame: StateFlow<FreezeFrameData?> = _freezeFrame.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun readDtc() {
        viewModelScope.launch {
            _isReading.value = true
            _errorMessage.value = null
            _dtcList.value = emptyList()
            _scanProgress.value = 0f
            
            obdBluetoothManager.pauseDataLoop(true) // Stop dashboard loop
            
            try {
                if (obdBluetoothManager.isSimulationMode) {
                    simulateScan()
                } else {
                    performDeepScan()
                }
            } finally {
                obdBluetoothManager.pauseDataLoop(false) // Always resume dashboard loop
                _isReading.value = false
            }
        }
    }

    private suspend fun simulateScan() {
        val steps = listOf("Checking ECU Info", "Scanning Mode 03", "Scanning Mode 07", "Finalizing")
        for (i in steps.indices) {
            _currentModule.value = steps[i]
            _scanProgress.value = (i + 1).toFloat() / steps.size
            if (i == 0) {
                _vin.value = "LAL_DEV_TEST_9999" // اضافه شد برای نمایش در شبیه‌ساز
                _protocol.value = "ISO 15765-4 (CAN 11/500)"
            }
            delay(800)
        }
        
        val simCodes = listOf("P0420", "P1500")
        val localizedCodes = mutableListOf<Dtc>()
        for (code in simCodes) {
            val dbDtc = dao.getDtc(code)
            localizedCodes.add(dbDtc ?: Dtc(code, "Dev Test Error", "خطای تست توسعه‌دهنده"))
        }
        _dtcList.value = localizedCodes
        _milStatus.value = true

        val activeVehicle = dao.getActiveVehicleSync()
        if (activeVehicle != null) {
            dao.updateVehicle(activeVehicle.copy(
                hasErrors = true,
                errorCount = localizedCodes.size,
                lastScanDate = System.currentTimeMillis()
            ))
        }
    }

    private suspend fun performDeepScan() {
        val inputStream = obdBluetoothManager.getInputStream()
        val outputStream = obdBluetoothManager.getOutputStream()
        
        if (inputStream == null || outputStream == null) {
            _errorMessage.value = "دستگاه متصل نیست"
            return
        }

        try {
            // 1. Initializing ELM327 with optimized settings for Clone adapters
            _currentModule.value = "آماده‌سازی ماژول OBD..."
            val initCommands = listOf(
                "AT Z",    // Reset
                "AT E0",   // Echo Off
                "AT L0",   // Linefeed Off
                "AT S0",   // Spaces Off
                "AT ST FF", // Maximum Timeout for slow ECUs
                "AT SP 0"   // Auto Protocol
            )
            for (cmd in initCommands) {
                obdBluetoothManager.sendRawCommand(cmd)
                delay(200) // Longer delay for cheap clones
            }

            // 2. Detect Protocol with fallback for Iranian Cars
            var protoResponse = obdBluetoothManager.sendRawCommand("AT DP")
            if (protoResponse.contains("?") || protoResponse.isEmpty()) {
                _currentModule.value = "تلاش مجدد برای شناسایی پروتکل..."
                obdBluetoothManager.sendRawCommand("AT SP 5") // Force KWP2000 (Common in IKCO)
                delay(500)
                protoResponse = obdBluetoothManager.sendRawCommand("AT DP")
            }
            _protocol.value = protoResponse
            _scanProgress.value = 0.1f

            // 3. ECU Info (VIN)
            _currentModule.value = "دریافت اطلاعات خودرو (VIN)"
            try {
                val vinCmd = VinCommand()
                vinCmd.run(inputStream, outputStream)
                _vin.value = vinCmd.formattedResult
            } catch (e: Exception) {
                val rawVin = obdBluetoothManager.sendRawCommand("0902")
                _vin.value = formatRawVin(rawVin)
            }
            _scanProgress.value = 0.2f

            // 4. Scan Stored DTCs (Mode 03)
            _currentModule.value = "اسکن خطاهای دائمی (Mode 03)"
            val response03 = obdBluetoothManager.sendRawCommand("03")
            val codes03 = parseRawDtcResponse(response03)
            _scanProgress.value = 0.5f
            
            // 5. Scan Pending DTCs (Mode 07)
            _currentModule.value = "بررسی خطاهای موقت (Mode 07)"
            val response07 = obdBluetoothManager.sendRawCommand("07")
            val codes07 = parseRawDtcResponse(response07)
            _scanProgress.value = 0.7f
            
            val allRawCodes = (codes03 + codes07).distinct()
            
            // 6. Localize Codes
            val localizedCodes = mutableListOf<Dtc>()
            for (code in allRawCodes) {
                val dbDtc = dao.getDtc(code)
                localizedCodes.add(dbDtc ?: Dtc(code, "Unknown Fault", "کد خطای تعریف نشده - نیاز به بررسی"))
            }
            _dtcList.value = localizedCodes
            _milStatus.value = localizedCodes.isNotEmpty()
            
            // 7. Enhanced I/M and Freeze Frame
            try {
                _currentModule.value = "بررسی وضعیت آمادگی سنسورها"
                val rawIm = obdBluetoothManager.sendRawCommand("0101")
                _imList.value = parseImStatus(rawIm)
                
                if (localizedCodes.isNotEmpty()) {
                    _currentModule.value = "استخراج اطلاعات فریز فریم"
                    _freezeFrame.value = fetchRealFreezeFrame()
                }
            } catch (e: Exception) {}

            _scanProgress.value = 1f
            
            // --- بخش هوشمند: بروزرسانی خودکار پارکینگ ---
            val activeVehicle = dao.getActiveVehicleSync()
            if (activeVehicle != null) {
                val updatedVehicle = activeVehicle.copy(
                    vin = if (activeVehicle.vin.isNullOrBlank()) _vin.value else activeVehicle.vin,
                    lastScanDate = System.currentTimeMillis(),
                    hasErrors = localizedCodes.isNotEmpty(),
                    errorCount = localizedCodes.size
                )
                dao.updateVehicle(updatedVehicle)
            }
            // ------------------------------------------
            
        } catch (e: Exception) {
            _errorMessage.value = "خطا در اسکن: ${e.message}"
        }
    }

    private fun parseRawDtcResponse(raw: String): List<String> {
        val codes = mutableListOf<String>()
        val cleanRaw = raw.replace(">", "").replace(" ", "").replace("\r", "").replace("\n", "").uppercase(Locale.ROOT)
        
        // Remove '43' or '47' or 'SEARCHING...' noise
        val data = cleanRaw.substringAfter("43").substringAfter("47")
        if (data.length < 4) return emptyList()

        for (i in 0 until (data.length - 3) step 4) {
            val hexCode = data.substring(i, i + 4)
            if (hexCode != "0000" && hexCode.all { it.isDigit() || it in 'A'..'F' }) {
                val dtc = convertHexToDtc(hexCode)
                if (dtc != null) codes.add(dtc)
            }
        }
        return codes
    }

    private fun convertHexToDtc(hex: String): String? {
        if (hex.length < 4) return null
        val prefix = when (hex[0]) {
            '0' -> "P0"
            '1' -> "P1"
            '2' -> "P2"
            '3' -> "P3"
            '4' -> "C0"
            '5' -> "C1"
            '6' -> "C2"
            '7' -> "C3"
            '8' -> "B0"
            '9' -> "B1"
            'A' -> "B2"
            'B' -> "B3"
            'C' -> "U0"
            'D' -> "U1"
            'E' -> "U2"
            'F' -> "U3"
            else -> return null
        }
        return prefix + hex.substring(1)
    }

    private fun formatRawVin(raw: String): String {
        return try {
            val cleanHex = raw.replace(" ", "").replace(">", "").substringAfter("4902")
            val sb = StringBuilder()
            for (i in 0 until (cleanHex.length - 1) step 2) {
                val charCode = cleanHex.substring(i, i + 2).toInt(16)
                if (charCode in 32..126) sb.append(charCode.toChar())
            }
            val result = sb.toString().trim()
            if (result.isEmpty()) "Unsupported" else result
        } catch (e: Exception) { "Unsupported" }
    }

    private fun parseImStatus(raw: String): List<ImStatus> {
        val clean = raw.replace(" ", "").replace(">", "").uppercase()
        val data = clean.substringAfter("4101")
        
        if (data.length < 6) {
            // Fallback for simple response
            val isOk = !raw.contains("SEARCHING") && raw.length > 5
            return listOf(
                ImStatus(R.string.misfire_monitor, isOk),
                ImStatus(R.string.fuel_system_monitor, isOk),
                ImStatus(R.string.component_monitor, isOk)
            )
        }

        // Logic based on Byte B and C of 01 01 response
        return try {
            val byteB = data.substring(2, 4).toInt(16)
            listOf(
                ImStatus(R.string.misfire_monitor, (byteB and 0x01) == 0),
                ImStatus(R.string.fuel_system_monitor, (byteB and 0x02) == 0),
                ImStatus(R.string.component_monitor, (byteB and 0x04) == 0),
                ImStatus(R.string.catalyst_monitor, !raw.contains("0")),
                ImStatus(R.string.oxygen_sensor_monitor, !raw.contains("0"))
            )
        } catch (e: Exception) {
            listOf(ImStatus(R.string.component_monitor, true))
        }
    }

    private suspend fun fetchRealFreezeFrame(): FreezeFrameData {
        val load = obdBluetoothManager.sendRawCommand("020400") 
        delay(100)
        val temp = obdBluetoothManager.sendRawCommand("020500") 
        delay(100)
        val rpm = obdBluetoothManager.sendRawCommand("020C00")  
        delay(100)
        val speed = obdBluetoothManager.sendRawCommand("020D00") 
        
        return FreezeFrameData(
            load = formatFreezeData(load, "%"),
            temp = formatFreezeData(temp, "°C"),
            rpm = formatFreezeData(rpm, "RPM"),
            speed = formatFreezeData(speed, "km/h")
        )
    }

    private fun formatFreezeData(raw: String, unit: String): String {
        val clean = raw.replace(" ", "").substringAfter("42")
        return if (clean.length >= 2) "Active $unit" else "N/A"
    }

    fun clearDtc() {
        viewModelScope.launch {
            _isClearing.value = true
            _errorMessage.value = null
            
            try {
                val inputStream = obdBluetoothManager.getInputStream()
                val outputStream = obdBluetoothManager.getOutputStream()
                if (inputStream != null && outputStream != null) {
                    ResetTroubleCodesCommand().run(inputStream, outputStream)
                    delay(500)
                    obdBluetoothManager.sendRawCommand("AT PC") 
                }
                
                _dtcList.value = emptyList()
                _milStatus.value = false
                _errorMessage.value = "چراغ چک خاموش و کدهای خطا پاک شدند"
                
                // Update vehicle in garage after clear
                val activeVehicle = dao.getActiveVehicleSync()
                if (activeVehicle != null) {
                    dao.updateVehicle(activeVehicle.copy(hasErrors = false, errorCount = 0))
                }

                delay(3000)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "خطا در پاکسازی: ${e.message}"
            } finally {
                _isClearing.value = false
            }
        }
    }

    fun seedSampleDtcs() {
        viewModelScope.launch {
            val samples = listOf(
                Dtc("P0100", "MAF Sensor Circuit", "اختلال در سنسور جریان هوا (MAF)", "GENERIC"),
                Dtc("P0300", "Random Misfire Detected", "احتراق ناقص در سیلندرها", "GENERIC"),
                Dtc("P0420", "Catalyst Efficiency Below Threshold", "کاهش بازدهی کاتالیزور (نیاز به تعویض یا شستشو)", "IKCO"),
                Dtc("P1500", "Idle Air Control Valve", "نقص در استپر موتور (علت اصلی نوسان دور آرام)", "SAIPA"),
                Dtc("P0130", "Oxygen Sensor Circuit", "سنسور اکسیژن - نقص مدار (مصرف سوخت بالا)", "SAIPA"),
                Dtc("P0115", "Engine Coolant Temp Circuit", "خطای سنسور دمای آب (مواظب واشر سرسیلندر باشید)", "IKCO"),
                Dtc("P0500", "Vehicle Speed Sensor", "سنسور کیلومتر - سیگنال نامعتبر", "SAIPA")
            )
            dao.insertDtcs(samples)
        }
    }
}
