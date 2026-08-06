package com.example.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.ConnectionState
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GreenConnected
import com.example.ui.theme.RedDisconnected
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.YellowConnecting

@Composable
fun KmthTopBar(
    connectionState: ConnectionState,
    ipAddress: String,
    onNavigateToDiagnostics: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Identity
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(1.dp, ElectricBlue.copy(alpha = 0.5f), CircleShape)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_kmth_logo_1786018192587),
                    contentDescription = "KMTH Logo",
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = "KMTH VPN",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    letterSpacing = 0.5.sp
                )

                // Status Badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (statusText, statusColor) = when (connectionState) {
                        ConnectionState.CONNECTED -> "SECURED" to GreenConnected
                        ConnectionState.CONNECTING -> "CONNECTING" to YellowConnecting
                        ConnectionState.DISCONNECTED -> "UNPROTECTED" to RedDisconnected
                        ConnectionState.DISCONNECTING -> "STOPPING" to YellowConnecting
                        ConnectionState.RECONNECTING -> "RECONNECTING" to YellowConnecting
                    }

                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$statusText • $ipAddress",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                }
            }
        }

        // Action Icons
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onNavigateToDiagnostics,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x22FFFFFF))
                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = "Diagnostics & Speed Test",
                    tint = ElectricBlue,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x22FFFFFF))
                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Security Settings",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
