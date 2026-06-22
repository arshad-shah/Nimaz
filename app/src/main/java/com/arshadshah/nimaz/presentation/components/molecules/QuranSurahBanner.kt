package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.res.stringResource
import com.arshadshah.nimaz.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme

@Composable
internal fun SurahBanner(
    surahNameArabic: String,
    surahNameEnglish: String,
    surahMeaning: String,
    revelationType: RevelationType,
    ayahCount: Int,
    showBismillah: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(15.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(NimazColors.QuranColors.CardGradientStart, NimazColors.QuranColors.CardGradientEnd)
                )
            )
            .border(1.dp, NimazColors.QuranColors.CardBorder, RoundedCornerShape(20.dp))
            .padding(25.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ArabicText(
                text = surahNameArabic,
                size = ArabicTextSize.EXTRA_LARGE,
                color = NimazColors.QuranColors.CardAccentGold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = surahNameEnglish,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = NimazColors.QuranColors.CardAccentGold
            )

            Text(
                text = surahMeaning,
                style = MaterialTheme.typography.bodySmall,
                color = NimazColors.QuranColors.CardAccentGold.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (revelationType == RevelationType.MECCAN) stringResource(R.string.quran_meccan) else stringResource(R.string.quran_medinan),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.quran_ayahs_count_format, ayahCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            if (showBismillah) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(20.dp))

                ArabicText(
                    text = "\u0628\u0650\u0633\u0652\u0645\u0650 \u0671\u0644\u0644\u0651\u064E\u0647\u0650 \u0671\u0644\u0631\u0651\u064E\u062D\u0652\u0645\u064E\u0670\u0646\u0650 \u0671\u0644\u0631\u0651\u064E\u062D\u0650\u064A\u0645\u0650",
                    size = ArabicTextSize.LARGE,
                    color = NimazColors.QuranColors.CardAccentGold
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Surah Banner")
@Composable
internal fun SurahBannerPreview() {
    NimazTheme {
        SurahBanner(
            surahNameArabic = "\u0627\u0644\u0641\u0627\u062A\u062D\u0629",
            surahNameEnglish = "Al-Fatihah",
            surahMeaning = "The Opening",
            revelationType = RevelationType.MECCAN,
            ayahCount = 7,
            showBismillah = true
        )
    }
}
