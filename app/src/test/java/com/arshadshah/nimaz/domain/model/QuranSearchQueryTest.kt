package com.arshadshah.nimaz.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class QuranSearchQueryTest {

    @Test
    fun `blank input is empty`() {
        assertThat(QuranSearchQuery.parse("")).isEqualTo(QuranSearchQuery.Empty)
        assertThat(QuranSearchQuery.parse("   ")).isEqualTo(QuranSearchQuery.Empty)
    }

    @Test
    fun `juz is recognised in its long and short forms`() {
        listOf("juz 15", "Juz15", "para 15", "j 15", "  JUZ  15 ").forEach { raw ->
            assertThat(QuranSearchQuery.parse(raw)).isEqualTo(QuranSearchQuery.Juz(15))
        }
    }

    @Test
    fun `page is recognised in its long and short forms`() {
        listOf("page 299", "Page299", "pg 299", "p 299").forEach { raw ->
            assertThat(QuranSearchQuery.parse(raw)).isEqualTo(QuranSearchQuery.Page(299))
        }
    }

    @Test
    fun `a bare number is a surah number`() {
        assertThat(QuranSearchQuery.parse("18")).isEqualTo(QuranSearchQuery.SurahNumber(18))
    }

    @Test
    fun `out-of-range juz falls back to a name search`() {
        assertThat(QuranSearchQuery.parse("juz 31")).isEqualTo(QuranSearchQuery.Name("juz 31"))
        assertThat(QuranSearchQuery.parse("juz 0")).isEqualTo(QuranSearchQuery.Name("juz 0"))
    }

    @Test
    fun `out-of-range page falls back to a name search`() {
        assertThat(QuranSearchQuery.parse("page 9999")).isEqualTo(QuranSearchQuery.Name("page 9999"))
    }

    @Test
    fun `out-of-range surah number falls back to a name search`() {
        assertThat(QuranSearchQuery.parse("115")).isEqualTo(QuranSearchQuery.Name("115"))
        assertThat(QuranSearchQuery.parse("0")).isEqualTo(QuranSearchQuery.Name("0"))
    }

    @Test
    fun `anything else is a name search, trimmed and lowercased`() {
        assertThat(QuranSearchQuery.parse("  Al-Kahf ")).isEqualTo(QuranSearchQuery.Name("al-kahf"))
    }

    @Test
    fun `a name that merely starts with p or j is not a page or juz`() {
        assertThat(QuranSearchQuery.parse("patience")).isEqualTo(QuranSearchQuery.Name("patience"))
        assertThat(QuranSearchQuery.parse("Jonah")).isEqualTo(QuranSearchQuery.Name("jonah"))
    }
}
