package com.example.myapplication.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.CarDiagApp
import com.example.myapplication.R
import com.example.myapplication.ui.viewmodels.PerformanceViewModel
import com.example.myapplication.ui.viewmodels.TimerState
import com.example.myapplication.ui.viewmodels.ObdViewModel
import com.example.myapplication.obd.ObdConnectionState

@Composable
fun PerformanceScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as CarDiagApp
    
    val perfViewModel: PerformanceViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PerformanceViewModel(app.obdBluetoothManager) as T
            }
        }
    )

    val obdViewModel: ObdViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ObdViewModel(app.obdBluetoothManager, app) as T
            }
        }
    )

    val timerState by perfViewModel.timerState.collectAsState()
    val currentTime by perfViewModel.currentTime.collectAsState()
    val metrics by perfViewModel.metrics.collectAsState()
    val personalBest by perfViewModel.personalBest.collectAsState()
    val obdData by obdViewModel.obdData.collectAsState()
    val connectionState by perfViewModel.connectionState.collectAsState()

    val isConnected = connectionState is ObdConnectionState.Connected
    val accentColor = when(timerState) {
        TimerState.RUNNING -> Color(0xFFFFEB3B) // Racing Yellow
        TimerState.FINISHED -> Color(0xFF00E676) // Success Green
        else -> Color(0xFF00E5FF) // Standby Cyan
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050505))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Pro Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CHRONO-LOG",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Box(modifier = Modifier.width(30.dp).height(2.dp).background(accentColor))
            }
            
            StatusIndicator(connectionState)
        }

        // 2. High-Visibility Speed Gauge
        Box(
            modifier = Modifier
                .size(220.dp)
                .drawBehind {
                    drawArc(
                        color = Color.White.copy(alpha = 0.05f),
                        startAngle = 140f, sweepAngle = 260f, useCenter = false,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        brush = Brush.sweepGradient(listOf(Color.Transparent, accentColor)),
                        startAngle = 140f,
                        sweepAngle = (obdData.speed.toFloat() / 180f).coerceIn(0f, 1f) * 260f,
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${obdData.speed}",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 90.sp),
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "KM/H",
                    style = MaterialTheme.typography.labelMedium,
                    color = accentColor,
                    letterSpacing = 4.sp
                )
                Spacer(Modifier.height(8.dp))
                GForceBadge(metrics.currentGForce)
            }
        }

        // 3. Dominant Timer
        val seconds = currentTime / 1000
        val millis = (currentTime % 1000) / 10
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "%d.%02d".format(seconds, millis),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 80.sp,
                    fontFamily = FontFamily.Monospace
                ),
                fontWeight = FontWeight.ExtraBold,
                color = if (timerState == TimerState.RUNNING) Color.White else Color.White.copy(alpha = 0.6f)
            )
            Text(
                text = when {
                    !isConnected -> "WAITING FOR OBD..."
                    obdData.speed == 0 && timerState != TimerState.FINISHED -> "READY TO LAUNCH"
                    timerState == TimerState.RUNNING -> "GO! GO! GO!"
                    timerState == TimerState.FINISHED -> "BEST: %.2fs".format(personalBest / 1000f)
                    else -> "STABILIZING..."
                },
                style = MaterialTheme.typography.titleMedium,
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
        }

        // 4. Compact Metrics & Graph
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricBox(label = "0-60", time = metrics.time0to60, modifier = Modifier.weight(1f))
                MetricBox(label = "0-100", time = metrics.time0to100, modifier = Modifier.weight(1f), isPrimary = true)
            }
            
            if (personalBest > 0) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF00E676).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "PERSONAL BEST: %.2fs".format(personalBest / 1000f),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF00E676),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        PerformanceGraph(
            history = metrics.speedHistory,
            accentColor = accentColor,
            modifier = Modifier.fillMaxWidth().height(60.dp)
        )

        // 5. Automatic Controls
        if (timerState == TimerState.FINISHED) {
            Button(
                onClick = { perfViewModel.resetTimer() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF212121))
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("RESET FOR NEXT RUN")
            }
        } else {
            Text(
                text = "SYSTEM FULLY AUTOMATED",
                style = MaterialTheme.typography.labelSmall,
                color = Color.DarkGray,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun StatusIndicator(state: ObdConnectionState) {
    val color = if (state is ObdConnectionState.Connected) Color(0xFF00E676) else Color.Red
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (state is ObdConnectionState.Connected) "LIVE" else "OFFLINE",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun GForceBadge(g: Float) {
    Surface(
        color = if (g > 0.4f) Color(0xFFFF3D00).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(4.dp),
        border = if (g > 0.4f) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF3D00)) else null
    ) {
        Text(
            text = "ACCEL: %.2f G".format(g),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = if (g > 0.4f) Color(0xFFFF3D00) else Color.Gray,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun MetricBox(label: String, time: Long, modifier: Modifier, isPrimary: Boolean = false) {
    Surface(
        modifier = modifier,
        color = if (isPrimary) Color.White.copy(alpha = 0.05f) else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(
                text = if (time == 0L) "--.--" else "%.2f".format(time / 1000f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = if (time > 0) Color.White else Color.DarkGray
            )
        }
    }
}

@Composable
fun PerformanceGraph(history: List<Pair<Long, Int>>, accentColor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.padding(horizontal = 4.dp)) {
        if (history.size < 2) {
            // Draw baseline
            drawLine(Color.White.copy(alpha = 0.05f), Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
            return@Canvas
        }
        val maxT = history.last().first.toFloat()
        val path = Path()
        history.forEachIndexed { i, (t, s) ->
            val x = (t / maxT) * size.width
            val y = size.height - (s / 120f) * size.height
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, accentColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
    }
}
