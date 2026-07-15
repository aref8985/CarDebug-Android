package com.example.myapplication.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import com.example.myapplication.data.settings.AppLanguage
import com.example.myapplication.data.settings.AppTheme
import com.example.myapplication.data.settings.SettingsRepository
import com.example.myapplication.data.settings.UnitSystem
import com.example.myapplication.ui.viewmodels.SettingsViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateToTerminal: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as android.app.Application
    val repository = remember { SettingsRepository(context) }
    val viewModel: SettingsViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(repository, app) as T
            }
        }
    )

    val theme by viewModel.theme.collectAsState()
    val language by viewModel.language.collectAsState()
    val units by viewModel.units.collectAsState()
    val rpmThreshold by viewModel.rpmThreshold.collectAsState()
    val tempThreshold by viewModel.tempThreshold.collectAsState()
    val autoConnect by viewModel.autoConnect.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings), fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Connection & Automation
            SettingsSection(title = "اتصال و هوشمندسازی", icon = Icons.Rounded.AutoMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("اتصال خودکار", style = MaterialTheme.typography.bodyLarge)
                        Text("به محض باز شدن برنامه به آخرین دانگل وصل شود", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                    Switch(checked = autoConnect, onCheckedChange = { viewModel.setAutoConnect(it) })
                }
            }

            // 2. Alert Thresholds (CRITICAL for Iranian Cars)
            SettingsSection(title = "آستانه هشدار سنسورها", icon = Icons.Rounded.NotificationsActive) {
                // RPM Alert
                Column {
                    Text(stringResource(R.string.rpm_threshold, rpmThreshold), style = MaterialTheme.typography.bodyLarge)
                    Slider(
                        value = rpmThreshold.toFloat(),
                        onValueChange = { viewModel.setRpmThreshold(it.toInt()) },
                        valueRange = 3000f..8000f,
                        steps = 5
                    )
                }
                
                Spacer(Modifier.height(16.dp))

                // Temp Alert
                Column {
                    Text("آستانه دمای موتور: $tempThreshold°C", style = MaterialTheme.typography.bodyLarge)
                    Text("توصیه شده برای پژو و سمند: ۱۰۵ درجه", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Slider(
                        value = tempThreshold.toFloat(),
                        onValueChange = { viewModel.setTempThreshold(it.toInt()) },
                        valueRange = 80f..120f,
                        steps = 8
                    )
                }
            }

            // 3. Interface Settings
            SettingsSection(title = "تنظیمات ظاهری و منطقه‌ای", icon = Icons.Rounded.Palette) {
                // Theme
                Text("پوسته برنامه", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Row(Modifier.selectableGroup().padding(top = 8.dp)) {
                    AppTheme.entries.forEach { entry ->
                        FilterChip(
                            selected = theme == entry,
                            onClick = { viewModel.setTheme(entry) },
                            label = { Text(when(entry){
                                AppTheme.LIGHT -> "روشن"
                                AppTheme.DARK -> "تاریک"
                                AppTheme.SYSTEM -> "سیستم"
                            }) },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Language
                Text("زبان برنامه", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Row(Modifier.selectableGroup().padding(top = 8.dp)) {
                    AppLanguage.entries.forEach { entry ->
                        FilterChip(
                            selected = language == entry,
                            onClick = { viewModel.setLanguage(entry) },
                            label = { Text(if (entry == AppLanguage.EN) "English" else "فارسی") },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Units
                Text("واحد اندازه‌گیری", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Row(Modifier.selectableGroup().padding(top = 8.dp)) {
                    UnitSystem.entries.forEach { entry ->
                        FilterChip(
                            selected = units == entry,
                            onClick = { viewModel.setUnits(entry) },
                            label = { Text(if (entry == UnitSystem.METRIC) "Metric (KM)" else "Imperial (Miles)") },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            }

            // 4. Advanced & Debug
            SettingsSection(title = "ابزارهای پیشرفته", icon = Icons.Rounded.SettingsSuggest) {
                Button(
                    onClick = onNavigateToTerminal,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Icon(Icons.Rounded.Terminal, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.open_terminal))
                }
                
                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { viewModel.clearAllData() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                ) {
                    Icon(Icons.Rounded.DeleteSweep, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("پاکسازی کل حافظه برنامه")
                }
            }

            // 5. Version Info
            var clickCount by remember { mutableIntStateOf(0) }
            val scope = rememberCoroutineScope()
            val app = context.applicationContext as com.example.myapplication.CarDiagApp

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {
                        clickCount++
                        if (clickCount >= 5) {
                            scope.launch {
                                app.obdBluetoothManager.startDeveloperTest()
                            }
                            clickCount = 0
                        }
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("نسخه نهایی ۱.۰.۰", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text("توسعه یافته برای خودروهای ایران", style = MaterialTheme.typography.labelSmall, color = Color.Gray.copy(alpha = 0.5f))
                
                if (clickCount > 0 && clickCount < 5) {
                    Text("Developer mode in ${5 - clickCount} steps", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}
