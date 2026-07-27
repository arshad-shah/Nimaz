package com.arshadshah.nimaz.presentation.screens.quran

import android.content.res.Configuration
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazPager
import com.arshadshah.nimaz.presentation.components.atoms.NimazScreenScaffold
import com.arshadshah.nimaz.presentation.components.atoms.PageSurahSeparator
import com.arshadshah.nimaz.presentation.components.atoms.rememberNimazPagerState
import com.arshadshah.nimaz.presentation.components.molecules.AudioBottomBar
import com.arshadshah.nimaz.presentation.components.molecules.MushafPageBar
import com.arshadshah.nimaz.presentation.components.molecules.NimazDropdownMenu
import com.arshadshah.nimaz.presentation.components.molecules.NimazDropdownRow
import com.arshadshah.nimaz.presentation.components.molecules.SurahHeaderCartouche
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.MushafLineType
import com.arshadshah.nimaz.domain.model.MushafPageLayout
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.presentation.components.organisms.AyahItem
import com.arshadshah.nimaz.presentation.components.organisms.MushafLinePage
import com.arshadshah.nimaz.presentation.components.organisms.MushafPage
import com.arshadshah.nimaz.presentation.components.organisms.TajweedLegendSheet
import com.arshadshah.nimaz.presentation.viewmodel.QuranEvent
import com.arshadshah.nimaz.presentation.viewmodel.QuranReaderUiState
import com.arshadshah.nimaz.presentation.viewmodel.QuranViewModel
import com.arshadshah.nimaz.presentation.viewmodel.ReadingMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranReaderScreen(
    surahNumber: Int? = null,
    juzNumber: Int? = null,
    pageNumber: Int? = null,
    initialAyahNumber: Int = 1,
    onNavigateBack: () -> Unit,
    onNavigateToQuranSettings: () -> Unit = {},
    onNavigateToTafseer: (surahNumber: Int, ayahNumber: Int) -> Unit = { _, _ -> },
    onNavigateToNextSurah: (Int) -> Unit = {},
    onPageModeChanged: (Boolean) -> Unit = {},
    viewModel: QuranViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.readerState.collectAsState()
    val audioState by viewModel.audioState.collectAsState()
    val homeState by viewModel.homeState.collectAsState()
    val surahByNumber = remember(homeState.surahs) { homeState.surahs.associateBy { it.number } }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var usePageView by rememberSaveable { mutableStateOf(false) }
    var showTajweedLegend by remember { mutableStateOf(false) }
    var savedListIndex by rememberSaveable { mutableIntStateOf(0) }
    var savedListOffset by rememberSaveable { mutableIntStateOf(0) }
    var pendingScrollRestore by rememberSaveable { mutableStateOf(false) }

    // Notify parent when page mode changes (for adaptive layout to collapse side panel)
    val isInPageMode = state.readingMode == ReadingMode.PAGE || usePageView
    LaunchedEffect(isInPageMode) {
        onPageModeChanged(isInPageMode)
    }

    // Keep screen on based on settings
    DisposableEffect(state.keepScreenOn) {
        val window = (context as? ComponentActivity)?.window
        if (state.keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Save reading position when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            val ayahs = when (state.readingMode) {
                ReadingMode.SURAH -> state.surahWithAyahs?.ayahs ?: emptyList()
                ReadingMode.JUZ, ReadingMode.PAGE -> state.ayahs
            }
            if (ayahs.isNotEmpty()) {
                val idx = (listState.firstVisibleItemIndex - 1).coerceIn(0, ayahs.size - 1)
                val ayah = ayahs[idx]
                viewModel.onEvent(
                    QuranEvent.UpdateReadingPosition(
                        surah = ayah.surahNumber,
                        ayah = ayah.numberInSurah,
                        page = ayah.page,
                        juz = ayah.juz
                    )
                )
            }
        }
    }

    // Load based on which param is provided
    LaunchedEffect(surahNumber, juzNumber, pageNumber) {
        when {
            juzNumber != null -> viewModel.onEvent(QuranEvent.LoadJuz(juzNumber))
            pageNumber != null -> viewModel.onEvent(QuranEvent.LoadPage(pageNumber))
            surahNumber != null -> viewModel.onEvent(QuranEvent.LoadSurah(surahNumber))
        }
    }

    // Save position when entering page view, restore data when leaving
    LaunchedEffect(usePageView) {
        if (usePageView) {
            savedListIndex = listState.firstVisibleItemIndex
            savedListOffset = listState.firstVisibleItemScrollOffset
        } else {
            pendingScrollRestore = true
            when {
                surahNumber != null -> viewModel.onEvent(QuranEvent.LoadSurah(surahNumber))
                juzNumber != null -> viewModel.onEvent(QuranEvent.LoadJuz(juzNumber))
            }
        }
    }

    // Restore scroll position after data reloads
    LaunchedEffect(pendingScrollRestore, state.isLoading) {
        if (pendingScrollRestore && !state.isLoading && !usePageView) {
            listState.scrollToItem(savedListIndex, savedListOffset)
            pendingScrollRestore = false
        }
    }

    // Track reading position (all modes: SURAH, JUZ, PAGE)
    val currentAyahIndex by remember {
        derivedStateOf { listState.firstVisibleItemIndex }
    }

    LaunchedEffect(currentAyahIndex, state.readingMode) {
        val ayahs = when (state.readingMode) {
            ReadingMode.SURAH -> state.surahWithAyahs?.ayahs ?: return@LaunchedEffect
            ReadingMode.JUZ, ReadingMode.PAGE -> state.ayahs
        }
        if (ayahs.isEmpty()) return@LaunchedEffect

        val ayahIdx = (currentAyahIndex - 1).coerceIn(0, ayahs.size - 1)
        val ayah = ayahs[ayahIdx]

        viewModel.onEvent(
            QuranEvent.UpdateReadingPosition(
                surah = ayah.surahNumber,
                ayah = ayah.numberInSurah,
                page = ayah.page,
                juz = ayah.juz
            )
        )
    }

    // Auto-scroll to currently playing ayah
    LaunchedEffect(audioState.currentAyahId) {
        if (audioState.currentAyahId > 0) {
            val displayAyahs = when (state.readingMode) {
                ReadingMode.SURAH -> state.surahWithAyahs?.ayahs ?: emptyList()
                ReadingMode.JUZ, ReadingMode.PAGE -> state.ayahs
            }
            val idx = displayAyahs.indexOfFirst { it.id == audioState.currentAyahId }
            if (idx >= 0) {
                listState.animateScrollToItem(idx + 1)
            }
        }
    }

    // Scroll to initial ayah when content first loads (for search/bookmarks/favorites navigation)
    LaunchedEffect(state.surahWithAyahs, initialAyahNumber) {
        if (initialAyahNumber > 1 && state.readingMode == ReadingMode.SURAH) {
            val ayahs = state.surahWithAyahs?.ayahs ?: return@LaunchedEffect
            val idx = ayahs.indexOfFirst { it.numberInSurah == initialAyahNumber }
            if (idx >= 0) {
                listState.animateScrollToItem(idx + 1) // +1 for banner
            }
        }
    }

    // No longer force list view when khatam is active - page view now supports khatam via bottom sheet

    val favoriteAyahIds = state.favoriteAyahIds

    val displayAyahs = when (state.readingMode) {
        ReadingMode.SURAH -> state.surahWithAyahs?.ayahs ?: emptyList()
        ReadingMode.JUZ, ReadingMode.PAGE -> state.ayahs
    }

    // Full-ayah lookup (by global id) used to resolve translation/copy/share content for a
    // 16-line word/page — best-effort from the Madani page cache; 6/7 makes the ayah source
    // fully script-aware.
    val ayahById = remember(state.pageCache) {
        state.pageCache.values.flatten().associateBy { it.id }
    }

    val headerTitle = when (state.readingMode) {
        ReadingMode.SURAH -> state.surahWithAyahs?.surah?.nameEnglish ?: ""
        ReadingMode.JUZ -> state.title
        ReadingMode.PAGE -> stringResource(R.string.al_quran)
    }
    val headerSubtitle = when (state.readingMode) {
        ReadingMode.PAGE -> ""
        else -> state.subtitle
    }
    val headerLoading = state.isLoading && state.readingMode != ReadingMode.PAGE

    // Page mode state — HorizontalPager with RTL layout so swipe-left = next page.
    // Page count follows the active Mushaf edition (604 Uthmani vs 548 IndoPak-16, #270).
    val totalPages = state.mushafScript.totalPages

    // Dual-page mode: tablet (sw >= 600dp) in landscape orientation
    val configuration = LocalConfiguration.current
    val isDualPageMode = configuration.smallestScreenWidthDp >= 600
            && configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // In dual mode: one spread per two pages (spread i → right page = 2i+1, left page = 2i+2)
    val pagerPageCount = if (isDualPageMode) (totalPages + 1) / 2 else totalPages

    // Stable initial page — computed once when entering page view
    val initialPageForPager = remember(state.readingMode, usePageView) {
        when {
            state.readingMode == ReadingMode.PAGE && pageNumber != null -> pageNumber
            usePageView && displayAyahs.isNotEmpty() -> displayAyahs.first().page
            else -> null
        }
    }

    // pagerState uses 0-based index
    val pagerState = if (initialPageForPager != null) {
        val initialIndex = if (isDualPageMode) {
            (initialPageForPager - 1) / 2 // spread index
        } else {
            initialPageForPager - 1
        }
        rememberNimazPagerState(
            initialPage = initialIndex,
            pageCount = { pagerPageCount }
        )
    } else {
        null
    }

    // Scroll pager when pageNumber changes (e.g. external navigation, bookmark, search)
    pagerState?.let { ps ->
        LaunchedEffect(pageNumber, isDualPageMode) {
            if (pageNumber != null) {
                val targetIndex = if (isDualPageMode) {
                    (pageNumber - 1) / 2
                } else {
                    pageNumber - 1
                }
                if (ps.currentPage != targetIndex) {
                    ps.animateScrollToPage(targetIndex)
                }
            }
        }
    }

    // Load page(s) when pager settles
    pagerState?.let { ps ->
        val settledIndex = ps.settledPage
        LaunchedEffect(settledIndex, isDualPageMode) {
            if (isDualPageMode) {
                val rightPage = settledIndex * 2 + 1 // lower page number (right side)
                val leftPage = settledIndex * 2 + 2  // higher page number (left side)
                if (rightPage in 1..totalPages) {
                    viewModel.onEvent(QuranEvent.LoadPage(rightPage))
                }
                if (leftPage in 1..totalPages) {
                    viewModel.onEvent(QuranEvent.LoadPage(leftPage))
                }
            } else {
                val settledPage = settledIndex + 1
                if (settledPage in 1..totalPages) {
                    viewModel.onEvent(QuranEvent.LoadPage(settledPage))
                }
            }
        }
    }

    NimazScreenScaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        if (headerLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = headerTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (headerSubtitle.isNotEmpty()) {
                                Text(
                                    text = headerSubtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        NimazIcon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                actions = {
                    // Khatam progress indicator
                    if (state.activeKhatamId != null) {
                        val khatamProgress =
                            state.khatamReadAyahIds.size.toFloat() / com.arshadshah.nimaz.domain.model.Khatam.TOTAL_QURAN_AYAHS
                        Box(
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = { khatamProgress },
                                modifier = Modifier.size(28.dp),
                                color = MaterialTheme.colorScheme.tertiary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                strokeWidth = 3.dp
                            )
                            Text(
                                text = "${(khatamProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    // Reader controls collapsed into one overflow menu so the bar stays
                    // minimal and nothing floats over the Arabic text. The view toggle is
                    // only offered where switching is meaningful (dedicated page mode has
                    // nothing to toggle to).
                    var menuExpanded by remember { mutableStateOf(false) }
                    val canToggleView = usePageView ||
                        state.readingMode == ReadingMode.SURAH ||
                        state.readingMode == ReadingMode.JUZ

                    IconButton(onClick = { menuExpanded = true }) {
                        NimazIcon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.cd_more_options)
                        )
                    }
                    NimazDropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        if (canToggleView) {
                            NimazDropdownRow(
                                text = if (usePageView) {
                                    stringResource(R.string.cd_switch_to_list_view)
                                } else {
                                    stringResource(R.string.cd_switch_to_page_view)
                                },
                                leadingIcon = if (usePageView) {
                                    Icons.AutoMirrored.Filled.ViewList
                                } else {
                                    Icons.Default.AutoStories
                                },
                                onClick = {
                                    usePageView = !usePageView
                                    menuExpanded = false
                                },
                            )
                        }
                        if (state.showTajweed) {
                            NimazDropdownRow(
                                text = stringResource(R.string.tajweed_colour_guide),
                                leadingIcon = Icons.AutoMirrored.Filled.MenuBook,
                                onClick = {
                                    menuExpanded = false
                                    showTajweedLegend = true
                                },
                            )
                        }
                        NimazDropdownRow(
                            text = stringResource(R.string.cd_settings),
                            leadingIcon = Icons.Default.Settings,
                            onClick = {
                                menuExpanded = false
                                onNavigateToQuranSettings()
                            },
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            // In page mode, use current page's ayahs for audio playback. In 16-line mode the
            // pager index is an IndoPak page number (1-548) — an unrelated scheme from the
            // Madani-keyed pageCache (populated via QuranEvent.LoadPage) — so it must be
            // resolved from the IndoPak layout cache instead, or the bar shows/plays the wrong
            // ayah (#280 review).
            val currentPageAyahsForAudio = if (pagerState != null) {
                val currentQuranPageForAudio = if (isDualPageMode) {
                    pagerState.settledPage * 2 + 1 // right page of current spread
                } else {
                    pagerState.settledPage + 1
                }
                if (state.useLineAccurateLayout) {
                    state.mushafPageLayoutCache[currentQuranPageForAudio]
                        ?.let { buildOrderedPageAyahsFromLayout(it, ayahById) }
                        ?: displayAyahs
                } else {
                    state.pageCache[currentQuranPageForAudio] ?: displayAyahs
                }
            } else {
                displayAyahs
            }

            // When audio is active, the bar should track the highlighted (audible)
            // ayah — sourced from audioState so it matches the highlight without
            // depending on the auto-scroll animation settling. Scroll position is
            // only the right source for "play from here" before playback starts.
            val playingAyah = if (audioState.isActive && audioState.currentAyahId > 0) {
                displayAyahs.find { it.id == audioState.currentAyahId }
            } else null

            val currentReaderAyah = playingAyah ?: when {
                pagerState != null -> currentPageAyahsForAudio.firstOrNull()
                displayAyahs.isNotEmpty() -> {
                    val idx = (listState.firstVisibleItemIndex - 1)
                        .coerceIn(0, displayAyahs.lastIndex)
                    displayAyahs[idx]
                }

                else -> null
            }

            val readerSurah = currentReaderAyah?.let { surahByNumber[it.surahNumber] }
            val readerSurahName = readerSurah?.nameEnglish
                ?: state.surahWithAyahs?.surah?.nameEnglish
                ?: ""
            val readerTotalAyahs = readerSurah?.ayahCount ?: 0

            AudioBottomBar(
                isAudioActive = audioState.isActive,
                isPlaying = audioState.isPlaying,
                isDownloading = audioState.isDownloading,
                isPreparing = audioState.isPreparing,
                downloadProgress = audioState.downloadProgress,
                downloadedCount = audioState.downloadedCount,
                totalToDownload = audioState.totalToDownload,
                surahName = readerSurahName,
                currentAyahInSurah = currentReaderAyah?.numberInSurah ?: 0,
                totalAyahsInSurah = readerTotalAyahs,
                pageNumber = currentReaderAyah?.page ?: 0,
                juzNumber = currentReaderAyah?.juz ?: 0,
                onPlayClick = {
                    if (audioState.isPlaying) {
                        viewModel.onEvent(QuranEvent.PauseAudio)
                    } else if (audioState.isActive && !audioState.isPreparing) {
                        viewModel.onEvent(QuranEvent.ResumeAudio)
                    } else if (!audioState.isPreparing && currentReaderAyah != null) {
                        // Start audio from the currently displayed ayah, not the
                        // surah's first — preserves the reader's context.
                        viewModel.onEvent(
                            QuranEvent.PlayAyahAudio(
                                ayahGlobalId = currentReaderAyah.id,
                                surahNumber = currentReaderAyah.surahNumber,
                                ayahNumber = currentReaderAyah.numberInSurah
                            )
                        )
                    }
                },
                onStopClick = { viewModel.onEvent(QuranEvent.StopAudio) }
            )
        },
        floatingActionButton = {
            // Khatam bulk-completion action. Floated (rather than an item pinned to
            // the top of the list) so it stays reachable on long surahs without
            // scrolling back up — issue #260. Icon-only so it stays compact and never
            // spans the content the way the old extended (labelled) FAB did — the label
            // is carried by the icon's contentDescription instead. Only meaningful in the
            // surah list view; page view drives khatam per page from MushafPageBar.
            val khatamSurah = state.surahWithAyahs
            if (
                state.activeKhatamId != null &&
                state.readingMode == ReadingMode.SURAH &&
                !usePageView &&
                khatamSurah != null
            ) {
                val surahAyahIds = remember(khatamSurah.ayahs) {
                    khatamSurah.ayahs.map { it.id }.toSet()
                }
                val allRead = surahAyahIds.isNotEmpty() &&
                    surahAyahIds.all { it in state.khatamReadAyahIds }

                when {
                    !allRead -> FloatingActionButton(
                        onClick = {
                            viewModel.onEvent(
                                QuranEvent.MarkSurahAsReadForKhatam(khatamSurah.surah.number)
                            )
                        },
                    ) {
                        NimazIcon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = stringResource(R.string.quran_mark_all_read),
                        )
                    }

                    khatamSurah.surah.number < 114 -> FloatingActionButton(
                        onClick = { onNavigateToNextSurah(khatamSurah.surah.number + 1) },
                    ) {
                        NimazIcon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = stringResource(R.string.quran_continue_next_surah),
                        )
                    }
                }
            }
        },
        // Opts out of the app ornament: long-form Arabic needs a plain backdrop.
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isLoading && state.readingMode != ReadingMode.PAGE && !usePageView) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (pagerState != null && (state.readingMode == ReadingMode.PAGE || usePageView)) {
                // Page mode with HorizontalPager (RTL so swipe-left = next page)
                val surahMap = surahByNumber

                // Current Quran page numbers (1-based)
                val currentRightPage = if (isDualPageMode) {
                    pagerState.settledPage * 2 + 1
                } else {
                    pagerState.settledPage + 1
                }
                val currentLeftPage = if (isDualPageMode) {
                    (pagerState.settledPage * 2 + 2).coerceAtMost(totalPages)
                } else {
                    currentRightPage
                }

                // Ayahs for the current page(s) (used for khatam & info bar)
                val currentPageAyahs = if (isDualPageMode) {
                    val rightAyahs = state.pageCache[currentRightPage] ?: emptyList()
                    val leftAyahs = state.pageCache[currentLeftPage] ?: emptyList()
                    rightAyahs + leftAyahs
                } else {
                    state.pageCache[currentRightPage] ?: displayAyahs.takeIf {
                        displayAyahs.firstOrNull()?.page == currentRightPage
                    } ?: emptyList()
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    // Compact info/nav bar (fixed, above pager)
                    MushafPageBar(
                        pageNumber = currentRightPage,
                        secondPageNumber = if (isDualPageMode && currentLeftPage != currentRightPage) currentLeftPage else null,
                        totalPages = totalPages,
                        ayahs = currentPageAyahs,
                        isKhatamActive = state.activeKhatamId != null,
                        khatamReadAyahIds = state.khatamReadAyahIds,
                        onKhatamTogglePage = { pageAyahs ->
                            viewModel.onEvent(
                                QuranEvent.TogglePageKhatam(pageAyahs.map { it.id })
                            )
                        },
                        onNavigatePrevious = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        },
                        onNavigateNext = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    )

                    // RTL HorizontalPager — page 1 on the right, swipe left for next
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        NimazPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            beyondViewportPageCount = 1,
                            key = { it }
                        ) { pageIndex ->
                            // Restore LTR for page content
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                val highlightedAyahId =
                                    if (audioState.isActive) audioState.currentAyahId else null

                                if (isDualPageMode) {
                                    // Dual-page spread: right page (lower number) + left page (higher number)
                                    val rightPageNum = pageIndex * 2 + 1
                                    val leftPageNum = (pageIndex * 2 + 2).coerceAtMost(totalPages)

                                    val rightPageAyahs =
                                        state.pageCache[rightPageNum] ?: emptyList()
                                    val leftPageAyahs = state.pageCache[leftPageNum] ?: emptyList()

                                    LaunchedEffect(rightPageNum) {
                                        if (rightPageNum !in state.pageCache.keys) {
                                            viewModel.onEvent(QuranEvent.LoadPage(rightPageNum))
                                        }
                                    }
                                    LaunchedEffect(leftPageNum) {
                                        if (leftPageNum !in state.pageCache.keys && leftPageNum != rightPageNum) {
                                            viewModel.onEvent(QuranEvent.LoadPage(leftPageNum))
                                        }
                                    }

                                    Row(modifier = Modifier.fillMaxSize()) {
                                        // Left page (higher page number — displayed on the left in a physical Mushaf)
                                        if (leftPageNum != rightPageNum) {
                                            ReaderMushafPage(
                                                pageNumber = leftPageNum,
                                                ayahs = leftPageAyahs,
                                                surahMap = surahMap,
                                                state = state,
                                                highlightedAyahId = highlightedAyahId,
                                                favoriteAyahIds = favoriteAyahIds,
                                                ayahById = ayahById,
                                                onEvent = viewModel::onEvent,
                                                onNavigateToTafseer = onNavigateToTafseer,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                                    .background(MaterialTheme.colorScheme.background)
                                            )
                                        }

                                        // Spine divider
                                        VerticalDivider(
                                            modifier = Modifier.fillMaxHeight(),
                                            thickness = 1.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant
                                        )

                                        // Right page (lower page number — displayed on the right in a physical Mushaf)
                                        ReaderMushafPage(
                                            pageNumber = rightPageNum,
                                            ayahs = rightPageAyahs,
                                            surahMap = surahMap,
                                            state = state,
                                            highlightedAyahId = highlightedAyahId,
                                            favoriteAyahIds = favoriteAyahIds,
                                            ayahById = ayahById,
                                            onEvent = viewModel::onEvent,
                                            onNavigateToTafseer = onNavigateToTafseer,
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .background(MaterialTheme.colorScheme.background)
                                        )
                                    }
                                } else {
                                    // Single-page mode
                                    val pageNum = pageIndex + 1

                                    val pageAyahs =
                                        state.pageCache[pageNum] ?: displayAyahs.takeIf {
                                            displayAyahs.firstOrNull()?.page == pageNum
                                        } ?: emptyList()

                                    LaunchedEffect(pageNum) {
                                        if (pageNum !in state.pageCache.keys) {
                                            viewModel.onEvent(QuranEvent.LoadPage(pageNum))
                                        }
                                    }

                                    ReaderMushafPage(
                                        pageNumber = pageNum,
                                        ayahs = pageAyahs,
                                        surahMap = surahMap,
                                        state = state,
                                        highlightedAyahId = highlightedAyahId,
                                        favoriteAyahIds = favoriteAyahIds,
                                        ayahById = ayahById,
                                        onEvent = viewModel::onEvent,
                                        onNavigateToTafseer = onNavigateToTafseer,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.background)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Surah/Juz mode: standard LazyColumn
                val surahStartIds = remember(displayAyahs) {
                    if (displayAyahs.isEmpty()) emptySet()
                    else {
                        val ids = mutableSetOf<Int>()
                        var lastSurah = -1
                        for (ayah in displayAyahs) {
                            if (ayah.surahNumber != lastSurah) {
                                ids.add(ayah.id)
                                lastSurah = ayah.surahNumber
                            }
                        }
                        ids
                    }
                }

                // Leave room under the last ayah for the floating khatam action so it
                // never covers content (issue #260).
                val listBottomPadding =
                    if (state.activeKhatamId != null && state.readingMode == ReadingMode.SURAH) {
                        88.dp
                    } else {
                        16.dp
                    }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = listBottomPadding)
                ) {
                    // Surah Banner or Juz Banner
                    if (state.readingMode == ReadingMode.SURAH) {
                        state.surahWithAyahs?.let { surahWithAyahs ->
                            item(key = "banner") {
                                SurahHeaderCartouche(
                                    surah = surahWithAyahs.surah,
                                    modifier = Modifier.padding(
                                        horizontal = 12.dp,
                                        vertical = 8.dp
                                    ),
                                    showBismillah = (surahNumber ?: 0) != 9 && (surahNumber
                                        ?: 0) != 1
                                )
                            }
                        }
                    }

                    // Ayahs
                    items(
                        items = displayAyahs,
                        key = { it.id }
                    ) { ayah ->
                        if (state.readingMode == ReadingMode.JUZ && ayah.id in surahStartIds) {
                            val surah = surahByNumber[ayah.surahNumber]
                            PageSurahSeparator(
                                surahNumber = ayah.surahNumber,
                                surahNameArabic = surah?.nameArabic ?: "",
                                surahNameEnglish = surah?.nameEnglish
                                    ?: stringResource(
                                        R.string.surah_number_format,
                                        ayah.surahNumber
                                    ),
                                showBismillah = ayah.numberInSurah == 1 && ayah.surahNumber != 1 && ayah.surahNumber != 9
                            )
                        }

                        val isHighlighted =
                            audioState.currentAyahId == ayah.id && audioState.isActive
                        val isAudioPlaying = isHighlighted && audioState.isPlaying

                        AyahItem(
                            ayah = ayah,
                            showTranslation = state.showTranslation,
                            showTransliteration = state.showTransliteration,
                            arabicFontSize = state.arabicFontSize,
                            arabicFontFamily = state.arabicFontFamily,
                            fontSize = state.fontSize,
                            isHighlighted = isHighlighted,
                            isAudioPlaying = isAudioPlaying,
                            isFavorite = ayah.id in favoriteAyahIds,
                            isKhatamRead = ayah.id in state.khatamReadAyahIds,
                            isKhatamMode = state.activeKhatamId != null,
                            showTajweed = state.showTajweed,
                            tajweedUnderline = state.tajweedUnderline,
                            onBookmarkClick = {
                                viewModel.onEvent(
                                    QuranEvent.ToggleBookmark(
                                        ayahId = ayah.id,
                                        surahNumber = ayah.surahNumber,
                                        ayahNumber = ayah.numberInSurah
                                    )
                                )
                            },
                            onFavoriteClick = {
                                viewModel.onEvent(
                                    QuranEvent.ToggleFavorite(
                                        ayahId = ayah.id,
                                        surahNumber = ayah.surahNumber,
                                        ayahNumber = ayah.numberInSurah
                                    )
                                )
                            },
                            onPlayAyahClick = {
                                viewModel.onEvent(
                                    QuranEvent.PlayAyahAudio(
                                        ayahGlobalId = ayah.id,
                                        surahNumber = ayah.surahNumber,
                                        ayahNumber = ayah.numberInSurah
                                    )
                                )
                            },
                            onTafseerClick = {
                                onNavigateToTafseer(ayah.surahNumber, ayah.numberInSurah)
                            },
                            onKhatamToggle = {
                                viewModel.onEvent(QuranEvent.ToggleKhatamAyah(ayah.id))
                            }
                        )
                    }
                }
            }
        }
    }

    // Tajweed colour guide, reachable from the reader's overflow menu (#294).
    if (showTajweedLegend) {
        TajweedLegendSheet(onDismiss = { showTajweedLegend = false })
    }
}

/**
 * Renders one Quran page inside the reader pager, choosing the renderer by the active Mushaf
 * script: the line-accurate 16-line IndoPak page ([MushafLinePage], 5/7 of #263) when
 * [QuranReaderUiState.useLineAccurateLayout] is set, otherwise the default Uthmani page
 * ([MushafPage]). Centralises the (identical) interaction wiring the single- and dual-page
 * call sites used to duplicate.
 *
 * In 16-line mode it lazily loads the page's [com.arshadshah.nimaz.domain.model.MushafPageLayout]
 * into the cache and shows a spinner until it arrives; [ayahById] supplies full-ayah content
 * (translation/copy/share) when available.
 */
@Composable
private fun ReaderMushafPage(
    pageNumber: Int,
    ayahs: List<Ayah>,
    surahMap: Map<Int, Surah>,
    state: QuranReaderUiState,
    highlightedAyahId: Int?,
    favoriteAyahIds: Set<Int>,
    ayahById: Map<Int, Ayah>,
    onEvent: (QuranEvent) -> Unit,
    onNavigateToTafseer: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.useLineAccurateLayout) {
        val layout = state.mushafPageLayoutCache[pageNumber]
        LaunchedEffect(pageNumber) {
            if (pageNumber !in state.mushafPageLayoutCache) {
                onEvent(QuranEvent.LoadMushafPageLayout(pageNumber))
            }
        }
        if (layout == null) {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            MushafLinePage(
                pageNumber = pageNumber,
                layout = layout,
                surahMap = surahMap,
                arabicFontSize = state.arabicFontSize,
                highlightedAyahId = highlightedAyahId,
                favoriteAyahIds = favoriteAyahIds,
                showTranslation = state.showTranslation,
                showTransliteration = state.showTransliteration,
                ayahLookup = { ayahById[it] },
                onBookmarkClick = { ayah ->
                    onEvent(
                        QuranEvent.ToggleBookmark(ayah.id, ayah.surahNumber, ayah.numberInSurah)
                    )
                },
                onFavoriteClick = { ayah ->
                    onEvent(
                        QuranEvent.ToggleFavorite(ayah.id, ayah.surahNumber, ayah.numberInSurah)
                    )
                },
                onPlayClick = { ayah ->
                    onEvent(
                        QuranEvent.PlayAyahAudio(ayah.id, ayah.surahNumber, ayah.numberInSurah)
                    )
                },
                onShareClick = { },
                onCopyClick = { },
                onTafseerClick = { ayah ->
                    onNavigateToTafseer(ayah.surahNumber, ayah.numberInSurah)
                },
                isKhatamActive = state.activeKhatamId != null,
                khatamReadAyahIds = state.khatamReadAyahIds,
                onKhatamToggle = { ayah -> onEvent(QuranEvent.ToggleKhatamAyah(ayah.id)) },
                modifier = modifier,
            )
        }
    } else {
        MushafPage(
            pageNumber = pageNumber,
            ayahs = ayahs,
            surahMap = surahMap,
            arabicFontSize = state.arabicFontSize,
            arabicFontFamily = state.arabicFontFamily,
            highlightedAyahId = highlightedAyahId,
            favoriteAyahIds = favoriteAyahIds,
            showTajweed = state.showTajweed,
            tajweedUnderline = state.tajweedUnderline,
            showTranslation = state.showTranslation,
            showTransliteration = state.showTransliteration,
            onBookmarkClick = { ayah ->
                onEvent(QuranEvent.ToggleBookmark(ayah.id, ayah.surahNumber, ayah.numberInSurah))
            },
            onFavoriteClick = { ayah ->
                onEvent(QuranEvent.ToggleFavorite(ayah.id, ayah.surahNumber, ayah.numberInSurah))
            },
            onPlayClick = { ayah ->
                onEvent(QuranEvent.PlayAyahAudio(ayah.id, ayah.surahNumber, ayah.numberInSurah))
            },
            onShareClick = { },
            onCopyClick = { },
            onTafseerClick = { ayah ->
                onNavigateToTafseer(ayah.surahNumber, ayah.numberInSurah)
            },
            isKhatamActive = state.activeKhatamId != null,
            khatamReadAyahIds = state.khatamReadAyahIds,
            onKhatamToggle = { ayah -> onEvent(QuranEvent.ToggleKhatamAyah(ayah.id)) },
            modifier = modifier,
        )
    }
}

/**
 * Reconstructs the ordered list of distinct ayahs printed on a 16-line Mushaf [layout] page,
 * preferring full ayah content from [ayahById] (the Madani-keyed page cache) and falling back
 * to a minimal layout-derived [Ayah] when the id isn't cached yet — mirrors the per-tap
 * reconstruction [com.arshadshah.nimaz.presentation.components.organisms.MushafLinePage] does,
 * so the audio bottom bar reads the same IndoPak page it renders instead of the unrelated
 * Madani page at the same page number (#280 review).
 */
internal fun buildOrderedPageAyahsFromLayout(
    layout: MushafPageLayout,
    ayahById: Map<Int, Ayah>
): List<Ayah> {
    val result = mutableListOf<Ayah>()
    val seen = mutableSetOf<Int>()
    for (line in layout.lines) {
        if (line.type != MushafLineType.AYAH) continue
        for (word in line.words) {
            if (!seen.add(word.ayahId)) continue
            result.add(
                ayahById[word.ayahId] ?: Ayah(
                    id = word.ayahId,
                    surahNumber = line.surahId,
                    ayahNumber = word.ayahNumber,
                    textArabic = "",
                    textSimple = "",
                    juzNumber = 0,
                    hizbNumber = 0,
                    rubNumber = 0,
                    pageNumber = layout.page,
                    sajdaType = null,
                    sajdaNumber = null,
                )
            )
        }
    }
    return result
}
