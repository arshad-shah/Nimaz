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
    fun `fromName parses stored enum names and falls back to the default`() {
        assertThat(MushafScript.fromName("MADANI")).isEqualTo(MushafScript.MADANI)
        assertThat(MushafScript.fromName("INDOPAK_16")).isEqualTo(MushafScript.INDOPAK_16)
        // null / legacy / unknown values must never break the reader — default to MADANI.
        assertThat(MushafScript.fromName(null)).isEqualTo(MushafScript.DEFAULT)
        assertThat(MushafScript.fromName("")).isEqualTo(MushafScript.DEFAULT)
        assertThat(MushafScript.fromName("madani")).isEqualTo(MushafScript.DEFAULT)
        assertThat(MushafScript.fromName("GALAXY")).isEqualTo(MushafScript.DEFAULT)
    }

    @Test
    fun `max total pages is the largest edition`() {
        // The upper bound for a page number whose edition isn't known yet (e.g. a deep link
        // resolved before the script preference is read). It is the 13-line IndoPak's 847,
        // not the Madani 604 — the reader clamps to the active edition afterwards.
        assertThat(MushafScript.MAX_TOTAL_PAGES).isEqualTo(847)
        MushafScript.entries.forEach {
            assertThat(it.totalPages).isAtMost(MushafScript.MAX_TOTAL_PAGES)
        }
    }

    @Test
    fun `only ayah-flow editions lack a line count and a text source`() {
        // The two properties travel together: a line-accurate edition needs stored glyphs to
        // slice, and an ayah-flow one renders the ayahs table's own text. A mismatch would
        // make the seeder silently skip an edition the reader then draws blank.
        MushafScript.entries.forEach { script ->
            assertThat(script.isLineAccurate).isEqualTo(script.linesPerPage != null)
            assertThat(script.isLineAccurate).isEqualTo(script.textSource != null)
        }
        assertThat(MushafScript.MADANI.isLineAccurate).isFalse()
        assertThat(MushafScript.INDOPAK_16.isLineAccurate).isTrue()
    }

    @Test
    fun `the IndoPak 15 and 16 line editions share a text source and the 13 line does not`() {
        // Verified against the shipped data by MushafLayoutFidelityTest; asserted here so the
        // catalogue itself states the relationship the storage layer depends on.
        assertThat(MushafScript.INDOPAK_15.textSource).isEqualTo(MushafScript.INDOPAK_16.textSource)
        assertThat(MushafScript.INDOPAK_13.textSource)
            .isNotEqualTo(MushafScript.INDOPAK_16.textSource)
    }

    @Test
    fun `default stays the ayah-flow Madani edition`() {
        // Changing this would repaginate the Quran for every user who never picked an edition.
        assertThat(MushafScript.DEFAULT).isEqualTo(MushafScript.MADANI)
        assertThat(MushafScript.DEFAULT.totalPages).isEqualTo(604)
    }

    @Test
    fun `line type parsing is tolerant of casing and unknown values`() {
        assertThat(MushafLineType.fromString("surah_header")).isEqualTo(MushafLineType.SURAH_HEADER)
        assertThat(MushafLineType.fromString("BASMALAH")).isEqualTo(MushafLineType.BASMALAH)
        assertThat(MushafLineType.fromString("ayah")).isEqualTo(MushafLineType.AYAH)
        assertThat(MushafLineType.fromString("something-else")).isEqualTo(MushafLineType.AYAH)
    }
}
