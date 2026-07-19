package com.arshadshah.nimaz.presentation.components.molecules

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardStyle
import com.arshadshah.nimaz.presentation.components.atoms.NimazCardTone
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ContinueReadingCard(
    surahNumber: Int,
    ayahNumber: Int,
    juzNumber: Int,
    pageNumber: Int,
    totalAyahsRead: Int,
    surahName: Surah?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalAyahs = 6236
    val progressFraction = (totalAyahsRead.toFloat() / totalAyahs).coerceIn(0f, 1f)
    val progressPercent = (progressFraction * 100).toInt()

    NimazCard(
        style = NimazCardStyle.FILLED,
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tone = NimazCardTone.TRANSPARENT
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(NimazColors.QuranColors.BannerGradient),
                    shape = RoundedCornerShape(20.dp)
                )
                .border(
                    width = 1.dp,
                    color = NimazColors.QuranColors.BannerBorder,
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = stringResource(R.string.quran_home_continue_reading),
                    style = MaterialTheme.typography.labelSmall,
                    color = NimazColors.Primary400,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = surahName?.nameEnglish
                        ?: stringResource(R.string.quran_home_surah_fallback, surahNumber),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (surahName != null) {
                    ArabicText(
                        text = surahName.nameArabic,
                        size = ArabicTextSize.MEDIUM,
                        color = NimazColors.QuranColors.BannerAccent
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.quran_home_verse_format, ayahNumber),
                        style = MaterialTheme.typography.bodySmall,
                        color = NimazColors.Gray300
                    )
                    Text(
                        text = stringResource(R.string.quran_home_juz_format, juzNumber),
                        style = MaterialTheme.typography.bodySmall,
                        color = NimazColors.Gray300
                    )
                    Text(
                        text = stringResource(R.string.quran_home_page_format, pageNumber),
                        style = MaterialTheme.typography.bodySmall,
                        color = NimazColors.Gray300
                    )
                }

                Spacer(modifier = Modifier.height(15.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(NimazColors.QuranColors.BannerAccent)
                        )
                    }

                    Text(
                        text = "$progressPercent%",
                        style = MaterialTheme.typography.bodySmall,
                        color = NimazColors.Gray300
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ContinueReadingCardPreview() {
    NimazTheme {
        ContinueReadingCard(
            surahNumber = 1,
            ayahNumber = 5,
            juzNumber = 1,
            pageNumber = 1,
            totalAyahsRead = 150,
            surahName = Surah(
                number = 1,
                nameArabic = "\u0627\u0644\u0641\u0627\u062A\u062D\u0629",
                nameEnglish = "Al-Fatihah",
                nameTransliteration = "The Opening",
                revelationType = RevelationType.MECCAN,
                ayahCount = 7,
                juzStart = 1,
                orderInMushaf = 5,
                startPage = 1
            ),
            onClick = {}
        )
    }
}
