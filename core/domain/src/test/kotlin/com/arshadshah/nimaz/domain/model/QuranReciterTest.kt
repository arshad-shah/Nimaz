package com.arshadshah.nimaz.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The reciter catalogue and the CDN wiring have to agree.
 *
 * `QuranReciter` is the single source of truth for *who* can be selected; the audio edition each
 * one streams lives separately in `QuranAudioManager.RECITER_CDN_MAP`. That agreement is
 * asserted by `QuranReciterCdnMapTest` in `:app`, which is the only side of the boundary that
 * can see both; this file covers the enum itself.
 *
 * [id] is also persisted as `quran_reciter_id`, so it must stay stable and unique across the
 * enum and its aliases, or a stored preference resolves to a different reciter.
 */
class QuranReciterTest {

    @Test
    fun `ids and aliases are unique across the whole catalogue`() {
        // fromId matches on either, so a duplicate would make resolution order-dependent.
        val everyToken = QuranReciter.entries.flatMap { listOf(it.id) + it.aliases }
        assertThat(everyToken).containsNoDuplicates()
    }

    @Test
    fun `the ids older builds wrote still resolve to the same reciter`() {
        // These were the ids in the audio layer's original CDN map, and are still on disk for
        // anyone who picked a reciter before the catalogue existed.
        assertThat(QuranReciter.fromId("alafasy")).isEqualTo(QuranReciter.MISHARY)
        assertThat(QuranReciter.fromId("muaiqly")).isEqualTo(QuranReciter.MAHER)
    }

    @Test
    fun `an unset or unknown preference falls back to the default rather than failing`() {
        assertThat(QuranReciter.fromId(null)).isEqualTo(QuranReciter.DEFAULT)
        assertThat(QuranReciter.fromId("")).isEqualTo(QuranReciter.DEFAULT)
        assertThat(QuranReciter.fromId("someone_from_a_newer_build"))
            .isEqualTo(QuranReciter.DEFAULT)
    }

    @Test
    fun `every reciter round-trips through its own id`() {
        QuranReciter.entries.forEach { reciter ->
            assertThat(QuranReciter.fromId(reciter.id)).isEqualTo(reciter)
        }
    }

    @Test
    fun `search matches on name and on country, and is case-insensitive`() {
        assertThat(QuranReciter.search("alafasy")).containsExactly(QuranReciter.MISHARY)
        assertThat(QuranReciter.search("EGYPT")).isNotEmpty()
        assertThat(QuranReciter.search("Egypt")).containsExactlyElementsIn(
            QuranReciter.entries.filter { it.country == "Egypt" }
        )
    }

    @Test
    fun `a blank search returns the whole catalogue, not nothing`() {
        assertThat(QuranReciter.search("")).containsExactlyElementsIn(QuranReciter.entries)
        assertThat(QuranReciter.search("   ")).containsExactlyElementsIn(QuranReciter.entries)
    }

    @Test
    fun `every reciter carries a name and a country to show in the picker`() {
        QuranReciter.entries.forEach { reciter ->
            assertThat(reciter.displayName).isNotEmpty()
            assertThat(reciter.country).isNotEmpty()
        }
    }
}
