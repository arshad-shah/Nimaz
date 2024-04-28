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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_QURAN_JUZ
import com.arshadshah.nimaz.data.local.models.LocalJuz
import com.arshadshah.nimaz.ui.components.common.QuranItemNumber
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont
import com.arshadshah.nimaz.utils.PrivateSharedPreferences

@Composable
fun JuzListUI(
    juz: ArrayList<LocalJuz>,
    onNavigateToAyatScreen: (String, Boolean, String, Int?) -> Unit,
    loading: Boolean,
) {
    val state = rememberLazyListState()
    LazyColumn(
        state = state,
        userScrollEnabled = !loading,
        modifier = Modifier.testTag(TEST_TAG_QURAN_JUZ)
    ) {
        items(juz.size) { index ->
            JuzListItem(
                isLoading = loading,
                juzNumber = juz[index].number,
                name = juz[index].name,
                translatedName = juz[index].tname,
                navigateToAyatScreen = onNavigateToAyatScreen
            )
            //if its not the last item, add a divider
            if (index != juz.size - 1) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.background,
                    thickness = 2.dp,
                )
            }
        }
    }
}


@Composable
fun JuzListItem(
    juzNumber: Int,
    isLoading: Boolean,
    name: String,
    translatedName: String,
    navigateToAyatScreen: (String, Boolean, String, Int?) -> Unit
) {
    //get the translation type from shared preferences
    val translationType =
        PrivateSharedPreferences(LocalContext.current).getData(
            key = AppConstants.TRANSLATION_LANGUAGE,
            s = "English"
        )
    val translation = when (translationType) {
        "English" -> "english"
        "Urdu" -> "urdu"
        else -> "english"
    }
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable {
                navigateToAyatScreen(juzNumber.toString(), false, translation, 0)
            }
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuranItemNumber(number = juzNumber)
            Column(
                modifier = Modifier.padding(2.dp)
            ) {
                Text(
                    text = "Juz $juzNumber",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = translatedName,
                    style = MaterialTheme.typography.titleSmall,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            modifier = Modifier.padding(2.dp),
            text = name,
            fontFamily = utmaniQuranFont,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}