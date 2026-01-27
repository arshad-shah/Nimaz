package com.arshadshah.nimaz.presentation.screens.quran

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.organisms.QuranReaderTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.viewmodel.QuranEvent
import com.arshadshah.nimaz.presentation.viewmodel.QuranViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranReaderScreen(
    surahNumber: Int,
    onNavigateBack: () -> Unit,
    viewModel: QuranViewModel = hiltViewModel()
) {
    val state by viewModel.readerState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val listState = rememberLazyListState()
    var showMenu by remember { mutableStateOf(false) }

    // Load surah on first composition
    LaunchedEffect(surahNumber) {
        viewModel.onEvent(QuranEvent.LoadSurah(surahNumber))
    }

    // Track reading position
    val currentAyahIndex by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex
        }
    }

    // Update reading position when scrolling
    LaunchedEffect(currentAyahIndex) {
        state.surahWithAyahs?.let { surahWithAyahs ->
            if (currentAyahIndex > 0 && currentAyahIndex < surahWithAyahs.ayahs.size) {
                val ayah = surahWithAyahs.ayahs[currentAyahIndex]
                viewModel.onEvent(
                    QuranEvent.UpdateReadingPosition(
                        surah = surahNumber,
                        ayah = ayah.numberInSurah,
                        page = ayah.page,
                        juz = ayah.juz
                    )
                )
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            state.surahWithAyahs?.let { surahWithAyahs ->
                QuranReaderTopAppBar(
                    surahName = surahWithAyahs.surah.nameEnglish,
                    surahNameArabic = surahWithAyahs.surah.nameArabic,
                    surahNumber = surahWithAyahs.surah.number,
                    onBackClick = onNavigateBack,
                    onSearchClick = { /* Search in surah */ },
                    onMoreClick = { showMenu = true },
                    scrollBehavior = scrollBehavior
                )
            } ?: TopAppBar(
                title = { Text("Loading...") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Play audio */ },
                containerColor = NimazColors.QuranColors.Meccan
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Audio",
                    tint = Color.White
                )
            }
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            state.surahWithAyahs?.let { surahWithAyahs ->
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    // Surah Header
                    item {
                        SurahHeader(
                            surahName = surahWithAyahs.surah.nameArabic,
                            surahNameEnglish = surahWithAyahs.surah.nameEnglish,
                            surahMeaning = surahWithAyahs.surah.nameTransliteration,
                            revelationType = surahWithAyahs.surah.revelationType,
                            ayahCount = surahWithAyahs.surah.numberOfAyahs
                        )
                    }

                    // Bismillah (except for Surah 9 - At-Tawbah)
                    if (surahNumber != 9 && surahNumber != 1) {
                        item {
                            BismillahCard()
                        }
                    }

                    // Ayahs
                    items(
                        items = surahWithAyahs.ayahs,
                        key = { it.id }
                    ) { ayah ->
                        AyahCard(
                            ayah = ayah,
                            showTranslation = state.showTranslation,
                            arabicFontSize = state.arabicFontSize,
                            fontSize = state.fontSize,
                            onBookmarkClick = {
                                viewModel.onEvent(
                                    QuranEvent.ToggleBookmark(
                                        ayahId = ayah.id,
                                        surahNumber = surahNumber,
                                        ayahNumber = ayah.numberInSurah
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }

        // Dropdown Menu
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text(if (state.showTranslation) "Hide Translation" else "Show Translation") },
                onClick = {
                    viewModel.onEvent(QuranEvent.ToggleTranslation)
                    showMenu = false
                }
            )
            DropdownMenuItem(
                text = { Text("Font Size") },
                onClick = { showMenu = false }
            )
            DropdownMenuItem(
                text = { Text("Go to Ayah") },
                onClick = { showMenu = false }
            )
        }
    }
}

@Composable
private fun SurahHeader(
    surahName: String,
    surahNameEnglish: String,
    surahMeaning: String,
    revelationType: RevelationType,
    ayahCount: Int,
    modifier: Modifier = Modifier
) {
    val gradientColors = if (revelationType == RevelationType.MECCAN) {
        listOf(NimazColors.QuranColors.Meccan, NimazColors.QuranColors.Meccan.copy(alpha = 0.7f))
    } else {
        listOf(NimazColors.QuranColors.Medinan, NimazColors.QuranColors.Medinan.copy(alpha = 0.7f))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(gradientColors))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ArabicText(
                    text = surahName,
                    size = ArabicTextSize.EXTRA_LARGE,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = surahNameEnglish,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = surahMeaning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = if (revelationType == RevelationType.MECCAN) "Meccan" else "Medinan",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "$ayahCount Ayahs",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BismillahCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            ArabicText(
                text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                size = ArabicTextSize.LARGE,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun AyahCard(
    ayah: Ayah,
    showTranslation: Boolean,
    arabicFontSize: Float,
    fontSize: Float,
    onBookmarkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Ayah Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ayah Number Badge
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ayah.numberInSurah.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Row {
                    IconButton(
                        onClick = onBookmarkClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (ayah.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (ayah.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { /* Share */ },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Arabic Text
            Text(
                text = ayah.textArabic,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = arabicFontSize.sp,
                    lineHeight = (arabicFontSize * 1.8f).sp
                ),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )

            // Translation
            if (showTranslation && ayah.translation != null) {
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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

            // Juz/Page Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Juz ${ayah.juz} • Page ${ayah.page}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}
