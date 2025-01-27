package com.arshadshah.nimaz.ui.screens.hadith

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.data.local.models.HadithChapter
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont
import com.arshadshah.nimaz.viewModel.HadithViewModel
import com.arshadshah.nimaz.viewModel.ViewState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithChaptersList(
    bookId: String?,
    viewModel: HadithViewModel = hiltViewModel(),
    onNavigateToAChapter: (Int, Int) -> Unit,
    navController: NavHostController
) {
    LaunchedEffect(Unit) {
        bookId?.let { viewModel.getAllChaptersForABook(it.toInt()) }
    }

    val chaptersState by viewModel.chaptersState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = navController.currentBackStackEntry?.arguments?.getString("bookName")
                            ?: "Hadith Book List"
                    )
                },
                navigationIcon = {
                    OutlinedIconButton(
                        modifier = Modifier
                            .testTag("backButton")
                            .padding(start = 8.dp),
                        onClick = { navController.popBackStack() }
                    ) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            painter = painterResource(id = R.drawable.back_icon),
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(paddingValues)
        ) {
            when (chaptersState) {
                is ViewState.Loading -> LoadingState()
                is ViewState.Success -> {
                    val chapters = (chaptersState as ViewState.Success<List<HadithChapter>>).data
                    if (chapters.isEmpty()) {
                        EmptyState()
                    } else {
                        ChaptersContent(
                            chapters = chapters,
                            onNavigateToAChapter = onNavigateToAChapter
                        )
                    }
                }

                is ViewState.Error -> ErrorState(message = (chaptersState as ViewState.Error).message)
            }
        }
    }
}

@Composable
private fun ChaptersContent(
    chapters: List<HadithChapter>,
    onNavigateToAChapter: (Int, Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            ChapterGroupCard(
                chaptersList = chapters,
                onChapterClick = onNavigateToAChapter
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Loading Chapters...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.quran_icon),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "No Chapters Found",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "This book appears to be empty",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ErrorState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Error Loading Chapters",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ChapterGroupCard(
    chaptersList: List<HadithChapter>,
    onChapterClick: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Section
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Chapters",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${chaptersList.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Chapters List
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                chaptersList.forEach { chapter ->
                    ChapterItem(
                        chapter = chapter,
                        onClick = { onChapterClick(chapter.bookId, chapter.chapterId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChapterItem(
    chapter: HadithChapter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Chapter Number Container
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = chapter.chapterId.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Text Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = chapter.title_english,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (chapter.title_arabic.isNotBlank()) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            text = chapter.title_arabic,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = utmaniQuranFont,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Navigation Arrow
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.angle_small_right_icon),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(16.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}


//small screen preview
@Preview
@Composable
fun HadithChapterGroupCardPreview() {
    val chapter = HadithChapter(
        bookId = 1,
        chapterId = 1,
        title_english = "Chapter 1",
        title_arabic = "الفاتحة"
    )
    NimazTheme {
        ChapterGroupCard(
            chaptersList = listOf(
                chapter, HadithChapter(
                    bookId = 1,
                    chapterId = 2,
                    title_english = "Chapter 2 - The Cow is a long chapter",
                    title_arabic = "البقرة"
                ), chapter
            ),
            onChapterClick = { _, _ -> }
        )
    }
}


@Preview
@Composable
fun ChapterItemPreview() {
    val chapter = HadithChapter(
        bookId = 1,
        chapterId = 1,
        title_english = "Chapter 1",
        title_arabic = "الفاتحة"
    )
    NimazTheme {
        ChapterItem(
            chapter = chapter,
            onClick = { }
        )
    }
}