package com.arshadshah.nimaz.presentation.viewmodel.quran

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.core.text.FakeStringProvider
import com.arshadshah.nimaz.domain.model.Ayah
import com.arshadshah.nimaz.domain.model.Khatam
import com.arshadshah.nimaz.domain.model.KhatamDetailSnapshot
import com.arshadshah.nimaz.domain.model.KhatamInsights
import com.arshadshah.nimaz.domain.model.MushafPageLayout
import com.arshadshah.nimaz.domain.model.MushafScript
import com.arshadshah.nimaz.domain.model.QuranBookmark
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.model.SurahWithAyahs
import com.arshadshah.nimaz.domain.repository.QuranPlayback
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.time.FakeTodayProvider
import com.arshadshah.nimaz.domain.usecase.KhatamUseCases
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
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
import java.time.LocalDate

/**
 * What the reader writes on a verse: a note, a bookmark, a khatam mark — and the line layout the
 * page mode needs to draw one.
 *
 * The note is the interesting one. `bookmarks` keys on `(kind, target_id)` and carries the note
 * as a column, so there is no such thing as a note without a mark: writing one on an unmarked
 * verse has to *create* the row, and writing one on a marked verse has to update the row that is
 * already there rather than insert a second. Getting that wrong is either a lost note or a
 * duplicate bookmark, and neither announces itself.
 *
 * The bookmark toggle is optimistic — the icon flips before the write lands — which means the
 * flip has to reach both copies of the verse the reader state holds (the surah object and the
 * flat list), or the tick appears in the list and not on the page.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QuranViewModelAnnotationTest {

    private val dispatcher = StandardTestDispatcher()
    private val telemetry = RecordingTelemetry()

    private lateinit var useCases: QuranUseCases
    private lateinit var audioManager: QuranPlayback
    private lateinit var settings: SettingsRepository
    private lateinit var khatamUseCases: KhatamUseCases

    private val bookmarks = MutableStateFlow<List<QuranBookmark>>(emptyList())
    private val activeKhatam = MutableStateFlow<Khatam?>(null)

    private val cave = Surah(
        number = 18,
        nameArabic = "الكهف",
        nameEnglish = "The Cave",
        nameTransliteration = "Al-Kahf",
        revelationType = RevelationType.MECCAN,
        ayahCount = 3,
        orderInMushaf = 18,
        startPage = 293,
    )

    private fun ayah(id: Int, bookmarked: Boolean = false) = Ayah(
        id = id,
        surahNumber = 18,
        ayahNumber = id - 2139,
        textArabic = "نص",
        textSimple = "nass",
        juzNumber = 15,
        hizbNumber = 30,
        rubNumber = 0,
        pageNumber = 293,
        sajdaType = null,
        sajdaNumber = null,
        isBookmarked = bookmarked,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)

        useCases = mockk(relaxed = true)
        audioManager = mockk(relaxed = true)
        settings = mockk(relaxed = true)
        khatamUseCases = mockk(relaxed = true)

        every { settings.quranMushafScript } returns MutableStateFlow(MushafScript.MADANI.name)
        every { settings.quranTranslatorId } returns MutableStateFlow("sahih_international")
        every { settings.showTranslation } returns MutableStateFlow(true)
        every { settings.showTransliteration } returns MutableStateFlow(false)
        every { settings.quranArabicFontSize } returns MutableStateFlow(28f)
        every { settings.quranArabicFont } returns MutableStateFlow("amiri")
        every { settings.quranTranslationFontSize } returns MutableStateFlow(16f)
        every { settings.continuousReading } returns MutableStateFlow(true)
        every { settings.keepScreenOn } returns MutableStateFlow(true)
        every { settings.selectedReciterId } returns MutableStateFlow(null)
        every { settings.showTajweed } returns MutableStateFlow(false)
        every { settings.tajweedUnderline } returns MutableStateFlow(false)

        every { useCases.getBookmarks() } returns bookmarks
        every { useCases.getSurahWithAyahs(any(), any()) } returns flowOf(
            SurahWithAyahs(cave, listOf(ayah(2140), ayah(2141), ayah(2142)))
        )

        every { khatamUseCases.observeActiveKhatam() } returns activeKhatam
        every { khatamUseCases.observeKhatamDetail(any()) } answers {
            val id = firstArg<Long>()
            MutableStateFlow(
                activeKhatam.value?.takeIf { it.id == id }?.let {
                    KhatamDetailSnapshot(
                        khatam = it,
                        juzProgress = emptyList(),
                        dailyLogs = emptyList(),
                        insights = KhatamInsights(),
                        readAyahIds = emptySet(),
                    )
                }
            )
        }
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = QuranViewModel(
        useCases,
        audioManager,
        settings,
        khatamUseCases,
        telemetry,
        FakeTodayProvider(LocalDate.of(2026, 3, 1)),
        FakeStringProvider(),
    )

    private fun bookmark(ayahId: Int, note: String?) = QuranBookmark(
        id = 5,
        ayahId = ayahId,
        surahNumber = 18,
        ayahNumber = 1,
        note = note,
        color = null,
        createdAt = 0,
        updatedAt = 0,
    )

    // ---- Notes ----

    @Test
    fun `a note on an unmarked verse creates the mark that carries it`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        val created = slot<QuranBookmark>()
        coEvery { useCases.insertBookmark(capture(created)) } returns Unit

        vm.onEvent(QuranEvent.SetAyahNote(2140, 18, 1, "worth returning to"))
        advanceUntilIdle()

        assertThat(created.captured.ayahId).isEqualTo(2140)
        assertThat(created.captured.note).isEqualTo("worth returning to")
        coVerify(exactly = 0) { useCases.updateBookmark(any()) }
    }

    @Test
    fun `a note on a verse already marked updates that row rather than adding one`() = runTest {
        bookmarks.value = listOf(bookmark(2140, note = null))
        val vm = viewModel()
        advanceUntilIdle()
        val updated = slot<QuranBookmark>()
        coEvery { useCases.updateBookmark(capture(updated)) } returns Unit

        vm.onEvent(QuranEvent.SetAyahNote(2140, 18, 1, "worth returning to"))
        advanceUntilIdle()

        assertThat(updated.captured.id).isEqualTo(5)
        assertThat(updated.captured.note).isEqualTo("worth returning to")
        coVerify(exactly = 0) { useCases.insertBookmark(any()) }
    }

    @Test
    fun `a note of nothing but spaces is stored as no note at all`() = runTest {
        bookmarks.value = listOf(bookmark(2140, note = "old"))
        val vm = viewModel()
        advanceUntilIdle()
        val updated = slot<QuranBookmark>()
        coEvery { useCases.updateBookmark(capture(updated)) } returns Unit

        vm.onEvent(QuranEvent.SetAyahNote(2140, 18, 1, "   "))
        advanceUntilIdle()

        assertThat(updated.captured.note).isNull()
    }

    @Test
    fun `writing a note is counted`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(QuranEvent.SetAyahNote(2140, 18, 1, "a thought"))
        advanceUntilIdle()

        assertThat(telemetry.featureUsages.map { it.action }).contains("set_ayah_note")
    }

    @Test
    fun `the reader's notes come from the bookmark stream, not a second query`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        bookmarks.value = listOf(bookmark(2140, note = "a thought"), bookmark(2141, note = "  "))
        advanceUntilIdle()

        // The blank one is not a note: a marker on a row with nothing behind it.
        assertThat(vm.readerState.value.ayahNotes).containsExactly(2140, "a thought")
    }

    // ---- Bookmarks ----

    @Test
    fun `toggling a bookmark flips both copies of the verse before the write lands`() = runTest {
        val vm = viewModel()
        vm.onEvent(QuranEvent.LoadSurah(18))
        advanceUntilIdle()

        vm.onEvent(QuranEvent.ToggleBookmark(2141, 18, 2))

        // Deliberately *before* advancing: the icon must change on the tap, not on the write.
        val state = vm.readerState.value
        assertThat(state.ayahs.first { it.id == 2141 }.isBookmarked).isTrue()
        assertThat(state.surahWithAyahs?.ayahs?.first { it.id == 2141 }?.isBookmarked).isTrue()
        assertThat(state.ayahs.first { it.id == 2140 }.isBookmarked).isFalse()
    }

    @Test
    fun `toggling a bookmark still writes it through`() = runTest {
        val vm = viewModel()
        vm.onEvent(QuranEvent.LoadSurah(18))
        advanceUntilIdle()

        vm.onEvent(QuranEvent.ToggleBookmark(2141, 18, 2))
        advanceUntilIdle()

        coVerify { useCases.toggleBookmark(2141, 18, 2) }
    }

    // ---- The line-accurate page layout ----

    @Test
    fun `a page's line layout is fetched once and answered from the cache after that`() = runTest {
        coEvery { useCases.getMushafPageLayout(any(), any()) } returns
            MushafPageLayout(page = 293, lines = emptyList())
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(QuranEvent.LoadMushafPageLayout(293))
        advanceUntilIdle()
        vm.onEvent(QuranEvent.LoadMushafPageLayout(293))
        advanceUntilIdle()

        coVerify(exactly = 1) { useCases.getMushafPageLayout(293, any()) }
        assertThat(vm.readerState.value.mushafPageLayoutCache).containsKey(293)
    }

    // ---- Khatam ----

    @Test
    fun `marking a surah read needs a khatam to mark it against`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(QuranEvent.MarkSurahAsReadForKhatam(18))
        advanceUntilIdle()

        coVerify(exactly = 0) { khatamUseCases.markSurahAsRead(any(), any()) }
    }

    @Test
    fun `marking a surah read goes to the active khatam`() = runTest {
        activeKhatam.value = Khatam(id = 3, name = "Ramadan")
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(QuranEvent.MarkSurahAsReadForKhatam(18))
        advanceUntilIdle()

        coVerify { khatamUseCases.markSurahAsRead(3, 18) }
    }
}
