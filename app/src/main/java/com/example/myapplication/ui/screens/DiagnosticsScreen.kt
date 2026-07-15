package com.example.myapplication.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import com.example.myapplication.data.local.Dtc
import com.example.myapplication.obd.ObdConnectionState
import com.example.myapplication.ui.viewmodels.DiagnosticsViewModel
import com.example.myapplication.ui.viewmodels.ObdViewModel
import com.example.myapplication.ui.viewmodels.ImStatus
import com.example.myapplication.ui.viewmodels.FreezeFrameData
import com.example.myapplication.CarDiagApp

@Composable
fun DiagnosticsScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as CarDiagApp
    
    val obdViewModel: ObdViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ObdViewModel(app.obdBluetoothManager, app) as T
            }
        }
    )

    val diagViewModel: DiagnosticsViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DiagnosticsViewModel(app, app.obdBluetoothManager) as T
            }
        }
    )

    val connectionState by obdViewModel.connectionState.collectAsState()
    val activeVehicle by obdViewModel.activeVehicle.collectAsState()
    val dtcs by diagViewModel.dtcList.collectAsState()
    val isReading by diagViewModel.isReading.collectAsState()
    val isClearing by diagViewModel.isClearing.collectAsState()
    val scanProgress by diagViewModel.scanProgress.collectAsState()
    val currentModule by diagViewModel.currentModule.collectAsState()
    val vin by diagViewModel.vin.collectAsState()
    val milStatus by diagViewModel.milStatus.collectAsState()
    val imList by diagViewModel.imList.collectAsState()
    val freezeFrame by diagViewModel.freezeFrame.collectAsState()
    val errorMessage by diagViewModel.errorMessage.collectAsState()

    LaunchedEffect(Unit) {
        diagViewModel.seedSampleDtcs()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = stringResource(R.string.diagnostics), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                activeVehicle?.let {
                    Text(it.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (dtcs.isNotEmpty()) {
                IconButton(onClick = { shareReport(context, dtcs) }) {
                    Icon(Icons.Rounded.Share, contentDescription = "Share")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        if (connectionState !is ObdConnectionState.Connected) {
            DisconnectedPlaceholder()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // 1. ECU Info Summary
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                EcuInfoItem(stringResource(R.string.mil_status), if (milStatus) stringResource(R.string.mil_on) else stringResource(R.string.mil_off), if (milStatus) Color.Red else Color(0xFF2E7D32))
                                EcuInfoItem(stringResource(R.string.vin_number), vin, MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                // 2. Action Buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { diagViewModel.readDtc() },
                            modifier = Modifier.weight(1f).height(50.dp),
                            enabled = !isReading && !isClearing,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.YoutubeSearchedFor, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("اسکن کل سیستم")
                        }
                        
                        OutlinedButton(
                            onClick = { diagViewModel.clearDtc() },
                            modifier = Modifier.weight(1f).height(50.dp),
                            enabled = !isReading && !isClearing,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
                        ) {
                            Icon(Icons.Rounded.AutoDelete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.clear_dtc))
                        }
                    }
                }

                // 3. Progress Section
                if (isReading || isClearing) {
                    item {
                        ScanProgressCard(isClearing, currentModule, scanProgress)
                    }
                }

                // 4. Status Messages
                errorMessage?.let { msg ->
                    item {
                        StatusMessageCard(msg)
                    }
                }

                // 5. I/M Readiness Section
                if (imList.isNotEmpty()) {
                    item {
                        ImReadinessSection(imList)
                    }
                }

                // 6. Freeze Frame Section
                freezeFrame?.let { data ->
                    item {
                        FreezeFrameSection(data)
                    }
                }

                // 7. DTC List
                if (dtcs.isEmpty() && !isReading && !isClearing) {
                    item {
                        EmptyStateCentered()
                    }
                } else {
                    items(dtcs) { dtc ->
                        DtcCard(dtc) {
                            val browserIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com/search?q=OBD+Code+${dtc.code}")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(browserIntent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImReadinessSection(imList: List<ImStatus>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Checklist, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.im_readiness_status), style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(12.dp))
            imList.forEach { status ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(status.nameRes), style = MaterialTheme.typography.bodySmall)
                    Surface(
                        color = if (status.isReady) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (status.isReady) stringResource(R.string.ready) else stringResource(R.string.not_ready),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (status.isReady) Color(0xFF2E7D32) else Color.Red,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FreezeFrameSection(data: FreezeFrameData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Camera, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.freeze_frame_title), style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FfItem(stringResource(R.string.ff_engine_load), data.load, Modifier.weight(1f))
                FfItem(stringResource(R.string.ff_coolant), data.temp, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FfItem(stringResource(R.string.ff_rpm), data.rpm, Modifier.weight(1f))
                FfItem(stringResource(R.string.ff_speed), data.speed, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun FfItem(label: String, value: String, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun EcuInfoItem(label: String, value: String, valueColor: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

@Composable
fun ScanProgressCard(isClearing: Boolean, currentModule: String, progress: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
                Text(
                    if (isClearing) "در حال ریست کردن ایسیو..." else "در حال بررسی: $currentModule",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
            )
        }
    }
}

@Composable
fun StatusMessageCard(message: String) {
    val isSuccess = message.contains("موفقیت")
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSuccess) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (isSuccess) Icons.Rounded.DoneAll else Icons.Rounded.ReportProblem,
                contentDescription = null,
                tint = if (isSuccess) Color(0xFF2E7D32) else Color.Red
            )
            Spacer(Modifier.width(12.dp))
            Text(message, color = if (isSuccess) Color(0xFF1B5E20) else Color.Red, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun EmptyStateCentered() {
    Box(Modifier.fillMaxSize().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.VerifiedUser, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color(0xFF4CAF50).copy(alpha = 0.3f))
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.no_dtc_found),
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF2E7D32),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun DisconnectedPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.SettingsBluetooth, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color.LightGray)
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.connect_dashboard_first),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 40.dp)
            )
        }
    }
}

fun shareReport(context: android.content.Context, dtcs: List<Dtc>) {
    val text = dtcs.joinToString("\n\n") { "${it.code}\n${it.descriptionFa}" }
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, "گزارش خطاهای خودرو:\n\n$text")
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "ارسال گزارش")
    shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(shareIntent)
}

@Composable
fun DtcCard(dtc: Dtc, onSearch: () -> Unit) {
    val severityColor = when {
        dtc.code.startsWith("P0") -> Color(0xFFD32F2F)
        else -> Color(0xFFFF6D00)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = severityColor, shape = RoundedCornerShape(6.dp)) {
                    Text(
                        text = dtc.code,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(12.dp))
                if (dtc.brand != null && dtc.brand != "GENERIC") {
                    Text(dtc.brand, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onSearch, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = dtc.descriptionEn, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.2f))
            Text(
                text = dtc.descriptionFa,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right
            )
        }
    }
}
