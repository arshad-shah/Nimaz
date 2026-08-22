package com.arshadshah.nimaz.presentation.viewmodel.ai

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.model.AnswerConfidence
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.usecase.ai.AskWithProofUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * What it costs to tap "Ask" twice.
 *
 * Every submit is one Cloudflare Worker invocation, billed. `submit()` validated only the
 * question's length: no in-flight guard, and no check that the feature the user has to opt
 * into is actually on. `docs/ai-ask-with-proof.md` states the design as "one Worker call per
 * question"; the ViewModel did not enforce it, and the retry button made a second tap the
 * natural reaction to a slow network.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AskViewModelGuardTest {

    private val dispatcher = StandardTestDispatcher()
    private val askWithProof = mockk<AskWithProofUseCase>()
    private val settings = mockk<SettingsRepository>(relaxed = true)
    private val telemetry = RecordingTelemetry()

    private val aiEnabled = MutableStateFlow(true)
    private val historyEnabled = MutableStateFlow(true)
    private val questionHistory = MutableStateFlow("""["an older question"]""")

    private val question = "What does the Quran say about patience?"

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { settings.aiAskEnabled } returns aiEnabled
        every { settings.aiAskHintDismissed } returns flowOf(false)
        every { settings.aiHistoryEnabled } returns historyEnabled
        every { settings.aiQuestionHistory } returns questionHistory

        coEvery { askWithProof.invoke(any()) } coAnswers {
            delay(500)
            AskWithProofUseCase.Outcome.Answered(
                answer = "Patience is virtuous.",
                confidence = AnswerConfidence.HIGH,
                proofs = emptyList(),
                relatedTerms = emptyList(),
            )
        }
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = AskViewModel(askWithProof, settings, telemetry)

    @Test
    fun `a second tap while the first is in flight does not bill a second worker call`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(AskEvent.UpdateQuestion(question))
        vm.onEvent(AskEvent.Submit)
        advanceTimeBy(50)          // the first call is still in flight
        vm.onEvent(AskEvent.Submit)
        advanceUntilIdle()

        coVerify(exactly = 1) { askWithProof.invoke(question) }
    }

    @Test
    fun `retrying a recent question while one is in flight does not bill twice either`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(AskEvent.UpdateQuestion(question))
        vm.onEvent(AskEvent.Submit)
        advanceTimeBy(50)
        vm.onEvent(AskEvent.SelectRecent("an older question"))
        advanceUntilIdle()

        coVerify(exactly = 1) { askWithProof.invoke(any()) }
    }

    @Test
    fun `a question is never sent while the feature is switched off`() = runTest {
        aiEnabled.value = false

        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(AskEvent.UpdateQuestion(question))
        vm.onEvent(AskEvent.Submit)
        advanceUntilIdle()

        coVerify(exactly = 0) { askWithProof.invoke(any()) }
        assertThat(vm.uiState.value.phase).isEqualTo(AskPhase.Idle)
    }

    @Test
    fun `turning history off takes the loaded questions off the screen`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        assertThat(vm.uiState.value.recentQuestions).containsExactly("an older question")

        // "Remember my questions" switched off in Search settings: the stored history is
        // cleared there, and what is already on screen must go with it.
        historyEnabled.value = false
        questionHistory.value = ""
        advanceUntilIdle()

        assertThat(vm.uiState.value.recentQuestions).isEmpty()
    }

    @Test
    fun `turning history back on restores what was persisted`() = runTest {
        historyEnabled.value = false
        questionHistory.value = ""
        val vm = viewModel()
        advanceUntilIdle()
        assertThat(vm.uiState.value.recentQuestions).isEmpty()

        historyEnabled.value = true
        questionHistory.value = """["restored question"]"""
        advanceUntilIdle()

        assertThat(vm.uiState.value.recentQuestions).containsExactly("restored question")
    }
}
