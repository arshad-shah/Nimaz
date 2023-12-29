package com.arshadshah.nimaz.ui.screens.hadith

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.local.models.HadithChapter
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont
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

    LazyColumn(
        contentPadding = paddingValues,
    ) {
        items(chaptersList.size) { chapter ->
            ChapterItem(chaptersList[chapter]) {
                onNavigateToAChapter(
                    chaptersList[chapter].bookId,
                    chaptersList[chapter].chapterId
                )
            }
        }
    }
}

@Composable
fun ChapterItem(chapter: HadithChapter, onClick: () -> Unit) {
    val cardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(elevation = 8.dp),
        contentColor = MaterialTheme.colorScheme.onSurface,
        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
    )
    Card(
        colors = cardColors,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable {
                onClick()
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "${chapter.chapterId}. ")
            Column(modifier = Modifier.padding(16.dp)) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = chapter.title_arabic,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = utmaniQuranFont,
                            fontWeight = FontWeight.SemiBold
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .fillMaxWidth()
                    )
                }
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = chapter.title_english,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}