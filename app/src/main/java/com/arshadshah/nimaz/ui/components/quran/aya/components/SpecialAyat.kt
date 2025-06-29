package com.arshadshah.nimaz.ui.components.quran.aya.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.data.local.models.LocalAya
import com.arshadshah.nimaz.ui.theme.urduFont
import com.arshadshah.nimaz.utils.DisplaySettings
import com.arshadshah.nimaz.utils.QuranUtils.getArabicFont
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SpecialAyat(
    aya: LocalAya,
    displaySettings: DisplaySettings,
    loading: Boolean
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ), label = "scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "shimmerAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {

        // Main content card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        // Arabic text with enhanced styling
                        Text(
                            text = aya.ayaArabic,
                            fontFamily = getArabicFont(displaySettings.arabicFont),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize =displaySettings.arabicFontSize.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = displaySettings.arabicFontSize.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(if (loading) shimmerAlpha else 1f)
                        )
                    }

                    // Decorative separator
                    DecorativeSeparator()

                    // Translation section
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (displaySettings.translation === "English" ) {
                            Text(
                                text = aya.translationEnglish,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = (displaySettings.translationFontSize + 4).sp,
                                    fontWeight = FontWeight.Normal,
                                    lineHeight = (displaySettings.translationFontSize + 8).sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .alpha(if (loading) shimmerAlpha else 1f)
                            )
                        }

                        if (displaySettings.translation === "Urdu") {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Text(
                                    text = aya.translationUrdu,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = (displaySettings.translationFontSize + 2).sp,
                                        fontWeight = FontWeight.Normal,
                                        fontFamily = urduFont,
                                        lineHeight = (displaySettings.translationFontSize + 6).sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign =TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .alpha(if (loading) shimmerAlpha else 1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DecorativeSeparator() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        // Left line with gradient
        Box(
            modifier = Modifier
                .weight(1f)
                .height(2.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFFD4AF37).copy(alpha = 0.8f),
                            Color(0xFFD4AF37)
                        )
                    )
                )
        )

        // Center ornament
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFD4AF37),
                            Color(0xFFB8860B)
                        )
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.4f))
                    .align(Alignment.Center)
            )
        }

        // Right line with gradient
        Box(
            modifier = Modifier
                .weight(1f)
                .height(2.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFD4AF37),
                            Color(0xFFD4AF37).copy(alpha = 0.8f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

private fun DrawScope.drawDecorativeRing(size: Size, color: Color) {
    val center = Offset(size.width / 2, size.height / 2)
    val radius = size.minDimension * 0.4f
    val strokeWidth = 1.5.dp.toPx()

    // Draw multiple concentric circles with varying opacity
    for (i in 0..2) {
        val currentRadius = radius + (i * 8.dp.toPx())
        val alpha = 1f - (i * 0.3f)

        drawCircle(
            color = color.copy(alpha = alpha),
            radius = currentRadius,
            center = center,
            style = Stroke(width = strokeWidth)
        )
    }

    // Draw decorative dots around the circles
    val dotCount = 24
    val dotRadius = 2.dp.toPx()

    for (i in 0 until dotCount) {
        val angle = (2 * Math.PI * i / dotCount).toFloat()
        val dotCenter = Offset(
            center.x + (radius + 16.dp.toPx()) * cos(angle),
            center.y + (radius + 16.dp.toPx()) * sin(angle)
        )

        drawCircle(
            color = color.copy(alpha = 0.6f),
            radius = dotRadius,
            center = dotCenter
        )
    }
}

private fun DrawScope.drawIslamicStar(
    center: Offset,
    radius: Float,
    primaryColor: Color,
    secondaryColor: Color
) {
    val points = 8
    val path = Path()

    // Create star path
    for (i in 0 until points * 2) {
        val angle = (Math.PI * i / points).toFloat()
        val currentRadius = if (i % 2 == 0) radius else radius * 0.5f
        val x = center.x + currentRadius * cos(angle)
        val y = center.y + currentRadius * sin(angle)

        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()

    // Draw filled star
    drawPath(
        path = path,
        color = secondaryColor.copy(alpha = 0.3f)
    )

    // Draw star outline
    drawPath(
        path = path,
        color = primaryColor,
        style = Stroke(width = 1.5.dp.toPx())
    )
}

@Preview
@Composable
fun SpecialAyatPreview() {
    val sampleAya = LocalAya(
        ayaArabic = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
        translationEnglish = "In the name of Allah, the Entirely Merciful, the Especially Merciful.",
        translationUrdu = "اللہ کے نام سے جو بہت مہربان نہایت رحم والا ہے۔",
        suraNumber = 1,
        ayaNumberInSurah = 0,
        bookmark = false,
        favorite = false,
        note = "",
        audioFileLocation = "",
        sajda = false,
        sajdaType = "",
        ruku = 1,
        juzNumber = 1,
        ayaNumberInQuran = 0
    )
    val displaySettings = DisplaySettings(
        translation = "Urdu",
    )

    SpecialAyat(
        aya = sampleAya,
        displaySettings = displaySettings,
        loading = false
    )
}