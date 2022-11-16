package com.arshadshah.nimaz.ui.components.ui.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.ui.theme.quranFont

@Composable
fun AyaListUI(ayaList: List<Map<String, String>>, paddingValues: PaddingValues) {
    LazyColumn(userScrollEnabled = true, contentPadding = paddingValues) {
        items(ayaList.size) { index ->
            AyaListItemUI(
                ayaNumber = ayaList[index]["ayaNumber"] ?: "",
                ayaArabic = ayaList[index]["ayaArabic"] ?: "",
                ayaTranslation = ayaList[index]["ayaTranslation"] ?: "",
            )
        }
    }
}

@Composable
fun AyaListItemUI(
    ayaNumber: String,
    ayaArabic: String,
    ayaTranslation: String,
) {

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
        ) {

            Text(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .weight(0.10f),
                text = "$ayaNumber.",
                style = MaterialTheme.typography.bodyLarge
            )

            Column(
                modifier = Modifier
                    .padding(16.dp, 0.dp)
                    .align(Alignment.CenterVertically)
                    .weight(0.80f)
            ) {
                //apply quran font
                Text(
                    text = ayaArabic,
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = quranFont,
                    textAlign = TextAlign.End,
                )
                Text(text = ayaTranslation, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Preview
@Composable
fun AyaListItemUIPreview() {
    NimazTheme {
        //make 10 Aya
        val ayaList = List(10) {
            mapOf(
                "ayaNumber" to it.toString(),
                "ayaArabic" to "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ",
                "ayaTranslation" to "All praise is due to Allah, the Lord of the worlds."
            )
        }
        AyaListUI(ayaList, PaddingValues(8.dp))
    }
}