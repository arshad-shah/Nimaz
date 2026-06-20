package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.core.util.TajweedParser
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.presentation.components.atoms.toArabicNumber
import androidx.compose.ui.text.font.FontFamily
import com.arshadshah.nimaz.presentation.theme.AmiriFontFamily
import com.arshadshah.nimaz.presentation.theme.NimazTheme

private val MushafLineColor = Color(0xFF0F766E).copy(alpha = 0.5f)

private const val BISMILLAH_TEXT = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"

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

private fun formatAyahEndMarker(ayahNumber: Int): String {
    val unicodeAyaEndStart = "\uFD3F"
    val unicodeAyaEndEnd = "\uFD3E"
    val arabicNumber = toArabicNumber(ayahNumber)
    return "$unicodeAyaEndStart$arabicNumber$unicodeAyaEndEnd"
}

/**
 * Core component that renders continuous Arabic text with clickable ayah spans.
 * Reports the tap Y position for tooltip anchoring.
 *
 * Two highlight modes:
 * - [highlightedAyahId]: Audio playback highlight (primaryContainer)
 * - [selectedAyahId]: Tooltip selection highlight (tertiaryContainer)
 *
 * @param ayahs List of ayahs to render
 * @param onAyahClick Callback with tapped ayah and tap Y coordinate within this component
 * @param highlightedAyahId Audio-highlighted ayah ID
 * @param selectedAyahId Tooltip-selected ayah ID
 * @param arabicFontSize Font size in sp
 * @param showRuledLines Show ruled lines behind text
 * @param showTajweed Show tajweed colors
 */
@Composable
fun MushafContinuousText(
    ayahs: List<Ayah>,
    onAyahClick: (Ayah, Float) -> Unit,
    modifier: Modifier = Modifier,
    highlightedAyahId: Int? = null,
    selectedAyahId: Int? = null,
    arabicFontSize: Float = 28f,
    arabicFontFamily: FontFamily = AmiriFontFamily,
    showRuledLines: Boolean = true,
    lineColor: Color = MushafLineColor,
    highlightColor: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 1f),
    selectedColor: Color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
    textColor: Color = MaterialTheme.colorScheme.onBackground,
    showTajweed: Boolean = false
) {
    val isDarkTheme = isSystemInDarkTheme()

    val annotatedText = remember(
        ayahs, highlightedAyahId, selectedAyahId,
        highlightColor, selectedColor, textColor, showTajweed, isDarkTheme
    ) {
        buildMushafAnnotatedString(
            ayahs = ayahs,
            highlightedAyahId = highlightedAyahId,
            selectedAyahId = selectedAyahId,
            highlightColor = highlightColor,
            selectedColor = selectedColor,
            textColor = textColor,
            showTajweed = showTajweed,
            isDarkTheme = isDarkTheme
        )
    }

    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = modifier.fillMaxWidth()) {
            BasicText(
                text = annotatedText,
                style = TextStyle(
                    fontFamily = arabicFontFamily,
                    fontSize = arabicFontSize.sp,
                    lineHeight = (arabicFontSize * 2.5).sp,
                    textDirection = TextDirection.Rtl,
                    textAlign = TextAlign.Justify,
                    color = textColor
                ),
                onTextLayout = { result -> textLayoutResult = result },
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(annotatedText, ayahs) {
                        detectTapGestures { position ->
                            val layout = textLayoutResult ?: return@detectTapGestures
                            val offset = layout.getOffsetForPosition(position)
                            annotatedText.getStringAnnotations(
                                tag = AYAH_TAG, start = offset, end = offset
                            ).firstOrNull()?.let { annotation ->
                                val ayahId = annotation.item.toIntOrNull()
                                ayahs.find { it.id == ayahId }?.let { ayah ->
                                    val line = layout.getLineForOffset(offset)
                                    val tapY =
                                        (layout.getLineTop(line) + layout.getLineBottom(line)) / 2f
                                    onAyahClick(ayah, tapY)
                                }
                            }
                        }
                    }
                    .then(
                        if (showRuledLines) {
                            Modifier.drawBehind {
                                textLayoutResult?.let { layout ->
                                    for (i in 0 until layout.lineCount) {
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
                        } else Modifier
                    )
            )
        }
    }
}

/**
 * Backward-compatible overload without tap position.
 */
@Composable
fun MushafContinuousText(
    ayahs: List<Ayah>,
    onAyahClick: (Ayah) -> Unit,
    modifier: Modifier = Modifier,
    highlightedAyahId: Int? = null,
    selectedAyahId: Int? = null,
    arabicFontSize: Float = 28f,
    arabicFontFamily: FontFamily = AmiriFontFamily,
    showRuledLines: Boolean = true,
    lineColor: Color = MushafLineColor,
    highlightColor: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 1f),
    selectedColor: Color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
    textColor: Color = MaterialTheme.colorScheme.onBackground,
    showTajweed: Boolean = false
) {
    MushafContinuousText(
        ayahs = ayahs,
        onAyahClick = { ayah, _ -> onAyahClick(ayah) },
        modifier = modifier,
        highlightedAyahId = highlightedAyahId,
        selectedAyahId = selectedAyahId,
        arabicFontSize = arabicFontSize,
        arabicFontFamily = arabicFontFamily,
        showRuledLines = showRuledLines,
        lineColor = lineColor,
        highlightColor = highlightColor,
        selectedColor = selectedColor,
        textColor = textColor,
        showTajweed = showTajweed
    )
}

private const val AYAH_TAG = "AYAH"

private fun buildMushafAnnotatedString(
    ayahs: List<Ayah>,
    highlightedAyahId: Int?,
    selectedAyahId: Int?,
    highlightColor: Color,
    selectedColor: Color,
    textColor: Color,
    showTajweed: Boolean = false,
    isDarkTheme: Boolean = false
): AnnotatedString {
    return buildAnnotatedString {
        ayahs.forEachIndexed { index, ayah ->
            val start = length

            if (showTajweed && ayah.textTajweed != null) {
                val tajweedAnnotated = TajweedParser.parse(
                    tajweedText = ayah.textTajweed,
                    isDarkTheme = isDarkTheme,
                    defaultColor = textColor
                )
                val displayTajweed =
                    if (ayah.ayahNumber == 1 && ayah.surahNumber != 1 && ayah.surahNumber != 9) {
                        tajweedAnnotated
                    } else {
                        tajweedAnnotated
                    }
                append(displayTajweed)
            } else {
                append(ayah.getDisplayArabicText())
            }

            append(" ")
            append(formatAyahEndMarker(ayah.ayahNumber))

            val end = length

            addStringAnnotation(
                tag = AYAH_TAG,
                annotation = ayah.id.toString(),
                start = start,
                end = end
            )

            // Audio highlight takes priority
            if (ayah.id == highlightedAyahId) {
                addStyle(
                    style = SpanStyle(background = highlightColor),
                    start = start,
                    end = end
                )
            } else if (ayah.id == selectedAyahId) {
                // Selection highlight (tooltip active)
                addStyle(
                    style = SpanStyle(background = selectedColor),
                    start = start,
                    end = end
                )
            }

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
            onAyahClick = { _, _ -> },
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
            onAyahClick = { _, _ -> },
            highlightedAyahId = 3,
            arabicFontSize = 28f
        )
    }
}

@Preview(showBackground = true, name = "Mushaf Continuous Text - With Selection")
@Composable
private fun MushafContinuousTextWithSelectionPreview() {
    NimazTheme {
        MushafContinuousText(
            ayahs = sampleFatihahAyahs,
            onAyahClick = { _, _ -> },
            selectedAyahId = 5,
            arabicFontSize = 28f
        )
    }
}

internal val sampleFatihahAyahs = listOf(
    Ayah(
        id = 1, surahNumber = 1, ayahNumber = 1,
        textArabic = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
        textSimple = "بسم الله الرحمن الرحيم",
        juzNumber = 1, hizbNumber = 1, rubNumber = 0, pageNumber = 1,
        sajdaType = null, sajdaNumber = null,
        translation = "In the name of Allah, the Entirely Merciful, the Especially Merciful.",
        isBookmarked = false
    ),
    Ayah(
        id = 2, surahNumber = 1, ayahNumber = 2,
        textArabic = "ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَٰلَمِينَ",
        textSimple = "الحمد لله رب العالمين",
        juzNumber = 1, hizbNumber = 1, rubNumber = 0, pageNumber = 1,
        sajdaType = null, sajdaNumber = null,
        translation = "All praise is due to Allah, Lord of the worlds.",
        isBookmarked = false
    ),
    Ayah(
        id = 3, surahNumber = 1, ayahNumber = 3,
        textArabic = "ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
        textSimple = "الرحمن الرحيم",
        juzNumber = 1, hizbNumber = 1, rubNumber = 0, pageNumber = 1,
        sajdaType = null, sajdaNumber = null,
        translation = "The Entirely Merciful, the Especially Merciful.",
        isBookmarked = false
    ),
    Ayah(
        id = 4, surahNumber = 1, ayahNumber = 4,
        textArabic = "مَٰلِكِ يَوْمِ ٱلدِّينِ",
        textSimple = "مالك يوم الدين",
        juzNumber = 1, hizbNumber = 1, rubNumber = 0, pageNumber = 1,
        sajdaType = null, sajdaNumber = null,
        translation = "Sovereign of the Day of Recompense.",
        isBookmarked = false
    ),
    Ayah(
        id = 5, surahNumber = 1, ayahNumber = 5,
        textArabic = "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ",
        textSimple = "إياك نعبد وإياك نستعين",
        juzNumber = 1, hizbNumber = 1, rubNumber = 0, pageNumber = 1,
        sajdaType = null, sajdaNumber = null,
        translation = "It is You we worship and You we ask for help.",
        isBookmarked = false
    ),
    Ayah(
        id = 6, surahNumber = 1, ayahNumber = 6,
        textArabic = "ٱهْدِنَا ٱلصِّرَٰطَ ٱلْمُسْتَقِيمَ",
        textSimple = "اهدنا الصراط المستقيم",
        juzNumber = 1, hizbNumber = 1, rubNumber = 0, pageNumber = 1,
        sajdaType = null, sajdaNumber = null,
        translation = "Guide us to the straight path.",
        isBookmarked = false
    ),
    Ayah(
        id = 7, surahNumber = 1, ayahNumber = 7,
        textArabic = "صِرَٰطَ ٱلَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ ٱلْمَغْضُوبِ عَلَيْهِمْ وَلَا ٱلضَّآلِّينَ",
        textSimple = "صراط الذين أنعمت عليهم غير المغضوب عليهم ولا الضالين",
        juzNumber = 1, hizbNumber = 1, rubNumber = 0, pageNumber = 1,
        sajdaType = null, sajdaNumber = null,
        translation = "The path of those upon whom You have bestowed favor, not of those who have earned anger or of those who are astray.",
        isBookmarked = false
    )
)