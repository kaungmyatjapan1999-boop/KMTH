package com.example.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.TrafficStats
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.VividOrange

@Composable
fun TrafficSpeedGraph(
    trafficStats: TrafficStats,
    pingMs: Int,
    speedHistory: List<Float>,
    modifier: Modifier = Modifier
) {
    GlassmorphicCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Traffic Stats",
                        tint = ElectricBlue,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = "REAL-TIME TRAFFIC",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = 0.5.sp
                    )
                }

                Text(
                    text = "${pingMs}ms PING",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (pingMs < 50) ElectricBlue else VividOrange
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Live Canvas Wave Graph
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                val width = size.width
                val height = size.height

                // Draw background grid lines
                val gridColor = Color(0x1B00F0FF)
                for (i in 1..3) {
                    val y = height * (i / 4f)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f
                    )
                }

                if (speedHistory.size >= 2) {
                    val maxVal = (speedHistory.maxOrNull() ?: 100f).coerceAtLeast(10f)
                    val points = speedHistory.mapIndexed { index, value ->
                        val x = (index.toFloat() / (speedHistory.size - 1)) * width
                        val y = height - ((value / maxVal) * (height - 10f))
                        Offset(x, y)
                    }

                    // Download line (Electric Blue)
                    val path = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            lineTo(points[i].x, points[i].y)
                        }
                    }

                    drawPath(
                        path = path,
                        color = ElectricBlue,
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // Upload line offset (Neon Purple)
                    val uploadPath = Path().apply {
                        moveTo(points.first().x, points.first().y + 8f)
                        for (i in 1 until points.size) {
                            lineTo(points[i].x, (points[i].y * 0.5f + height * 0.4f).coerceAtMost(height))
                        }
                    }

                    drawPath(
                        path = uploadPath,
                        color = NeonPurple,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Download Ticker
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Download",
                        tint = ElectricBlue,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Column {
                        Text(
                            text = "DOWNLOAD",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMuted
                        )
                        Text(
                            text = "${"%.1f".format(trafficStats.downloadSpeedKbps)} Mbps",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Upload Ticker
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Upload",
                        tint = NeonPurple,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Column {
                        Text(
                            text = "UPLOAD",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMuted
                        )
                        Text(
                            text = "${"%.1f".format(trafficStats.uploadSpeedKbps)} Mbps",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonPurple
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Total Session Data
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "TOTAL DATA",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted
                    )
                    val totalMb = (trafficStats.totalBytesReceived + trafficStats.totalBytesSent) / (1024f * 1024f)
                    Text(
                        text = "${"%.1f".format(totalMb)} MB",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}
