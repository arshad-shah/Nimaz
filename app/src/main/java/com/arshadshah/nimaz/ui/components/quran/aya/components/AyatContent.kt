package com.arshadshah.nimaz.ui.components.quran.aya.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.data.local.models.LocalAya
import com.arshadshah.nimaz.utils.DisplaySettings

/**
 * Aya content section containing Arabic text and translation.
 *
 * Design System Alignment:
 * - Surface with surfaceVariant @ 0.5 alpha
 * - 16dp corner radius
 * - 12dp padding
 * - 12dp spacing between elements
 */
@Composable
fun AyatContent(
    aya: LocalAya,
    displaySettings: DisplaySettings,
    loading: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Arabic Text
            ArabicText(
                aya = aya,
                displaySettings = displaySettings,
                loading = loading
            )

            // Translation Text
            TranslationText(
                aya = aya,
                displaySettings = displaySettings,
                loading = loading
            )
        }
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
    translationUrdu = "سب تعریف اللہ کے لیے ہے جو تمام جہانوں کا پالنے والا ہے",
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
private fun AyatContentPreview_English() {
    MaterialTheme {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(16.dp)
        ) {
            AyatContent(
                aya = previewAya,
                displaySettings = DisplaySettings(translation = "English"),
                loading = false
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun AyatContentPreview_Urdu() {
    MaterialTheme {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(16.dp)
        ) {
            AyatContent(
                aya = previewAya,
                displaySettings = DisplaySettings(translation = "Urdu"),
                loading = false
            )
        }
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AyatContentPreview_Dark() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(16.dp)
        ) {
            AyatContent(
                aya = previewAya,
                displaySettings = DisplaySettings(translation = "English"),
                loading = false
            )
        }
    }
}