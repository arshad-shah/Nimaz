package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * A row of small dots showing how many of a lesson's lines are complete —
 * a calm, kid-readable progress indicator for the reader's top bar.
 */
@Composable
fun QaidaLineProgressDots(
    total: Int,
    completed: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(total) { i ->
            val color = if (i < completed) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
                    .testTag("qaida_dot"),
            )
        }
    }
}


// ==================== PREVIEWS ====================

@Composable
private fun QaidaLineProgressDotsShowcase() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        QaidaLineProgressDots(total = 6, completed = 0)
        QaidaLineProgressDots(total = 6, completed = 3)
        QaidaLineProgressDots(total = 6, completed = 6)
    }
}

@Preview(showBackground = true, name = "Qaida Line Progress Dots — Light")
@Composable
private fun QaidaLineProgressDotsLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) {
        QaidaLineProgressDotsShowcase()
    }
}

@Preview(showBackground = true, name = "Qaida Line Progress Dots — Dark")
@Composable
private fun QaidaLineProgressDotsDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        QaidaLineProgressDotsShowcase()
    }
}
