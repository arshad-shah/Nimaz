package com.arshadshah.nimaz.ui.components.quran

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_QURAN_JUZ
import com.arshadshah.nimaz.data.local.models.LocalJuz
import com.arshadshah.nimaz.ui.components.common.QuranItemNumber
import com.arshadshah.nimaz.ui.components.common.placeholder.material.PlaceholderHighlight
import com.arshadshah.nimaz.ui.components.common.placeholder.material.placeholder
import com.arshadshah.nimaz.ui.components.common.placeholder.material.shimmer
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
    val context = LocalContext.current
    val translationType = remember {
        when (PrivateSharedPreferences(context).getData(AppConstants.TRANSLATION_LANGUAGE, "English")) {
            "Urdu" -> "urdu"
            else -> "english"
        }
    }

    Card(
        onClick = { navigateToAyatScreen(juzNumber.toString(), false, translationType, 0) },
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                QuranItemNumber(number = juzNumber, loading = isLoading)

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Juz $juzNumber",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.placeholder(
                            visible = isLoading,
                            highlight = PlaceholderHighlight.shimmer()
                        )
                    )

                    Text(
                        text = translatedName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.placeholder(
                            visible = isLoading,
                            highlight = PlaceholderHighlight.shimmer()
                        )
                    )
                }
            }

            Text(
                text = name,
                fontFamily = utmaniQuranFont,
                fontSize = 32.sp,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f),
                modifier = Modifier.padding(start = 16.dp).placeholder(
                    visible = isLoading,
                    highlight = PlaceholderHighlight.shimmer()
                )
            )
        }
    }
}