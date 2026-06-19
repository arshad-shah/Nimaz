package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A small row of stars showing [filled] out of [max] earned, used under lesson
 * medallions and in the celebration. Gold (the theme `secondary`) for earned,
 * muted outline for the rest.
 */
@Composable
fun QaidaStarRow(
    filled: Int,
    modifier: Modifier = Modifier,
    max: Int = 3,
    starSize: Dp = 14.dp,
    filledColor: Color = MaterialTheme.colorScheme.secondary,
    emptyColor: Color = MaterialTheme.colorScheme.outline,
) {
    Row(
        modifier = modifier.clearAndSetSemantics {
            contentDescription = "$filled of $max stars"
        },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        repeat(max) { i ->
            val isFilled = i < filled
            Icon(
                imageVector = if (isFilled) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = null,
                tint = if (isFilled) filledColor else emptyColor,
                modifier = Modifier
                    .size(starSize)
                    .testTag("qaida_star"),
            )
        }
    }
}
