package com.arshadshah.nimaz.presentation.screens.search

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.molecules.NimazEmptyState
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.components.organisms.NimazSearchBar
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.viewmodel.SearchEvent
import com.arshadshah.nimaz.presentation.viewmodel.SearchFilter
import com.arshadshah.nimaz.presentation.viewmodel.SearchViewModel
import com.arshadshah.nimaz.presentation.viewmodel.UnifiedSearchResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onNavigateToQuranAyah: (Int, Int) -> Unit,
    onNavigateToSurah: (Int) -> Unit,
    onNavigateToHadith: (String, String) -> Unit,
    onNavigateToDua: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.searchState.collectAsState()
    val statsState by viewModel.statsState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.search_title),
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search Bar
            item {
                NimazSearchBar(
                    query = state.query,
                    onQueryChange = { viewModel.onEvent(SearchEvent.UpdateQuery(it)) },
                    placeholder = stringResource(R.string.search_placeholder),
                    modifier = Modifier.fillMaxWidth(),
                    showClearButton = state.query.isNotEmpty(),
                    onClear = { viewModel.onEvent(SearchEvent.ClearSearch) },
                    onSearch = { viewModel.onEvent(SearchEvent.ExecuteSearch) }
                )
            }

            // Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SearchFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = state.selectedFilter == filter,
                            onClick = { viewModel.onEvent(SearchEvent.SetFilter(filter)) },
                            label = {
                                Text(
                                    when (filter) {
                                        SearchFilter.ALL -> stringResource(R.string.all)
                                        SearchFilter.QURAN -> stringResource(R.string.quran)
                                        SearchFilter.HADITH -> stringResource(R.string.hadith)
                                        SearchFilter.DUA -> stringResource(R.string.duas)
                                    }
                                )
                            }
                        )
                    }
                }
            }

            // Recent Searches (when not searching)
            if (state.query.isEmpty() && state.recentSearches.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.recent_searches),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { viewModel.onEvent(SearchEvent.ClearRecentSearches) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.clear_recent),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                items(state.recentSearches) { recentSearch ->
                    RecentSearchItem(
                        query = recentSearch,
                        onClick = { viewModel.onEvent(SearchEvent.SelectRecentSearch(recentSearch)) },
                        onRemove = { viewModel.onEvent(SearchEvent.RemoveRecentSearch(recentSearch)) }
                    )
                }
            }

            // Loading Indicator
            if (state.isSearching) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            // Results Summary
            if (state.query.isNotEmpty() && !state.isSearching && state.filteredResults.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.results_found_format, statsState.totalResults),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Search Results
            items(state.filteredResults) { result ->
                when (result) {
                    is UnifiedSearchResult.QuranResult -> QuranSearchResultCard(
                        result = result.result,
                        query = state.query,
                        onClick = { onNavigateToQuranAyah(result.result.ayah.surahNumber, result.result.ayah.ayahNumber) }
                    )
                    is UnifiedSearchResult.SurahResult -> SurahSearchResultCard(
                        surah = result.surah,
                        onClick = { onNavigateToSurah(result.surah.number) }
                    )
                    is UnifiedSearchResult.HadithResult -> HadithSearchResultCard(
                        result = result.result,
                        query = state.query,
                        onClick = { onNavigateToHadith(result.result.hadith.bookId, result.result.hadith.id) }
                    )
                    is UnifiedSearchResult.DuaResult -> DuaSearchResultCard(
                        result = result.result,
                        query = state.query,
                        onClick = { onNavigateToDua(result.result.dua.id) }
                    )
                }
            }

            // No Results
            if (state.query.isNotEmpty() && !state.isSearching && state.filteredResults.isEmpty()) {
                item {
                    NimazEmptyState(
                        title = stringResource(R.string.no_results_format, state.query),
                        message = stringResource(R.string.no_results_hint),
                        icon = Icons.Default.Search,
                        iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecentSearchItem(
    query: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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
                text = query,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.remove),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuranSearchResultCard(
    result: com.arshadshah.nimaz.domain.model.QuranSearchResult,
    query: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SearchResultCard(
        icon = Icons.Default.MenuBook,
        iconColor = NimazColors.QuranColors.Meccan,
        type = stringResource(R.string.quran_type),
        title = stringResource(R.string.surah_result_format, result.ayah.surahNumber, result.ayah.ayahNumber),
        subtitle = result.surahName,
        highlightedText = result.matchedText,
        query = query,
        onClick = onClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SurahSearchResultCard(
    surah: com.arshadshah.nimaz.domain.model.Surah,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(NimazColors.QuranColors.Meccan.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = surah.number.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = NimazColors.QuranColors.Meccan
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = surah.nameEnglish,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.surah_ayahs_format, surah.nameTransliteration, surah.numberOfAyahs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ArabicText(
                text = surah.nameArabic,
                size = ArabicTextSize.SMALL,
                color = NimazColors.QuranColors.Meccan
            )
        }
    }
}

@Composable
private fun HadithSearchResultCard(
    result: com.arshadshah.nimaz.domain.model.HadithSearchResult,
    query: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SearchResultCard(
        icon = Icons.Default.Book,
        iconColor = MaterialTheme.colorScheme.primary,
        type = stringResource(R.string.hadith_type),
        title = stringResource(R.string.hadith_result_format, result.hadith.hadithNumber),
        subtitle = result.bookName,
        highlightedText = result.matchedText,
        query = query,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
private fun DuaSearchResultCard(
    result: com.arshadshah.nimaz.domain.model.DuaSearchResult,
    query: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SearchResultCard(
        icon = Icons.Default.Mosque,
        iconColor = MaterialTheme.colorScheme.secondary,
        type = stringResource(R.string.dua_type),
        title = result.dua.titleEnglish,
        subtitle = result.categoryName,
        highlightedText = result.matchedText,
        query = query,
        onClick = onClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchResultCard(
    icon: ImageVector,
    iconColor: Color,
    type: String,
    title: String,
    subtitle: String,
    highlightedText: String,
    query: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = iconColor.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = type,
                            style = MaterialTheme.typography.labelSmall,
                            color = iconColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                HighlightedText(
                    text = highlightedText,
                    query = query,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun HighlightedText(
    text: String,
    query: String,
    style: androidx.compose.ui.text.TextStyle,
    maxLines: Int = Int.MAX_VALUE
) {
    val annotatedString = buildAnnotatedString {
        val lowerText = text.lowercase()
        val lowerQuery = query.lowercase()
        var startIndex = 0

        while (true) {
            val index = lowerText.indexOf(lowerQuery, startIndex)
            if (index < 0) {
                append(text.substring(startIndex))
                break
            }

            append(text.substring(startIndex, index))
            withStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    background = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
            ) {
                append(text.substring(index, index + query.length))
            }
            startIndex = index + query.length
        }
    }

    Text(
        text = annotatedString,
        style = style,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

