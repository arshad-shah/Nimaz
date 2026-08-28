@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.arshadshah.nimaz.data.audio

import com.arshadshah.nimaz.domain.model.QuranReciter
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The reciter catalogue and the CDN wiring have to agree.
 *
 * `QuranReciter` is the single source of truth for *who* can be selected; the audio edition each
 * one streams lives separately in [QuranAudioManager.RECITER_CDN_MAP], because which slug at
 * which bitrate is a data-layer concern. Nothing forces the two to line up: a reciter added to
 * the enum without a map entry falls back to `ar.alafasy` and plays **the wrong voice** — no
 * crash, no empty state, just Alafasy under someone else's name.
 *
 * These two assertions used to sit in `QuranReciterTest`, which moved to `:core:domain` — a pure
 * JVM module that cannot see `data/`, and must not. The contract is between the two layers, so
 * the test belongs on the data side of it. Everything in `QuranReciterTest` that is about the
 * enum alone stayed there.
 */
class QuranReciterCdnMapTest {

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
}
