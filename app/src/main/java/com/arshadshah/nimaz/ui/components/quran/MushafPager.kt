package com.arshadshah.nimaz.ui.components.quran

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.utils.QuranUtils.getArabicFont
import kotlinx.coroutines.launch

// ============ DATA MODELS ============

data class MushafAya(
    val ayaNumberInQuran: Int,
    val ayaNumberInSurah: Int,
    val suraNumber: Int,
    val ayaArabic: String,
    val translationEnglish: String = "",
    val translationUrdu: String = "",
    val juzNumber: Int,
    val bookmark: Boolean = false,
    val favorite: Boolean = false,
    val sajda: Boolean = false,
    val sajdaType: String = "",
    val ruku: Int = 0,
    val note: String = ""
)

data class MushafPage(
    val pageNumber: Int,
    val juzNumber: Int,
    val hizbNumber: Int? = null,
    val surahHeaders: List<SurahHeaderInfo> = emptyList(),
    val ayat: List<MushafAya>
)

data class SurahHeaderInfo(
    val surahNumber: Int,
    val surahNameArabic: String,
    val surahNameEnglish: String,
    val showBismillah: Boolean,
    val insertBeforeAyaIndex: Int
)

// Sealed class for page content items - enables efficient LazyColumn rendering
sealed class MushafPageItem {
    data class Header(val info: SurahHeaderInfo) : MushafPageItem()
    data class Bismillah(val surahNumber: Int) : MushafPageItem()
    data class Aya(val aya: MushafAya, val isSelected: Boolean = false) : MushafPageItem()
}

// ============ MAIN MUSHAF PAGER ============

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MushafPager(
    pages: List<MushafPage>,
    initialPage: Int = 0,
    showTranslation: Boolean = false,
    translationLanguage: String = "en",
    arabicFontName: String = "Uthmanic",
    arabicFontSize: Int = 24,
    selectedAya: MushafAya? = null,
    onPageChanged: (Int) -> Unit = {},
    onAyaClick: (MushafAya) -> Unit = {},
    onAyaLongClick: (MushafAya) -> Unit = {},
    onBookmarkClick: (MushafAya) -> Unit = {},
    onShareClick: (MushafAya) -> Unit = {},
    onTafsirClick: (MushafAya) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { pages.size }
    )
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        onPageChanged(pagerState.currentPage)
    }

    Column(modifier = modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            reverseLayout = true,
            beyondViewportPageCount = 1,
            key = { pages[it].pageNumber }
        ) { pageIndex ->
            MushafPageView(
                page = pages[pageIndex],
                showTranslation = showTranslation,
                translationLanguage = translationLanguage,
                arabicFontName = arabicFontName,
                arabicFontSize = arabicFontSize,
                selectedAya = selectedAya,
                onAyaClick = onAyaClick,
                onAyaLongClick = onAyaLongClick,
                onBookmarkClick = onBookmarkClick,
                onShareClick = onShareClick,
                onTafsirClick = onTafsirClick
            )
        }

        MushafNavigationBar(
            pagerState = pagerState,
            totalPages = pages.size,
            currentJuz = pages.getOrNull(pagerState.currentPage)?.juzNumber ?: 1,
            onNavigate = { targetPage ->
                coroutineScope.launch {
                    pagerState.animateScrollToPage(targetPage)
                }
            }
        )
    }
}

// ============ OPTIMIZED PAGE VIEW WITH LAZY COLUMN ============

@Composable
fun MushafPageView(
    page: MushafPage,
    showTranslation: Boolean = false,
    translationLanguage: String = "en",
    arabicFontName: String = "Uthmanic",
    arabicFontSize: Int = 24,
    selectedAya: MushafAya? = null,
    onAyaClick: (MushafAya) -> Unit = {},
    onAyaLongClick: (MushafAya) -> Unit = {},
    onBookmarkClick: (MushafAya) -> Unit = {},
    onShareClick: (MushafAya) -> Unit = {},
    onTafsirClick: (MushafAya) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Convert page data to flat list of items for LazyColumn
    val pageItems = remember(page, selectedAya) {
        buildPageItems(page, selectedAya)
    }

    val listState = rememberLazyListState()

    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Page header
            MushafPageHeader(
                pageNumber = page.pageNumber,
                juzNumber = page.juzNumber,
                hizbNumber = page.hizbNumber
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Lazy column for efficient rendering
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(
                    items = pageItems,
                    key = { item ->
                        when (item) {
                            is MushafPageItem.Header -> "header_${item.info.surahNumber}"
                            is MushafPageItem.Bismillah -> "bismillah_${item.surahNumber}"
                            is MushafPageItem.Aya -> "aya_${item.aya.ayaNumberInQuran}"
                        }
                    }
                ) { item ->
                    when (item) {
                        is MushafPageItem.Header -> {
                            SurahHeaderFrame(
                                header = item.info,
                                arabicFontName = arabicFontName
                            )
                        }
                        is MushafPageItem.Bismillah -> {
                            BismillahItem(arabicFontName = arabicFontName)
                        }
                        is MushafPageItem.Aya -> {
                            MushafAyaItem(
                                aya = item.aya,
                                isSelected = item.isSelected,
                                showTranslation = showTranslation,
                                translationLanguage = translationLanguage,
                                arabicFontName = arabicFontName,
                                arabicFontSize = arabicFontSize,
                                onClick = { onAyaClick(item.aya) },
                                onLongClick = { onAyaLongClick(item.aya) },
                                onBookmarkClick = { onBookmarkClick(item.aya) },
                                onShareClick = { onShareClick(item.aya) },
                                onTafsirClick = { onTafsirClick(item.aya) }
                            )
                        }
                    }
                }
            }

            // Page footer
            PageFooter(pageNumber = page.pageNumber)
        }
    }
}

// Build flat list of items for LazyColumn
private fun buildPageItems(page: MushafPage, selectedAya: MushafAya?): List<MushafPageItem> {
    val items = mutableListOf<MushafPageItem>()
    val sortedHeaders = page.surahHeaders.sortedBy { it.insertBeforeAyaIndex }

    var currentAyaIndex = 0

    sortedHeaders.forEach { header ->
        // Add ayat before this header
        while (currentAyaIndex < header.insertBeforeAyaIndex && currentAyaIndex < page.ayat.size) {
            val aya = page.ayat[currentAyaIndex]
            items.add(MushafPageItem.Aya(aya, aya.ayaNumberInQuran == selectedAya?.ayaNumberInQuran))
            currentAyaIndex++
        }

        // Add header
        items.add(MushafPageItem.Header(header))

        // Add Bismillah if needed
        if (header.showBismillah) {
            items.add(MushafPageItem.Bismillah(header.surahNumber))
        }
    }

    // Add remaining ayat
    while (currentAyaIndex < page.ayat.size) {
        val aya = page.ayat[currentAyaIndex]
        items.add(MushafPageItem.Aya(aya, aya.ayaNumberInQuran == selectedAya?.ayaNumberInQuran))
        currentAyaIndex++
    }

    return items
}

// ============ INDIVIDUAL AYA ITEM ============

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MushafAyaItem(
    aya: MushafAya,
    isSelected: Boolean,
    showTranslation: Boolean,
    translationLanguage: String,
    arabicFontName: String,
    arabicFontSize: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onShareClick: () -> Unit,
    onTafsirClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var showMenu by remember { mutableStateOf(false) }

    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        aya.sajda -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showMenu = true
                    onLongClick()
                }
            ),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // Aya number badge and actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Aya number badge
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = toArabicNumeral(aya.ayaNumberInSurah),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sajda indicator
                    if (aya.sajda) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiary,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "۩",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Bookmark button
                    IconButton(
                        onClick = onBookmarkClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (aya.bookmark) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (aya.bookmark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // More options
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Tafsir") },
                                onClick = {
                                    showMenu = false
                                    onTafsirClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Share, contentDescription = null)
                                },
                                onClick = {
                                    showMenu = false
                                    onShareClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (aya.bookmark) "Remove Bookmark" else "Add Bookmark") },
                                leadingIcon = {
                                    Icon(
                                        if (aya.bookmark) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onBookmarkClick()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Arabic text
            SelectionContainer {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = aya.ayaArabic,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = arabicFontSize.sp,
                            lineHeight = (arabicFontSize * 2).sp
                        ),
                        fontFamily = getArabicFont(arabicFontName),
                        textAlign = TextAlign.Justify,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Translation if enabled
            if (showTranslation) {
                val translation = when (translationLanguage) {
                    "ur" -> aya.translationUrdu
                    else -> aya.translationEnglish
                }

                if (translation.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        thickness = 0.5.dp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    CompositionLocalProvider(
                        LocalLayoutDirection provides if (translationLanguage == "ur") LayoutDirection.Rtl else LayoutDirection.Ltr
                    ) {
                        Text(
                            text = translation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            textAlign = if (translationLanguage == "ur") TextAlign.End else TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

// ============ BISMILLAH ITEM ============

@Composable
fun BismillahItem(
    arabicFontName: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Text(
                text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 26.sp,
                    lineHeight = 44.sp
                ),
                fontFamily = getArabicFont(arabicFontName),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ============ SURAH HEADER FRAME ============

@Composable
fun SurahHeaderFrame(
    header: SurahHeaderInfo,
    arabicFontName: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp)
            )
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            .padding(vertical = 16.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = header.surahNameArabic,
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = getArabicFont(arabicFontName),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = header.surahNameEnglish,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

// ============ PAGE HEADER ============

@Composable
fun MushafPageHeader(
    pageNumber: Int,
    juzNumber: Int,
    hizbNumber: Int?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        shape = RoundedCornerShape(8.dp)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الجزء ${toArabicNumeral(juzNumber)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                hizbNumber?.let {
                    Text(
                        text = "الحزب ${toArabicNumeral(it)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }

                Text(
                    text = toArabicNumeral(pageNumber),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

// ============ PAGE FOOTER ============

@Composable
fun PageFooter(
    pageNumber: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = toArabicNumeral(pageNumber),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }
}

// ============ NAVIGATION BAR ============

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MushafNavigationBar(
    pagerState: PagerState,
    totalPages: Int,
    currentJuz: Int,
    onNavigate: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentPage = pagerState.currentPage + 1

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onNavigate(pagerState.currentPage + 1) },
                enabled = currentPage < totalPages
            ) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = "Next Page",
                    tint = if (currentPage < totalPages)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Juz ${toArabicNumeral(currentJuz)}",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${toArabicNumeral(currentPage)} / ${toArabicNumeral(totalPages)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            IconButton(
                onClick = { onNavigate(pagerState.currentPage - 1) },
                enabled = currentPage > 1
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Previous Page",
                    tint = if (currentPage > 1)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }
    }
}

// ============ HELPER FUNCTIONS ============

private fun toArabicNumeral(number: Int): String {
    val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    return number.toString().map { arabicDigits[it - '0'] }.joinToString("")
}

// ============ SAMPLE DATA FOR PREVIEWS ============

private fun getSampleFullPage(): MushafPage {
    // Simulating a full page from the middle of the Quran (Page 50 - Al-Baqarah continuation)
    return MushafPage(
        pageNumber = 50,
        juzNumber = 1,
        hizbNumber = 3,
        surahHeaders = emptyList(),
        ayat = listOf(
            MushafAya(300, 293, 2, "يَـٰٓأَيُّهَا ٱلَّذِينَ ءَامَنُوٓا۟ إِذَا تَدَايَنتُم بِدَيْنٍ إِلَىٰٓ أَجَلٍ مُّسَمًّى فَٱكْتُبُوهُ", "O you who believe! When you contract a debt for a fixed period, write it down.", "اے ایمان والو! جب تم کسی مقررہ مدت کے لیے آپس میں قرض کا معاملہ کرو تو اسے لکھ لیا کرو", 1, bookmark = true),
            MushafAya(301, 294, 2, "وَلْيَكْتُب بَّيْنَكُمْ كَاتِبٌۢ بِٱلْعَدْلِ", "Let a scribe write it down in justice between you.", "اور تمہارے درمیان کوئی لکھنے والا انصاف سے لکھے", 1),
            MushafAya(302, 295, 2, "وَلَا يَأْبَ كَاتِبٌ أَن يَكْتُبَ كَمَا عَلَّمَهُ ٱللَّهُ", "Let not the scribe refuse to write as Allah has taught him.", "اور لکھنے والا لکھنے سے انکار نہ کرے جیسا اللہ نے اسے سکھایا ہے", 1),
            MushafAya(303, 296, 2, "فَلْيَكْتُبْ وَلْيُمْلِلِ ٱلَّذِى عَلَيْهِ ٱلْحَقُّ", "So let him write and let the one who has the obligation dictate.", "پس وہ لکھے اور جس پر حق ہے وہ لکھوائے", 1, sajda = false),
            MushafAya(304, 297, 2, "وَلْيَتَّقِ ٱللَّهَ رَبَّهُۥ وَلَا يَبْخَسْ مِنْهُ شَيْـًٔا", "And let him fear Allah his Lord and not leave anything out of it.", "اور اپنے رب اللہ سے ڈرے اور اس میں سے کچھ کم نہ کرے", 1),
            MushafAya(305, 298, 2, "فَإِن كَانَ ٱلَّذِى عَلَيْهِ ٱلْحَقُّ سَفِيهًا أَوْ ضَعِيفًا", "But if the one who has the obligation is of limited understanding or weak.", "پھر اگر جس پر حق ہے وہ کم عقل یا کمزور ہو", 1),
            MushafAya(306, 299, 2, "أَوْ لَا يَسْتَطِيعُ أَن يُمِلَّ هُوَ فَلْيُمْلِلْ وَلِيُّهُۥ بِٱلْعَدْلِ", "Or unable to dictate himself, then let his guardian dictate in justice.", "یا خود لکھوانے کی طاقت نہ رکھتا ہو تو اس کا ولی انصاف سے لکھوائے", 1),
            MushafAya(307, 300, 2, "وَٱسْتَشْهِدُوا۟ شَهِيدَيْنِ مِن رِّجَالِكُمْ", "And bring to witness two witnesses from among your men.", "اور اپنے مردوں میں سے دو گواہ بنا لو", 1, bookmark = false, sajda = true, sajdaType = "recommended"),
        )
    )
}

private fun getSamplePageWithHeader(): MushafPage {
    return MushafPage(
        pageNumber = 1,
        juzNumber = 1,
        hizbNumber = 1,
        surahHeaders = listOf(
            SurahHeaderInfo(
                surahNumber = 1,
                surahNameArabic = "سُورَةُ الفَاتِحَة",
                surahNameEnglish = "Al-Fatiha • The Opening",
                showBismillah = true,
                insertBeforeAyaIndex = 0
            )
        ),
        ayat = listOf(
            MushafAya(1, 1, 1, "ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَـٰلَمِينَ", "All praise is for Allah—Lord of all worlds", "تمام تعریفیں اللہ کے لیے ہیں جو تمام جہانوں کا رب ہے", 1),
            MushafAya(2, 2, 1, "ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ", "The Most Compassionate, Most Merciful", "بڑا مہربان نہایت رحم والا", 1),
            MushafAya(3, 3, 1, "مَـٰلِكِ يَوْمِ ٱلدِّينِ", "Master of the Day of Judgment", "روزِ جزا کا مالک", 1),
            MushafAya(4, 4, 1, "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ", "You alone we worship and You alone we ask for help", "ہم تیری ہی عبادت کرتے ہیں اور تجھ ہی سے مدد چاہتے ہیں", 1, bookmark = true),
            MushafAya(5, 5, 1, "ٱهْدِنَا ٱلصِّرَٰطَ ٱلْمُسْتَقِيمَ", "Guide us along the Straight Path", "ہمیں سیدھے راستے کی ہدایت دے", 1),
            MushafAya(6, 6, 1, "صِرَٰطَ ٱلَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ ٱلْمَغْضُوبِ عَلَيْهِمْ وَلَا ٱلضَّآلِّينَ", "The Path of those You have blessed—not those who have incurred Your wrath, nor those who have gone astray", "ان لوگوں کا راستہ جن پر تو نے انعام کیا، نہ ان کا جن پر غضب ہوا اور نہ گمراہوں کا", 1)
        )
    )
}

// ============ PREVIEWS ============

@OptIn(ExperimentalFoundationApi::class)
@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
fun MushafPagerPreview() {
    NimazTheme {
        MushafPager(
            pages = listOf(getSamplePageWithHeader(), getSampleFullPage()),
            initialPage = 0,
            showTranslation = false,
            arabicFontName = "Uthmanic"
        )
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
fun MushafPageViewPreview() {
    NimazTheme {
        MushafPageView(
            page = getSamplePageWithHeader(),
            showTranslation = false,
            arabicFontName = "Uthmanic"
        )
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
fun MushafPageWithTranslationPreview() {
    NimazTheme {
        MushafPageView(
            page = getSamplePageWithHeader(),
            showTranslation = true,
            translationLanguage = "en",
            arabicFontName = "Uthmanic"
        )
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
fun MushafFullPagePreview() {
    NimazTheme {
        MushafPageView(
            page = getSampleFullPage(),
            showTranslation = false,
            arabicFontName = "Uthmanic",
            arabicFontSize = 22
        )
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
fun MushafFullPageWithTranslationPreview() {
    NimazTheme {
        MushafPageView(
            page = getSampleFullPage(),
            showTranslation = true,
            translationLanguage = "en",
            arabicFontName = "Uthmanic"
        )
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
fun MushafUrduTranslationPreview() {
    NimazTheme {
        MushafPageView(
            page = getSamplePageWithHeader(),
            showTranslation = true,
            translationLanguage = "ur",
            arabicFontName = "Uthmanic"
        )
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
fun MushafAyaItemPreview() {
    NimazTheme {
        MushafAyaItem(
            aya = MushafAya(
                ayaNumberInQuran = 1,
                ayaNumberInSurah = 1,
                suraNumber = 1,
                ayaArabic = "ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَـٰلَمِينَ",
                translationEnglish = "All praise is for Allah—Lord of all worlds",
                translationUrdu = "تمام تعریفیں اللہ کے لیے ہیں",
                juzNumber = 1,
                bookmark = true
            ),
            isSelected = false,
            showTranslation = true,
            translationLanguage = "en",
            arabicFontName = "Uthmanic",
            arabicFontSize = 24,
            onClick = {},
            onLongClick = {},
            onBookmarkClick = {},
            onShareClick = {},
            onTafsirClick = {},
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
fun MushafAyaItemSelectedPreview() {
    NimazTheme {
        MushafAyaItem(
            aya = MushafAya(
                ayaNumberInQuran = 307,
                ayaNumberInSurah = 300,
                suraNumber = 2,
                ayaArabic = "وَٱسْتَشْهِدُوا۟ شَهِيدَيْنِ مِن رِّجَالِكُمْ",
                translationEnglish = "And bring to witness two witnesses from among your men.",
                juzNumber = 1,
                sajda = true,
                sajdaType = "recommended"
            ),
            isSelected = true,
            showTranslation = true,
            translationLanguage = "en",
            arabicFontName = "Uthmanic",
            arabicFontSize = 24,
            onClick = {},
            onLongClick = {},
            onBookmarkClick = {},
            onShareClick = {},
            onTafsirClick = {},
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
fun SurahHeaderFramePreview() {
    NimazTheme {
        SurahHeaderFrame(
            header = SurahHeaderInfo(
                surahNumber = 1,
                surahNameArabic = "سُورَةُ الفَاتِحَة",
                surahNameEnglish = "Al-Fatiha • The Opening",
                showBismillah = true,
                insertBeforeAyaIndex = 0
            ),
            arabicFontName = "Uthmanic",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
fun BismillahItemPreview() {
    NimazTheme {
        BismillahItem(
            arabicFontName = "Uthmanic",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
fun MushafPageHeaderPreview() {
    NimazTheme {
        MushafPageHeader(
            pageNumber = 50,
            juzNumber = 3,
            hizbNumber = 5,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Preview(showBackground = true, widthDp = 400)
@Composable
fun MushafNavigationBarPreview() {
    NimazTheme {
        val pagerState = rememberPagerState(
            initialPage = 49,
            pageCount = { 604 }
        )
        MushafNavigationBar(
            pagerState = pagerState,
            totalPages = 604,
            currentJuz = 3,
            onNavigate = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
fun MushafPageTawbahPreview() {
    // Surah At-Tawbah - no Bismillah
    NimazTheme {
        MushafPageView(
            page = MushafPage(
                pageNumber = 187,
                juzNumber = 10,
                hizbNumber = 19,
                surahHeaders = listOf(
                    SurahHeaderInfo(
                        surahNumber = 9,
                        surahNameArabic = "سُورَةُ التَّوْبَة",
                        surahNameEnglish = "At-Tawbah • The Repentance",
                        showBismillah = false,
                        insertBeforeAyaIndex = 0
                    )
                ),
                ayat = listOf(
                    MushafAya(1, 1, 9, "بَرَآءَةٌ مِّنَ ٱللَّهِ وَرَسُولِهِۦٓ إِلَى ٱلَّذِينَ عَـٰهَدتُّم مِّنَ ٱلْمُشْرِكِينَ", "This is a declaration of disassociation from Allah and His Messenger to the polytheists you had made a treaty with", "اللہ اور اس کے رسول کی طرف سے ان مشرکوں سے براءت ہے جن سے تم نے معاہدہ کیا تھا", 10),
                    MushafAya(2, 2, 9, "فَسِيحُوا۟ فِى ٱلْأَرْضِ أَرْبَعَةَ أَشْهُرٍ", "So travel freely throughout the land for four months", "پس تم زمین میں چار مہینے چلو پھرو", 10),
                )
            ),
            showTranslation = true,
            arabicFontName = "Uthmanic"
        )
    }
}