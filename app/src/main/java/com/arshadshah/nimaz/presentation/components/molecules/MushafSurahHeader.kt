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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.theme.NimazTheme

// Bismillah text constant
private const val BISMILLAH_TEXT_DISPLAY = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"

// Theme colors for Mushaf header (matching QuranReaderScreen patterns)
private val MushafHeaderGradientStart = Color(0xFF115E59)
private val MushafHeaderGradientEnd = Color(0xFF042F2E)
private val MushafHeaderBorder = Color(0xFF0F766E)
private val MushafGoldColor = Color(0xFFEAB308)

/**
 * Decorative surah header shown when a new surah starts on a Mushaf page.
 *
 * Features:
 * - Ornamental frame with gradient background
 * - Surah name in Arabic (gold/amber color)
 * - Surah number and ayah count
 * - Bismillah below (except for Surah 1 and 9)
 *
 * @param surah The surah to display header for
 * @param showBismillah Whether to show Bismillah below the header
 * @param modifier Modifier for the composable
 */
@Composable
fun MushafSurahHeader(
    surah: Surah,
    modifier: Modifier = Modifier,
    showBismillah: Boolean = surah.number != 1 && surah.number != 9
) {
    MushafSurahHeader(
        surahNumber = surah.number,
        surahNameArabic = surah.nameArabic,
        surahNameEnglish = surah.nameEnglish,
        ayahCount = surah.ayahCount,
        showBismillah = showBismillah,
        modifier = modifier
    )
}

/**
 * Decorative surah header with explicit parameters.
 */
@Composable
fun MushafSurahHeader(
    surahNumber: Int,
    surahNameArabic: String,
    surahNameEnglish: String,
    ayahCount: Int,
    modifier: Modifier = Modifier,
    showBismillah: Boolean = surahNumber != 1 && surahNumber != 9
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Main header box with gradient background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        listOf(MushafHeaderGradientStart, MushafHeaderGradientEnd)
                    )
                )
                .border(1.dp, MushafHeaderBorder, RoundedCornerShape(16.dp))
                .padding(vertical = 16.dp, horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: Surah number and English name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Surah number badge
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = surahNumber.toString(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Column {
                        Text(
                            text = surahNameEnglish,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = "$ayahCount Ayahs",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                // Right side: Arabic name
                if (surahNameArabic.isNotEmpty()) {
                    ArabicText(
                        text = surahNameArabic,
                        size = ArabicTextSize.LARGE,
                        color = MushafGoldColor
                    )
                }
            }
        }

        // Bismillah (shown for all surahs except Al-Fatihah and At-Tawbah)
        if (showBismillah) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                ArabicText(
                    text = BISMILLAH_TEXT_DISPLAY,
                    size = ArabicTextSize.LARGE,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Compact surah separator for inline use within continuous text.
 * Used when a surah boundary occurs mid-page.
 */
@Composable
fun MushafSurahSeparator(
    surahNumber: Int,
    surahNameArabic: String,
    surahNameEnglish: String,
    modifier: Modifier = Modifier,
    showBismillah: Boolean = surahNumber != 1 && surahNumber != 9
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        listOf(MushafHeaderGradientStart, MushafHeaderGradientEnd)
                    )
                )
                .border(1.dp, MushafHeaderBorder, RoundedCornerShape(14.dp))
                .padding(vertical = 14.dp, horizontal = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = surahNumber.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = surahNameEnglish,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                if (surahNameArabic.isNotEmpty()) {
                    ArabicText(
                        text = surahNameArabic,
                        size = ArabicTextSize.MEDIUM,
                        color = MushafGoldColor
                    )
                }
            }
        }

        if (showBismillah) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                ArabicText(
                    text = BISMILLAH_TEXT_DISPLAY,
                    size = ArabicTextSize.LARGE,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ==================== PREVIEWS ====================

@Preview(showBackground = true, name = "Mushaf Surah Header - Al-Fatihah")
@Composable
private fun MushafSurahHeaderFatihahPreview() {
    NimazTheme {
        MushafSurahHeader(
            surah = sampleSurahFatihah
        )
    }
}

@Preview(showBackground = true, name = "Mushaf Surah Header - Al-Baqarah")
@Composable
private fun MushafSurahHeaderBaqarahPreview() {
    NimazTheme {
        MushafSurahHeader(
            surahNumber = 2,
            surahNameArabic = "البقرة",
            surahNameEnglish = "Al-Baqarah",
            ayahCount = 286,
            showBismillah = true
        )
    }
}

@Preview(showBackground = true, name = "Mushaf Surah Header - At-Tawbah (No Bismillah)")
@Composable
private fun MushafSurahHeaderTawbahPreview() {
    NimazTheme {
        MushafSurahHeader(
            surahNumber = 9,
            surahNameArabic = "التوبة",
            surahNameEnglish = "At-Tawbah",
            ayahCount = 129,
            showBismillah = false
        )
    }
}

@Preview(showBackground = true, name = "Mushaf Surah Separator")
@Composable
private fun MushafSurahSeparatorPreview() {
    NimazTheme {
        MushafSurahSeparator(
            surahNumber = 3,
            surahNameArabic = "آل عمران",
            surahNameEnglish = "Ali 'Imran",
            showBismillah = true
        )
    }
}

// Sample data for previews
internal val sampleSurahFatihah = Surah(
    number = 1,
    nameArabic = "الفاتحة",
    nameEnglish = "Al-Fatihah",
    nameTransliteration = "The Opening",
    revelationType = RevelationType.MECCAN,
    ayahCount = 7,
    juzStart = 1,
    orderInMushaf = 1
)

internal val sampleSurahBaqarah = Surah(
    number = 2,
    nameArabic = "البقرة",
    nameEnglish = "Al-Baqarah",
    nameTransliteration = "The Cow",
    revelationType = RevelationType.MEDINAN,
    ayahCount = 286,
    juzStart = 1,
    orderInMushaf = 2
)
