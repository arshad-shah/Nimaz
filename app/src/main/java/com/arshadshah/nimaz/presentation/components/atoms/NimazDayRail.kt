package com.arshadshah.nimaz.presentation.components.atoms

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/** How far a disabled cell is faded. Matches the Material 3 disabled-content alpha. */
private const val DisabledAlpha = 0.38f

/**
 * One cell of a [NimazDayRail].
 *
 * Labels arrive already formatted — the rail knows nothing about dates, locales or fasting, which
 * is what keeps it an atom rather than a feature component wearing one's clothes.
 *
 * @param contentDescription required rather than optional: "13" alone is not a date, and a rail
 *   of seven bare numbers is unusable with a screen reader.
 */
data class NimazDayRailItem(
    val weekdayLabel: String,
    val dayLabel: String,
    val marker: NimazStatusDotSpec? = null,
    val isToday: Boolean = false,
    val enabled: Boolean = true,
    val contentDescription: String,
)

/**
 * A week as equal cells, each with a weekday initial, a day number and an optional status marker.
 *
 * A `Row`, not a `LazyRow`: a week is a fixed seven, and a lazy list here would add scroll state
 * to lose on recomposition in exchange for nothing.
 *
 * @param selectedIndex the chosen cell, or `null` for none.
 * @param onSelect invoked with the tapped index; a cell with `enabled = false` never emits.
 */
@Composable
fun NimazDayRail(
    days: List<NimazDayRailItem>,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        days.forEachIndexed { index, day ->
            val selected = selectedIndex == index
            val cellShape = RoundedCornerShape(16.dp)
            val alpha = if (day.enabled) 1f else DisabledAlpha

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(cellShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.surface else Color.Transparent
                    )
                    .then(
                        if (selected) {
                            Modifier.border(1.dp, MaterialTheme.colorScheme.primary, cellShape)
                        } else {
                            Modifier
                        }
                    )
                    .selectable(
                        selected = selected,
                        enabled = day.enabled,
                        role = Role.Tab,
                        onClick = { onSelect(index) }
                    )
                    // One announcement per cell. Without this a screen reader reads the weekday
                    // initial, the number and the marker as three separate unlabelled nodes.
                    .clearAndSetSemantics { contentDescription = day.contentDescription }
                    .padding(vertical = 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(
                    text = day.weekdayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                )
                Text(
                    text = day.dayLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium,
                    color = if (day.isToday) {
                        MaterialTheme.colorScheme.primary.copy(alpha = alpha)
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                    }
                )
                // The slot is always occupied, so cells keep one height whether or not a day
                // carries a marker — a rail that reflows as records load reads as jitter.
                Box(modifier = Modifier.size(NimazStatusDotSize.MEDIUM.diameter)) {
                    day.marker?.let { NimazStatusDot(spec = it) }
                }
            }
        }
    }
}

// ==================== PREVIEWS ====================

private val previewWeek = listOf(
    NimazDayRailItem(
        "M", "10", NimazStatusDotSpec(NimazTone.SUCCESS),
        contentDescription = "Monday 10 August, fasted"
    ),
    NimazDayRailItem(
        "T", "11", NimazStatusDotSpec(NimazTone.NEUTRAL, NimazStatusDotStyle.OUTLINED),
        contentDescription = "Tuesday 11 August, not fasting"
    ),
    NimazDayRailItem(
        "W", "12", NimazStatusDotSpec(NimazTone.WARNING),
        contentDescription = "Wednesday 12 August, owed"
    ),
    NimazDayRailItem(
        "T", "13", isToday = true,
        contentDescription = "Thursday 13 August, today, not logged"
    ),
    NimazDayRailItem("F", "14", enabled = false, contentDescription = "Friday 14 August"),
    NimazDayRailItem("S", "15", enabled = false, contentDescription = "Saturday 15 August"),
    NimazDayRailItem("S", "16", enabled = false, contentDescription = "Sunday 16 August"),
)

@Composable
private fun ShowcaseLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun NimazDayRailShowcase() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        ShowcaseLabel("Today selected")
        NimazDayRail(previewWeek, selectedIndex = 3, onSelect = {})

        ShowcaseLabel("A past day selected")
        NimazDayRail(previewWeek, selectedIndex = 0, onSelect = {})

        ShowcaseLabel("Nothing selected")
        NimazDayRail(previewWeek, selectedIndex = null, onSelect = {})

        ShowcaseLabel("A fully logged week")
        NimazDayRail(
            previewWeek.map {
                it.copy(marker = NimazStatusDotSpec(NimazTone.SUCCESS), enabled = true)
            },
            selectedIndex = 3,
            onSelect = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 360, name = "NimazDayRail — Light")
@Composable
private fun NimazDayRailLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { NimazDayRailShowcase() }
}

@Preview(
    showBackground = true, widthDp = 360, name = "NimazDayRail — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun NimazDayRailDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { NimazDayRailShowcase() }
}
