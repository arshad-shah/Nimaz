package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazChip
import com.arshadshah.nimaz.presentation.components.atoms.NimazChipVariant
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * Search result data.
 */
data class SearchResult(
    val id: String,
    val title: String,
    val subtitle: String?,
    val type: SearchResultType,
    val highlightedText: String? = null
)

enum class SearchResultType {
    QURAN,
    HADITH,
    DUA,
    SURAH,
    CHAPTER,
    CATEGORY
}

/**
 * Pinned height for the search bar. Matches Material 3's standard
 * single-line input height so the bar lines up visually with text fields
 * elsewhere in the app and stays consistent whether or not the trailing
 * area (clear button, loading spinner, custom slot) is rendered.
 */
private val NimazSearchBarHeight = 56.dp

/**
 * The single search-bar primitive for the app. Every screen that needs a
 * search input should use this — it provides everything the various ad-hoc
 * search bars across the codebase used to do individually:
 *
 * - Focus-driven primary border (animated) so users see the field is active.
 * - Optional [isLoading] spinner that swaps in where the clear button would
 *   sit — for screens that fire async lookups (location search, etc.).
 * - Optional [autoFocus] for full-screen search and picker dialogs where
 *   the keyboard should pop up immediately.
 * - Optional [trailing] slot for filter / voice / scan icons that some
 *   screens may want next to the clear button.
 * - Animated visibility on the clear button so its appearance/disappearance
 *   doesn't feel jumpy as the user types.
 *
 * API is fully backwards-compatible — existing callers (NimazListPicker,
 * SearchScreen, the asma/prophets list screens) continue to work with just
 * the original parameters.
 */
@Composable
fun NimazSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search...",
    enabled: Boolean = true,
    isLoading: Boolean = false,
    showClearButton: Boolean = true,
    autoFocus: Boolean = false,
    onClear: () -> Unit = {},
    onSearch: (String) -> Unit = {},
    leadingIcon: @Composable (() -> Unit)? = {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    },
    trailing: @Composable (RowScope.() -> Unit)? = null,
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    // Animate the border to communicate focus state. M3 OutlinedTextField does
    // the same thing; we replicate it here so this primitive matches.
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) {
            MaterialTheme.colorScheme.primary
        } else {
            Color.Transparent
        },
        label = "search_focus_border"
    )
    val borderWidth: Dp by animateDpAsState(
        targetValue = if (isFocused) 1.5.dp else 0.dp,
        label = "search_focus_border_width"
    )

    LaunchedEffect(autoFocus) {
        if (autoFocus) focusRequester.requestFocus()
    }

    val maxWidth = com.arshadshah.nimaz.presentation.theme.AdaptiveSpacing.maxSearchBarWidth()
    Surface(
        modifier = modifier
            .then(
                if (maxWidth != Dp.Unspecified) Modifier.widthIn(max = maxWidth)
                else Modifier
            )
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 1.dp,
        border = if (borderWidth > 0.dp) {
            androidx.compose.foundation.BorderStroke(borderWidth, borderColor)
        } else null,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // Pin the bar's height to the M3 search-bar standard so it
                // looks identical whether or not the clear button / loading
                // spinner / trailing slot is rendered. Without this floor,
                // the bar collapses to ~36dp when nothing's typed and pops up
                // to ~56dp the moment a trailing affordance appears.
                .heightIn(min = NimazSearchBarHeight)
                .padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon?.invoke()
            if (leadingIcon != null) {
                Spacer(modifier = Modifier.width(12.dp))
            }

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .semantics { contentDescription = placeholder },
                enabled = enabled,
                interactionSource = interactionSource,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        onSearch(query)
                        focusManager.clearFocus()
                    }
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        innerTextField()
                    }
                }
            )

            // Trailing affordances: loading spinner OR clear button (mutually
            // exclusive — when loading, clearing makes no sense), plus an
            // optional caller-supplied trailing slot.
            AnimatedVisibility(
                visible = isLoading,
                enter = fadeIn() + scaleIn(initialScale = 0.7f),
                exit = fadeOut() + scaleOut(targetScale = 0.7f)
            ) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            AnimatedVisibility(
                visible = !isLoading && showClearButton && query.isNotEmpty(),
                enter = fadeIn() + scaleIn(initialScale = 0.7f),
                exit = fadeOut() + scaleOut(targetScale = 0.7f)
            ) {
                IconButton(
                    onClick = {
                        onClear()
                        focusRequester.requestFocus()
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.cd_clear_search),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (trailing != null) {
                trailing()
            }
        }
    }
}

/**
 * Expandable search bar with suggestions.
 */
@Composable
fun ExpandableSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search Quran, Hadith, Duas...",
    recentSearches: List<String> = emptyList(),
    suggestions: List<String> = emptyList(),
    isLoading: Boolean = false,
    onRecentSearchClick: (String) -> Unit = {},
    onClearRecentSearches: () -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val expandableMaxWidth =
        com.arshadshah.nimaz.presentation.theme.AdaptiveSpacing.maxSearchBarWidth()
    Column(
        modifier = modifier
            .then(
                if (expandableMaxWidth != Dp.Unspecified)
                    Modifier.widthIn(max = expandableMaxWidth)
                else Modifier
            )
            .fillMaxWidth()
    ) {
        // Search input
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(if (isExpanded) 16.dp else 28.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            onClick = { isExpanded = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.width(12.dp))

                BasicTextField(
                    value = query,
                    onValueChange = {
                        onQueryChange(it)
                        isExpanded = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            onSearch(query)
                            isExpanded = false
                            focusManager.clearFocus()
                        }
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box {
                            if (query.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else if (query.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            onQueryChange("")
                            focusRequester.requestFocus()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(R.string.cd_clear),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Expanded content
        AnimatedVisibility(
            visible = isExpanded && query.isEmpty(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Recent searches
                    if (recentSearches.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recent Searches",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Clear",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { onClearRecentSearches() }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        recentSearches.take(5).forEach { search ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onRecentSearchClick(search)
                                        isExpanded = false
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = search,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    // Suggestions
                    if (suggestions.isNotEmpty()) {
                        if (recentSearches.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Popular Searches",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(suggestions) { suggestion ->
                                NimazChip(
                                    text = suggestion,
                                    onClick = {
                                        onQueryChange(suggestion)
                                        onSearch(suggestion)
                                        isExpanded = false
                                    },
                                    variant = NimazChipVariant.SUGGESTION
                                )
                            }
                        }
                    }
                }
            }
        }

        // Auto suggestions while typing
        AnimatedVisibility(
            visible = isExpanded && query.isNotEmpty() && suggestions.isNotEmpty(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    suggestions.filter { it.contains(query, ignoreCase = true) }.take(5)
                        .forEach { suggestion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onQueryChange(suggestion)
                                        onSearch(suggestion)
                                        isExpanded = false
                                        focusManager.clearFocus()
                                    }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = suggestion,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                }
            }
        }
    }

    // Request focus when expanded
    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            focusRequester.requestFocus()
        }
    }
}

@Composable
private fun SearchResultCard(
    result: SearchResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(getSearchResultTypeColor(result.type).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getSearchResultTypeLabel(result.type),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = getSearchResultTypeColor(result.type)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (result.subtitle != null) {
                    Text(
                        text = result.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (result.highlightedText != null) {
                    Text(
                        text = result.highlightedText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun getSearchResultTypeColor(type: SearchResultType): Color {
    return when (type) {
        SearchResultType.QURAN -> Color(0xFF795548)
        SearchResultType.HADITH -> Color(0xFF00796B)
        SearchResultType.DUA -> Color(0xFF7C4DFF)
        SearchResultType.SURAH -> Color(0xFF795548)
        SearchResultType.CHAPTER -> Color(0xFF00796B)
        SearchResultType.CATEGORY -> Color(0xFF7C4DFF)
    }
}

private fun getSearchResultTypeLabel(type: SearchResultType): String {
    return when (type) {
        SearchResultType.QURAN -> "Q"
        SearchResultType.HADITH -> "H"
        SearchResultType.DUA -> "D"
        SearchResultType.SURAH -> "S"
        SearchResultType.CHAPTER -> "C"
        SearchResultType.CATEGORY -> "C"
    }
}

// ──── NimazSearchBar previews ───────────────────────────────────────────────
//
// Open these in Android Studio's preview pane. The "0. Showcase" preview
// stacks every state with a caption so all variants read in one glance — use
// it for design iteration. The individual previews below let you zoom in on
// each state for focused inspection.

@Preview(
    showBackground = true,
    widthDp = 412,
    heightDp = 760,
    name = "0. Showcase — all states"
)
@Composable
private fun NimazSearchBar_Showcase_Preview() {
    NimazTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PreviewSection(label = "Empty (default placeholder)") {
                NimazSearchBar(query = "", onQueryChange = {})
            }
            PreviewSection(label = "With query (clear button visible)") {
                NimazSearchBar(
                    query = "Al-Fatiha",
                    onQueryChange = {},
                    onClear = {},
                )
            }
            PreviewSection(label = "Loading (async lookup in flight)") {
                NimazSearchBar(
                    query = "Mecca",
                    onQueryChange = {},
                    isLoading = true,
                    placeholder = "Search city or address...",
                )
            }
            PreviewSection(label = "Disabled") {
                NimazSearchBar(
                    query = "",
                    onQueryChange = {},
                    enabled = false,
                    placeholder = "Search unavailable while syncing…",
                )
            }
            PreviewSection(label = "Trailing slot (filter chip)") {
                NimazSearchBar(
                    query = "",
                    onQueryChange = {},
                    placeholder = "Search reciters...",
                    trailing = {
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = {}, modifier = Modifier.size(40.dp)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = stringResource(R.string.cd_filter),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                )
            }
            PreviewSection(label = "Long query (truncation behaviour)") {
                NimazSearchBar(
                    query = "Surat al-Kahf wal-Anbiya wal-Saffat verses about patience",
                    onQueryChange = {},
                    onClear = {},
                )
            }
            PreviewSection(label = "No leading icon (terse contexts)") {
                NimazSearchBar(
                    query = "Karachi",
                    onQueryChange = {},
                    onClear = {},
                    leadingIcon = null,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, name = "1. Empty (default)")
@Composable
private fun NimazSearchBar_Empty_Preview() {
    NimazTheme {
        NimazSearchBar(
            query = "",
            onQueryChange = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 412, name = "2. With query")
@Composable
private fun NimazSearchBar_WithQuery_Preview() {
    NimazTheme {
        NimazSearchBar(
            query = "Al-Fatiha",
            onQueryChange = {},
            onClear = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 412, name = "3. Loading (location search)")
@Composable
private fun NimazSearchBar_Loading_Preview() {
    NimazTheme {
        NimazSearchBar(
            query = "Mecca",
            onQueryChange = {},
            isLoading = true,
            placeholder = "Search city or address...",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 412, name = "4. Disabled")
@Composable
private fun NimazSearchBar_Disabled_Preview() {
    NimazTheme {
        NimazSearchBar(
            query = "",
            onQueryChange = {},
            enabled = false,
            placeholder = "Search unavailable",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 412, name = "5. With trailing filter slot")
@Composable
private fun NimazSearchBar_Trailing_Preview() {
    NimazTheme {
        NimazSearchBar(
            query = "",
            onQueryChange = {},
            placeholder = "Search reciters...",
            trailing = {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = {}, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = stringResource(R.string.cd_filter),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 412, name = "6. Long query (truncates)")
@Composable
private fun NimazSearchBar_LongQuery_Preview() {
    NimazTheme {
        NimazSearchBar(
            query = "Surat al-Kahf wal-Anbiya wal-Saffat verses about patience",
            onQueryChange = {},
            onClear = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 412, name = "7. No leading icon")
@Composable
private fun NimazSearchBar_NoLeadingIcon_Preview() {
    NimazTheme {
        NimazSearchBar(
            query = "Karachi",
            onQueryChange = {},
            onClear = {},
            leadingIcon = null,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 260, name = "8. On dialog surface")
@Composable
private fun NimazSearchBar_OnDialogSurface_Preview() {
    // Verifies the bar still reads as a distinct input when placed inside a
    // tonal-elevated Surface like NimazListPicker / NimazDialog.
    NimazTheme {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Calculation Method",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                NimazSearchBar(
                    query = "",
                    onQueryChange = {},
                    placeholder = "Search",
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, name = "9. ExpandableSearchBar")
@Composable
private fun ExpandableSearchBarPreview() {
    NimazTheme {
        ExpandableSearchBar(
            query = "",
            onQueryChange = {},
            onSearch = {},
            recentSearches = listOf("Al-Baqarah", "Ayat al-Kursi"),
            suggestions = listOf("Surah Yasin", "Juz Amma"),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun PreviewSection(
    label: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        content()
    }
}
