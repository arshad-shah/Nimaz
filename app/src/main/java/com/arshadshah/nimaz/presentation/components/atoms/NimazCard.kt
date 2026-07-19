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
import androidx.compose.foundation.layout.fillMaxSize
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
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardDefaults.onColorFor
import com.arshadshah.nimaz.presentation.theme.CardArtColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * Card style variants.
 *
 * - [FILLED] — solid container (the default).
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
    internal fun container(selected: Boolean) =
        if (selected) activeContainerColor else containerColor

    internal fun content(selected: Boolean) = if (selected) activeContentColor else contentColor
    internal fun border(selected: Boolean) = if (selected) activeBorderColor else borderColor
    internal fun strokeWidth(selected: Boolean) = if (selected) activeBorderWidth else borderWidth
}

/**
 * Semantic tone — *what a surface signifies*. Shared across the design system:
 * [NimazCard] and [NimazBadge] both take a tone, and each resolves it to its own
 * colours (a card wants a large tonal container; a badge wants contrast at 18.dp
 * with `labelSmall` text). Same vocabulary, per-primitive rendering.
 *
 * Tone is orthogonal to how a surface *sits* — [NimazCardStyle] for cards,
 * [NimazBadgeEmphasis] for badges.
 *
 * Call sites pick a tone by meaning and never specify raw colours, so the app has
 * one muted tint rather than the `surfaceVariant` @ 0.4/0.5/0.6 spread this axis
 * was introduced to replace.
 *
 * - [NEUTRAL] — the default surface.
 * - [MUTED] — a quiet, recessed surface (inset notes, secondary detail).
 * - [ACCENT] — brand-tinted, draws the eye (highlighted panels, hero surfaces).
 * - [PROMINENT] — the high-emphasis *filled* brand surface, for a card acting as a
 *   primary call to action. [ACCENT]'s `primaryContainer` is the low-emphasis tonal
 *   counterpart; collapsing the two demotes CTAs, so they stay distinct.
 * - [SUCCESS] — a completed//achieved state (streaks, finished khatam).
 * - [WARNING] — needs attention but is not an error (missed prayer, qada due).
 * - [ERROR] — a destructive or failed state.
 * - [TRANSPARENT] — no container at all; for cards laid over imagery or a
 *   gradient, where the backdrop must show through.
 */
enum class NimazTone {
    NEUTRAL,
    MUTED,
    ACCENT,
    PROMINENT,
    SUCCESS,
    WARNING,
    ERROR,
    TRANSPARENT
}

/**
 * Elevation rung for a [NimazTone.NEUTRAL] card.
 *
 * Material 3's `surface` → `surfaceContainer` → `surfaceContainerHigh` roles are an
 * intentional ladder: a card nested inside another card must step up a rung or its
 * boundary disappears. Naming the rungs keeps that expressive while stopping call
 * sites inventing their own — the sweep found the same "plain card" written six
 * different ways across the app.
 *
 * Only [NimazTone.NEUTRAL] varies by level; every other tone already has a
 * dedicated container role and ignores this.
 *
 * - [BASE] — a card directly on the screen background.
 * - [RAISED] — a card on an already-tinted section.
 * - [NESTED] — a card inside another card.
 */
enum class NimazCardLevel {
    BASE,
    RAISED,
    NESTED
}

object NimazCardDefaults {

    /** Default corner radius for every Nimaz card. */
    val Shape: Shape = RoundedCornerShape(16.dp)

    /**
     * Resolves a [NimazTone] into container/content colours.
     *
     * This is the single place the app decides what each tone *looks like* — every
     * card routes through here, so changing a tone restyles the whole app.
     *
     * Every tone resolves to an **opaque** Material container role rather than a
     * `.copy(alpha = …)` tint. Two reasons: opaque roles are contrast-checked in
     * light *and* dark, and [contentColorFor] can resolve a real `onXxxContainer`
     * for them — so [onColorFor] returns the correct content colour instead of
     * silently falling back to `onSurface` the way an alpha-tinted colour does.
     *
     * Feature-specific Islamic palette art (NimazColors/CardArtColors) is not
     * modelled here; it stays in the [GradientCard] / [PrayerCard] presets.
     */
    @Composable
    fun tone(
        tone: NimazTone,
        level: NimazCardLevel = NimazCardLevel.BASE,
    ): NimazCardColors {
        val container: Color = when (tone) {
            NimazTone.NEUTRAL -> when (level) {
                NimazCardLevel.BASE -> MaterialTheme.colorScheme.surface
                NimazCardLevel.RAISED -> MaterialTheme.colorScheme.surfaceContainer
                NimazCardLevel.NESTED -> MaterialTheme.colorScheme.surfaceContainerHigh
            }

            NimazTone.MUTED -> MaterialTheme.colorScheme.surfaceContainer
            NimazTone.ACCENT -> MaterialTheme.colorScheme.primaryContainer
            NimazTone.PROMINENT -> MaterialTheme.colorScheme.primary
            NimazTone.SUCCESS -> MaterialTheme.colorScheme.tertiaryContainer
            NimazTone.WARNING -> MaterialTheme.colorScheme.secondaryContainer
            NimazTone.ERROR -> MaterialTheme.colorScheme.errorContainer
            NimazTone.TRANSPARENT -> Color.Transparent
        }
        // Transparent has no on-colour of its own; inherit the surface's so text
        // stays legible against whatever shows through.
        val content: Color = if (tone == NimazTone.TRANSPARENT) {
            LocalContentColor.current
        } else {
            onColorFor(container)
        }
        return colors(container = container, content = content)
    }

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
 * @param tone the card's semantic meaning ([NimazTone]) — the preferred way to
 *   colour a card. Prefer this over passing bespoke [colors]: tones are the shared
 *   vocabulary, raw colours are how surfaces drift apart.
 * @param colors container/content/border. Defaults to [tone]; override only for a
 *   selectable card ([NimazCardDefaults.selectable]) or a genuine one-off.
 * @param gradient gradient stops for [NimazCardStyle.GRADIENT].
 * @param elevation overrides the resting elevation (default: Material's per-style).
 */
@Composable
fun NimazCard(
    modifier: Modifier = Modifier,
    style: NimazCardStyle = NimazCardStyle.FILLED,
    selected: Boolean = false,
    tone: NimazTone = NimazTone.NEUTRAL,
    level: NimazCardLevel = NimazCardLevel.BASE,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    shape: Shape = NimazCardDefaults.Shape,
    colors: NimazCardColors = NimazCardDefaults.tone(tone, level),
    gradient: List<Color>? = null,
    elevation: Dp? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val containerColor = colors.container(selected)
    val contentColor = colors.content(selected)
    val borderStroke =
        colors.border(selected)?.let { BorderStroke(colors.strokeWidth(selected), it) }
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
                    .then(
                        if (borderStroke != null) Modifier.border(
                            borderStroke,
                            shape
                        ) else Modifier
                    )
                    .then(
                        if (onClick != null) Modifier.clickable(
                            enabled = enabled,
                            onClick = onClick
                        )
                        else Modifier
                    )
            ) {
                CompositionLocalProvider(LocalContentColor provides contentColor) {
                    Column(modifier = Modifier.fillMaxSize(), content = content)
                }
            }
        }

        NimazCardStyle.ELEVATED -> {
            val cardColors = CardDefaults.elevatedCardColors(
                containerColor = containerColor,
                contentColor = contentColor,
            )
            val cardElevation =
                elevation?.let { CardDefaults.elevatedCardElevation(defaultElevation = it) }
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
            // Honour `elevation` here too. Previously this branch dropped it, so
            // an outlined card silently ignored the parameter — callers passing
            // `elevation = 0.dp` to guarantee flatness were writing a no-op.
            val cardElevation =
                elevation?.let { CardDefaults.outlinedCardElevation(defaultElevation = it) }
                    ?: CardDefaults.outlinedCardElevation()
            if (onClick != null) {
                OutlinedCard(
                    onClick = onClick, modifier = modifier, enabled = enabled, shape = shape,
                    colors = cardColors, elevation = cardElevation, border = borderStroke!!,
                    content = content,
                )
            } else {
                OutlinedCard(
                    modifier = modifier, shape = shape, colors = cardColors,
                    elevation = cardElevation, border = borderStroke!!, content = content,
                )
            }
        }

        NimazCardStyle.FILLED -> {
            val cardColors = CardDefaults.cardColors(
                containerColor = containerColor,
                contentColor = contentColor,
            )
            val cardElevation = elevation?.let { CardDefaults.cardElevation(defaultElevation = it) }
                ?: CardDefaults.cardElevation()
            if (onClick != null) {
                Card(
                    onClick = onClick,
                    modifier = modifier,
                    enabled = enabled,
                    shape = shape,
                    colors = cardColors,
                    elevation = cardElevation,
                    border = borderStroke,
                    content = content,
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
 * Showcase of every [NimazCardStyle] plus the gradient/tone/selectable variants,
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
        // Every semantic tone — container and content both come from the tone.
        NimazTone.entries.forEach { tone ->
            NimazCard(tone = tone) {
                Text(text = "Tone — ${tone.name}", modifier = Modifier.padding(16.dp))
            }
        }
        // Selectable — inactive then active, content colour inherited from the card.
        NimazCard(selected = false, colors = NimazCardDefaults.selectable()) {
            Text(text = "Selectable (inactive)", modifier = Modifier.padding(16.dp))
        }
        NimazCard(selected = true, colors = NimazCardDefaults.selectable()) {
            Text(text = "Selectable (active)", modifier = Modifier.padding(16.dp))
        }
        GradientCard(
            gradientColors = listOf(
                CardArtColors.IndigoGradientStart,
                CardArtColors.IndigoGradientEnd
            )
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

@Preview(
    showBackground = true, name = "Cards — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun NimazCardDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        NimazCardShowcase()
    }
}
