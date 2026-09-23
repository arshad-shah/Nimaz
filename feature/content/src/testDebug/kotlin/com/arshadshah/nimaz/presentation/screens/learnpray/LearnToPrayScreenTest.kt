package com.arshadshah.nimaz.presentation.screens.learnpray

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.viewmodel.learnpray.*
import com.arshadshah.nimaz.presentation.model.PrayerFigure
import com.arshadshah.nimaz.domain.model.PrayerAudio
import com.arshadshah.nimaz.domain.model.PrayerAudioState
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LearnToPrayScreenTest {
    @get:Rule val composeRule = createComponentComposeRule()
    private fun string(id: Int) = ApplicationProvider.getApplicationContext<Context>().getString(id)
    private fun moreLabel(others: Int) =
        ApplicationProvider.getApplicationContext<Context>().getString(R.string.learn_pray_more_recitations, others)

    @Test fun preparationReferenceOpensTheInternalVerseWithoutAdvancing() {
        val targets = mutableListOf<com.arshadshah.nimaz.domain.model.ContentTarget>()
        val events = mutableListOf<LearnPrayEvent>()
        composeRule.setThemedContent {
            LearnToPrayContent(LearnPrayUiState(), events::add, {}, onOpenReference = targets::add)
        }
        composeRule.onNodeWithText(string(R.string.learn_pray_sources)).performScrollTo().performClick()
        val label = ApplicationProvider.getApplicationContext<Context>()
            .getString(R.string.learn_pray_reference_quran, "5:6")
        composeRule.onNodeWithText(label).performScrollTo().performClick()
        assertThat(targets).containsExactly(com.arshadshah.nimaz.domain.model.ContentTarget.Ayah(5, 6))
        assertThat(events).isEmpty()
    }

    @Test fun femaleIllustrationControlDispatchesAnExplicitChoice() {
        val events = mutableListOf<LearnPrayEvent>()
        composeRule.setThemedContent {
            LearnToPrayContent(LearnPrayUiState(), events::add, {})
        }
        composeRule.onNodeWithText(string(R.string.learn_pray_woman)).performClick()
        assertThat(events).containsExactly(LearnPrayEvent.SelectFigure(PrayerFigure.WOMAN))
    }

    @Test fun preparationIsExplicitAndBeginDispatchesNext() {
        val events = mutableListOf<LearnPrayEvent>()
        composeRule.setThemedContent {
            LearnToPrayContent(LearnPrayUiState(), events::add, {})
        }
        composeRule.onNodeWithText(string(R.string.learn_pray_prepare)).assertExists()
        composeRule.onNodeWithText(string(R.string.learn_pray_start)).performClick()
        assertThat(events).containsExactly(LearnPrayEvent.Next)
    }

    @Test fun eachMovementRendersItsOwnHeadingAndMainRecitation() {
        // The pinned card shows the step's main recitation — the first with audio, so the standing
        // step leads with Al-Fatihah rather than the optional opening — by its first line.
        val state = mutableStateOf(LearnPrayUiState(0))
        composeRule.setThemedContent {
            LearnToPrayContent(state.value, {}, {})
        }
        PrayerFigure.entries.forEach { figure ->
            PrayerLesson.steps.forEachIndexed { index, step ->
                composeRule.runOnIdle { state.value = LearnPrayUiState(index, figure) }
                composeRule.onNodeWithText(string(step.title)).assertExists()
                val main = step.recitations.firstOrNull { it.audio != null } ?: step.recitations.first()
                composeRule.onNodeWithText(string(main.arabic).lineSequence().first()).assertExists()
            }
        }
    }

    @Test fun whatToSayOpensEveryRecitationOfTheStep() {
        val standing = PrayerLesson.steps.indexOfFirst { it.recitations.size > 1 }
        composeRule.setThemedContent {
            LearnToPrayContent(LearnPrayUiState(standing), {}, {})
        }
        composeRule.onNodeWithText(moreLabel(PrayerLesson.steps[standing].recitations.size - 1)).performClick()
        composeRule.waitForIdle()
        PrayerLesson.steps[standing].recitations.forEach { recitation ->
            composeRule.onNodeWithText(string(recitation.title)).performScrollTo().assertExists()
        }
    }

    @Test fun nextAndPreviousDispatchWithoutLeavingTheLesson() {
        val events = mutableListOf<LearnPrayEvent>()
        var exited = false
        composeRule.setThemedContent {
            LearnToPrayContent(LearnPrayUiState(0), events::add, { exited = true })
        }
        composeRule.onNodeWithText(string(R.string.learn_pray_next)).performClick()
        composeRule.onNodeWithContentDescription(string(R.string.learn_pray_back)).performClick()
        assertThat(events).containsExactly(LearnPrayEvent.Next, LearnPrayEvent.Previous).inOrder()
        assertThat(exited).isFalse()
    }

    @Test fun completionOffersExitAndPracticeAgain() {
        val events = mutableListOf<LearnPrayEvent>()
        var exited = false
        composeRule.setThemedContent {
            LearnToPrayContent(LearnPrayUiState(LearnPrayUiState.STEP_COUNT), events::add, { exited = true })
        }
        composeRule.onNodeWithText(string(R.string.learn_pray_complete)).assertExists()
        composeRule.onNodeWithText(string(R.string.learn_pray_restart)).performClick()
        assertThat(events).containsExactly(LearnPrayEvent.Restart)
        composeRule.onNodeWithText(string(R.string.learn_pray_done)).performClick()
        assertThat(exited).isTrue()
    }

    private val bowing get() = PrayerLesson.steps.indexOfFirst { step -> step.recitations.any { it.audio == PrayerAudio.RUKU } }

    @Test fun listenOnTheCardTogglesTheStepsMainRecitation() {
        val events = mutableListOf<LearnPrayEvent>()
        composeRule.setThemedContent {
            LearnToPrayContent(LearnPrayUiState(bowing), events::add, {})
        }
        composeRule.onNodeWithText(string(R.string.learn_pray_audio_listen)).performClick()
        assertThat(events).containsExactly(LearnPrayEvent.ToggleAudio(PrayerAudio.RUKU))
    }

    @Test fun playingAudioOffersStopAndShowsLoading() {
        composeRule.setThemedContent {
            LearnToPrayContent(LearnPrayUiState(bowing), {}, {},
                audioState = PrayerAudioState(current = PrayerAudio.RUKU, loading = true))
        }
        composeRule.onNodeWithText(string(R.string.learn_pray_audio_stop)).assertExists()
        composeRule.onAllNodes(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).onFirst().assertExists()
    }

    @Test fun aSingleRecitationPreviewsItsPronunciationInsteadOfACount() {
        composeRule.setThemedContent {
            LearnToPrayContent(LearnPrayUiState(bowing), {}, {})
        }
        val ruku = PrayerLesson.steps[bowing].recitations.single()
        composeRule.onNodeWithText(string(ruku.transliteration).lineSequence().first()).assertExists()
    }

    @Test fun theSheetCarriesAudioNotesFailuresAndUnrecordedRecitations() {
        // The standing step mixes recorded Qur'an (Al-Fatihah) with recitations that have no
        // recording yet (the optional opening), so one sheet exercises every audio state.
        val standing = PrayerLesson.steps.indexOfFirst { it.recitations.size > 1 }
        composeRule.setThemedContent {
            LearnToPrayContent(LearnPrayUiState(standing), {}, {},
                audioState = PrayerAudioState(current = PrayerAudio.FATIHAH, loading = true, failed = true))
        }
        composeRule.onNodeWithText(moreLabel(PrayerLesson.steps[standing].recitations.size - 1)).performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText(string(R.string.learn_pray_audio_error)).onFirst().performScrollTo().assertExists()
        composeRule.onAllNodesWithText(string(R.string.learn_pray_audio_quran)).onFirst().performScrollTo().assertExists()
        composeRule.onAllNodesWithText(string(R.string.learn_pray_audio_pending)).onFirst().performScrollTo().assertExists()
    }

    @Test fun theSheetsHisnNoteAndSourcesAreReachable() {
        val targets = mutableListOf<com.arshadshah.nimaz.domain.model.ContentTarget>()
        composeRule.setThemedContent {
            LearnToPrayContent(LearnPrayUiState(bowing, PrayerFigure.WOMAN), {}, {}, onOpenReference = targets::add)
        }
        composeRule.onNodeWithText(string(R.string.learn_pray_recite)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(string(R.string.learn_pray_audio_hisn)).performScrollTo().assertExists()
        composeRule.onNodeWithText(string(R.string.learn_pray_sources)).performScrollTo().performClick()
        composeRule.onNodeWithText(string(R.string.learn_pray_source_intro)).performScrollTo().assertExists()
    }

    @Test fun completionShowsTheFinalPostureWithoutTheFigureChoice() {
        composeRule.setThemedContent {
            LearnToPrayContent(LearnPrayUiState(LearnPrayUiState.STEP_COUNT, PrayerFigure.WOMAN), {}, {})
        }
        composeRule.onAllNodesWithText(string(R.string.learn_pray_woman)).assertCountEquals(0)
        composeRule.onNodeWithContentDescription(string(R.string.learn_pray_back)).assertDoesNotExist()
    }

    @Test fun theLastStepFinishesTheLesson() {
        val events = mutableListOf<LearnPrayEvent>()
        composeRule.setThemedContent {
            LearnToPrayContent(LearnPrayUiState(LearnPrayUiState.STEP_COUNT - 1), events::add, {})
        }
        composeRule.onNodeWithText(string(R.string.learn_pray_finish)).performClick()
        assertThat(events).containsExactly(LearnPrayEvent.Next)
    }

    @Test fun clothingNotesFollowTheChosenFigureAndOnlyTheWomansCiteTheirSource() {
        val state = mutableStateOf(LearnPrayUiState(figure = PrayerFigure.MAN))
        composeRule.setThemedContent {
            LearnToPrayContent(state.value, {}, {})
        }
        composeRule.onNodeWithText(string(R.string.learn_pray_clothing)).performScrollTo().performClick()
        composeRule.onNodeWithText(string(R.string.learn_pray_male_clothing)).performScrollTo().assertExists()
        composeRule.onNodeWithText(string(R.string.learn_pray_differences)).performScrollTo().assertExists()
        composeRule.onAllNodesWithText(string(R.string.learn_pray_female_source)).assertCountEquals(0)

        composeRule.runOnIdle { state.value = LearnPrayUiState(figure = PrayerFigure.WOMAN) }
        composeRule.onNodeWithText(string(R.string.learn_pray_female_clothing)).performScrollTo().assertExists()
        composeRule.onNodeWithText(string(R.string.learn_pray_female_source)).performScrollTo().assertExists()

        // Collapsing hides the notes again.
        composeRule.onNodeWithText(string(R.string.learn_pray_clothing)).performScrollTo().performClick()
        composeRule.onAllNodesWithText(string(R.string.learn_pray_female_clothing)).assertCountEquals(0)
    }
}
