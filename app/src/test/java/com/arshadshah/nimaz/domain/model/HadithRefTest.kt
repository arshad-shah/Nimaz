package com.arshadshah.nimaz.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HadithRefTest {

    @Test
    fun `parses a canonical collection ref and round-trips the reference`() {
        val parsed = HadithRef.parse("bukhari:6018")
        assertThat(parsed).isEqualTo(HadithRef("bukhari", 6018))
        assertThat(parsed!!.reference).isEqualTo("bukhari:6018")
    }

    @Test
    fun `normalizes case and whitespace`() {
        assertThat(HadithRef.parse("  Muslim:2564 ")).isEqualTo(HadithRef("muslim", 2564))
    }

    @Test
    fun `malformed refs return null`() {
        assertThat(HadithRef.parse("")).isNull()
        assertThat(HadithRef.parse("bukhari")).isNull()
        assertThat(HadithRef.parse("bukhari:")).isNull()
        assertThat(HadithRef.parse(":6018")).isNull()
        assertThat(HadithRef.parse("bukhari:0")).isNull()
        assertThat(HadithRef.parse("bukhari:-1")).isNull()
        assertThat(HadithRef.parse("bukhari:12:34")).isNull()
        assertThat(HadithRef.parse("bukhari:abc")).isNull()
        assertThat(HadithRef.parse("2:153")).isNull() // a Quran ref is not a hadith ref
    }
}
