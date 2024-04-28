package com.arshadshah.nimaz.ui.components.dashboard

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.lifecycle.LiveData
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.ui.theme.englishQuranTranslation
import com.arshadshah.nimaz.ui.theme.urduFont
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.viewModel.DashboardViewmodel

@Composable
fun DashboardRandomAyatCard(
    onNavigateToAyatScreen: (String, Boolean, String, Int) -> Unit,
    randomAya: LiveData<DashboardViewmodel.RandomAyaState>
) {
    val context = LocalContext.current

    val translationSelected = PrivateSharedPreferences(context).getData(
        AppConstants.TRANSLATION_LANGUAGE,
        "English"
    )

    Card(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
            .clickable {
                randomAya.value?.randomAya?.let {
                    onNavigateToAyatScreen(
                        //number : String , isSurah : Boolean , language : String , scrollToAya : Int?
                        randomAya.value?.surah?.number.toString(),
                        true,
                        PrivateSharedPreferences(context).getData(
                            AppConstants.TRANSLATION_LANGUAGE,
                            "English"
                        ),
                        it.ayaNumberInSurah
                    )
                }
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "${randomAya.value?.randomAya?.ayaNumberInSurah} : ${randomAya.value?.randomAya?.suraNumber}")
            IconButton(
                onClick = {
                    //share the aya
                    val shareIntent = Intent(Intent.ACTION_SEND)
                    shareIntent.type = "text/plain"
                    //create the share message
                    //with the aya text, aya translation
                    //the sura number followed by the aya number
                    shareIntent.putExtra(
                        Intent.EXTRA_TEXT,
                        "Aya of the Day - Chapter ${randomAya.value?.randomAya?.suraNumber}: Verse ${randomAya.value?.randomAya?.ayaNumberInSurah}\n\n" +
                                "${randomAya.value?.randomAya?.ayaArabic} \n\n" +
                                "${if (translationSelected == "Urdu") randomAya.value?.randomAya?.translationUrdu else randomAya.value?.randomAya?.translationEnglish} " +
                                "\n\n${randomAya.value?.randomAya?.suraNumber}:${randomAya.value?.randomAya?.ayaNumberInSurah}" +
                                "\n\nDownload the app to read more: https://play.google.com/store/apps/details?id=com.arshadshah.nimaz"
                    )
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Aya of the Day")

                    //start the share intent
                    context.startActivity(
                        Intent.createChooser(
                            shareIntent,
                            "Share Ramadan Times"
                        )
                    )
                }, modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(id = R.drawable.share_icon),
                    contentDescription = "Share Ramadan Times",
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {

            Column(
                modifier = Modifier
                    .weight(0.90f)
            ) {
                SelectionContainer {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        randomAya.value?.randomAya?.ayaArabic.let {
                            if (it != null) {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontSize = 26.sp,
                                    fontFamily = utmaniQuranFont,
                                    textAlign = if (randomAya.value?.randomAya?.ayaNumberInSurah != 0) TextAlign.Justify else TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(4.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                if (translationSelected == "Urdu") {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            text = "${randomAya.value?.randomAya?.translationUrdu} ۔",
                            style = MaterialTheme.typography.titleSmall,
                            fontSize = 16.sp,
                            fontFamily = urduFont,
                            textAlign = if (randomAya.value?.randomAya?.ayaNumberInSurah != 0) TextAlign.Justify else TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                        )
                    }
                }
                if (translationSelected == "English") {
                    randomAya.value?.randomAya?.translationEnglish.let {
                        if (it != null) {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 16.sp,
                                fontFamily = englishQuranTranslation,
                                textAlign = if (randomAya.value?.randomAya?.ayaNumberInSurah != 0) TextAlign.Justify else TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

}