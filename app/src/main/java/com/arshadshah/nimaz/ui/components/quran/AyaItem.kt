package com.arshadshah.nimaz.ui.components.quran

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.data.local.models.LocalAya
import com.arshadshah.nimaz.ui.components.quran.aya.components.AyatContent
import com.arshadshah.nimaz.ui.components.quran.aya.components.AyatFeatures
import com.arshadshah.nimaz.ui.components.quran.aya.components.SpecialAyat
import com.arshadshah.nimaz.ui.components.quran.aya.components.TafseerSection
import com.arshadshah.nimaz.utils.DisplaySettings
import com.arshadshah.nimaz.viewModel.AudioState
import com.arshadshah.nimaz.viewModel.AyatViewModel

/**
 * Individual Aya item card displayed in the Ayat list.
 *
 * Design System Alignment:
 * - ElevatedCard with extraLarge shape
 * - 4dp elevation
 * - 8dp inner padding
 * - 12dp section spacing
 * - Contains: AyatFeatures (header), AyatContent (content), TafseerSection (action)
 */
@Composable
fun AyaItem(
    aya: LocalAya,
    displaySettings: DisplaySettings,
    audioState: AudioState,
    onTafseerClick: (Int, Int) -> Unit,
    onEvent: (AyatViewModel.AyatEvent) -> Unit,
    loading: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (aya.ayaNumberInSurah != 0) {
        ElevatedCard(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            shape = MaterialTheme.shapes.extraLarge,
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Section - Features/Actions
                AyatFeatures(
                    aya = aya,
                    audioState = audioState,
                    onEvent = onEvent,
                    loading = loading
                )

                // Content Section - Arabic + Translation
                AyatContent(
                    aya = aya,
                    displaySettings = displaySettings,
                    loading = loading
                )

                // Action Section - Tafseer
                TafseerSection(
                    aya = aya,
                    onOpenTafsir = onTafseerClick
                )
            }
        }
    } else {
        // Special Ayat (Bismillah)
        SpecialAyat(
            aya = aya,
            displaySettings = displaySettings,
            loading = loading,
            modifier = modifier
        )
    }
}

// =============================================================================
// PREVIEWS
// =============================================================================

private val previewAya = LocalAya(
    ayaNumberInQuran = 1,
    suraNumber = 1,
    ayaNumberInSurah = 1,
    ayaArabic = "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ",
    translationEnglish = "All praise is due to Allah, Lord of the worlds.",
    translationUrdu = "سب تعریف اللہ کے لیے ہے جو تمام جہانوں کا پالنے والا ہے۔",
    audioFileLocation = "",
    sajda = false,
    sajdaType = "",
    bookmark = true,
    favorite = false,
    note = "Test note",
    ruku = 1,
    juzNumber = 1
)

private val previewBismillah = LocalAya(
    ayaNumberInQuran = 0,
    suraNumber = 1,
    ayaNumberInSurah = 0,
    ayaArabic = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
    translationEnglish = "In the name of Allah, the Entirely Merciful, the Especially Merciful.",
    translationUrdu = "اللہ کے نام سے جو بہت مہربان نہایت رحم والا ہے۔",
    audioFileLocation = "",
    sajda = false,
    sajdaType = "",
    bookmark = false,
    favorite = false,
    note = "",
    ruku = 1,
    juzNumber = 1
)

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun AyaItemPreview() {
    MaterialTheme {
        AyaItem(
            aya = previewAya,
            displaySettings = DisplaySettings(translation = "English"),
            audioState = AudioState(),
            onTafseerClick = { _, _ -> },
            onEvent = {},
            loading = false,
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun AyaItemPreview_Bismillah() {
    MaterialTheme {
        AyaItem(
            aya = previewBismillah,
            displaySettings = DisplaySettings(translation = "English"),
            audioState = AudioState(),
            onTafseerClick = { _, _ -> },
            onEvent = {},
            loading = false,
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AyaItemPreview_Dark() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(color = MaterialTheme.colorScheme.background) {
            AyaItem(
                aya = previewAya,
                displaySettings = DisplaySettings(translation = "English"),
                audioState = AudioState(),
                onTafseerClick = { _, _ -> },
                onEvent = {},
                loading = false,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}