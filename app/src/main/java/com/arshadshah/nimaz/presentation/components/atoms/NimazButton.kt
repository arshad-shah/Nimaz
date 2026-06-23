package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
 * Visual treatment of a [NimazButton]. Colours map onto the app's Material 3
 * scheme so they track theming automatically.
 */
enum class NimazButtonVariant {
    /** High-emphasis primary call to action. `primary` / `onPrimary`. */
    FILLED,

    /** Medium-emphasis. `primaryContainer` / `onPrimaryContainer`. */
    TONAL,

    /** Low-emphasis with a 1.dp `outline` border, transparent container. */
    OUTLINED,

    /** Lowest-emphasis, label only (links, dialog "cancel"). */
    TEXT,

    /** Destructive action (delete, reset). `error` / `onError`. */
    DESTRUCTIVE
}

/**
 * Size preset for a [NimazButton]: container height, horizontal content padding,
 * icon size, icon/label gap, and the corner radius used by [NimazButtonType.STANDARD].
 */
enum class NimazButtonSize(
    val height: Dp,
    val horizontalPadding: Dp,
    val iconSize: Dp,
    val gap: Dp,
    val corner: Dp
) {
    SMALL(36.dp, 12.dp, 16.dp, 6.dp, 10.dp),
    MEDIUM(48.dp, 20.dp, 18.dp, 8.dp, 12.dp),
    LARGE(56.dp, 24.dp, 20.dp, 8.dp, 16.dp)
}

/**
 * Shape character of a [NimazButton]: a rounded rectangle ([STANDARD], radius from
 * the [NimazButtonSize]) or a fully-rounded [PILL].
 */
enum class NimazButtonType {
    STANDARD,
    PILL
}

/**
 * Unified text / icon+text button for the Nimaz app.
 *
 * Replaces the dozens of one-off Material 3 `Button`/`OutlinedButton`/`TextButton`
 * call sites (each re-specifying shape, colours and padding) with a single,
 * variant/size/type-driven entry point. Icon-only buttons stay with
 * [NimazIconButton]; this component always renders a [text] label.
 *
 * Built on the matching Material 3 button per [variant] so ripple, elevation and
 * accessibility behave exactly as the platform expects — only shape, colours,
 * content padding and the content row are customised.
 *
 * @param leadingIcon optional icon shown before the label.
 * @param trailingIcon optional icon shown after the label.
 * @param loading when true, swaps the content for a spinner and blocks clicks.
 * @param fullWidth when true, stretches to the available width.
 */
@Composable
fun NimazButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: NimazButtonVariant = NimazButtonVariant.FILLED,
    size: NimazButtonSize = NimazButtonSize.MEDIUM,
    type: NimazButtonType = NimazButtonType.STANDARD,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    fullWidth: Boolean = false
) {
    val shape = when (type) {
        NimazButtonType.STANDARD -> RoundedCornerShape(size.corner)
        NimazButtonType.PILL -> RoundedCornerShape(percent = 50)
    }
    val contentPadding = PaddingValues(horizontal = size.horizontalPadding, vertical = 0.dp)
    val sizedModifier = modifier
        .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
        .height(size.height)

    // Loading blocks interaction but keeps the button looking active is preferable;
    // we simply disable to get the platform's clear, accessible disabled treatment.
    val isEnabled = enabled && !loading

    val content: @Composable RowScope.() -> Unit = {
        ButtonContent(
            text = text,
            size = size,
            loading = loading,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon
        )
    }

    when (variant) {
        NimazButtonVariant.FILLED -> Button(
            onClick = onClick,
            modifier = sizedModifier,
            enabled = isEnabled,
            shape = shape,
            colors = ButtonDefaults.buttonColors(),
            contentPadding = contentPadding,
            content = content
        )

        NimazButtonVariant.TONAL -> FilledTonalButton(
            onClick = onClick,
            modifier = sizedModifier,
            enabled = isEnabled,
            shape = shape,
            colors = ButtonDefaults.filledTonalButtonColors(),
            contentPadding = contentPadding,
            content = content
        )

        NimazButtonVariant.OUTLINED -> OutlinedButton(
            onClick = onClick,
            modifier = sizedModifier,
            enabled = isEnabled,
            shape = shape,
            colors = ButtonDefaults.outlinedButtonColors(),
            border = outlinedBorder(isEnabled),
            contentPadding = contentPadding,
            content = content
        )

        NimazButtonVariant.TEXT -> TextButton(
            onClick = onClick,
            modifier = sizedModifier,
            enabled = isEnabled,
            shape = shape,
            colors = ButtonDefaults.textButtonColors(),
            contentPadding = contentPadding,
            content = content
        )

        NimazButtonVariant.DESTRUCTIVE -> Button(
            onClick = onClick,
            modifier = sizedModifier,
            enabled = isEnabled,
            shape = shape,
            colors = destructiveColors(),
            contentPadding = contentPadding,
            content = content
        )
    }
}

@Composable
private fun destructiveColors(): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.error,
    contentColor = MaterialTheme.colorScheme.onError
)

@Composable
private fun outlinedBorder(enabled: Boolean): BorderStroke {
    val color = MaterialTheme.colorScheme.outline
    return BorderStroke(1.dp, if (enabled) color else color.copy(alpha = 0.12f))
}

@Composable
private fun textStyleFor(size: NimazButtonSize): TextStyle = when (size) {
    NimazButtonSize.SMALL -> MaterialTheme.typography.labelMedium
    NimazButtonSize.MEDIUM -> MaterialTheme.typography.labelLarge
    NimazButtonSize.LARGE -> MaterialTheme.typography.titleSmall
}

@Composable
private fun RowScope.ButtonContent(
    text: String,
    size: NimazButtonSize,
    loading: Boolean,
    leadingIcon: ImageVector?,
    trailingIcon: ImageVector?
) {
    if (loading) {
        CircularProgressIndicator(
            modifier = Modifier.size(size.iconSize),
            strokeWidth = 2.dp,
            color = LocalContentColor.current
        )
        return
    }

    leadingIcon?.let {
        Icon(
            imageVector = it,
            contentDescription = null,
            modifier = Modifier.size(size.iconSize)
        )
        Spacer(Modifier.width(size.gap))
    }
    Text(
        text = text,
        style = textStyleFor(size),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
    trailingIcon?.let {
        Spacer(Modifier.width(size.gap))
        Icon(
            imageVector = it,
            contentDescription = null,
            modifier = Modifier.size(size.iconSize)
        )
    }
}


// ==================== PREVIEWS ====================

@Composable
private fun NimazButtonVariantsShowcase() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NimazButton(text = "Filled", onClick = {}, variant = NimazButtonVariant.FILLED)
        NimazButton(text = "Tonal", onClick = {}, variant = NimazButtonVariant.TONAL)
        NimazButton(text = "Outlined", onClick = {}, variant = NimazButtonVariant.OUTLINED)
        NimazButton(text = "Text", onClick = {}, variant = NimazButtonVariant.TEXT)
        NimazButton(text = "Delete", onClick = {}, variant = NimazButtonVariant.DESTRUCTIVE)
    }
}

@Composable
private fun NimazButtonSizesShowcase() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NimazButton(text = "Small", onClick = {}, size = NimazButtonSize.SMALL)
        NimazButton(text = "Medium", onClick = {}, size = NimazButtonSize.MEDIUM)
        NimazButton(text = "Large", onClick = {}, size = NimazButtonSize.LARGE)
    }
}

@Composable
private fun NimazButtonTypesShowcase() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NimazButton(text = "Standard", onClick = {}, type = NimazButtonType.STANDARD)
        NimazButton(text = "Pill", onClick = {}, type = NimazButtonType.PILL)
        NimazButton(
            text = "Pill Tonal",
            onClick = {},
            variant = NimazButtonVariant.TONAL,
            type = NimazButtonType.PILL
        )
    }
}

@Composable
private fun NimazButtonContentStatesShowcase() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NimazButton(text = "Leading icon", onClick = {}, leadingIcon = Icons.Default.Add)
        NimazButton(text = "Trailing icon", onClick = {}, trailingIcon = Icons.Default.ChevronRight)
        NimazButton(
            text = "Both",
            onClick = {},
            leadingIcon = Icons.Default.Check,
            trailingIcon = Icons.Default.ChevronRight
        )
        NimazButton(text = "Loading", onClick = {}, loading = true)
        NimazButton(text = "Disabled", onClick = {}, enabled = false)
        NimazButton(text = "Full width", onClick = {}, fullWidth = true)
        NimazButton(
            text = "Delete account",
            onClick = {},
            variant = NimazButtonVariant.DESTRUCTIVE,
            leadingIcon = Icons.Default.Delete,
            fullWidth = true
        )
    }
}

@Preview(showBackground = true, name = "Button Variants — Light")
@Composable
private fun NimazButtonVariantsLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { NimazButtonVariantsShowcase() }
}

@Preview(showBackground = true, name = "Button Variants — Dark")
@Composable
private fun NimazButtonVariantsDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { NimazButtonVariantsShowcase() }
}

@Preview(showBackground = true, name = "Button Sizes — Light")
@Composable
private fun NimazButtonSizesLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { NimazButtonSizesShowcase() }
}

@Preview(showBackground = true, name = "Button Sizes — Dark")
@Composable
private fun NimazButtonSizesDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { NimazButtonSizesShowcase() }
}

@Preview(showBackground = true, name = "Button Types — Light")
@Composable
private fun NimazButtonTypesLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { NimazButtonTypesShowcase() }
}

@Preview(showBackground = true, name = "Button Types — Dark")
@Composable
private fun NimazButtonTypesDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { NimazButtonTypesShowcase() }
}

@Preview(showBackground = true, name = "Button States — Light")
@Composable
private fun NimazButtonStatesLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { NimazButtonContentStatesShowcase() }
}

@Preview(showBackground = true, name = "Button States — Dark")
@Composable
private fun NimazButtonStatesDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { NimazButtonContentStatesShowcase() }
}
