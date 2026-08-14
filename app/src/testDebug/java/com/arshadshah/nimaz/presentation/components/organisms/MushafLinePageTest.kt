package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.domain.model.MushafLine
import com.arshadshah.nimaz.domain.model.MushafLineType
import com.arshadshah.nimaz.domain.model.MushafPageLayout
import com.arshadshah.nimaz.domain.model.MushafWord
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.presentation.components.atoms.BISMILLAH_TEXT
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Robolectric render tests for the 16-line IndoPak page organism [MushafLinePage] (5/7),
 * added in the 7/7 verification pass (#271). Complements the ayah-keyed [MushafPageTest]:
 * here the page is driven by a printed [MushafPageLayout], so the tests pin that the header
 * cartouche, the **dedicated basmalah line**, the per-word ayah lines, and the page footer
 * all render line-for-line as supplied.
 */
@RunWith(RobolectricTestRunner::class)
class MushafLinePageTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val fatihah = Surah(
        number = 1,
        nameArabic = "الفاتحة",
        nameEnglish = "Al-Fatihah",
        nameTransliteration = "The Opening",
        revelationType = RevelationType.MECCAN,
        ayahCount = 7,
        orderInMushaf = 1,
        startPage = 1,
    )

    private val anbiya = Surah(
        number = 21,
        nameArabic = "الأنبياء",
        nameEnglish = "Al-Anbiya",
        nameTransliteration = "The Prophets",
        revelationType = RevelationType.MECCAN,
        ayahCount = 112,
        orderInMushaf = 21,
        startPage = 322,
    )

    private fun words(vararg text: String, ayahId: Int, ayahNumber: Int) =
        text.mapIndexed { i, w -> MushafWord(text = w, ayahId = ayahId, ayahNumber = ayahNumber, position = i + 1) }

    @Test
    fun `renders the page number footer`() {
        composeRule.setThemedContent {
            MushafLinePage(
                pageNumber = 547,
                layout = MushafPageLayout(
                    page = 547,
                    lines = listOf(
                        MushafLine(page = 547, lineNumber = 1, type = MushafLineType.AYAH, surahId = 1,
                            words = words("قُلْ", ayahId = 6222, ayahNumber = 1)),
                    ),
                ),
                surahMap = mapOf(1 to fatihah),
            )
        }

        composeRule.onNodeWithText("547").assertExists()
    }

    @Test
    fun `renders the surah header and its dedicated basmalah line`() {
        // The mapper (post-#271 fix) emits a header line and a separate basmalah line for the
        // 81 surahs that ship both on one line_number, e.g. Al-Anbiya (21) on p.290. The page
        // must draw both — the ruled heading AND the standalone basmalah.
        composeRule.setThemedContent {
            MushafLinePage(
                pageNumber = 290,
                layout = MushafPageLayout(
                    page = 290,
                    lines = listOf(
                        MushafLine(page = 290, lineNumber = 1, type = MushafLineType.SURAH_HEADER, surahId = 21),
                        MushafLine(page = 290, lineNumber = 1, type = MushafLineType.BASMALAH, surahId = 21),
                        MushafLine(page = 290, lineNumber = 3, type = MushafLineType.AYAH, surahId = 21,
                            words = words("ٱقْتَرَبَ", "لِلنَّاسِ", ayahId = 2484, ayahNumber = 1)),
                    ),
                ),
                surahMap = mapOf(21 to anbiya),
            )
        }

        // The ruled heading prints the Arabic name only — see `RuledSurahHeading`.
        composeRule.onNodeWithText("الأنبياء").assertExists()
        composeRule.onNodeWithText("Meccan").assertDoesNotExist()
        composeRule.onNodeWithText(BISMILLAH_TEXT).assertExists()
        composeRule.onNodeWithText("ٱقْتَرَبَ").assertExists()
    }

    @Test
    fun `renders every word of a line that spans two ayahs`() {
        // A single printed line can carry the tail of one ayah and the head of the next; both
        // ayahs' words must render, each tappable in reading order.
        composeRule.setThemedContent {
            MushafLinePage(
                pageNumber = 42,
                layout = MushafPageLayout(
                    page = 42,
                    lines = listOf(
                        MushafLine(page = 42, lineNumber = 1, type = MushafLineType.AYAH, surahId = 1,
                            words = words("وَٱلضُّحَىٰ", ayahId = 500, ayahNumber = 1) +
                                words("وَٱلَّيْلِ", ayahId = 501, ayahNumber = 2)),
                    ),
                ),
                surahMap = mapOf(1 to fatihah),
            )
        }

        composeRule.onNodeWithText("وَٱلضُّحَىٰ").assertExists()
        composeRule.onNodeWithText("وَٱلَّيْلِ").assertExists()
    }

    @Test
    fun `does not render a basmalah when the layout has none`() {
        // Al-Fatihah (1) and At-Tawbah (9) carry no basmalah line; the page must not invent one.
        composeRule.setThemedContent {
            MushafLinePage(
                pageNumber = 1,
                layout = MushafPageLayout(
                    page = 1,
                    lines = listOf(
                        MushafLine(page = 1, lineNumber = 1, type = MushafLineType.SURAH_HEADER, surahId = 1),
                        MushafLine(page = 1, lineNumber = 2, type = MushafLineType.AYAH, surahId = 1,
                            words = words("بِسْمِ", "ٱللَّهِ", ayahId = 1, ayahNumber = 1)),
                    ),
                ),
                surahMap = mapOf(1 to fatihah),
            )
        }

        composeRule.onNodeWithText("الفاتحة").assertExists()
        // The full centred basmalah line (BISMILLAH_TEXT) is not drawn as a standalone line.
        composeRule.onAllNodesWithText(BISMILLAH_TEXT).assertCountEquals(0)
    }
}
