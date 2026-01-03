package com.arshadshah.nimaz.ui.components.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.ui.theme.NimazTheme

@Composable
fun NumberSelector(
    value: Float,
    onValueChange: (Float) -> Unit,
    minValue: Float,
    maxValue: Float,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    // Track previous value for animation direction
    var previousValue by remember { mutableFloatStateOf(value) }
    val isIncreasing = value > previousValue

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Decrease button
            Surface(
                onClick = {
                    val newValue = value - 1
                    if (newValue >= minValue) {
                        previousValue = value
                        onValueChange(newValue)
                    }
                },
                enabled = enabled && value > minValue,
                shape = RoundedCornerShape(10.dp),
                color = if (enabled && value > minValue)
                    MaterialTheme.colorScheme.secondaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Remove,
                        contentDescription = "Decrease",
                        tint = if (enabled && value > minValue)
                            MaterialTheme.colorScheme.onSecondaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Value display window with animation
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 8.dp,
                tonalElevation = 4.dp,
                border = BorderStroke(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    .widthIn(min = 80.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    AnimatedContent(
                        targetState = value.toInt(),
                        transitionSpec = {
                            if (isIncreasing) {
                                // Slide up when increasing
                                (slideInVertically(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                ) { height -> height } + fadeIn())
                                    .togetherWith(
                                        slideOutVertically(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            )
                                        ) { height -> -height } + fadeOut()
                                    )
                            } else {
                                // Slide down when decreasing
                                (slideInVertically(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                ) { height -> -height } + fadeIn())
                                    .togetherWith(
                                        slideOutVertically(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            )
                                        ) { height -> height } + fadeOut()
                                    )
                            }
                        },
                        label = "numberAnimation"
                    ) { targetValue ->
                        Text(
                            text = targetValue.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Increase button
            Surface(
                onClick = {
                    val newValue = value + 1
                    if (newValue <= maxValue) {
                        previousValue = value
                        onValueChange(newValue)
                    }
                },
                enabled = enabled && value < maxValue,
                shape = RoundedCornerShape(10.dp),
                color = if (enabled && value < maxValue)
                    MaterialTheme.colorScheme.secondaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Increase",
                        tint = if (enabled && value < maxValue)
                            MaterialTheme.colorScheme.onSecondaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NumberSelectorPreview() {
    var value by remember { mutableFloatStateOf(5f) }
    NimazTheme {
        NumberSelector(
            value = value,
            onValueChange = { value = it },
            minValue = 0f,
            maxValue = 100f,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, name = "At Minimum")
@Composable
fun NumberSelectorAtMinPreview() {
    NimazTheme {
        NumberSelector(
            value = 0f,
            onValueChange = {},
            minValue = 0f,
            maxValue = 100f,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, name = "At Maximum")
@Composable
fun NumberSelectorAtMaxPreview() {
    NimazTheme {
        NumberSelector(
            value = 100f,
            onValueChange = {},
            minValue = 0f,
            maxValue = 100f,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, name = "Disabled")
@Composable
fun NumberSelectorDisabledPreview() {
    NimazTheme {
        NumberSelector(
            value = 50f,
            onValueChange = {},
            minValue = 0f,
            maxValue = 100f,
            enabled = false,
            modifier = Modifier.padding(16.dp)
        )
    }
}