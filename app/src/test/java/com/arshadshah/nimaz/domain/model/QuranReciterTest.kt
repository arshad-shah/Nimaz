@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.arshadshah.nimaz.domain.model

import com.arshadshah.nimaz.data.audio.QuranAudioManager
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The reciter catalogue and the CDN wiring have to agree.
 *
 * `QuranReciter` is the single source of truth for *who* can be selected; the audio edition each
 * one streams lives separately in `QuranAudioManager.RECITER_CDN_MAP`, because which slug at
 * which bitrate is a data-layer concern. Nothing forces the two to line up: a reciter added to
 * the enum without a map entry falls back to `ar.alafasy` and plays **the wrong voice** — no
 * crash, no empty state, just Alafasy under someone else's name.
 *
 * [id] is also persisted as `quran_reciter_id`, so it must stay stable and unique across the
 * enum and its aliases, or a stored preference resolves to a different reciter.
 */
class QuranReciterTest {

    @Test
    fun `every selectable reciter has a CDN edition`() {
        val missing = QuranReciter.entries.filter { it !in QuranAudioManager.RECITER_CDN_MAP }
        assertThat(missing.map { it.id }).isEmpty()
    }

    @Test
    fun `no CDN edition is stranded without a reciter to select it`() {
        // The reverse drift: a map entry nothing can reach is dead wiring, and usually means a
        // reciter was removed from the catalogue without cleaning up.
        assertThat(QuranAudioManager.RECITER_CDN_MAP.keys)
            .containsExactlyElementsIn(QuranReciter.entries)
    }

    @Test
    fun `ids and aliases are unique across the whole catalogue`() {
        // fromId matches on either, so a duplicate would make resolution order-dependent.
        val everyToken = QuranReciter.entries.flatMap { listOf(it.id) + it.aliases }
        assertThat(everyToken).containsNoDuplicates()
    }

    @Test
    fun `the ids older builds wrote still resolve to the same reciter`() {
        // These were the ids in QuranAudioManager's original map, and are still on disk for
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
