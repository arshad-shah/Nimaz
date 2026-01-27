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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arshadshah.nimaz.domain.model.Hadith
import com.arshadshah.nimaz.domain.model.HadithGrade
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.viewmodel.HadithEvent
import com.arshadshah.nimaz.presentation.viewmodel.HadithViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithReaderScreen(
    bookId: String,
    chapterId: String,
    onNavigateBack: () -> Unit,
    viewModel: HadithViewModel = hiltViewModel()
) {
    val state by viewModel.readerState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    LaunchedEffect(chapterId) {
        viewModel.onEvent(HadithEvent.LoadChapter(chapterId))
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = state.chapter?.nameEnglish ?: "Loading...",
                subtitle = state.chapter?.let { "${state.hadiths.size} Hadiths" },
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior
            )
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Chapter Header
                item {
                    state.chapter?.let { chapter ->
                        ChapterHeader(
                            titleArabic = chapter.nameArabic,
                            titleEnglish = chapter.nameEnglish,
                            hadithCount = state.hadiths.size
                        )
                    }
                }

                items(
                    items = state.hadiths,
                    key = { it.id }
                ) { hadith ->
                    HadithCard(
                        hadith = hadith,
                        showArabic = state.showArabic,
                        fontSize = state.fontSize,
                        arabicFontSize = state.arabicFontSize,
                        onBookmarkClick = {
                            viewModel.onEvent(
                                HadithEvent.ToggleBookmark(
                                    hadithId = hadith.id,
                                    bookId = bookId,
                                    hadithNumber = hadith.hadithNumber
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChapterHeader(
    titleArabic: String,
    titleEnglish: String,
    hadithCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = NimazColors.Primary.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = titleArabic,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = titleEnglish,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "$hadithCount Hadiths in this chapter",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HadithCard(
    hadith: Hadith,
    showArabic: Boolean,
    fontSize: Float,
    arabicFontSize: Float,
    onBookmarkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Hadith Number
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = hadith.hadithNumber.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Grade Badge
                    hadith.grade?.let { grade ->
                        GradeBadge(grade = grade)
                    }
                }

                Row {
                    IconButton(onClick = onBookmarkClick) {
                        Icon(
                            imageVector = if (hadith.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (hadith.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { /* Share */ }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Arabic Text
            if (showArabic) {
                Text(
                    text = hadith.textArabic,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = arabicFontSize.sp,
                        lineHeight = (arabicFontSize * 1.8f).sp
                    ),
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // English Text
            Text(
                text = hadith.textEnglish,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * 1.5f).sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Narrator Chain
            if (!hadith.narratorName.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "Narrated by: ${hadith.narratorName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            // Reference
            hadith.reference?.let { ref ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = ref,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun GradeBadge(
    grade: HadithGrade,
    modifier: Modifier = Modifier
) {
    val (text, color) = when (grade) {
        HadithGrade.SAHIH -> "Sahih" to NimazColors.StatusColors.Prayed
        HadithGrade.HASAN -> "Hasan" to NimazColors.StatusColors.Late
        HadithGrade.DAIF -> "Da'if" to NimazColors.StatusColors.Missed
        HadithGrade.MAWDU -> "Mawdu'" to Color.Gray
        HadithGrade.UNKNOWN -> "Unknown" to Color.Gray
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
