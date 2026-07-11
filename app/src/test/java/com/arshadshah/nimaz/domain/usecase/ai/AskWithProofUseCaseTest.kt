package com.arshadshah.nimaz.domain.usecase.ai

import com.arshadshah.nimaz.core.navigation.Route
import com.arshadshah.nimaz.domain.model.AnswerConfidence
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.Dua
import com.arshadshah.nimaz.domain.model.DuaSearchResult
import com.arshadshah.nimaz.domain.model.GroundedAnswer
import com.arshadshah.nimaz.domain.model.Hadith
import com.arshadshah.nimaz.domain.model.HadithSearchResult
import com.arshadshah.nimaz.domain.model.ProofPassage
import com.arshadshah.nimaz.domain.model.QuranSearchResult
import com.arshadshah.nimaz.domain.model.SearchType
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.repository.AiRepository
import com.arshadshah.nimaz.domain.usecase.DuaUseCases
import com.arshadshah.nimaz.domain.usecase.GetAyahsBySurahUseCase
import com.arshadshah.nimaz.domain.usecase.GetDuaByIdUseCase
import com.arshadshah.nimaz.domain.usecase.GetHadithByIdUseCase
import com.arshadshah.nimaz.domain.usecase.GetSurahByNumberUseCase
import com.arshadshah.nimaz.domain.usecase.HadithUseCases
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import com.arshadshah.nimaz.domain.usecase.SearchDuasUseCase
import com.arshadshah.nimaz.domain.usecase.SearchHadithsUseCase
import com.arshadshah.nimaz.domain.usecase.SearchQuranUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class AskWithProofUseCaseTest {

    private val aiRepository = mockk<AiRepository>()

    private val searchQuranUC = mockk<SearchQuranUseCase>()
    private val getAyahsBySurahUC = mockk<GetAyahsBySurahUseCase>()
    private val getSurahByNumberUC = mockk<GetSurahByNumberUseCase>()
    private val quranUseCases = mockk<QuranUseCases>()

    private val searchHadithsUC = mockk<SearchHadithsUseCase>()
    private val getHadithByIdUC = mockk<GetHadithByIdUseCase>()
    private val hadithUseCases = mockk<HadithUseCases>()

    private val searchDuasUC = mockk<SearchDuasUseCase>()
    private val getDuaByIdUC = mockk<GetDuaByIdUseCase>()
    private val duaUseCases = mockk<DuaUseCases>()

    private lateinit var useCase: AskWithProofUseCase

    @Before
    fun setUp() {
        every { quranUseCases.searchQuran } returns searchQuranUC
        every { quranUseCases.getAyahsBySurah } returns getAyahsBySurahUC
        every { quranUseCases.getSurahByNumber } returns getSurahByNumberUC
        every { hadithUseCases.searchHadiths } returns searchHadithsUC
        every { hadithUseCases.getHadithById } returns getHadithByIdUC
        every { duaUseCases.searchDuas } returns searchDuasUC
        every { duaUseCases.getDuaById } returns getDuaByIdUC

        useCase = AskWithProofUseCase(aiRepository, quranUseCases, hadithUseCases, duaUseCases)
    }

    private val allSources = AskWithProofUseCase.Sources(quran = true, hadith = true, dua = true)

    @Test
    fun `no local results short-circuits without a network call`() = runTest {
        every { searchQuranUC.invoke(any(), any()) } returns flowOf(emptyList())
        every { searchHadithsUC.invoke(any()) } returns flowOf(emptyList())
        every { searchDuasUC.invoke(any()) } returns flowOf(emptyList())

        val outcome = useCase("What is patience?", allSources, maxProofs = 5)

        assertThat(outcome).isEqualTo(AskWithProofUseCase.Outcome.NoEvidence)
        coVerify(exactly = 0) { aiRepository.ask(any(), any()) }
    }

    @Test
    fun `disabled sources are not searched`() = runTest {
        every { searchQuranUC.invoke(any(), any()) } returns flowOf(listOf(quranResult(2, 153)))
        coEvery { aiRepository.ask(any(), any()) } returns
            Result.success(answer(citationIds = emptyList()))

        useCase(
            "patience prayer",
            AskWithProofUseCase.Sources(quran = true, hadith = false, dua = false),
            maxProofs = 5,
        )

        verify(exactly = 0) { searchHadithsUC.invoke(any()) }
        verify(exactly = 0) { searchDuasUC.invoke(any()) }
        verify { searchQuranUC.invoke(any(), any()) }
    }

    @Test
    fun `passage set is capped by maxProofs and truncated to 1200 chars`() = runTest {
        val many = (1..20).map { quranResult(2, it, translation = "x".repeat(1500)) }
        every { searchQuranUC.invoke(any(), any()) } returns flowOf(many)
        every { searchHadithsUC.invoke(any()) } returns flowOf(emptyList())
        every { searchDuasUC.invoke(any()) } returns flowOf(emptyList())

        val captured = slot<List<ProofPassage>>()
        coEvery { aiRepository.ask(any(), capture(captured)) } returns
            Result.success(answer(citationIds = emptyList()))

        useCase("patience", allSources, maxProofs = 3)

        assertThat(captured.captured).hasSize(3)
        assertThat(captured.captured.all { it.text.length <= 1200 }).isTrue()
    }

    @Test
    fun `citations resolve to proofs and malformed or unresolvable ids are dropped`() = runTest {
        every { searchQuranUC.invoke(any(), any()) } returns flowOf(listOf(quranResult(2, 153)))
        every { searchHadithsUC.invoke(any()) } returns flowOf(listOf(hadithResult("bukhari-1")))
        every { searchDuasUC.invoke(any()) } returns flowOf(emptyList())

        coEvery { aiRepository.ask(any(), any()) } returns Result.success(
            answer(
                citationIds = listOf(
                    "quran:2:153",       // resolves
                    "hadith:bukhari-1",  // resolves
                    "garbage",           // malformed -> dropped
                    "quran:99:99",       // unresolvable -> dropped
                ),
            ),
        )
        // Resolution stubs
        every { getAyahsBySurahUC.invoke(2) } returns flowOf(listOf(ayah(2, 153)))
        every { getAyahsBySurahUC.invoke(99) } returns flowOf(emptyList())
        coEvery { getSurahByNumberUC.invoke(2) } returns surah(2)
        coEvery { getHadithByIdUC.invoke("bukhari-1") } returns hadith("bukhari-1")

        val outcome = useCase("patience prayer", allSources, maxProofs = 8)

        val answered = outcome as AskWithProofUseCase.Outcome.Answered
        assertThat(answered.proofs.map { it.citationId })
            .containsExactly("quran:2:153", "hadith:bukhari-1")
        val quranProof = answered.proofs.first { it.citationId == "quran:2:153" }
        assertThat(quranProof.route).isEqualTo(Route.QuranReader(2, 153))
        val hadithProof = answered.proofs.first { it.citationId == "hadith:bukhari-1" }
        assertThat(hadithProof.route).isEqualTo(Route.HadithReader("bukhari-1"))
    }

    // ── model builders ────────────────────────────────────────────────────────

    private fun answer(citationIds: List<String>) = GroundedAnswer(
        answer = "The sources describe patience.",
        citationIds = citationIds,
        confidence = AnswerConfidence.HIGH,
        insufficientEvidence = false,
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

    private fun quranResult(surah: Int, ayahNumber: Int, translation: String = "Be patient.") =
        QuranSearchResult(
            ayah = ayah(surah, ayahNumber, translation),
            surahName = "Al-Baqarah",
            matchedText = "patience",
            searchType = SearchType.TRANSLATION,
        )

    private fun hadith(id: String) = Hadith(
        id = id,
        bookId = "bukhari",
        chapterId = "ch1",
        hadithNumber = 1,
        hadithNumberInBook = 1,
        textArabic = "arabic",
        textEnglish = "Actions are by intentions.",
        narratorChain = null,
        narratorName = null,
        grade = null,
        gradeArabic = null,
        reference = "Sahih al-Bukhari 1",
    )

    private fun hadithResult(id: String) = HadithSearchResult(
        hadith = hadith(id),
        bookName = "Sahih al-Bukhari",
        chapterName = "Revelation",
        matchedText = "intentions",
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

    @Suppress("unused")
    private fun duaResult(id: String) = DuaSearchResult(
        dua = Dua(
            id = id,
            categoryId = "morning",
            titleArabic = "arabic",
            titleEnglish = "Morning dua",
            textArabic = "arabic",
            textTransliteration = null,
            textEnglish = "O Allah, help me.",
            reference = null,
            occasion = null,
            benefits = null,
            repeatCount = null,
            audioUrl = null,
            displayOrder = 0,
        ),
        categoryName = "Morning",
        matchedText = "Allah",
    )
}
