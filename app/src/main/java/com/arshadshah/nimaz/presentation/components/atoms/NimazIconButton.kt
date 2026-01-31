package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * Icon button style variants
 */
enum class NimazIconButtonStyle {
    STANDARD,
    FILLED,
    FILLED_TONAL,
    OUTLINED
}

/**
 * Icon button size presets
 */
enum class NimazIconButtonSize(val containerSize: Dp, val iconSize: Dp) {
    SMALL(32.dp, 18.dp),
    MEDIUM(40.dp, 24.dp),
    LARGE(48.dp, 28.dp),
    EXTRA_LARGE(56.dp, 32.dp)
}

/**
 * Primary icon button component for Nimaz app with multiple styles.
 */
@Composable
fun NimazIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    style: NimazIconButtonStyle = NimazIconButtonStyle.STANDARD,
    size: NimazIconButtonSize = NimazIconButtonSize.MEDIUM,
    enabled: Boolean = true,
    colors: IconButtonColors? = null
) {
    val iconModifier = Modifier.size(size.iconSize)

    when (style) {
        NimazIconButtonStyle.STANDARD -> {
            IconButton(
                onClick = onClick,
                modifier = modifier.size(size.containerSize),
                enabled = enabled,
                colors = colors ?: IconButtonDefaults.iconButtonColors()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    modifier = iconModifier
                )
            }
        }
        NimazIconButtonStyle.FILLED -> {
            FilledIconButton(
                onClick = onClick,
                modifier = modifier.size(size.containerSize),
                enabled = enabled,
                colors = colors ?: IconButtonDefaults.filledIconButtonColors()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    modifier = iconModifier
                )
            }
        }
        NimazIconButtonStyle.FILLED_TONAL -> {
            FilledTonalIconButton(
                onClick = onClick,
                modifier = modifier.size(size.containerSize),
                enabled = enabled,
                colors = colors ?: IconButtonDefaults.filledTonalIconButtonColors()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    modifier = iconModifier
                )
            }
        }
        NimazIconButtonStyle.OUTLINED -> {
            OutlinedIconButton(
                onClick = onClick,
                modifier = modifier.size(size.containerSize),
                enabled = enabled,
                colors = colors ?: IconButtonDefaults.outlinedIconButtonColors()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    modifier = iconModifier
                )
            }
        }
    }
}


// ==================== PREVIEWS ====================

@Preview(showBackground = true, name = "Icon Button Styles")
@Composable
private fun IconButtonStylesPreview() {
    NimazTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NimazIconButton(Icons.Default.Star, onClick = {}, style = NimazIconButtonStyle.STANDARD)
            NimazIconButton(Icons.Default.Star, onClick = {}, style = NimazIconButtonStyle.FILLED)
            NimazIconButton(Icons.Default.Star, onClick = {}, style = NimazIconButtonStyle.FILLED_TONAL)
            NimazIconButton(Icons.Default.Star, onClick = {}, style = NimazIconButtonStyle.OUTLINED)
        }
    }
}

@Preview(showBackground = true, name = "Icon Button Sizes")
@Composable
private fun IconButtonSizesPreview() {
    NimazTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NimazIconButton(Icons.Default.Star, onClick = {}, size = NimazIconButtonSize.SMALL, style = NimazIconButtonStyle.FILLED)
            NimazIconButton(Icons.Default.Star, onClick = {}, size = NimazIconButtonSize.MEDIUM, style = NimazIconButtonStyle.FILLED)
            NimazIconButton(Icons.Default.Star, onClick = {}, size = NimazIconButtonSize.LARGE, style = NimazIconButtonStyle.FILLED)
            NimazIconButton(Icons.Default.Star, onClick = {}, size = NimazIconButtonSize.EXTRA_LARGE, style = NimazIconButtonStyle.FILLED)
        }
    }
}

