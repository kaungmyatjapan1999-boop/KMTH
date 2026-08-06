package com.example.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.VividOrange

@Composable
fun FrostedMeshBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF05060A))
    ) {
        // Ambient Mesh Orbs Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Top Right Blue Mesh Orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ElectricBlue.copy(alpha = 0.25f),
                        ElectricBlue.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.9f, height * 0.1f),
                    radius = width * 0.65f
                ),
                center = Offset(width * 0.9f, height * 0.1f),
                radius = width * 0.65f
            )

            // Bottom Left Purple Mesh Orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonPurple.copy(alpha = 0.30f),
                        NeonPurple.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.1f, height * 0.75f),
                    radius = width * 0.70f
                ),
                center = Offset(width * 0.1f, height * 0.75f),
                radius = width * 0.70f
            )

            // Middle Right Vivid Orange Mesh Orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        VividOrange.copy(alpha = 0.20f),
                        VividOrange.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.85f, height * 0.45f),
                    radius = width * 0.55f
                ),
                center = Offset(width * 0.85f, height * 0.45f),
                radius = width * 0.55f
            )
        }

        // Screen Content
        content()
    }
}
