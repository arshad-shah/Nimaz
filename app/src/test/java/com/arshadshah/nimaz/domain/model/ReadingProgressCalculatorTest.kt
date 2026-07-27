package com.arshadshah.nimaz.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for the Continue Reading progress maths.
 *
 * These pin the regression that motivated them: the card used to divide a never-written
 * `totalAyahsRead` counter by 6236, so a reader sitting on An-Nahl verse 45 saw "0%".
 */
class ReadingProgressCalculatorTest {

    // An-Nahl (surah 16) has 128 ayat — the case from the bug report.
    private val anNahlAyahCount = 128

    @Test
    fun `surah progress is relative to the surah, not the whole Quran`() {
        val fraction = ReadingProgressCalculator.surahFraction(45, anNahlAyahCount)

        assertThat(fraction).isWithin(0.001f).of(45f / 128f)
        assertThat(ReadingProgressCalculator.percent(fraction)).isEqualTo(35)
    }

    @Test
    fun `genuine progress never renders as zero percent`() {
        // 1 of 286 (Al-Baqarah) is 0.35%, which naive rounding would show as 0%.
        val fraction = ReadingProgressCalculator.surahFraction(1, 286)

        assertThat(fraction).isGreaterThan(0f)
        assertThat(ReadingProgressCalculator.percent(fraction)).isEqualTo(1)
    }

    @Test
    fun `only a finished surah reports one hundred percent`() {
        assertThat(ReadingProgressCalculator.percent(ReadingProgressCalculator.surahFraction(128, anNahlAyahCount)))
            .isEqualTo(100)
        // One ayah short must not round up to 100%.
        assertThat(ReadingProgressCalculator.percent(ReadingProgressCalculator.surahFraction(127, anNahlAyahCount)))
            .isEqualTo(99)
    }

    @Test
    fun `fraction is clamped and safe for degenerate input`() {
        assertThat(ReadingProgressCalculator.surahFraction(200, anNahlAyahCount)).isEqualTo(1f)
        assertThat(ReadingProgressCalculator.surahFraction(-5, anNahlAyahCount)).isEqualTo(0f)
        assertThat(ReadingProgressCalculator.surahFraction(45, 0)).isEqualTo(0f)
        assertThat(ReadingProgressCalculator.surahFraction(45, -1)).isEqualTo(0f)
    }

    @Test
    fun `page fallback maps mushaf position onto zero to one`() {
        assertThat(ReadingProgressCalculator.pageFraction(302)).isWithin(0.001f).of(0.5f)
        assertThat(ReadingProgressCalculator.pageFraction(604)).isEqualTo(1f)
        assertThat(ReadingProgressCalculator.pageFraction(0)).isEqualTo(0f)
        assertThat(ReadingProgressCalculator.pageFraction(9_999)).isEqualTo(1f)
    }

    @Test
    fun `page fallback follows the active edition's page count`() {
        // The 16-line IndoPak mushaf ends at 548, so its last page is 100%, not 91% (#325).
        val indopak = MushafScript.INDOPAK_16.totalPages
        assertThat(ReadingProgressCalculator.pageFraction(indopak, indopak)).isEqualTo(1f)
        assertThat(ReadingProgressCalculator.pageFraction(274, indopak))
            .isWithin(0.001f).of(0.5f)
        // Degenerate page counts must not divide by zero.
        assertThat(ReadingProgressCalculator.pageFraction(10, 0)).isEqualTo(0f)
        assertThat(ReadingProgressCalculator.pageFraction(10, -1)).isEqualTo(0f)
    }

    @Test
    fun `percent clamps out-of-range fractions`() {
        assertThat(ReadingProgressCalculator.percent(-1f)).isEqualTo(0)
        assertThat(ReadingProgressCalculator.percent(2f)).isEqualTo(100)
    }
}
