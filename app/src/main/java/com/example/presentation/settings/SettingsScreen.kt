package com.example.presentation.settings

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.VpnProtocol
import com.example.presentation.components.FrostedMeshBackground
import com.example.presentation.components.GlassmorphicCard
import com.example.presentation.viewmodel.VpnViewModel
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividOrange

@Composable
fun SettingsScreen(
    viewModel: VpnViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val securitySettings by viewModel.securitySettings.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    FrostedMeshBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 12.dp)
                .verticalScroll(scrollState)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                        text = "SECURITY & SETTINGS",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Text(
                        text = "Kill Switch, Protocol & Encryption Controls",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            ) {
                // Section Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Security",
                        tint = ElectricBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "HARDENED PRIVACY CONTROLS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Kill Switch Card
                GlassmorphicCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Kill Switch Protection",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Blocks all internet traffic if VPN drops unexpectedly.",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        Switch(
                            checked = securitySettings.killSwitchEnabled,
                            onCheckedChange = { viewModel.toggleKillSwitch(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = ElectricBlue,
                                uncheckedTrackColor = Color(0x33FFFFFF)
                            ),
                            modifier = Modifier.testTag("kill_switch_toggle")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Auto-Connect Card
                GlassmorphicCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Connect on Wi-Fi",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Automatically activates KMTH shield when joining public networks.",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        Switch(
                            checked = securitySettings.autoConnectOnWifi,
                            onCheckedChange = { viewModel.toggleAutoConnect(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = ElectricBlue,
                                uncheckedTrackColor = Color(0x33FFFFFF)
                            ),
                            modifier = Modifier.testTag("auto_connect_toggle")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // DNS Leak Protection Card
                GlassmorphicCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "DNS Leak Protection",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Routes all DNS queries through private KMTH encrypted resolver.",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        Switch(
                            checked = securitySettings.dnsLeakProtection,
                            onCheckedChange = { viewModel.toggleDnsLeakProtection(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = ElectricBlue,
                                uncheckedTrackColor = Color(0x33FFFFFF)
                            ),
                            modifier = Modifier.testTag("dns_protection_toggle")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Split Tunneling Card
                GlassmorphicCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Split Tunneling",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Bypass specific apps (e.g. YouTube, Banking) from VPN tunnel.",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        Switch(
                            checked = securitySettings.splitTunnelingEnabled,
                            onCheckedChange = { viewModel.toggleSplitTunneling(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = ElectricBlue,
                                uncheckedTrackColor = Color(0x33FFFFFF)
                            ),
                            modifier = Modifier.testTag("split_tunneling_toggle")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Protocol Selection Section
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Protocol",
                        tint = NeonPurple,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TUNNELING PROTOCOL & CIPHERS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                VpnProtocol.values().forEach { protocol ->
                    val isSelected = protocol == securitySettings.selectedProtocol

                    GlassmorphicCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .testTag("protocol_option_${protocol.name}")
                            .clickable { viewModel.selectProtocol(protocol) },
                        borderColor = if (isSelected) ElectricBlue else Color(0x22FFFFFF),
                        borderWidth = if (isSelected) 2.dp else 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = protocol.displayName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) ElectricBlue else TextPrimary
                                )
                                Text(
                                    text = protocol.description,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ElectricBlue.copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ElectricBlue
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Encryption Specs Banner
                GlassmorphicCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "KMTH HARDWARE CIPHER SPECIFICATIONS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = VividOrange,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Symmetric Cipher: AES-256-GCM / ChaCha20-Poly1305\n" +
                                    "• Handshake Key: RSA-4096 & ECDHE-P384\n" +
                                    "• Authentication Digest: SHA-512 Hash\n" +
                                    "• Forward Secrecy: Ephemeral session key rotation every 60m",
                            fontSize = 11.sp,
                            color = TextMuted,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

