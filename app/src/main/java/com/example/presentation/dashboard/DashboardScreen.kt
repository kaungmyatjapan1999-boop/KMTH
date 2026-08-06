package com.example.presentation.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.model.ConnectionState
import com.example.presentation.components.FrostedMeshBackground
import com.example.presentation.components.GlassmorphicCard
import com.example.presentation.components.GlowingConnectButton
import com.example.presentation.components.KmthTopBar
import com.example.presentation.components.ProtocolChipSelector
import com.example.presentation.components.TrafficSpeedGraph
import com.example.presentation.viewmodel.VpnViewModel
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GreenConnected
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividOrange
import com.example.ui.theme.YellowConnecting

@Composable
fun DashboardScreen(
    viewModel: VpnViewModel,
    onNavigateToServers: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val selectedServer by viewModel.selectedServer.collectAsStateWithLifecycle()
    val trafficStats by viewModel.trafficStats.collectAsStateWithLifecycle()
    val speedHistory by viewModel.speedHistory.collectAsStateWithLifecycle()
    val timerSeconds by viewModel.connectionTimerSeconds.collectAsStateWithLifecycle()
    val securitySettings by viewModel.securitySettings.collectAsStateWithLifecycle()
    val ipInfo by viewModel.ipCheckResponse.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()

    FrostedMeshBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Top Bar
            KmthTopBar(
                connectionState = connectionState,
                ipAddress = ipInfo.ip,
                onNavigateToDiagnostics = onNavigateToDiagnostics,
                onNavigateToSettings = onNavigateToSettings
            )

            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Hero World Map Banner
                GlassmorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    borderWidth = 1.dp
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_vpn_map_hero_1786018208696),
                            contentDescription = "Global VPN Map Hero",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Banner Overlay Content
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x9905060A))
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (connectionState == ConnectionState.CONNECTED) "KMTH PROTOCOL ACTIVE" else "READY TO SHIELD",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (connectionState == ConnectionState.CONNECTED) GreenConnected else ElectricBlue,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = ipInfo.city + ", " + ipInfo.country,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "AES-256 OpenVPN • Zero Logs",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }

                            // Connection Timer Pill
                            if (connectionState == ConnectionState.CONNECTED) {
                                val hours = timerSeconds / 3600
                                val minutes = (timerSeconds % 3600) / 60
                                val seconds = timerSeconds % 60
                                val timeStr = "%02d:%02d:%02d".format(hours, minutes, seconds)

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(GreenConnected.copy(alpha = 0.2f))
                                        .border(1.dp, GreenConnected, RoundedCornerShape(20.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Timer,
                                            contentDescription = "Timer",
                                            tint = GreenConnected,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = timeStr,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GreenConnected
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Main Glowing Connect Button
                GlowingConnectButton(
                    connectionState = connectionState,
                    onClick = { viewModel.toggleConnection() }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Selected Server Quick Selector Card
                GlassmorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("selected_server_card")
                        .clickable(onClick = onNavigateToServers),
                    borderColor = ElectricBlue.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x22FFFFFF))
                                    .border(1.dp, ElectricBlue, CircleShape)
                            ) {
                                Text(
                                    text = selectedServer?.flagEmoji ?: "🌐",
                                    fontSize = 20.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "LOCATION",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = selectedServer?.let { "${it.countryName} - ${it.cityName}" } ?: "Select VPN Node",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ElectricBlue.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${selectedServer?.pingMs ?: 0} ms",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricBlue
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Change Server",
                                tint = TextMuted,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Protocol Selector Chips
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "OPENVPN & SPEED PROTOCOL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    ProtocolChipSelector(
                        selectedProtocol = securitySettings.selectedProtocol,
                        onProtocolSelected = { viewModel.selectProtocol(it) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Real-time Traffic Graph
                TrafficSpeedGraph(
                    trafficStats = trafficStats,
                    pingMs = selectedServer?.pingMs ?: 28,
                    speedHistory = speedHistory
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

