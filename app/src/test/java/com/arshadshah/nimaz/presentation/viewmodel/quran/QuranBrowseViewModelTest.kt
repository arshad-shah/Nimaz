package com.arshadshah.nimaz.presentation.viewmodel.quran

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.model.MushafPagination
import com.arshadshah.nimaz.domain.model.MushafScript
import com.arshadshah.nimaz.domain.model.QuranSearchQuery
import com.arshadshah.nimaz.domain.model.RevelationType
import com.arshadshah.nimaz.domain.model.Surah
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.usecase.QuranUseCases
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
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
 * Browse's one field, which has to answer four different questions.
 *
 * This replaces `QuranSurahFilterTest`: the filter it covered lived on `QuranHomeUiState` behind
 * a `QuranEvent.Search` nobody emitted, and it moved here when Browse became a destination. The
 * cases it pinned are all still here, plus the three the single field adds — a juz, a page and a
 * bare surah number, each of which also raises a jump card the list alone cannot offer.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QuranBrowseViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var useCases: QuranUseCases
    private lateinit var settings: SettingsRepository

    private val surahs = listOf(
        surah(1, "The Opening", "Al-Fatihah", "الفاتحة", juzStart = 1, startPage = 1),
        surah(2, "The Cow", "Al-Baqarah", "البقرة", juzStart = 1, startPage = 2),
        surah(18, "The Cave", "Al-Kahf", "الكهف", juzStart = 15, startPage = 293),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        useCases = mockk(relaxed = true)
        settings = mockk(relaxed = true)
        every { settings.quranMushafScript } returns MutableStateFlow("MADANI")
        every { useCases.getSurahList() } returns flowOf(surahs)
        // The juz on each row and each header comes from the *edition's* page mapping, not from
        // the surah row — `Surah` has no juzStart any more, because the column it came from does
        // not exist and the mapper was filling it with a literal 1. The Madani fallback carries
        // the printed juz start pages, which is real reference data.
        coEvery { useCases.getMushafPagination(any()) } returns
            MushafPagination.fallback(MushafScript.MADANI)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = QuranBrowseViewModel(useCases, settings, RecordingTelemetry())

    private fun loaded() = viewModel()

    @Test
    fun `state starts loading and then lists every surah in mushaf order`() = runTest {
        val vm = loaded()
        assertThat(vm.state.value.isLoading).isTrue()

        advanceUntilIdle()

        assertThat(vm.state.value.isLoading).isFalse()
        assertThat(vm.state.value.rows.map { it.number }).containsExactly(1, 2, 18).inOrder()
        assertThat(vm.state.value.jumpTarget).isNull()
    }

    @Test
    fun `a name query filters by english name, transliteration and arabic`() = runTest {
        val vm = loaded()
        advanceUntilIdle()

        vm.onEvent(QuranBrowseEvent.QueryChanged("kahf"))
        assertThat(vm.state.value.rows.map { it.number }).containsExactly(18)

        vm.onEvent(QuranBrowseEvent.QueryChanged("THE COW"))
        assertThat(vm.state.value.rows.map { it.number }).containsExactly(2)

        vm.onEvent(QuranBrowseEvent.QueryChanged("الكهف"))
        assertThat(vm.state.value.rows.map { it.number }).containsExactly(18)
    }

    @Test
    fun `a name query raises no jump card`() = runTest {
        val vm = loaded()
        advanceUntilIdle()

        vm.onEvent(QuranBrowseEvent.QueryChanged("kahf"))

        assertThat(vm.state.value.jumpTarget).isNull()
    }

    @Test
    fun `a juz query filters to the surahs starting there and sets the jump target`() = runTest {
        val vm = loaded()
        advanceUntilIdle()

        vm.onEvent(QuranBrowseEvent.QueryChanged("juz 15"))

        // Al-Kahf opens on Madani page 293, which is in juz 15.
        assertThat(vm.state.value.rows.map { it.number }).containsExactly(18)
        assertThat(vm.state.value.jumpTarget).isEqualTo(QuranSearchQuery.Juz(15))
    }

    @Test
    fun `a page query sets a page jump target`() = runTest {
        val vm = loaded()
        advanceUntilIdle()

        vm.onEvent(QuranBrowseEvent.QueryChanged("page 299"))

        assertThat(vm.state.value.jumpTarget).isEqualTo(QuranSearchQuery.Page(299))
    }

    @Test
    fun `a surah number query sets a surah jump target and narrows to it`() = runTest {
        val vm = loaded()
        advanceUntilIdle()

        vm.onEvent(QuranBrowseEvent.QueryChanged("18"))

        assertThat(vm.state.value.rows.map { it.number }).containsExactly(18)
        assertThat(vm.state.value.jumpTarget).isEqualTo(QuranSearchQuery.SurahNumber(18))
    }

    @Test
    fun `clearing the query restores the full list and clears the jump target`() = runTest {
        val vm = loaded()
        advanceUntilIdle()
        vm.onEvent(QuranBrowseEvent.QueryChanged("juz 15"))

        vm.onEvent(QuranBrowseEvent.ClearQuery)

        assertThat(vm.state.value.rows.map { it.number }).containsExactly(1, 2, 18).inOrder()
        assertThat(vm.state.value.jumpTarget).isNull()
        assertThat(vm.state.value.query).isEmpty()
    }

    @Test
    fun `a query matching nothing yields an empty list and no jump target`() = runTest {
        val vm = loaded()
        advanceUntilIdle()

        vm.onEvent(QuranBrowseEvent.QueryChanged("zzzz"))

        assertThat(vm.state.value.rows).isEmpty()
        assertThat(vm.state.value.jumpTarget).isNull()
    }
}

private fun surah(
    number: Int,
    english: String,
    transliteration: String,
    arabic: String,
    juzStart: Int,
    startPage: Int,
) = Surah(
    number = number,
    nameArabic = arabic,
    nameEnglish = english,
    nameTransliteration = transliteration,
    revelationType = RevelationType.MECCAN,
    ayahCount = 7,
    orderInMushaf = number,
    startPage = startPage,
)
