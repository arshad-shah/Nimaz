package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.BismillahText
import com.arshadshah.nimaz.presentation.components.molecules.AyahCard
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * Complete Quran reader with scrollable ayahs and controls.
 */
@Composable
fun QuranReader(
    surah: Surah,
    ayahs: List<Ayah>,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    isLoading: Boolean = false,
    showTranslation: Boolean = true,
    showTransliteration: Boolean = false,
    arabicTextSize: ArabicTextSize = ArabicTextSize.QURAN,
    currentPlayingAyah: Int? = null,
    onAyahClick: (Ayah) -> Unit = {},
    onBookmarkClick: (Ayah) -> Unit = {},
    onPlayClick: (Ayah) -> Unit = {},
    onShareClick: (Ayah) -> Unit = {},
    onCopyClick: (Ayah) -> Unit = {},
    onScrollToTop: () -> Unit = {},
    onScrollToBottom: () -> Unit = {}
) {
    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 5 }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Surah header
                item {
                    SurahHeader(surah = surah)
                }

                // Bismillah (except for Surah At-Tawbah)
                if (surah.number != 9) {
                    item {
                        BismillahText(
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                }

                // Ayahs
                items(
                    items = ayahs,
                    key = { it.id }
                ) { ayah ->
                    AyahCard(
                        arabicText = ayah.textArabic,
                        translation = ayah.translation ?: "",
                        surahNumber = ayah.surahNumber,
                        ayahNumber = ayah.ayahNumber,
                        surahName = surah.nameEnglish,
                        transliteration = null,
                        showTransliteration = false,
                        isBookmarked = ayah.isBookmarked,
                        isSajdaAyah = ayah.sajdaType != null,
                        arabicFontSize = arabicTextSize,
                        onBookmarkClick = { onBookmarkClick(ayah) },
                        onPlayClick = { onPlayClick(ayah) },
                        onShareClick = { onShareClick(ayah) },
                        onCopyClick = { onCopyClick(ayah) },
                        onClick = { onAyahClick(ayah) }
                    )
                }

                // Bottom padding
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        // Scroll controls
        AnimatedVisibility(
            visible = showScrollToTop,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallFloatingActionButton(
                    onClick = onScrollToTop,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    elevation = FloatingActionButtonDefaults.elevation(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Scroll to top"
                    )
                }
                SmallFloatingActionButton(
                    onClick = onScrollToBottom,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    elevation = FloatingActionButtonDefaults.elevation(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Scroll to bottom"
                    )
                }
            }
        }
    }
}

/**
 * Surah header with name and info.
 */
@Composable
private fun SurahHeader(
    surah: Surah,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ArabicText(
                text = surah.nameArabic,
                size = ArabicTextSize.EXTRA_LARGE,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = surah.nameEnglish,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = surah.nameTransliteration,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SurahInfoChip(text = "${surah.ayahCount} Ayahs")
                Spacer(modifier = Modifier.width(12.dp))
                SurahInfoChip(text = surah.revelationType.name.lowercase().replaceFirstChar { it.uppercase() })
                Spacer(modifier = Modifier.width(12.dp))
                SurahInfoChip(text = "Juz ${surah.juzStart}")
            }
        }
    }
}

@Composable
private fun SurahInfoChip(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Reading controls toolbar for Quran reader.
 */
@Composable
fun QuranReaderControls(
    showTranslation: Boolean,
    showTransliteration: Boolean,
    modifier: Modifier = Modifier,
    onTranslationToggle: (Boolean) -> Unit = {},
    onTransliterationToggle: (Boolean) -> Unit = {},
    onFontSizeClick: () -> Unit = {},
    onPlayAllClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onTranslationToggle(!showTranslation) }) {
                Icon(
                    imageVector = Icons.Default.Translate,
                    contentDescription = "Toggle translation",
                    tint = if (showTranslation) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            IconButton(onClick = onFontSizeClick) {
                Icon(
                    imageVector = Icons.Default.FormatSize,
                    contentDescription = "Font size",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onPlayAllClick) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play all",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Page mode Quran reader (Mushaf style).
 */
@Composable
fun QuranPageReader(
    pageNumber: Int,
    ayahs: List<Ayah>,
    modifier: Modifier = Modifier,
    surahName: String? = null,
    juzNumber: Int? = null,
    onPreviousPage: () -> Unit = {},
    onNextPage: () -> Unit = {},
    onAyahLongPress: (Ayah) -> Unit = {}
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Page header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (juzNumber != null) {
                    Text(
                        text = "Juz $juzNumber",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "Page $pageNumber",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                if (surahName != null) {
                    Text(
                        text = surahName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Page content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = ayahs.joinToString(" ") { it.textArabic },
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    lineHeight = MaterialTheme.typography.headlineSmall.lineHeight * 2
                )
            }
        }
    }
}

/**
 * Continuous reading mode (Surah after Surah).
 */
@Composable
fun QuranContinuousReader(
    surahsWithAyahs: List<Pair<Surah, List<Ayah>>>,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    showTranslation: Boolean = true,
    showTransliteration: Boolean = false,
    arabicTextSize: ArabicTextSize = ArabicTextSize.QURAN,
    onAyahClick: (Ayah) -> Unit = {},
    onBookmarkClick: (Ayah) -> Unit = {}
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        surahsWithAyahs.forEach { (surah, ayahs) ->
            // Surah header
            item(key = "surah_header_${surah.number}") {
                SurahHeader(surah = surah)
            }

            // Bismillah (except for Surah At-Tawbah)
            if (surah.number != 9) {
                item(key = "bismillah_${surah.number}") {
                    BismillahText(
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            }

            // Ayahs
            items(
                items = ayahs,
                key = { "ayah_${it.surahNumber}_${it.ayahNumber}" }
            ) { ayah ->
                AyahCard(
                    arabicText = ayah.textArabic,
                    translation = ayah.translation ?: "",
                    surahNumber = ayah.surahNumber,
                    ayahNumber = ayah.ayahNumber,
                    surahName = surah.nameEnglish,
                    transliteration = null,
                    showTransliteration = false,
                    isBookmarked = ayah.isBookmarked,
                    isSajdaAyah = ayah.sajdaType != null,
                    arabicFontSize = arabicTextSize,
                    onBookmarkClick = { onBookmarkClick(ayah) },
                    onClick = { onAyahClick(ayah) }
                )
            }

            // Surah separator
            item(key = "separator_${surah.number}") {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// Previews
@Preview(showBackground = true)
@Composable
private fun QuranReaderPreview() {
    NimazTheme {
        val surah = Surah(
            number = 1,
            nameArabic = "الفاتحة",
            nameEnglish = "The Opening",
            nameTransliteration = "Al-Fatihah",
            ayahCount = 7,
            revelationType = RevelationType.MECCAN,
            juzStart = 1,
            orderInMushaf = 5
        )
        val ayahs = listOf(
            Ayah(
                id = 1,
                surahNumber = 1,
                ayahNumber = 1,
                textArabic = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                textSimple = "بسم الله الرحمن الرحيم",
                juzNumber = 1,
                hizbNumber = 1,
                rubNumber = 1,
                pageNumber = 1,
                sajdaType = null,
                sajdaNumber = null,
                translation = "In the name of Allah, the Entirely Merciful, the Especially Merciful."
            ),
            Ayah(
                id = 2,
                surahNumber = 1,
                ayahNumber = 2,
                textArabic = "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ",
                textSimple = "الحمد لله رب العالمين",
                juzNumber = 1,
                hizbNumber = 1,
                rubNumber = 1,
                pageNumber = 1,
                sajdaType = null,
                sajdaNumber = null,
                translation = "All praise is due to Allah, Lord of the worlds."
            )
        )
        QuranReader(
            surah = surah,
            ayahs = ayahs,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SurahHeaderPreview() {
    NimazTheme {
        SurahHeader(
            surah = Surah(
                number = 1,
                nameArabic = "الفاتحة",
                nameEnglish = "The Opening",
                nameTransliteration = "Al-Fatihah",
                ayahCount = 7,
                revelationType = RevelationType.MECCAN,
                juzStart = 1,
                orderInMushaf = 5
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
