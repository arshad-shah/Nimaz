package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ArrowForward

/**
 * Button variant types for NimazButton
 */
enum class NimazButtonVariant {
    FILLED,
    TONAL,
    OUTLINED,
    ELEVATED,
    TEXT
}

/**
 * Button size presets
 */
enum class NimazButtonSize(val height: Dp, val horizontalPadding: Dp, val textStyle: @Composable () -> TextStyle) {
    SMALL(36.dp, 12.dp, { MaterialTheme.typography.labelMedium }),
    MEDIUM(44.dp, 16.dp, { MaterialTheme.typography.labelLarge }),
    LARGE(52.dp, 24.dp, { MaterialTheme.typography.titleSmall })
}

/**
 * Primary button component for Nimaz app with multiple variants.
 */
@Composable
fun NimazButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: NimazButtonVariant = NimazButtonVariant.FILLED,
    size: NimazButtonSize = NimazButtonSize.MEDIUM,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    shape: Shape = RoundedCornerShape(12.dp),
    colors: ButtonColors? = null
) {
    val contentPadding = PaddingValues(horizontal = size.horizontalPadding, vertical = 0.dp)

    val content: @Composable RowScope.() -> Unit = {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            leadingIcon?.let { icon ->
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = size.textStyle()
            )
            trailingIcon?.let { icon ->
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    val buttonModifier = modifier.height(size.height)

    when (variant) {
        NimazButtonVariant.FILLED -> {
            Button(
                onClick = { if (!loading) onClick() },
                modifier = buttonModifier,
                enabled = enabled,
                shape = shape,
                colors = colors ?: ButtonDefaults.buttonColors(),
                contentPadding = contentPadding,
                content = content
            )
        }
        NimazButtonVariant.TONAL -> {
            FilledTonalButton(
                onClick = { if (!loading) onClick() },
                modifier = buttonModifier,
                enabled = enabled,
                shape = shape,
                colors = colors ?: ButtonDefaults.filledTonalButtonColors(),
                contentPadding = contentPadding,
                content = content
            )
        }
        NimazButtonVariant.OUTLINED -> {
            OutlinedButton(
                onClick = { if (!loading) onClick() },
                modifier = buttonModifier,
                enabled = enabled,
                shape = shape,
                colors = colors ?: ButtonDefaults.outlinedButtonColors(),
                contentPadding = contentPadding,
                content = content
            )
        }
        NimazButtonVariant.ELEVATED -> {
            ElevatedButton(
                onClick = { if (!loading) onClick() },
                modifier = buttonModifier,
                enabled = enabled,
                shape = shape,
                colors = colors ?: ButtonDefaults.elevatedButtonColors(),
                contentPadding = contentPadding,
                content = content
            )
        }
        NimazButtonVariant.TEXT -> {
            TextButton(
                onClick = { if (!loading) onClick() },
                modifier = buttonModifier,
                enabled = enabled,
                shape = shape,
                colors = colors ?: ButtonDefaults.textButtonColors(),
                contentPadding = contentPadding,
                content = content
            )
        }
    }
}

/**
 * Convenience composable for prayer-themed button.
 */
@Composable
fun PrayerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    prayerColor: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    NimazButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        variant = NimazButtonVariant.FILLED,
        enabled = enabled,
        loading = loading,
        colors = ButtonDefaults.buttonColors(
            containerColor = prayerColor,
            contentColor = Color.White
        )
    )
}

// ==================== PREVIEWS ====================

@Preview(showBackground = true, name = "Filled Button")
@Composable
private fun NimazButtonFilledPreview() {
    MaterialTheme {
        NimazButton(
            text = "Filled Button",
            onClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Tonal Button")
@Composable
private fun NimazButtonTonalPreview() {
    MaterialTheme {
        NimazButton(
            text = "Tonal Button",
            onClick = {},
            variant = NimazButtonVariant.TONAL
        )
    }
}

@Preview(showBackground = true, name = "Outlined Button")
@Composable
private fun NimazButtonOutlinedPreview() {
    MaterialTheme {
        NimazButton(
            text = "Outlined Button",
            onClick = {},
            variant = NimazButtonVariant.OUTLINED
        )
    }
}

@Preview(showBackground = true, name = "Elevated Button")
@Composable
private fun NimazButtonElevatedPreview() {
    MaterialTheme {
        NimazButton(
            text = "Elevated Button",
            onClick = {},
            variant = NimazButtonVariant.ELEVATED
        )
    }
}

@Preview(showBackground = true, name = "Text Button")
@Composable
private fun NimazButtonTextPreview() {
    MaterialTheme {
        NimazButton(
            text = "Text Button",
            onClick = {},
            variant = NimazButtonVariant.TEXT
        )
    }
}

@Preview(showBackground = true, name = "Button with Icons")
@Composable
private fun NimazButtonWithIconsPreview() {
    MaterialTheme {
        NimazButton(
            text = "Continue",
            onClick = {},
            leadingIcon = Icons.Default.Favorite,
            trailingIcon = Icons.Default.ArrowForward
        )
    }
}

@Preview(showBackground = true, name = "Loading Button")
@Composable
private fun NimazButtonLoadingPreview() {
    MaterialTheme {
        NimazButton(
            text = "Loading",
            onClick = {},
            loading = true
        )
    }
}

@Preview(showBackground = true, name = "Button Sizes")
@Composable
private fun NimazButtonSizesPreview() {
    MaterialTheme {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NimazButton(text = "Small", onClick = {}, size = NimazButtonSize.SMALL)
            NimazButton(text = "Medium", onClick = {}, size = NimazButtonSize.MEDIUM)
            NimazButton(text = "Large", onClick = {}, size = NimazButtonSize.LARGE)
        }
    }
}

@Preview(showBackground = true, name = "Prayer Button")
@Composable
private fun PrayerButtonPreview() {
    MaterialTheme {
        PrayerButton(
            text = "Pray Fajr",
            onClick = {},
            prayerColor = Color(0xFF5C6BC0)
        )
    }
}
