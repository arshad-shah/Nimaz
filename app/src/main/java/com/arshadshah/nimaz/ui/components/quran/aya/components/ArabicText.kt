package com.arshadshah.nimaz.ui.components.quran.aya.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.data.local.models.LocalAya
import com.arshadshah.nimaz.ui.components.common.placeholder.material.PlaceholderHighlight
import com.arshadshah.nimaz.ui.components.common.placeholder.material.placeholder
import com.arshadshah.nimaz.ui.components.common.placeholder.material.shimmer
import com.arshadshah.nimaz.ui.theme.almajeed
import com.arshadshah.nimaz.ui.theme.amiri
import com.arshadshah.nimaz.ui.theme.hidayat
import com.arshadshah.nimaz.ui.theme.quranFont
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont
import com.arshadshah.nimaz.utils.DisplaySettings
import com.arshadshah.nimaz.utils.QuranUtils.getArabicFont
import com.arshadshah.nimaz.utils.StringUtils.cleanTextFromBackslash

@Composable
fun ArabicText(
    aya: LocalAya,
    displaySettings: DisplaySettings,
    loading: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        SelectionContainer {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = aya.ayaArabic.cleanTextFromBackslash(),
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = displaySettings.arabicFontSize.sp,
                    fontFamily = getArabicFont(displaySettings.arabicFont),
                    textAlign = TextAlign.Justify,
                    lineHeight = (1.5 * displaySettings.arabicFontSize).sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .placeholder(
                            visible = loading,
                            highlight = PlaceholderHighlight.shimmer()
                        )
                )
            }
        }
    }
}

@Preview
@Composable
fun ArabicTextPreview() {
    val sampleAya = LocalAya(
        ayaArabic = "ذَٰلِكَ ٱلْكِتَـٰبُ لَا رَيْبَ ۛ فِيهِ ۛ هُدًى لِّلْمُتَّقِينَ ﴿٢﴾",
        translationEnglish = "",
        translationUrdu = "",
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
        arabicFont = "Default"
    )

    ArabicText(aya = sampleAya, displaySettings = displaySettings, loading = false)
}