package com.arshadshah.nimaz.domain.usecase.ai

import com.arshadshah.nimaz.core.navigation.Route
import com.arshadshah.nimaz.domain.model.AiError
import com.arshadshah.nimaz.domain.model.AnswerConfidence
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.CitationId
import com.arshadshah.nimaz.domain.model.Hadith
import com.arshadshah.nimaz.domain.model.HadithBook
import com.arshadshah.nimaz.domain.model.HadithRef
import com.arshadshah.nimaz.domain.model.ProofSource
import com.arshadshah.nimaz.domain.model.Proof
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.SearchAssist
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.model.SurahWithAyahs
import com.arshadshah.nimaz.domain.repository.AiRepository
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.repository.AiRequestException
import com.arshadshah.nimaz.domain.usecase.GetBookByIdUseCase
import com.arshadshah.nimaz.domain.usecase.GetHadithByReferenceUseCase
import com.arshadshah.nimaz.domain.usecase.GetSurahWithAyahsUseCase
import com.arshadshah.nimaz.domain.usecase.HadithUseCases
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
    private val getSurahWithAyahsUC = mockk<GetSurahWithAyahsUseCase>()
    private val quranUseCases = mockk<QuranUseCases>()
    private val getHadithByReferenceUC = mockk<GetHadithByReferenceUseCase>()
    private val getBookByIdUC = mockk<GetBookByIdUseCase>()
    private val hadithUseCases = mockk<HadithUseCases>()
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)

    private lateinit var useCase: AskWithProofUseCase

    @Before
    fun setUp() {
        every { quranUseCases.getSurahWithAyahs } returns getSurahWithAyahsUC
        every { hadithUseCases.getHadithByReference } returns getHadithByReferenceUC
        every { hadithUseCases.getBookById } returns getBookByIdUC
        // Consent is granted throughout: the refusal path is AskWithProofConsentTest.
        every { settingsRepository.aiAskEnabled } returns flowOf(true)
        useCase = AskWithProofUseCase(aiRepository, quranUseCases, hadithUseCases, settingsRepository)
    }

    @Test
    fun `cited refs resolve to real local records with deep links`() = runTest {
        coEvery { aiRepository.assist(any()) } returns Result.success(
            assist(
                refs = listOf(CitationId.Quran(2, 153), CitationId.Quran(39, 10)),
                terms = listOf("patience", "sabr"),
            ),
        )
        every { getSurahWithAyahsUC.invoke(2, any()) } returns
            flowOf(surahWithAyahs(2, listOf(ayah(2, 153))))
        every { getSurahWithAyahsUC.invoke(39, any()) } returns
            flowOf(surahWithAyahs(39, listOf(ayah(39, 10))))

        val outcome = useCase("What does the Quran say about patience?")

        val answered = outcome as AskWithProofUseCase.Outcome.Answered
        assertThat(answered.answer).isEqualTo("Patience is encouraged.")
        assertThat(answered.confidence).isEqualTo(AnswerConfidence.HIGH)
        assertThat(answered.relatedTerms).containsExactly("patience", "sabr")
        assertThat(answered.proofs.map { it.citationId })
            .containsExactly("quran:2:153", "quran:39:10")
            .inOrder()
        // The proof carries the same structured fields a keyword result shows:
        // surah:ayah numbers, English surah name and the LOCAL translation.
        val quranProof = answered.proofs.first() as Proof.Quran
        assertThat(quranProof.route).isEqualTo(Route.QuranReader(2, 153))
        assertThat(quranProof.surahNumber).isEqualTo(2)
        assertThat(quranProof.ayahNumber).isEqualTo(153)
        assertThat(quranProof.surahName).isEqualTo("The Cow")
        assertThat(quranProof.displayText).isEqualTo("Be patient.")
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
        every { getSurahWithAyahsUC.invoke(2, any()) } returns
            flowOf(surahWithAyahs(2, listOf(ayah(2, 153))))
        every { getSurahWithAyahsUC.invoke(99, any()) } returns flowOf(null)

        val outcome = useCase("q?") as AskWithProofUseCase.Outcome.Answered

        assertThat(outcome.proofs.map { it.citationId }).containsExactly("quran:2:153")
    }

    @Test
    fun `cited hadith refs resolve to real local records after the quran proofs`() = runTest {
        coEvery { aiRepository.assist(any()) } returns Result.success(
            assist(
                refs = listOf(CitationId.Quran(2, 153)),
                hadithRefs = listOf(HadithRef("bukhari", 6018)),
            ),
        )
        every { getSurahWithAyahsUC.invoke(2, any()) } returns
            flowOf(surahWithAyahs(2, listOf(ayah(2, 153))))
        coEvery { getHadithByReferenceUC.invoke("bukhari:6018") } returns
            hadith(id = "6041", numberInBook = 6018)
        coEvery { getBookByIdUC.invoke("1") } returns book()

        val outcome = useCase("q?") as AskWithProofUseCase.Outcome.Answered

        // Quran proofs first, then hadith proofs — the proof carries the LOCAL
        // hadith id so the results list can dedupe it, plus a reader deep link.
        assertThat(outcome.proofs.map { it.citationId })
            .containsExactly("quran:2:153", "hadith:6041")
            .inOrder()
        // The proof mirrors the keyword hadith result: same title number
        // (hadithNumber), book-name subtitle and English text.
        val hadithProof = outcome.proofs.last() as Proof.Hadith
        assertThat(hadithProof.source).isEqualTo(ProofSource.HADITH)
        assertThat(hadithProof.displayText).isEqualTo("Actions are judged by intentions.")
        assertThat(hadithProof.hadithNumber).isEqualTo(1)
        assertThat(hadithProof.bookName).isEqualTo("Sahih al-Bukhari")
        assertThat(hadithProof.route).isEqualTo(Route.HadithReader("6041"))
    }

    @Test
    fun `hadith refs that do not resolve locally are dropped silently`() = runTest {
        coEvery { aiRepository.assist(any()) } returns Result.success(
            assist(
                refs = emptyList(),
                hadithRefs = listOf(
                    HadithRef("bukhari", 6018), // resolves
                    HadithRef("muslim", 99999), // no such row -> dropped
                ),
            ),
        )
        coEvery { getHadithByReferenceUC.invoke("bukhari:6018") } returns
            hadith(id = "6041", numberInBook = 6018)
        coEvery { getHadithByReferenceUC.invoke("muslim:99999") } returns null
        coEvery { getBookByIdUC.invoke("1") } returns book()

        val outcome = useCase("q?") as AskWithProofUseCase.Outcome.Answered

        assertThat(outcome.proofs.map { it.citationId }).containsExactly("hadith:6041")
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
    fun `proofs are capped at MAX_PROOFS per source`() = runTest {
        val refs = (1..12).map { CitationId.Quran(2, it) }
        val hadithRefs = (1..9).map { HadithRef("bukhari", it) }
        coEvery { aiRepository.assist(any()) } returns
            Result.success(assist(refs = refs, hadithRefs = hadithRefs))
        every { getSurahWithAyahsUC.invoke(2, any()) } returns
            flowOf(surahWithAyahs(2, (1..12).map { ayah(2, it) }))
        coEvery { getHadithByReferenceUC.invoke(any()) } answers {
            val number = firstArg<String>().substringAfter(':').toInt()
            hadith(id = number.toString(), numberInBook = number)
        }
        coEvery { getBookByIdUC.invoke("1") } returns book()

        val outcome = useCase("q?") as AskWithProofUseCase.Outcome.Answered

        assertThat(outcome.proofs).hasSize(
            AskWithProofUseCase.MAX_PROOFS + AskWithProofUseCase.MAX_HADITH_PROOFS,
        )
    }

    // ── model builders ────────────────────────────────────────────────────────

    private fun assist(
        refs: List<CitationId.Quran>,
        hadithRefs: List<HadithRef> = emptyList(),
        terms: List<String> = listOf("patience"),
    ) = SearchAssist(
        answer = "Patience is encouraged.",
        quranRefs = refs,
        hadithRefs = hadithRefs,
        terms = terms,
        confidence = AnswerConfidence.HIGH,
    )

    private fun hadith(id: String, numberInBook: Int, bookId: String = "1") = Hadith(
        id = id,
        bookId = bookId,
        chapterId = "1_0",
        hadithNumber = 1,
        hadithNumberInBook = numberInBook,
        textArabic = "arabic",
        textEnglish = "Actions are judged by intentions.",
        narratorChain = null,
        narratorName = "Umar ibn al-Khattab",
        grade = null,
        gradeArabic = null,
        reference = "bukhari:$numberInBook",
    )

    private fun book(id: String = "1", name: String = "Sahih al-Bukhari") = HadithBook(
        id = id,
        nameArabic = "صحيح البخاري",
        nameEnglish = name,
        authorName = "Imam al-Bukhari",
        authorArabic = "",
        totalHadiths = 7563,
        totalChapters = 97,
        description = null,
        displayOrder = 1,
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

    private fun surahWithAyahs(number: Int, ayahs: List<Ayah>) =
        SurahWithAyahs(surah = surah(number), ayahs = ayahs)

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
