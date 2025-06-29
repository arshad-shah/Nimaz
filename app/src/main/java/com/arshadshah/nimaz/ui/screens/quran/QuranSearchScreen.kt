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
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardReturn
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ExpandMore
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
            onSearch = onSearch,
            onAdvancedSearch = onAdvancedSearch,
            onSearchInFavorites = onSearchInFavorites,
            onSearchInBookmarks = onSearchInBookmarks,
            onSearchInNotes = onSearchInNotes,
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
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .scale(
                animateFloatAsState(
                    targetValue = if (isSearchFocused) 1.02f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "searchScale"
                ).value
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (isSearchFocused) 8.dp else 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // Main Search Input
            MainSearchInput(
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                onSearch = onSearch,
                onClearSearch = onClearSearch,
                onFocusChange = onSearchFocusChange
            )

            // Filter Controls
            FilterControlsRow(
                searchLanguage = searchLanguage,
                searchQuery = searchQuery,
                showLanguageDropdown = showLanguageDropdown,
                onSearchLanguageChange = onSearchLanguageChange,
                onSearchInFavorites = onSearchInFavorites,
                onSearchInBookmarks = onSearchInBookmarks,
                onSearchInNotes = onSearchInNotes,
                onSearch = onSearch,
                onLanguageDropdownChange = onLanguageDropdownChange,
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

@Preview
@Composable
fun SearchBarCardPreview() {
    // Sample data for preview
    val sampleSearchQuery = "Allah"
    val sampleSearchLanguage = "Arabic"
    val sampleSearchFilters = QuranViewModel.SearchFilters()

    SearchBarCard(
        searchQuery = sampleSearchQuery,
        searchLanguage = sampleSearchLanguage,
        searchFilters = sampleSearchFilters,
        isSearchFocused = false,
        showLanguageDropdown = false,
        showFilters = false,
        onSearchQueryChange = {},
        onSearchLanguageChange = {},
        onSearchFiltersChange = {},
        onSearch = {},
        onAdvancedSearch = {},
        onSearchInFavorites = {},
        onSearchInBookmarks = {},
        onSearchInNotes = {},
        onClearSearch = {},
        onSearchFocusChange = {},
        onLanguageDropdownChange = {},
        onFiltersToggle = {}
    )
}

@Composable
private fun MainSearchInput(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onClearSearch: () -> Unit,
    onFocusChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = when {
                    searchQuery.isNotEmpty() -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                },
                shape = RoundedCornerShape(16.dp)
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { onFocusChange(it.isFocused) },
            placeholder = {
                Text(
                    "Search Quran...",
                    style = MaterialTheme.typography.bodyLarge
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
                AnimatedVisibility(
                    visible = searchQuery.isNotEmpty(),
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    IconButton(
                        onClick = onClearSearch,
                        modifier = Modifier.scale(0.8f)
                    ) {
                        Icon(
                            Icons.Rounded.Clear,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            shape = RoundedCornerShape(24.dp)
        )

        FilledIconButton(
            onClick = { onSearch(searchQuery) },
            modifier = Modifier.scale(0.9f),
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

@Composable
private fun FilterControlsRow(
    searchLanguage: String,
    searchQuery: String,
    showLanguageDropdown: Boolean,
    onSearchLanguageChange: (String) -> Unit,
    onSearchInFavorites: (String) -> Unit,
    onSearchInBookmarks: (String) -> Unit,
    onSearchInNotes: (String) -> Unit,
    onSearch: (String) -> Unit,
    onLanguageDropdownChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
            // Language Selector
            LanguageSelector(
                searchLanguage = searchLanguage,
                searchQuery = searchQuery,
                showLanguageDropdown = showLanguageDropdown,
                onSearchLanguageChange = onSearchLanguageChange,
                onSearchInFavorites = onSearchInFavorites,
                onSearchInBookmarks = onSearchInBookmarks,
                onSearchInNotes = onSearchInNotes,
                onSearch = onSearch,
                onLanguageDropdownChange = onLanguageDropdownChange
            )



            QuickAccessChips(
                searchLanguage = searchLanguage,
                searchQuery = searchQuery,
                onSearchInFavorites = onSearchInFavorites,
                onSearchInBookmarks = onSearchInBookmarks
            )

    }
}

@Composable
private fun LanguageSelector(
    searchLanguage: String,
    searchQuery: String,
    showLanguageDropdown: Boolean,
    onSearchLanguageChange: (String) -> Unit,
    onSearchInFavorites: (String) -> Unit,
    onSearchInBookmarks: (String) -> Unit,
    onSearchInNotes: (String) -> Unit,
    onSearch: (String) -> Unit,
    onLanguageDropdownChange: (Boolean) -> Unit
) {
    Box {
        FilterChip(
            onClick = { onLanguageDropdownChange(true) },
            label = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Language,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(searchLanguage)
                    Icon(
                        Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            },
            selected = searchLanguage != "All",
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        DropdownMenu(
            expanded = showLanguageDropdown,
            onDismissRequest = { onLanguageDropdownChange(false) }
        ) {
            listOf("All", "Arabic", "English", "Urdu", "Favorites", "Bookmarks", "Notes").forEach { language ->
                DropdownMenuItem(
                    text = { Text(language) },
                    onClick = {
                        onSearchLanguageChange(language)
                        onLanguageDropdownChange(false)
                        if (searchQuery.isNotEmpty()) {
                            when (language) {
                                "Favorites" -> onSearchInFavorites(searchQuery)
                                "Bookmarks" -> onSearchInBookmarks(searchQuery)
                                "Notes" -> onSearchInNotes(searchQuery)
                                else -> onSearch(searchQuery)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun QuickAccessChips(
    searchLanguage: String,
    searchQuery: String,
    onSearchInFavorites: (String) -> Unit,
    onSearchInBookmarks: (String) -> Unit
) {
        FilterChip(
            onClick = {
                if (searchQuery.isNotEmpty()) onSearchInFavorites(searchQuery)
            },
            label = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Favorites")
                }
            },
            selected = searchLanguage == "Favorites",
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.errorContainer
            )
        )

        FilterChip(
            onClick = {
                if (searchQuery.isNotEmpty()) onSearchInBookmarks(searchQuery)
            },
            label = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Bookmark,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bookmarks")
                }
            },
            selected = searchLanguage == "Bookmarks",
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        )
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
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Advanced Filters",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        onClick = {
                            onSearchFiltersChange(
                                searchFilters.copy(
                                    isFavorite = if (searchFilters.isFavorite == true) null else true
                                )
                            )
                            if (searchQuery.isNotEmpty()) onAdvancedSearch()
                        },
                        label = { Text("Favorites Only") },
                        selected = searchFilters.isFavorite == true,
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        onClick = {
                            onSearchFiltersChange(
                                searchFilters.copy(
                                    isBookmarked = if (searchFilters.isBookmarked == true) null else true
                                )
                            )
                            if (searchQuery.isNotEmpty()) onAdvancedSearch()
                        },
                        label = { Text("Bookmarked Only") },
                        selected = searchFilters.isBookmarked == true,
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        onClick = {
                            onSearchFiltersChange(
                                searchFilters.copy(
                                    hasNote = if (searchFilters.hasNote == true) null else true
                                )
                            )
                            if (searchQuery.isNotEmpty()) onAdvancedSearch()
                        },
                        label = { Text("With Notes") },
                        selected = searchFilters.hasNote == true,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (searchFilters.surahNumber != null ||
                    searchFilters.juzNumber != null ||
                    searchFilters.isFavorite != null ||
                    searchFilters.isBookmarked != null ||
                    searchFilters.hasNote != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            "Clear Filters",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                onSearchFiltersChange(QuranViewModel.SearchFilters())
                                if (searchQuery.isNotEmpty()) onSearch(searchQuery)
                            }
                        )
                    }
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
            fadeIn() + slideInVertically() togetherWith fadeOut() + slideOutVertically()
        },
        label = "contentTransition"
    ) { (isEmpty, hasQuery, loading) ->
        when {
            loading -> {
                LoadingState()
            }

            error.isNotEmpty() -> {
                ErrorState(error = error)
            }

            isEmpty && hasQuery -> {
                EmptyResultsState()
            }

            !hasQuery -> {
                EmptySearchState()
            }

            else -> {
                SearchResultsList(
                    searchResults = searchResults,
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
        Text("Searching...")
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
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EmptyResultsState() {
    NoResultFound(
        title = "No Ayahs Found",
        subtitle = "Try different search terms or change language filter"
    )
}

@Composable
private fun EmptySearchState() {
    NoResultFound(
        title = "Search the Holy Quran",
        subtitle = "Search across Arabic text, translations, and your personal collection"
    )
}

@Composable
private fun SearchResultsList(
    searchResults: List<LocalAya>,
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
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        items(
            items = searchResults,
            key = { "${it.suraNumber}-${it.ayaNumberInSurah}" }
        ) { aya ->
            AyaSearchCard(
                aya = aya,
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
    onClick: () -> Unit,
    onBookmarkToggle: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Surah and Aya info
            AyaCardHeader(
                aya = aya,
                onBookmarkToggle = onBookmarkToggle,
                onFavoriteToggle = onFavoriteToggle
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Arabic Text
            AyaArabicText(aya = aya)

            Spacer(modifier = Modifier.height(8.dp))

            // English Translation
            AyaEnglishText(aya = aya)

            // Show note if exists
            AyaNoteSection(aya = aya)
        }
    }
}

@Composable
private fun AyaCardHeader(
    aya: LocalAya,
    onBookmarkToggle: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Surah ${aya.suraNumber} • Ayah ${aya.ayaNumberInSurah}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

//        Row {
//            IconButton(
//                onClick = onBookmarkToggle,
//                modifier = Modifier.scale(0.8f)
//            ) {
//                Icon(
//                    imageVector = if (aya.bookmark) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
//                    contentDescription = "Toggle bookmark",
//                    tint = if (aya.bookmark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
//                )
//            }
//
//            IconButton(
//                onClick = onFavoriteToggle,
//                modifier = Modifier.scale(0.8f)
//            ) {
//                Icon(
//                    imageVector = if (aya.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
//                    contentDescription = "Toggle favorite",
//                    tint = if (aya.favorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
//                )
//            }
//        }
    }
}

@Composable
private fun AyaArabicText(aya: LocalAya) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = aya.ayaArabic.cleanTextFromBackslash(),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = utmaniQuranFont,
                    fontSize = 24.sp,
                    lineHeight = 40.sp,
                    textAlign = TextAlign.Start
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                softWrap = true,
                overflow = TextOverflow.Visible
            )
        }
    }
}

@Composable
private fun AyaEnglishText(aya: LocalAya) {
    Text(
        text = aya.translationEnglish.cleanTextFromBackslash(),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun AyaNoteSection(aya: LocalAya) {
    if (aya.note.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Note: ${aya.note}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(8.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}