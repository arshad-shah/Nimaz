package com.arshadshah.nimaz.presentation.screens.bookmarks

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.components.organisms.NimazSearchBar
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.viewmodel.BookmarkSortOrder
import com.arshadshah.nimaz.presentation.viewmodel.BookmarkType
import com.arshadshah.nimaz.presentation.viewmodel.BookmarksEvent
import com.arshadshah.nimaz.presentation.viewmodel.BookmarksViewModel
import com.arshadshah.nimaz.presentation.viewmodel.UnifiedBookmark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    onNavigateBack: () -> Unit,
    onNavigateToQuranAyah: (Int, Int) -> Unit,
    onNavigateToHadith: (String, Int) -> Unit,
    onNavigateToDua: (String) -> Unit,
    viewModel: BookmarksViewModel = hiltViewModel()
) {
    val state by viewModel.bookmarksState.collectAsState()
    val statsState by viewModel.statsState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = "Bookmarks",
                subtitle = "${statsState.totalBookmarks} items",
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        if (state.allBookmarks.isEmpty() && !state.isLoading) {
            EmptyBookmarksState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
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
                        query = state.searchQuery,
                        onQueryChange = { viewModel.onEvent(BookmarksEvent.Search(it)) },
                        placeholder = "Search bookmarks...",
                        modifier = Modifier.fillMaxWidth(),
                        showClearButton = state.searchQuery.isNotEmpty(),
                        onClear = { viewModel.onEvent(BookmarksEvent.ClearSearch) }
                    )
                }

                // Stats Summary
                item {
                    BookmarkStatsRow(
                        quranCount = statsState.quranCount,
                        hadithCount = statsState.hadithCount,
                        duaCount = statsState.duaCount
                    )
                }

                // Filter Chips
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = state.selectedFilter == null,
                            onClick = { viewModel.onEvent(BookmarksEvent.SetFilter(null)) },
                            label = { Text("All") }
                        )
                        BookmarkType.entries.forEach { type ->
                            FilterChip(
                                selected = state.selectedFilter == type,
                                onClick = { viewModel.onEvent(BookmarksEvent.SetFilter(type)) },
                                label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                }

                // Bookmarks List
                items(
                    items = state.filteredBookmarks,
                    key = { it.id }
                ) { bookmark ->
                    BookmarkCard(
                        bookmark = bookmark,
                        onClick = {
                            when (bookmark.type) {
                                BookmarkType.QURAN -> bookmark.surahNumber?.let { surah ->
                                    bookmark.ayahNumber?.let { ayah ->
                                        onNavigateToQuranAyah(surah, ayah)
                                    }
                                }
                                BookmarkType.HADITH -> bookmark.hadithBookId?.let { bookId ->
                                    bookmark.hadithNumber?.let { number ->
                                        onNavigateToHadith(bookId, number)
                                    }
                                }
                                BookmarkType.DUA -> bookmark.duaId?.let { duaId ->
                                    onNavigateToDua(duaId)
                                }
                            }
                        },
                        onDelete = {
                            when (bookmark.type) {
                                BookmarkType.QURAN -> bookmark.surahNumber?.let {
                                    // Need ayahId for deletion
                                }
                                BookmarkType.HADITH -> bookmark.hadithBookId?.let {
                                    viewModel.onEvent(BookmarksEvent.DeleteHadithBookmark(bookmark.id.removePrefix("hadith_")))
                                }
                                BookmarkType.DUA -> bookmark.duaId?.let {
                                    viewModel.onEvent(BookmarksEvent.DeleteDuaBookmark(it))
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyBookmarksState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Bookmark,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Bookmarks Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Save ayahs, hadiths, and duas for quick access",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BookmarkStatsRow(
    quranCount: Int,
    hadithCount: Int,
    duaCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatChip(
            icon = Icons.Default.MenuBook,
            label = "Quran",
            count = quranCount,
            color = NimazColors.QuranColors.Meccan,
            modifier = Modifier.weight(1f)
        )
        StatChip(
            icon = Icons.Default.Book,
            label = "Hadith",
            count = hadithCount,
            color = NimazColors.Primary,
            modifier = Modifier.weight(1f)
        )
        StatChip(
            icon = Icons.Default.Mosque,
            label = "Dua",
            count = duaCount,
            color = NimazColors.Secondary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookmarkCard(
    bookmark: UnifiedBookmark,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, color) = when (bookmark.type) {
        BookmarkType.QURAN -> Icons.Default.MenuBook to NimazColors.QuranColors.Meccan
        BookmarkType.HADITH -> Icons.Default.Book to NimazColors.Primary
        BookmarkType.DUA -> Icons.Default.Mosque to NimazColors.Secondary
    }

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
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.padding(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bookmark.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = bookmark.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = color
                )
                bookmark.note?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
