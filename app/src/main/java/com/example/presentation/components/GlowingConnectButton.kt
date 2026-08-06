package com.example.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ConnectionState
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GreenConnected
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.VividOrange
import com.example.ui.theme.YellowConnecting

@Composable
fun GlowingConnectButton(
    connectionState: ConnectionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "GlowAnimation")

    // Rotation angle for gradient sweep
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (connectionState == ConnectionState.CONNECTING) 1800 else 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Angle"
    )

    // Pulse scale for connecting/connected glow ring
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (connectionState == ConnectionState.CONNECTING) 1.32f else 1.20f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (connectionState == ConnectionState.CONNECTING) 800 else 1800,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    // Pulse alpha for background aura
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = if (connectionState == ConnectionState.CONNECTING) 0.85f else 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (connectionState == ConnectionState.CONNECTING) 800 else 1800,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    // Smooth state color transitions
    val targetMainColor = when (connectionState) {
        ConnectionState.DISCONNECTED -> ElectricBlue
        ConnectionState.CONNECTING -> YellowConnecting
        ConnectionState.CONNECTED -> GreenConnected
        ConnectionState.DISCONNECTING -> VividOrange
        ConnectionState.RECONNECTING -> YellowConnecting
    }

    val mainColor by animateColorAsState(
        targetValue = targetMainColor,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "MainColor"
    )

    val targetAuraColor = when (connectionState) {
        ConnectionState.DISCONNECTED -> ElectricBlue.copy(alpha = 0.4f)
        ConnectionState.CONNECTING -> YellowConnecting.copy(alpha = 0.8f)
        ConnectionState.CONNECTED -> GreenConnected.copy(alpha = 0.7f)
        ConnectionState.DISCONNECTING -> VividOrange.copy(alpha = 0.6f)
        ConnectionState.RECONNECTING -> YellowConnecting.copy(alpha = 0.8f)
    }

    val auraColor by animateColorAsState(
        targetValue = targetAuraColor,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "AuraColor"
    )

    val glowColors = when (connectionState) {
        ConnectionState.DISCONNECTED -> listOf(ElectricBlue, NeonPurple, VividOrange, ElectricBlue)
        ConnectionState.CONNECTING -> listOf(YellowConnecting, VividOrange, YellowConnecting)
        ConnectionState.CONNECTED -> listOf(GreenConnected, ElectricBlue, GreenConnected)
        ConnectionState.DISCONNECTING -> listOf(VividOrange, ElectricBlue, VividOrange)
        ConnectionState.RECONNECTING -> listOf(YellowConnecting, VividOrange, YellowConnecting)
    }

    val buttonSize = 180.dp

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(240.dp)
        ) {
            // Outer Glowing Aura Ring (Pulsing Yellow for Connecting, Solid Green for Connected)
            Box(
                modifier = Modifier
                    .size(buttonSize * pulseScale)
                    .alpha(if (connectionState == ConnectionState.CONNECTED || connectionState == ConnectionState.CONNECTING) pulseAlpha else pulseAlpha * 0.5f)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                auraColor,
                                auraColor.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Animated Gradient Border Ring
            Box(
                modifier = Modifier
                    .size(buttonSize + 16.dp)
                    .rotate(angle)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.sweepGradient(colors = glowColors)
                    )
                    .padding(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(CircleShape)
                        .background(Color(0xFF05060A))
                )
            }

            // Inner Clickable Button Surface
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(buttonSize)
                    .testTag("connect_button")
                    .clip(CircleShape)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF121520),
                                Color(0xFF0C0C12)
                            )
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                mainColor.copy(alpha = 0.8f),
                                mainColor.copy(alpha = 0.2f)
                            )
                        ),
                        shape = CircleShape
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (connectionState) {
                        ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Connecting",
                                tint = YellowConnecting,
                                modifier = Modifier
                                    .size(54.dp)
                                    .rotate(angle * 2)
                            )
                        }
                        ConnectionState.CONNECTED -> {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Connected",
                                tint = GreenConnected,
                                modifier = Modifier.size(54.dp)
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = "Connect",
                                tint = mainColor,
                                modifier = Modifier.size(54.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = when (connectionState) {
                            ConnectionState.DISCONNECTED -> "TAP TO CONNECT"
                            ConnectionState.CONNECTING -> "CONNECTING..."
                            ConnectionState.CONNECTED -> "CONNECTED"
                            ConnectionState.DISCONNECTING -> "STOPPING..."
                            ConnectionState.RECONNECTING -> "RECONNECTING"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = mainColor,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        Text(
            text = when (connectionState) {
                ConnectionState.CONNECTED -> "KMTH Ultra-Encrypted Tunnel Active"
                ConnectionState.CONNECTING -> "Establishing VLESS Reality Handshake..."
                ConnectionState.DISCONNECTED -> "Traffic Exposed • Select Node & Connect"
                ConnectionState.DISCONNECTING -> "Tearing down secure socket..."
                ConnectionState.RECONNECTING -> "Optimizing route for lowest latency..."
            },
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (connectionState == ConnectionState.CONNECTED) GreenConnected else TextMuted,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
