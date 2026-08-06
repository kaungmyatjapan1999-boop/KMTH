package com.example.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.VpnServer
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GreenConnected
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividOrange
import com.example.ui.theme.YellowConnecting

@Composable
fun ServerItemCard(
    server: VpnServer,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) ElectricBlue else Color(0x2200F0FF)

    GlassmorphicCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("server_item_${server.id}")
            .clickable(onClick = onSelect),
        borderColor = borderColor,
        borderWidth = if (isSelected) 2.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Country Flag Badge
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(CyberDarkSurface)
                        .border(1.dp, Color(0x3300F0FF), CircleShape)
                ) {
                    Text(
                        text = server.flagEmoji,
                        fontSize = 22.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = server.countryName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (server.isPremium) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = "VIP Server",
                                tint = VividOrange,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Text(
                        text = "${server.cityName} • ${server.ipAddress}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Server Load Progress Bar
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LinearProgressIndicator(
                            progress = { server.serverLoadPercentage / 100f },
                            modifier = Modifier
                                .width(70.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = when {
                                server.serverLoadPercentage < 50 -> GreenConnected
                                server.serverLoadPercentage < 80 -> YellowConnecting
                                else -> VividOrange
                            },
                            trackColor = Color(0x33FFFFFF)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${server.serverLoadPercentage}% load",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Ping Badge
                val pingColor = when {
                    server.pingMs < 35 -> GreenConnected
                    server.pingMs < 75 -> YellowConnecting
                    else -> VividOrange
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(pingColor.copy(alpha = 0.15f))
                        .border(1.dp, pingColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${server.pingMs} ms",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = pingColor
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Favorite Toggle
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (server.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (server.isFavorite) VividOrange else TextMuted
                    )
                }

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = ElectricBlue,
                        modifier = Modifier
                            .padding(start = 2.dp)
                            .size(24.dp)
                    )
                }
            }
        }
    }
}
