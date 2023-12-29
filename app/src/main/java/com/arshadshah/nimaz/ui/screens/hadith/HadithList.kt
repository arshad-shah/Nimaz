package com.arshadshah.nimaz.ui.screens.hadith

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.local.models.HadithEntity
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont
import com.arshadshah.nimaz.viewModel.HadithViewModel

@Composable
fun HadithList(
    paddingValues: PaddingValues,
    bookId: String?,
    chapterId: String?,
    viewModel: HadithViewModel = viewModel(
        key = AppConstants.HADITH_VIEW_MODEL,
    )
) {

    LaunchedEffect(Unit) {
        if (bookId != null && chapterId != null) {
            viewModel.getAllHadithForChapter(bookId.toInt(), chapterId.toInt())
        }
    }
    val hadithList by viewModel.hadithForAChapter.collectAsState()
    val loading by viewModel.loading.collectAsState()

    if (loading) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(60.dp),
                strokeWidth = 10.dp,
                strokeCap = StrokeCap.Round,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Loading...", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(hadithList.size) { row ->
                HadithItem(hadithList[row]) { id: Int, isFavourite: Boolean ->
                    viewModel.updateFavouriteStatus(
                        bookId = hadithList[row].bookId,
                        chapterId = hadithList[row].chapterId,
                        id = id,
                        favouriteStatus = isFavourite
                    )
                }
            }
        }
    }

}

@Composable
fun HadithItem(hadith: HadithEntity, updateFavouriteStatus: (Int, Boolean) -> Unit) {
    val reusableModifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)

    val cardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(elevation = 8.dp),
        contentColor = MaterialTheme.colorScheme.onSurface,
        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
    )
    Card(
        colors = cardColors,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = reusableModifier.clip(MaterialTheme.shapes.extraLarge)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = hadith.arabic,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = utmaniQuranFont,
                        fontWeight = FontWeight.SemiBold
                    ),
                    textAlign = TextAlign.Start,
                    modifier = reusableModifier
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            val annotatedString = AnnotatedString(
                text = "${hadith.narrator_english} ${hadith.text_english}"
            )
            Text(
                text = annotatedString,
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic
            )
            // Optionally handle favorite status
            IconToggleButton(checked = hadith.favourite, onCheckedChange = { isChecked ->
                // Handle favorite toggle here
                updateFavouriteStatus(hadith.id, isChecked)
            }) {
                Icon(
                    modifier = Modifier.size(16.dp),
                    painter = if (hadith.favourite) painterResource(id = R.drawable.favorite_icon) else painterResource(
                        id = R.drawable.favorite_icon_unseletced
                    ),
                    contentDescription = "Favorite"
                )
            }
        }
    }
}
