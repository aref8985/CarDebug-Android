package com.example.myapplication.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CircularGauge(
    value: Float,
    maxValue: Float,
    label: String,
    unit: String,
    modifier: Modifier = Modifier,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.primaryContainer
) {
    val sweepAngle = 240f
    val startAngle = 150f
    val animatedValue by animateFloatAsState(targetValue = value, label = "gaugeValue")
    val ratio = (animatedValue / maxValue).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .padding(8.dp) // Add internal padding to avoid being cut off
            .aspectRatio(1.2f), // Slightly wider than tall to give space for labels below
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 10.dp.toPx()
            val canvasSize = size.minDimension
            val radius = (canvasSize - strokeWidth) / 2
            
            val arcSize = Size(radius * 2, radius * 2)
            val topLeft = Offset(
                (size.width - arcSize.width) / 2,
                (size.height - arcSize.height) / 2
            )
            
            // Background arc
            drawArc(
                color = secondaryColor.copy(alpha = 0.2f),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Foreground arc
            drawArc(
                color = primaryColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle * ratio,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value.toInt().toString(),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}
