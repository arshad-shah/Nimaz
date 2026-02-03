package com.arshadshah.nimaz.presentation.screens.quran

import com.arshadshah.nimaz.data.local.database.dao.PageAyahRange
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material.icons.filled.CheckCircle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.Khatam
import com.arshadshah.nimaz.domain.model.KhatamConstants
import com.arshadshah.nimaz.domain.model.QuranBookmark
import com.arshadshah.nimaz.domain.model.QuranFavorite
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.organisms.NimazPillTabs
import com.arshadshah.nimaz.presentation.components.organisms.NimazTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.QuranEvent
import com.arshadshah.nimaz.presentation.viewmodel.QuranViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranHomeScreen(
    onNavigateToSurah: (Int) -> Unit,
    onNavigateToJuz: (Int) -> Unit,
    onNavigateToPage: (Int) -> Unit,
    onNavigateToBookmarks: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToSurahInfo: (Int) -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToQuranAyah: (Int, Int) -> Unit = { surah, ayah -> onNavigateToSurah(surah) },
    onNavigateToKhatam: () -> Unit = {},
    onNavigateToKhatamDetail: (Long) -> Unit = {},
    viewModel: QuranViewModel = hiltViewModel()
) {
    val state by viewModel.homeState.collectAsState()
    val bookmarksState by viewModel.bookmarksState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazTopAppBar(
                title = stringResource(R.string.quran_home_title),
                scrollBehavior = scrollBehavior,
                actions = {
                    // Search icon
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.quran_home_search)
                        )
                    }
                    // Bookmarks icon with badge - navigates to dedicated bookmarks screen
                    IconButton(onClick = onNavigateToBookmarks) {
                        BadgedBox(
                            badge = {
                                if (bookmarksState.bookmarks.isNotEmpty()) {
                                    Badge {
                                        Text(
                                            text = bookmarksState.bookmarks.size.toString(),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = stringResource(R.string.quran_home_bookmarks)
                            )
                        }
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.quran_home_quran_settings)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Top-level tabs: Home / Browse / Favorites (Bookmarks moved to topbar icon)
            NimazPillTabs(
                tabs = listOf(stringResource(R.string.quran_home_tab_home), stringResource(R.string.quran_home_tab_browse), stringResource(R.string.quran_home_tab_favorites)),
                selectedIndex = state.topTab.coerceIn(0, 2),
                onTabSelect = { viewModel.onEvent(QuranEvent.SetTopTab(it)) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                when (state.topTab.coerceIn(0, 2)) {
                    0 -> HomeTabContent(
                        state = state,
                        bookmarks = bookmarksState.bookmarks,
                        onNavigateToSurah = onNavigateToSurah,
                        onNavigateToBookmarks = onNavigateToBookmarks,
                        onNavigateToQuranAyah = onNavigateToQuranAyah,
                        onNavigateToKhatam = onNavigateToKhatam,
                        onNavigateToKhatamDetail = onNavigateToKhatamDetail
                    )
                    1 -> BrowseTabContent(
                        state = state,
                        onNavigateToSurah = onNavigateToSurah,
                        onNavigateToJuz = onNavigateToJuz,
                        onNavigateToPage = onNavigateToPage,
                        onTabSelect = { viewModel.onEvent(QuranEvent.SetTab(it)) },
                        onNavigateToSurahInfo = onNavigateToSurahInfo
                    )
                    2 -> FavoritesTabContent(
                        favorites = state.favorites,
                        surahs = state.surahs,
                        onNavigateToQuranAyah = onNavigateToQuranAyah
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeTabContent(
    state: com.arshadshah.nimaz.presentation.viewmodel.QuranHomeUiState,
    bookmarks: List<QuranBookmark>,
    onNavigateToSurah: (Int) -> Unit,
    onNavigateToBookmarks: () -> Unit,
    onNavigateToQuranAyah: (Int, Int) -> Unit = { surah, _ -> onNavigateToSurah(surah) },
    onNavigateToKhatam: () -> Unit = {},
    onNavigateToKhatamDetail: (Long) -> Unit = {}
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Continue Reading Card
        if (state.readingProgress != null) {
            val progress = state.readingProgress
            item(key = "continue_reading") {
                ContinueReadingCard(
                    surahNumber = progress.lastSurah,
                    ayahNumber = progress.lastAyah,
                    juzNumber = progress.lastReadJuz,
                    pageNumber = progress.lastReadPage,
                    totalAyahsRead = progress.totalAyahsRead,
                    surahName = state.surahs.find { it.number == progress.lastSurah },
                    onClick = { onNavigateToQuranAyah(progress.lastSurah, progress.lastAyah) }
                )
            }

            // Khatam Progress Card - uses real Khatam data
            item(key = "khatam_progress") {
                KhatamProgressCard(
                    activeKhatam = state.activeKhatam,
                    completedCount = state.completedKhatamCount,
                    onClickActive = { khatamId -> onNavigateToKhatamDetail(khatamId) },
                    onClickStart = onNavigateToKhatam
                )
            }
        } else {
            // Khatam Progress Card (even without reading progress)
            item(key = "khatam_progress") {
                KhatamProgressCard(
                    activeKhatam = state.activeKhatam,
                    completedCount = state.completedKhatamCount,
                    onClickActive = { khatamId -> onNavigateToKhatamDetail(khatamId) },
                    onClickStart = onNavigateToKhatam
                )
            }

            // No reading progress yet - show start reading prompt
            item(key = "start_reading") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    ),
                    onClick = { if (state.surahs.isNotEmpty()) onNavigateToSurah(1) }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF115E59),
                                        Color(0xFF042F2E)
                                    )
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color(0xFFEAB308),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.quran_home_start_reading),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.quran_home_begin_journey),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFD4D4D4)
                            )
                        }
                    }
                }
            }
        }

        // Bookmarks Horizontal Row
        if (bookmarks.isNotEmpty()) {
            item(key = "bookmarks_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.quran_home_bookmarks),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.quran_home_see_all),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onNavigateToBookmarks() }
                    )
                }
            }

            item(key = "bookmarks_row") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = bookmarks,
                        key = { it.id }
                    ) { bookmark ->
                        BookmarkCard(
                            bookmark = bookmark,
                            onClick = { onNavigateToSurah(bookmark.surahNumber) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowseTabContent(
    state: com.arshadshah.nimaz.presentation.viewmodel.QuranHomeUiState,
    onNavigateToSurah: (Int) -> Unit,
    onNavigateToJuz: (Int) -> Unit,
    onNavigateToPage: (Int) -> Unit,
    onTabSelect: (Int) -> Unit,
    onNavigateToSurahInfo: (Int) -> Unit = {}
) {
    val isKhatamActive = state.activeKhatam != null
    val khatamReadAyahIds = state.khatamReadAyahIds

    // Build surah ayah ranges: Map<surahNumber, IntRange> (cumulative sum of ayahCount)
    val surahAyahRanges = remember(state.surahs) {
        val ranges = mutableMapOf<Int, IntRange>()
        var start = 1
        for (surah in state.surahs) {
            val end = start + surah.ayahCount - 1
            ranges[surah.number] = start..end
            start = end + 1
        }
        ranges
    }

    Column {
        // Sticky TabRow outside LazyColumn - only Surah/Juz/Page
        TabRow(
            selectedTabIndex = state.selectedTab,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            listOf(stringResource(R.string.quran_home_tab_surah), stringResource(R.string.quran_home_tab_juz), stringResource(R.string.quran_home_tab_page)).forEachIndexed { index, title ->
                Tab(
                    selected = state.selectedTab == index,
                    onClick = { onTabSelect(index) },
                    text = { Text(title) }
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (state.selectedTab) {
                0 -> {
                    items(
                        items = state.filteredSurahs,
                        key = { it.number }
                    ) { surah ->
                        val surahRange = surahAyahRanges[surah.number]
                        val readCount = if (isKhatamActive && surahRange != null)
                            khatamReadAyahIds.count { it in surahRange } else 0
                        SurahListItem(
                            surah = surah,
                            onClick = { onNavigateToSurah(surah.number) },
                            onInfoClick = { onNavigateToSurahInfo(surah.number) },
                            khatamReadCount = readCount,
                            khatamTotalAyahs = surah.ayahCount,
                            isKhatamActive = isKhatamActive
                        )
                    }
                }
                1 -> {
                    item(key = "juz_grid") {
                        JuzGrid(
                            onNavigateToJuz = onNavigateToJuz,
                            khatamReadAyahIds = khatamReadAyahIds,
                            isKhatamActive = isKhatamActive
                        )
                    }
                }
                2 -> {
                    // Page grid items added directly to avoid nested LazyColumn
                    pageGridItems(
                        onNavigateToPage = onNavigateToPage,
                        khatamReadAyahIds = khatamReadAyahIds,
                        isKhatamActive = isKhatamActive,
                        pageAyahRanges = state.pageAyahRanges
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoritesTabContent(
    favorites: List<QuranFavorite>,
    surahs: List<Surah>,
    onNavigateToQuranAyah: (Int, Int) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (favorites.isEmpty()) {
            item(key = "no_favorites") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.quran_home_no_favorite_ayahs),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.quran_home_favorite_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        } else {
            items(
                items = favorites,
                key = { "fav_${it.ayahId}" }
            ) { favorite ->
                val surahName = surahs.find { it.number == favorite.surahNumber }?.nameEnglish
                    ?: stringResource(R.string.quran_home_surah_fallback, favorite.surahNumber)
                FavoriteAyahItem(
                    surahName = surahName,
                    ayahNumber = favorite.ayahNumber,
                    surahNumber = favorite.surahNumber,
                    onClick = { onNavigateToQuranAyah(favorite.surahNumber, favorite.ayahNumber) }
                )
            }
        }
    }
}

@Composable
private fun BookmarksTabContent(
    bookmarks: List<QuranBookmark>,
    onNavigateToSurah: (Int) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (bookmarks.isEmpty()) {
            item(key = "no_bookmarks") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.quran_home_no_bookmarks),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.quran_home_bookmark_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        } else {
            items(
                items = bookmarks,
                key = { "bm_${it.id}" }
            ) { bookmark ->
                BookmarkListItem(
                    bookmark = bookmark,
                    onClick = { onNavigateToSurah(bookmark.surahNumber) }
            )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookmarkListItem(
    bookmark: QuranBookmark,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Bookmark,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bookmark.surahName ?: stringResource(R.string.quran_home_surah_fallback, bookmark.surahNumber),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.quran_home_verse_format, bookmark.ayahNumber),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!bookmark.ayahText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = bookmark.ayahText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavoriteAyahItem(
    surahName: String,
    ayahNumber: Int,
    surahNumber: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = surahName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.quran_home_verse_format, ayahNumber),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KhatamProgressCard(
    activeKhatam: Khatam?,
    completedCount: Int,
    onClickActive: (Long) -> Unit,
    onClickStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (activeKhatam != null) {
        val progressFraction = activeKhatam.progressPercent
        Card(
            onClick = { onClickActive(activeKhatam.id) },
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = activeKhatam.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = stringResource(R.string.quran_home_completed_count, completedCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${activeKhatam.totalAyahsRead}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.quran_home_ayahs_read),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${Khatam.TOTAL_QURAN_AYAHS - activeKhatam.totalAyahsRead}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.quran_home_remaining),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFraction)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF14B8A6),
                                        Color(0xFFEAB308)
                                    )
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.quran_home_percent_complete, (progressFraction * 100).toInt()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    } else {
        Card(
            onClick = onClickStart,
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.quran_home_start_khatam),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.quran_home_track_progress),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookmarkCard(
    bookmark: QuranBookmark,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.width(160.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = bookmark.surahName ?: stringResource(R.string.quran_home_surah_fallback, bookmark.surahNumber),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.quran_home_verse_format, bookmark.ayahNumber),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!bookmark.ayahText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = bookmark.ayahText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun JuzGrid(
    onNavigateToJuz: (Int) -> Unit,
    khatamReadAyahIds: Set<Int> = emptySet(),
    isKhatamActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val columns = 5
    val juzNumbers = (1..30).toList()

    // Pre-compute juz progress
    val juzProgress = remember(khatamReadAyahIds, isKhatamActive) {
        if (!isKhatamActive) emptyMap()
        else KhatamConstants.JUZ_AYAH_RANGES.mapIndexed { index, (start, end) ->
            val total = end - start + 1
            val read = khatamReadAyahIds.count { it in start..end }
            (index + 1) to (read to total)
        }.toMap()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        juzNumbers.chunked(columns).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { juzNumber ->
                    val (readCount, totalCount) = juzProgress[juzNumber] ?: (0 to 0)
                    val progress = if (totalCount > 0) readCount.toFloat() / totalCount else 0f
                    val isComplete = isKhatamActive && totalCount > 0 && readCount == totalCount

                    Card(
                        onClick = { onNavigateToJuz(juzNumber) },
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isComplete)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            // Show progress ring behind number when khatam is active
                            if (isKhatamActive && progress > 0f && !isComplete) {
                                CircularProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.size(48.dp),
                                    color = MaterialTheme.colorScheme.tertiary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    strokeWidth = 3.dp
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = juzNumber.toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isComplete)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(R.string.quran_home_juz_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isComplete)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                repeat(columns - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// Juz to page mapping (approximate start pages for each Juz)
private val juzStartPages = listOf(
    1, 22, 42, 62, 82, 102, 121, 142, 162, 182,
    201, 222, 242, 262, 282, 302, 322, 342, 362, 382,
    402, 422, 442, 462, 482, 502, 522, 542, 562, 582
)

private fun getJuzForPage(page: Int): Int {
    for (i in juzStartPages.indices.reversed()) {
        if (page >= juzStartPages[i]) return i + 1
    }
    return 1
}

private fun getJuzStartPage(juz: Int): Int = juzStartPages.getOrElse(juz - 1) { 1 }

private fun getJuzEndPage(juz: Int): Int = if (juz < 30) juzStartPages[juz] - 1 else 604

/**
 * Adds page grid items directly to a LazyListScope to avoid nested scrolling.
 */
@OptIn(ExperimentalMaterial3Api::class)
private fun LazyListScope.pageGridItems(
    onNavigateToPage: (Int) -> Unit,
    khatamReadAyahIds: Set<Int> = emptySet(),
    isKhatamActive: Boolean = false,
    pageAyahRanges: List<PageAyahRange> = emptyList()
) {
    val columns = 5

    // Pre-compute page progress map
    val pageProgressMap = if (isKhatamActive && pageAyahRanges.isNotEmpty()) {
        pageAyahRanges.associate { range ->
            val readCount = khatamReadAyahIds.count { it in range.minAyahId..range.maxAyahId }
            range.page to (readCount to range.ayahCount)
        }
    } else {
        emptyMap()
    }

    // Jump-to-page input as first item
    item(key = "page_jump_input") {
        var jumpToPage by remember { mutableStateOf("") }
        OutlinedTextField(
            value = jumpToPage,
            onValueChange = { newValue ->
                if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                    jumpToPage = newValue
                }
            },
            label = { Text(stringResource(R.string.quran_home_jump_to_page)) },
            placeholder = { Text(stringResource(R.string.quran_home_enter_page_number)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Go
            ),
            keyboardActions = KeyboardActions(
                onGo = {
                    jumpToPage.toIntOrNull()?.let { page ->
                        if (page in 1..604) {
                            onNavigateToPage(page)
                        }
                    }
                }
            ),
            trailingIcon = {
                IconButton(
                    onClick = {
                        jumpToPage.toIntOrNull()?.let { page ->
                            if (page in 1..604) {
                                onNavigateToPage(page)
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.quran_home_go_to_page),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            shape = RoundedCornerShape(12.dp)
        )
    }

    // Add items for each Juz section
    (1..30).forEach { juz ->
        val startPage = getJuzStartPage(juz)
        val endPage = getJuzEndPage(juz)

        // Juz header
        item(key = "page_juz_header_$juz") {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.quran_home_juz_pages_format, juz, startPage, endPage),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }

        // Pages in this Juz, chunked into rows
        val pagesInJuz = (startPage..endPage).toList()
        items(
            items = pagesInJuz.chunked(columns),
            key = { row -> "page_row_${row.first()}" }
        ) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { pageNumber ->
                    val (readCount, totalCount) = pageProgressMap[pageNumber] ?: (0 to 0)
                    val progress = if (totalCount > 0) readCount.toFloat() / totalCount else 0f
                    val isComplete = isKhatamActive && totalCount > 0 && readCount == totalCount

                    Card(
                        onClick = { onNavigateToPage(pageNumber) },
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isComplete)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            // Progress ring when khatam is active and partially read
                            if (isKhatamActive && progress > 0f && !isComplete) {
                                CircularProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.size(48.dp),
                                    color = MaterialTheme.colorScheme.tertiary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    strokeWidth = 3.dp
                                )
                            }
                            Text(
                                text = pageNumber.toString(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isComplete)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                // Fill remaining space if row is incomplete
                repeat(columns - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContinueReadingCard(
    surahNumber: Int,
    ayahNumber: Int,
    juzNumber: Int,
    pageNumber: Int,
    totalAyahsRead: Int,
    surahName: Surah?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalAyahs = 6236
    val progressFraction = (totalAyahsRead.toFloat() / totalAyahs).coerceIn(0f, 1f)
    val progressPercent = (progressFraction * 100).toInt()

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF115E59),
                            Color(0xFF042F2E)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color(0xFF0F766E),
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = stringResource(R.string.quran_home_continue_reading),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF2DD4BF),
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = surahName?.nameEnglish ?: stringResource(R.string.quran_home_surah_fallback, surahNumber),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (surahName != null) {
                    ArabicText(
                        text = surahName.nameArabic,
                        size = ArabicTextSize.MEDIUM,
                        color = Color(0xFFEAB308)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.quran_home_verse_format, ayahNumber),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD4D4D4)
                    )
                    Text(
                        text = stringResource(R.string.quran_home_juz_format, juzNumber),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD4D4D4)
                    )
                    Text(
                        text = stringResource(R.string.quran_home_page_format, pageNumber),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD4D4D4)
                    )
                }

                Spacer(modifier = Modifier.height(15.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFFEAB308))
                        )
                    }

                    Text(
                        text = "$progressPercent%",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD4D4D4)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SurahListItem(
    surah: Surah,
    onClick: () -> Unit,
    onInfoClick: () -> Unit = {},
    khatamReadCount: Int = 0,
    khatamTotalAyahs: Int = 0,
    isKhatamActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isComplete = isKhatamActive && khatamTotalAyahs > 0 && khatamReadCount == khatamTotalAyahs
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isComplete) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = stringResource(R.string.quran_home_completed),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .rotate(45f)
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                        Text(
                            text = surah.number.toString(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = surah.nameEnglish,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val isMeccan = surah.revelationType == RevelationType.MECCAN
                        Text(
                            text = if (isMeccan) stringResource(R.string.quran_home_makkah) else stringResource(R.string.quran_home_madinah),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "\u2022",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.quran_home_verses_count, surah.ayahCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                ArabicText(
                    text = surah.nameArabic,
                    size = ArabicTextSize.MEDIUM,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                IconButton(
                    onClick = onInfoClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = stringResource(R.string.quran_home_surah_info),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Khatam progress bar
            if (isKhatamActive && khatamTotalAyahs > 0 && khatamReadCount > 0) {
                LinearProgressIndicator(
                    progress = { khatamReadCount.toFloat() / khatamTotalAyahs },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .padding(horizontal = 14.dp),
                    color = if (isComplete) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

