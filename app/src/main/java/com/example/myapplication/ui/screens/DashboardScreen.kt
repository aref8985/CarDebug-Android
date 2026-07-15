package com.example.myapplication.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.obd.ObdConnectionState
import com.example.myapplication.ui.viewmodels.ObdViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.ui.components.CircularGauge
import com.example.myapplication.ui.components.RealTimeGraph
import com.example.myapplication.CarDiagApp
import com.example.myapplication.R
import com.example.myapplication.data.settings.SettingsRepository
import com.example.myapplication.data.settings.UnitSystem

@SuppressLint("MissingPermission")
@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as CarDiagApp
    
    val obdViewModel: ObdViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ObdViewModel(app.obdBluetoothManager, app) as T
            }
        }
    )

    val connectionState by obdViewModel.connectionState.collectAsState()
    val obdData by obdViewModel.obdData.collectAsState()
    val rpmHistory by obdViewModel.rpmHistory.collectAsState()
    val maxRpm by obdViewModel.maxRpm.collectAsState()
    val maxSpeed by obdViewModel.maxSpeed.collectAsState()
    val activeVehicle by obdViewModel.activeVehicle.collectAsState()
    
    val repository = remember { SettingsRepository(context) }
    val units by repository.unitsFlow.collectAsState(initial = UnitSystem.METRIC)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.dashboard), 
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                activeVehicle?.let {
                    Text(it.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            
            if (connectionState is ObdConnectionState.Connected) {
                FilledTonalIconButton(
                    onClick = { obdViewModel.resetMaxValues() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Reset", modifier = Modifier.size(20.dp))
                }
            }
        }

        StatusCard(connectionState)

        Spacer(modifier = Modifier.height(12.dp))

        if (connectionState is ObdConnectionState.Connected) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Main Gauges in a styled card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        WarningSection(obdData)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularGauge(
                                    value = obdData.rpm.toFloat(),
                                    maxValue = 8000f,
                                    label = stringResource(R.string.sensor_engine),
                                    unit = stringResource(R.string.unit_rpm),
                                    primaryColor = if (obdData.rpm > 5000) Color.Red else MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    stringResource(R.string.max_val, maxRpm), 
                                    style = MaterialTheme.typography.labelSmall, 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                val displaySpeed = if (units == UnitSystem.METRIC) obdData.speed.toFloat() else obdData.speed * 0.621371f
                                val speedUnit = if (units == UnitSystem.METRIC) "km/h" else "mph"
                                
                                CircularGauge(
                                    value = displaySpeed,
                                    maxValue = if (units == UnitSystem.METRIC) 240f else 150f,
                                    label = stringResource(R.string.sensor_speed),
                                    unit = speedUnit,
                                )
                                Text(
                                    stringResource(R.string.max_val, if (units == UnitSystem.METRIC) maxSpeed else (maxSpeed * 0.621371f).toInt()),
                                    style = MaterialTheme.typography.labelSmall, 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                // RPM Real-time Graph
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    RealTimeGraph(
                        data = rpmHistory,
                        label = stringResource(R.string.rpm_live_trace),
                        maxValue = 8000f,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                // Detailed Sensors
                SensorGrid(obdData, units)
                
                // Trip Summary Card
                TripSummaryCard(obdData)
                
                Button(
                    onClick = { obdViewModel.disconnect() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer, 
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(Icons.Rounded.BluetoothDisabled, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.disconnect), fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        } else {
            DeviceList(
                devices = obdViewModel.getPairedDevices(),
                onDeviceClick = { obdViewModel.connect(it) }
            )
        }
    }
}

@Composable
fun TripSummaryCard(obdData: com.example.myapplication.obd.ObdData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("مسافت این سفر", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text("%.2f KM".format(obdData.tripDistance), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            VerticalDivider(modifier = Modifier.height(40.dp).width(1.dp), color = Color.LightGray.copy(alpha = 0.3f))
            Column(horizontalAlignment = Alignment.End) {
                Text("مصرف سوخت لحظه‌ای", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text("%.1f L/100".format(obdData.instantFuelConsumption), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun SensorGrid(obdData: com.example.myapplication.obd.ObdData, units: UnitSystem) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val displayTemp = if (units == UnitSystem.METRIC) obdData.coolantTemp.toFloat() else (obdData.coolantTemp * 9/5) + 32f
            val tempUnit = if (units == UnitSystem.METRIC) "°C" else "°F"
            
            SensorCard(
                icon = Icons.Rounded.Thermostat,
                label = stringResource(R.string.sensor_coolant),
                value = "%.0f %s".format(displayTemp, tempUnit),
                modifier = Modifier.weight(1f),
                color = if (obdData.coolantTemp > 105) Color.Red else Color(0xFF1976D2)
            )
            SensorCard(
                icon = Icons.Rounded.ElectricBolt,
                label = stringResource(R.string.sensor_battery),
                value = "%.1f ${stringResource(R.string.unit_volt)}".format(obdData.voltage),
                modifier = Modifier.weight(1f),
                color = if (obdData.voltage < 12.5f) Color(0xFFFBC02D) else Color(0xFF388E3C)
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SensorCard(
                icon = Icons.Rounded.Speed,
                label = stringResource(R.string.sensor_load),
                value = "%.0f %%".format(obdData.load),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.primary
            )
            SensorCard(
                icon = Icons.Rounded.TireRepair,
                label = stringResource(R.string.sensor_throttle),
                value = "%.0f %%".format(obdData.throttle),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.secondary
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SensorCard(
                icon = Icons.Rounded.Air,
                label = "دمای هوای ورودی",
                value = "${obdData.intakeTemp}°C",
                modifier = Modifier.weight(1f),
                color = Color(0xFF00BCD4)
            )
            SensorCard(
                icon = Icons.Rounded.BarChart,
                label = "تصحیح سوخت",
                value = "%.1f %%".format(obdData.fuelTrim),
                modifier = Modifier.weight(1f),
                color = if (obdData.fuelTrim > 10 || obdData.fuelTrim < -10) Color.Red else Color(0xFF4CAF50)
            )
        }
    }
}

@Composable
fun SensorCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = color)
            }
        }
    }
}

@Composable
fun StatusCard(state: ObdConnectionState) {
    val (color, text) = when (state) {
        is ObdConnectionState.Connected -> Color(0xFF388E3C) to stringResource(R.string.state_connected)
        is ObdConnectionState.Connecting -> Color(0xFFFBC02D) to stringResource(R.string.state_connecting)
        is ObdConnectionState.Error -> Color(0xFFD32F2F) to stringResource(R.string.state_error, state.message)
        else -> Color.Gray to stringResource(R.string.state_disconnected)
    }

    Surface(
        modifier = Modifier.fillMaxWidth().height(40.dp),
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text, 
                style = MaterialTheme.typography.labelMedium, 
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun DeviceList(
    devices: List<android.bluetooth.BluetoothDevice>, 
    onDeviceClick: (android.bluetooth.BluetoothDevice) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Bluetooth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(text = stringResource(R.string.paired_devices), style = MaterialTheme.typography.titleMedium)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (devices.isEmpty()) {
            Text(stringResource(R.string.no_paired_devices), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(devices) { device ->
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth().clickable { onDeviceClick(device) },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        ListItem(
                            headlineContent = { Text(device.name ?: "Unknown Device", fontWeight = FontWeight.Bold) },
                            supportingContent = { Text(device.address) },
                            leadingContent = { 
                                Icon(
                                    Icons.Rounded.BluetoothConnected, 
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                ) 
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WarningSection(obdData: com.example.myapplication.obd.ObdData) {
    val showOverheat = obdData.coolantTemp > 105
    val showLowBattery = obdData.voltage < 12.0f && obdData.voltage > 5.0f

    if (showOverheat || showLowBattery) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showOverheat) {
                WarningIcon(Icons.Rounded.Thermostat, Color.Red)
            }
            if (showOverheat && showLowBattery) Spacer(Modifier.width(16.dp))
            if (showLowBattery) {
                WarningIcon(Icons.Rounded.BatteryAlert, Color(0xFFFBC02D))
            }
        }
    }
}

@Composable
fun WarningIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "warning")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = color.copy(alpha = alpha),
        modifier = Modifier.size(32.dp)
    )
}
