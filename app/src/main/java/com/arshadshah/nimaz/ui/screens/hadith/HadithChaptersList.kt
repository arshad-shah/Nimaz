package com.arshadshah.nimaz.ui.screens.hadith

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.local.models.HadithChapter
import com.arshadshah.nimaz.viewModel.HadithViewModel

@Composable
fun HadithChaptersList(
    paddingValues: PaddingValues, bookId: String?,
    viewModel: HadithViewModel = viewModel(
        key = AppConstants.HADITH_VIEW_MODEL,
    ),
    onNavigateToAChapter: (Int, Int) -> Unit
) {
    LaunchedEffect(Unit) {
        bookId?.let { viewModel.getAllChaptersForABook(it.toInt()) }
    }

    val chaptersList by viewModel.chaptersForABook.collectAsState()

    val cardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(elevation = 8.dp),
        contentColor = MaterialTheme.colorScheme.onSurface,
    )
    Card(
        colors = cardColors,
        modifier = Modifier
            .fillMaxWidth()
            .padding(paddingValues)
            .padding(8.dp)
    ) {
        LazyColumn {
            items(chaptersList.size) { chapter ->
                ChapterItem(chaptersList[chapter]) {
                    onNavigateToAChapter(
                        chaptersList[chapter].bookId,
                        chaptersList[chapter].chapterId
                    )
                }
                if (chapter != chaptersList.size - 1) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.background,
                        thickness = 2.dp,
                    )
                }
            }
        }
    }
}

@Composable
fun ChapterItem(chapter: HadithChapter, onClick: () -> Unit) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable {
                onClick()
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${chapter.chapterId}. ",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            modifier = Modifier.padding(8.dp),
            text = chapter.title_english,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}