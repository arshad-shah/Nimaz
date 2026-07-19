package com.arshadshah.nimaz.presentation.screens.hadith

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.navigation.ScreenTags
import com.arshadshah.nimaz.core.share.ContentShareManager
import com.arshadshah.nimaz.core.share.Shareables
import com.arshadshah.nimaz.core.util.formatGrouped
import com.arshadshah.nimaz.domain.model.Hadith
import com.arshadshah.nimaz.domain.model.HadithBook
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazIcon
import com.arshadshah.nimaz.presentation.components.atoms.NimazLoadingState
import com.arshadshah.nimaz.presentation.components.atoms.HadithArabicText
import com.arshadshah.nimaz.presentation.components.atoms.NimazActionPill
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazPillActionButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazSectionHeader
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.components.organisms.NimazStatData
import com.arshadshah.nimaz.presentation.components.organisms.NimazStatsGrid
import com.arshadshah.nimaz.presentation.viewmodel.HadithEvent
import com.arshadshah.nimaz.presentation.viewmodel.HadithViewModel
import kotlinx.coroutines.launch
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadge
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeDefaults
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeEmphasis
import com.arshadshah.nimaz.presentation.components.atoms.NimazBadgeSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithCollectionScreen(
    onNavigateBack: () -> Unit,
    onNavigateToBook: (String) -> Unit,
    onNavigateToBookmarks: () -> Unit,
    onNavigateToSearch: () -> Unit,
    viewModel: HadithViewModel = hiltViewModel()
) {
    val state by viewModel.collectionState.collectAsState()
    val bookmarksState by viewModel.bookmarksState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val context = LocalContext.current
    val shareScope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.hadith_title),
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        NimazIcon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.search)
                        )
                    }
                    IconButton(onClick = onNavigateToBookmarks) {
                        NimazIcon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = stringResource(R.string.bookmarks)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (state.isLoading) {
            NimazLoadingState(modifier = Modifier.padding(paddingValues))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .testTag(ScreenTags.HadithBookList),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    NimazStatsGrid(
                        stats = listOf(
                            NimazStatData(value = "0", label = stringResource(R.string.read_today)),
                            NimazStatData(
                                value = "${bookmarksState.bookmarks.size}",
                                label = stringResource(R.string.bookmarked)
                            ),
                            NimazStatData(value = "0", label = stringResource(R.string.day_streak))
                        ),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }

                item {
                    val hadithOfTheDay = state.hadithOfTheDay
                    val fallbackArabic = stringResource(R.string.hadith_fallback_arabic)
                    val fallbackEnglish = stringResource(R.string.hadith_fallback_english)
                    val fallbackSource = stringResource(R.string.hadith_fallback_source)
                    HadithOfTheDayCard(
                        hadith = hadithOfTheDay,
                        onBookmarkClick = {
                            hadithOfTheDay?.let { hadith ->
                                viewModel.onEvent(
                                    HadithEvent.ToggleBookmark(
                                        hadithId = hadith.id,
                                        bookId = hadith.bookId,
                                        hadithNumber = hadith.hadithNumberInBook
                                    )
                                )
                            }
                        },
                        onShareClick = {
                            shareScope.launch {
                                if (hadithOfTheDay != null) {
                                    ContentShareManager.shareBranded(
                                        context,
                                        Shareables.hadith(context, hadithOfTheDay)
                                    )
                                } else {
                                    val fallbackBody = buildString {
                                        appendLine(fallbackArabic)
                                        appendLine()
                                        appendLine(fallbackEnglish)
                                        appendLine()
                                        append(fallbackSource)
                                    }
                                    ContentShareManager.shareText(
                                        context,
                                        Shareables.text(fallbackBody)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }

                item {
                    NimazSectionHeader(
                        title = stringResource(R.string.kutub_al_sittah),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                item {
                    BooksGrid(
                        books = state.books,
                        onBookClick = onNavigateToBook,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HadithOfTheDayCard(
    hadith: Hadith?,
    onBookmarkClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val arabicText = hadith?.textArabic ?: stringResource(R.string.hadith_fallback_arabic)
    val translationText = hadith?.textEnglish ?: stringResource(R.string.hadith_fallback_english)
    val source = hadith?.reference ?: stringResource(R.string.hadith_fallback_source)
    val grade = hadith?.let { hadithGradeDisplay(it.grade) }
    val isBookmarked = hadith?.isBookmarked == true

    NimazCard(
        modifier = modifier.fillMaxWidth(),
        style = NimazCardStyle.ELEVATED,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NimazBadge(
                    text = stringResource(R.string.hadith_of_the_day),
                    size = NimazBadgeSize.SMALL,
                    icon = Icons.Default.Star,
                    colors = NimazBadgeDefaults.feature(
                        color = NimazColors.Gold500,
                        emphasis = NimazBadgeEmphasis.SOFT
                    )
                )
                grade?.let { HadithGradeChip(label = it.label, color = it.color) }
            }

            Spacer(modifier = Modifier.height(15.dp))

            HadithArabicText(
                text = arabicText,
                size = ArabicTextSize.MEDIUM,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(15.dp))

            Text(
                text = translationText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(15.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = source,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )

                NimazActionPill {
                    NimazPillActionButton(
                        icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = stringResource(R.string.bookmark),
                        onClick = onBookmarkClick,
                        active = isBookmarked,
                        activeColor = NimazColors.Gold500
                    )
                    NimazPillActionButton(
                        icon = Icons.Default.Share,
                        contentDescription = stringResource(R.string.share),
                        onClick = onShareClick
                    )
                }
            }
        }
    }
}

@Composable
private fun BooksGrid(
    books: List<HadithBook>,
    onBookClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = books.chunked(2)
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        rows.forEach { rowBooks ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowBooks.forEach { book ->
                    BookCard(
                        book = book,
                        onClick = { onBookClick(book.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowBooks.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun BookCard(
    book: HadithBook,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bookGradient = getBookGradient(book.id)

    NimazCard(
        modifier = modifier,
        style = NimazCardStyle.FILLED,
        onClick = onClick,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(brush = Brush.linearGradient(bookGradient)),
                contentAlignment = Alignment.Center
            ) {
                NimazIcon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = Color.White,
                    iconSize = 26.dp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = book.nameEnglish,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (book.authorName.isNotBlank()) {
                Text(
                    text = book.authorName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(
                    R.string.hadith_count_format,
                    formatGrouped(book.totalHadiths)
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun getBookGradient(bookId: String): List<Color> {
    return when (bookId.lowercase()) {
        "bukhari" -> NimazColors.HadithCollectionColors.Bukhari
        "muslim" -> NimazColors.HadithCollectionColors.Muslim
        "tirmidhi" -> NimazColors.HadithCollectionColors.Tirmidhi
        "nasai" -> NimazColors.HadithCollectionColors.Nasai
        "abudawud" -> NimazColors.HadithCollectionColors.AbuDawud
        "ibnmajah" -> NimazColors.HadithCollectionColors.IbnMajah
        else -> NimazColors.HadithCollectionColors.Default
    }
}
