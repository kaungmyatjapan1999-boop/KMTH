package com.example.presentation.diagnostics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.presentation.components.FrostedMeshBackground
import com.example.presentation.components.GlassmorphicCard
import com.example.presentation.viewmodel.VpnViewModel
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GreenConnected
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividOrange
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DiagnosticsScreen(
    viewModel: VpnViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val diagnosticResult by viewModel.diagnosticResult.collectAsStateWithLifecycle()
    val isTesting by viewModel.isTestingDiagnostics.collectAsStateWithLifecycle()
    val sessionLogs by viewModel.sessionLogs.collectAsStateWithLifecycle()
    val selectedServer by viewModel.selectedServer.collectAsStateWithLifecycle()

    FrostedMeshBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 12.dp)
        ) {
            // Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                            .border(1.dp, Color(0x33FFFFFF), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "DIAGNOSTICS & PING",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        Text(
                            text = "Latency Analyzer & Session History",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                if (sessionLogs.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.clearLogs() },
                        modifier = Modifier.testTag("clear_logs_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear Session History",
                            tint = TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                // Speed & Ping Diagnostic Tool Card
                GlassmorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = ElectricBlue.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.NetworkCheck,
                                    contentDescription = "Diagnostic",
                                    tint = ElectricBlue,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Text(
                                    text = "LATENCY & SPEED TEST",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }

                            Text(
                                text = selectedServer?.countryName ?: "Default Node",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ElectricBlue
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (isTesting) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 12.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = ElectricBlue,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Measuring socket latency & bandwidth...",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ElectricBlue
                                )
                            }
                        } else if (diagnosticResult != null) {
                            val result = diagnosticResult!!
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                // Ping
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("PING", fontSize = 10.sp, color = TextMuted)
                                    Text("${result.pingMs} ms", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GreenConnected)
                                }

                                // Download
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("DOWNLOAD", fontSize = 10.sp, color = TextMuted)
                                    Text("${"%.1f".format(result.downloadSpeedMbps)} Mbps", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ElectricBlue)
                                }

                                // Upload
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("UPLOAD", fontSize = 10.sp, color = TextMuted)
                                    Text("${"%.1f".format(result.uploadSpeedMbps)} Mbps", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeonPurple)
                                }

                                // Jitter
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("JITTER", fontSize = 10.sp, color = TextMuted)
                                    Text("${result.jitterMs} ms", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = VividOrange)
                                }
                            }
                        } else {
                            Text(
                                text = "Tap test to run live network diagnostic on current VPN server node.",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.runDiagnostics() },
                            enabled = !isTesting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricBlue,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("run_diagnostics_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = "Run Test",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isTesting) "ANALYZING..." else "TEST SERVER SPEED & PING",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Session History Section Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "History",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "VPN SESSION LOGS (${sessionLogs.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Session Logs List
            if (sessionLogs.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No Connection History Yet",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Your completed VPN connection sessions will appear here.",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(sessionLogs, key = { it.id }) { log ->
                        GlassmorphicCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = log.serverName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )

                                    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                                    Text(
                                        text = sdf.format(Date(log.connectedAtTimestamp)),
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val mins = log.durationSeconds / 60
                                    val secs = log.durationSeconds % 60
                                    Text(
                                        text = "Duration: ${mins}m ${secs}s • Protocol: ${log.protocolUsed}",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )

                                    val totalMb = (log.bytesDownloaded + log.bytesUploaded) / (1024f * 1024f)
                                    Text(
                                        text = "${"%.1f".format(totalMb)} MB",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ElectricBlue
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

