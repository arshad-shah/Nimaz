package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * Visual treatment of a [NimazButton].
 *
 * Four variants, two sizes, one shape — the result of collapsing 5 variants × 3 sizes × 2 shapes
 * into a grid where each cell has a written reason to exist.
 *
 * - [PRIMARY]: high-emphasis call to action. `primary` / `onPrimary`.
 * - [QUIET]: medium-emphasis. Filled `surfaceVariant`, no border. Replaces the old TONAL and OUTLINED.
 * - [DESTRUCTIVE]: irreversible action. `error` / `onError`.
 * - [TEXT]: lowest-emphasis link/cancel. No container.
 *
 * Deprecated aliases remain for source-compatibility and map to the nearest equivalent.
 */
enum class NimazButtonVariant {
    /** High-emphasis primary call to action. `primary` / `onPrimary`. */
    PRIMARY,

    /** Medium-emphasis. `surfaceVariant` container, `onSurfaceVariant` label. No border. */
    QUIET,

    /** Destructive action (delete, reset). `error` / `onError`. */
    DESTRUCTIVE,

    /** Lowest-emphasis, label only (links, dialog "cancel"). */
    TEXT,

    /** @suppress Use [PRIMARY]. */
    @Deprecated("Use PRIMARY", ReplaceWith("PRIMARY"))
    FILLED,

    /** @suppress Use [QUIET]. */
    @Deprecated("Use QUIET", ReplaceWith("QUIET"))
    TONAL,

    /** @suppress Use [QUIET]. */
    @Deprecated("Use QUIET", ReplaceWith("QUIET"))
    OUTLINED,
}

/**
 * Size preset for a [NimazButton]: 48dp [STANDARD] for screen-level actions,
 * 38dp [SMALL] for actions inside sheets, banners and cards.
 */
enum class NimazButtonSize(
    val height: Dp,
    val horizontalPadding: Dp,
    val iconSize: Dp,
    val gap: Dp,
    val corner: Dp
) {
    SMALL(38.dp, 14.dp, 16.dp, 6.dp, 12.dp),
    STANDARD(48.dp, 20.dp, 18.dp, 8.dp, 15.dp),

    /** @suppress Use [STANDARD]. */
    @Deprecated("Use STANDARD", ReplaceWith("STANDARD"))
    MEDIUM(48.dp, 20.dp, 18.dp, 8.dp, 15.dp),

    /** @suppress Use [STANDARD]. Deleted from the design system — maps to STANDARD. */
    @Deprecated("Use STANDARD", ReplaceWith("STANDARD"))
    LARGE(48.dp, 20.dp, 18.dp, 8.dp, 15.dp),
}

/**
 * @suppress PILL is removed. Buttons always use a rounded-rectangle shape.
 * Use the default shape and omit this parameter.
 */
@Deprecated("Button shape is always a rounded rectangle. Remove the type parameter.")
enum class NimazButtonType { STANDARD, PILL }

/**
 * Unified text / icon+text button for the Nimaz app.
 *
 * [loading] implies disabled — the button will not respond to clicks while the spinner
 * is shown. The label stays visible alongside the spinner so the button keeps its width.
 *
 * @param leadingIcon optional icon shown before the label.
 * @param loading shows a spinner next to the label; implies [enabled] = false.
 * @param fullWidth stretches to the available width.
 */
@Suppress("DEPRECATION")
@Composable
fun NimazButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: NimazButtonVariant = NimazButtonVariant.PRIMARY,
    size: NimazButtonSize = NimazButtonSize.STANDARD,
    type: NimazButtonType = NimazButtonType.STANDARD,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    fullWidth: Boolean = false,
) {
    val shape = RoundedCornerShape(size.corner)
    val contentPadding = PaddingValues(horizontal = size.horizontalPadding, vertical = 0.dp)
    val sizedModifier = modifier
        .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
        .height(size.height)
    val isEnabled = enabled && !loading

    val content: @Composable RowScope.() -> Unit = {
        ButtonContent(text = text, size = size, loading = loading, leadingIcon = leadingIcon)
    }

    // Normalise deprecated aliases to their replacements.
    val effectiveVariant = when (variant) {
        NimazButtonVariant.FILLED -> NimazButtonVariant.PRIMARY
        NimazButtonVariant.TONAL, NimazButtonVariant.OUTLINED -> NimazButtonVariant.QUIET
        else -> variant
    }

    when (effectiveVariant) {
        NimazButtonVariant.PRIMARY -> Button(
            onClick = onClick,
            modifier = sizedModifier,
            enabled = isEnabled,
            shape = shape,
            colors = ButtonDefaults.buttonColors(),
            contentPadding = contentPadding,
            content = content,
        )

        NimazButtonVariant.QUIET -> FilledTonalButton(
            onClick = onClick,
            modifier = sizedModifier,
            enabled = isEnabled,
            shape = shape,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            contentPadding = contentPadding,
            content = content,
        )

        NimazButtonVariant.DESTRUCTIVE -> Button(
            onClick = onClick,
            modifier = sizedModifier,
            enabled = isEnabled,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
            contentPadding = contentPadding,
            content = content,
        )

        NimazButtonVariant.TEXT -> TextButton(
            onClick = onClick,
            modifier = sizedModifier,
            enabled = isEnabled,
            shape = shape,
            colors = ButtonDefaults.textButtonColors(),
            contentPadding = contentPadding,
            content = content,
        )

        // Unreachable after alias normalisation above; exhaustive when branch.
        else -> Button(
            onClick = onClick,
            modifier = sizedModifier,
            enabled = isEnabled,
            shape = shape,
            contentPadding = contentPadding,
            content = content,
        )
    }
}

@Composable
private fun RowScope.ButtonContent(
    text: String,
    size: NimazButtonSize,
    loading: Boolean,
    leadingIcon: ImageVector?,
) {
    if (loading) {
        CircularProgressIndicator(
            modifier = Modifier.size(size.iconSize),
            strokeWidth = 2.dp,
            color = LocalContentColor.current,
        )
        Spacer(Modifier.width(size.gap))
    } else {
        leadingIcon?.let {
            NimazIcon(imageVector = it, contentDescription = null, iconSize = size.iconSize)
            Spacer(Modifier.width(size.gap))
        }
    }
    Text(text = text, style = textStyleFor(size), maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun textStyleFor(size: NimazButtonSize): TextStyle = when (size) {
    NimazButtonSize.SMALL -> MaterialTheme.typography.labelLarge
    else -> MaterialTheme.typography.labelLarge
}

// ── Previews ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "Button Variants — Light")
@Composable
private fun NimazButtonVariantsLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NimazButton("Save", onClick = {}, variant = NimazButtonVariant.PRIMARY)
            NimazButton("Cancel", onClick = {}, variant = NimazButtonVariant.QUIET)
            NimazButton("Reset", onClick = {}, variant = NimazButtonVariant.DESTRUCTIVE)
            NimazButton("See all", onClick = {}, variant = NimazButtonVariant.TEXT)
        }
    }
}

@Preview(showBackground = true, name = "Button Variants — Dark")
@Composable
private fun NimazButtonVariantsDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NimazButton("Save", onClick = {}, variant = NimazButtonVariant.PRIMARY)
            NimazButton("Cancel", onClick = {}, variant = NimazButtonVariant.QUIET)
            NimazButton("Reset", onClick = {}, variant = NimazButtonVariant.DESTRUCTIVE)
            NimazButton("See all", onClick = {}, variant = NimazButtonVariant.TEXT)
        }
    }
}

@Preview(showBackground = true, name = "Button Sizes — Light")
@Composable
private fun NimazButtonSizesLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NimazButton("Standard 48dp", onClick = {}, size = NimazButtonSize.STANDARD)
            NimazButton("Small 38dp", onClick = {}, size = NimazButtonSize.SMALL)
        }
    }
}

@Preview(showBackground = true, name = "Button States — Light")
@Composable
private fun NimazButtonStatesLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NimazButton("With icon", onClick = {}, leadingIcon = Icons.Default.Add)
            NimazButton("Saving", onClick = {}, loading = true)
            NimazButton("Disabled", onClick = {}, enabled = false)
            NimazButton("Full width", onClick = {}, fullWidth = true)
            NimazButton("Delete account", onClick = {}, variant = NimazButtonVariant.DESTRUCTIVE, leadingIcon = Icons.Default.Delete, fullWidth = true)
            NimazButton("Mark read", onClick = {}, variant = NimazButtonVariant.PRIMARY, leadingIcon = Icons.Default.Check)
        }
    }
}
