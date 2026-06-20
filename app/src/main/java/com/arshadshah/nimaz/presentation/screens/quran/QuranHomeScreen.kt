package com.arshadshah.nimaz.presentation.screens.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.QuranBookmark
import com.arshadshah.nimaz.domain.model.QuranFavorite
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.presentation.components.molecules.BookmarkCard
import com.arshadshah.nimaz.presentation.components.molecules.BookmarkListItem
import com.arshadshah.nimaz.presentation.components.molecules.ContinueReadingCard
import com.arshadshah.nimaz.presentation.components.molecules.FavoriteAyahItem
import com.arshadshah.nimaz.presentation.components.molecules.KhatamProgressCard
import com.arshadshah.nimaz.presentation.components.molecules.SurahListItem
import com.arshadshah.nimaz.presentation.components.organisms.JuzGrid
import com.arshadshah.nimaz.presentation.components.organisms.NimazTopAppBar
import com.arshadshah.nimaz.presentation.components.organisms.computeJuzHeaderIndices
import com.arshadshah.nimaz.presentation.components.organisms.pageGridItems
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.viewmodel.QuranEvent
import com.arshadshah.nimaz.presentation.viewmodel.QuranHomeUiState
import com.arshadshah.nimaz.presentation.viewmodel.QuranViewModel
import kotlinx.coroutines.launch

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
    // Tablet: highlight currently selected item in list pane
    selectedSurahNumber: Int? = null,
    selectedJuzNumber: Int? = null,
    selectedPageNumber: Int? = null,
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

            PrimaryTabRow(
                selectedTabIndex = state.topTab.coerceIn(0, 2),
                tabs = {
                    listOf(
                        stringResource(R.string.quran_home_tab_home),
                        stringResource(R.string.quran_home_tab_browse),
                        stringResource(R.string.quran_home_tab_favorites)
                    ).forEachIndexed { index, title ->
                        Tab(
                            selected = state.selectedTab == index,
                            onClick = { viewModel.onEvent(QuranEvent.SetTopTab(index)) },
                            text = { Text(title) }
                        )
                    }
                }
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
                        onNavigateToSurahInfo = onNavigateToSurahInfo,
                        selectedSurahNumber = selectedSurahNumber,
                        selectedJuzNumber = selectedJuzNumber,
                        selectedPageNumber = selectedPageNumber
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
    state: QuranHomeUiState,
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
    state: QuranHomeUiState,
    onNavigateToSurah: (Int) -> Unit,
    onNavigateToJuz: (Int) -> Unit,
    onNavigateToPage: (Int) -> Unit,
    onTabSelect: (Int) -> Unit,
    onNavigateToSurahInfo: (Int) -> Unit = {},
    selectedSurahNumber: Int? = null,
    selectedJuzNumber: Int? = null,
    selectedPageNumber: Int? = null
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

    // Page number -> list of surah names that start on that page
    val surahStartPageMap = remember(state.surahs) {
        state.surahs.groupBy { it.startPage }
            .mapValues { (_, surahs) -> surahs.map { it.nameEnglish } }
    }

    // Surah number -> (startPage, endPage)
    val surahPageRanges = remember(state.surahs) {
        val sorted = state.surahs.sortedBy { it.number }
        sorted.mapIndexed { index, surah ->
            val endPage = if (index < sorted.lastIndex) sorted[index + 1].startPage - 1 else 604
            surah.number to (surah.startPage to endPage)
        }.toMap()
    }

    Column {
        // Sticky TabRow outside LazyColumn - only Surah/Juz/Page
        SecondaryTabRow(
            selectedTabIndex = state.selectedTab,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            listOf(
                stringResource(R.string.quran_home_tab_surah),
                stringResource(R.string.quran_home_tab_juz),
                stringResource(R.string.quran_home_tab_page)
            ).forEachIndexed { index, title ->
                Tab(
                    selected = state.selectedTab == index,
                    onClick = { onTabSelect(index) },
                    text = { Text(title) }
                )
            }
        }

        // Sticky jump-to-page input for the Page tab
        if (state.selectedTab == 2) {
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
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Shared list state for the page tab (used by scrollbar)
        val pageListState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()

        // Pre-compute juz header indices for the scrollbar
        val juzHeaderIndices = remember(surahStartPageMap) {
            computeJuzHeaderIndices(surahStartPageMap)
        }

        // Reverse lookup: item index → juz number (find which juz the first visible item belongs to)
        val currentJuz by remember {
            derivedStateOf {
                val firstVisibleIndex = pageListState.firstVisibleItemIndex
                var result = 1
                for (juz in 1..30) {
                    val headerIndex = juzHeaderIndices[juz] ?: 0
                    if (headerIndex <= firstVisibleIndex) result = juz
                    else break
                }
                result
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = if (state.selectedTab == 2) pageListState else rememberLazyListState(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = if (state.selectedTab == 2) 40.dp else 16.dp,
                    top = 8.dp,
                    bottom = 8.dp
                ),
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
                            val (startPage, endPage) = surahPageRanges[surah.number]
                                ?: (surah.startPage to surah.startPage)
                            SurahListItem(
                                surah = surah,
                                onClick = { onNavigateToSurah(surah.number) },
                                onInfoClick = { onNavigateToSurahInfo(surah.number) },
                                khatamReadCount = readCount,
                                khatamTotalAyahs = surah.ayahCount,
                                isKhatamActive = isKhatamActive,
                                isSelected = selectedSurahNumber == surah.number,
                                startPage = startPage,
                                endPage = endPage
                            )
                        }
                    }

                    1 -> {
                        item(key = "juz_grid") {
                            JuzGrid(
                                onNavigateToJuz = onNavigateToJuz,
                                khatamReadAyahIds = khatamReadAyahIds,
                                isKhatamActive = isKhatamActive,
                                selectedJuzNumber = selectedJuzNumber
                            )
                        }
                    }

                    2 -> {
                        // Page grid items added directly to avoid nested LazyColumn
                        pageGridItems(
                            onNavigateToPage = onNavigateToPage,
                            khatamReadAyahIds = khatamReadAyahIds,
                            isKhatamActive = isKhatamActive,
                            pageAyahRanges = state.pageAyahRanges,
                            selectedPageNumber = selectedPageNumber,
                            surahStartPageMap = surahStartPageMap
                        )
                    }
                }
            }

            // Juz scrollbar rail — only shown on Page tab
            if (state.selectedTab == 2) {
                JuzScrollbar(
                    currentJuz = currentJuz,
                    onJuzSelected = { juz ->
                        val targetIndex = juzHeaderIndices[juz] ?: 0
                        coroutineScope.launch {
                            pageListState.animateScrollToItem(targetIndex)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(end = 2.dp, top = 4.dp, bottom = 4.dp)
                )
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

/**
 * Thin vertical scrollbar with Juz 1–30 indicators.
 * Tap a number to scroll, or drag along the rail for fast scrubbing.
 */
@Composable
private fun JuzScrollbar(
    currentJuz: Int,
    onJuzSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    /** Map a y-offset inside the rail to a juz number (1..30). */
    fun yToJuz(y: Float, height: Float): Int =
        ((y / height) * 30).toInt().coerceIn(0, 29) + 1

    Column(
        modifier = modifier
            .width(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, _ ->
                    change.consume()
                    onJuzSelected(yToJuz(change.position.y, size.height.toFloat()))
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onJuzSelected(yToJuz(offset.y, size.height.toFloat()))
                }
            },
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val activeBackground = MaterialTheme.colorScheme.primary
        val activeTextColor = MaterialTheme.colorScheme.onPrimary
        val inactiveTextColor = MaterialTheme.colorScheme.onSurfaceVariant

        (1..30).forEach { juz ->
            val isCurrent = juz == currentJuz
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .then(
                        if (isCurrent) Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(activeBackground)
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = juz.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCurrent) activeTextColor else inactiveTextColor
                )
            }
        }
    }
}

@Preview(showBackground = false, heightDp = 600, showSystemUi = false, backgroundColor = 0xFFFFFFFF)
@Composable
private fun JuzScrollbarPreview() {
    NimazTheme {
        JuzScrollbar(
            currentJuz = 1,
            onJuzSelected = {},
            modifier = Modifier.fillMaxHeight()
        )
    }
}
