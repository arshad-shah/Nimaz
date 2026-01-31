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
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.SajdaType
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.viewmodel.QuranEvent
import com.arshadshah.nimaz.presentation.viewmodel.QuranViewModel
import com.arshadshah.nimaz.presentation.viewmodel.ReadingMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import com.arshadshah.nimaz.core.util.TajweedParser
import com.arshadshah.nimaz.presentation.components.atoms.toArabicNumber
import com.arshadshah.nimaz.presentation.theme.AmiriFontFamily
import com.arshadshah.nimaz.presentation.components.organisms.MushafPage
import androidx.compose.ui.tooling.preview.Preview
import com.arshadshah.nimaz.presentation.theme.NimazTheme

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
    viewModel: QuranViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.readerState.collectAsState()
    val audioState by viewModel.audioState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

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

    // Page mode pager state
    val totalPages = 604
    val pagerState = if (state.readingMode == ReadingMode.PAGE && pageNumber != null) {
        rememberPagerState(
            initialPage = (pageNumber - 1).coerceIn(0, totalPages - 1),
            pageCount = { totalPages }
        )
    } else null

    // Load page when pager settles
    pagerState?.let { ps ->
        LaunchedEffect(ps.settledPage) {
            val newPageNumber = ps.settledPage + 1
            if (newPageNumber != pageNumber) {
                viewModel.onEvent(QuranEvent.LoadPage(newPageNumber))
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
                        if (state.readingMode == ReadingMode.SURAH && surahNumber != null) {
                            val name = state.surahWithAyahs?.surah?.nameEnglish ?: "Surah $surahNumber"
                            viewModel.onEvent(QuranEvent.PlaySurahAudio(surahNumber, name))
                        } else if (displayAyahs.isNotEmpty()) {
                            viewModel.onEvent(
                                QuranEvent.PlayAyahAudio(
                                    ayahGlobalId = displayAyahs.first().id,
                                    surahNumber = displayAyahs.first().surahNumber,
                                    ayahNumber = displayAyahs.first().ayahNumber
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
            if (state.isLoading && state.readingMode != ReadingMode.PAGE) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (state.readingMode == ReadingMode.PAGE && pagerState != null) {
                // Page mode with HorizontalPager using MushafPage
                val homeState by viewModel.homeState.collectAsState()

                // Build surah map for MushafPage
                val surahMap = remember(homeState.surahs) {
                    homeState.surahs.associateBy { it.number }
                }

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val pageNum = page + 1
                        val pageAyahs = state.pageCache[pageNum] ?: displayAyahs.takeIf {
                            pagerState.settledPage == page
                        } ?: emptyList()

                        LaunchedEffect(pageNum) {
                            if (pageNum !in state.pageCache.keys) {
                                viewModel.onEvent(QuranEvent.LoadPage(pageNum))
                            }
                        }

                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            val highlightedAyahId = if (audioState.isActive) audioState.currentAyahId else null

                            MushafPage(
                                pageNumber = pageNum,
                                ayahs = pageAyahs,
                                surahMap = surahMap,
                                arabicFontSize = state.arabicFontSize,
                                totalPages = totalPages,
                                highlightedAyahId = highlightedAyahId,
                                favoriteAyahIds = favoriteAyahIds,
                                showTajweed = state.showTajweed,
                                onNavigatePrevious = {
                                    coroutineScope.launch {
                                        if (pagerState.currentPage < totalPages - 1) {
                                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                        }
                                    }
                                },
                                onNavigateNext = {
                                    coroutineScope.launch {
                                        if (pagerState.currentPage > 0) {
                                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                        }
                                    }
                                },
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
                                onShareClick = { /* Share is handled in bottom sheet */ },
                                onCopyClick = { /* Copy is handled in bottom sheet */ },
                                modifier = Modifier.fillMaxSize()
                            )
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
                            }
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Audio Bottom Bar using BottomAppBar
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
        tonalElevation = 3.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Progress bar - show download progress when preparing, playback progress otherwise
            if (isAudioActive || isPreparing) {
                if (isPreparing && totalToDownload > 0) {
                    LinearProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = MaterialTheme.colorScheme.tertiary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: audio info or prompt
                Column(modifier = Modifier.weight(1f)) {
                    if (isPreparing && totalToDownload > 0) {
                        Text(
                            text = "Preparing Audio",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "Downloading $downloadedCount of $totalToDownload ayahs...",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else if (isAudioActive) {
                        Text(
                            text = "Now Playing",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = audioTitle,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        Text(
                            text = "Tap to play audio",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Play/Pause button
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = onPlayClick,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        if (isDownloading || isPreparing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Stop button (only when audio active or preparing)
                if (isAudioActive || isPreparing) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        onClick = onStopClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Stop",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
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
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF115E59), Color(0xFF042F2E))
                    )
                )
                .border(1.dp, Color(0xFF0F766E), RoundedCornerShape(14.dp))
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
    showTajweed: Boolean = false,
    onBookmarkClick: () -> Unit,
    onFavoriteClick: () -> Unit = {},
    onPlayAyahClick: () -> Unit = {},
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
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Arabic text with ayah end marker (with optional tajweed colors)
        val displayText = ayah.getDisplayArabicText()
        val formattedText = formatAyahWithEndMarker(displayText, ayah.numberInSurah)
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
            ArabicText(
                text = formattedText,
                modifier = Modifier.fillMaxWidth(),
                color = textColor,
                style = TextStyle(
                    fontFamily = AmiriFontFamily,
                    fontSize = arabicFontSize.sp,
                    lineHeight = (arabicFontSize * 2).sp,
                    textDirection = TextDirection.Rtl
                )
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
