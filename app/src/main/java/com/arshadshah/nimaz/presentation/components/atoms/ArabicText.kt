package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.presentation.theme.AmiriFontFamily
import com.arshadshah.nimaz.presentation.theme.ArabicTextStyles
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * Arabic text size presets
 */
enum class ArabicTextSize(val fontSize: TextUnit, val lineHeight: TextUnit) {
    SMALL(20.sp, 36.sp),
    MEDIUM(24.sp, 44.sp),
    LARGE(28.sp, 52.sp),
    EXTRA_LARGE(32.sp, 56.sp),
    QURAN(28.sp, 56.sp)
}

/**
 * Base Arabic text component with RTL support.
 */
@Composable
fun ArabicText(
    text: String,
    modifier: Modifier = Modifier,
    size: ArabicTextSize = ArabicTextSize.MEDIUM,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontWeight: FontWeight = FontWeight.Normal,
    textAlign: TextAlign = TextAlign.Center,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    style: TextStyle? = null
) {
    val textStyle = style ?: TextStyle(
        fontFamily = AmiriFontFamily,
        fontSize = size.fontSize,
        lineHeight = size.lineHeight,
        fontWeight = fontWeight,
        textAlign = textAlign,
        textDirection = TextDirection.Rtl
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Text(
            text = text,
            modifier = modifier,
            style = textStyle,
            color = color,
            maxLines = maxLines,
            overflow = overflow
        )
    }
}

/**
 * Quran verse text with proper styling.
 */
@Composable
fun QuranVerseText(
    modifier: Modifier = Modifier,
    arabicText: String,
    verseNumber: Int? = null,
    size: ArabicTextSize = ArabicTextSize.QURAN,
    customFontSize: Float? = null,
    color: Color = MaterialTheme.colorScheme.onSurface,
    showVerseNumber: Boolean = true
) {
    val displayText = if (showVerseNumber && verseNumber != null) {
        formatAyahWithEndMarker(arabicText, toArabicNumber(verseNumber).toInt())
    } else {
        arabicText
    }

    // Use customFontSize if provided, otherwise use enum
    val actualFontSize = customFontSize?.sp ?: size.fontSize
    val actualLineHeight = customFontSize?.let { (it * 2).sp } ?: size.lineHeight

    ArabicText(
        text = displayText,
        modifier = modifier.fillMaxWidth(),
        size = size,
        color = color,
        textAlign = TextAlign.Center,
        style = ArabicTextStyles.quranLarge.copy(
            fontSize = actualFontSize,
            lineHeight = actualLineHeight
        )
    )
}

/**
 * Process ayah text to append Arabic numeral with ornamental brackets at the end
 */
private fun formatAyahWithEndMarker(arabicText: String, ayahNumber: Int): String {
    return "$arabicText ${formatAyahEndMarker(ayahNumber)}"
}

/**
 * Format just the ayah end marker with ornamental brackets
 */
private fun formatAyahEndMarker(ayahNumber: Int): String {
    val unicodeAyaEndStart = "\uFD3F" // ﴿
    val unicodeAyaEndEnd = "\uFD3E"   // ﴾
    val arabicNumber = toArabicNumber(ayahNumber)
    return "$unicodeAyaEndStart$arabicNumber$unicodeAyaEndEnd"
}


/**
 * Hadith Arabic text with proper styling.
 */
@Composable
fun HadithArabicText(
    text: String,
    modifier: Modifier = Modifier,
    size: ArabicTextSize = ArabicTextSize.MEDIUM,
    customFontSize: Float? = null,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    val actualFontSize = customFontSize?.sp ?: size.fontSize
    val actualLineHeight = customFontSize?.let { (it * 2.4f).sp } ?: size.lineHeight

    ArabicText(
        text = text,
        modifier = modifier.fillMaxWidth(),
        size = size,
        color = color,
        textAlign = TextAlign.End,
        style = ArabicTextStyles.hadithArabic.copy(
            fontSize = actualFontSize,
            lineHeight = actualLineHeight
        )
    )
}

/**
 * Dua Arabic text with proper styling.
 */
@Composable
fun DuaArabicText(
    text: String,
    modifier: Modifier = Modifier,
    size: ArabicTextSize = ArabicTextSize.LARGE,
    customFontSize: Float? = null,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    val actualFontSize = customFontSize?.sp ?: size.fontSize
    val actualLineHeight = customFontSize?.let { (it * 2.4f).sp } ?: size.lineHeight

    ArabicText(
        text = text,
        modifier = modifier.fillMaxWidth(),
        size = size,
        color = color,
        textAlign = TextAlign.Center,
        style = ArabicTextStyles.duaArabic.copy(
            fontSize = actualFontSize,
            lineHeight = actualLineHeight
        )
    )
}

/**
 * Bismillah text component.
 */
@Composable
fun BismillahText(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    ArabicText(
        text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
        modifier = modifier.fillMaxWidth(),
        size = ArabicTextSize.LARGE,
        color = color,
        textAlign = TextAlign.Center,
        style = ArabicTextStyles.bismillah
    )
}

/**
 * Ayah display with Arabic, translation, and optional transliteration.
 */
@Composable
fun AyahDisplay(
    arabicText: String,
    translation: String,
    verseNumber: Int,
    modifier: Modifier = Modifier,
    transliteration: String? = null,
    showTransliteration: Boolean = false,
    arabicSize: ArabicTextSize = ArabicTextSize.QURAN,
    arabicColor: Color = MaterialTheme.colorScheme.onSurface,
    translationColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    transliterationColor: Color = MaterialTheme.colorScheme.tertiary
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        QuranVerseText(
            arabicText = arabicText,
            verseNumber = verseNumber,
            size = arabicSize,
            color = arabicColor
        )

        if (showTransliteration && transliteration != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = transliteration,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = transliterationColor,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = translation,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyLarge,
            color = translationColor,
            textAlign = TextAlign.Start
        )
    }
}

/**
 * Converts a number to Arabic-Indic numerals used in Quran.
 */
fun toArabicNumber(number: Int): String {
    val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    return number.toString().map { digit ->
        if (digit.isDigit()) arabicDigits[digit.digitToInt()] else digit
    }.joinToString("")
}

// ==================== PREVIEWS ====================

@Preview(showBackground = true, name = "Arabic Text Sizes")
@Composable
private fun ArabicTextSizesPreview() {
    NimazTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ArabicText(text = "بسم الله", size = ArabicTextSize.SMALL)
            ArabicText(text = "بسم الله", size = ArabicTextSize.MEDIUM)
            ArabicText(text = "بسم الله", size = ArabicTextSize.LARGE)
            ArabicText(text = "بسم الله", size = ArabicTextSize.EXTRA_LARGE)
        }
    }
}

@Preview(showBackground = true, name = "Bismillah Text")
@Composable
private fun BismillahTextPreview() {
    NimazTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            BismillahText()
        }
    }
}

@Preview(showBackground = true, name = "Quran Verse Text")
@Composable
private fun QuranVerseTextPreview() {
    NimazTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            QuranVerseText(
                arabicText = "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ",
                verseNumber = 2
            )
        }
    }
}

@Preview(showBackground = true, name = "Hadith Arabic Text")
@Composable
private fun HadithArabicTextPreview() {
    NimazTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            HadithArabicText(
                text = "إنما الأعمال بالنيات"
            )
        }
    }
}

@Preview(showBackground = true, name = "Dua Arabic Text")
@Composable
private fun DuaArabicTextPreview() {
    NimazTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            DuaArabicText(
                text = "اللَّهُمَّ إِنِّي أَسْأَلُكَ الْهُدَى وَالتُّقَى"
            )
        }
    }
}

@Preview(showBackground = true, name = "Ayah Display")
@Composable
private fun AyahDisplayPreview() {
    NimazTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            AyahDisplay(
                arabicText = "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ",
                translation = "All praise is due to Allah, Lord of the worlds.",
                verseNumber = 2,
                transliteration = "Alhamdu lillahi rabbi al-'alamin",
                showTransliteration = true
            )
        }
    }
}

