package com.arshadshah.nimaz.ui.components.quran

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.local.models.LocalSurah
import com.arshadshah.nimaz.ui.components.common.QuranItemNumber
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont
import com.arshadshah.nimaz.utils.PrivateSharedPreferences

@Composable
fun SurahListUI(
    surahs: ArrayList<LocalSurah>,
    onNavigateToAyatScreen: (String, Boolean, String, Int?) -> Unit,
    loading: Boolean,
) {
    val state = rememberLazyListState()
    LazyColumn(
        state = state,
        userScrollEnabled = !loading,
        modifier = Modifier.testTag(AppConstants.TEST_TAG_QURAN_SURAH)
    ) {
        items(surahs.size) { index ->
            SuraListItem(
                isLoading = loading,
                suraNumber = surahs[index].number,
                verseCount = surahs[index].numberOfAyahs,
                arabicName = surahs[index].name,
                englishName = surahs[index].englishName,
                transliteration = surahs[index].englishNameTranslation,
                revelationType = surahs[index].revelationType,
                navigateToAyaScreen = onNavigateToAyatScreen
            )
            //if its not the last item, add a divider
            if (index != surahs.size - 1) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.background,
                    thickness = 2.dp,
                )
            }
        }
    }
}

@Composable
fun SuraListItem(
    suraNumber: Int,
    englishName: String,
    transliteration: String,
    isLoading: Boolean,
    arabicName: String,
    verseCount: Int,
    verseNumber: Int = 0,
    revelationType: String,
    navigateToAyaScreen: (String, Boolean, String, Int?) -> Unit
) {
    val translationType =
        PrivateSharedPreferences(LocalContext.current).getData(
            key = AppConstants.TRANSLATION_LANGUAGE,
            s = "English"
        )
    val language = when (translationType) {
        "English" -> "english"
        "Urdu" -> "urdu"
        else -> "english"
    }

    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable {
                navigateToAyaScreen(suraNumber.toString(), true, language, verseNumber)
            }
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(0.7f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuranItemNumber(number = suraNumber)
            Column(
                modifier = Modifier.padding(2.dp)
            ) {
                Text(text = englishName, style = MaterialTheme.typography.titleLarge)
                Text(text = transliteration, style = MaterialTheme.typography.titleSmall)
                if (verseNumber > 0) {
                    Text(
                        text = "Verse $verseNumber",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                else{
                    Text(
                        text = "$verseCount Verses | $revelationType",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
        Text(
            text = arabicName,
            fontFamily = utmaniQuranFont,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}