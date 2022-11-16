package com.arshadshah.nimaz.ui.components.ui.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.ui.theme.quranFont

@Composable
fun SurahListUI(
    surahs: List<Map<String, String>>,
    paddingValues: PaddingValues,
    onNavigateToAyatScreen: (String, Boolean, Boolean) -> Unit
) {
    LazyColumn(userScrollEnabled = true, contentPadding = paddingValues) {
        items(surahs.size) { index ->
            SurahListItemUI(
                surahNumber = surahs[index]["surahNumber"] ?: "0",
                surahAyaAmount = surahs[index]["surahAyaAmount"] ?: "0",
                surahName = surahs[index]["surahName"] ?: "0",
                englishName = surahs[index]["englishName"] ?: "0",
                englishNameTranslation = surahs[index]["englishNameTranslation"] ?: "0",
                type = surahs[index]["type"] ?: "0",
                rukus = surahs[index]["rukus"] ?: "0",
                onNavigateToAyatScreen = onNavigateToAyatScreen
            )
        }
    }
}

@Composable
fun SurahListItemUI(
    surahNumber: String,
    surahAyaAmount: String,
    surahName: String,
    englishName: String,
    englishNameTranslation: String,
    type: String,
    rukus: String,
    onNavigateToAyatScreen: (String, Boolean, Boolean) -> Unit
) {
    val context = LocalContext.current
    ElevatedCard(
        modifier = Modifier
            .padding(4.dp)
            .shadow(8.dp, clip = true, shape = RoundedCornerShape(8.dp))
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
                .clickable(
                    enabled = true,
                    onClick = {
                        onNavigateToAyatScreen(surahNumber, true, true)
                    }
                )
        ) {

            Text(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .weight(0.15f),
                text = "$surahNumber.",
                style = MaterialTheme.typography.bodyLarge
            )

            Column(
                modifier = Modifier
                    .padding(16.dp, 0.dp)
                    .align(Alignment.CenterVertically)
                    .weight(0.50f)
            ) {
                Text(text = englishName, style = MaterialTheme.typography.titleSmall)
                //apply quran font
                Text(
                    text = surahName,
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = quranFont
                )
                Text(text = englishNameTranslation, style = MaterialTheme.typography.titleSmall)
            }

            Column(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .weight(0.30f)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Type: $type",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxWidth()
                )
                Text(
                    text = "Ayat: $surahAyaAmount",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxWidth()
                )
                Text(
                    text = "Ruku: $rukus",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxWidth()
                )
            }
            //an arrow right icon
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowRight,
                contentDescription = "Clear",
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .weight(0.05f)
                    .fillMaxWidth()
            )
        }
    }
}