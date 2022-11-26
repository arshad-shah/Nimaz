package com.arshadshah.nimaz.ui.components.ui.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.data.remote.models.Aya
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.ui.theme.quranFont
import com.arshadshah.nimaz.utils.AyaEndProcesser

@Composable
fun AyaListUI(ayaList: ArrayList<Aya>, paddingValues: PaddingValues) {
    LazyColumn(userScrollEnabled = true, contentPadding = paddingValues) {
        items(ayaList.size) { index ->
            AyaListItemUI(
                ayaNumber = ayaList[index].ayaNumber.toString(),
                ayaArabic = ayaList[index].ayaArabic,
                ayaTranslation = ayaList[index].translation,
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
            .fillMaxHeight()
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
        ) {

            if (ayaNumber != "0") {
                Text(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .weight(0.12f)
                        .padding(start = 8.dp),
                    text = "$ayaNumber.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Column(
                modifier = Modifier
                    .weight(0.90f)
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = AyaEndProcesser(ayaArabic, ayaNumber.toInt()),
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = quranFont,
                        textAlign = if (ayaNumber != "0") TextAlign.Justify else TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = ayaTranslation,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = if (ayaNumber != "0") TextAlign.Justify else TextAlign.Center,
                    modifier = if (ayaNumber != "0") Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp) else Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                )
            }
        }
    }
}

@Preview
@Composable
fun AyaListItemUIPreview() {
    NimazTheme {
        //make 10 LocalAya
        val ayaList = ArrayList<Aya>()
        //add the aya to the list
        ayaList.add(
            Aya(
                1,
                "بسم الله الرحمن الرحيم",
                "In the name of Allah, the Entirely Merciful, the Especially Merciful.", "Surah",
                1
            )
        )
        ayaList.add(
            Aya(
                2,
                "الحمد لله رب العالمين",
                "All praise is due to Allah, Lord of the worlds.", "Surah",
                1
            )
        )
        ayaList.add(
            Aya(
                3, "الرحمن الرحيم", "The Entirely Merciful, the Especially Merciful.", "Surah",
                1
            )
        )
        ayaList.add(
            Aya(
                4, "مالك يوم الدين", "Master of the Day of Judgment.", "Surah",
                1
            )
        )
        ayaList.add(
            Aya(
                5,
                "إياك نعبد وإياك نستعين",
                "You alone do we worship, and You alone do we implore for help.", "Surah",
                1
            )
        )
        ayaList.add(
            Aya(
                6, "اهدنا الصراط المستقيم", "Guide us to the straight path.", "Surah",
                1
            )
        )
        ayaList.add(
            Aya(
                7,
                "صراط الذين أنعمت عليهم غير المغضوب عليهم ولا الضالين",
                "The path of those upon whom You have bestowed favor, not of those who have evoked [Your] anger or of those who are astray.",
                "Surah",
                1
            )
        )

        AyaListUI(ayaList, PaddingValues(8.dp))
    }
}