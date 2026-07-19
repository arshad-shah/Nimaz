package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

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
    val starsLabel = pluralStringResource(R.plurals.qaida_a11y_stars_format, max, filled, max)
    Row(
        // Merge the children into a single accessibility node that announces the
        // "$filled of $max stars" summary (rather than reading each star icon).
        // Unlike clearAndSetSemantics this keeps the per-star testTags present in
        // the unmerged semantics tree so the row stays inspectable in tests.
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = starsLabel
        },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        repeat(max) { i ->
            val isFilled = i < filled
            NimazIcon(
                imageVector = if (isFilled) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = null,
                tint = if (isFilled) filledColor else emptyColor,
                iconSize = starSize,
                modifier = Modifier.testTag("qaida_star"),
            )
        }
    }
}


// ==================== PREVIEWS ====================

@Composable
private fun QaidaStarRowShowcase() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        QaidaStarRow(filled = 0, starSize = 20.dp)
        QaidaStarRow(filled = 1, starSize = 20.dp)
        QaidaStarRow(filled = 2, starSize = 20.dp)
        QaidaStarRow(filled = 3, starSize = 20.dp)
    }
}

@Preview(showBackground = true, name = "Qaida Star Row — Light")
@Composable
private fun QaidaStarRowLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        QaidaStarRowShowcase()
    }
}

@Preview(showBackground = true, name = "Qaida Star Row — Dark")
@Composable
private fun QaidaStarRowDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        QaidaStarRowShowcase()
    }
}
