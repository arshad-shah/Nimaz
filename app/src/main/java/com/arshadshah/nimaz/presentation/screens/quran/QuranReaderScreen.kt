package com.arshadshah.nimaz.presentation.screens.quran

import android.content.Intent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.SajdaType
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.QuranVerseText
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.viewmodel.QuranEvent
import com.arshadshah.nimaz.presentation.viewmodel.QuranViewModel
import com.arshadshah.nimaz.presentation.viewmodel.ReadingMode

@Composable
fun QuranReaderScreen(
    surahNumber: Int? = null,
    juzNumber: Int? = null,
    pageNumber: Int? = null,
    onNavigateBack: () -> Unit,
    onNavigateToQuranSettings: () -> Unit = {},
    viewModel: QuranViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.readerState.collectAsState()
    val audioState by viewModel.audioState.collectAsState()
    val listState = rememberLazyListState()

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
        // Get ayahs for the current reading mode
        val ayahs = when (state.readingMode) {
            ReadingMode.SURAH -> state.surahWithAyahs?.ayahs ?: return@LaunchedEffect
            ReadingMode.JUZ, ReadingMode.PAGE -> state.ayahs
        }
        if (ayahs.isEmpty()) return@LaunchedEffect

        // Offset for banner item (index 0 is the banner)
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
                // +2 for banner + settings bar items
                listState.animateScrollToItem(idx + 2)
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
    val headerMeta = when (state.readingMode) {
        ReadingMode.PAGE -> "" // Page view has its own nav bar with page info
        else -> state.subtitle
    }
    // Page view always knows its title from the route param, so never show loading for it
    val headerLoading = state.isLoading && state.readingMode != ReadingMode.PAGE

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                ReaderHeader(
                    title = headerTitle,
                    subtitle = headerMeta,
                    isLoading = headerLoading,
                    onBackClick = onNavigateBack,
                    onSettingsClick = onNavigateToQuranSettings
                )

                // Content
                if (state.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else if (state.readingMode == ReadingMode.PAGE && pageNumber != null) {
                    // Page mode: HorizontalPager for swipe navigation between pages
                    val totalPages = 604 // Total pages in the Quran
                    val pagerState = rememberPagerState(
                        initialPage = (pageNumber - 1).coerceIn(0, totalPages - 1),
                        pageCount = { totalPages }
                    )

                    // Load page when pager settles on a new page
                    LaunchedEffect(pagerState.settledPage) {
                        val newPageNumber = pagerState.settledPage + 1
                        if (newPageNumber != pageNumber) {
                            viewModel.onEvent(QuranEvent.LoadPage(newPageNumber))
                        }
                    }

                    val coroutineScope = rememberCoroutineScope()

                    // Page number indicator with navigation buttons (LTR for the nav bar)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    // In RTL pager, "previous page" visually means higher index
                                    if (pagerState.currentPage < totalPages - 1) {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                }
                            },
                            enabled = pagerState.currentPage < totalPages - 1,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                                contentDescription = "Previous Page",
                                tint = if (pagerState.currentPage < totalPages - 1)
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Text(
                            text = "Page ${pagerState.settledPage + 1} of $totalPages",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    // In RTL pager, "next page" visually means lower index
                                    if (pagerState.currentPage > 0) {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                }
                            },
                            enabled = pagerState.currentPage > 0,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                                contentDescription = "Next Page",
                                tint = if (pagerState.currentPage > 0)
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Access surah list for names in page view
                    val homeState by viewModel.homeState.collectAsState()

                    // RTL pager: swipe left = next page (higher number), matching mushaf direction
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) { page ->
                    // Restore LTR for page content so text/layout renders correctly
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 64.dp)
                        ) {
                            // Page header with juz/hizb info
                            item(key = "header_$page") {
                                val firstAyah = displayAyahs.firstOrNull()
                                val juzNum = firstAyah?.juzNumber ?: 0
                                val hizbNum = firstAyah?.hizbNumber ?: 0
                                PageHeaderBar(
                                    pageNumber = page + 1,
                                    juzNumber = juzNum,
                                    hizbNumber = hizbNum,
                                    ayahCount = displayAyahs.size
                                )
                            }

                            // Render ayahs with surah separators when surah changes
                            var lastSurahNumber = -1
                            displayAyahs.forEach { ayah ->
                                val showSurahSeparator = ayah.surahNumber != lastSurahNumber
                                lastSurahNumber = ayah.surahNumber

                                if (showSurahSeparator) {
                                    item(key = "surah_sep_${page}_${ayah.surahNumber}") {
                                        val surahName = homeState.surahs
                                            .find { it.number == ayah.surahNumber }
                                        PageSurahSeparator(
                                            surahNumber = ayah.surahNumber,
                                            surahNameArabic = surahName?.nameArabic ?: "",
                                            surahNameEnglish = surahName?.nameEnglish ?: "Surah ${ayah.surahNumber}",
                                            showBismillah = ayah.numberInSurah == 1 && ayah.surahNumber != 1 && ayah.surahNumber != 9
                                        )
                                    }
                                }

                                item(key = "page_${page}_${ayah.id}") {
                                    val isHighlighted = audioState.currentAyahId == ayah.id && audioState.isActive

                                    AyahItem(
                                        ayah = ayah,
                                        showTranslation = state.showTranslation,
                                        showTransliteration = state.showTransliteration,
                                        arabicFontSize = state.arabicFontSize,
                                        fontSize = state.fontSize,
                                        isHighlighted = isHighlighted,
                                        isFavorite = ayah.id in favoriteAyahIds,
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
                    } // end LTR CompositionLocalProvider
                    } // end HorizontalPager
                    } // end RTL CompositionLocalProvider
                } else {
                    // Surah/Juz mode: standard LazyColumn
                    // Precompute which ayah IDs start a new surah (for juz mode separators)
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
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 64.dp)
                    ) {
                        // Surah Banner or Juz/Page Banner
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

                        // Ayahs — with surah separators in Juz mode
                        items(
                            items = displayAyahs,
                            key = { it.id }
                        ) { ayah ->
                            // Show surah separator for juz mode when surah changes
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

            // Compact bottom bar — unified play/audio controls (always visible)
            CompactBottomBar(
                isAudioActive = audioState.isActive,
                isPlaying = audioState.isPlaying,
                isDownloading = audioState.isDownloading,
                audioTitle = audioState.currentSubtitle ?: audioState.currentTitle,
                progress = if (audioState.duration > 0) audioState.position.toFloat() / audioState.duration else 0f,
                onPlayClick = {
                    if (audioState.isPlaying) {
                        viewModel.onEvent(QuranEvent.PauseAudio)
                    } else if (audioState.isActive) {
                        // Resume paused audio
                        viewModel.onEvent(QuranEvent.ResumeAudio)
                    } else if (state.readingMode == ReadingMode.SURAH && surahNumber != null) {
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
                },
                onStopClick = { viewModel.onEvent(QuranEvent.StopAudio) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Header: back | title | settings
// ---------------------------------------------------------------------------
@Composable
private fun ReaderHeader(
    title: String,
    subtitle: String,
    isLoading: Boolean,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                onClick = onBackClick
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (subtitle.isNotEmpty()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                onClick = onSettingsClick
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
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
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "$ayahCount Ayahs",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            }

            if (showBismillah) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(20.dp))

                ArabicText(
                    text = "\u0628\u0650\u0633\u0652\u0645\u0650 \u0627\u0644\u0644\u0651\u064E\u0647\u0650 \u0627\u0644\u0631\u0651\u064E\u062D\u0652\u0645\u064E\u0670\u0646\u0650 \u0627\u0644\u0631\u0651\u064E\u062D\u0650\u064A\u0645\u0650",
                    size = ArabicTextSize.LARGE,
                    color = Color(0xFFEAB308)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Page View: Header bar showing page, juz, hizb info
// ---------------------------------------------------------------------------
@Composable
private fun PageHeaderBar(
    pageNumber: Int,
    juzNumber: Int,
    hizbNumber: Int,
    ayahCount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Juz badge
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = juzNumber.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Juz",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Vertical divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(28.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            )

            // Hizb badge
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = hizbNumber.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = "Hizb",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Vertical divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(28.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            )

            // Ayah count
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = ayahCount.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Ayahs",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
        // Surah name card
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
                // Left: number + English name
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

                // Right: Arabic name
                if (surahNameArabic.isNotEmpty()) {
                    ArabicText(
                        text = surahNameArabic,
                        size = ArabicTextSize.MEDIUM,
                        color = Color(0xFFEAB308)
                    )
                }
            }
        }

        // Bismillah
        if (showBismillah) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                ArabicText(
                    text = "\u0628\u0650\u0633\u0652\u0645\u0650 \u0627\u0644\u0644\u0651\u064E\u0647\u0650 \u0627\u0644\u0631\u0651\u064E\u062D\u0652\u0645\u064E\u0670\u0646\u0650 \u0627\u0644\u0631\u0651\u064E\u062D\u0650\u064A\u0645\u0650",
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
    onBookmarkClick: () -> Unit,
    onFavoriteClick: () -> Unit = {},
    onPlayAyahClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

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

        // Arabic text with Amiri font and verse end markers
        QuranVerseText(
            arabicText = ayah.textArabic,
            verseNumber = ayah.numberInSurah,
            size = ArabicTextSize.QURAN,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth()
        )

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

        // Indicators row: sajdah, hizb, rub, juz, page
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: special indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sajdah indicator
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

                // Hizb quarter indicator (rub al-hizb)
                if (ayah.rubNumber > 0 && ayah.numberInSurah == 1 || (ayah.rubNumber > 0 && ayah.rubNumber % 1 == 0)) {
                    val hizbQuarter = ((ayah.hizbNumber - 1) * 4 + ayah.rubNumber)
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

            // Right side: juz / page
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
// Compact Bottom Bar: unified play / audio controls (always visible)
// ---------------------------------------------------------------------------
@Composable
private fun CompactBottomBar(
    isAudioActive: Boolean,
    isPlaying: Boolean,
    isDownloading: Boolean,
    audioTitle: String,
    progress: Float,
    onPlayClick: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Progress bar (only when audio active)
            if (isAudioActive) {
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: audio info or prompt
                Column(modifier = Modifier.weight(1f)) {
                    if (isAudioActive) {
                        Text(
                            text = "Now Playing",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            text = audioTitle,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Tap to play audio",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
                        if (isDownloading) {
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

                // Stop button (only when audio active)
                if (isAudioActive) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
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
