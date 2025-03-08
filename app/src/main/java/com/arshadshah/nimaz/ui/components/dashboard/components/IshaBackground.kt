package com.arshadshah.nimaz.ui.components.dashboard.components

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.EaseInOutQuad
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Pre-compute moon crater data
private data class MoonCrater(
    val xOffset: Float,
    val yOffset: Float,
    val sizeRatio: Float
)

// Pre-compute star data for more efficient rendering
private data class EnhancedStar(
    val x: Float,
    val y: Float,
    val size: Float,
    val twinkleSpeed: Float,
    val phaseOffset: Float,
    val colorTemp: Float,
    val needsGlow: Boolean,       // Pre-compute if star needs glow
    val needsExtraGlow: Boolean,  // Pre-compute if star needs extra glow
    val colorIndex: Int           // Pre-compute color index (0-3)
)

@Composable
fun IshaBackground(modifier: Modifier = Modifier) {
    // Cache colors to avoid recreating them during recompositions
    val topColor1 = remember { Color(0xFF0D1321) }
    val topColor2 = remember { Color(0xFF0A0F1A) }
    val midColor1 = remember { Color(0xFF1A237E) }
    val midColor2 = remember { Color(0xFF151F69) }
    val bottomColor1 = remember { Color(0xFF311B92) }
    val bottomColor2 = remember { Color(0xFF281563) }

    // Moon colors
    val moonGlow1 = remember { Color(0xFFE1F5FE) }
    val moonGlow2 = remember { Color(0xFFB3E5FC) }
    val moonGlow3 = remember { Color(0xFFE3F2FD) }
    val moonGlow4 = remember { Color(0xFFBBDEFB) }
    val moonSurface1 = remember { Color(0xFFFAFAFA) }
    val moonSurface2 = remember { Color(0xFFF5F5F5) }
    val moonSurface3 = remember { Color(0xFFEEEEEE) }
    val craterColor = remember { Color(0xFFE0E0E0) }
    val craterShadowColor = remember { Color(0xFFBDBDBD) }

    // Mist colors
    val mistColor1 = remember { Color(0xFF424242) }
    val mistColor2 = remember { Color(0xFF212121) }

    // Nebula colors
    val nebulaColor1 = remember { Color(0x05B3E5FC) }
    val nebulaColor2 = remember { Color(0x055D4037) }

    // Transparent color reused
    val transparent = remember { Color.Transparent }

    // Pre-compute math constants
    val piFloat = remember { PI.toFloat() }
    val piTwo = remember { 2f * piFloat }
    val piByDeg = remember { piFloat / 180f }

    // Single transition for all animations
    val infiniteTransition = rememberInfiniteTransition(label = "night animations")

    // Slow color shift for subtle sky variations
    val colorShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "night color shift"
    )

    // Moon animations
    val moonPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = piTwo,
        animationSpec = infiniteRepeatable(
            animation = tween(40000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "moon phase"
    )

    val moonGlow by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "moon glow intensity"
    )

    // Dynamic sky colors with subtle shifts - calculate only when colorShift changes
    val topColor = lerp(topColor1, topColor2, colorShift)
    val midColor = lerp(midColor1, midColor2, colorShift)
    val bottomColor = lerp(bottomColor1, bottomColor2, colorShift)

    // Create sky gradient once per composition update
    val skyGradient = remember(topColor, midColor, bottomColor) {
        Brush.verticalGradient(
            colors = listOf(topColor, midColor, bottomColor)
        )
    }

    // Pre-compute moon glow alpha values
    val outerGlowAlpha = 0.06f * moonGlow
    val outerGlowAlpha2 = 0.03f * moonGlow
    val largeGlowAlpha = 0.2f * moonGlow
    val largeGlowAlpha2 = 0.1f * moonGlow
    val mediumGlowAlpha = 0.4f * moonGlow
    val mediumGlowAlpha2 = 0.2f * moonGlow
    val closeGlowAlpha = 0.7f * moonGlow
    val closeGlowAlpha2 = 0.4f * moonGlow

    // Pre-compute mist alpha values
    val mistAlpha1 = 0.1f
    val mistAlpha2 = 0.05f

    // Get density once
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    // Pre-compute moon position and size
    val screenWidth = remember(configuration) {
        density.run { configuration.screenWidthDp.dp.toPx() }
    }
    val screenHeight = remember(configuration) {
        density.run { configuration.screenHeightDp.dp.toPx() }
    }
    val moonX = remember(screenWidth, density) {
        screenWidth - 60f * density.density
    }
    val moonY = remember(density) {
        40f * density.density
    }
    val baseMoonRadius = remember(density) {
        30f * density.density
    }

    // Pre-compute moon crater data
    val moonCraters = remember {
        listOf(
            MoonCrater(0.2f, -0.3f, 0.15f),
            MoonCrater(-0.3f, 0.1f, 0.2f),
            MoonCrater(0.1f, 0.25f, 0.12f),
            MoonCrater(-0.15f, -0.2f, 0.1f),
            MoonCrater(0.25f, 0.1f, 0.08f),
            MoonCrater(-0.1f, 0.35f, 0.07f),
            MoonCrater(0.35f, -0.1f, 0.06f)
        )
    }

    // Pre-compute nebula positions
    val nebulae = remember {
        List(3) { i ->
            Triple(
                0.2f + 0.6f * i / 3f,  // x position (relative)
                0.2f + 0.5f * (i % 2), // y position (relative)
                0.3f + 0.2f * (i % 3) / 3f // radius (relative)
            )
        }
    }

    Box(modifier = modifier.background(skyGradient)) {
        // Nebula/cosmic dust effect for depth
        Canvas(modifier = Modifier.fillMaxSize()) {
            nebulae.forEach { (xRatio, yRatio, radiusRatio) ->
                val x = size.width * xRatio
                val y = size.height * yRatio
                val radius = size.width * radiusRatio

                val nebulaBrush = Brush.radialGradient(
                    colors = listOf(
                        nebulaColor1,
                        nebulaColor2,
                        transparent
                    ),
                    center = Offset(x, y),
                    radius = radius
                )

                drawCircle(
                    brush = nebulaBrush,
                    center = Offset(x, y),
                    radius = radius
                )
            }
        }

        // Enhanced starry background
        EnhancedStarryBackground(
            maxStars = 300,
            modifier = Modifier.fillMaxSize(),
            colorShift = colorShift
        )

        // Moon and glows
        Canvas(modifier = Modifier.fillMaxSize()) {
            val moonCenter = Offset(moonX, moonY)

            // Pre-calculate moon radii
            val outerGlowRadius = baseMoonRadius * 8f
            val largeGlowRadius = baseMoonRadius * 4f
            val mediumGlowRadius = baseMoonRadius * 2.5f
            val closeGlowRadius = baseMoonRadius * 1.5f

            // Outermost atmospheric glow
            val outerGlowBrush = Brush.radialGradient(
                colors = listOf(
                    moonGlow1.copy(alpha = outerGlowAlpha),
                    moonGlow2.copy(alpha = outerGlowAlpha2),
                    transparent
                ),
                center = moonCenter,
                radius = outerGlowRadius
            )

            drawCircle(
                brush = outerGlowBrush,
                center = moonCenter,
                radius = outerGlowRadius
            )

            // Large moon glow
            val largeGlowBrush = Brush.radialGradient(
                colors = listOf(
                    moonGlow1.copy(alpha = largeGlowAlpha),
                    moonGlow2.copy(alpha = largeGlowAlpha2),
                    transparent
                ),
                center = moonCenter,
                radius = largeGlowRadius
            )

            drawCircle(
                brush = largeGlowBrush,
                center = moonCenter,
                radius = largeGlowRadius
            )

            // Medium moon glow
            val mediumGlowBrush = Brush.radialGradient(
                colors = listOf(
                    moonGlow3.copy(alpha = mediumGlowAlpha),
                    moonGlow4.copy(alpha = mediumGlowAlpha2),
                    transparent
                ),
                center = moonCenter,
                radius = mediumGlowRadius
            )

            drawCircle(
                brush = mediumGlowBrush,
                center = moonCenter,
                radius = mediumGlowRadius
            )

            // Close moon glow
            val closeGlowBrush = Brush.radialGradient(
                colors = listOf(
                    moonGlow3.copy(alpha = closeGlowAlpha),
                    moonGlow4.copy(alpha = closeGlowAlpha2),
                    transparent
                ),
                center = moonCenter,
                radius = closeGlowRadius
            )

            drawCircle(
                brush = closeGlowBrush,
                center = moonCenter,
                radius = closeGlowRadius
            )

            // The moon itself with realistic surface
            val moonSurfaceOffset = Offset(
                moonCenter.x + baseMoonRadius * 0.1f,
                moonCenter.y - baseMoonRadius * 0.1f
            )

            val moonSurfaceBrush = Brush.radialGradient(
                colors = listOf(
                    moonSurface1,
                    moonSurface2,
                    moonSurface3
                ),
                center = moonSurfaceOffset,
                radius = baseMoonRadius
            )

            drawCircle(
                brush = moonSurfaceBrush,
                center = moonCenter,
                radius = baseMoonRadius
            )

            // Moon terminator effect (shadow edge based on phase)
            val phaseOffset =
                (sin(moonPhase) * baseMoonRadius).coerceIn(-baseMoonRadius, baseMoonRadius)

            // Draw moon craters with a single pass through the list
            for (crater in moonCraters) {
                // Only draw craters not in the shadow
                if (moonCenter.x + baseMoonRadius * crater.xOffset > moonCenter.x + phaseOffset || phaseOffset >= 0) {
                    val craterX = moonCenter.x + baseMoonRadius * crater.xOffset
                    val craterY = moonCenter.y + baseMoonRadius * crater.yOffset
                    val craterRadius = baseMoonRadius * crater.sizeRatio
                    val craterCenter = Offset(craterX, craterY)

                    // Main crater
                    drawCircle(
                        color = craterColor.copy(alpha = 0.7f),
                        radius = craterRadius,
                        center = craterCenter
                    )

                    // Crater shadow
                    val shadowRadius = craterRadius * 0.8f
                    val shadowOffset = crater.sizeRatio * 0.2f * baseMoonRadius
                    val shadowCenter = Offset(
                        craterX + shadowOffset,
                        craterY + shadowOffset
                    )

                    drawCircle(
                        color = craterShadowColor.copy(alpha = 0.8f),
                        radius = shadowRadius,
                        center = shadowCenter
                    )
                }
            }
        }

        // Night mist/fog near the ground
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .align(Alignment.BottomCenter)
        ) {

            val mistBrush = Brush.verticalGradient(
                colors = listOf(
                    mistColor1.copy(alpha = mistAlpha1),
                    mistColor2.copy(alpha = mistAlpha2)
                )
            )

            // Create a wavy mist pattern once per draw
            val mistPath = Path().apply {
                moveTo(0f, size.height)

                // Pre-compute segments for the wave
                val segments = 20
                val width = size.width
                val height = size.height

                for (i in 0..segments) {
                    val x = width * (i / segments.toFloat())
                    val y = height * (0.7f + 0.3f * sin(i * 0.3f + colorShift * 4))
                    quadraticTo(
                        x - width * 0.025f,
                        height * (0.6f + 0.2f * sin(i * 0.5f + colorShift * 3)),
                        x,
                        y
                    )
                }

                lineTo(width, height)
                close()
            }

            drawPath(
                path = mistPath,
                brush = mistBrush,
                alpha = 0.4f
            )
        }
    }
}

@Composable
fun EnhancedStarryBackground(
    maxStars: Int,
    modifier: Modifier = Modifier,
    colorShift: Float = 0f
) {
    // Pre-compute star colors to avoid recreating them
    val starColor1 = remember { Color(0xFFF5F5F5) } // White-blue stars
    val starColor2 = remember { Color(0xFFFFFFFF) } // White stars
    val starColor3 = remember { Color(0xFFFFF8E1) } // Slightly yellow stars
    val starColor4 = remember { Color(0xFFFFE0B2) } // Orange-ish stars

    // Cached star colors array for faster lookup
    val starColors = remember { arrayOf(starColor1, starColor2, starColor3, starColor4) }

    // Pre-compute PI value
    val piFloat = remember { PI.toFloat() }
    val piTwo = remember { 2f * piFloat }
    val piByDeg = remember { piFloat / 180f }

    // Generate stars with varied properties - with optimization flags
    val stars = remember {
        List(maxStars) {
            val x = Random.nextFloat()
            val y = Random.nextFloat()
            val size = when {
                Random.nextFloat() > 0.97f -> 3f + Random.nextFloat() * 1.5f // Few large stars
                Random.nextFloat() > 0.8f -> 2f + Random.nextFloat() * 1f // Medium stars
                else -> 0.8f + Random.nextFloat() * 1.2f // Most stars are small
            }
            val twinkleSpeed = 0.5f + Random.nextFloat() * 2f
            val phaseOffset = Random.nextFloat() * piTwo
            val colorTemp = Random.nextFloat() // Star color temperature

            // Pre-compute optimization flags
            val needsGlow = size > 2f
            val needsExtraGlow = size > 2.5f

            // Pre-compute color index for faster lookup
            val colorIndex = when {
                colorTemp > 0.85f -> 0 // White-blue stars
                colorTemp > 0.7f -> 1  // White stars
                colorTemp > 0.4f -> 2  // Slightly yellow stars
                else -> 3             // Orange-ish stars
            }

            EnhancedStar(
                x,
                y,
                size,
                twinkleSpeed,
                phaseOffset,
                colorTemp,
                needsGlow,
                needsExtraGlow,
                colorIndex
            )
        }
    }

    // Animation for twinkling effect - reuse the transition
    val infiniteTransition = rememberInfiniteTransition(label = "star twinkle")
    val twinkleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = piTwo,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
        ),
        label = "twinkle animation"
    )

    Canvas(modifier = modifier) {
        stars.forEach { star ->
            // Calculate brightness with sine wave for twinkling
            val sinValue =
                (0.5f + 0.5f * sin(twinkleProgress * star.twinkleSpeed + star.phaseOffset))
            val brightness = 0.3f + 0.7f * sinValue

            // Get star color from pre-computed index
            val starColor = starColors[star.colorIndex]
            val centerX = star.x * size.width
            val centerY = star.y * size.height
            val center = Offset(centerX, centerY)

            // Calculate radius with brightness adjustment
            val radius = star.size * (0.8f + 0.2f * brightness)

            // Draw star with calculated brightness
            drawCircle(
                color = starColor.copy(alpha = brightness),
                radius = radius,
                center = center
            )

            // Add glow to larger stars - only if needed
            if (star.needsGlow) {
                drawCircle(
                    color = starColor.copy(alpha = brightness * 0.4f),
                    radius = star.size * 2f,
                    center = center
                )

                // Extra glow and rays for very bright stars - only if needed
                if (star.needsExtraGlow && brightness > 0.7f) {
                    drawCircle(
                        color = starColor.copy(alpha = brightness * 0.2f),
                        radius = star.size * 4f,
                        center = center
                    )

                    // Simple ray effect for brightest stars
                    val rayCount = 4
                    val rayLineAlpha = brightness * 0.3f
                    val rayThickness = star.size * 0.5f
                    val rayColor = starColor.copy(alpha = rayLineAlpha)

                    for (i in 0 until rayCount) {
                        val angle = (i * 90f) * piByDeg // 360/4 = 90 degrees
                        val cosAngle = cos(angle)
                        val sinAngle = sin(angle)
                        val rayLength = star.size * (4f + 4f * brightness)

                        val startX = centerX + cosAngle * star.size
                        val startY = centerY + sinAngle * star.size
                        val endX = centerX + cosAngle * rayLength
                        val endY = centerY + sinAngle * rayLength

                        drawLine(
                            color = rayColor,
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = rayThickness
                        )
                    }
                }
            }
        }
    }
}