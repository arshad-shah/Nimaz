package com.arshadshah.nimaz.presentation.components.atoms

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * Semantic colour role for a [NimazCheckbox]. The role's fill tints the box when
 * checked (and the box border when unchecked, at reduced alpha); its on-fill colour
 * is the check glyph's contrast colour. Roles map onto the app's Material 3 scheme so
 * they track theming automatically — pass an explicit `tint` to [NimazCheckbox] to
 * escape them.
 */
enum class NimazCheckboxVariant {
    /** The faithful default check tint. `primary` / `onPrimary`. */
    DEFAULT,

    /** Brand emphasis. `primary` / `onPrimary`. */
    PRIMARY,

    /** Positive / completed (prayer & fast completion). `NimazColors.Success`. */
    SUCCESS,

    /** Destructive / alert. `error` / `onError`. */
    ERROR
}

/**
 * Size preset for a [NimazCheckbox]: the box dimension, the inner check glyph size,
 * the unchecked border stroke, and the [NimazCheckboxType.SQUARE] corner radius.
 */
enum class NimazCheckboxSize(
    val box: Dp,
    val check: Dp,
    val stroke: Dp,
    val corner: Dp
) {
    SMALL(18.dp, 12.dp, 1.5.dp, 6.dp),
    MEDIUM(22.dp, 14.dp, 2.dp, 7.dp),
    LARGE(28.dp, 18.dp, 2.dp, 9.dp)
}

/**
 * Shape of a [NimazCheckbox]: the classic rounded [SQUARE] (Material-style) or the
 * [CIRCLE] check toggle the app uses for prayer / fast completion.
 */
enum class NimazCheckboxType {
    SQUARE,
    CIRCLE
}

/**
 * The app's single boolean check-toggle primitive.
 *
 * Replaces the hand-built "bordered box / circle that shows a `Check` when selected"
 * pattern that was copy-pasted across the prayer & fast trackers, the Quran/settings
 * pickers and the dropdown/list selection rows, with one variant/size/type-driven
 * entry point. Switches stay with Material 3 `Switch`; genuine single-choice pickers
 * keep their `RadioButton`.
 *
 * Unchecked renders an outlined box; checked animates to a filled box with a centred
 * check glyph in the contrast colour. Colour comes from [variant] (a semantic theme
 * role) unless [tint] is given.
 *
 * Pass `onCheckedChange = null` for a **display-only indicator** — the box renders
 * its checked/unchecked state but is not interactive and carries no click semantics.
 * This is the drop-in for selected-card rows where the parent card owns the click.
 *
 * @param checked whether the box is currently checked.
 * @param onCheckedChange invoked with the toggled value on tap; `null` makes the box
 *   a non-interactive indicator (the card-selection drop-in).
 * @param variant semantic colour role; [tint] overrides it.
 * @param size box / check / stroke / corner preset.
 * @param type rounded square or circle.
 * @param enabled when false, dims the box and blocks interaction.
 * @param tint escape hatch overriding [variant]'s fill colour (brand `NimazColors.*`
 *   or a runtime colour); the check glyph stays the variant's contrast colour.
 * @param contentDescription accessibility label for the toggle.
 */
@Composable
fun NimazCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    variant: NimazCheckboxVariant = NimazCheckboxVariant.PRIMARY,
    size: NimazCheckboxSize = NimazCheckboxSize.MEDIUM,
    type: NimazCheckboxType = NimazCheckboxType.SQUARE,
    enabled: Boolean = true,
    tint: Color? = null,
    contentDescription: String? = null,
) {
    val fill = tint ?: variant.resolveFill()
    val onFill = variant.resolveOnFill()
    val shape: Shape = when (type) {
        NimazCheckboxType.SQUARE -> RoundedCornerShape(size.corner)
        NimazCheckboxType.CIRCLE -> CircleShape
    }

    // Animate the border thickness so the box reads as "growing a fill" when checked
    // rather than hard-cutting between the two states.
    val borderWidth by animateDpAsState(
        targetValue = if (checked) 0.dp else size.stroke,
        label = "NimazCheckbox border"
    )

    val interactionModifier = if (onCheckedChange != null) {
        Modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Checkbox,
            onValueChange = onCheckedChange
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .then(interactionModifier)
            .size(size.box)
            .clip(shape)
            .background(if (checked) fill else Color.Transparent)
            .border(BorderStroke(borderWidth, fill.copy(alpha = UNCHECKED_BORDER_ALPHA)), shape),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = checked,
            enter = fadeIn() + scaleIn(initialScale = 0.6f),
            exit = fadeOut() + scaleOut(targetScale = 0.6f)
        ) {
            NimazIcon(
                imageVector = Icons.Default.Check,
                contentDescription = contentDescription,
                iconSize = size.check,
                tint = onFill
            )
        }
    }
}

private const val DISABLED_ALPHA = 0.38f
private const val UNCHECKED_BORDER_ALPHA = 0.55f

/** Box fill / unchecked-border colour for the variant. */
@Composable
private fun NimazCheckboxVariant.resolveFill(): Color = when (this) {
    NimazCheckboxVariant.DEFAULT -> MaterialTheme.colorScheme.primary
    NimazCheckboxVariant.PRIMARY -> MaterialTheme.colorScheme.primary
    NimazCheckboxVariant.SUCCESS -> NimazColors.Success
    NimazCheckboxVariant.ERROR -> MaterialTheme.colorScheme.error
}

/** Check-glyph contrast colour for the variant. */
@Composable
private fun NimazCheckboxVariant.resolveOnFill(): Color = when (this) {
    NimazCheckboxVariant.DEFAULT -> MaterialTheme.colorScheme.onPrimary
    NimazCheckboxVariant.PRIMARY -> MaterialTheme.colorScheme.onPrimary
    NimazCheckboxVariant.SUCCESS -> Color.White
    NimazCheckboxVariant.ERROR -> MaterialTheme.colorScheme.onError
}


// ==================== PREVIEWS ====================

@Composable
private fun NimazCheckboxVariantsShowcase() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NimazCheckboxVariant.entries.forEach { variant ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NimazCheckbox(checked = false, onCheckedChange = {}, variant = variant)
                NimazCheckbox(checked = true, onCheckedChange = {}, variant = variant)
                NimazCheckbox(
                    checked = true,
                    onCheckedChange = {},
                    variant = variant,
                    type = NimazCheckboxType.CIRCLE
                )
                Text(variant.name, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun NimazCheckboxSizesShowcase() {
    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NimazCheckboxSize.entries.forEach { size ->
            NimazCheckbox(checked = true, onCheckedChange = {}, size = size)
        }
        NimazCheckbox(checked = false, onCheckedChange = {}, enabled = false)
        // Display-only indicator (no handler).
        NimazCheckbox(checked = true, onCheckedChange = null, variant = NimazCheckboxVariant.SUCCESS)
    }
}

@Composable
private fun NimazCheckboxShowcase() {
    Column(
        modifier = Modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Variants (square off · square on · circle on)", style = MaterialTheme.typography.labelMedium)
        NimazCheckboxVariantsShowcase()
        Text("Sizes · disabled · indicator", style = MaterialTheme.typography.labelMedium)
        NimazCheckboxSizesShowcase()
    }
}

@Preview(showBackground = true, name = "NimazCheckbox — Light")
@Composable
private fun NimazCheckboxLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { NimazCheckboxShowcase() }
}

@Preview(
    showBackground = true, name = "NimazCheckbox — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun NimazCheckboxDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { NimazCheckboxShowcase() }
}
