package com.arshadshah.nimaz.domain.search

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Word forms and synonyms, which the index cannot do for itself: FTS4 on Android has no
 * stemmer, only prefix matching, so everything here is "which prefixes to ask for".
 *
 * The property every rule has to keep is that it only *widens*: the typed word is always
 * matched, directly or through a shorter prefix that covers it.
 */
class SearchTermExpanderTest {

    @Test
    fun `endings trim to a base whose prefix still reaches the typed word`() {
        assertThat(SearchTermExpander.trimEnding("prayers")).isEqualTo("prayer")
        assertThat(SearchTermExpander.trimEnding("fasting")).isEqualTo("fast")
        assertThat(SearchTermExpander.trimEnding("prayed")).isEqualTo("pray")
        assertThat(SearchTermExpander.trimEnding("forgiveness")).isEqualTo("forgive")
        assertThat(SearchTermExpander.trimEnding("wishes")).isEqualTo("wish")
        assertThat(SearchTermExpander.trimEnding("sitting")).isEqualTo("sit")
        assertThat(SearchTermExpander.trimEnding("mercies")).isEqualTo("mercy")
    }

    @Test
    fun `words the rules would mangle are left alone`() {
        // "moses" → "mos" would reach "mosque"; "jesus" is not a plural.
        assertThat(SearchTermExpander.trimEnding("moses")).isEqualTo("mose")
        assertThat(SearchTermExpander.trimEnding("jesus")).isNull()
        assertThat(SearchTermExpander.trimEnding("bliss")).isNull()
        // Too short to trim safely, and Arabic is not suffix-trimmed at all.
        assertThat(SearchTermExpander.trimEnding("days")).isNull()
        assertThat(SearchTermExpander.trimEnding("الصلوات")).isNull()
    }

    @Test
    fun `the typed word is always covered`() {
        listOf("prayers", "fasting", "mercies", "rahman", "الله", "salah", "a").forEach { word ->
            val prefixes = SearchTermExpander.prefixes(word)
            assertThat(prefixes.any { word.startsWith(it) }).isTrue()
        }
    }

    @Test
    fun `synonyms expand both ways`() {
        assertThat(SearchTermExpander.prefixes("salah")).contains("prayer")
        assertThat(SearchTermExpander.prefixes("prayer")).contains("salah")
        // Looked up under the trimmed base too, so the plural joins the same group.
        assertThat(SearchTermExpander.prefixes("prayers")).contains("salat")
    }

    @Test
    fun `a prefix another one covers is dropped`() {
        // "prayer*" already matches "prayers"; asking for both is redundant work.
        assertThat(SearchTermExpander.prefixes("prayers")).doesNotContain("prayers")
    }

    @Test
    fun `blank is nothing`() {
        assertThat(SearchTermExpander.prefixes("")).isEmpty()
    }
}
