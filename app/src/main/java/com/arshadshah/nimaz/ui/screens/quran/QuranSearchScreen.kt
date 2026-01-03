package com.arshadshah.nimaz.ui.screens.quran

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.data.local.models.LocalAya
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont
import com.arshadshah.nimaz.utils.StringUtils.cleanTextFromBackslash
import com.arshadshah.nimaz.viewModel.QuranViewModel

@Composable
fun QuranSearchScreen(
    searchQuery: String,
    searchResults: List<LocalAya>,
    searchLanguage: String,
    searchFilters: QuranViewModel.SearchFilters,
    isLoading: Boolean,
    error: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchLanguageChange: (String) -> Unit,
    onSearchFiltersChange: (QuranViewModel.SearchFilters) -> Unit,
    onSearch: (String) -> Unit,
    onAdvancedSearch: () -> Unit,
    onSearchInFavorites: (String) -> Unit,
    onSearchInBookmarks: (String) -> Unit,
    onSearchInNotes: (String) -> Unit,
    onClearSearch: () -> Unit,
    onAyaClick: (LocalAya) -> Unit,
    onBookmarkToggle: (LocalAya) -> Unit,
    onFavoriteToggle: (LocalAya) -> Unit
) {
    var showFilters by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Search Card
        SearchCard(
            searchQuery = searchQuery,
            searchLanguage = searchLanguage,
            searchFilters = searchFilters,
            showFilters = showFilters,
            onSearchQueryChange = onSearchQueryChange,
            onSearchLanguageChange = onSearchLanguageChange,
            onSearchFiltersChange = onSearchFiltersChange,
            onSearch = {
                onSearch(it)
                focusManager.clearFocus()
            },
            onAdvancedSearch = {
                onAdvancedSearch()
                focusManager.clearFocus()
            },
            onSearchInFavorites = {
                onSearchInFavorites(it)
                focusManager.clearFocus()
            },
            onSearchInBookmarks = {
                onSearchInBookmarks(it)
                focusManager.clearFocus()
            },
            onClearSearch = onClearSearch,
            onToggleFilters = { showFilters = !showFilters }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Results Section
        SearchResults(
            searchResults = searchResults,
            searchQuery = searchQuery,
            isLoading = isLoading,
            error = error,
            onAyaClick = onAyaClick,
            onBookmarkToggle = onBookmarkToggle,
            onFavoriteToggle = onFavoriteToggle
        )
    }
}

@Composable
private fun SearchCard(
    searchQuery: String,
    searchLanguage: String,
    searchFilters: QuranViewModel.SearchFilters,
    showFilters: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onSearchLanguageChange: (String) -> Unit,
    onSearchFiltersChange: (QuranViewModel.SearchFilters) -> Unit,
    onSearch: (String) -> Unit,
    onAdvancedSearch: () -> Unit,
    onSearchInFavorites: (String) -> Unit,
    onSearchInBookmarks: (String) -> Unit,
    onClearSearch: () -> Unit,
    onToggleFilters: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(10.dp)
                                    .size(24.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Search Quran",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Find ayat by text, translation, or keywords",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Search Input Section
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Search Field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "Type to search...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        trailingIcon = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                AnimatedVisibility(
                                    visible = searchQuery.isNotEmpty(),
                                    enter = fadeIn() + scaleIn(),
                                    exit = fadeOut() + scaleOut()
                                ) {
                                    IconButton(
                                        onClick = onClearSearch,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                AnimatedVisibility(
                                    visible = searchQuery.isNotEmpty(),
                                    enter = fadeIn() + scaleIn(),
                                    exit = fadeOut() + scaleOut()
                                ) {
                                    FilledIconButton(
                                        onClick = { onSearch(searchQuery) },
                                        modifier = Modifier.size(36.dp),
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Icon(
                                            Icons.Default.Search,
                                            contentDescription = "Search",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = { if (searchQuery.isNotEmpty()) onSearch(searchQuery) }
                        ),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    // Quick Filters
                    QuickFiltersRow(
                        searchLanguage = searchLanguage,
                        searchQuery = searchQuery,
                        searchFilters = searchFilters,
                        showFilters = showFilters,
                        onSearchLanguageChange = onSearchLanguageChange,
                        onSearchInFavorites = onSearchInFavorites,
                        onSearchInBookmarks = onSearchInBookmarks,
                        onSearch = onSearch,
                        onToggleFilters = onToggleFilters
                    )

                    // Advanced Filters
                    AnimatedVisibility(
                        visible = showFilters,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        AdvancedFilters(
                            searchFilters = searchFilters,
                            searchQuery = searchQuery,
                            onSearchFiltersChange = onSearchFiltersChange,
                            onAdvancedSearch = onAdvancedSearch,
                            onSearch = onSearch
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickFiltersRow(
    searchLanguage: String,
    searchQuery: String,
    searchFilters: QuranViewModel.SearchFilters,
    showFilters: Boolean,
    onSearchLanguageChange: (String) -> Unit,
    onSearchInFavorites: (String) -> Unit,
    onSearchInBookmarks: (String) -> Unit,
    onSearch: (String) -> Unit,
    onToggleFilters: () -> Unit
) {
    val hasActiveFilters = searchFilters.isFavorite == true ||
            searchFilters.isBookmarked == true ||
            searchFilters.hasNote == true

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        // Advanced Filters Toggle
        item {
            FilterChip(
                selected = showFilters || hasActiveFilters,
                onClick = onToggleFilters,
                label = { Text("Filters") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        }

        // Language Selector
        item {
            var expanded by remember { mutableStateOf(false) }
            Box {
                FilterChip(
                    selected = searchLanguage !in listOf("All", "Favorites", "Bookmarks", "Notes"),
                    onClick = { expanded = true },
                    label = {
                        Text(
                            when (searchLanguage) {
                                "Favorites", "Bookmarks", "Notes" -> "All"
                                else -> searchLanguage
                            }
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Language,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    listOf("All", "Arabic", "English", "Urdu").forEach { language ->
                        DropdownMenuItem(
                            text = { Text(language) },
                            onClick = {
                                onSearchLanguageChange(language)
                                expanded = false
                                if (searchQuery.isNotEmpty()) onSearch(searchQuery)
                            },
                            leadingIcon = if (language == searchLanguage) {
                                {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else null
                        )
                    }
                }
            }
        }

        // Favorites Filter
        item {
            FilterChip(
                selected = searchLanguage == "Favorites",
                onClick = {
                    val newLang = if (searchLanguage == "Favorites") "All" else "Favorites"
                    onSearchLanguageChange(newLang)
                    if (searchQuery.isNotEmpty()) {
                        if (newLang == "Favorites") onSearchInFavorites(searchQuery)
                        else onSearch(searchQuery)
                    }
                },
                label = { Text("Favorites") },
                leadingIcon = {
                    Icon(
                        if (searchLanguage == "Favorites") Icons.Default.Favorite
                        else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onErrorContainer
                )
            )
        }

        // Bookmarks Filter
        item {
            FilterChip(
                selected = searchLanguage == "Bookmarks",
                onClick = {
                    val newLang = if (searchLanguage == "Bookmarks") "All" else "Bookmarks"
                    onSearchLanguageChange(newLang)
                    if (searchQuery.isNotEmpty()) {
                        if (newLang == "Bookmarks") onSearchInBookmarks(searchQuery)
                        else onSearch(searchQuery)
                    }
                },
                label = { Text("Bookmarks") },
                leadingIcon = {
                    Icon(
                        if (searchLanguage == "Bookmarks") Icons.Default.Bookmark
                        else Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
private fun AdvancedFilters(
    searchFilters: QuranViewModel.SearchFilters,
    searchQuery: String,
    onSearchFiltersChange: (QuranViewModel.SearchFilters) -> Unit,
    onAdvancedSearch: () -> Unit,
    onSearch: (String) -> Unit
) {
    val hasActiveFilters = searchFilters.isFavorite == true ||
            searchFilters.isBookmarked == true ||
            searchFilters.hasNote == true

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter Results",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (hasActiveFilters) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable {
                            onSearchFiltersChange(QuranViewModel.SearchFilters())
                            if (searchQuery.isNotEmpty()) onSearch(searchQuery)
                        }
                    ) {
                        Text(
                            text = "Clear All",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = searchFilters.isFavorite == true,
                    onClick = {
                        val newValue = if (searchFilters.isFavorite == true) null else true
                        onSearchFiltersChange(searchFilters.copy(isFavorite = newValue))
                        if (searchQuery.isNotEmpty()) onAdvancedSearch()
                    },
                    label = { Text("Favorites Only") },
                    leadingIcon = {
                        Icon(
                            if (searchFilters.isFavorite == true) Icons.Default.Favorite
                            else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = searchFilters.isBookmarked == true,
                    onClick = {
                        val newValue = if (searchFilters.isBookmarked == true) null else true
                        onSearchFiltersChange(searchFilters.copy(isBookmarked = newValue))
                        if (searchQuery.isNotEmpty()) onAdvancedSearch()
                    },
                    label = { Text("Bookmarked") },
                    leadingIcon = {
                        Icon(
                            if (searchFilters.isBookmarked == true) Icons.Default.Bookmark
                            else Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            FilterChip(
                selected = searchFilters.hasNote == true,
                onClick = {
                    val newValue = if (searchFilters.hasNote == true) null else true
                    onSearchFiltersChange(searchFilters.copy(hasNote = newValue))
                    if (searchQuery.isNotEmpty()) onAdvancedSearch()
                },
                label = { Text("Has Notes") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Notes,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun SearchResults(
    searchResults: List<LocalAya>,
    searchQuery: String,
    isLoading: Boolean,
    error: String,
    onAyaClick: (LocalAya) -> Unit,
    onBookmarkToggle: (LocalAya) -> Unit,
    onFavoriteToggle: (LocalAya) -> Unit
) {
    AnimatedContent(
        targetState = Triple(searchResults.isEmpty(), searchQuery.isNotEmpty(), isLoading),
        transitionSpec = {
            fadeIn() + slideInVertically { 20 } togetherWith fadeOut()
        },
        label = "searchResults"
    ) { (isEmpty, hasQuery, loading) ->
        when {
            loading -> LoadingState()
            error.isNotEmpty() -> ErrorState(error)
            isEmpty && hasQuery -> EmptyResultsState()
            !hasQuery -> EmptySearchState()
            else -> ResultsList(
                results = searchResults,
                searchQuery = searchQuery,
                onAyaClick = onAyaClick,
                onBookmarkToggle = onBookmarkToggle,
                onFavoriteToggle = onFavoriteToggle
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp
            )
            Text(
                text = "Searching the Quran...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorState(error: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        ElevatedCard(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(24.dp)
            )
        }
    }
}

@Composable
private fun EmptyResultsState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "No Matches Found",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Try adjusting your search terms or filters",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptySearchState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = "Search the Quran",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Find ayat by Arabic text, translation, or keywords",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ResultsList(
    results: List<LocalAya>,
    searchQuery: String,
    onAyaClick: (LocalAya) -> Unit,
    onBookmarkToggle: (LocalAya) -> Unit,
    onFavoriteToggle: (LocalAya) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Results Count
        item {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "${results.size} result${if (results.size != 1) "s" else ""} found",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        items(
            items = results,
            key = { "${it.suraNumber}-${it.ayaNumberInSurah}" }
        ) { aya ->
            AyaResultCard(
                aya = aya,
                searchQuery = searchQuery,
                onClick = { onAyaClick(aya) },
                onBookmarkToggle = { onBookmarkToggle(aya) },
                onFavoriteToggle = { onFavoriteToggle(aya) }
            )
        }
    }
}

@Composable
private fun AyaResultCard(
    aya: LocalAya,
    searchQuery: String,
    onClick: () -> Unit,
    onBookmarkToggle: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "${aya.suraNumber}:${aya.ayaNumberInSurah}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = "Surah ${aya.suraNumber}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilledTonalIconButton(
                            onClick = onFavoriteToggle,
                            modifier = Modifier.size(32.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (aya.favorite)
                                    MaterialTheme.colorScheme.errorContainer
                                else
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                            )
                        ) {
                            Icon(
                                imageVector = if (aya.favorite) Icons.Default.Favorite
                                else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                modifier = Modifier.size(16.dp),
                                tint = if (aya.favorite)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        FilledTonalIconButton(
                            onClick = onBookmarkToggle,
                            modifier = Modifier.size(32.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (aya.bookmark)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                            )
                        ) {
                            Icon(
                                imageVector = if (aya.bookmark) Icons.Default.Bookmark
                                else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark",
                                modifier = Modifier.size(16.dp),
                                tint = if (aya.bookmark)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Arabic Text
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = aya.ayaArabic.cleanTextFromBackslash(),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = utmaniQuranFont,
                            lineHeight = 44.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        textAlign = TextAlign.Start
                    )
                }
            }

            // Translation
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = highlightText(
                        text = aya.translationEnglish.cleanTextFromBackslash(),
                        query = searchQuery,
                        highlightColor = MaterialTheme.colorScheme.primary
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    lineHeight = 22.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                )
            }

            // Urdu Translation (if matches query)
            if (searchQuery.isNotEmpty() && aya.translationUrdu.contains(searchQuery, ignoreCase = true)) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            text = highlightText(
                                text = aya.translationUrdu.cleanTextFromBackslash(),
                                query = searchQuery,
                                highlightColor = MaterialTheme.colorScheme.secondary
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }

            // Note Section
            if (aya.note.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Notes,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = highlightText(
                                text = aya.note,
                                query = searchQuery,
                                highlightColor = MaterialTheme.colorScheme.tertiary
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun highlightText(
    text: String,
    query: String,
    highlightColor: Color
): androidx.compose.ui.text.AnnotatedString {
    if (query.isBlank()) return androidx.compose.ui.text.AnnotatedString(text)

    val lowerText = text.lowercase()
    val lowerQuery = query.lowercase()

    val builder = androidx.compose.ui.text.AnnotatedString.Builder(text)
    var startIndex = 0

    while (true) {
        val index = lowerText.indexOf(lowerQuery, startIndex)
        if (index == -1) break

        builder.addStyle(
            style = SpanStyle(
                background = highlightColor.copy(alpha = 0.2f),
                fontWeight = FontWeight.Bold,
                color = highlightColor
            ),
            start = index,
            end = index + query.length
        )
        startIndex = index + query.length
    }

    return builder.toAnnotatedString()
}

// =============================================================================
// PREVIEWS
// =============================================================================

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun QuranSearchScreenPreview_Empty() {
    MaterialTheme {
        QuranSearchScreen(
            searchQuery = "",
            searchResults = emptyList(),
            searchLanguage = "All",
            searchFilters = QuranViewModel.SearchFilters(),
            isLoading = false,
            error = "",
            onSearchQueryChange = {},
            onSearchLanguageChange = {},
            onSearchFiltersChange = {},
            onSearch = {},
            onAdvancedSearch = {},
            onSearchInFavorites = {},
            onSearchInBookmarks = {},
            onSearchInNotes = {},
            onClearSearch = {},
            onAyaClick = {},
            onBookmarkToggle = {},
            onFavoriteToggle = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun QuranSearchScreenPreview_WithQuery() {
    MaterialTheme {
        QuranSearchScreen(
            searchQuery = "mercy",
            searchResults = emptyList(),
            searchLanguage = "English",
            searchFilters = QuranViewModel.SearchFilters(),
            isLoading = false,
            error = "",
            onSearchQueryChange = {},
            onSearchLanguageChange = {},
            onSearchFiltersChange = {},
            onSearch = {},
            onAdvancedSearch = {},
            onSearchInFavorites = {},
            onSearchInBookmarks = {},
            onSearchInNotes = {},
            onClearSearch = {},
            onAyaClick = {},
            onBookmarkToggle = {},
            onFavoriteToggle = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun QuranSearchScreenPreview_Loading() {
    MaterialTheme {
        QuranSearchScreen(
            searchQuery = "peace",
            searchResults = emptyList(),
            searchLanguage = "All",
            searchFilters = QuranViewModel.SearchFilters(),
            isLoading = true,
            error = "",
            onSearchQueryChange = {},
            onSearchLanguageChange = {},
            onSearchFiltersChange = {},
            onSearch = {},
            onAdvancedSearch = {},
            onSearchInFavorites = {},
            onSearchInBookmarks = {},
            onSearchInNotes = {},
            onClearSearch = {},
            onAyaClick = {},
            onBookmarkToggle = {},
            onFavoriteToggle = {}
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun QuranSearchScreenPreview_Dark() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(color = MaterialTheme.colorScheme.background) {
            QuranSearchScreen(
                searchQuery = "",
                searchResults = emptyList(),
                searchLanguage = "All",
                searchFilters = QuranViewModel.SearchFilters(),
                isLoading = false,
                error = "",
                onSearchQueryChange = {},
                onSearchLanguageChange = {},
                onSearchFiltersChange = {},
                onSearch = {},
                onAdvancedSearch = {},
                onSearchInFavorites = {},
                onSearchInBookmarks = {},
                onSearchInNotes = {},
                onClearSearch = {},
                onAyaClick = {},
                onBookmarkToggle = {},
                onFavoriteToggle = {}
            )
        }
    }
}