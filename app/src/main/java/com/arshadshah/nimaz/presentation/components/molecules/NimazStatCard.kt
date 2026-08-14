package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * One number and what it counts.
 *
 * **A zero is drawn muted, not accented.** A khatam started today shows 0 / 0 / 0, and three
 * bold accent-coloured zeroes at headline size read as three empty outlined ovals — the tiles
 * looked broken rather than empty, and the bug filed against them was "the stat tiles are
 * blank" when the data was correct all along. An accent is for a figure worth looking at; the
 * absence of one is not that, and saying so in the colour is what makes the zero legible as a
 * zero.
 */
@Composable
fun NimazStatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    valueColor: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val isZero = value.isBlank() || value.all { it == '0' || it == '.' || it == '%' }
    val resolvedValueColor =
        if (isZero) MaterialTheme.colorScheme.onSurfaceVariant else valueColor

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(if (compact) 10.dp else 14.dp))
            .background(containerColor)
            .padding(if (compact) 8.dp else 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                style = if (compact) MaterialTheme.typography.titleMedium
                else MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = resolvedValueColor
            )
            if (!compact) {
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 150, name = "NimazStatCard")
@Composable
private fun NimazStatCardPreview() {
    NimazTheme {
        NimazStatCard(
            value = "15",
            label = "Fasted",
            modifier = Modifier.padding(8.dp)
        )
    }
}
