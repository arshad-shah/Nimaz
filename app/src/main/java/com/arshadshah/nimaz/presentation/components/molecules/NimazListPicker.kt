package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.components.organisms.NimazSearchBar
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * A single row in a [NimazListPicker]. Generic over the underlying value
 * type so callers stay type-safe.
 *
 * - [icon]: optional leading icon (region flag, reciter avatar etc.).
 *   Pass null to reserve no leading space; the title left-aligns.
 * - [group]: optional section header. Items with the same group appear
 *   under one heading, in insertion order. Items with null groups are
 *   rendered ungrouped above any grouped sections.
 */
data class NimazPickerItem<T>(
    val value: T,
    val title: String,
    val description: String? = null,
    val icon: ImageVector? = null,
    val iconTint: Color? = null,
    val group: String? = null,
)

/**
 * A type-safe, searchable selection dialog. Use this anywhere the user picks
 * one option from a list (calculation method, reciter, translator, language,
 * etc.). Built on top of [NimazDialog], so it inherits the shared dialog
 * chrome (corners, tonal elevation, accent stripe, header glyph).
 *
 * Defaults are tuned for the common case (single-select, auto-dismiss):
 * tapping an item calls [onSelected] and [onDismiss] together. Override
 * [autoDismiss] to require an explicit Confirm tap instead.
 *
 * - [searchable]: shows a search bar that filters items by title +
 *   description (case-insensitive substring). Auto-enabled when the list has
 *   ≥ 8 items; pass true/false to override.
 * - The dialog ignores `usePlatformDefaultWidth` so it can stretch wider than
 *   a stock Material dialog, which matters for long descriptions / grouped
 *   sections that AlertDialog truncates.
 * - On open the list scrolls to centre the currently-selected item so the
 *   user sees where they are.
 */
@Composable
fun <T> NimazListPicker(
    title: String,
    items: List<NimazPickerItem<T>>,
    selected: T?,
    onSelected: (T) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    searchable: Boolean = items.size >= 8,
    autoDismiss: Boolean = true,
    confirmText: String = "Done",
    cancelText: String = "Cancel",
    searchPlaceholder: String = "Search",
    emptySearchText: String = "No matches",
) {
    var query by remember { mutableStateOf("") }

    // Filter items reactively against the search query. Description is part
    // of the search corpus because users often remember a method by where
    // it's used ("Karachi" vs "Used in Pakistan") rather than its name.
    val filtered by remember(items, query) {
        derivedStateOf {
            if (query.isBlank()) items
            else items.filter { item ->
                item.title.contains(query, ignoreCase = true) ||
                        (item.description?.contains(query, ignoreCase = true) ?: false)
            }
        }
    }

    // Group items by their `group` field (nullable). Nulls land in a
    // synthetic "Ungrouped" bucket and render first with no header.
    val grouped by remember(filtered) {
        derivedStateOf {
            filtered.groupBy { it.group }
        }
    }

    val listState = rememberLazyListState()

    // Scroll the selected item into view on first composition so the user
    // sees "where they are" rather than always landing at index 0.
    LaunchedEffect(selected) {
        if (selected != null) {
            val targetIndex = items.indexOfFirst { it.value == selected }
            if (targetIndex >= 0) {
                listState.scrollToItem(index = targetIndex)
            }
        }
    }

    // Delegate the dialog chrome (Surface, header row with title + close X,
    // tonal elevation, corners, action divider) to NimazDialog so the picker
    // automatically inherits any future design-system tweaks. We pass
    // wrapContent = false because the picker structures its own card-like
    // content (search bar + grouped list).
    NimazDialog(
        title = title,
        onDismiss = onDismiss,
        modifier = modifier,
        wrapContent = false,
        showActionsDivider = !autoDismiss,
        actions = if (autoDismiss) null else {
            {
                NimazDialogCancelButton(text = cancelText, onClick = onDismiss)
                NimazDialogConfirmButton(text = confirmText, onClick = onDismiss)
            }
        },
        content = {
            if (searchable) {
                NimazSearchBar(
                    query = query,
                    onQueryChange = { query = it },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (filtered.isEmpty()) {
                EmptyState(
                    text = emptySearchText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .heightIn(max = 440.dp)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Render ungrouped (null group) items first, then each
                    // named group with its header.
                    grouped[null]?.let { ungrouped ->
                        items(ungrouped, key = { it.title }) { item ->
                            PickerRow(
                                item = item,
                                isSelected = item.value == selected,
                                onClick = {
                                    onSelected(item.value)
                                    if (autoDismiss) onDismiss()
                                }
                            )
                        }
                    }
                    grouped
                        .filter { it.key != null }
                        .forEach { (group, groupItems) ->
                            item(key = "__header_${group}") {
                                GroupHeader(text = group!!)
                            }
                            items(groupItems, key = { "${group}_${it.title}" }) { item ->
                                PickerRow(
                                    item = item,
                                    isSelected = item.value == selected,
                                    onClick = {
                                        onSelected(item.value)
                                        if (autoDismiss) onDismiss()
                                    }
                                )
                            }
                        }
                }
            }
        }
    )
}

@Composable
private fun <T> PickerRow(
    item: NimazPickerItem<T>,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            }
        ),
        border = if (isSelected) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.icon != null) {
                val tint = item.iconTint ?: MaterialTheme.colorScheme.primary
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(tint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    NimazIcon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = tint,
                        size = NimazIconSize.MEDIUM
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                if (!item.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            if (isSelected) {
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    NimazIcon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        variant = NimazIconVariant.ON_ACCENT,
                        iconSize = 14.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 6.dp, top = 12.dp, bottom = 4.dp)
    )
}


@Composable
private fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            NimazIcon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                variant = NimazIconVariant.MUTED,
                iconSize = 28.dp
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ──── Previews ───────────────────────────────────────────────────────────────

private enum class CalcSample { MWL, EGYPT, KARACHI, UMM_AL_QURA, DUBAI, ISNA, KUWAIT, QATAR, SINGAPORE, TURKEY, MOON_SIGHTING }

@Preview(
    showBackground = true,
    widthDp = 412,
    heightDp = 700,
    name = "1. Searchable, with description"
)
@Composable
private fun NimazListPicker_Searchable_Preview() {
    NimazTheme {
        val items = listOf(
            NimazPickerItem(
                CalcSample.MWL,
                "Muslim World League",
                "Europe, Far East, parts of the US"
            ),
            NimazPickerItem(CalcSample.EGYPT, "Egyptian", "Africa, Syria, Lebanon, Malaysia"),
            NimazPickerItem(
                CalcSample.KARACHI,
                "Karachi",
                "Pakistan, Bangladesh, India, Afghanistan"
            ),
            NimazPickerItem(CalcSample.UMM_AL_QURA, "Umm al-Qura", "Arabian Peninsula"),
            NimazPickerItem(CalcSample.DUBAI, "Dubai", "UAE"),
            NimazPickerItem(CalcSample.ISNA, "ISNA (North America)", "US and Canada"),
            NimazPickerItem(CalcSample.KUWAIT, "Kuwait", "Kuwait"),
            NimazPickerItem(CalcSample.QATAR, "Qatar", "Qatar"),
            NimazPickerItem(CalcSample.SINGAPORE, "Singapore", "Singapore, Malaysia, Indonesia"),
            NimazPickerItem(CalcSample.TURKEY, "Turkey", "Turkey and Central Asia"),
            NimazPickerItem(
                CalcSample.MOON_SIGHTING,
                "Moon Sighting Committee",
                "UK, parts of Europe"
            ),
        )
        NimazListPicker(
            title = "Calculation Method",
            items = items,
            selected = CalcSample.KARACHI,
            onSelected = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 500, name = "2. Short list with icons")
@Composable
private fun NimazListPicker_WithIcons_Preview() {
    NimazTheme {
        val items = listOf(
            NimazPickerItem(
                value = "standard",
                title = "Standard",
                description = "Shafi'i, Maliki, Hanbali — shadow equals object length",
                icon = Icons.Default.WbSunny,
            ),
            NimazPickerItem(
                value = "hanafi",
                title = "Hanafi",
                description = "Shadow equals twice object length",
                icon = Icons.Default.Schedule,
            ),
        )
        NimazListPicker(
            title = "Asr Calculation",
            items = items,
            selected = "standard",
            onSelected = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 700, name = "3. Grouped (by region)")
@Composable
private fun NimazListPicker_Grouped_Preview() {
    NimazTheme {
        val items = listOf(
            NimazPickerItem(
                CalcSample.EGYPT,
                "Egyptian",
                "General Authority of Survey",
                group = "Africa & Middle East"
            ),
            NimazPickerItem(
                CalcSample.UMM_AL_QURA,
                "Umm al-Qura",
                "Saudi Arabia",
                group = "Africa & Middle East"
            ),
            NimazPickerItem(CalcSample.DUBAI, "Dubai", "UAE", group = "Africa & Middle East"),
            NimazPickerItem(CalcSample.KUWAIT, "Kuwait", "Kuwait", group = "Africa & Middle East"),
            NimazPickerItem(CalcSample.QATAR, "Qatar", "Qatar", group = "Africa & Middle East"),
            NimazPickerItem(
                CalcSample.KARACHI,
                "Karachi",
                "Pakistan, Bangladesh",
                group = "South Asia"
            ),
            NimazPickerItem(
                CalcSample.SINGAPORE,
                "Singapore",
                "Malaysia, Indonesia",
                group = "South Asia"
            ),
            NimazPickerItem(
                CalcSample.MWL,
                "Muslim World League",
                "Europe, parts of the US",
                group = "Other"
            ),
            NimazPickerItem(CalcSample.ISNA, "ISNA", "North America", group = "Other"),
            NimazPickerItem(CalcSample.TURKEY, "Turkey", "Turkey, Central Asia", group = "Other"),
            NimazPickerItem(
                CalcSample.MOON_SIGHTING,
                "Moon Sighting Committee",
                "UK",
                group = "Other"
            ),
        )
        NimazListPicker(
            title = "Calculation Method",
            items = items,
            selected = CalcSample.KARACHI,
            onSelected = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 400, name = "4. Empty search result")
@Composable
private fun NimazListPicker_EmptySearch_Preview() {
    NimazTheme {
        // Force searchable + we'll show the search bar; can't simulate query
        // in a static preview, but the empty-state composable is exercised
        // in the Searchable preview when the user types junk.
        NimazListPicker(
            title = "Reciter",
            items = emptyList<NimazPickerItem<String>>(),
            selected = null,
            onSelected = {},
            onDismiss = {},
            searchable = true,
        )
    }
}
