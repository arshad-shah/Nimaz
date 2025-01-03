package com.arshadshah.nimaz.ui.components.quran

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.data.local.models.LocalSurah
import com.arshadshah.nimaz.ui.components.common.QuranItemNumber
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.ui.components.common.placeholder.material.PlaceholderHighlight
import com.arshadshah.nimaz.ui.components.common.placeholder.material.placeholder
import com.arshadshah.nimaz.ui.components.common.placeholder.material.shimmer
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
    ) {
        items(surahs.size) { index ->
                SurahCard(
                    surah = surahs[index],
                    loading = loading,
                    onNavigate = onNavigateToAyatScreen
                )
        }
    }
}


@Composable
fun SurahCard(
    surah: LocalSurah,
    onNavigate: (String, Boolean, String, Int?) -> Unit,
    loading: Boolean
) {
    val context = LocalContext.current
    val language = remember {
        when (PrivateSharedPreferences(context)
            .getData(AppConstants.TRANSLATION_LANGUAGE, "English")) {
            "Urdu" -> "urdu"
            else -> "english"
        }
    }

    Card(
        onClick = {
            if(!loading){
                onNavigate(surah.number.toString(), true, language, 0)
            }
        },
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
                QuranItemNumber(number = surah.number, loading = loading)

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = surah.englishName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.placeholder(
                            visible = loading,
                            highlight = PlaceholderHighlight.shimmer()
                        )
                    )

                    Text(
                        text = surah.englishNameTranslation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.placeholder(
                            visible = loading,
                            highlight = PlaceholderHighlight.shimmer()
                        )
                    )

                    Text(
                        text = "${surah.numberOfAyahs} Verses • ${surah.revelationType}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.placeholder(
                            visible = loading,
                            highlight = PlaceholderHighlight.shimmer()
                        )
                    )
                }
            }

            Text(
                text = surah.name,
                fontFamily = utmaniQuranFont,
                fontSize = 32.sp,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f),
                modifier = Modifier.padding(start = 16.dp).placeholder(
                    visible = loading,
                    highlight = PlaceholderHighlight.shimmer()
                )
            )
        }
    }
}