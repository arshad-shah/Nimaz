package com.arshadshah.nimaz.presentation.components.atoms

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.CardArtColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * Card style variants
 */
enum class NimazCardStyle {
    FILLED,
    ELEVATED,
    OUTLINED,
    GRADIENT
}

/**
 * Primary card component for Nimaz app with multiple styles.
 */
@Composable
fun NimazCard(
    modifier: Modifier = Modifier,
    style: NimazCardStyle = NimazCardStyle.FILLED,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(16.dp),
    colors: CardColors? = null,
    elevation: CardElevation? = null,
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    when (style) {
        NimazCardStyle.FILLED, NimazCardStyle.GRADIENT -> {
            // GRADIENT uses a filled card as its base; the gradient is applied in content.
            val cardColors = colors ?: CardDefaults.cardColors()
            val cardElevation = elevation ?: CardDefaults.cardElevation()
            if (onClick != null) {
                Card(
                    onClick = onClick,
                    modifier = modifier,
                    shape = shape,
                    colors = cardColors,
                    elevation = cardElevation,
                    content = content
                )
            } else {
                Card(
                    modifier = modifier,
                    shape = shape,
                    colors = cardColors,
                    elevation = cardElevation,
                    content = content
                )
            }
        }

        NimazCardStyle.ELEVATED -> {
            val cardColors = colors ?: CardDefaults.elevatedCardColors()
            val cardElevation = elevation ?: CardDefaults.elevatedCardElevation()
            if (onClick != null) {
                ElevatedCard(
                    onClick = onClick,
                    modifier = modifier,
                    shape = shape,
                    colors = cardColors,
                    elevation = cardElevation,
                    content = content
                )
            } else {
                ElevatedCard(
                    modifier = modifier,
                    shape = shape,
                    colors = cardColors,
                    elevation = cardElevation,
                    content = content
                )
            }
        }

        NimazCardStyle.OUTLINED -> {
            val cardColors = colors ?: CardDefaults.outlinedCardColors()
            val cardBorder = border ?: BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            if (onClick != null) {
                OutlinedCard(
                    onClick = onClick,
                    modifier = modifier,
                    shape = shape,
                    colors = cardColors,
                    border = cardBorder,
                    content = content
                )
            } else {
                OutlinedCard(
                    modifier = modifier,
                    shape = shape,
                    colors = cardColors,
                    border = cardBorder,
                    content = content
                )
            }
        }
    }
}

/**
 * Gradient card with customizable gradient colors.
 */
@Composable
fun GradientCard(
    modifier: Modifier = Modifier,
    gradientColors: List<Color>,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(Brush.linearGradient(gradientColors))
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
    ) {
        Column(content = content)
    }
}

/**
 * Flat, outlined "content card".
 *
 * Centralises the `surface` container + zero elevation + 1.dp `outline` border +
 * 16.dp corners combination that was copy-pasted across the home/today surfaces
 * (DuaOfTheMomentCard, HadithOfTheDayCard, FastingStatusCard, TodaysProgressCard,
 * JumuahCard). Pass [onClick] to make the whole card tappable, or keep the click
 * handling in the caller's [modifier].
 */
@Composable
fun NimazSurfaceCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(16.dp),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = CardDefaults.cardColors(containerColor = containerColor)
    val elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = border,
            content = content
        )
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = border,
            content = content
        )
    }
}

/**
 * Prayer-themed card with appropriate gradient colors.
 */
@Composable
fun PrayerCard(
    modifier: Modifier = Modifier,
    primaryColor: Color,
    secondaryColor: Color,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    GradientCard(
        modifier = modifier,
        gradientColors = listOf(primaryColor, secondaryColor),
        onClick = onClick,
        shape = shape,
        content = content
    )
}


// ==================== PREVIEWS ====================

/**
 * Showcase of every [NimazCardStyle] plus [GradientCard]/[PrayerCard] so each
 * variant is visually distinct (filled vs. elevated vs. outlined). Rendered in
 * both light and dark themes by the previews below.
 */
@Composable
private fun NimazCardShowcase() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        NimazCard(style = NimazCardStyle.FILLED) {
            Text(text = "Filled Card", modifier = Modifier.padding(16.dp))
        }
        NimazCard(style = NimazCardStyle.ELEVATED) {
            Text(text = "Elevated Card", modifier = Modifier.padding(16.dp))
        }
        NimazCard(style = NimazCardStyle.OUTLINED) {
            Text(text = "Outlined Card", modifier = Modifier.padding(16.dp))
        }
        GradientCard(
            gradientColors = listOf(CardArtColors.IndigoGradientStart, CardArtColors.IndigoGradientEnd)
        ) {
            Text(
                text = "Gradient Card",
                color = Color.White,
                modifier = Modifier.padding(16.dp)
            )
        }
        PrayerCard(
            primaryColor = CardArtColors.AmberPrimary,
            secondaryColor = CardArtColors.AmberSecondary
        ) {
            Text(
                text = "Prayer Card",
                color = Color.Black,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true, name = "Cards — Light")
@Composable
private fun NimazCardLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        NimazCardShowcase()
    }
}

@Preview(showBackground = true, name = "Cards — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun NimazCardDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        NimazCardShowcase()
    }
}

