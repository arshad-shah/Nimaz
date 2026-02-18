package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.SajdaType
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.atoms.NimazBottomSheet
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * Bottom sheet showing translation and transliteration for an ayah.
 *
 * Opened from the "Translation" button in [AyahActionPopup].
 * Contains only display content — no action buttons.
 *
 * @param ayah The ayah to display
 * @param surahName Optional surah name
 * @param showTranslation Whether to show translation
 * @param showTransliteration Whether to show transliteration
 * @param onDismissRequest Callback when dismissed
 * @param sheetState Sheet state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AyahTranslationBottomSheet(
    ayah: Ayah,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    surahName: String? = null,
    showTranslation: Boolean = true,
    showTransliteration: Boolean = false,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    NimazBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier
    ) {
        AyahTranslationContent(
            ayah = ayah,
            surahName = surahName,
            showTranslation = showTranslation,
            showTransliteration = showTransliteration
        )
    }
}

@Composable
fun AyahTranslationContent(
    ayah: Ayah,
    modifier: Modifier = Modifier,
    surahName: String? = null,
    showTranslation: Boolean = true,
    showTransliteration: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = surahName ?: "Surah ${ayah.surahNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Ayah ${ayah.ayahNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ) {
                Text(
                    text = "Juz ${ayah.juzNumber} | P${ayah.pageNumber}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Arabic text preview
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            ArabicText(
                text = ayah.textArabic,
                size = ArabicTextSize.MEDIUM,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }

        // Sajda indicator
        if (ayah.sajdaType != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFDC2626).copy(alpha = 0.15f)
            ) {
                Text(
                    text = if (ayah.sajdaType == SajdaType.OBLIGATORY)
                        "۩ Sajdah (Wajib)"
                    else
                        "۩ Sajdah (Recommended)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFDC2626),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // Translation
        if (showTranslation && !ayah.translation.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Translation",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = ayah.translation,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Transliteration
        if (showTransliteration && !ayah.transliteration.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Transliteration",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = ayah.transliteration,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ==================== PREVIEWS ====================

@Preview(showBackground = true, name = "Translation Content - Both")
@Composable
private fun AyahTranslationContentBothPreview() {
    NimazTheme {
        Surface {
            AyahTranslationContent(
                ayah = translationPreviewAyah,
                surahName = "Al-Fatihah",
                showTranslation = true,
                showTransliteration = true
            )
        }
    }
}

@Preview(showBackground = true, name = "Translation Content - Translation Only")
@Composable
private fun AyahTranslationContentTranslationOnlyPreview() {
    NimazTheme {
        Surface {
            AyahTranslationContent(
                ayah = translationPreviewAyah,
                surahName = "Al-Fatihah",
                showTranslation = true,
                showTransliteration = false
            )
        }
    }
}

@Preview(showBackground = true, name = "Translation Content - With Sajda")
@Composable
private fun AyahTranslationContentSajdaPreview() {
    NimazTheme {
        Surface {
            AyahTranslationContent(
                ayah = translationPreviewAyahSajda,
                surahName = "Al-A'raf",
                showTranslation = true,
                showTransliteration = true
            )
        }
    }
}

private val translationPreviewAyah = Ayah(
    id = 2,
    surahNumber = 1,
    ayahNumber = 2,
    textArabic = "ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَٰلَمِينَ",
    textSimple = "الحمد لله رب العالمين",
    juzNumber = 1,
    hizbNumber = 1,
    rubNumber = 0,
    pageNumber = 1,
    sajdaType = null,
    sajdaNumber = null,
    translation = "All praise is due to Allah, Lord of the worlds.",
    transliteration = "Al-hamdu lillahi rabbi al-'alamin",
    isBookmarked = false
)

private val translationPreviewAyahSajda = Ayah(
    id = 1160,
    surahNumber = 7,
    ayahNumber = 206,
    textArabic = "إِنَّ ٱلَّذِينَ عِندَ رَبِّكَ لَا يَسْتَكْبِرُونَ عَنْ عِبَادَتِهِۦ وَيُسَبِّحُونَهُۥ وَلَهُۥ يَسْجُدُونَ",
    textSimple = "إن الذين عند ربك لا يستكبرون عن عبادته ويسبحونه وله يسجدون",
    juzNumber = 9,
    hizbNumber = 18,
    rubNumber = 4,
    pageNumber = 176,
    sajdaType = SajdaType.OBLIGATORY,
    sajdaNumber = 1,
    translation = "Indeed, those who are near your Lord are not prevented by arrogance from His worship, and they exalt Him, and to Him they prostrate.",
    transliteration = "Inna alladhina 'inda rabbika la yastakbiroona 'an 'ibadatihi wa yusabbihoonahu wa lahu yasjudoon",
    isBookmarked = false
)