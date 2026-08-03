package com.arshadshah.nimaz.presentation.components.organisms

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.NimazChip
import com.arshadshah.nimaz.presentation.components.atoms.NimazChipVariant
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * A sticky index over a long document: one pill per section, tracking where you are.
 *
 * An index, not a set of doors. Accordions ask the reader to decide what is worth opening
 * before they have read any of it, and they auto-open whichever section happens to be first —
 * usually the least interesting one. Here the prose runs continuously and this says where in it
 * you are, the way a running head does.
 *
 * Scrolls horizontally, because the labels are words and there can be six of them on a 390dp
 * screen. The selected pill is scrolled into view when the reader arrives at its section, so
 * the index never claims you are somewhere it cannot show you.
 */
@Composable
fun NimazScrollSpyIndex(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (labels.size < 2) return

    val rowState = rememberLazyListState()
    LaunchedEffect(selectedIndex) {
        if (selectedIndex in labels.indices) rowState.animateScrollToItem(selectedIndex)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        androidx.compose.foundation.layout.Column {
            LazyRow(
                state = rowState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(labels) { index, label ->
                    NimazChip(
                        text = label,
                        selected = index == selectedIndex,
                        variant = NimazChipVariant.FILTER,
                        onClick = { onSelect(index) },
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

/**
 * Which section of a `LazyColumn` is currently being read.
 *
 * [anchors] holds each section's index in the list, ascending. The answer is the last anchor at
 * or before the first visible item — so scrolling *through* a long section keeps naming that
 * section rather than falling back to the one before it, which is what makes the index feel
 * attached to the prose rather than to the scrollbar.
 */
@Composable
fun rememberScrollSpyIndex(listState: LazyListState, anchors: List<Int>): State<Int> =
    remember(listState, anchors) {
        derivedStateOf {
            val first = listState.firstVisibleItemIndex
            anchors.indexOfLast { it <= first }.coerceAtLeast(0)
        }
    }

@Preview(showBackground = true, widthDp = 390, name = "NimazScrollSpyIndex — Light")
@Composable
private fun NimazScrollSpyIndexLightPreview() {
    NimazTheme(themeMode = ThemeMode.LIGHT) { ScrollSpySample() }
}

@Preview(
    showBackground = true, widthDp = 390, name = "NimazScrollSpyIndex — Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun NimazScrollSpyIndexDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) { ScrollSpySample() }
}

@Composable
private fun ScrollSpySample() {
    NimazScrollSpyIndex(
        labels = listOf("Name", "Revelation", "Theme", "Background"),
        selectedIndex = 2,
        onSelect = {},
    )
}
