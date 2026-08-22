package com.arshadshah.nimaz.presentation.screens.quran

import com.arshadshah.nimaz.presentation.viewmodel.quran.ReadingMode
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for [surahToFollowRecitationInto] — continuous playback rolled the recitation on
 * into the next surah but the reader stayed on the one that had just finished, so anyone with
 * the app open kept looking at verses nobody was reciting any more.
 */
class ReaderFollowsRecitationTest {

    private fun follow(
        readingMode: ReadingMode = ReadingMode.SURAH,
        inPageView: Boolean = false,
        openSurah: Int? = 18,
        isAudioActive: Boolean = true,
        recitedSurah: Int = 19,
    ) = surahToFollowRecitationInto(
        readingMode = readingMode,
        inPageView = inPageView,
        openSurah = openSurah,
        isAudioActive = isAudioActive,
        recitedSurah = recitedSurah,
    )

    @Test
    fun `follows the recitation out of the finished surah into the next one`() {
        assertThat(follow(openSurah = 18, recitedSurah = 19)).isEqualTo(19)
    }

    @Test
    fun `stays put while the recitation is still in the surah on screen`() {
        assertThat(follow(openSurah = 18, recitedSurah = 18)).isNull()
    }

    @Test
    fun `does not chase a recitation the reader is not reading along with`() {
        // Al-Hijr open, Al-Baqarah playing, and Al-Baqarah rolls into Al-Imran: a supported
        // state — the audio bar is written for it — and not a reason to move the reader.
        assertThat(follow(openSurah = 15, recitedSurah = 3)).isNull()
    }

    @Test
    fun `does not jump backwards`() {
        assertThat(follow(openSurah = 19, recitedSurah = 18)).isNull()
    }

    @Test
    fun `ignores a finished session`() {
        // markPlaybackFinished clears isActive and zeroes the surah at the end of An-Nas.
        assertThat(follow(isAudioActive = false)).isNull()
        assertThat(follow(recitedSurah = 0)).isNull()
    }

    @Test
    fun `does nothing before the reader has a surah`() {
        assertThat(follow(openSurah = null)).isNull()
    }

    @Test
    fun `leaves juz and page mode alone`() {
        // A juz carries on inside content the reader already has; the mushaf pager is
        // paginated rather than navigated, so moving it is follow-along's page turn.
        assertThat(follow(readingMode = ReadingMode.JUZ)).isNull()
        assertThat(follow(readingMode = ReadingMode.PAGE)).isNull()
        assertThat(follow(inPageView = true)).isNull()
    }
}
