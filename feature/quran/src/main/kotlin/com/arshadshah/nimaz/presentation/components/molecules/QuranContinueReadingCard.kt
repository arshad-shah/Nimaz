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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.ReadingProgressCalculator
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.GradientCard
import com.arshadshah.nimaz.presentation.components.atoms.NimazButton
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazButtonType
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * "Resume where you left off" — the Quran home tab's primary action and the **only**
 * gradient card on that screen. Everything else there uses the normal `NimazCard`
 * treatment, so this one keeps its signal instead of competing with a second hero.
 *
 * Deliberately compact: the label, the surah name (English + Arabic on one line), the
 * verse/juz/page metadata and an explicit Resume button share a single row, with the
 * progress bar underneath — roughly half the height of the old stacked layout.
 */
@Composable
internal fun ContinueReadingCard(
    surahNumber: Int,
    ayahNumber: Int,
    juzNumber: Int,
    pageNumber: Int,
    @Suppress("UNUSED_PARAMETER") totalAyahsRead: Int,
    surahName: Surah?,
    onClick: () -> Unit,
    /** Page count of the active Mushaf edition — 604 Madani, 548 IndoPak-16 (#325). */
    totalPages: Int = ReadingProgressCalculator.TOTAL_QURAN_PAGES,
    modifier: Modifier = Modifier
) {
    // The card labels itself with a surah name and "Verse N", so the bar shows progress
    // through the *current surah*. Falls back to mushaf position by page while the surah
    // metadata is still loading.
    val progressFraction = if (surahName != null) {
        ReadingProgressCalculator.surahFraction(ayahNumber, surahName.ayahCount)
    } else {
        ReadingProgressCalculator.pageFraction(pageNumber, totalPages)
    }
    val progressPercent = ReadingProgressCalculator.percent(progressFraction)
    val shape = RoundedCornerShape(20.dp)

    GradientCard(
        gradientColors = NimazColors.QuranColors.BannerGradient,
        onClick = onClick,
        shape = shape,
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = NimazColors.QuranColors.BannerBorder,
                shape = shape
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.quran_home_continue_reading),
                        style = MaterialTheme.typography.labelSmall,
                        color = NimazColors.Primary400,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // English and Arabic share a line — stacking them onto separate rows
                    // was most of the old card's height.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = surahName?.nameEnglish
                                ?: stringResource(
                                    R.string.quran_home_surah_fallback,
                                    surahNumber
                                ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (surahName != null) {
                            ArabicText(
                                text = surahName.nameArabic,
                                size = ArabicTextSize.SMALL,
                                color = NimazColors.QuranColors.BannerAccent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                }

                Spacer(modifier = Modifier.width(12.dp))

                NimazButton(
                    text = stringResource(R.string.quran_home_resume),
                    onClick = onClick,
                    type = NimazButtonType.PILL,
                    size = NimazButtonSize.SMALL,
                    leadingIcon = Icons.Default.PlayArrow
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFraction)
                            .height(5.dp)
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
                nameArabic = "الفاتحة",
                nameEnglish = "Al-Fatihah",
                nameTransliteration = "The Opening",
                revelationType = RevelationType.MECCAN,
                ayahCount = 7,
                orderInMushaf = 5,
                startPage = 1
            ),
            onClick = {}
        )
    }
}
