package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Check
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
 * Simple icon component.
 */
@Composable
fun NimazIcon(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = tint
    )
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
 * Feature icon with tinted background (for feature showcases).
 */
@Composable
fun FeatureIcon(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    featureColor: Color = MaterialTheme.colorScheme.primary,
    size: NimazIconSize = NimazIconSize.LARGE
) {
    ContainedIcon(
        imageVector = imageVector,
        modifier = modifier,
        contentDescription = contentDescription,
        size = size,
        containerShape = NimazIconContainerShape.ROUNDED_SQUARE,
        backgroundColor = featureColor.copy(alpha = 0.15f),
        iconColor = featureColor
    )
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

/**
 * Status icon with background.
 */
@Composable
fun StatusIcon(
    imageVector: ImageVector,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.outline,
    size: NimazIconSize = NimazIconSize.MEDIUM
) {
    val color = if (isActive) activeColor else inactiveColor

    ContainedIcon(
        imageVector = imageVector,
        modifier = modifier,
        contentDescription = contentDescription,
        size = size,
        backgroundColor = color.copy(alpha = 0.15f),
        iconColor = color
    )
}

/**
 * Outlined icon (just border, no fill).
 */
@Composable
fun OutlinedIcon(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    size: NimazIconSize = NimazIconSize.MEDIUM,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    iconColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Box(
        modifier = modifier
            .size(size.containerSize)
            .clip(CircleShape)
            .background(Color.Transparent)
            .then(
                Modifier.padding(1.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
                .background(Color.Transparent)
        )
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = Modifier.size(size.iconSize),
            tint = iconColor
        )
    }
}

/**
 * Action icon for quick actions (floating action style).
 */
@Composable
fun ActionIcon(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    iconColor: Color = MaterialTheme.colorScheme.onPrimary,
    size: NimazIconSize = NimazIconSize.LARGE
) {
    Box(
        modifier = modifier
            .size(size.containerSize)
            .clip(CircleShape)
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
 * Gradient icon background.
 */
@Composable
fun GradientIcon(
    imageVector: ImageVector,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    iconColor: Color = Color.White,
    size: NimazIconSize = NimazIconSize.LARGE
) {
    Box(
        modifier = modifier
            .size(size.containerSize)
            .clip(CircleShape)
            .background(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(gradientColors)
            ),
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

@Preview(showBackground = true, name = "NimazIcon Sizes")
@Composable
private fun NimazIconSizesPreview() {
    NimazTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            NimazIcon(Icons.Default.Star, size = 16.dp)
            NimazIcon(Icons.Default.Star, size = 24.dp)
            NimazIcon(Icons.Default.Star, size = 32.dp)
        }
    }
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

@Preview(showBackground = true, name = "Feature Icons")
@Composable
private fun FeatureIconPreview() {
    NimazTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FeatureIcon(Icons.Default.Star, featureColor = Color(0xFF5C6BC0))
            FeatureIcon(Icons.Default.Favorite, featureColor = Color(0xFFE91E63))
            FeatureIcon(Icons.Default.Notifications, featureColor = Color(0xFFFF9800))
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

@Preview(showBackground = true, name = "Status Icons")
@Composable
private fun StatusIconPreview() {
    NimazTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatusIcon(Icons.Default.Check, isActive = true)
            StatusIcon(Icons.Default.Check, isActive = false)
        }
    }
}

@Preview(showBackground = true, name = "Action Icon")
@Composable
private fun ActionIconPreview() {
    NimazTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ActionIcon(Icons.Default.Star)
            ActionIcon(Icons.Default.Favorite, backgroundColor = Color(0xFFE91E63))
        }
    }
}

@Preview(showBackground = true, name = "Gradient Icon")
@Composable
private fun GradientIconPreview() {
    NimazTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GradientIcon(
                Icons.Default.Star,
                gradientColors = listOf(Color(0xFF5C6BC0), Color(0xFF9FA8DA))
            )
            GradientIcon(
                Icons.Default.Favorite,
                gradientColors = listOf(Color(0xFFE91E63), Color(0xFFF48FB1))
            )
        }
    }
}
