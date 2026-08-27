package com.arshadshah.nimaz.presentation.components.atoms

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeDefaults.SOFT_TINT_ALPHA
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeDefaults.container
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeDefaults.foreground
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeDefaults.solid
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazPalette
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * Badge size presets.
 */
enum class NimazBadgeSize(val height: Dp, val horizontalPadding: Dp, val verticalPadding: Dp) {
    SMALL(18.dp, 6.dp, 2.dp),
    MEDIUM(22.dp, 8.dp, 3.dp),
    LARGE(26.dp, 10.dp, 4.dp)
}

/**
 * How much visual weight a badge carries. Orthogonal to [NimazTone], which says
 * what the badge *means*.
 *
 * - [FILLED] — the tone's solid role with its on-colour. Highest contrast; use for
 *   the selected state of a tab pill, or a status that must not be missed.
 * - [SOFT] — the tone's tonal container role. The quiet default.
 * - [OUTLINED] — no fill, tone-coloured text and border.
 * - [CUTOUT] — a translucent "punched-out" well: a semi-opaque surface with a
 *   tone-tinted border, so whatever art sits behind (a gradient juz tile, a
 *   surah cartouche) shows through. Generalised from the Quran juz grid.
 */
enum class NimazBadgeEmphasis {
    FILLED,
    SOFT,
    OUTLINED,
    CUTOUT
}

/**
 * Badge silhouette. [PILL] is the default across the app; [ROUNDED] is for badges
 * that sit flush inside tighter art, such as the cutout page markers.
 */
enum class NimazBadgeShape {
    PILL,
    ROUNDED
}

/** Resolved badge colours. Build via [NimazBadgeDefaults]. */
@Immutable
data class NimazBadgeColors(
    val containerColor: Color,
    val contentColor: Color,
    val borderColor: Color? = null,
)

/** Diameter of the optional status dot rendered before a badge's label. */
private val INDICATOR_DOT_SIZE = 6.dp

object NimazBadgeDefaults {

    /** Opacity of the [NimazBadgeEmphasis.CUTOUT] well and its border. */
    private const val CUTOUT_FILL_ALPHA = 0.6f

    /** How far a tone is knocked back for a SOFT badge. Matches `NimazToneColors.container`. */
    private const val SOFT_TINT_ALPHA = 0.16f
    private const val CUTOUT_BORDER_ALPHA = 0.4f

    /**
     * The tone's **solid** role — high-contrast, paired with a real `onXxx`.
     *
     * Badges resolve differently from cards on purpose. A card is a large surface
     * where a tonal container reads clearly; a badge is ~18–22.dp tall with
     * `labelSmall` text, where the same container can be too faint. So
     * [NimazBadgeEmphasis.FILLED] reaches for the solid role while
     * [NimazBadgeEmphasis.SOFT] uses the tonal one — same [NimazTone] vocabulary,
     * rendering tuned to the primitive.
     */
    @Composable
    private fun solid(tone: NimazTone): Color = when (tone) {
        NimazTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceContainerHighest
        NimazTone.MUTED -> MaterialTheme.colorScheme.surfaceContainer
        NimazTone.ACCENT, NimazTone.PROMINENT -> MaterialTheme.colorScheme.primary
        // Green and amber, matching NimazToneColors — see the note on `foreground` below.
        NimazTone.SUCCESS -> NimazColors.Success
        NimazTone.WARNING -> NimazColors.Warning
        NimazTone.ERROR -> MaterialTheme.colorScheme.error
        NimazTone.TRANSPARENT -> Color.Transparent
    }

    /**
     * The tone's **foreground** colour — used for text and borders when there is
     * no filled container ([NimazBadgeEmphasis.OUTLINED], [NimazBadgeEmphasis.CUTOUT]).
     *
     * This is deliberately not [solid]: the neutral tones' solid roles are
     * *surface* colours, so drawing text or a border with them on a surface
     * background is invisible. Neutrals therefore resolve to `onSurfaceVariant`
     * here — the muted-but-legible content role that outlined metadata chips want.
     */
    @Composable
    private fun foreground(tone: NimazTone): Color = when (tone) {
        NimazTone.NEUTRAL, NimazTone.MUTED -> MaterialTheme.colorScheme.onSurfaceVariant
        NimazTone.ACCENT, NimazTone.PROMINENT -> MaterialTheme.colorScheme.primary
        // `NimazColors.Success` / `Warning`, not the scheme's tertiary / secondary. Those are a
        // deep purple and the brand gold here, so a SUCCESS badge came out purple beside a
        // SUCCESS dot drawn green by `NimazToneColors`. One tone, one colour.
        NimazTone.SUCCESS -> NimazColors.Success
        NimazTone.WARNING -> NimazColors.Warning
        NimazTone.ERROR -> MaterialTheme.colorScheme.error
        NimazTone.TRANSPARENT -> LocalContentColor.current
    }

    /** The tone's border colour for outlined/cutout badges. */
    @Composable
    private fun outline(tone: NimazTone): Color = when (tone) {
        NimazTone.NEUTRAL, NimazTone.MUTED -> MaterialTheme.colorScheme.outlineVariant
        else -> foreground(tone)
    }

    /** The tone's **tonal container** role — the quiet counterpart to [solid]. */
    @Composable
    private fun container(tone: NimazTone): Color = when (tone) {
        NimazTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceContainerHighest
        NimazTone.MUTED -> MaterialTheme.colorScheme.surfaceContainer
        NimazTone.ACCENT, NimazTone.PROMINENT -> MaterialTheme.colorScheme.primaryContainer
        // Composited so the badge stays opaque and `onColorFor` can read it.
        NimazTone.SUCCESS -> NimazColors.Success
            .copy(alpha = SOFT_TINT_ALPHA)
            .compositeOver(MaterialTheme.colorScheme.surface)

        NimazTone.WARNING -> NimazColors.Warning
            .copy(alpha = SOFT_TINT_ALPHA)
            .compositeOver(MaterialTheme.colorScheme.surface)

        NimazTone.ERROR -> MaterialTheme.colorScheme.errorContainer
        NimazTone.TRANSPARENT -> Color.Transparent
    }

    /**
     * The **on-container** colour a [NimazBadgeEmphasis.SOFT] badge's text should use.
     *
     * [container] draws from two different kinds of colour, and they need two different pairing
     * strategies:
     *  - [NimazTone.ACCENT]/[NimazTone.PROMINENT] and [NimazTone.ERROR] resolve to real Material
     *    container roles (`primaryContainer`, `errorContainer`), and Material ships each of those
     *    with a paired `onXxxContainer` role already tuned for contrast against it — that pairing
     *    is what makes a SOFT badge read as one coloured object instead of body text sitting on a
     *    tint. Picking it by luminance instead (black or white) threw that pairing away and drew
     *    black text on every light container, including the green "On time" pill this fixes.
     *  - [NimazTone.SUCCESS] and [NimazTone.WARNING] draw from [NimazColors] composited over the
     *    surface at [SOFT_TINT_ALPHA], and the two [NimazTone.NEUTRAL]/[NimazTone.MUTED] surface
     *    roles are not "containers" at all, just tonal greys — none of the four are real Material
     *    container roles, so there is no `onXxxContainer` to pair with. [foreground] is already
     *    the app's fallback for exactly this shape of problem — it is what
     *    [NimazBadgeEmphasis.OUTLINED] and [NimazBadgeEmphasis.CUTOUT] use to draw tone-coloured
     *    text with no container behind it at all — so SOFT reuses it rather than inventing a
     *    second fallback.
     */
    @Composable
    private fun softContentColor(tone: NimazTone): Color = when (tone) {
        NimazTone.ACCENT, NimazTone.PROMINENT -> MaterialTheme.colorScheme.onPrimaryContainer
        NimazTone.ERROR -> MaterialTheme.colorScheme.onErrorContainer
        NimazTone.NEUTRAL, NimazTone.MUTED, NimazTone.SUCCESS, NimazTone.WARNING,
        NimazTone.TRANSPARENT -> foreground(tone)
    }

    /**
     * Resolves a [tone] + [emphasis] into badge colours. This is the single place
     * the app decides what a badge looks like — every badge routes through here.
     */
    @Composable
    fun colors(
        tone: NimazTone = NimazTone.NEUTRAL,
        emphasis: NimazBadgeEmphasis = NimazBadgeEmphasis.SOFT,
    ): NimazBadgeColors {
        val solidColor = solid(tone)
        return when (emphasis) {
            NimazBadgeEmphasis.FILLED -> NimazBadgeColors(
                containerColor = solidColor,
                contentColor = NimazCardDefaults.onColorFor(solidColor),
            )

            NimazBadgeEmphasis.SOFT -> NimazBadgeColors(
                containerColor = container(tone),
                contentColor = softContentColor(tone),
            )

            NimazBadgeEmphasis.OUTLINED -> NimazBadgeColors(
                containerColor = Color.Transparent,
                contentColor = foreground(tone),
                borderColor = outline(tone),
            )

            NimazBadgeEmphasis.CUTOUT -> NimazBadgeColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = CUTOUT_FILL_ALPHA),
                contentColor = foreground(tone),
                borderColor = outline(tone).copy(alpha = CUTOUT_BORDER_ALPHA),
            )
        }
    }

    /**
     * Colours for a **feature-art** badge — an Islamic-palette colour from
     * [BadgeType] rather than a Material role.
     *
     * Feature art deliberately sits outside the [NimazTone] vocabulary (the same
     * decision the cards made), so it gets its own entry point instead of being
     * forced into a tone that would flatten the palette.
     */
    @Composable
    fun feature(
        color: Color,
        emphasis: NimazBadgeEmphasis = NimazBadgeEmphasis.FILLED,
    ): NimazBadgeColors = when (emphasis) {
        NimazBadgeEmphasis.FILLED -> NimazBadgeColors(
            containerColor = color,
            contentColor = Color.White,
        )

        NimazBadgeEmphasis.SOFT -> NimazBadgeColors(
            containerColor = color.copy(alpha = 0.15f),
            contentColor = color,
        )

        NimazBadgeEmphasis.OUTLINED -> NimazBadgeColors(
            containerColor = Color.Transparent,
            contentColor = color,
            borderColor = color,
        )

        NimazBadgeEmphasis.CUTOUT -> NimazBadgeColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = CUTOUT_FILL_ALPHA),
            contentColor = color,
            borderColor = color.copy(alpha = CUTOUT_BORDER_ALPHA),
        )
    }

    /** Resolves a [NimazBadgeShape] into a concrete [Shape]. */
    fun shapeOf(shape: NimazBadgeShape): Shape = when (shape) {
        NimazBadgeShape.PILL -> RoundedCornerShape(percent = 50)
        NimazBadgeShape.ROUNDED -> RoundedCornerShape(6.dp)
    }

    /**
     * Type scale for a badge [size]. Typography tracks the size preset so a LARGE
     * badge (a tab pill) does not render at the same `labelSmall` as a SMALL one
     * (a cutout page marker) — without this, migrating a `labelLarge` pill onto the
     * atom silently shrinks its text.
     */
    @Composable
    fun textStyleFor(size: NimazBadgeSize): TextStyle = when (size) {
        NimazBadgeSize.SMALL -> MaterialTheme.typography.labelSmall
        NimazBadgeSize.MEDIUM -> MaterialTheme.typography.labelMedium
        NimazBadgeSize.LARGE -> MaterialTheme.typography.labelLarge
    }
}

/**
 * The single badge/pill/status-label primitive for Nimaz.
 *
 * Every small label — status pills, tab pills, filter pills, grade chips, cutout
 * page markers — is built from this, not a hand-rolled `Surface`/`Box`. Call sites
 * pick a [tone] (meaning) and an [emphasis] (weight); they never pass raw colours.
 *
 * For a **selectable** pill (tabs, category filters), pass [selected]. The resting
 * look comes from [tone] + [emphasis]; the selected look from [selectedTone] at
 * [NimazBadgeEmphasis.FILLED]. The defaults (`NEUTRAL`/`SOFT` resting, `ACCENT`
 * filled when selected) match every tab pill in the app, so a tab is just
 * `NimazBadge(text, selected = …, onClick = …)` with no colour arguments at all.
 *
 * [tone] always applies when the badge is not selected, so a static
 * `NimazBadge(text, tone = ERROR)` renders as an error badge, not a neutral one.
 *
 * @param onClick makes the badge tappable; leave null for a static label.
 * @param selectedTone the tone used while [selected]; ignored otherwise.
 * @param indicatorColor draws a small filled dot before the label — a status
 *   indicator ("Active", a confidence level, an availability state) whose colour
 *   is independent of the badge's own tone. [icon] cannot express this: a dot is
 *   not an [ImageVector], and call sites that wanted one hand-rolled the whole
 *   pill to get it. Pass null for no dot; pass an [icon] instead when a glyph
 *   carries more meaning than a colour.
 * @param colors escape hatch for feature art — prefer [tone]/[emphasis]. Build it
 *   with [NimazBadgeDefaults.feature], as [StatusBadge] does.
 */
@Composable
fun NimazBadge(
    text: String,
    modifier: Modifier = Modifier,
    tone: NimazTone = NimazTone.NEUTRAL,
    emphasis: NimazBadgeEmphasis = NimazBadgeEmphasis.SOFT,
    shape: NimazBadgeShape = NimazBadgeShape.PILL,
    size: NimazBadgeSize = NimazBadgeSize.MEDIUM,
    icon: ImageVector? = null,
    indicatorColor: Color? = null,
    selected: Boolean = false,
    selectedTone: NimazTone = NimazTone.ACCENT,
    onClick: (() -> Unit)? = null,
    colors: NimazBadgeColors = if (selected) {
        NimazBadgeDefaults.colors(tone = selectedTone, emphasis = NimazBadgeEmphasis.FILLED)
    } else {
        NimazBadgeDefaults.colors(tone = tone, emphasis = emphasis)
    },
) {
    val resolvedShape = NimazBadgeDefaults.shapeOf(shape)
    Row(
        modifier = modifier
            .clip(resolvedShape)
            .background(colors.containerColor)
            .then(
                colors.borderColor?.let { Modifier.border(1.dp, it, resolvedShape) } ?: Modifier
            )
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            .padding(horizontal = size.horizontalPadding, vertical = size.verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (indicatorColor != null) {
            Box(
                modifier = Modifier
                    .size(INDICATOR_DOT_SIZE)
                    .clip(CircleShape)
                    .background(indicatorColor)
            )
        }
        if (icon != null) {
            NimazIcon(
                imageVector = icon,
                contentDescription = null,
                iconSize = 14.dp,
                tint = colors.contentColor
            )
        }
        Text(
            text = text,
            style = NimazBadgeDefaults.textStyleFor(size),
            // A selected pill carries a little extra weight on top of its filled
            // container, preserving the affordance hand-rolled pills got from bold.
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = colors.contentColor
        )
    }
}

/**
 * Status badge types — Islamic domain semantics mapped to the app's palette.
 *
 * This is feature art, not a [NimazTone]: "Sahih" and "Meccan" carry meaning the
 * Material scheme has no role for, so they keep their own colours.
 */
sealed class BadgeType(val label: String, val color: Color) {
    // Hadith grades
    object Sahih : BadgeType("Sahih", NimazPalette.MatGreen)
    object Hasan : BadgeType("Hasan", NimazPalette.LightGreen)
    object Daif : BadgeType("Da'if", NimazPalette.MatOrange)
    object Mawdu : BadgeType("Mawdu'", NimazPalette.MatRed)

    // Quran revelation types
    object Meccan : BadgeType("Meccan", NimazColors.QuranColors.Meccan)
    object Medinan : BadgeType("Medinan", NimazColors.QuranColors.Medinan)

    // Prayer status
    object Prayed : BadgeType("Prayed", NimazColors.StatusColors.Prayed)
    object Missed : BadgeType("Missed", NimazColors.StatusColors.Missed)
    object Pending : BadgeType("Pending", NimazColors.StatusColors.Pending)
    object Qada : BadgeType("Qada", NimazColors.StatusColors.Qada)
    object Jamaah : BadgeType("Jama'ah", NimazColors.StatusColors.Jamaah)

    // Fasting status
    object Fasted : BadgeType("Fasted", NimazColors.FastingColors.Fasted)
    object NotFasted : BadgeType("Not Fasted", NimazColors.FastingColors.NotFasted)
    object Makeup : BadgeType("Makeup", NimazColors.FastingColors.Makeup)
    object Exempted : BadgeType("Exempted", NimazColors.FastingColors.Exempted)

    // Custom
    data class Custom(
        private val customLabel: String,
        private val customColor: Color
    ) : BadgeType(customLabel, customColor)
}

/**
 * Typed badge using the Islamic domain palette ([BadgeType]).
 */
@Composable
fun StatusBadge(
    type: BadgeType,
    modifier: Modifier = Modifier,
    size: NimazBadgeSize = NimazBadgeSize.MEDIUM,
    emphasis: NimazBadgeEmphasis = NimazBadgeEmphasis.FILLED,
    shape: NimazBadgeShape = NimazBadgeShape.PILL,
) {
    NimazBadge(
        text = type.label,
        modifier = modifier,
        size = size,
        shape = shape,
        colors = NimazBadgeDefaults.feature(color = type.color, emphasis = emphasis)
    )
}

/**
 * Surah number badge for Quran — a fixed-size circular numeral, distinct from the
 * text badges above.
 */
@Composable
fun SurahNumberBadge(
    number: Int,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    tone: NimazTone = NimazTone.ACCENT,
) {
    val colors = NimazBadgeDefaults.colors(tone = tone, emphasis = NimazBadgeEmphasis.SOFT)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.containerColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = colors.contentColor
        )
    }
}

// ==================== PREVIEWS ====================

@Composable
private fun NimazBadgeShowcase() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Every emphasis, at the ACCENT tone.
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            NimazBadgeEmphasis.entries.forEach { emphasis ->
                NimazBadge(text = emphasis.name, tone = NimazTone.ACCENT, emphasis = emphasis)
            }
        }
        // Every tone, at SOFT emphasis -- the content colour must read as one coloured pill,
        // not black/white text sitting on a tint, in both this preview and its dark twin below.
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            NimazTone.entries.filter { it != NimazTone.TRANSPARENT }.forEach { tone ->
                NimazBadge(text = tone.name, tone = tone)
            }
        }
        // Selectable tab pills — unselected then selected.
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            NimazBadge(text = "All", tone = NimazTone.ACCENT, selected = false, onClick = {})
            NimazBadge(text = "Favourites", tone = NimazTone.ACCENT, selected = true, onClick = {})
        }
        // Feature-art status badges.
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatusBadge(type = BadgeType.Sahih)
            StatusBadge(type = BadgeType.Meccan)
            StatusBadge(type = BadgeType.Missed)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatusBadge(type = BadgeType.Prayed, emphasis = NimazBadgeEmphasis.SOFT)
            StatusBadge(type = BadgeType.Fasted, emphasis = NimazBadgeEmphasis.OUTLINED)
        }
        // Cutout markers, as used over Quran art.
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            NimazBadge(
                text = "21",
                tone = NimazTone.ACCENT,
                emphasis = NimazBadgeEmphasis.CUTOUT,
                shape = NimazBadgeShape.ROUNDED,
                size = NimazBadgeSize.SMALL
            )
            SurahNumberBadge(number = 36)
        }
    }
}

@Preview(showBackground = true, name = "Badges — Light")
@Composable
private fun NimazBadgeLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        NimazBadgeShowcase()
    }
}

@Preview(
    showBackground = true, name = "Badges — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun NimazBadgeDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        NimazBadgeShowcase()
    }
}
