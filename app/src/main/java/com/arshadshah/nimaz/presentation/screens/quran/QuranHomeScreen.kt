package com.arshadshah.nimaz.presentation.screens.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.navigation.ScreenTags
import com.arshadshah.nimaz.core.share.ContentShareManager
import com.arshadshah.nimaz.core.share.Shareables
import com.arshadshah.nimaz.domain.model.QuranBookmark
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.presentation.components.atoms.GradientCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeEmphasis
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazIconVariant
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.molecules.NimazMenuItem
import com.arshadshah.nimaz.presentation.components.molecules.BookmarkCard
import com.arshadshah.nimaz.presentation.components.molecules.BookmarkListItem
import com.arshadshah.nimaz.presentation.components.molecules.ContinueReadingCard
import com.arshadshah.nimaz.presentation.components.molecules.KhatamProgressCard
import com.arshadshah.nimaz.presentation.components.molecules.NimazEmptyState
import com.arshadshah.nimaz.presentation.components.molecules.QuranRecommendedSurahs
import com.arshadshah.nimaz.presentation.components.molecules.SurahListItem
import com.arshadshah.nimaz.presentation.components.molecules.VerseOfTheDayCard
import com.arshadshah.nimaz.presentation.components.organisms.JuzGrid
import com.arshadshah.nimaz.presentation.components.organisms.NimazMenuAction
import com.arshadshah.nimaz.presentation.components.organisms.NimazTopAppBar
import com.arshadshah.nimaz.presentation.components.organisms.SwipeableSavedCard
import com.arshadshah.nimaz.presentation.components.organisms.computeJuzHeaderIndices
import com.arshadshah.nimaz.presentation.components.organisms.pageGridItems
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazPalette
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.viewmodel.quran.FavoriteAyahUi
import com.arshadshah.nimaz.presentation.viewmodel.quran.QuranEvent
import com.arshadshah.nimaz.presentation.viewmodel.quran.QuranHomeUiState
import com.arshadshah.nimaz.presentation.viewmodel.quran.QuranViewModel
import java.time.DayOfWeek
import java.time.LocalDate
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
    onNavigateToTopics: () -> Unit = {},
    onNavigateToQuranAyah: (Int, Int) -> Unit = { surah, ayah -> onNavigateToSurah(surah) },
    onNavigateToKhatam: () -> Unit = {},
    onNavigateToKhatamDetail: (Long) -> Unit = {},
    // Tablet: highlight currently selected item in list pane
    selectedSurahNumber: Int? = null,
    selectedJuzNumber: Int? = null,
    selectedPageNumber: Int? = null,
    viewModel: QuranViewModel = hiltViewModel()
) {
    val state by viewModel.homeState.collectAsStateWithLifecycle()
    val bookmarksState by viewModel.bookmarksState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }

    // Drive the Undo snackbar off the most recently removed favourite.
    val favoriteRemovedMessage = stringResource(R.string.favorite_removed)
    val undoLabel = stringResource(R.string.undo)
    val removedFavoriteId = state.recentlyRemovedFavorite?.ayahId
    LaunchedEffect(removedFavoriteId) {
        if (removedFavoriteId != null) {
            val result = snackbarHostState.showSnackbar(
                message = favoriteRemovedMessage,
                actionLabel = undoLabel,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.onEvent(QuranEvent.UndoRemoveFavorite)
            } else {
                viewModel.onEvent(QuranEvent.DismissFavoriteUndo)
            }
        }
    }

    NimazScreenScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            NimazTopAppBar(
                title = stringResource(R.string.quran_home_title),
                scrollBehavior = scrollBehavior,
                actions = {
                    // Subjects used to sit here as a bare Category glyph. An icon is a fine
                    // control for something a reader already knows exists; 2,512 subjects
                    // indexed by hand is not that, and nothing on the screen said so. It is a
                    // labelled card in the content now, where it can say what it is.
                    // Search icon
                    IconButton(onClick = onNavigateToSearch) {
                        NimazIcon(
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
                            NimazIcon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = stringResource(R.string.quran_home_bookmarks)
                            )
                        }
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        NimazIcon(
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

            // `topTab` is the authoritative field for this outer row (it also drives the
            // `when` below); `selectedTab` is the Browse sub-tab (Surah/Juz/Page) and must
            // not be read here.
            val topTabIndex = state.topTab.coerceIn(0, 2)

            PrimaryTabRow(
                selectedTabIndex = topTabIndex,
                tabs = {
                    listOf(
                        stringResource(R.string.quran_home_tab_home),
                        stringResource(R.string.quran_home_tab_browse),
                        stringResource(R.string.quran_home_tab_favorites)
                    ).forEachIndexed { index, title ->
                        Tab(
                            selected = topTabIndex == index,
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
                when (topTabIndex) {
                    0 -> HomeTabContent(
                        state = state,
                        bookmarks = bookmarksState.bookmarks,
                        onNavigateToSurah = onNavigateToSurah,
                        onNavigateToTopics = onNavigateToTopics,
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
                        onNavigateToQuranAyah = onNavigateToQuranAyah,
                        onRemoveFavorite = { viewModel.onEvent(QuranEvent.RemoveFavorite(it)) }
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
    onNavigateToTopics: () -> Unit,
    onNavigateToBookmarks: () -> Unit,
    onNavigateToQuranAyah: (Int, Int) -> Unit = { surah, _ -> onNavigateToSurah(surah) },
    onNavigateToKhatam: () -> Unit = {},
    onNavigateToKhatamDetail: (Long) -> Unit = {}
) {
    val isFriday = remember { LocalDate.now().dayOfWeek == DayOfWeek.FRIDAY }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Resume first. Exactly one card on this screen carries the teal gradient:
        // continue-reading when there is progress, otherwise the start-reading hero.
        // Rendering both stacked two near-identical "begin" affordances on each other,
        // and spending the gradient twice meant it signalled nothing.
        val progress = state.readingProgress
        if (progress != null) {
            item(key = "continue_reading") {
                ContinueReadingCard(
                    surahNumber = progress.lastSurah,
                    ayahNumber = progress.lastAyah,
                    juzNumber = progress.lastReadJuz,
                    pageNumber = progress.lastReadPage,
                    totalAyahsRead = progress.totalAyahsRead,
                    surahName = state.surahs.find { it.number == progress.lastSurah },
                    onClick = { onNavigateToQuranAyah(progress.lastSurah, progress.lastAyah) },
                    totalPages = state.pagination.totalPages
                )
            }
        } else {
            item(key = "start_reading") {
                val shape = RoundedCornerShape(20.dp)
                GradientCard(
                    gradientColors = NimazColors.QuranColors.BannerGradient,
                    shape = shape,
                    onClick = { if (state.surahs.isNotEmpty()) onNavigateToSurah(1) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = NimazColors.QuranColors.BannerBorder,
                            shape = shape
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NimazIcon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = NimazColors.QuranColors.BannerAccent,
                            iconSize = 32.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.quran_home_start_reading),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = stringResource(R.string.quran_home_begin_journey),
                                style = MaterialTheme.typography.bodySmall,
                                color = NimazColors.Gray300
                            )
                        }
                    }
                }
            }
        }

        // 2. Khatam — the only item with a deadline, so it must survive the fold.
        // Compact row form; collapses to a one-row prompt when no khatam is active.
        item(key = "khatam_progress") {
            KhatamProgressCard(
                activeKhatam = state.activeKhatam,
                insights = state.activeKhatamInsights,
                completedCount = state.completedKhatamCount,
                onClickActive = { khatamId -> onNavigateToKhatamDetail(khatamId) },
                onClickStart = onNavigateToKhatam
            )
        }

        // 3. Recommended surahs (contextual — Al-Kahf first on Fridays)
        item(key = "recommended_header") {
            HomeSectionTitle(text = stringResource(R.string.quran_home_recommended))
        }
        item(key = "recommended_surahs") {
            QuranRecommendedSurahs(
                surahs = state.surahs,
                isFriday = isFriday,
                onSurahClick = onNavigateToSurah
            )
        }

        // 3b. The way into the subject browser. Gated on the artifact actually carrying the
        // thematic layer — an install between the migration and a schemaVersion 24 release has
        // the tables and no rows, and a card promising 2,512 subjects would open onto an
        // explanation.
        //
        // Deliberately not called "Browse by subject": this screen's tab row already has a tab
        // called "Browse", and two different "Browse" affordances on one screen is a coin flip
        // for the reader about which one lists surahs. (It was also a coin flip for
        // `QuranOpenSurahTest`, whose `clickText` matches on substring.)
        if (state.hasThematicContent) {
            item(key = "browse_subjects") {
                NimazMenuItem(
                    title = stringResource(R.string.quran_home_browse_subjects),
                    subtitle = stringResource(R.string.quran_home_browse_subjects_subtitle),
                    icon = Icons.Default.AccountTree,
                    onClick = onNavigateToTopics
                )
            }
        }

        // 4. Verse of the Day — a daily nudge, not the headline. Demoted below the
        // resume/khatam/recommended stack and rendered as a normal elevated card.
        val verse = state.verseOfTheDay
        if (verse != null) {
            item(key = "verse_of_the_day") {
                val surahName = state.surahs.find { it.number == verse.surahNumber }?.nameEnglish
                    ?: stringResource(R.string.quran_home_surah_fallback, verse.surahNumber)
                VerseOfTheDayCard(
                    arabicText = verse.textArabic,
                    translation = verse.translation,
                    reference = stringResource(
                        R.string.quran_home_verse_reference,
                        surahName,
                        verse.surahNumber,
                        verse.ayahNumber
                    ),
                    onClick = { onNavigateToQuranAyah(verse.surahNumber, verse.ayahNumber) }
                )
            }
        }

        // 5. Bookmarks Horizontal Row
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
private fun HomeSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(vertical = 4.dp)
    )
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

    // Surah number -> the page it opens on, in the *active* edition. `surah.startPage` is
    // the Madani column, so under the 16-line layout the page is resolved from the surah's
    // first ayah through the active pagination instead (#325).
    val surahStartPages = remember(state.surahs, state.pagination) {
        state.surahs.associate { surah ->
            val firstAyahId = surahAyahRanges[surah.number]?.first
            val page = firstAyahId?.let { state.pagination.pageForAyah(it) } ?: surah.startPage
            surah.number to page
        }
    }

    // Page number -> list of surah names that start on that page
    val surahStartPageMap = remember(surahStartPages, state.surahs) {
        state.surahs.groupBy { surahStartPages[it.number] ?: it.startPage }
            .mapValues { (_, surahs) -> surahs.map { it.nameEnglish } }
    }

    // Surah number -> (startPage, endPage)
    val surahPageRanges = remember(surahStartPages, state.surahs, state.pagination) {
        val sorted = state.surahs.sortedBy { it.number }
        sorted.mapIndexed { index, surah ->
            val startPage = surahStartPages[surah.number] ?: surah.startPage
            val endPage = if (index < sorted.lastIndex) {
                (surahStartPages[sorted[index + 1].number] ?: sorted[index + 1].startPage) - 1
            } else {
                state.pagination.totalPages
            }
            surah.number to (startPage to endPage.coerceAtLeast(startPage))
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
            // Bounded by the *active edition's* page ranges, not the count declared on the
            // enum: the two disagree for an edition whose data paginates differently, and for
            // any non-Madani edition the mapping is not ready until its ranges load — the
            // window in which the grid below shows a spinner (#325).
            val requestedPage = state.pagination.pageFromInput(jumpToPage)
            val isOutOfRange = jumpToPage.isNotBlank() && requestedPage == null
            val goToPage = { requestedPage?.let(onNavigateToPage); Unit }

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
                isError = isOutOfRange,
                // An out-of-range number used to do nothing at all — no error, no message —
                // which is what made a stale bound invisible.
                supportingText = {
                    Text(
                        stringResource(
                            R.string.quran_home_page_range_format,
                            1,
                            state.pagination.totalPages
                        )
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions(onGo = { goToPage() }),
                trailingIcon = {
                    IconButton(onClick = goToPage, enabled = requestedPage != null) {
                        NimazIcon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = stringResource(R.string.quran_home_go_to_page),
                            variant = NimazIconVariant.PRIMARY
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // One stably-remembered list state per sub-tab. These must be hoisted here rather
        // than created inside the `state = if (...) ... else rememberLazyListState()`
        // expression below: a `remember` inside a conditional is not keyed stably across
        // recompositions, so the Surah/Juz tabs kept losing their scroll position.
        val surahListState = rememberLazyListState()
        val juzListState = rememberLazyListState()
        val pageListState = rememberLazyListState() // also driven by the juz scrollbar
        val coroutineScope = rememberCoroutineScope()

        // Pre-compute juz header indices for the scrollbar
        val juzHeaderIndices = remember(surahStartPageMap, state.pagination) {
            computeJuzHeaderIndices(surahStartPageMap, state.pagination)
        }

        // Reverse lookup: item index → juz number (find which juz the first visible item belongs to)
        val currentJuz by remember(juzHeaderIndices) {
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
                modifier = Modifier.testTag(ScreenTags.QuranSurahList),
                state = when (state.selectedTab) {
                    1 -> juzListState
                    2 -> pageListState
                    else -> surahListState
                },
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
                                endPage = endPage,
                                juzNumber = state.pagination.juzForPage(startPage),
                                rukuCount = state.rukuCounts[surah.number] ?: 0
                            )
                        }
                    }

                    1 -> {
                        item(key = "juz_grid") {
                            JuzGrid(
                                onNavigateToJuz = onNavigateToJuz,
                                pagination = state.pagination,
                                khatamReadAyahIds = khatamReadAyahIds,
                                isKhatamActive = isKhatamActive,
                                selectedJuzNumber = selectedJuzNumber
                            )
                        }
                    }

                    2 -> {
                        // Page grid items added directly to avoid nested LazyColumn.
                        // A not-yet-ready pagination (a non-Madani edition whose page data
                        // is still loading) would print wrong page numbers, so wait (#325).
                        if (state.pagination.isReady) {
                            pageGridItems(
                                onNavigateToPage = onNavigateToPage,
                                pagination = state.pagination,
                                khatamReadAyahIds = khatamReadAyahIds,
                                isKhatamActive = isKhatamActive,
                                selectedPageNumber = selectedPageNumber,
                                surahStartPageMap = surahStartPageMap
                            )
                        } else {
                            item(key = "page_grid_loading") {
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
    favorites: List<FavoriteAyahUi>,
    surahs: List<Surah>,
    onNavigateToQuranAyah: (Int, Int) -> Unit,
    onRemoveFavorite: (FavoriteAyahUi) -> Unit
) {
    if (favorites.isEmpty()) {
        NimazEmptyState(
            title = stringResource(R.string.quran_home_no_favorite_ayahs),
            message = stringResource(R.string.quran_home_favorite_hint),
            icon = Icons.Default.Favorite,
            iconTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        )
        return
    }

    val context = LocalContext.current
    val shareScope = rememberCoroutineScope()
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = favorites,
            key = { "fav_${it.ayahId}" }
        ) { favorite ->
            val surahName = surahs.find { it.number == favorite.surahNumber }?.nameEnglish
                ?: stringResource(R.string.quran_home_surah_fallback, favorite.surahNumber)
            val verseLabel = stringResource(R.string.quran_home_verse_format, favorite.ayahNumber)
            SwipeableSavedCard(
                title = surahName,
                subtitle = verseLabel,
                timestamp = favorite.createdAt,
                arabicText = favorite.arabicText,
                onClick = { onNavigateToQuranAyah(favorite.surahNumber, favorite.ayahNumber) },
                onDelete = { onRemoveFavorite(favorite) },
                enableSwipeToDelete = false,
                menuActions = listOf(
                    NimazMenuAction(
                        text = stringResource(R.string.share),
                        icon = Icons.Default.Share,
                        onClick = {
                            shareScope.launch {
                                ContentShareManager.shareBranded(
                                    context,
                                    Shareables.favorite(
                                        context,
                                        surahName = surahName,
                                        verseLabel = verseLabel,
                                        arabicText = favorite.arabicText,
                                    )
                                )
                            }
                        },
                    ),
                    NimazMenuAction(
                        text = stringResource(R.string.remove_from_favorites),
                        icon = Icons.Default.Delete,
                        onClick = { onRemoveFavorite(favorite) },
                        destructive = true,
                    ),
                ),
                leading = {
                    NimazBadge(
                        text = stringResource(R.string.favorite_type),
                        size = NimazBadgeSize.SMALL,
                        colors = NimazBadgeDefaults.feature(
                            color = NimazPalette.Red500,
                            emphasis = NimazBadgeEmphasis.FILLED
                        )
                    )
                }
            )
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
                        NimazIcon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            iconSize = 48.dp
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
