package com.arshadshah.nimaz.ui.components.dashboard

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.local.models.LocalAya
import com.arshadshah.nimaz.ui.theme.englishQuranTranslation
import com.arshadshah.nimaz.ui.theme.urduFont
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.viewModel.DashboardViewModel

@Composable
fun DashboardRandomAyatCard(
    onNavigateToAyatScreen: (String, Boolean, String, Int) -> Unit,
    randomAya: DashboardViewModel.RandomAyaState?,
    isLoading: Boolean
) {
    val context = LocalContext.current
    val translationLanguage = PrivateSharedPreferences(context).getData(
        AppConstants.TRANSLATION_LANGUAGE,
        "English"
    )
    val aya = randomAya?.randomAya
    val surah = randomAya?.surah

    Log.d("DashboardRandomAya", "Random Aya: $randomAya - Surah: $surah - IsLoading: $isLoading")

    Card(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with badge and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.quran_icon),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Verse of the Day",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = { shareAya(context, translationLanguage, aya) }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.share_icon),
                        contentDescription = "Share",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Reference number with divider
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = surah?.englishNameTranslation ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${aya?.ayaNumberInSurah} : ${aya?.suraNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
            }

            // Arabic Text
            SelectionContainer {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    aya?.ayaArabic?.let { arabicText ->
                        Text(
                            text = arabicText,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 26.sp,
                                fontFamily = utmaniQuranFont,
                                lineHeight = 46.sp
                            ),
                            textAlign = if (aya.ayaNumberInSurah != 0) TextAlign.Justify else TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Translation
            when (translationLanguage) {
                "Urdu" -> CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = "${aya?.translationUrdu} ۔",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 16.sp,
                            fontFamily = urduFont,
                            lineHeight = 28.sp
                        ),
                        textAlign = if (aya?.ayaNumberInSurah != 0) TextAlign.Justify else TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                "English" -> aya?.translationEnglish?.let { englishText ->
                    Text(
                        text = englishText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 16.sp,
                            fontFamily = englishQuranTranslation,
                            lineHeight = 24.sp
                        ),
                        textAlign = if (aya.ayaNumberInSurah != 0) TextAlign.Justify else TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        aya?.let {
                            onNavigateToAyatScreen(
                                surah?.number.toString(),
                                true,
                                translationLanguage,
                                it.ayaNumberInSurah
                            )
                        }
                    }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.quran_icon),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Read Full Surah",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}
private fun shareAya(
    context: Context,
    translationLanguage: String,
    aya: LocalAya?
) {
    val shareText = buildString {
        append("Aya of the Day - Chapter ${aya?.suraNumber}: Verse ${aya?.ayaNumberInSurah}\n\n")
        append("${aya?.ayaArabic}\n\n")
        append(if (translationLanguage == "Urdu") aya?.translationUrdu else aya?.translationEnglish)
        append("\n\n${aya?.suraNumber}:${aya?.ayaNumberInSurah}")
        append("\n\nDownload the app to read more: https://play.google.com/store/apps/details?id=com.arshadshah.nimaz")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
        putExtra(Intent.EXTRA_SUBJECT, "Aya of the Day")
    }

    context.startActivity(Intent.createChooser(intent, "Share Aya"))
}