package com.arshadshah.nimaz.presentation.screens.quran

import android.content.Intent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.core.util.TajweedParser
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.SajdaType
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.QuranVerseText
import com.arshadshah.nimaz.presentation.components.atoms.toArabicNumber
import com.arshadshah.nimaz.presentation.components.organisms.MushafPage
import com.arshadshah.nimaz.presentation.theme.AmiriFontFamily
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.viewmodel.QuranEvent
import com.arshadshah.nimaz.presentation.viewmodel.QuranViewModel
import com.arshadshah.nimaz.presentation.viewmodel.ReadingMode
import kotlinx.coroutines.launch

// Bismillah text to strip from first ayah (uses alef wasla ٱ as in database)
private const val BISMILLAH_TEXT = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"

/**
 * Strip bismillah from first ayah's Arabic text for all surahs EXCEPT:
 * - Surah 1 (Al-Fatiha) - bismillah IS ayah 1
 * - Surah 9 (At-Tawbah) - has no bismillah
 */
private fun Ayah.getDisplayArabicText(): String {
    return if (numberInSurah == 1 && surahNumber != 1 && surahNumber != 9) {
        textArabic
            .removePrefix("$BISMILLAH_TEXT ")
            .removePrefix(BISMILLAH_TEXT)
            .trim()
    } else {
        textArabic
    }
}

/**
 * Process ayah text to append Arabic numeral with ornamental brackets at the end
 */
private fun formatAyahWithEndMarker(arabicText: String, ayahNumber: Int): String {
    return "$arabicText ${formatAyahEndMarker(ayahNumber)}"
}

/**
 * Format just the ayah end marker with ornamental brackets
 */
private fun formatAyahEndMarker(ayahNumber: Int): String {
    val unicodeAyaEndStart = "\uFD3F" // ﴿
    val unicodeAyaEndEnd = "\uFD3E"   // ﴾
    val arabicNumber = toArabicNumber(ayahNumber)
    return "$unicodeAyaEndStart$arabicNumber$unicodeAyaEndEnd"
}

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
    viewModel: QuranViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.readerState.collectAsState()
    val audioState by viewModel.audioState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var usePageView by rememberSaveable { mutableStateOf(false) }
    var savedListIndex by rememberSaveable { mutableIntStateOf(0) }
    var savedListOffset by rememberSaveable { mutableIntStateOf(0) }
    var pendingScrollRestore by rememberSaveable { mutableStateOf(false) }

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

    val headerTitle = when (state.readingMode) {
        ReadingMode.SURAH -> state.surahWithAyahs?.surah?.nameEnglish ?: ""
        ReadingMode.JUZ -> state.title
        ReadingMode.PAGE -> "Al Quran"
    }
    val headerSubtitle = when (state.readingMode) {
        ReadingMode.PAGE -> ""
        else -> state.subtitle
    }
    val headerLoading = state.isLoading && state.readingMode != ReadingMode.PAGE

    // Page mode state — HorizontalPager with RTL layout so swipe-left = next page
    val totalPages = 604

    // Stable initial page — computed once when entering page view
    val initialPageForPager = remember(state.readingMode, usePageView) {
        when {
            state.readingMode == ReadingMode.PAGE && pageNumber != null -> pageNumber
            usePageView && displayAyahs.isNotEmpty() -> displayAyahs.first().page
            else -> null
        }
    }

    // pagerState uses 0-based index; page 1 = index 0, page 604 = index 603
    val pagerState = if (initialPageForPager != null) {
        rememberPagerState(
            initialPage = initialPageForPager - 1,
            pageCount = { totalPages }
        )
    } else {
        null
    }

    // Load page when pager settles on a new page
    pagerState?.let { ps ->
        val settledPage = ps.settledPage + 1 // 1-based Quran page
        LaunchedEffect(settledPage) {
            if (settledPage in 1..totalPages) {
                viewModel.onEvent(QuranEvent.LoadPage(settledPage))
            }
        }
    }

    Scaffold(
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
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Khatam progress indicator
                    if (state.activeKhatamId != null) {
                        val khatamProgress = if (com.arshadshah.nimaz.domain.model.Khatam.TOTAL_QURAN_AYAHS > 0)
                            state.khatamReadAyahIds.size.toFloat() / com.arshadshah.nimaz.domain.model.Khatam.TOTAL_QURAN_AYAHS
                        else 0f
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
                    if (usePageView || state.readingMode == ReadingMode.SURAH || state.readingMode == ReadingMode.JUZ) {
                        IconButton(onClick = { usePageView = !usePageView }) {
                            Icon(
                                imageVector = if (usePageView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.AutoStories,
                                contentDescription = if (usePageView) "Switch to list view" else "Switch to page view"
                            )
                        }
                    }
                    IconButton(onClick = onNavigateToQuranSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
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
            // In page mode, use current page's ayahs for audio playback
            val currentPageAyahsForAudio = if (pagerState != null) {
                val currentQuranPageForAudio = pagerState.settledPage + 1
                state.pageCache[currentQuranPageForAudio] ?: displayAyahs
            } else {
                displayAyahs
            }

            AudioBottomBar(
                isAudioActive = audioState.isActive,
                isPlaying = audioState.isPlaying,
                isDownloading = audioState.isDownloading,
                isPreparing = audioState.isPreparing,
                downloadProgress = audioState.downloadProgress,
                downloadedCount = audioState.downloadedCount,
                totalToDownload = audioState.totalToDownload,
                audioTitle = audioState.currentSubtitle ?: audioState.currentTitle,
                progress = if (audioState.duration > 0) audioState.position.toFloat() / audioState.duration else 0f,
                onPlayClick = {
                    if (audioState.isPlaying) {
                        viewModel.onEvent(QuranEvent.PauseAudio)
                    } else if (audioState.isActive && !audioState.isPreparing) {
                        viewModel.onEvent(QuranEvent.ResumeAudio)
                    } else if (!audioState.isPreparing) {
                        if (state.readingMode == ReadingMode.SURAH && surahNumber != null && pagerState == null) {
                            val name = state.surahWithAyahs?.surah?.nameEnglish ?: "Surah $surahNumber"
                            viewModel.onEvent(QuranEvent.PlaySurahAudio(surahNumber, name))
                        } else if (currentPageAyahsForAudio.isNotEmpty()) {
                            viewModel.onEvent(
                                QuranEvent.PlayAyahAudio(
                                    ayahGlobalId = currentPageAyahsForAudio.first().id,
                                    surahNumber = currentPageAyahsForAudio.first().surahNumber,
                                    ayahNumber = currentPageAyahsForAudio.first().ayahNumber
                                )
                            )
                        }
                    }
                },
                onStopClick = { viewModel.onEvent(QuranEvent.StopAudio) }
            )
        },
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
                val homeState by viewModel.homeState.collectAsState()

                val surahMap = remember(homeState.surahs) {
                    homeState.surahs.associateBy { it.number }
                }

                // Current Quran page number (1-based)
                val currentQuranPage = pagerState.settledPage + 1

                // Ayahs for the current page (used for khatam & info bar)
                val currentPageAyahs = state.pageCache[currentQuranPage] ?: displayAyahs.takeIf {
                    displayAyahs.firstOrNull()?.page == currentQuranPage
                } ?: emptyList()

                Column(modifier = Modifier.fillMaxSize()) {
                    // Compact info/nav bar (fixed, above pager)
                    MushafPageBar(
                        pageNumber = currentQuranPage,
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
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            beyondViewportPageCount = 1,
                            key = { it + 1 } // Use Quran page number as key
                        ) { pageIndex ->
                            // Restore LTR for page content
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                val pageNum = pageIndex + 1 // 1-based Quran page

                                val pageAyahs = state.pageCache[pageNum] ?: displayAyahs.takeIf {
                                    displayAyahs.firstOrNull()?.page == pageNum
                                } ?: emptyList()

                                LaunchedEffect(pageNum) {
                                    if (pageNum !in state.pageCache.keys) {
                                        viewModel.onEvent(QuranEvent.LoadPage(pageNum))
                                    }
                                }

                                val highlightedAyahId = if (audioState.isActive) audioState.currentAyahId else null

                                MushafPage(
                                    pageNumber = pageNum,
                                    ayahs = pageAyahs,
                                    surahMap = surahMap,
                                    arabicFontSize = state.arabicFontSize,
                                    highlightedAyahId = highlightedAyahId,
                                    favoriteAyahIds = favoriteAyahIds,
                                    showTajweed = state.showTajweed,
                                    showTranslation = state.showTranslation,
                                    showTransliteration = state.showTransliteration,
                                    onBookmarkClick = { ayah ->
                                        viewModel.onEvent(
                                            QuranEvent.ToggleBookmark(
                                                ayahId = ayah.id,
                                                surahNumber = ayah.surahNumber,
                                                ayahNumber = ayah.numberInSurah
                                            )
                                        )
                                    },
                                    onFavoriteClick = { ayah ->
                                        viewModel.onEvent(
                                            QuranEvent.ToggleFavorite(
                                                ayahId = ayah.id,
                                                surahNumber = ayah.surahNumber,
                                                ayahNumber = ayah.numberInSurah
                                            )
                                        )
                                    },
                                    onPlayClick = { ayah ->
                                        viewModel.onEvent(
                                            QuranEvent.PlayAyahAudio(
                                                ayahGlobalId = ayah.id,
                                                surahNumber = ayah.surahNumber,
                                                ayahNumber = ayah.numberInSurah
                                            )
                                        )
                                    },
                                    onShareClick = { },
                                    onCopyClick = { },
                                    onTafseerClick = { ayah ->
                                        onNavigateToTafseer(ayah.surahNumber, ayah.numberInSurah)
                                    },
                                    isKhatamActive = state.activeKhatamId != null,
                                    khatamReadAyahIds = state.khatamReadAyahIds,
                                    onKhatamToggle = { ayah ->
                                        viewModel.onEvent(QuranEvent.ToggleKhatamAyah(ayah.id))
                                    },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.background)
                                )
                            }
                        }
                    }
                }
            } else {
                // Surah/Juz mode: standard LazyColumn
                val homeState by viewModel.homeState.collectAsState()
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

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    // Surah Banner or Juz Banner
                    if (state.readingMode == ReadingMode.SURAH) {
                        state.surahWithAyahs?.let { surahWithAyahs ->
                            item(key = "banner") {
                                SurahBanner(
                                    surahNameArabic = surahWithAyahs.surah.nameArabic,
                                    surahNameEnglish = surahWithAyahs.surah.nameEnglish,
                                    surahMeaning = surahWithAyahs.surah.nameTransliteration,
                                    revelationType = surahWithAyahs.surah.revelationType,
                                    ayahCount = surahWithAyahs.surah.numberOfAyahs,
                                    showBismillah = (surahNumber ?: 0) != 9 && (surahNumber ?: 0) != 1
                                )
                            }

                            // Khatam mode: "Mark all as read" or "Continue to next surah"
                            if (state.activeKhatamId != null) {
                                item(key = "khatam_mark_surah") {
                                    val surahAyahIds = surahWithAyahs.ayahs.map { it.id }.toSet()
                                    val allRead = surahAyahIds.isNotEmpty() && surahAyahIds.all { it in state.khatamReadAyahIds }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 15.dp),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        if (allRead) {
                                            if (surahWithAyahs.surah.number < 114) {
                                                TextButton(
                                                    onClick = {
                                                        onNavigateToNextSurah(surahWithAyahs.surah.number + 1)
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Continue to next surah")
                                                }
                                            }
                                        } else {
                                            TextButton(
                                                onClick = {
                                                    viewModel.onEvent(
                                                        QuranEvent.MarkSurahAsReadForKhatam(surahWithAyahs.surah.number)
                                                    )
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.CheckCircle,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Mark all ayahs in this surah as read")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        item(key = "banner") {
                            JuzPageBanner(
                                title = state.title,
                                subtitle = state.subtitle
                            )
                        }
                    }

                    // Ayahs
                    items(
                        items = displayAyahs,
                        key = { it.id }
                    ) { ayah ->
                        if (state.readingMode == ReadingMode.JUZ && ayah.id in surahStartIds) {
                            val surah = homeState.surahs.find { it.number == ayah.surahNumber }
                            PageSurahSeparator(
                                surahNumber = ayah.surahNumber,
                                surahNameArabic = surah?.nameArabic ?: "",
                                surahNameEnglish = surah?.nameEnglish ?: "Surah ${ayah.surahNumber}",
                                showBismillah = ayah.numberInSurah == 1 && ayah.surahNumber != 1 && ayah.surahNumber != 9
                            )
                        }

                        val isHighlighted = audioState.currentAyahId == ayah.id && audioState.isActive

                        AyahItem(
                            ayah = ayah,
                            showTranslation = state.showTranslation,
                            showTransliteration = state.showTransliteration,
                            arabicFontSize = state.arabicFontSize,
                            fontSize = state.fontSize,
                            isHighlighted = isHighlighted,
                            isFavorite = ayah.id in favoriteAyahIds,
                            isKhatamRead = ayah.id in state.khatamReadAyahIds,
                            isKhatamMode = state.activeKhatamId != null,
                            showTajweed = state.showTajweed,
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
}

// ---------------------------------------------------------------------------
// Compact Mushaf Page Bar (fixed above PageCurl)
// ---------------------------------------------------------------------------
@Composable
private fun MushafPageBar(
    pageNumber: Int,
    totalPages: Int,
    ayahs: List<Ayah>,
    isKhatamActive: Boolean,
    khatamReadAyahIds: Set<Int>,
    onKhatamTogglePage: (List<Ayah>) -> Unit,
    onNavigatePrevious: () -> Unit,
    onNavigateNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val firstAyah = ayahs.firstOrNull()
    val juzNumber = firstAyah?.juz ?: 0
    val hizbNumber = firstAyah?.hizbNumber ?: 0

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous page (higher Quran page number)
            IconButton(
                onClick = onNavigatePrevious,
                enabled = pageNumber < totalPages,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous Page",
                    modifier = Modifier.size(22.dp)
                )
            }

            // Page info
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Page $pageNumber",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (juzNumber > 0) {
                    Text(
                        text = "  •  Juz $juzNumber",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (hizbNumber > 0) {
                    Text(
                        text = "  •  Hizb $hizbNumber",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Khatam toggle for the page
            if (isKhatamActive && ayahs.isNotEmpty()) {
                val pageAyahIds = ayahs.map { it.id }.toSet()
                val allPageRead = pageAyahIds.all { it in khatamReadAyahIds }

                IconButton(
                    onClick = { if (!allPageRead) onKhatamTogglePage(ayahs) },
                    enabled = !allPageRead,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (allPageRead) Icons.Filled.CheckCircle
                        else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = if (allPageRead) "Page read" else "Mark page as read",
                        tint = if (allPageRead) Color(0xFF22C55E)
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Next page (lower Quran page number)
            IconButton(
                onClick = onNavigateNext,
                enabled = pageNumber > 1,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next Page",
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Audio Bottom Bar
// ---------------------------------------------------------------------------
@Composable
private fun AudioBottomBar(
    isAudioActive: Boolean,
    isPlaying: Boolean,
    isDownloading: Boolean,
    isPreparing: Boolean,
    downloadProgress: Float,
    downloadedCount: Int,
    totalToDownload: Int,
    audioTitle: String,
    progress: Float,
    onPlayClick: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BottomAppBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 3.dp,
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Thin progress bar at top
            if (isAudioActive || isPreparing) {
                LinearProgressIndicator(
                    progress = {
                        if (isPreparing && totalToDownload > 0) downloadProgress else progress
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = if (isPreparing) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play/Pause button
                FilledIconButton(onClick = onPlayClick, modifier = Modifier.size(36.dp)) {
                    if (isDownloading || isPreparing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Audio info text
                Text(
                    text = when {
                        isPreparing && totalToDownload > 0 -> "Downloading $downloadedCount/$totalToDownload..."
                        isAudioActive -> audioTitle
                        else -> "Tap to play audio"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isAudioActive) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )

                // Stop button (only when audio active or preparing)
                if (isAudioActive || isPreparing) {
                    IconButton(onClick = onStopClick, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Stop",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Juz/Page Banner
// ---------------------------------------------------------------------------
@Composable
private fun JuzPageBanner(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(15.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF115E59), Color(0xFF042F2E))
                )
            )
            .border(1.dp, Color(0xFF0F766E), RoundedCornerShape(20.dp))
            .padding(25.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEAB308)
            )
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFEAB308).copy(alpha = 0.8f)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Surah Banner
// ---------------------------------------------------------------------------
@Composable
private fun SurahBanner(
    surahNameArabic: String,
    surahNameEnglish: String,
    surahMeaning: String,
    revelationType: RevelationType,
    ayahCount: Int,
    showBismillah: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(15.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF115E59), Color(0xFF042F2E))
                )
            )
            .border(1.dp, Color(0xFF0F766E), RoundedCornerShape(20.dp))
            .padding(25.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ArabicText(
                text = surahNameArabic,
                size = ArabicTextSize.EXTRA_LARGE,
                color = Color(0xFFEAB308)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = surahNameEnglish,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFEAB308)
            )

            Text(
                text = surahMeaning,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFEAB308).copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (revelationType == RevelationType.MECCAN) "Meccan" else "Medinan",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "$ayahCount Ayahs",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            if (showBismillah) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(20.dp))

                ArabicText(
                    text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
                    size = ArabicTextSize.LARGE,
                    color = Color(0xFFEAB308)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Page View: Surah separator shown when surah changes within a page
// ---------------------------------------------------------------------------
@Composable
private fun PageSurahSeparator(
    surahNumber: Int,
    surahNameArabic: String,
    surahNameEnglish: String,
    showBismillah: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF115E59), Color(0xFF042F2E))
                    )
                )
                .padding(vertical = 14.dp, horizontal = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = surahNumber.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = surahNameEnglish,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                if (surahNameArabic.isNotEmpty()) {
                    ArabicText(
                        text = surahNameArabic,
                        size = ArabicTextSize.MEDIUM,
                        color = Color(0xFFEAB308)
                    )
                }
            }
        }

        if (showBismillah) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                ArabicText(
                    text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
                    size = ArabicTextSize.LARGE,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Ayah Item with highlight support
// ---------------------------------------------------------------------------
@Composable
private fun AyahItem(
    ayah: Ayah,
    showTranslation: Boolean,
    showTransliteration: Boolean = false,
    arabicFontSize: Float,
    fontSize: Float,
    isHighlighted: Boolean = false,
    isFavorite: Boolean = false,
    isKhatamRead: Boolean = false,
    isKhatamMode: Boolean = false,
    showTajweed: Boolean = false,
    onBookmarkClick: () -> Unit,
    onFavoriteClick: () -> Unit = {},
    onPlayAyahClick: () -> Unit = {},
    onTafseerClick: () -> Unit = {},
    onKhatamToggle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()

    val bgColor by animateColorAsState(
        targetValue = if (isHighlighted)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else
            Color.Transparent,
        animationSpec = tween(300),
        label = "ayah_highlight"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 15.dp, vertical = 6.dp)
    ) {
        // Number badge + actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ayah.numberInSurah.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                if (isKhatamMode) {
                    IconButton(
                        onClick = onKhatamToggle,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isKhatamRead) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                            contentDescription = if (isKhatamRead) "Mark as unread" else "Mark as read",
                            tint = if (isKhatamRead) Color(0xFF22C55E) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color(0xFFEF4444)
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onBookmarkClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (ayah.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (ayah.isBookmarked) NimazColors.QuranColors.BookmarkPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = {
                        val textToShare = "${ayah.textArabic}\n\n${ayah.translation ?: ""}\n\n- Surah ${ayah.surahNumber}, Ayah ${ayah.numberInSurah}"
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, textToShare)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Ayah"))
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onPlayAyahClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isHighlighted) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isHighlighted) "Playing" else "Play",
                        tint = if (isHighlighted) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onTafseerClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = "Tafseer",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Arabic text with ayah end marker (with optional tajweed colors)
        val displayText = ayah.getDisplayArabicText()
        val textColor = MaterialTheme.colorScheme.onBackground

        if (showTajweed && ayah.textTajweed != null) {
            // Render with tajweed colors using ClickableText
            val tajweedAnnotated = remember(ayah.textTajweed, isDarkTheme, ayah.numberInSurah) {
                val parsed = TajweedParser.parse(
                    tajweedText = ayah.textTajweed,
                    isDarkTheme = isDarkTheme,
                    defaultColor = textColor
                )
                // Append the end marker to the tajweed text
                androidx.compose.ui.text.buildAnnotatedString {
                    append(parsed)
                    append(" ")
                    append(formatAyahEndMarker(ayah.numberInSurah))
                }
            }
            androidx.compose.foundation.text.BasicText(
                text = tajweedAnnotated,
                modifier = Modifier.fillMaxWidth(),
                style = TextStyle(
                    fontFamily = AmiriFontFamily,
                    fontSize = arabicFontSize.sp,
                    lineHeight = (arabicFontSize * 2).sp,
                    textDirection = TextDirection.Rtl,
                    color = textColor
                )
            )
        } else {
            QuranVerseText(
                arabicText = displayText,
                verseNumber = ayah.numberInSurah,
                customFontSize = arabicFontSize.sp.value
            )
        }

        // Translation
        if (showTranslation && ayah.translation != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                Text(
                    text = ayah.translation,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize * 1.5f).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Transliteration
        if (showTransliteration && ayah.transliteration != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
            ) {
                Text(
                    text = ayah.transliteration,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize * 1.5f).sp
                    ),
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Indicators row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (ayah.sajdaType != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFDC2626).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (ayah.sajdaType == SajdaType.OBLIGATORY) "Sajdah (Wajib)" else "Sajdah",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFDC2626),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                if (ayah.rubNumber > 0 && ayah.numberInSurah == 1 || (ayah.rubNumber > 0)) {
                    val quarterLabel = when (ayah.rubNumber) {
                        1 -> "Hizb ${ayah.hizbNumber}"
                        2 -> "\u00BC Hizb ${ayah.hizbNumber}"
                        3 -> "\u00BD Hizb ${ayah.hizbNumber}"
                        4 -> "\u00BE Hizb ${ayah.hizbNumber}"
                        else -> ""
                    }
                    if (quarterLabel.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = quarterLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Text(
                text = "Juz ${ayah.juz} \u2022 Page ${ayah.page}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            thickness = 0.5.dp
        )
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(showBackground = true, widthDp = 400, name = "Juz Page Banner")
@Composable
private fun JuzPageBannerPreview() {
    NimazTheme {
        JuzPageBanner(
            title = "Juz 1",
            subtitle = "Al-Fatihah - Al-Baqarah"
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Surah Banner")
@Composable
private fun SurahBannerPreview() {
    NimazTheme {
        SurahBanner(
            surahNameArabic = "الفاتحة",
            surahNameEnglish = "Al-Fatihah",
            surahMeaning = "The Opening",
            revelationType = RevelationType.MECCAN,
            ayahCount = 7,
            showBismillah = true
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Page Surah Separator")
@Composable
private fun PageSurahSeparatorPreview() {
    NimazTheme {
        PageSurahSeparator(
            surahNumber = 2,
            surahNameArabic = "البقرة",
            surahNameEnglish = "Al-Baqarah",
            showBismillah = true
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Audio Bottom Bar - Playing")
@Composable
private fun AudioBottomBarPlayingPreview() {
    NimazTheme {
        AudioBottomBar(
            isAudioActive = true,
            isPlaying = true,
            isDownloading = false,
            isPreparing = false,
            downloadProgress = 0f,
            downloadedCount = 0,
            totalToDownload = 0,
            audioTitle = "Al-Fatihah - Ayah 1",
            progress = 0.4f,
            onPlayClick = {},
            onStopClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Audio Bottom Bar - Downloading")
@Composable
private fun AudioBottomBarDownloadingPreview() {
    NimazTheme {
        AudioBottomBar(
            isAudioActive = true,
            isPlaying = false,
            isDownloading = true,
            isPreparing = false,
            downloadProgress = 0.65f,
            downloadedCount = 5,
            totalToDownload = 7,
            audioTitle = "Downloading...",
            progress = 0f,
            onPlayClick = {},
            onStopClick = {}
        )
    }
}
