package com.arshadshah.nimaz.ui.components.quran.aya.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.data.local.models.LocalAya
import com.arshadshah.nimaz.ui.components.common.placeholder.material.PlaceholderHighlight
import com.arshadshah.nimaz.ui.components.common.placeholder.material.placeholder
import com.arshadshah.nimaz.ui.components.common.placeholder.material.shimmer
import com.arshadshah.nimaz.ui.theme.englishQuranTranslation
import com.arshadshah.nimaz.ui.theme.urduFont
import com.arshadshah.nimaz.utils.DisplaySettings
import com.arshadshah.nimaz.utils.StringUtils.cleanTextFromBackslash

@Composable
fun TranslationText(
    aya: LocalAya,
    displaySettings: DisplaySettings,
    loading: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {

            when (displaySettings.translation) {
                "Urdu" -> UrduTranslation(
                    text = aya.translationUrdu,
                    fontSize = displaySettings.translationFontSize.toInt(),
                    loading = loading
                )

                else -> EnglishTranslation(
                    text = aya.translationEnglish,
                    fontSize = displaySettings.translationFontSize.toInt(),
                    loading = loading
                )
            }
        }
    }
}

@Composable
fun UrduTranslation(
    modifier: Modifier = Modifier,
    text: String,
    fontSize: Int,
    loading: Boolean = false
) {

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Text(
                text = "${text.cleanTextFromBackslash()} ۔",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = fontSize.sp,
                    fontFamily = urduFont,
                    textAlign = TextAlign.Justify,
                ),
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = modifier
                    .fillMaxWidth()
                    .placeholder(
                        visible = loading,
                        highlight = PlaceholderHighlight.shimmer(),
                    )
            )
        }
}

@Composable
fun EnglishTranslation(
    modifier: Modifier = Modifier,
    text: String,
    fontSize: Int,
    loading: Boolean = false
) {

        Text(
            text = text.cleanTextFromBackslash(),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = fontSize.sp,
                fontFamily = englishQuranTranslation,
                textAlign = TextAlign.Justify,
            ),
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = modifier
                .fillMaxWidth()
                .placeholder(
                    visible = loading,
                    highlight = PlaceholderHighlight.shimmer(),
                )
        )
}


@Preview
@Composable
fun TranslationUrduTextPreview() {
    val sampleAya = LocalAya(
        ayaArabic = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
        translationEnglish = "In the name of Allah, the Entirely Merciful, the Especially Merciful.",
        translationUrdu = "اللہ کے نام سے جو بہت مہربان نہایت رحم والا ہے۔",
        suraNumber = 1,
        ayaNumberInSurah = 0,
        bookmark = false,
        favorite = false,
        note = "",
        audioFileLocation = "",
        sajda = false,
        sajdaType = "",
        ruku = 1,
        juzNumber = 1,
        ayaNumberInQuran = 0
    )
    val displaySettings = DisplaySettings(
        translation = "Urdu",
    )

    TranslationText(aya = sampleAya, displaySettings = displaySettings, loading = false)
}

@Preview
@Composable
fun TranslationEnglishTextPreview() {
    val sampleAya = LocalAya(
        ayaArabic = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
        translationEnglish = "In the name of Allah, the Entirely Merciful, the Especially Merciful.",
        translationUrdu = "اللہ کے نام سے جو بہت مہربان نہایت رحم والا ہے۔",
        suraNumber = 1,
        ayaNumberInSurah = 0,
        bookmark = false,
        favorite = false,
        note = "",
        audioFileLocation = "",
        sajda = false,
        sajdaType = "",
        ruku = 1,
        juzNumber = 1,
        ayaNumberInQuran = 0
    )
    val displaySettings = DisplaySettings(
        translation = "English",
    )

    TranslationText(aya = sampleAya, displaySettings = displaySettings, loading = false)
}
