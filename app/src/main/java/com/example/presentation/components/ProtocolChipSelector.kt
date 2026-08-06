package com.example.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.VpnProtocol
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun ProtocolChipSelector(
    selectedProtocol: VpnProtocol,
    onProtocolSelected: (VpnProtocol) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        VpnProtocol.values().forEach { protocol ->
            val isSelected = protocol == selectedProtocol

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .testTag("protocol_chip_${protocol.name}")
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) {
                            Brush.horizontalGradient(listOf(ElectricBlue.copy(alpha = 0.25f), NeonPurple.copy(alpha = 0.25f)))
                        } else {
                            Brush.linearGradient(listOf(Color(0x1B1E293B), Color(0x1B1E293B)))
                        }
                    )
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) ElectricBlue else Color(0x2200F0FF),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onProtocolSelected(protocol) }
                    .padding(vertical = 8.dp, horizontal = 4.dp)
            ) {
                Text(
                    text = protocol.displayName.split(" ").first(),
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) ElectricBlue else TextMuted
                )
            }
        }
    }
}
