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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

enum class NavTab(val route: String, val label: String, val icon: ImageVector) {
    HOME("dashboard", "HOME", Icons.Default.Shield),
    SERVERS("servers", "SERVERS", Icons.Default.Dns),
    USAGE("diagnostics", "USAGE", Icons.Default.Analytics),
    SETTINGS("settings", "SETTINGS", Icons.Default.Security)
}

@Composable
fun FrostedBottomNavBar(
    currentRoute: String,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x22FFFFFF),
                        Color(0x0CFFFFFF)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x33FFFFFF),
                        Color(0x05FFFFFF)
                    )
                ),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavTab.values().forEach { tab ->
                val isSelected = currentRoute == tab.route

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .testTag("nav_tab_${tab.route}")
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) {
                                    Brush.horizontalGradient(
                                        listOf(ElectricBlue.copy(alpha = 0.25f), NeonPurple.copy(alpha = 0.25f))
                                    )
                                } else {
                                    Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                }
                            )
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = if (isSelected) ElectricBlue else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = tab.label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) ElectricBlue else TextMuted,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}
