package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * Divider thickness presets
 */
enum class NimazDividerThickness(val thickness: Dp) {
    THIN(0.5.dp),
    REGULAR(1.dp),
    THICK(2.dp)
}

/**
 * Primary horizontal divider component.
 */
@Composable
fun NimazHorizontalDivider(
    modifier: Modifier = Modifier,
    thickness: NimazDividerThickness = NimazDividerThickness.REGULAR,
    color: Color = MaterialTheme.colorScheme.outlineVariant,
    startIndent: Dp = 0.dp
) {
    HorizontalDivider(
        modifier = modifier.padding(start = startIndent),
        thickness = thickness.thickness,
        color = color
    )
}

/**
 * Primary vertical divider component.
 */
@Composable
fun NimazVerticalDivider(
    modifier: Modifier = Modifier,
    thickness: NimazDividerThickness = NimazDividerThickness.REGULAR,
    color: Color = MaterialTheme.colorScheme.outlineVariant
) {
    VerticalDivider(
        modifier = modifier,
        thickness = thickness.thickness,
        color = color
    )
}

/**
 * Divider with text label in the middle.
 */
@Composable
fun LabeledDivider(
    label: String,
    modifier: Modifier = Modifier,
    thickness: NimazDividerThickness = NimazDividerThickness.THIN,
    color: Color = MaterialTheme.colorScheme.outlineVariant,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = thickness.thickness,
            color = color
        )
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.labelMedium,
            color = textColor
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = thickness.thickness,
            color = color
        )
    }
}

/**
 * Spacer divider - adds visual space between sections.
 */
@Composable
fun SpacerDivider(
    height: Dp = 8.dp,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(color)
    )
}

/**
 * Section divider with optional title.
 */
@Composable
fun SectionDivider(
    modifier: Modifier = Modifier,
    title: String? = null,
    titleColor: Color = MaterialTheme.colorScheme.primary,
    showLine: Boolean = true
) {
    if (title != null) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = titleColor,
                letterSpacing = 1.5.sp
            )
            if (showLine) {
                Spacer(modifier = Modifier.width(12.dp))
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = titleColor.copy(alpha = 0.3f)
                )
            }
        }
    } else {
        NimazHorizontalDivider(modifier = modifier)
    }
}

/**
 * Inset divider for list items (indented from left).
 */
@Composable
fun InsetDivider(
    modifier: Modifier = Modifier,
    startInset: Dp = 16.dp,
    endInset: Dp = 0.dp,
    color: Color = MaterialTheme.colorScheme.outlineVariant
) {
    HorizontalDivider(
        modifier = modifier.padding(start = startInset, end = endInset),
        thickness = 0.5.dp,
        color = color
    )
}

/**
 * Gradient divider for decorative purposes.
 */
@Composable
fun GradientDivider(
    modifier: Modifier = Modifier,
    height: Dp = 2.dp,
    colors: List<Color> = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary
    )
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(
                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(colors)
            )
    )
}

/**
 * Dotted divider line.
 */
@Composable
fun DottedDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outlineVariant,
    dotSize: Dp = 4.dp,
    spacing: Dp = 4.dp
) {
    // Simplified implementation using a thin dashed line
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        repeat(30) {
            Box(
                modifier = Modifier
                    .width(dotSize)
                    .height(1.dp)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(spacing))
        }
    }
}

// ==================== PREVIEWS ====================

@Preview(showBackground = true, name = "Horizontal Dividers")
@Composable
private fun NimazHorizontalDividerPreview() {
    NimazTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Thin", style = MaterialTheme.typography.labelSmall)
            NimazHorizontalDivider(thickness = NimazDividerThickness.THIN)
            Text("Regular", style = MaterialTheme.typography.labelSmall)
            NimazHorizontalDivider(thickness = NimazDividerThickness.REGULAR)
            Text("Thick", style = MaterialTheme.typography.labelSmall)
            NimazHorizontalDivider(thickness = NimazDividerThickness.THICK)
        }
    }
}

@Preview(showBackground = true, name = "Vertical Divider")
@Composable
private fun NimazVerticalDividerPreview() {
    NimazTheme {
        Row(
            modifier = Modifier
                .height(50.dp)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Left")
            NimazVerticalDivider()
            Text("Right")
        }
    }
}

@Preview(showBackground = true, name = "Labeled Divider")
@Composable
private fun LabeledDividerPreview() {
    NimazTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            LabeledDivider(label = "OR")
        }
    }
}

@Preview(showBackground = true, name = "Spacer Divider")
@Composable
private fun SpacerDividerPreview() {
    NimazTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Above")
            SpacerDivider()
            Text("Below")
        }
    }
}

@Preview(showBackground = true, name = "Section Divider")
@Composable
private fun SectionDividerPreview() {
    NimazTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionDivider(title = "Prayer Settings")
            Text("Content here...")
            SectionDivider(title = "Notifications")
            Text("More content...")
        }
    }
}

@Preview(showBackground = true, name = "Inset Divider")
@Composable
private fun InsetDividerPreview() {
    NimazTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("List Item 1", modifier = Modifier.padding(vertical = 8.dp))
            InsetDivider(startInset = 56.dp)
            Text("List Item 2", modifier = Modifier.padding(vertical = 8.dp))
            InsetDivider(startInset = 56.dp)
            Text("List Item 3", modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

@Preview(showBackground = true, name = "Gradient Divider")
@Composable
private fun GradientDividerPreview() {
    NimazTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Above")
            Spacer(modifier = Modifier.height(8.dp))
            GradientDivider()
            Spacer(modifier = Modifier.height(8.dp))
            Text("Below")
        }
    }
}

@Preview(showBackground = true, name = "Dotted Divider")
@Composable
private fun DottedDividerPreview() {
    NimazTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Above")
            Spacer(modifier = Modifier.height(8.dp))
            DottedDivider()
            Spacer(modifier = Modifier.height(8.dp))
            Text("Below")
        }
    }
}
