package com.arshadshah.nimaz.data.audio

import androidx.media3.common.util.UnstableApi
import com.arshadshah.nimaz.domain.model.RecitationRepeat
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * When the end of a surah is the end of the sitting, and when it is a boundary.
 *
 * Continuous playback is described in settings as playing "on to the next verse **and the next
 * surah**", and it only ever did the first half — the playlist is one surah's verses, so its end
 * stopped everything however the setting was set. This is the rule that half was missing, kept
 * pure precisely so it can be asserted: the alternative is arranging for a real ExoPlayer to
 * reach `STATE_ENDED`.
 */
@UnstableApi
class NextSurahToPlayTest {

    private fun decide(
        continuous: Boolean = true,
        isReading: Boolean = true,
        repeat: RecitationRepeat = RecitationRepeat.Off,
        finished: Int? = 18,
    ): Int? = QuranAudioManager.nextSurahToPlay(
        continuousPlayback = continuous,
        playlistIsAReading = isReading,
        repeat = repeat,
        finishedSurah = finished,
    )

    @Test
    fun `a finished surah rolls into its neighbour`() {
        assertThat(decide(finished = 18)).isEqualTo(19)
    }

    @Test
    fun `continuous playback off stops at the end of the surah`() {
        assertThat(decide(continuous = false)).isNull()
    }

    @Test
    fun `a single verse does not roll into a whole surah`() {
        assertThat(decide(isReading = false)).isNull()
    }

    @Test
    fun `a verse repeat is a request to stay`() {
        assertThat(decide(repeat = RecitationRepeat.Ayah(times = 3))).isNull()
    }

    @Test
    fun `a range repeat is a request to stay`() {
        assertThat(decide(repeat = RecitationRepeat.Range(fromAyah = 1, toAyah = 7))).isNull()
    }

    @Test
    fun `a surah repeat never hands over`() {
        assertThat(decide(repeat = RecitationRepeat.Surah)).isNull()
    }

    @Test
    fun `there is nothing after An-Nas`() {
        assertThat(decide(finished = 114)).isNull()
    }

    @Test
    fun `an empty playlist has no next`() {
        assertThat(decide(finished = null)).isNull()
    }
}
