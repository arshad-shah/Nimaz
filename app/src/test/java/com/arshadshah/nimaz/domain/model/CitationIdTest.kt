package com.arshadshah.nimaz.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CitationIdTest {

    @Test
    fun `quran id round-trips`() {
        val parsed = CitationId.parse("quran:2:255")
        assertThat(parsed).isEqualTo(CitationId.Quran(2, 255))
        assertThat(parsed!!.raw).isEqualTo("quran:2:255")
        assertThat(parsed.source).isEqualTo(ProofSource.QURAN)
    }

    @Test
    fun `hadith id round-trips with opaque string id`() {
        val parsed = CitationId.parse("hadith:bukhari-1")
        assertThat(parsed).isEqualTo(CitationId.Hadith("bukhari-1"))
        assertThat(parsed!!.raw).isEqualTo("hadith:bukhari-1")
    }

    @Test
    fun `dua id round-trips`() {
        val parsed = CitationId.parse("dua:morning-42")
        assertThat(parsed).isEqualTo(CitationId.Dua("morning-42"))
        assertThat(parsed!!.raw).isEqualTo("dua:morning-42")
    }

    @Test
    fun `malformed ids return null`() {
        assertThat(CitationId.parse("")).isNull()
        assertThat(CitationId.parse("garbage")).isNull()
        assertThat(CitationId.parse("quran:")).isNull()
        assertThat(CitationId.parse("quran:2")).isNull()
        assertThat(CitationId.parse("quran:2:255:1")).isNull()
        assertThat(CitationId.parse("quran:x:255")).isNull()
        assertThat(CitationId.parse("quran:0:1")).isNull()
        assertThat(CitationId.parse("quran:2:0")).isNull()
        assertThat(CitationId.parse("unknown:1")).isNull()
        assertThat(CitationId.parse(":2:255")).isNull()
    }

    @Test
    fun `parse trims surrounding whitespace`() {
        assertThat(CitationId.parse("  quran:2:255  ")).isEqualTo(CitationId.Quran(2, 255))
    }
}
