package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * Icon size presets
 */
enum class NimazIconSize(val iconSize: Dp, val containerSize: Dp, val padding: Dp) {
    EXTRA_SMALL(12.dp, 24.dp, 6.dp),
    SMALL(16.dp, 32.dp, 8.dp),
    MEDIUM(20.dp, 40.dp, 10.dp),
    LARGE(24.dp, 48.dp, 12.dp),
    EXTRA_LARGE(32.dp, 64.dp, 16.dp)
}

/**
 * Icon container shape presets
 */
enum class NimazIconContainerShape {
    CIRCLE,
    ROUNDED_SQUARE,
    SQUARE
}

/**
 * Icon with background container.
 */
@Composable
fun ContainedIcon(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    size: NimazIconSize = NimazIconSize.MEDIUM,
    containerShape: NimazIconContainerShape = NimazIconContainerShape.CIRCLE,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    iconColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    val shape: Shape = when (containerShape) {
        NimazIconContainerShape.CIRCLE -> CircleShape
        NimazIconContainerShape.ROUNDED_SQUARE -> RoundedCornerShape(12.dp)
        NimazIconContainerShape.SQUARE -> RoundedCornerShape(0.dp)
    }

    Box(
        modifier = modifier
            .size(size.containerSize)
            .clip(shape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = Modifier.size(size.iconSize),
            tint = iconColor
        )
    }
}

/**
 * Prayer icon with prayer-specific color.
 */
@Composable
fun PrayerIcon(
    imageVector: ImageVector,
    prayerColor: Color,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    size: NimazIconSize = NimazIconSize.MEDIUM
) {
    ContainedIcon(
        imageVector = imageVector,
        modifier = modifier,
        contentDescription = contentDescription,
        size = size,
        backgroundColor = prayerColor.copy(alpha = 0.15f),
        iconColor = prayerColor
    )
}

@Preview(showBackground = true, name = "Contained Icons")
@Composable
private fun ContainedIconPreview() {
    NimazTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ContainedIcon(Icons.Default.Star, size = NimazIconSize.SMALL)
            ContainedIcon(Icons.Default.Star, size = NimazIconSize.MEDIUM)
            ContainedIcon(Icons.Default.Star, size = NimazIconSize.LARGE)
        }
    }
}

@Preview(showBackground = true, name = "Container Shapes")
@Composable
private fun ContainerShapesPreview() {
    NimazTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ContainedIcon(Icons.Default.Star, containerShape = NimazIconContainerShape.CIRCLE)
            ContainedIcon(Icons.Default.Star, containerShape = NimazIconContainerShape.ROUNDED_SQUARE)
            ContainedIcon(Icons.Default.Star, containerShape = NimazIconContainerShape.SQUARE)
        }
    }
}

@Preview(showBackground = true, name = "Prayer Icons")
@Composable
private fun PrayerIconPreview() {
    NimazTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PrayerIcon(Icons.Default.Star, prayerColor = Color(0xFF5C6BC0))
            PrayerIcon(Icons.Default.Star, prayerColor = Color(0xFFFFB74D))
            PrayerIcon(Icons.Default.Star, prayerColor = Color(0xFF81C784))
        }
    }
}

