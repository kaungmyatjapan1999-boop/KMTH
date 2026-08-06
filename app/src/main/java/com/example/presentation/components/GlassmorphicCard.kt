package com.example.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyberGlassBorder

@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    borderColor: Color = CyberGlassBorder,
    borderWidth: Dp = 1.dp,
    elevation: Dp = 4.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = Color(0x333B82F6),
                spotColor = Color(0x33A855F7)
            )
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x1AFFFFFF), // Frosted top highlight
                        Color(0x0CFFFFFF)  // Translucent body
                    )
                )
            )
            .border(
                width = borderWidth,
                brush = Brush.linearGradient(
                    colors = listOf(
                        borderColor.copy(alpha = 0.5f),
                        borderColor.copy(alpha = 0.1f)
                    )
                ),
                shape = shape
            ),
        content = content
    )
}

