package com.arshadshah.nimaz.presentation.viewmodel

import com.arshadshah.nimaz.domain.model.AiError
import com.arshadshah.nimaz.domain.model.AnswerConfidence
import com.arshadshah.nimaz.domain.model.GroundedAnswer
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.usecase.ai.AskWithProofUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AskViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val askWithProof = mockk<AskWithProofUseCase>()
    private val settings = mockk<SettingsRepository>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { settings.aiAskEnabled } returns flowOf(true)
        every { settings.aiAskHintDismissed } returns flowOf(false)
        every { settings.aiHistoryEnabled } returns flowOf(false)
        every { settings.aiQuestionHistory } returns flowOf("")
        every { settings.aiSourcesQuran } returns flowOf(true)
        every { settings.aiSourcesHadith } returns flowOf(true)
        every { settings.aiSourcesDua } returns flowOf(true)
        every { settings.aiMaxProofs } returns flowOf(5)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = AskViewModel(askWithProof, settings)

    @Test
    fun `reflects enabled state from settings`() = runTest {
        val vm = viewModel()
        assertThat(vm.uiState.value.aiEnabled).isTrue()
        assertThat(vm.uiState.value.phase).isEqualTo(AskPhase.Idle)
    }

    @Test
    fun `too-short question does not submit`() = runTest {
        val vm = viewModel()
        vm.onEvent(AskEvent.UpdateQuestion("hi"))
        vm.onEvent(AskEvent.Submit)
        assertThat(vm.uiState.value.phase).isEqualTo(AskPhase.Idle)
    }

    @Test
    fun `successful ask ends in Answer phase`() = runTest {
        coEvery { askWithProof.invoke(any(), any(), any()) } returns
            AskWithProofUseCase.Outcome.Answered(
                answer = GroundedAnswer(
                    answer = "Patience is virtuous.",
                    citationIds = emptyList(),
                    confidence = AnswerConfidence.HIGH,
                    insufficientEvidence = false,
                ),
                proofs = emptyList(),
            )

        val vm = viewModel()
        vm.onEvent(AskEvent.UpdateQuestion("What does the Quran say about patience?"))
        vm.onEvent(AskEvent.Submit)

        val phase = vm.uiState.value.phase
        assertThat(phase).isInstanceOf(AskPhase.Answer::class.java)
        assertThat((phase as AskPhase.Answer).answer.answer).isEqualTo("Patience is virtuous.")
        assertThat(vm.uiState.value.recentQuestions).contains(
            "What does the Quran say about patience?",
        )
    }

    @Test
    fun `no-evidence outcome ends in an insufficient-evidence Answer`() = runTest {
        coEvery { askWithProof.invoke(any(), any(), any()) } returns
            AskWithProofUseCase.Outcome.NoEvidence

        val vm = viewModel()
        vm.onEvent(AskEvent.UpdateQuestion("An unrelated question"))
        vm.onEvent(AskEvent.Submit)

        val phase = vm.uiState.value.phase as AskPhase.Answer
        assertThat(phase.answer.insufficientEvidence).isTrue()
        assertThat(phase.proofs).isEmpty()
    }

    @Test
    fun `failed outcome ends in Error phase`() = runTest {
        coEvery { askWithProof.invoke(any(), any(), any()) } returns
            AskWithProofUseCase.Outcome.Failed(AiError.Network)

        val vm = viewModel()
        vm.onEvent(AskEvent.UpdateQuestion("What is patience?"))
        vm.onEvent(AskEvent.Submit)

        val phase = vm.uiState.value.phase
        assertThat(phase).isInstanceOf(AskPhase.Error::class.java)
        assertThat((phase as AskPhase.Error).error).isEqualTo(AiError.Network)
    }
}
