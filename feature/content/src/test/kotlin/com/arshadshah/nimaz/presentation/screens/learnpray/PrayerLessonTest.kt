package com.arshadshah.nimaz.presentation.screens.learnpray

import com.arshadshah.nimaz.presentation.viewmodel.learnpray.LearnPrayUiState
import com.arshadshah.nimaz.presentation.model.PrayerFigure
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PrayerLessonTest {
    @Test fun completeSequenceIncludesAllRepeatedMovements() {
        assertThat(PrayerLesson.steps.map { it.id }).containsExactly(
            "opening_takbir", "recitation_1", "bow_1", "rise_1", "prostrate_1a",
            "sit_1", "prostrate_1b", "recitation_2", "bow_2", "rise_2",
            "prostrate_2a", "sit_2", "prostrate_2b", "tashahhud", "salawat",
            "dua", "salam_right", "salam_left",
        ).inOrder()
        assertThat(PrayerLesson.steps).hasSize(LearnPrayUiState.STEP_COUNT)
        assertThat(PrayerLesson.steps.take(7).map { it.rakah }).containsExactlyElementsIn(List(7) { 1 })
        assertThat(PrayerLesson.steps.drop(7).map { it.rakah }).containsExactlyElementsIn(List(11) { 2 })
    }

    @Test fun everyStepHasWordsArtworkAndExplicitSources() {
        PrayerLesson.steps.forEach {
            assertThat(it.title).isNotEqualTo(0)
            assertThat(it.instruction).isNotEqualTo(0)
            assertThat(it.artwork).isNotEqualTo(0)
            assertThat(it.femaleArtwork).isNotEqualTo(0)
            assertThat(it.artworkFor(PrayerFigure.MAN)).isEqualTo(it.artwork)
            assertThat(it.artworkFor(PrayerFigure.WOMAN)).isEqualTo(it.femaleArtwork)
            assertThat(it.femaleArtwork).isNotEqualTo(it.artwork)
            assertThat(it.recitations).isNotEmpty()
            assertThat(it.references).isNotEmpty()
            it.references.forEach { url ->
                assertThat(url.startsWith("https://sunnah.com/") || url.startsWith("https://quran.com/")).isTrue()
            }
        }
    }
}
