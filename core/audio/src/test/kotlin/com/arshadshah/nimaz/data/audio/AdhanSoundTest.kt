package com.arshadshah.nimaz.data.audio

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The adhan catalogue.
 *
 * The Fajr adhan carries a phrase the others do not ("الصلاة خير من النوم"), so every sound has
 * two files and two URLs — and every caller passes an `isFajr` flag through. Getting that flag's
 * sense backwards is not an error: it plays the wrong recording at the wrong prayer, which is
 * the one defect the URL version counter in [AdhanAudioManager] exists to undo (v2 exists
 * because Mishary's *regular* URL was serving the Fajr variant).
 */
class AdhanSoundTest {

    @Test
    fun `each sound resolves a different file for Fajr`() {
        AdhanSound.entries
            .filter { it != AdhanSound.SIMPLE_BEEP }
            .forEach { sound ->
                assertThat(sound.getFileName(isFajr = true)).isEqualTo(sound.fajrFileName)
                assertThat(sound.getFileName(isFajr = false)).isEqualTo(sound.fileName)
                assertThat(sound.getFileName(true)).isNotEqualTo(sound.getFileName(false))
            }
    }

    @Test
    fun `the beep is the same file at every prayer`() {
        // It is generated, not recorded, so there is no Fajr phrase to add.
        assertThat(AdhanSound.SIMPLE_BEEP.getFileName(isFajr = true))
            .isEqualTo(AdhanSound.SIMPLE_BEEP.getFileName(isFajr = false))
    }

    @Test
    fun `every recorded sound has both URLs and the beep has neither`() {
        AdhanSound.entries
            .filter { it != AdhanSound.SIMPLE_BEEP }
            .forEach { sound ->
                assertThat(sound.getDownloadUrl(isFajr = false)).isEqualTo(sound.downloadUrl)
                assertThat(sound.getDownloadUrl(isFajr = true)).isEqualTo(sound.fajrDownloadUrl)
                assertThat(sound.getDownloadUrl(false)).isNotEmpty()
                assertThat(sound.getDownloadUrl(true)).isNotEmpty()
            }

        assertThat(AdhanSound.SIMPLE_BEEP.getDownloadUrl(false)).isEmpty()
        assertThat(AdhanSound.SIMPLE_BEEP.getDownloadUrl(true)).isEmpty()
    }

    @Test
    fun `no two sounds share a file name`() {
        // Two sounds writing to one file means downloading one silently replaces the other.
        val names = AdhanSound.entries.map { it.fileName }
        assertThat(names).containsNoDuplicates()
    }

    @Test
    fun `a stored preference that no longer names a sound falls back rather than crashing`() {
        assertThat(AdhanSound.fromName("A_SOUND_THAT_WAS_REMOVED")).isEqualTo(AdhanSound.MISHARY)
        assertThat(AdhanSound.fromName("")).isEqualTo(AdhanSound.MISHARY)
    }

    @Test
    fun `a stored preference that names a sound round trips`() {
        AdhanSound.entries.forEach { sound ->
            assertThat(AdhanSound.fromName(sound.name)).isEqualTo(sound)
        }
    }
}
