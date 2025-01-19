package com.arshadshah.nimaz.ui.components.dashboard

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.arshadshah.nimaz.ui.components.quran.cleanTextFromBackslash
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

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Compact Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.quran_icon),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Verse of the Day",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                FilledIconButton(
                    onClick = { shareAya(context, translationLanguage, aya) },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.share_icon),
                        contentDescription = "Share",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Surah Info Surface
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = surah?.englishNameTranslation ?: "",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = surah?.englishName ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${aya?.ayaNumberInSurah} : ${aya?.suraNumber}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Arabic Text Surface
            SelectionContainer {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        aya?.ayaArabic?.let { arabicText ->
                            Text(
                                text = arabicText.cleanTextFromBackslash(),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 22.sp,
                                    fontFamily = utmaniQuranFont,
                                    lineHeight = 40.sp
                                ),
                                textAlign = TextAlign.Justify,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            // Translation Surface
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                when (translationLanguage) {
                    "Urdu" -> CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            text = "${aya?.translationUrdu} ۔",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                fontFamily = urduFont,
                                lineHeight = 24.sp
                            ),
                            textAlign = TextAlign.Justify,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    "English" -> aya?.translationEnglish?.let { englishText ->
                        Text(
                            text = englishText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                fontFamily = englishQuranTranslation,
                                lineHeight = 20.sp
                            ),
                            textAlign = TextAlign.Justify,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // Action Button
            Button(
                onClick = {
                    aya?.let {
                        onNavigateToAyatScreen(
                            surah?.number.toString(),
                            true,
                            translationLanguage,
                            it.ayaNumberInSurah
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.quran_icon),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Read Full Surah",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

private fun shareAya(context: Context, translationLanguage: String, aya: LocalAya?) {
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