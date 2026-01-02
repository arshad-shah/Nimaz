package com.arshadshah.nimaz.ui.screens.quran

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardReturn
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.data.local.models.LocalAya
import com.arshadshah.nimaz.ui.components.common.NoResultFound
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont
import com.arshadshah.nimaz.utils.StringUtils.cleanTextFromBackslash
import com.arshadshah.nimaz.viewModel.QuranViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    var isSearchFocused by remember { mutableStateOf(false) }
    var showLanguageDropdown by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Search Bar Component
        SearchBarCard(
            searchQuery = searchQuery,
            searchLanguage = searchLanguage,
            searchFilters = searchFilters,
            isSearchFocused = isSearchFocused,
            showLanguageDropdown = showLanguageDropdown,
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
            onSearchInNotes = {
                onSearchInNotes(it)
                focusManager.clearFocus()
            },
            onClearSearch = onClearSearch,
            onSearchFocusChange = { isSearchFocused = it },
            onLanguageDropdownChange = { showLanguageDropdown = it },
            onFiltersToggle = { showFilters = !showFilters }
        )

        // Search Results Component
        SearchResultsSection(
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
private fun SearchBarCard(
    searchQuery: String,
    searchLanguage: String,
    searchFilters: QuranViewModel.SearchFilters,
    isSearchFocused: Boolean,
    showLanguageDropdown: Boolean,
    showFilters: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onSearchLanguageChange: (String) -> Unit,
    onSearchFiltersChange: (QuranViewModel.SearchFilters) -> Unit,
    onSearch: (String) -> Unit,
    onAdvancedSearch: () -> Unit,
    onSearchInFavorites: (String) -> Unit,
    onSearchInBookmarks: (String) -> Unit,
    onSearchInNotes: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSearchFocusChange: (Boolean) -> Unit,
    onLanguageDropdownChange: (Boolean) -> Unit,
    onFiltersToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Main Search Input
            MainSearchInput(
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                onSearch = onSearch,
                onClearSearch = onClearSearch,
                onFocusChange = onSearchFocusChange
            )

            // Filter Chips Row
            FilterChipsRow(
                searchLanguage = searchLanguage,
                searchQuery = searchQuery,
                searchFilters = searchFilters,
                onSearchLanguageChange = onSearchLanguageChange,
                onSearchInFavorites = onSearchInFavorites,
                onSearchInBookmarks = onSearchInBookmarks,
                onSearchInNotes = onSearchInNotes,
                onSearch = onSearch,
                onFiltersToggle = onFiltersToggle,
                showFilters = showFilters
            )

            // Advanced Filters Panel
            AdvancedFiltersPanel(
                showFilters = showFilters,
                searchFilters = searchFilters,
                searchQuery = searchQuery,
                onSearchFiltersChange = onSearchFiltersChange,
                onAdvancedSearch = onAdvancedSearch,
                onSearch = onSearch
            )
        }
    }
}

@Composable
private fun MainSearchInput(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onClearSearch: () -> Unit,
    onFocusChange: (Boolean) -> Unit
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { onFocusChange(it.isFocused) },
        placeholder = {
            Text(
                "Search in Quran...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        },
        leadingIcon = {
            Icon(
                Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingIcon = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                AnimatedVisibility(
                    visible = searchQuery.isNotEmpty(),
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    IconButton(
                        onClick = onClearSearch,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Clear,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (searchQuery.isNotEmpty()) {
                    FilledIconButton(
                        onClick = { onSearch(searchQuery) },
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardReturn,
                            contentDescription = "Search",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch(searchQuery) }),
        shape = RoundedCornerShape(24.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    )
}

@Composable
private fun FilterChipsRow(
    searchLanguage: String,
    searchQuery: String,
    searchFilters: QuranViewModel.SearchFilters,
    onSearchLanguageChange: (String) -> Unit,
    onSearchInFavorites: (String) -> Unit,
    onSearchInBookmarks: (String) -> Unit,
    onSearchInNotes: (String) -> Unit,
    onSearch: (String) -> Unit,
    onFiltersToggle: () -> Unit,
    showFilters: Boolean
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        // Advanced Filters Toggle
        item {
            FilterChip(
                selected = showFilters || hasActiveFilters(searchFilters),
                onClick = onFiltersToggle,
                label = { Text("Filters") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Notes,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                trailingIcon = {
                    if (hasActiveFilters(searchFilters)) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            )
        }

        // Language Filter
        item {
            var expanded by remember { mutableStateOf(false) }
            Box {
                FilterChip(
                    selected = searchLanguage !in listOf("All", "Favorites", "Bookmarks"),
                    onClick = { expanded = true },
                    label = { Text(if (searchLanguage in listOf("Favorites", "Bookmarks", "Notes")) "All" else searchLanguage) },
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
                            }
                        )
                    }
                }
            }
        }

        // Favorites Quick Access
        item {
            FilterChip(
                selected = searchLanguage == "Favorites",
                onClick = {
                    val newLang = if (searchLanguage == "Favorites") "All" else "Favorites"
                    onSearchLanguageChange(newLang)
                    if (searchQuery.isNotEmpty()) {
                        if (newLang == "Favorites") onSearchInFavorites(searchQuery) else onSearch(searchQuery)
                    }
                },
                label = { Text("Favorites") },
                leadingIcon = {
                    Icon(
                        if (searchLanguage == "Favorites") Icons.Default.Favorite else Icons.Default.FavoriteBorder,
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

        // Bookmarks Quick Access
        item {
            FilterChip(
                selected = searchLanguage == "Bookmarks",
                onClick = {
                    val newLang = if (searchLanguage == "Bookmarks") "All" else "Bookmarks"
                    onSearchLanguageChange(newLang)
                    if (searchQuery.isNotEmpty()) {
                        if (newLang == "Bookmarks") onSearchInBookmarks(searchQuery) else onSearch(searchQuery)
                    }
                },
                label = { Text("Bookmarks") },
                leadingIcon = {
                    Icon(
                        if (searchLanguage == "Bookmarks") Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
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

private fun hasActiveFilters(filters: QuranViewModel.SearchFilters): Boolean {
    return filters.surahNumber != null || 
           filters.juzNumber != null || 
           filters.isFavorite == true || 
           filters.isBookmarked == true || 
           filters.hasNote == true
}

@Composable
private fun AdvancedFiltersPanel(
    showFilters: Boolean,
    searchFilters: QuranViewModel.SearchFilters,
    searchQuery: String,
    onSearchFiltersChange: (QuranViewModel.SearchFilters) -> Unit,
    onAdvancedSearch: () -> Unit,
    onSearch: (String) -> Unit
) {
    AnimatedVisibility(
        visible = showFilters,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Filter Results By:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        onClick = {
                            val newValue = if (searchFilters.isFavorite == true) null else true
                            onSearchFiltersChange(searchFilters.copy(isFavorite = newValue))
                            if (searchQuery.isNotEmpty()) onAdvancedSearch()
                        },
                        label = { Text("Favorites Only") },
                        selected = searchFilters.isFavorite == true,
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        onClick = {
                            val newValue = if (searchFilters.isBookmarked == true) null else true
                            onSearchFiltersChange(searchFilters.copy(isBookmarked = newValue))
                            if (searchQuery.isNotEmpty()) onAdvancedSearch()
                        },
                        label = { Text("Bookmarked") },
                        selected = searchFilters.isBookmarked == true,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        onClick = {
                            val newValue = if (searchFilters.hasNote == true) null else true
                            onSearchFiltersChange(searchFilters.copy(hasNote = newValue))
                            if (searchQuery.isNotEmpty()) onAdvancedSearch()
                        },
                        label = { Text("Has Notes") },
                        selected = searchFilters.hasNote == true,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                }

                if (hasActiveFilters(searchFilters)) {
                    Text(
                        "Clear All Filters",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.End)
                            .clickable {
                                onSearchFiltersChange(QuranViewModel.SearchFilters())
                                if (searchQuery.isNotEmpty()) onSearch(searchQuery)
                            }
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultsSection(
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
        label = "contentTransition"
    ) { (isEmpty, hasQuery, loading) ->
        when {
            loading -> LoadingState()
            error.isNotEmpty() -> ErrorState(error = error)
            isEmpty && hasQuery -> EmptyResultsState()
            !hasQuery -> EmptySearchState()
            else -> {
                SearchResultsList(
                    searchResults = searchResults,
                    searchQuery = searchQuery,
                    onAyaClick = onAyaClick,
                    onBookmarkToggle = onBookmarkToggle,
                    onFavoriteToggle = onFavoriteToggle
                )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.compose.material3.CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Searching...", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ErrorState(error: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun EmptyResultsState() {
    NoResultFound(
        title = "No Matches Found",
        subtitle = "Try adjusting your search terms or filters"
    )
}

@Composable
private fun EmptySearchState() {
    NoResultFound(
        title = "Search the Quran",
        subtitle = "Find Ayahs by Arabic text, translation, or keywords"
    )
}

@Composable
private fun SearchResultsList(
    searchResults: List<LocalAya>,
    searchQuery: String,
    onAyaClick: (LocalAya) -> Unit,
    onBookmarkToggle: (LocalAya) -> Unit,
    onFavoriteToggle: (LocalAya) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "${searchResults.size} result${if (searchResults.size != 1) "s" else ""} found",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
        }

        items(
            items = searchResults,
            key = { "${it.suraNumber}-${it.ayaNumberInSurah}" }
        ) { aya ->
            AyaSearchCard(
                aya = aya,
                searchQuery = searchQuery,
                onClick = { onAyaClick(aya) },
                onBookmarkToggle = { onBookmarkToggle(aya) },
                onFavoriteToggle = { onFavoriteToggle(aya) },
                modifier = Modifier.animateItem()
            )
        }
    }
}

@Composable
private fun AyaSearchCard(
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
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with Surah info and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Surah ${aya.suraNumber} : ${aya.ayaNumberInSurah}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }

                Row {
                    IconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (aya.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (aya.favorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = onBookmarkToggle,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (aya.bookmark) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (aya.bookmark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Arabic Text
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = aya.ayaArabic.cleanTextFromBackslash(),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = utmaniQuranFont,
                        lineHeight = 44.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // English Translation with Highlight
            Text(
                text = getHighlightedAnnotatedString(
                    text = aya.translationEnglish.cleanTextFromBackslash(),
                    query = searchQuery,
                    color = MaterialTheme.colorScheme.primary
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                lineHeight = 20.sp
            )

            // Show Urdu Translation if searched or if it matches query
            if (searchQuery.isNotEmpty() && aya.translationUrdu.contains(searchQuery, ignoreCase = true)) {
                 Spacer(modifier = Modifier.height(8.dp))
                 CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                     Text(
                         text = getHighlightedAnnotatedString(
                             text = aya.translationUrdu.cleanTextFromBackslash(),
                             query = searchQuery,
                             color = MaterialTheme.colorScheme.primary
                         ),
                         style = MaterialTheme.typography.bodyMedium,
                         color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                         textAlign = TextAlign.Start,
                         modifier = Modifier.fillMaxWidth()
                     )
                 }
            }

            // Note Section
            if (aya.note.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(8.dp)) {
                        Icon(
                            Icons.Default.Notes,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp).padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = getHighlightedAnnotatedString(
                                text = aya.note,
                                query = searchQuery,
                                color = MaterialTheme.colorScheme.tertiary
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

fun getHighlightedAnnotatedString(text: String, query: String, color: Color): androidx.compose.ui.text.AnnotatedString {
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
                background = color.copy(alpha = 0.2f),
                fontWeight = FontWeight.Bold,
                color = color.copy(alpha = 1f) // Make text color match highlight for visibility
            ),
            start = index,
            end = index + query.length
        )
        startIndex = index + query.length
    }
    
    return builder.toAnnotatedString()
}
