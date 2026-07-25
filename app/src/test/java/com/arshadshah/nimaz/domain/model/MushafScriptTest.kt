package com.arshadshah.nimaz.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Pins the script-aware page counts (#268): the 16-line IndoPak edition paginates to 548
 * pages vs. 604 for the Madani mushaf, and the legacy [ReadingProgressCalculator.TOTAL_QURAN_PAGES]
 * stays single-sourced from [MushafScript.MADANI].
 */
class MushafScriptTest {

    @Test
    fun `page counts match each edition`() {
        assertThat(MushafScript.MADANI.totalPages).isEqualTo(604)
        assertThat(MushafScript.INDOPAK_16.totalPages).isEqualTo(548)
    }

    @Test
    fun `legacy total pages stays in sync with the Madani script`() {
        assertThat(ReadingProgressCalculator.TOTAL_QURAN_PAGES)
            .isEqualTo(MushafScript.MADANI.totalPages)
    }

    @Test
    fun `line type parsing is tolerant of casing and unknown values`() {
        assertThat(MushafLineType.fromString("surah_header")).isEqualTo(MushafLineType.SURAH_HEADER)
        assertThat(MushafLineType.fromString("BASMALAH")).isEqualTo(MushafLineType.BASMALAH)
        assertThat(MushafLineType.fromString("ayah")).isEqualTo(MushafLineType.AYAH)
        assertThat(MushafLineType.fromString("something-else")).isEqualTo(MushafLineType.AYAH)
    }
}
