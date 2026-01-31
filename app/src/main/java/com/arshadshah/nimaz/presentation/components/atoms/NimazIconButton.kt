package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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

/**
 * Custom colored icon button with background.
 */
@Composable
fun ColoredIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    iconColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    size: NimazIconButtonSize = NimazIconButtonSize.MEDIUM,
    shape: Shape = CircleShape,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .size(size.containerSize)
            .clip(shape)
            .background(if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.5f))
            .clickable(
                enabled = enabled,
                onClick = onClick,
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(size.iconSize),
            tint = if (enabled) iconColor else iconColor.copy(alpha = 0.5f)
        )
    }
}

/**
 * Toggle icon button that changes appearance based on selected state.
 */
@Composable
fun ToggleIconButton(
    icon: ImageVector,
    selectedIcon: ImageVector = icon,
    selected: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    size: NimazIconButtonSize = NimazIconButtonSize.MEDIUM,
    selectedColor: Color = MaterialTheme.colorScheme.primary,
    unselectedColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    enabled: Boolean = true
) {
    IconButton(
        onClick = { onToggle(!selected) },
        modifier = modifier.size(size.containerSize),
        enabled = enabled
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(size.iconSize),
            tint = if (selected) selectedColor else unselectedColor
        )
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

@Preview(showBackground = true, name = "Colored Icon Buttons")
@Composable
private fun ColoredIconButtonPreview() {
    NimazTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ColoredIconButton(Icons.Default.Share, onClick = {})
            ColoredIconButton(
                Icons.Default.Favorite,
                onClick = {},
                backgroundColor = Color(0xFFE91E63).copy(alpha = 0.15f),
                iconColor = Color(0xFFE91E63)
            )
        }
    }
}

@Preview(showBackground = true, name = "Toggle Icon Button")
@Composable
private fun ToggleIconButtonPreview() {
    NimazTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            var selected1 by remember { mutableStateOf(false) }
            var selected2 by remember { mutableStateOf(true) }

            ToggleIconButton(
                icon = Icons.Default.FavoriteBorder,
                selectedIcon = Icons.Default.Favorite,
                selected = selected1,
                onToggle = { selected1 = it }
            )
            ToggleIconButton(
                icon = Icons.Default.FavoriteBorder,
                selectedIcon = Icons.Default.Favorite,
                selected = selected2,
                onToggle = { selected2 = it },
                selectedColor = Color(0xFFE91E63)
            )
        }
    }
}
