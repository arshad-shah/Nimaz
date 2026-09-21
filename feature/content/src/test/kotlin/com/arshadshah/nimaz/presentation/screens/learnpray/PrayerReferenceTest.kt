package com.arshadshah.nimaz.presentation.screens.learnpray

import com.arshadshah.nimaz.domain.model.ContentTarget
import com.arshadshah.nimaz.presentation.model.PrayerFigure
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PrayerReferenceTest {
    @Test fun everyLessonCorpusCitationOpensAnInternalTarget() {
        PrayerFigure.entries.forEach { figure ->
            PrayerLesson.steps.flatMap { it.referencesFor(figure) }.forEach { url ->
                val reference = PrayerReference.fromUrl(url)
                if (url.startsWith("https://sunnah.com/") || url.startsWith("https://quran.com/")) {
                    assertThat(reference.target).isNotNull()
                }
            }
        }
    }

    @Test fun quranReferencesPreserveTheAyahAndWholeSurahsStartAtOne() {
        assertThat(PrayerReference.fromUrl("https://quran.com/2/144").target)
            .isEqualTo(ContentTarget.Ayah(2, 144))
        assertThat(PrayerReference.fromUrl("https://quran.com/112").target)
            .isEqualTo(ContentTarget.Ayah(112, 1))
    }

    @Test fun muslimNumberingAndSuffixesResolveToTheCorrectCorpusRecords() {
        mapOf("402a" to "8486", "580b" to "8899", "588a" to "8913",
            "772" to "9403", "582" to "8904").forEach { (number, id) ->
            val reference = PrayerReference.fromUrl("https://sunnah.com/muslim:$number")
            assertThat(reference.target).isEqualTo(ContentTarget.Hadith(id))
            assertThat(reference.number).isEqualTo(number)
        }
    }
}
