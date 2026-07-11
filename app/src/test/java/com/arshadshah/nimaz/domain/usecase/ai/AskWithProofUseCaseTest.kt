package com.arshadshah.nimaz.domain.usecase.ai

import com.arshadshah.nimaz.core.navigation.Route
import com.arshadshah.nimaz.domain.model.AiError
import com.arshadshah.nimaz.domain.model.AnswerConfidence
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.CitationId
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.SearchAssist
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.repository.AiRepository
import com.arshadshah.nimaz.domain.repository.AiRequestException
import com.arshadshah.nimaz.domain.usecase.GetAyahsBySurahUseCase
import com.arshadshah.nimaz.domain.usecase.GetSurahByNumberUseCase
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class AskWithProofUseCaseTest {

    private val aiRepository = mockk<AiRepository>()
    private val getAyahsBySurahUC = mockk<GetAyahsBySurahUseCase>()
    private val getSurahByNumberUC = mockk<GetSurahByNumberUseCase>()
    private val quranUseCases = mockk<QuranUseCases>()

    private lateinit var useCase: AskWithProofUseCase

    @Before
    fun setUp() {
        every { quranUseCases.getAyahsBySurah } returns getAyahsBySurahUC
        every { quranUseCases.getSurahByNumber } returns getSurahByNumberUC
        useCase = AskWithProofUseCase(aiRepository, quranUseCases)
    }

    @Test
    fun `cited refs resolve to real local records with deep links`() = runTest {
        coEvery { aiRepository.assist(any()) } returns Result.success(
            assist(
                refs = listOf(CitationId.Quran(2, 153), CitationId.Quran(39, 10)),
                terms = listOf("patience", "sabr"),
            ),
        )
        every { getAyahsBySurahUC.invoke(2) } returns flowOf(listOf(ayah(2, 153)))
        every { getAyahsBySurahUC.invoke(39) } returns flowOf(listOf(ayah(39, 10)))
        coEvery { getSurahByNumberUC.invoke(2) } returns surah(2)
        coEvery { getSurahByNumberUC.invoke(39) } returns surah(39)

        val outcome = useCase("What does the Quran say about patience?")

        val answered = outcome as AskWithProofUseCase.Outcome.Answered
        assertThat(answered.answer).isEqualTo("Patience is encouraged.")
        assertThat(answered.confidence).isEqualTo(AnswerConfidence.HIGH)
        assertThat(answered.relatedTerms).containsExactly("patience", "sabr")
        assertThat(answered.proofs.map { it.citationId })
            .containsExactly("quran:2:153", "quran:39:10")
            .inOrder()
        assertThat(answered.proofs.first().route).isEqualTo(Route.QuranReader(2, 153))
        // The proof shows the real local translation, not model text.
        assertThat(answered.proofs.first().displayText).isEqualTo("Be patient.")
    }

    @Test
    fun `refs that do not resolve locally are dropped, answer still returned`() = runTest {
        coEvery { aiRepository.assist(any()) } returns Result.success(
            assist(
                refs = listOf(
                    CitationId.Quran(2, 153), // resolves
                    CitationId.Quran(2, 999), // ayah out of range -> dropped
                    CitationId.Quran(99, 1), // surah has no rows -> dropped
                ),
            ),
        )
        every { getAyahsBySurahUC.invoke(2) } returns flowOf(listOf(ayah(2, 153)))
        every { getAyahsBySurahUC.invoke(99) } returns flowOf(emptyList())
        coEvery { getSurahByNumberUC.invoke(any()) } returns surah(2)

        val outcome = useCase("q?") as AskWithProofUseCase.Outcome.Answered

        assertThat(outcome.proofs.map { it.citationId }).containsExactly("quran:2:153")
    }

    @Test
    fun `an answer with no citations is still an answer, never a dead end`() = runTest {
        coEvery { aiRepository.assist(any()) } returns Result.success(
            assist(refs = emptyList(), terms = listOf("charity")),
        )

        val outcome = useCase("q?") as AskWithProofUseCase.Outcome.Answered

        assertThat(outcome.proofs).isEmpty()
        assertThat(outcome.answer).isNotEmpty()
        assertThat(outcome.relatedTerms).containsExactly("charity")
    }

    @Test
    fun `repository failure surfaces the mapped AiError`() = runTest {
        coEvery { aiRepository.assist(any()) } returns
            Result.failure(AiRequestException(AiError.RateLimited(60)))

        val outcome = useCase("q?")

        assertThat(outcome).isEqualTo(
            AskWithProofUseCase.Outcome.Failed(AiError.RateLimited(60)),
        )
    }

    @Test
    fun `unexpected exception maps to Unknown`() = runTest {
        coEvery { aiRepository.assist(any()) } returns
            Result.failure(IllegalStateException("boom"))

        val outcome = useCase("q?")

        assertThat(outcome).isEqualTo(AskWithProofUseCase.Outcome.Failed(AiError.Unknown))
    }

    @Test
    fun `proofs are capped at MAX_PROOFS`() = runTest {
        val refs = (1..12).map { CitationId.Quran(2, it) }
        coEvery { aiRepository.assist(any()) } returns Result.success(assist(refs = refs))
        every { getAyahsBySurahUC.invoke(2) } returns
            flowOf((1..12).map { ayah(2, it) })
        coEvery { getSurahByNumberUC.invoke(2) } returns surah(2)

        val outcome = useCase("q?") as AskWithProofUseCase.Outcome.Answered

        assertThat(outcome.proofs).hasSize(AskWithProofUseCase.MAX_PROOFS)
    }

    // ── model builders ────────────────────────────────────────────────────────

    private fun assist(
        refs: List<CitationId.Quran>,
        terms: List<String> = listOf("patience"),
    ) = SearchAssist(
        answer = "Patience is encouraged.",
        quranRefs = refs,
        terms = terms,
        confidence = AnswerConfidence.HIGH,
    )

    private fun ayah(surah: Int, ayahNumber: Int, translation: String = "Be patient.") = Ayah(
        id = surah * 1000 + ayahNumber,
        surahNumber = surah,
        ayahNumber = ayahNumber,
        textArabic = "arabic",
        textSimple = "simple",
        juzNumber = 1,
        hizbNumber = 1,
        rubNumber = 1,
        pageNumber = 1,
        sajdaType = null,
        sajdaNumber = null,
        translation = translation,
    )

    private fun surah(number: Int) = Surah(
        number = number,
        nameArabic = "البقرة",
        nameEnglish = "The Cow",
        nameTransliteration = "Al-Baqarah",
        revelationType = RevelationType.MEDINAN,
        ayahCount = 286,
        juzStart = 1,
        orderInMushaf = 2,
    )
}
