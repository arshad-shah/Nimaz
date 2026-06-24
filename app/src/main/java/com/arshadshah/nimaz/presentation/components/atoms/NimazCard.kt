package com.arshadshah.nimaz.presentation.components.atoms

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.CardArtColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * Card style variants.
 *
 * - [FILLED] — solid container + a subtle 1.dp resting shadow (the default). The
 *   standard "content card": a softened-white surface lifted off the background.
 * - [ELEVATED] — solid container + a tonal shadow, no border.
 * - [OUTLINED] — solid container + a border (defaults to the theme outline).
 * - [GRADIENT] — the container is a linear gradient ([NimazCard]'s `gradient` param).
 */
enum class NimazCardStyle {
    FILLED,
    ELEVATED,
    OUTLINED,
    GRADIENT
}

/**
 * Container/content/border colours for a [NimazCard], with a distinct **active**
 * (selected) set. [NimazCard] picks the active or inactive triple based on its
 * `selected` flag; the resolved content colour is published via [LocalContentColor]
 * so child `Text`/`NimazIcon` inherit it without each call site re-specifying it.
 *
 * Build instances with [NimazCardDefaults.colors] (static) or
 * [NimazCardDefaults.selectable] (active/inactive) rather than the constructor.
 */
@Immutable
data class NimazCardColors(
    val containerColor: Color,
    val contentColor: Color,
    val borderColor: Color? = null,
    val borderWidth: Dp = 1.dp,
    val activeContainerColor: Color,
    val activeContentColor: Color,
    val activeBorderColor: Color? = null,
    val activeBorderWidth: Dp = 1.dp,
) {
    internal fun container(selected: Boolean) = if (selected) activeContainerColor else containerColor
    internal fun content(selected: Boolean) = if (selected) activeContentColor else contentColor
    internal fun border(selected: Boolean) = if (selected) activeBorderColor else borderColor
    internal fun strokeWidth(selected: Boolean) = if (selected) activeBorderWidth else borderWidth
}

object NimazCardDefaults {

    /** Default corner radius for every Nimaz card. */
    val Shape: Shape = RoundedCornerShape(16.dp)

    /**
     * A non-selectable card's colours — the active set mirrors the inactive set,
     * so a stray `selected = true` is harmless. [content] defaults to the correct
     * on-colour for [container] (falling back to `onSurface`).
     */
    @Composable
    fun colors(
        container: Color = MaterialTheme.colorScheme.surface,
        content: Color = onColorFor(container),
        border: Color? = null,
        borderWidth: Dp = 1.dp,
    ): NimazCardColors = NimazCardColors(
        containerColor = container,
        contentColor = content,
        borderColor = border,
        borderWidth = borderWidth,
        activeContainerColor = container,
        activeContentColor = content,
        activeBorderColor = border,
        activeBorderWidth = borderWidth,
    )

    /**
     * A selectable card's colours — distinct active vs inactive container/content/
     * border. Defaults match the common "quiet surface, primary-container when
     * active" pattern; override per feature (prayer, tasbih accent, settings…).
     */
    @Composable
    fun selectable(
        container: Color = MaterialTheme.colorScheme.surfaceVariant,
        content: Color = onColorFor(container),
        border: Color? = null,
        borderWidth: Dp = 1.dp,
        activeContainer: Color = MaterialTheme.colorScheme.primaryContainer,
        activeContent: Color = onColorFor(activeContainer),
        activeBorder: Color? = null,
        activeBorderWidth: Dp = 1.dp,
    ): NimazCardColors = NimazCardColors(
        containerColor = container,
        contentColor = content,
        borderColor = border,
        borderWidth = borderWidth,
        activeContainerColor = activeContainer,
        activeContentColor = activeContent,
        activeBorderColor = activeBorder,
        activeBorderWidth = activeBorderWidth,
    )

    /**
     * The flat "surface card" preset: a [MaterialTheme.colorScheme.surface]
     * container with a 1.dp outline and no elevation. Used by [NimazSurfaceCard].
     */
    @Composable
    fun surface(
        container: Color = MaterialTheme.colorScheme.surface,
        border: Color? = MaterialTheme.colorScheme.outline,
    ): NimazCardColors = colors(container = container, border = border)

    /** [contentColorFor] but falling back to `onSurface` for off-scheme colours. */
    @Composable
    fun onColorFor(background: Color): Color {
        val mapped = contentColorFor(background)
        return if (mapped == Color.Unspecified) MaterialTheme.colorScheme.onSurface else mapped
    }
}

/**
 * The single card primitive for Nimaz. Every card-like surface (filled, elevated,
 * outlined, gradient, and selectable active/inactive cards) is built from this —
 * not hand-rolled `Card`/`Surface`/`Box(clip+background)`.
 *
 * @param selected when the card has an active/selected state, drives which colour
 *   triple from [colors] is used (and the content colour published to children).
 * @param colors container/content/border, via [NimazCardDefaults.colors] /
 *   [NimazCardDefaults.selectable].
 * @param gradient gradient stops for [NimazCardStyle.GRADIENT].
 * @param elevation overrides the resting elevation (FILLED defaults to 1.dp; pass
 *   0.dp for a flat card).
 */
@Composable
fun NimazCard(
    modifier: Modifier = Modifier,
    style: NimazCardStyle = NimazCardStyle.FILLED,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    shape: Shape = NimazCardDefaults.Shape,
    colors: NimazCardColors = NimazCardDefaults.colors(),
    gradient: List<Color>? = null,
    elevation: Dp? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val containerColor = colors.container(selected)
    val contentColor = colors.content(selected)
    val borderStroke = colors.border(selected)?.let { BorderStroke(colors.strokeWidth(selected), it) }
        ?: if (style == NimazCardStyle.OUTLINED) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        } else null

    when (style) {
        NimazCardStyle.GRADIENT -> {
            val brush = gradient?.let { Brush.linearGradient(it) }
                ?: Brush.linearGradient(listOf(containerColor, containerColor))
            Box(
                modifier = modifier
                    .clip(shape)
                    .background(brush)
                    .then(if (borderStroke != null) Modifier.border(borderStroke, shape) else Modifier)
                    .then(
                        if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick)
                        else Modifier
                    )
            ) {
                CompositionLocalProvider(LocalContentColor provides contentColor) {
                    Column(content = content)
                }
            }
        }

        NimazCardStyle.ELEVATED -> {
            val cardColors = CardDefaults.elevatedCardColors(
                containerColor = containerColor,
                contentColor = contentColor,
            )
            val cardElevation = elevation?.let { CardDefaults.elevatedCardElevation(defaultElevation = it) }
                ?: CardDefaults.elevatedCardElevation()
            if (onClick != null) {
                ElevatedCard(
                    onClick = onClick, modifier = modifier, enabled = enabled, shape = shape,
                    colors = cardColors, elevation = cardElevation, content = content,
                )
            } else {
                ElevatedCard(
                    modifier = modifier, shape = shape, colors = cardColors,
                    elevation = cardElevation, content = content,
                )
            }
        }

        NimazCardStyle.OUTLINED -> {
            val cardColors = CardDefaults.outlinedCardColors(
                containerColor = containerColor,
                contentColor = contentColor,
            )
            if (onClick != null) {
                OutlinedCard(
                    onClick = onClick, modifier = modifier, enabled = enabled, shape = shape,
                    colors = cardColors, border = borderStroke!!, content = content,
                )
            } else {
                OutlinedCard(
                    modifier = modifier, shape = shape, colors = cardColors,
                    border = borderStroke!!, content = content,
                )
            }
        }

        NimazCardStyle.FILLED -> {
            val cardColors = CardDefaults.cardColors(
                containerColor = containerColor,
                contentColor = contentColor,
            )
            // Filled is the standard "content card": a softened-white `surface`
            // lifted off the background by a subtle 1.dp resting shadow (the look
            // shared by the More screen). Callers can override via `elevation`
            // (e.g. 0.dp for a flat card, or NimazSurfaceCard for outlined-flat).
            val cardElevation = CardDefaults.cardElevation(defaultElevation = elevation ?: 1.dp)
            if (onClick != null) {
                Card(
                    onClick = onClick, modifier = modifier, enabled = enabled, shape = shape,
                    colors = cardColors, elevation = cardElevation, border = borderStroke, content = content,
                )
            } else {
                Card(
                    modifier = modifier, shape = shape, colors = cardColors,
                    elevation = cardElevation, border = borderStroke, content = content,
                )
            }
        }
    }
}

/**
 * Gradient card preset — a [NimazCard] whose container is a linear gradient.
 */
@Composable
fun GradientCard(
    modifier: Modifier = Modifier,
    gradientColors: List<Color>,
    onClick: (() -> Unit)? = null,
    shape: Shape = NimazCardDefaults.Shape,
    content: @Composable ColumnScope.() -> Unit,
) {
    NimazCard(
        modifier = modifier,
        style = NimazCardStyle.GRADIENT,
        onClick = onClick,
        shape = shape,
        gradient = gradientColors,
        content = content,
    )
}

/**
 * Flat, outlined "content card" preset.
 *
 * Centralises the `surface` container + zero elevation + 1.dp `outline` border +
 * 16.dp corners combination shared by the home/today surfaces (DuaOfTheMomentCard,
 * HadithOfTheDayCard, FastingStatusCard, TodaysProgressCard, JumuahCard). Pass
 * [onClick] to make the whole card tappable, or keep the click handling in the
 * caller's [modifier].
 */
@Composable
fun NimazSurfaceCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = NimazCardDefaults.Shape,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color? = MaterialTheme.colorScheme.outline,
    content: @Composable ColumnScope.() -> Unit,
) {
    NimazCard(
        modifier = modifier,
        style = NimazCardStyle.FILLED,
        onClick = onClick,
        shape = shape,
        colors = NimazCardDefaults.surface(container = containerColor, border = borderColor),
        elevation = 0.dp,
        content = content,
    )
}

/**
 * Prayer-themed gradient card preset.
 */
@Composable
fun PrayerCard(
    modifier: Modifier = Modifier,
    primaryColor: Color,
    secondaryColor: Color,
    onClick: (() -> Unit)? = null,
    shape: Shape = NimazCardDefaults.Shape,
    content: @Composable ColumnScope.() -> Unit,
) {
    GradientCard(
        modifier = modifier,
        gradientColors = listOf(primaryColor, secondaryColor),
        onClick = onClick,
        shape = shape,
        content = content,
    )
}


// ==================== PREVIEWS ====================

/**
 * Showcase of every [NimazCardStyle] plus the gradient/surface/selectable variants,
 * rendered in both light and dark themes by the previews below.
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
        NimazSurfaceCard {
            Text(text = "Surface Card", modifier = Modifier.padding(16.dp))
        }
        // Selectable — inactive then active, content colour inherited from the card.
        NimazCard(selected = false, colors = NimazCardDefaults.selectable()) {
            Text(text = "Selectable (inactive)", modifier = Modifier.padding(16.dp))
        }
        NimazCard(selected = true, colors = NimazCardDefaults.selectable()) {
            Text(text = "Selectable (active)", modifier = Modifier.padding(16.dp))
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
