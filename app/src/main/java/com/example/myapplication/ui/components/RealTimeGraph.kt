package com.example.myapplication.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RealTimeGraph(
    data: List<Int>,
    label: String,
    maxValue: Float,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "${data.lastOrNull() ?: 0}", style = MaterialTheme.typography.labelSmall, color = lineColor)
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                if (data.size < 2) return@Canvas
                
                val path = Path()
                val width = size.width
                val height = size.height
                val stepX = width / (50 - 1) // Fixed for 50 points
                
                data.forEachIndexed { index, value ->
                    val x = index * stepX
                    val y = height - (value.toFloat() / maxValue * height).coerceIn(0f, height)
                    
                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }
                
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}
