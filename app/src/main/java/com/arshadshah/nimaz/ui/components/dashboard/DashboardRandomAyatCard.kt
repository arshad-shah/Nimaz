package com.arshadshah.nimaz.ui.components.dashboard

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.viewModel.DashboardViewModel
import com.arshadshah.nimaz.ui.theme.*
import com.arshadshah.nimaz.ui.components.quran.cleanTextFromBackslash

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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 8.dp
        ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.quran_icon),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Verse of the Day",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Share Button
                    FilledIconButton(
                        onClick = { shareAya(context, translationLanguage, aya) },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.share_icon),
                            contentDescription = "Share",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Surah Info Section
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = surah?.englishNameTranslation ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = surah?.englishName ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${aya?.ayaNumberInSurah} : ${aya?.suraNumber}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Arabic Text
            SelectionContainer {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        aya?.ayaArabic?.let { arabicText ->
                            Text(
                                text = arabicText.cleanTextFromBackslash(),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 26.sp,
                                    fontFamily = utmaniQuranFont,
                                    lineHeight = 46.sp
                                ),
                                textAlign = TextAlign.Justify,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }

            // Translation
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                when (translationLanguage) {
                    "Urdu" -> CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            text = "${aya?.translationUrdu} ۔",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 16.sp,
                                fontFamily = urduFont,
                                lineHeight = 28.sp
                            ),
                            textAlign = TextAlign.Justify,
                            modifier = Modifier.padding(16.dp)
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
                            textAlign = TextAlign.Justify,
                            modifier = Modifier.padding(16.dp)
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
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.quran_icon),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Read Full Surah",
                    style = MaterialTheme.typography.labelLarge
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