package com.arshadshah.nimaz.ui.components.dashboard.components

import androidx.compose.animation.core.EaseInOutQuad
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

// Precomputed nebula patch to avoid recreating during recomposition
private data class NebulaPatch(val x: Float, val y: Float, val radius: Float)

@Composable
fun FajrBackground(modifier: Modifier = Modifier) {
    // Cache colors to avoid recreating on each recomposition
    val topColorDark = remember { Color(0xFF0B1026) }
    val topColorLight = remember { Color(0xFF101530) }
    val midColorDark = remember { Color(0xFF192048) }
    val midColorLight = remember { Color(0xFF1E2552) }
    val bottomColorDark = remember { Color(0xFF283370) }
    val bottomColorLight = remember { Color(0xFF2E3980) }

    // Cache nebula colors
    val nebulaColor1 = remember { Color(0xFF3F51B5) }
    val nebulaColor2 = remember { Color(0xFF303F9F) }
    val transparent = remember { Color.Transparent }

    // Dawn glow colors
    val dawnColor1 = remember { Color(0xFF4D4D80) }
    val dawnColor2 = remember { Color(0xFF3A3A78) }
    val horizonColor1 = remember { Color(0xFF394382) }
    val horizonColor2 = remember { Color(0xFF4A5496) }

    // Pre-compute PI value
    val piFloat = remember { PI.toFloat() }

    // Generate nebula patches once and reuse
    val nebulaPatches = remember {
        List(4) {
            val x = 0.2f + 0.6f * Random.nextFloat()
            val y = 0.1f + 0.4f * Random.nextFloat()
            val radius = 0.15f + 0.2f * Random.nextFloat()
            NebulaPatch(x, y, radius)
        }
    }

    // Use a single InfiniteTransition for all animations
    val infiniteTransition = rememberInfiniteTransition(label = "animations")

    // Time-based animation for subtle sky color shifts
    val colorShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dawn shift"
    )

    // Dawn glow intensity animation - reuse the transition
    val dawnIntensity by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dawn intensity"
    )

    // Dynamic sky gradient with time-based color shift
    val topColor = lerp(topColorDark, topColorLight, colorShift)
    val midColor = lerp(midColorDark, midColorLight, colorShift)
    val bottomColor = lerp(bottomColorDark, bottomColorLight, colorShift)

    // Create gradients only once per composition
    val skyGradient = remember(topColor, midColor, bottomColor) {
        Brush.verticalGradient(listOf(topColor, midColor, bottomColor))
    }

    val dawnGlowAlpha1 = 0.3f * dawnIntensity
    val dawnGlowAlpha2 = 0.15f * dawnIntensity
    val dawnGlowGradient = remember(dawnGlowAlpha1, dawnGlowAlpha2) {
        Brush.verticalGradient(
            colors = listOf(
                dawnColor1.copy(alpha = dawnGlowAlpha1),
                dawnColor2.copy(alpha = dawnGlowAlpha2),
                transparent
            ),
            startY = Float.POSITIVE_INFINITY,
            endY = 0f
        )
    }

    val horizonGlowAlpha = 0.15f * dawnIntensity
    val horizonGradient = remember(horizonGlowAlpha) {
        Brush.horizontalGradient(
            colors = listOf(
                horizonColor1.copy(alpha = 0.05f),
                horizonColor2.copy(alpha = horizonGlowAlpha),
                horizonColor1.copy(alpha = 0.05f)
            )
        )
    }

    Box(modifier = modifier.background(skyGradient)) {
        // Subtle nebula/cosmic dust effect
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Calculate sin values once
            val sinColorShift = (1 + sin(colorShift * piFloat)).toFloat()
            val sinColorShiftOffset = (1 + sin(colorShift * piFloat + 0.5f)).toFloat()

            // Create several patches of nebula using precomputed locations
            nebulaPatches.forEach { patch ->
                val x = width * patch.x
                val y = height * patch.y
                val radius = width * patch.radius
                val center = Offset(x, y)

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            nebulaColor1.copy(alpha = 0.02f * sinColorShift),
                            nebulaColor2.copy(alpha = 0.01f * sinColorShiftOffset),
                            transparent
                        ),
                        center = center,
                        radius = radius
                    ),
                    center = center,
                    radius = radius
                )
            }
        }

        // Enhanced starry background with star twinkling
        StarryBackground(
            maxStars = 200,
            modifier = Modifier.fillMaxSize(),
            twinkleSpeed = 1.5f,
            brightnessRange = 0.3f to 0.9f
        )

        // Dawn glow on horizon
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .align(Alignment.BottomCenter)
                .background(dawnGlowGradient)
        )

        // Subtle horizontal gradient for dawn light
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .align(Alignment.BottomCenter)
                .background(horizonGradient)
        )
    }
}