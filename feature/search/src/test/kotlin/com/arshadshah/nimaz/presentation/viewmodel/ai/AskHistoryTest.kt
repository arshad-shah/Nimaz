package com.arshadshah.nimaz.presentation.viewmodel.ai

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.model.AiError
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * What "Ask with Proof" remembers, and what it reports about failing.
 *
 * The question history is the only user content this feature stores, and it is opt-in twice over:
 * the feature has to be on, and "remember my questions" has to be on as well. Both switches gate
 * the *write*, not merely the display — a question kept on disk after someone turned remembering
 * off is a promise broken quietly, and nothing on screen would show it.
 *
 * The stored form is JSON in a preference, which means it can be malformed: written by a newer
 * build, truncated, restored from another device. Decoding it must degrade to an empty list rather
 * than throw, because the throw would happen while composing the resting search screen — the
 * feature's front door — and take the whole screen down with it.
 *
 * The error slugs are the operational half. Every one of them is a distinct thing going wrong with
 * a paid Worker call, and they are indistinguishable in aggregate if the ViewModel reports them
 * under one name.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AskHistoryTest {

    private val dispatcher = StandardTestDispatcher()
    private val askWithProof = mockk<AskWithProofUseCase>()
    private val settings = mockk<SettingsRepository>(relaxed = true)
    private val telemetry = RecordingTelemetry()

    private val aiEnabled = MutableStateFlow(true)
    private val historyEnabled = MutableStateFlow(true)
    private val questionHistory = MutableStateFlow("[]")

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { settings.aiAskEnabled } returns aiEnabled
        every { settings.aiAskHintDismissed } returns flowOf(false)
        every { settings.aiHistoryEnabled } returns historyEnabled
        every { settings.aiQuestionHistory } returns questionHistory
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = AskViewModel(askWithProof, settings, telemetry)

    private fun answered(answer: String = "Patience is virtuous.") =
        AskWithProofUseCase.Outcome.Answered(
            answer = answer,
            confidence = AnswerConfidence.HIGH,
            proofs = emptyList(),
            relatedTerms = emptyList(),
        )

    // ── what is written down ─────────────────────────────────────────────────

    @Test
    fun `an answered question is persisted when remembering is on`() = runTest {
        coEvery { askWithProof.invoke(any()) } returns answered()
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(AskEvent.UpdateQuestion("What does the Quran say about patience?"))
        vm.onEvent(AskEvent.Submit)
        advanceUntilIdle()

        coVerify {
            settings.setAiQuestionHistory(
                match { it.contains("What does the Quran say about patience?") },
            )
        }
    }

    /** "Remember my questions", off, has to stop the write — not merely hide the list. */
    @Test
    fun `nothing is written down while remembering is off`() = runTest {
        historyEnabled.value = false
        coEvery { askWithProof.invoke(any()) } returns answered()
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(AskEvent.UpdateQuestion("What does the Quran say about patience?"))
        vm.onEvent(AskEvent.Submit)
        advanceUntilIdle()

        coVerify(exactly = 0) { settings.setAiQuestionHistory(any()) }
    }

    /** A question asked twice is one entry, moved to the top — not two identical rows. */
    @Test
    fun `re-asking a remembered question does not duplicate it`() = runTest {
        questionHistory.value = """["older question","What is patience?"]"""
        coEvery { askWithProof.invoke(any()) } returns answered()
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(AskEvent.SelectRecent("What is patience?"))
        advanceUntilIdle()

        val recents = vm.uiState.value.recentQuestions
        assertThat(recents).containsExactly("What is patience?", "older question").inOrder()
    }

    @Test
    fun `the remembered list stops at ten questions`() = runTest {
        questionHistory.value = (1..10).joinToString(",", "[", "]") { "\"question $it\"" }
        coEvery { askWithProof.invoke(any()) } returns answered()
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(AskEvent.UpdateQuestion("a brand new question"))
        vm.onEvent(AskEvent.Submit)
        advanceUntilIdle()

        val recents = vm.uiState.value.recentQuestions
        assertThat(recents).hasSize(10)
        assertThat(recents.first()).isEqualTo("a brand new question")
        assertThat(recents).doesNotContain("question 10")
    }

    /**
     * A preference file outlives the build that wrote it. Malformed JSON here would throw while
     * the resting search screen is composing, so it degrades to "no history" instead.
     */
    @Test
    fun `unreadable stored history degrades to no history`() = runTest {
        questionHistory.value = "{not json at all"
        val vm = viewModel()
        advanceUntilIdle()

        assertThat(vm.uiState.value.recentQuestions).isEmpty()
    }

    @Test
    fun `dismissing the discovery hint is remembered`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(AskEvent.DismissHint)
        advanceUntilIdle()

        coVerify { settings.setAiAskHintDismissed(true) }
    }

    // ── what is reported ─────────────────────────────────────────────────────

    /**
     * Seven ways a billed call can fail, seven slugs. Reported under one name they are one
     * undifferentiated error rate, and the two that matter operationally — the budget cap and a
     * failing integrity check — are exactly the two that look like everything else in aggregate.
     */
    @Test
    fun `each kind of failure is reported under its own name`() = runTest {
        val cases = mapOf(
            AiError.RateLimited(retryAfterSeconds = 60) to "ask_rate_limited",
            AiError.BudgetExceeded to "ask_budget",
            AiError.Network to "ask_network",
            AiError.Invalid("too long") to "ask_invalid",
            AiError.Unverified to "ask_unverified",
            AiError.ConsentRequired to "ask_consent_required",
            AiError.Unknown to "ask_unknown",
        )

        cases.forEach { (error, slug) ->
            telemetry.clear()
            coEvery { askWithProof.invoke(any()) } returns
                AskWithProofUseCase.Outcome.Failed(error)
            val vm = viewModel()
            advanceUntilIdle()

            vm.onEvent(AskEvent.UpdateQuestion("What is patience?"))
            vm.onEvent(AskEvent.Submit)
            advanceUntilIdle()

            assertThat(telemetry.errors.map { it.type }).containsExactly(slug)
            assertThat(vm.uiState.value.phase).isEqualTo(AskPhase.Error(error))
        }
    }

    /**
     * The proof count is the silent-degradation signal for the whole feature: an answer whose
     * citations resolved to nothing still renders, and still reads as a working answer.
     */
    @Test
    fun `an answer reports how many citations actually resolved`() = runTest {
        coEvery { askWithProof.invoke(any()) } returns answered()
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(AskEvent.UpdateQuestion("What is patience?"))
        vm.onEvent(AskEvent.Submit)
        advanceUntilIdle()

        val recorded = telemetry.aiAnswers.single()
        assertThat(recorded.proofCount).isEqualTo(0)
        assertThat(recorded.confidence).isEqualTo(AnswerConfidence.HIGH.name)
    }

    /** The question text is the one thing that must never reach analytics. */
    @Test
    fun `the question itself is never recorded`() = runTest {
        coEvery { askWithProof.invoke(any()) } returns answered()
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(AskEvent.UpdateQuestion("Is my specific personal situation permissible?"))
        vm.onEvent(AskEvent.Submit)
        advanceUntilIdle()

        assertThat(telemetry.calls.toString())
            .doesNotContain("Is my specific personal situation permissible?")
    }

    /**
     * A crash inside the use case is still a failure the screen has to leave: without the
     * `onFailure` hand-off the phase stays Loading forever and the card spins for good.
     */
    @Test
    fun `a thrown failure still takes the screen out of the loading state`() = runTest {
        coEvery { askWithProof.invoke(any()) } throws IllegalStateException("boom")
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(AskEvent.UpdateQuestion("What is patience?"))
        vm.onEvent(AskEvent.Submit)
        advanceUntilIdle()

        assertThat(vm.uiState.value.phase).isEqualTo(AskPhase.Error(AiError.Unknown))
    }
}
