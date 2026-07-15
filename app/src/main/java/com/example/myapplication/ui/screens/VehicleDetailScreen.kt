package com.example.myapplication.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import com.example.myapplication.data.local.Vehicle
import com.example.myapplication.ui.viewmodels.GarageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailScreen(vehicleId: Int, onBack: () -> Unit) {
    val viewModel: GarageViewModel = viewModel()
    val vehicles by viewModel.allVehicles.collectAsState()
    val vehicle = vehicles.find { it.id == vehicleId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(vehicle?.name ?: "جزئیات فنی") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (vehicle == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("خودرو یافت نشد")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Health Score
                HealthScoreCard(vehicle)

                // 2. Service & Maintenance
                Text("وضعیت سرویس‌های دوره‌ای", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        MaintenanceItemPro("روغن موتور", (vehicle.mileage % 5000).toInt(), 5000, Icons.Rounded.OilBarrel)
                        MaintenanceItemPro("تسمه تایم", (vehicle.mileage % 60000).toInt(), 60000, Icons.Rounded.Sync)
                    }
                }

                // 3. Odometer & Sync
                var showSyncDialog by remember { mutableStateOf(false) }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("کارکرد و پیمایش", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { showSyncDialog = true }) {
                                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("تنظیم کیلومتر اصلی")
                            }
                        }
                        TechnicalRow(Icons.Rounded.Speed, "کارکرد کل (Odometer)", "${vehicle.mileage.toInt()} کیلومتر")
                        Text(
                            "این عدد با هر بار رانندگی به صورت خودکار بروزرسانی می‌شود.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // 4. Identification
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("شناسنامه فنی", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                        TechnicalRow(Icons.Rounded.Fingerprint, "شماره شاسی", vehicle.vin ?: "ناشناخته")
                        TechnicalRow(Icons.Rounded.SettingsSuggest, "پلتفرم موتور", vehicle.engineType ?: "استاندارد")
                        TechnicalRow(Icons.Rounded.CalendarMonth, "آخرین بازبینی", if (vehicle.lastScanDate != null) "اخیراً" else "بدون سابقه")
                    }
                }

                if (showSyncDialog) {
                    var inputMileage by remember { mutableStateOf(vehicle.mileage.toString()) }
                    AlertDialog(
                        onDismissRequest = { showSyncDialog = false },
                        title = { Text("همگام‌سازی کیلومترشمار") },
                        text = {
                            Column {
                                Text("عدد دقیق کیلومتر پشت آمپر را وارد کنید تا برنامه با آن هماهنگ شود.", style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = inputMileage,
                                    onValueChange = { inputMileage = it },
                                    label = { Text("کیلومتر فعلی") },
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                viewModel.syncOdometer(vehicle.id, inputMileage.toIntOrNull() ?: 0)
                                showSyncDialog = false
                            }) { Text("تأیید و ذخیره") }
                        },
                        dismissButton = { TextButton(onClick = { showSyncDialog = false }) { Text("انصراف") } }
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun HealthScoreCard(vehicle: Vehicle) {
    val score = when {
        vehicle.errorCount == 0 -> 100
        vehicle.errorCount == 1 -> 85
        vehicle.errorCount < 3 -> 60
        else -> 35
    }
    val color = when {
        score > 80 -> Color(0xFF2E7D32)
        score > 50 -> Color(0xFFFBC02D)
        else -> Color.Red
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("امتیاز سلامت خودرو", style = MaterialTheme.typography.labelMedium, color = color)
                Text(
                    if (score > 80) "وضعیت عالی" else if (score > 50) "نیاز به سرویس" else "وضعیت بحرانی",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = color
                )
            }
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { score / 100f },
                    modifier = Modifier.size(60.dp),
                    color = color,
                    strokeWidth = 6.dp,
                    strokeCap = StrokeCap.Round
                )
                Text("$score", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
            }
        }
    }
}

@Composable
fun MaintenanceItemPro(label: String, currentKm: Int, maxKm: Int, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val progress = (currentKm.toFloat() / maxKm).coerceIn(0f, 1f)
    val color = if (progress > 0.85f) Color.Red else MaterialTheme.colorScheme.primary
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = color)
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("${maxKm - currentKm} کیلومتر مانده", style = MaterialTheme.typography.labelSmall, color = color)
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
            color = color,
            trackColor = color.copy(alpha = 0.1f)
        )
    }
}

@Composable
fun TechnicalRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
    }
}
