package com.arshadshah.nimaz.presentation.screens.bookmarks

import android.content.Intent
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
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
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tabs
                item {
                    TabRow(
                        selectedFilter = state.selectedFilter,
                        onFilterSelected = { viewModel.onEvent(BookmarksEvent.SetFilter(it)) }
                    )
                }

                // Stats Row
                item {
                    StatsRow(
                        quranCount = statsState.quranCount,
                        hadithCount = statsState.hadithCount,
                        duaCount = statsState.duaCount
                    )
                }

                // Bookmark Cards
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
                                BookmarkType.QURAN -> {
                                    val ayahId = bookmark.id.removePrefix("quran_").toIntOrNull()
                                    if (ayahId != null) {
                                        viewModel.onEvent(BookmarksEvent.DeleteQuranBookmark(ayahId))
                                    }
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

                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun TabRow(
    selectedFilter: BookmarkType?,
    onFilterSelected: (BookmarkType?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TabChip(
            label = "All",
            isSelected = selectedFilter == null,
            onClick = { onFilterSelected(null) }
        )
        BookmarkType.entries.forEach { type ->
            TabChip(
                label = type.name.lowercase().replaceFirstChar { it.uppercase() },
                isSelected = selectedFilter == type,
                onClick = { onFilterSelected(type) }
            )
        }
    }
}

@Composable
private fun TabChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(25.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainer,
        border = null
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatsRow(
    quranCount: Int,
    hadithCount: Int,
    duaCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            value = quranCount.toString(),
            label = "Quran Verses",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            value = hadithCount.toString(),
            label = "Hadith",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            value = duaCount.toString(),
            label = "Duas",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BookmarkCard(
    bookmark: UnifiedBookmark,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val typeLabel = when (bookmark.type) {
        BookmarkType.QURAN -> "Quran"
        BookmarkType.HADITH -> "Hadith"
        BookmarkType.DUA -> "Dua"
    }

    val typeEmoji = when (bookmark.type) {
        BookmarkType.QURAN -> "\uD83D\uDCD6"
        BookmarkType.HADITH -> "\uD83D\uDCDC"
        BookmarkType.DUA -> "\uD83E\uDD32"
    }

    val typeColor = when (bookmark.type) {
        BookmarkType.QURAN -> MaterialTheme.colorScheme.primary
        BookmarkType.HADITH -> MaterialTheme.colorScheme.tertiary
        BookmarkType.DUA -> MaterialTheme.colorScheme.secondary
    }

    val typeBgColor = when (bookmark.type) {
        BookmarkType.QURAN -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        BookmarkType.HADITH -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
        BookmarkType.DUA -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            // Header row: icon, title/meta, actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(typeBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = typeEmoji,
                        fontSize = 20.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title and meta
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bookmark.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = bookmark.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Action buttons
                IconButton(
                    onClick = {
                        val textToShare = buildString {
                            append(bookmark.title)
                            bookmark.arabicText?.let { append("\n\n$it") }
                            bookmark.note?.let { append("\n\n$it") }
                        }
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, textToShare)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share"))
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Preview section: Arabic text and translation
            if (bookmark.arabicText != null || bookmark.note != null) {
                Column(
                    modifier = Modifier.padding(start = 15.dp, end = 15.dp, bottom = 15.dp)
                ) {
                    bookmark.arabicText?.let { arabic ->
                        Text(
                            text = arabic,
                            style = MaterialTheme.typography.bodyLarge,
                            fontSize = 18.sp,
                            lineHeight = 32.sp,
                            textAlign = TextAlign.End,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    bookmark.note?.let { translation ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = translation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Footer: date and type badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 15.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Added ${
                        DateUtils.getRelativeTimeSpanString(
                            bookmark.createdAt,
                            System.currentTimeMillis(),
                            DateUtils.DAY_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_RELATIVE
                        )
                    }",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = typeBgColor
                ) {
                    Text(
                        text = typeLabel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = typeColor
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
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(20.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Bookmark,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "No Bookmarks Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Save ayahs, hadiths, and duas for quick access",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
