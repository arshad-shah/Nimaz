package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Text

/**
 * Card style variants
 */
enum class NimazCardStyle {
    FILLED,
    ELEVATED,
    OUTLINED,
    GRADIENT
}

/**
 * Primary card component for Nimaz app with multiple styles.
 */
@Composable
fun NimazCard(
    modifier: Modifier = Modifier,
    style: NimazCardStyle = NimazCardStyle.FILLED,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(16.dp),
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    when (style) {
        NimazCardStyle.FILLED -> {
            if (onClick != null) {
                Card(
                    onClick = onClick,
                    modifier = modifier,
                    shape = shape,
                    colors = colors,
                    elevation = elevation,
                    content = content
                )
            } else {
                Card(
                    modifier = modifier,
                    shape = shape,
                    colors = colors,
                    elevation = elevation,
                    content = content
                )
            }
        }
        NimazCardStyle.ELEVATED -> {
            if (onClick != null) {
                ElevatedCard(
                    onClick = onClick,
                    modifier = modifier,
                    shape = shape,
                    colors = colors,
                    elevation = elevation,
                    content = content
                )
            } else {
                ElevatedCard(
                    modifier = modifier,
                    shape = shape,
                    colors = colors,
                    elevation = elevation,
                    content = content
                )
            }
        }
        NimazCardStyle.OUTLINED -> {
            if (onClick != null) {
                OutlinedCard(
                    onClick = onClick,
                    modifier = modifier,
                    shape = shape,
                    colors = colors,
                    border = border ?: BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    content = content
                )
            } else {
                OutlinedCard(
                    modifier = modifier,
                    shape = shape,
                    colors = colors,
                    border = border ?: BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    content = content
                )
            }
        }
        NimazCardStyle.GRADIENT -> {
            // Use FILLED as base, gradient should be applied in content
            if (onClick != null) {
                Card(
                    onClick = onClick,
                    modifier = modifier,
                    shape = shape,
                    colors = colors,
                    elevation = elevation,
                    content = content
                )
            } else {
                Card(
                    modifier = modifier,
                    shape = shape,
                    colors = colors,
                    elevation = elevation,
                    content = content
                )
            }
        }
    }
}

/**
 * Gradient card with customizable gradient colors.
 */
@Composable
fun GradientCard(
    modifier: Modifier = Modifier,
    gradientColors: List<Color>,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(Brush.linearGradient(gradientColors))
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
    ) {
        Column(content = content)
    }
}

/**
 * Prayer-themed card with appropriate gradient colors.
 */
@Composable
fun PrayerCard(
    modifier: Modifier = Modifier,
    primaryColor: Color,
    secondaryColor: Color,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    GradientCard(
        modifier = modifier,
        gradientColors = listOf(primaryColor, secondaryColor),
        onClick = onClick,
        shape = shape,
        content = content
    )
}

/**
 * Section card with title and content area.
 */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    style: NimazCardStyle = NimazCardStyle.ELEVATED,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    NimazCard(
        modifier = modifier.fillMaxWidth(),
        style = style
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

/**
 * Clickable list item card.
 */
@Composable
fun ListItemCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    NimazCard(
        modifier = modifier.fillMaxWidth(),
        style = NimazCardStyle.FILLED,
        onClick = onClick,
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        content = content
    )
}

// ==================== PREVIEWS ====================

@Preview(showBackground = true, name = "Filled Card")
@Composable
private fun NimazCardFilledPreview() {
    MaterialTheme {
        NimazCard(
            modifier = Modifier.padding(16.dp),
            style = NimazCardStyle.FILLED
        ) {
            Text(
                text = "Filled Card Content",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true, name = "Elevated Card")
@Composable
private fun NimazCardElevatedPreview() {
    MaterialTheme {
        NimazCard(
            modifier = Modifier.padding(16.dp),
            style = NimazCardStyle.ELEVATED
        ) {
            Text(
                text = "Elevated Card Content",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true, name = "Outlined Card")
@Composable
private fun NimazCardOutlinedPreview() {
    MaterialTheme {
        NimazCard(
            modifier = Modifier.padding(16.dp),
            style = NimazCardStyle.OUTLINED
        ) {
            Text(
                text = "Outlined Card Content",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true, name = "Gradient Card")
@Composable
private fun GradientCardPreview() {
    MaterialTheme {
        GradientCard(
            modifier = Modifier.padding(16.dp),
            gradientColors = listOf(Color(0xFF5C6BC0), Color(0xFF9FA8DA))
        ) {
            Text(
                text = "Gradient Card",
                color = Color.White,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true, name = "Prayer Card")
@Composable
private fun PrayerCardPreview() {
    MaterialTheme {
        PrayerCard(
            modifier = Modifier.padding(16.dp),
            primaryColor = Color(0xFFFFB74D),
            secondaryColor = Color(0xFFFFE0B2)
        ) {
            Text(
                text = "Fajr Prayer",
                color = Color.Black,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true, name = "Section Card")
@Composable
private fun SectionCardPreview() {
    MaterialTheme {
        SectionCard(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = "Section Title")
            Text(text = "Section content goes here")
        }
    }
}

@Preview(showBackground = true, name = "List Item Card")
@Composable
private fun ListItemCardPreview() {
    MaterialTheme {
        ListItemCard(
            onClick = {},
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Clickable List Item",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
