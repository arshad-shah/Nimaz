package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.domain.model.TafseerHighlight
import com.arshadshah.nimaz.presentation.theme.AmiriFontFamily
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TafseerHighlightableTextTest {
    private fun mark(start: Int, end: Int) = TafseerHighlight(1, 1, "ibn_kathir_en", start, end, "#FDE68A", null, 0, 0)

    @Test fun `mixed scripts and whitespace keep exact persisted offsets`() {
        val original = "Introduction\nEnglish بسم الله الرحمن الرحيم\n\nmore text."
        val styled = styledTafseerText(original, listOf(mark(13, 20)), 16f)
        assertThat(styled.text).isEqualTo(original)
        assertThat(styled.spanStyles.any { it.item.fontFamily == AmiriFontFamily }).isTrue()
        val span = styled.spanStyles.single { it.item.background != Color.Unspecified }
        assertThat(span.start).isEqualTo(13)
        assertThat(span.end).isEqualTo(20)
        assertThat(span.item.color).isEqualTo(NimazColors.OnSurfaceLight)
    }

    @Test fun `vocalised Arabic stays in one shaping run with its marks and original offsets`() {
        val quote = "الْحَمْدُ للهِ رَبَ الْعَالَمِينَ أُمُّ الْقُرْآنِ وَأُمُّ الْكِتَابِ وَالسَّبْعُ الْمَثَانِي وَالْقُرْآنُ الْعَظِيمُ"
        val prefix = "The Messenger said,\n"
        val original = prefix + quote + "\nAl-Hamdu lillahi Rabbil-`Alamin is the Mother of the Qur'an."
        val start = prefix.length
        val end = start + quote.length
        for (size in listOf(16f, 24f)) {
            val styled = styledTafseerText(original, listOf(mark(start, end)), size)
            assertThat(styled.text).isEqualTo(original)
            val run = styled.spanStyles.single { it.item.fontFamily == AmiriFontFamily }
            assertThat(run.start).isEqualTo(start)
            assertThat(run.end).isEqualTo(end)
            assertThat(run.item.fontSize).isEqualTo((size + 8f).sp)
            assertThat(run.item.letterSpacing).isEqualTo(0.sp)
            val paragraph = styled.paragraphStyles.single()
            assertThat(paragraph.start).isEqualTo(start)
            assertThat(paragraph.end).isEqualTo(end)
            assertThat(paragraph.item.lineHeight.value).isWithin(0.001f)
                .of(44f * ((size + 8f) / 24f))
            val highlight = styled.spanStyles.single { it.item.background != Color.Unspecified }
            assertThat(highlight.start).isEqualTo(start)
            assertThat(highlight.end).isEqualTo(end)
        }
    }

    @Test fun `Arabic spans keep combining marks without absorbing adjacent English or newlines`() {
        val original = "English café قُرْآنٌ، رَبِّ English\nبِسْمِ اللهِ\nTranslation."
        val styled = styledTafseerText(original, emptyList(), 16f)
        val runs = styled.spanStyles.filter { it.item.fontFamily == AmiriFontFamily }
        assertThat(runs.map { original.substring(it.start, it.end) })
            .containsExactly("قُرْآنٌ، رَبِّ", "بِسْمِ اللهِ").inOrder()
        assertThat(styled.text).isEqualTo(original)
        assertThat(styled.paragraphStyles).hasSize(2)
    }

    @Test fun `invalid old ranges are clamped without changing content`() {
        val styled = styledTafseerText("Mercy", listOf(mark(-2, 900), mark(600, 900)), 24f)
        assertThat(styled.text).isEqualTo("Mercy")
        val span = styled.spanStyles.single { it.item.background != Color.Unspecified }
        assertThat(span.start).isEqualTo(0)
        assertThat(span.end).isEqualTo(5)
    }

    @Test fun `invalid highlight colour has a safe fallback`() {
        assertThat(parseColor("not a colour")).isEqualTo(NimazColors.Secondary)
    }
}
