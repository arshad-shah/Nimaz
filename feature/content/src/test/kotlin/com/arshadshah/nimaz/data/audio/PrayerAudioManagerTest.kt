package com.arshadshah.nimaz.data.audio

import com.arshadshah.nimaz.domain.model.PrayerAudio
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PrayerAudioManagerTest {
    @Test fun allClipsUseHttpsFromTheExplicitProviderCatalogue() {
        PrayerAudio.entries.forEach { clip ->
            val urls = PrayerAudioManager.urls(clip)
            assertThat(urls).isNotEmpty()
            urls.forEach { url ->
                assertThat(url.startsWith("https://everyayah.com/data/Husary_128kbps/") ||
                    url.startsWith("https://www.hisnmuslim.com/audio/ar/")).isTrue()
            }
        }
    }

    @Test fun fatihahContainsAllSevenAyahsOnceInOrder() {
        assertThat(PrayerAudioManager.urls(PrayerAudio.FATIHAH).map { it.substringAfterLast("/") })
            .containsExactly("001001.mp3", "001002.mp3", "001003.mp3", "001004.mp3",
                "001005.mp3", "001006.mp3", "001007.mp3").inOrder()
    }

    @Test fun ikhlasIncludesBasmalahThenFourAyahs() {
        assertThat(PrayerAudioManager.urls(PrayerAudio.IKHLAS).map { it.substringAfterLast("/") })
            .containsExactly("001001.mp3", "112001.mp3", "112002.mp3", "112003.mp3", "112004.mp3").inOrder()
    }

    @Test fun hisnIdsAreMappedToTheMatchingPublishedPhrases() {
        listOf(PrayerAudio.RUKU to 33, PrayerAudio.SUJUD to 41, PrayerAudio.SITTING to 48,
            PrayerAudio.TASHAHHUD to 52, PrayerAudio.SALAWAT to 53).forEach { (clip, id) ->
            assertThat(PrayerAudioManager.urls(clip)).containsExactly("https://www.hisnmuslim.com/audio/ar/$id.mp3")
        }
    }
}
