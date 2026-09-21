package com.arshadshah.nimaz.presentation.screens.learnpray

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.presentation.viewmodel.learnpray.*
import com.arshadshah.nimaz.presentation.model.PrayerFigure
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

    @Test fun eachMovementRendersItsOwnHeadingAndRecitation() {
        val state = mutableStateOf(LearnPrayUiState(0))
        composeRule.setThemedContent {
            LearnToPrayContent(state.value, {}, {})
        }
        PrayerFigure.entries.forEach { figure ->
            PrayerLesson.steps.forEachIndexed { index, step ->
                composeRule.runOnIdle { state.value = LearnPrayUiState(index, figure) }
                composeRule.onNodeWithText(string(step.title)).assertExists()
                composeRule.onNodeWithText(string(step.recitations.first().arabic)).assertExists()
            }
        }
    }

    @Test fun nextAndPreviousDispatchWithoutLeavingTheLesson() {
        val events = mutableListOf<LearnPrayEvent>()
        var exited = false
        composeRule.setThemedContent {
            LearnToPrayContent(LearnPrayUiState(0), events::add, { exited = true })
        }
        composeRule.onNodeWithText(string(R.string.learn_pray_next)).performClick()
        composeRule.onNodeWithText(string(R.string.learn_pray_back)).performClick()
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
}
