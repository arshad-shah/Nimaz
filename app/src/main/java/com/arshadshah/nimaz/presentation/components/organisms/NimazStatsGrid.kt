package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.molecules.NimazStatCard
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.currentWindowSizeClass
import com.arshadshah.nimaz.presentation.theme.isCompact

data class NimazStatData(
    val value: String,
    val label: String,
    val color: Color? = null
)

@Composable
fun NimazStatsGrid(
    stats: List<NimazStatData>,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val windowSizeClass = currentWindowSizeClass()
    val spacing = if (compact) 6.dp else if (windowSizeClass.isCompact) 10.dp else 14.dp

    if (!windowSizeClass.isCompact && stats.size > 4) {
        // On tablet with many stats, split into two rows
        val firstRowCount = (stats.size + 1) / 2
        val firstRow = stats.take(firstRowCount)
        val secondRow = stats.drop(firstRowCount)

        androidx.compose.foundation.layout.Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                firstRow.forEach { stat ->
                    NimazStatCard(
                        value = stat.value,
                        label = stat.label,
                        compact = compact,
                        valueColor = stat.color ?: MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                secondRow.forEach { stat ->
                    NimazStatCard(
                        value = stat.value,
                        label = stat.label,
                        compact = compact,
                        valueColor = stat.color ?: MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
                // Add spacers for alignment if second row has fewer items
                repeat(firstRowCount - secondRow.size) {
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    } else {
        // Standard single-row layout (phone or <= 4 stats on tablet)
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            stats.forEach { stat ->
                NimazStatCard(
                    value = stat.value,
                    label = stat.label,
                    compact = compact,
                    valueColor = stat.color ?: MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 400, name = "NimazStatsGrid - 3 columns")
@Composable
private fun NimazStatsGridPreview() {
    NimazTheme {
        NimazStatsGrid(
            stats = listOf(
                NimazStatData("15", "Fasted"),
                NimazStatData("3", "Missed"),
                NimazStatData("12", "Remaining")
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
