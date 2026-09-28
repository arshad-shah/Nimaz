package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.graphics.Color
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
