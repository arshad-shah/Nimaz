package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.domain.model.MushafLine
import com.arshadshah.nimaz.domain.model.MushafLineType
import com.arshadshah.nimaz.domain.model.MushafPageLayout
import com.arshadshah.nimaz.domain.model.MushafWord
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.foundation.text.BISMILLAH_TEXT
import com.arshadshah.nimaz.presentation.theme.IndoPakFontFamily
import com.arshadshah.nimaz.presentation.theme.NimazTheme
import com.arshadshah.nimaz.presentation.theme.QuranSurfaceColors
import com.arshadshah.nimaz.presentation.theme.ThemeMode

/**
 * The line-accurate renderer for the 16-line IndoPak Mushaf (sub-task 5/7 of #263) — the
 * heart of the feature.
 *
 * Unlike [MushafContinuousText], which concatenates a page's ayahs into one justified
 * [BasicText] and draws ruled lines behind **whatever lines the text engine happens to wrap
 * to**, this component draws **exactly the lines** carried by a [MushafPageLayout]: one row
 * per printed line, in `line_number` order, so the page matches a printed 16-line Mushaf
 * line-for-line regardless of screen width — the property that makes the view usable for hifz.
 *
 * Per-line rules (classic printed-Mushaf look):
 * - [MushafLineType.AYAH] → words rendered right-to-left and **justified to full width**
 *   ([Arrangement.SpaceBetween]), except the last ayah line of the page and the last line of a
 *   surah (the line before a [MushafLineType.SURAH_HEADER]), which sit at natural width.
 * - [MushafLineType.SURAH_HEADER] → a centred [RuledSurahHeading] (bismillah suppressed;
 *   the basmalah is its own line in the layout).
 * - [MushafLineType.BASMALAH] → the basmalah centred on its own line.
 *
 * ## Sizing
 * Every line on a page is drawn at **one** size, and the reader's Arabic-font-size preference
 * genuinely moves it.
 *
 * Each line used to auto-fit its own font *down* from [arabicFontSize] until it fit the width.
 * On a real page that meant two things: lines on the same page rendered at different sizes
 * (a printed Mushaf has one), and — because the densest line never fits at 18sp, let alone at
 * 42sp — every value of the preference collapsed onto the same width-determined size, so the
 * slider did nothing at all on the IndoPak editions while working normally on Madani.
 *
 * Instead: [pageFitFontSize] measures the page's densest line once and derives the size at
 * which it exactly fills the width. That is the page's size at the *default* preference
 * ([REFERENCE_FONT_SIZE]), so the default rendering is unchanged; the preference then scales
 * it proportionally. Below the default the page simply gets smaller. Above it the lines are
 * wider than the viewport, and the page pans horizontally rather than silently shrinking back
 * — line accuracy is the one thing this renderer exists to preserve, so it is never traded for
 * fit. A single printed line can span more than one ayah, so highlight and tap are resolved
 * **per word** via [MushafWord.ayahId].
 *
 * Tajweed colouring is intentionally not applied here: the layout carries only the IndoPak
 * glyph text (`text_indopak`), which has no per-letter tajweed spans — the tajweed path stays
 * on the ayah-keyed [MushafContinuousText] renderer.
 *
 * @param onAyahClick invoked with the tapped word's global `ayahId` and the tapped line's
 *   **window** Y (centre); the host converts it to a viewport offset for tooltip anchoring.
 */
@Composable
fun MushafLineLayout(
    lines: List<MushafLine>,
    surahMap: Map<Int, Surah>,
    onAyahClick: (ayahId: Int, tapWindowY: Float) -> Unit,
    modifier: Modifier = Modifier,
    arabicFontSize: Float = 24f,
    arabicFontFamily: FontFamily = IndoPakFontFamily,
    highlightedAyahId: Int? = null,
    selectedAyahId: Int? = null,
    textColor: Color = QuranSurfaceColors.ayahInk,
    highlightColor: Color = MaterialTheme.colorScheme.primaryContainer,
    selectedColor: Color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
    basmalahColor: Color = QuranSurfaceColors.frameGold,
    divisionMarkerColor: Color = QuranSurfaceColors.frameGold,
    // Resolved by the host, which already holds the page's ayahs — this renderer has no
    // database of its own, and must not acquire one to draw three signs.
    divisionMarks: (ayahId: Int) -> MushafDivisionMarks = { MushafDivisionMarks.NONE },
) {
    // Last physical AYAH line on the page — never justified (it may be short).
    val lastAyahIndex = remember(lines) { lines.indexOfLast { it.type == MushafLineType.AYAH } }
    val measurer = rememberTextMeasurer()

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // A page is always measured against a bounded width (its host scrolls vertically, not
        // horizontally). Treat an unbounded one as "unknown" so nothing below tries to turn
        // Constraints.Infinity into a Dp.
        val viewportPx = if (constraints.hasBoundedWidth) constraints.maxWidth else 0

        // The densest line's natural (space-separated) width at the reference size. Measured
        // once per page: text width scales linearly with font size, so every size below is
        // arithmetic on this one number rather than a measure-and-shrink loop per line.
        val widestLinePx = remember(lines, arabicFontFamily, measurer) {
            lines.asSequence()
                .filter { it.type == MushafLineType.AYAH && it.words.isNotEmpty() }
                .maxOfOrNull { line ->
                    measurer.measure(
                        AnnotatedString(line.words.joinToString(" ") { it.text }),
                        style = TextStyle(
                            fontFamily = arabicFontFamily,
                            fontSize = REFERENCE_FONT_SIZE.sp,
                        ),
                    ).size.width
                } ?: 0
        }

        val lineFontSize = pageFitFontSize(
            requestedFontSize = arabicFontSize,
            widestLinePx = widestLinePx,
            viewportPx = viewportPx,
        )
        // What the page needs to be wide enough for at that size. Equal to the viewport
        // whenever the preference is at or below the default, so nothing pans until the
        // reader actually asks for text larger than the page can hold.
        val contentWidthPx = if (widestLinePx <= 0 || viewportPx <= 0) {
            viewportPx
        } else {
            maxOf(viewportPx, (widestLinePx * lineFontSize / REFERENCE_FONT_SIZE).toInt())
        }
        val panState = rememberScrollState()
        val needsPan = contentWidthPx > viewportPx

        Column(
            modifier = Modifier
                .then(if (needsPan) Modifier.horizontalScroll(panState) else Modifier)
                .then(
                    if (contentWidthPx > 0) {
                        Modifier.width(with(LocalDensity.current) { contentWidthPx.toDp() })
                    } else {
                        Modifier.fillMaxWidth()
                    }
                ),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            lines.forEachIndexed { index, line ->
                when (line.type) {
                    MushafLineType.SURAH_HEADER -> {
                        val surah = surahMap[line.surahId]
                        if (surah != null) {
                            // Ruled, not the cartouche: this page is paper. See
                            // `RuledSurahHeading`.
                            RuledSurahHeading(
                                surah = surah,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                // Basmalah is a separate line in the 16-line layout.
                                showBismillah = false,
                            )
                        }
                    }

                    MushafLineType.BASMALAH -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            ArabicText(
                                text = BISMILLAH_TEXT,
                                size = ArabicTextSize.MEDIUM,
                                color = basmalahColor,
                                // Scales with the rest of the page rather than staying at the
                                // atom's fixed size while the ayah lines around it move.
                                style = TextStyle(
                                    fontFamily = arabicFontFamily,
                                    fontSize = lineFontSize.sp,
                                    lineHeight = (lineFontSize * 1.8f).sp,
                                    textAlign = TextAlign.Center,
                                    textDirection = TextDirection.Rtl,
                                ),
                            )
                        }
                    }

                    MushafLineType.AYAH -> {
                        // Justify every ayah line except the page's last ayah line and a
                        // surah's last line (the one immediately before a header cartouche).
                        val nextIsHeader =
                            lines.getOrNull(index + 1)?.type == MushafLineType.SURAH_HEADER
                        val justify = index != lastAyahIndex && !nextIsHeader
                        MushafAyahLine(
                            words = line.words,
                            justify = justify,
                            arabicFontSize = lineFontSize,
                            arabicFontFamily = arabicFontFamily,
                            textColor = textColor,
                            highlightColor = highlightColor,
                            selectedColor = selectedColor,
                            highlightedAyahId = highlightedAyahId,
                            selectedAyahId = selectedAyahId,
                            divisionMarks = divisionMarks,
                            divisionMarkerColor = divisionMarkerColor,
                            onAyahClick = onAyahClick,
                        )
                    }
                }
            }
        }
    }
}

/**
 * The font size every line of a page is drawn at, in sp.
 *
 * [widestLinePx] is the page's densest line measured at [REFERENCE_FONT_SIZE], so
 * `REFERENCE_FONT_SIZE * viewportPx / widestLinePx` is the size at which that line exactly
 * fills the width — the largest size that keeps the whole page inside the viewport. That is
 * what the page renders at when the reader leaves the Arabic font size at its default, and
 * [requestedFontSize] scales it from there. A page whose densest line already fits at the
 * reference size is not enlarged to fill the width: it is drawn at the requested size.
 *
 * Pure and Android-free so the relationship between preference and rendered size is testable.
 */
internal fun pageFitFontSize(
    requestedFontSize: Float,
    widestLinePx: Int,
    viewportPx: Int,
): Float {
    if (widestLinePx <= 0 || viewportPx <= 0) return requestedFontSize
    val fitAtReference =
        (REFERENCE_FONT_SIZE * viewportPx / widestLinePx).coerceAtMost(REFERENCE_FONT_SIZE)
    val scale = requestedFontSize / REFERENCE_FONT_SIZE
    return (fitAtReference * scale).coerceAtLeast(MIN_FONT_SIZE)
}

/**
 * The Arabic font size the page's fit is expressed relative to — the default of
 * `QuranSettingsUiState.arabicFontSize`. Leaving the preference alone therefore reproduces the
 * fit-to-width rendering exactly; moving it scales the page around that.
 */
private const val REFERENCE_FONT_SIZE = 28f

/** Floor for [pageFitFontSize], so a very dense page stays legible on a narrow screen. */
private const val MIN_FONT_SIZE = 10f

/** The structural signs a printed Mushaf carries around the text. */
private const val RUB_EL_HIZB = "۞"
private const val SAJDA_SIGN = "۩"
private const val RUKU_SIGN = "ع"

/** Words are 1-based within their ayah, so this is the verse's opening word. */
private const val FIRST_WORD = 1

/**
 * The end-of-verse glyphs the IndoPak text closes every ayah with — the small rounded zero,
 * alone or with the ornament that follows a surah's final verse.
 */
internal val VERSE_END_GLYPHS = charArrayOf('۟', '۠')

/** Whether this word carries the verse's end glyph, i.e. whether the ayah stops here. */
internal fun String.endsOfVerse(): Boolean = any { it in VERSE_END_GLYPHS }

/**
 * Which structural signs a verse carries. Resolved once per page by the host rather than per
 * word: a page is ~15 lines of ~8 words, and the lookup behind this reads the database.
 */
data class MushafDivisionMarks(
    val opensQuarter: Boolean = false,
    val closesRuku: Boolean = false,
    val hasSajda: Boolean = false,
) {
    companion object {
        val NONE = MushafDivisionMarks()
    }
}

/**
 * One structural sign set beside the text.
 *
 * Slightly smaller than the verse and in the ornament gold, because it is apparatus: it has
 * to be findable at a glance without ever being mistaken for a word of the Qur'an.
 */
@Composable
private fun MushafDivisionSign(
    sign: String,
    arabicFontSize: Float,
    arabicFontFamily: FontFamily,
    color: Color,
) {
    BasicText(
        text = sign,
        style = TextStyle(
            fontFamily = arabicFontFamily,
            fontSize = (arabicFontSize * 0.8f).sp,
            color = color,
            textDirection = TextDirection.Rtl,
            textAlign = TextAlign.Center,
        ),
        modifier = Modifier.padding(horizontal = 2.dp),
    )
}

/**
 * One justified (or naturally-spaced) row of ayah words, right-to-left, at the page's size.
 */
@Composable
private fun MushafAyahLine(
    words: List<MushafWord>,
    justify: Boolean,
    arabicFontSize: Float,
    arabicFontFamily: FontFamily,
    textColor: Color,
    highlightColor: Color,
    selectedColor: Color,
    highlightedAyahId: Int?,
    selectedAyahId: Int?,
    divisionMarks: (Int) -> MushafDivisionMarks,
    divisionMarkerColor: Color,
    onAyahClick: (ayahId: Int, tapWindowY: Float) -> Unit,
) {
    if (words.isEmpty()) return

    // Window Y of this line's centre, captured on layout, reported on tap for tooltip anchoring.
    var lineCenterWindowY by remember { mutableFloatStateOf(0f) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coords ->
                    lineCenterWindowY = coords.positionInWindow().y + coords.size.height / 2f
                }
                .padding(vertical = 2.dp),
            horizontalArrangement = if (justify && words.size > 1) {
                Arrangement.SpaceBetween
            } else {
                Arrangement.Center
            },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            words.forEach { word ->
                val background = when (word.ayahId) {
                    highlightedAyahId -> highlightColor
                    selectedAyahId -> selectedColor
                    else -> Color.Transparent
                }
                val marks = divisionMarks(word.ayahId)

                // ۞ opens a hizb quarter, so it precedes the verse's first word.
                if (marks.opensQuarter && word.position == FIRST_WORD) {
                    MushafDivisionSign(
                        sign = RUB_EL_HIZB,
                        arabicFontSize = arabicFontSize,
                        arabicFontFamily = arabicFontFamily,
                        color = divisionMarkerColor,
                    )
                }

                BasicText(
                    text = word.text,
                    style = TextStyle(
                        fontFamily = arabicFontFamily,
                        fontSize = arabicFontSize.sp,
                        color = textColor,
                        textDirection = TextDirection.Rtl,
                        textAlign = TextAlign.Center,
                    ),
                    modifier = Modifier
                        .background(background, RoundedCornerShape(4.dp))
                        .clickable { onAyahClick(word.ayahId, lineCenterWindowY) }
                        .padding(horizontal = 1.dp, vertical = 2.dp),
                )

                // ۩ and ع follow the verse they belong to. The verse's own end glyph is the
                // reliable anchor: it is the last token of every ayah in the IndoPak text,
                // so this needs no word counts and stays correct when an ayah runs across a
                // page boundary — where "the last word on this page" would be wrong.
                if (word.text.endsOfVerse()) {
                    if (marks.hasSajda) {
                        MushafDivisionSign(
                            sign = SAJDA_SIGN,
                            arabicFontSize = arabicFontSize,
                            arabicFontFamily = arabicFontFamily,
                            color = divisionMarkerColor,
                        )
                    }
                    if (marks.closesRuku) {
                        MushafDivisionSign(
                            sign = RUKU_SIGN,
                            arabicFontSize = arabicFontSize,
                            arabicFontFamily = arabicFontFamily,
                            color = divisionMarkerColor,
                        )
                    }
                }
            }
        }
    }
}

// ==================== PREVIEWS ====================

@Preview(showBackground = true, widthDp = 400, name = "Mushaf Line Layout — Al-Fatihah")
@Composable
private fun MushafLineLayoutFatihahPreview() {
    NimazTheme {
        MushafLineLayout(
            lines = sampleMushafPageLayout.lines,
            surahMap = mapOf(1 to sampleSurahFatihah),
            onAyahClick = { _, _ -> },
        )
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF0A0A08,
    widthDp = 400,
    name = "Mushaf Line Layout — Dark + Highlight",
)
@Composable
private fun MushafLineLayoutDarkPreview() {
    NimazTheme(themeMode = ThemeMode.DARK) {
        MushafLineLayout(
            lines = sampleMushafPageLayout.lines,
            surahMap = mapOf(1 to sampleSurahFatihah),
            highlightedAyahId = 5,
            onAyahClick = { _, _ -> },
        )
    }
}

/** Splits an Arabic string into per-word [MushafWord]s for previews. */
private fun sampleWords(text: String, ayahId: Int, ayahNumber: Int): List<MushafWord> =
    text.trim().split(" ").filter { it.isNotBlank() }.mapIndexed { i, w ->
        MushafWord(text = w, ayahId = ayahId, ayahNumber = ayahNumber, position = i + 1)
    }

/**
 * A sampled Al-Fatihah page laid out the way the 16-line data would deliver it: a header
 * cartouche, a basmalah line, then the ayahs broken across physical lines.
 */
internal val sampleMushafPageLayout: MushafPageLayout = MushafPageLayout(
    page = 1,
    lines = listOf(
        MushafLine(page = 1, lineNumber = 1, type = MushafLineType.SURAH_HEADER, surahId = 1),
        MushafLine(page = 1, lineNumber = 2, type = MushafLineType.BASMALAH, surahId = 1),
        MushafLine(
            page = 1, lineNumber = 3, type = MushafLineType.AYAH, surahId = 1,
            words = sampleWords(
                "ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَٰلَمِينَ",
                ayahId = 2,
                ayahNumber = 2
            ),
        ),
        MushafLine(
            page = 1, lineNumber = 4, type = MushafLineType.AYAH, surahId = 1,
            words = sampleWords("ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", ayahId = 3, ayahNumber = 3) +
                    sampleWords("مَٰلِكِ يَوْمِ ٱلدِّينِ", ayahId = 4, ayahNumber = 4),
        ),
        MushafLine(
            page = 1, lineNumber = 5, type = MushafLineType.AYAH, surahId = 1,
            words = sampleWords(
                "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ",
                ayahId = 5,
                ayahNumber = 5
            ),
        ),
        MushafLine(
            page = 1, lineNumber = 6, type = MushafLineType.AYAH, surahId = 1,
            words = sampleWords("ٱهْدِنَا ٱلصِّرَٰطَ ٱلْمُسْتَقِيمَ", ayahId = 6, ayahNumber = 6),
        ),
        MushafLine(
            page = 1, lineNumber = 7, type = MushafLineType.AYAH, surahId = 1,
            words = sampleWords(
                "صِرَٰطَ ٱلَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ ٱلْمَغْضُوبِ عَلَيْهِمْ وَلَا ٱلضَّآلِّينَ",
                ayahId = 7, ayahNumber = 7,
            ),
        ),
    ),
)
