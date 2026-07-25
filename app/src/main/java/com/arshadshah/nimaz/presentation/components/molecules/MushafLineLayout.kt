package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.arshadshah.nimaz.presentation.components.atoms.BISMILLAH_TEXT
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
 * - [MushafLineType.SURAH_HEADER] → a centred [SurahHeaderCartouche] (bismillah suppressed;
 *   the basmalah is its own line in the layout).
 * - [MushafLineType.BASMALAH] → the basmalah centred on its own line.
 *
 * Each ayah line auto-fits its font **down** (never above [arabicFontSize]) so a dense line
 * never overflows the page width — the fixed-fit half of the "fixed-fit vs. reflow" trade-off
 * called out in #269. A single printed line can span more than one ayah, so highlight and tap
 * are resolved **per word** via [MushafWord.ayahId].
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
) {
    // Last physical AYAH line on the page — never justified (it may be short).
    val lastAyahIndex = remember(lines) { lines.indexOfLast { it.type == MushafLineType.AYAH } }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        lines.forEachIndexed { index, line ->
            when (line.type) {
                MushafLineType.SURAH_HEADER -> {
                    val surah = surahMap[line.surahId]
                    if (surah != null) {
                        SurahHeaderCartouche(
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
                        )
                    }
                }

                MushafLineType.AYAH -> {
                    // Justify every ayah line except the page's last ayah line and a surah's
                    // last line (the one immediately before a header cartouche).
                    val nextIsHeader = lines.getOrNull(index + 1)?.type == MushafLineType.SURAH_HEADER
                    val justify = index != lastAyahIndex && !nextIsHeader
                    MushafAyahLine(
                        words = line.words,
                        justify = justify,
                        arabicFontSize = arabicFontSize,
                        arabicFontFamily = arabicFontFamily,
                        textColor = textColor,
                        highlightColor = highlightColor,
                        selectedColor = selectedColor,
                        highlightedAyahId = highlightedAyahId,
                        selectedAyahId = selectedAyahId,
                        onAyahClick = onAyahClick,
                    )
                }
            }
        }
    }
}

/**
 * One justified (or naturally-spaced) row of ayah words, right-to-left, auto-fitted so the
 * widest natural rendering never exceeds the available width.
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
    onAyahClick: (ayahId: Int, tapWindowY: Float) -> Unit,
) {
    if (words.isEmpty()) return

    val measurer = rememberTextMeasurer()
    // Window Y of this line's centre, captured on layout, reported on tap for tooltip anchoring.
    var lineCenterWindowY by remember { mutableFloatStateOf(0f) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val maxWidthPx = constraints.maxWidth
            val naturalText = remember(words) { words.joinToString(" ") { it.text } }

            // Shrink the font until the natural (space-separated) rendering fits the width.
            // The justified layout drops the inter-word spaces into gaps, so a line that fits
            // naturally always fits when justified.
            val fitFontSize = remember(naturalText, maxWidthPx, arabicFontSize, arabicFontFamily) {
                if (maxWidthPx <= 0) return@remember arabicFontSize
                val minSize = (arabicFontSize * 0.55f).coerceAtLeast(12f)
                var size = arabicFontSize
                while (size > minSize) {
                    val measured = measurer.measure(
                        AnnotatedString(naturalText),
                        style = TextStyle(fontFamily = arabicFontFamily, fontSize = size.sp),
                    )
                    if (measured.size.width <= maxWidthPx) break
                    size -= 1f
                }
                size
            }

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
                    BasicText(
                        text = word.text,
                        style = TextStyle(
                            fontFamily = arabicFontFamily,
                            fontSize = fitFontSize.sp,
                            color = textColor,
                            textDirection = TextDirection.Rtl,
                            textAlign = TextAlign.Center,
                        ),
                        modifier = Modifier
                            .background(background, RoundedCornerShape(4.dp))
                            .clickable { onAyahClick(word.ayahId, lineCenterWindowY) }
                            .padding(horizontal = 1.dp, vertical = 2.dp),
                    )
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
            words = sampleWords("ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَٰلَمِينَ", ayahId = 2, ayahNumber = 2),
        ),
        MushafLine(
            page = 1, lineNumber = 4, type = MushafLineType.AYAH, surahId = 1,
            words = sampleWords("ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", ayahId = 3, ayahNumber = 3) +
                sampleWords("مَٰلِكِ يَوْمِ ٱلدِّينِ", ayahId = 4, ayahNumber = 4),
        ),
        MushafLine(
            page = 1, lineNumber = 5, type = MushafLineType.AYAH, surahId = 1,
            words = sampleWords("إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ", ayahId = 5, ayahNumber = 5),
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
