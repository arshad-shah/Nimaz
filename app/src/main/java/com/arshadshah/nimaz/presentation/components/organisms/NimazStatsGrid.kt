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

data class NimazStatData(
    val value: String,
    val label: String,
    val color: Color? = null
)

@Composable
fun NimazStatsGrid(
    stats: List<NimazStatData>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        stats.forEach { stat ->
            NimazStatCard(
                value = stat.value,
                label = stat.label,
                valueColor = stat.color ?: MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
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
