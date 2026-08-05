package com.arshadshah.nimaz.domain.usecase.ai

import com.arshadshah.nimaz.domain.model.AiError
import com.arshadshah.nimaz.domain.repository.AiRepository
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.usecase.HadithUseCases
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Consent for "Ask with Proof" is enforced below the UI layer.
 *
 * `aiAskEnabled` used to be checked in exactly one place — a visibility condition in
 * `SearchScreen`. Nothing in `AskViewModel`, this use case, `AiRepository` or the Worker
 * client re-checked it, so any future caller of `AskEvent.Submit` would have sent the
 * question off the device with the feature switched off. For an opt-in privacy control,
 * one UI site is not where the guarantee belongs.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AskWithProofConsentTest {

    private val aiRepository = mockk<AiRepository>(relaxed = true)
    private val quranUseCases = mockk<QuranUseCases>(relaxed = true)
    private val hadithUseCases = mockk<HadithUseCases>(relaxed = true)
    private val settings = mockk<SettingsRepository>(relaxed = true)

    private fun useCase() =
        AskWithProofUseCase(aiRepository, quranUseCases, hadithUseCases, settings)

    @Test
    fun `the question never leaves the device when the feature is off`() = runTest {
        every { settings.aiAskEnabled } returns flowOf(false)

        val outcome = useCase().invoke("What does the Quran say about patience?")

        coVerify(exactly = 0) { aiRepository.assist(any()) }
        assertThat(outcome)
            .isEqualTo(AskWithProofUseCase.Outcome.Failed(AiError.ConsentRequired))
    }

    @Test
    fun `with consent given the question reaches the worker`() = runTest {
        every { settings.aiAskEnabled } returns flowOf(true)
        // What the Worker answers is AskWithProofUseCaseTest's subject; here the only
        // question is whether the call was made at all.
        coEvery { aiRepository.assist(any()) } returns Result.failure(RuntimeException("boom"))

        useCase().invoke("What does the Quran say about patience?")

        coVerify(exactly = 1) { aiRepository.assist(any()) }
    }
}
