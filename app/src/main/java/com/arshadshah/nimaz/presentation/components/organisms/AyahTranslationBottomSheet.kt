package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.SajdaType
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.components.molecules.NimazBottomSheet
import com.arshadshah.nimaz.presentation.components.molecules.NimazSheetHeader
import com.arshadshah.nimaz.presentation.components.molecules.NimazSheetPreviewCard
import com.arshadshah.nimaz.presentation.components.molecules.NimazSheetSectionLabel
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * Bottom sheet showing translation and transliteration for an ayah.
 *
 * Opened from the "Translation" button in [AyahActionPopup]. Display-only — no
 * action buttons. Built on the shared [NimazBottomSheet] kit.
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
        modifier = modifier,
        contentPadding = PaddingValues(0.dp)
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
    Column(modifier = modifier.fillMaxWidth()) {
        NimazSheetHeader(
            title = surahName ?: stringResource(R.string.surah_number_format, ayah.surahNumber),
            subtitle = stringResource(R.string.ayah_number_format, ayah.ayahNumber),
            badge = stringResource(R.string.juz_page_format, ayah.juzNumber, ayah.pageNumber)
        )

        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
            // Arabic text preview
            NimazSheetPreviewCard {
                ArabicText(
                    text = ayah.textArabic,
                    size = ArabicTextSize.MEDIUM,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Sajda indicator
            if (ayah.sajdaType != null) {
                Spacer(modifier = Modifier.height(12.dp))
                SajdaIndicator(sajdaType = ayah.sajdaType, withGlyph = true)
            }

            // Translation
            if (showTranslation && !ayah.translation.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                NimazSheetSectionLabel(text = stringResource(R.string.translation))
                NimazSheetPreviewCard {
                    Text(
                        text = ayah.translation,
                        // The catalogue ships right-to-left translations (Urdu), so the
                        // paragraph direction has to come from the text itself rather than
                        // from the app's locale — otherwise Urdu renders left-aligned with
                        // its punctuation on the wrong side. TextAlign.Start then follows
                        // the resolved direction.
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 22.sp,
                            textDirection = TextDirection.Content,
                            textAlign = TextAlign.Start
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Transliteration
            if (showTransliteration && !ayah.transliteration.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                NimazSheetSectionLabel(
                    text = stringResource(R.string.transliteration),
                    color = MaterialTheme.colorScheme.tertiary
                )
                NimazSheetPreviewCard(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
                ) {
                    Text(
                        text = ayah.transliteration,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
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
