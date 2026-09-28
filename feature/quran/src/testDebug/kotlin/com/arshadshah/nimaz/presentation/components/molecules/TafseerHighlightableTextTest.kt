package com.arshadshah.nimaz.presentation.components.molecules

import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.TypefaceSpan
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.domain.model.TafseerHighlight
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TafseerHighlightableTextTest {
    private fun mark(start: Int, end: Int) = TafseerHighlight(1, 1, "ibn_kathir_en", start, end, "#FDE68A", null, 0, 0)

    @Test fun `mixed scripts and whitespace keep exact persisted offsets`() {
        val original = "Introduction\nEnglish بسم الله الرحمن الرحيم\n\nmore text."
        val styled = styledTafseerText(ApplicationProvider.getApplicationContext(), original, listOf(mark(13, 20)), 16f)
        assertThat(styled.toString()).isEqualTo(original)
        assertThat(styled.getSpans(0, styled.length, TypefaceSpan::class.java)).isNotEmpty()
        val span = styled.getSpans(0, styled.length, BackgroundColorSpan::class.java).single()
        assertThat(styled.getSpanStart(span)).isEqualTo(13)
        assertThat(styled.getSpanEnd(span)).isEqualTo(20)
        assertThat(styled.getSpans(0, styled.length, ForegroundColorSpan::class.java)).hasLength(1)
    }

    @Test fun `invalid old ranges are clamped without changing content`() {
        val styled = styledTafseerText(ApplicationProvider.getApplicationContext(), "Mercy", listOf(mark(-2, 900), mark(600, 900)), 24f)
        assertThat(styled.toString()).isEqualTo("Mercy")
        assertThat(styled.getSpans(0, styled.length, BackgroundColorSpan::class.java)).hasLength(1)
    }

    @Test fun `invalid highlight colour has a safe fallback`() {
        assertThat(parseColor("not a colour")).isEqualTo(com.arshadshah.nimaz.presentation.theme.NimazColors.Secondary)
    }
}
