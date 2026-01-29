package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.presentation.components.atoms.toArabicNumber
import com.arshadshah.nimaz.presentation.theme.AmiriFontFamily
import com.arshadshah.nimaz.presentation.theme.NimazTheme

// Mushaf line color - subtle teal to match the frame
private val MushafLineColor = Color(0xFF0F766E).copy(alpha = 0.5f)

// Bismillah text to strip from first ayah (uses alef wasla ٱ as in database)
private const val BISMILLAH_TEXT = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"

/**
 * Strip bismillah from first ayah's Arabic text for all surahs EXCEPT:
 * - Surah 1 (Al-Fatiha) - bismillah IS ayah 1
 * - Surah 9 (At-Tawbah) - has no bismillah
 */
private fun Ayah.getDisplayArabicText(): String {
    return if (ayahNumber == 1 && surahNumber != 1 && surahNumber != 9) {
        textArabic
            .removePrefix("$BISMILLAH_TEXT ")
            .removePrefix(BISMILLAH_TEXT)
            .trim()
    } else {
        textArabic
    }
}

/**
 * Format ayah end marker with ornamental brackets and Arabic-Indic numerals.
 * Uses direct character mapping for consistent Arabic numeral display across all devices.
 */
private fun formatAyahEndMarker(ayahNumber: Int): String {
    val unicodeAyaEndStart = "\uFD3F" // ﴿
    val unicodeAyaEndEnd = "\uFD3E"   // ﴾
    val arabicNumber = toArabicNumber(ayahNumber)
    return "$unicodeAyaEndStart$arabicNumber$unicodeAyaEndEnd"
}

/**
 * Core component that renders continuous Arabic text with clickable ayah spans.
 * Uses AnnotatedString with ClickableText for click detection.
 * Includes ruled lines like a traditional printed Mushaf.
 *
 * @param ayahs List of ayahs to render as continuous text
 * @param onAyahClick Callback when an ayah is clicked
 * @param highlightedAyahId ID of currently highlighted ayah (e.g., during audio playback)
 * @param arabicFontSize Font size for Arabic text in sp
 * @param showRuledLines Whether to show ruled lines behind the text (default true)
 * @param lineColor Color of the ruled lines
 * @param modifier Modifier for the composable
 */
@Composable
fun MushafContinuousText(
    ayahs: List<Ayah>,
    onAyahClick: (Ayah) -> Unit,
    modifier: Modifier = Modifier,
    highlightedAyahId: Int? = null,
    arabicFontSize: Float = 28f,
    showRuledLines: Boolean = true,
    lineColor: Color = MushafLineColor,
    highlightColor: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 1f),
    textColor: Color = MaterialTheme.colorScheme.onBackground
) {
    val annotatedText = remember(ayahs, highlightedAyahId, highlightColor, textColor) {
        buildMushafAnnotatedString(
            ayahs = ayahs,
            highlightedAyahId = highlightedAyahId,
            highlightColor = highlightColor,
            textColor = textColor
        )
    }

    // Store the full TextLayoutResult to get actual line positions
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = modifier.fillMaxWidth()
        ) {
            ClickableText(
                text = annotatedText,
                style = TextStyle(
                    fontFamily = AmiriFontFamily,
                    fontSize = arabicFontSize.sp,
                    lineHeight = (arabicFontSize * 2.5).sp,
                    textDirection = TextDirection.Rtl,
                    textAlign = TextAlign.Justify,
                    color = textColor
                ),
                onClick = { offset ->
                    annotatedText.getStringAnnotations(
                        tag = AYAH_TAG,
                        start = offset,
                        end = offset
                    ).firstOrNull()?.let { annotation ->
                        val ayahId = annotation.item.toIntOrNull()
                        ayahs.find { it.id == ayahId }?.let(onAyahClick)
                    }
                },
                onTextLayout = { result ->
                    textLayoutResult = result
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (showRuledLines) {
                            Modifier.drawBehind {
                                textLayoutResult?.let { layout ->
                                    for (i in 0 until layout.lineCount) {
                                        // Use actual line bottom position from TextLayoutResult
                                        val y = layout.getLineBottom(i)
                                        drawLine(
                                            color = lineColor,
                                            start = Offset(0f, y),
                                            end = Offset(size.width, y),
                                            strokeWidth = 1.5f
                                        )
                                    }
                                }
                            }
                        } else {
                            Modifier
                        }
                    )
            )
        }
    }
}

private const val AYAH_TAG = "AYAH"

/**
 * Builds the annotated string for Mushaf display with:
 * - Continuous text from all ayahs
 * - Inline ayah end markers
 * - Click annotations for each ayah
 * - Highlighting for the currently playing ayah
 */
private fun buildMushafAnnotatedString(
    ayahs: List<Ayah>,
    highlightedAyahId: Int?,
    highlightColor: Color,
    textColor: Color
): AnnotatedString {
    return buildAnnotatedString {
        ayahs.forEachIndexed { index, ayah ->
            val start = length

            // Append the ayah text
            append(ayah.getDisplayArabicText())

            // Append space and end marker
            append(" ")
            append(formatAyahEndMarker(ayah.ayahNumber))

            val end = length

            // Add click annotation for the entire ayah span
            addStringAnnotation(
                tag = AYAH_TAG,
                annotation = ayah.id.toString(),
                start = start,
                end = end
            )

            // Apply highlight style if this is the currently playing ayah
            if (ayah.id == highlightedAyahId) {
                addStyle(
                    style = SpanStyle(background = highlightColor),
                    start = start,
                    end = end
                )
            }

            // Add space between ayahs (except for the last one)
            if (index < ayahs.size - 1) {
                append(" ")
            }
        }
    }
}

// ==================== PREVIEWS ====================

@Preview(showBackground = true, name = "Mushaf Continuous Text - Basic")
@Composable
private fun MushafContinuousTextPreview() {
    NimazTheme {
        MushafContinuousText(
            ayahs = sampleFatihahAyahs,
            onAyahClick = {},
            arabicFontSize = 28f
        )
    }
}

@Preview(showBackground = true, name = "Mushaf Continuous Text - With Highlight")
@Composable
private fun MushafContinuousTextWithHighlightPreview() {
    NimazTheme {
        MushafContinuousText(
            ayahs = sampleFatihahAyahs,
            onAyahClick = {},
            highlightedAyahId = 3, // Highlight ayah 3
            arabicFontSize = 28f
        )
    }
}

@Preview(showBackground = true, name = "Mushaf Continuous Text - Large Font")
@Composable
private fun MushafContinuousTextLargeFontPreview() {
    NimazTheme {
        MushafContinuousText(
            ayahs = sampleFatihahAyahs,
            onAyahClick = {},
            arabicFontSize = 32f
        )
    }
}

@Preview(showBackground = true, name = "Mushaf Continuous Text - Without Ruled Lines")
@Composable
private fun MushafContinuousTextNoLinesPreview() {
    NimazTheme {
        MushafContinuousText(
            ayahs = sampleFatihahAyahs,
            onAyahClick = {},
            arabicFontSize = 28f,
            showRuledLines = false
        )
    }
}

// Sample data for previews
internal val sampleFatihahAyahs = listOf(
    Ayah(
        id = 1,
        surahNumber = 1,
        ayahNumber = 1,
        textArabic = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
        textSimple = "بسم الله الرحمن الرحيم",
        juzNumber = 1,
        hizbNumber = 1,
        rubNumber = 0,
        pageNumber = 1,
        sajdaType = null,
        sajdaNumber = null,
        translation = "In the name of Allah, the Entirely Merciful, the Especially Merciful.",
        isBookmarked = false
    ),
    Ayah(
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
        isBookmarked = false
    ),
    Ayah(
        id = 3,
        surahNumber = 1,
        ayahNumber = 3,
        textArabic = "ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
        textSimple = "الرحمن الرحيم",
        juzNumber = 1,
        hizbNumber = 1,
        rubNumber = 0,
        pageNumber = 1,
        sajdaType = null,
        sajdaNumber = null,
        translation = "The Entirely Merciful, the Especially Merciful.",
        isBookmarked = false
    ),
    Ayah(
        id = 4,
        surahNumber = 1,
        ayahNumber = 4,
        textArabic = "مَٰلِكِ يَوْمِ ٱلدِّينِ",
        textSimple = "مالك يوم الدين",
        juzNumber = 1,
        hizbNumber = 1,
        rubNumber = 0,
        pageNumber = 1,
        sajdaType = null,
        sajdaNumber = null,
        translation = "Sovereign of the Day of Recompense.",
        isBookmarked = false
    ),
    Ayah(
        id = 5,
        surahNumber = 1,
        ayahNumber = 5,
        textArabic = "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ",
        textSimple = "إياك نعبد وإياك نستعين",
        juzNumber = 1,
        hizbNumber = 1,
        rubNumber = 0,
        pageNumber = 1,
        sajdaType = null,
        sajdaNumber = null,
        translation = "It is You we worship and You we ask for help.",
        isBookmarked = false
    ),
    Ayah(
        id = 6,
        surahNumber = 1,
        ayahNumber = 6,
        textArabic = "ٱهْدِنَا ٱلصِّرَٰطَ ٱلْمُسْتَقِيمَ",
        textSimple = "اهدنا الصراط المستقيم",
        juzNumber = 1,
        hizbNumber = 1,
        rubNumber = 0,
        pageNumber = 1,
        sajdaType = null,
        sajdaNumber = null,
        translation = "Guide us to the straight path.",
        isBookmarked = false
    ),
    Ayah(
        id = 7,
        surahNumber = 1,
        ayahNumber = 7,
        textArabic = "صِرَٰطَ ٱلَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ ٱلْمَغْضُوبِ عَلَيْهِمْ وَلَا ٱلضَّآلِّينَ",
        textSimple = "صراط الذين أنعمت عليهم غير المغضوب عليهم ولا الضالين",
        juzNumber = 1,
        hizbNumber = 1,
        rubNumber = 0,
        pageNumber = 1,
        sajdaType = null,
        sajdaNumber = null,
        translation = "The path of those upon whom You have bestowed favor, not of those who have earned anger or of those who are astray.",
        isBookmarked = false
    )
)
