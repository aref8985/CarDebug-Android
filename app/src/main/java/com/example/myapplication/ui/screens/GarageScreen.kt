package com.example.myapplication.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import com.example.myapplication.data.local.Vehicle
import com.example.myapplication.ui.viewmodels.GarageViewModel

@Composable
fun GarageScreen(viewModel: GarageViewModel = viewModel(), onNavigateToDetail: (Int) -> Unit) {
    val vehicles by viewModel.allVehicles.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.garage),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black
        )
        Text(
            text = "شناسایی خودکار خودروهای متصل شده",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (vehicles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Surface(
                        modifier = Modifier.size(120.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = CircleShape
                    ) {
                        Icon(
                            Icons.Rounded.BluetoothSearching, 
                            contentDescription = null, 
                            modifier = Modifier.padding(30.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "هنوز خودرویی شناسایی نشده است",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "به محض اتصال دانگل به خودرو، شناسنامه ماشین شما اینجا ساخته می‌شود.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    Text(
                        "خودروهای شناسایی شده (${vehicles.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
                items(vehicles) { vehicle ->
                    ProfessionalVehicleItem(
                        vehicle = vehicle,
                        onDelete = { viewModel.deleteVehicle(vehicle) },
                        onSelect = { viewModel.selectVehicle(vehicle.id) },
                        onClick = { onNavigateToDetail(vehicle.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ProfessionalVehicleItem(vehicle: Vehicle, onDelete: () -> Unit, onSelect: () -> Unit, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                onSelect() // Select as active when clicked
                onClick() 
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (vehicle.isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
        ),
        border = if (vehicle.isActive) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.DirectionsCar, 
                        contentDescription = null, 
                        tint = if (vehicle.isActive) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(vehicle.name, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val statusColor = if (vehicle.hasErrors) Color.Red else Color(0xFF2E7D32)
                            val statusText = if (vehicle.hasErrors) "نیاز به بررسی (${vehicle.errorCount} خطا)" else "وضعیت موتور: سالم"
                            val statusIcon = if (vehicle.hasErrors) Icons.Rounded.Warning else Icons.Rounded.CheckCircle
                            
                            Icon(statusIcon, contentDescription = null, Modifier.size(12.dp), tint = statusColor)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = statusText,
                                color = statusColor,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color.LightGray.copy(alpha = 0.2f))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoTag(Icons.Rounded.Fingerprint, vehicle.vin?.takeLast(8) ?: "---")
                InfoTag(Icons.Rounded.SettingsSuggest, vehicle.engineType ?: "استاندارد")
                InfoTag(Icons.Rounded.History, "${vehicle.mileage.toInt()} KM")
            }
        }
    }
}

@Composable
fun InfoTag(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
    }
}
