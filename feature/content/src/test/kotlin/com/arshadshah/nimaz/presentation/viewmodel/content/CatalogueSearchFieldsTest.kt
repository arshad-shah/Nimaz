package com.arshadshah.nimaz.presentation.viewmodel.content

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.model.AsmaUlHusna
import com.arshadshah.nimaz.domain.model.AsmaUnNabi
import com.arshadshah.nimaz.domain.model.Prophet
import com.arshadshah.nimaz.domain.usecase.AsmaUlHusnaUseCases
import com.arshadshah.nimaz.domain.usecase.AsmaUnNabiUseCases
import com.arshadshah.nimaz.domain.usecase.ProphetUseCases
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
 * **Which fields each catalogue's search looks at** — the one thing that genuinely differs
 * between the three ViewModels.
 *
 * `CatalogViewModel` collapsed three byte-identical classes into one generic, and what stayed
 * per-feature is a `CatalogSource`: where the rows come from, and `matches`. `CatalogViewModelTest`
 * drives the generic with a synthetic source, so it proves the *machinery* and can say nothing
 * about the three real ones — which are private classes in three files, and the only thing a
 * reader ever experiences of them.
 *
 * The fields are not the same across the three, and the difference is deliberate: the Prophets
 * search also covers the **title** ("Friend of Allah") and the **era**, because those are how
 * someone looks for a prophet whose name they cannot spell. Dropping either arm is a search that
 * quietly stops finding things — no crash, no empty state, just fewer results than there should
 * be.
 *
 * All three are case-insensitive on the transliterated and English fields **and on the Arabic**,
 * which costs nothing and means a query pasted from anywhere still matches.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CatalogueSearchFieldsTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    // ---- the ninety-nine names --------------------------------------------------------

    private val rahman = AsmaUlHusna(
        id = 1,
        number = 1,
        nameArabic = "الرحمن",
        nameTransliteration = "Ar-Rahman",
        nameEnglish = "The Most Compassionate",
        meaning = "The One whose mercy encompasses all creation",
        explanation = "…",
        benefits = "…",
        quranReferences = listOf("1:1"),
        usageInDua = "…",
        displayOrder = 1,
    )
    private val aziz = rahman.copy(
        id = 2,
        nameArabic = "العزيز",
        nameTransliteration = "Al-Aziz",
        nameEnglish = "The Almighty",
        meaning = "The One who is invincible",
    )

    private fun allahViewModel(
        useCases: AsmaUlHusnaUseCases = mockk(relaxed = true),
    ): Pair<AsmaUlHusnaViewModel, AsmaUlHusnaUseCases> {
        every { useCases.getAllNames() } returns MutableStateFlow(listOf(rahman, aziz))
        every { useCases.getFavorites() } returns flowOf(emptyList())
        return AsmaUlHusnaViewModel(useCases, RecordingTelemetry()) to useCases
    }

    @Test
    fun `a divine name is found by its transliteration, its English and its meaning`() = runTest {
        val (vm, _) = allahViewModel()
        advanceUntilIdle()

        vm.onEvent(CatalogEvent.Search("rahman"))
        assertThat(vm.listState.value.filteredItems).containsExactly(rahman)

        vm.onEvent(CatalogEvent.Search("almighty"))
        assertThat(vm.listState.value.filteredItems).containsExactly(aziz)

        // The meaning is the arm most easily dropped, and the one a reader uses when they know
        // what a name means and not what it is called.
        vm.onEvent(CatalogEvent.Search("invincible"))
        assertThat(vm.listState.value.filteredItems).containsExactly(aziz)
    }

    @Test
    fun `a divine name is found by its Arabic`() = runTest {
        val (vm, _) = allahViewModel()
        advanceUntilIdle()

        vm.onEvent(CatalogEvent.Search("الرحمن"))

        assertThat(vm.listState.value.filteredItems).containsExactly(rahman)
    }

    @Test
    fun `starring a divine name re-reads the open item, by its own id`() = runTest {
        val useCases: AsmaUlHusnaUseCases = mockk(relaxed = true)
        val (vm, _) = allahViewModel(useCases)
        coEvery { useCases.getNameById(2) } returns aziz.copy(isFavorite = true)
        advanceUntilIdle()

        vm.onEvent(CatalogEvent.LoadDetail(2))
        advanceUntilIdle()
        vm.onEvent(CatalogEvent.ToggleFavorite(2))
        advanceUntilIdle()

        coVerify { useCases.toggleFavorite(2) }
        assertThat(vm.detailState.value.item?.isFavorite).isTrue()
    }

    @Test
    fun `starring a name that is not the one open does not re-read anything`() = runTest {
        // `idOf(open) == itemId` guards the re-read: starring from the *list* while a different
        // detail is loaded must not replace what the detail screen is showing.
        val useCases: AsmaUlHusnaUseCases = mockk(relaxed = true)
        val (vm, _) = allahViewModel(useCases)
        coEvery { useCases.getNameById(1) } returns rahman
        advanceUntilIdle()

        vm.onEvent(CatalogEvent.LoadDetail(1))
        advanceUntilIdle()
        vm.onEvent(CatalogEvent.ToggleFavorite(2))
        advanceUntilIdle()

        coVerify { useCases.toggleFavorite(2) }
        assertThat(vm.detailState.value.item).isEqualTo(rahman)
    }

    @Test
    fun `the slower of two detail reads cannot land on the newer one's screen`() {
        // `requestedItemId` is set synchronously and checked after the suspension point,
        // because a coroutine cancelled after its last suspension still runs to the end of its
        // block. Open one name, go back, open another quickly — the detail screen fires
        // `LoadDetail` from a `LaunchedEffect(id)` and all three catalogue screens share one
        // ViewModel per back-stack entry — and the first read resolving second would put the
        // first name's text under the second name's route, with `isLoading = false` to say it
        // was ready.
        val useCases: AsmaUlHusnaUseCases = mockk(relaxed = true)
        val (vm, _) = allahViewModel(useCases)
        coEvery { useCases.getNameById(1) } coAnswers {
            kotlinx.coroutines.delay(100)
            rahman
        }
        coEvery { useCases.getNameById(2) } returns aziz

        runTest {
            advanceUntilIdle()
            vm.onEvent(CatalogEvent.LoadDetail(1))
            vm.onEvent(CatalogEvent.LoadDetail(2))
            advanceUntilIdle()

            assertThat(vm.detailState.value.item).isEqualTo(aziz)
        }
    }

    // ---- the names of the Prophet ﷺ ---------------------------------------------------

    private val mustafa = AsmaUnNabi(
        id = 1,
        number = 1,
        nameArabic = "المصطفى",
        nameTransliteration = "Al-Mustafa",
        nameEnglish = "The Chosen One",
        meaning = "Selected above all creation",
        explanation = "…",
        source = "Sahih Muslim 2278",
        displayOrder = 1,
    )
    private val amin = mustafa.copy(
        id = 2,
        nameArabic = "الأمين",
        nameTransliteration = "Al-Amin",
        nameEnglish = "The Trustworthy",
        meaning = "Known for honesty before prophethood",
    )

    @Test
    fun `a name of the Prophet is found by transliteration, English, Arabic and meaning`() =
        runTest {
            val useCases: AsmaUnNabiUseCases = mockk(relaxed = true)
            every { useCases.getAllNames() } returns MutableStateFlow(listOf(mustafa, amin))
            every { useCases.getFavorites() } returns flowOf(emptyList())
            val vm = AsmaUnNabiViewModel(useCases, RecordingTelemetry())
            advanceUntilIdle()

            vm.onEvent(CatalogEvent.Search("mustafa"))
            assertThat(vm.listState.value.filteredItems).containsExactly(mustafa)

            vm.onEvent(CatalogEvent.Search("trustworthy"))
            assertThat(vm.listState.value.filteredItems).containsExactly(amin)

            vm.onEvent(CatalogEvent.Search("الأمين"))
            assertThat(vm.listState.value.filteredItems).containsExactly(amin)

            vm.onEvent(CatalogEvent.Search("honesty"))
            assertThat(vm.listState.value.filteredItems).containsExactly(amin)
        }

    // ---- the prophets -----------------------------------------------------------------

    private val ibrahim = Prophet(
        id = 1,
        number = 1,
        nameArabic = "إبراهيم",
        nameEnglish = "Abraham",
        nameTransliteration = "Ibrahim",
        titleArabic = "خليل الله",
        titleEnglish = "Friend of Allah",
        storySummary = "…",
        keyLessons = emptyList(),
        quranMentions = emptyList(),
        era = "circa 2000 BCE",
        lineage = "Son of Azar",
        yearsLived = "175 years",
        placeOfPreaching = "Ur and Canaan",
        miracles = emptyList(),
        displayOrder = 1,
    )
    private val musa = ibrahim.copy(
        id = 2,
        nameArabic = "موسى",
        nameEnglish = "Moses",
        nameTransliteration = "Musa",
        titleEnglish = "The One who spoke with Allah",
        era = "circa 1300 BCE",
    )

    private fun prophetViewModel(): ProphetViewModel {
        val useCases: ProphetUseCases = mockk(relaxed = true)
        every { useCases.getAllProphets() } returns MutableStateFlow(listOf(ibrahim, musa))
        every { useCases.getFavorites() } returns flowOf(emptyList())
        return ProphetViewModel(useCases, RecordingTelemetry())
    }

    @Test
    fun `a prophet is found by every spelling of the name`() = runTest {
        val vm = prophetViewModel()
        advanceUntilIdle()

        vm.onEvent(CatalogEvent.Search("abraham"))
        assertThat(vm.listState.value.filteredItems).containsExactly(ibrahim)

        vm.onEvent(CatalogEvent.Search("ibrahim"))
        assertThat(vm.listState.value.filteredItems).containsExactly(ibrahim)

        vm.onEvent(CatalogEvent.Search("إبراهيم"))
        assertThat(vm.listState.value.filteredItems).containsExactly(ibrahim)
    }

    @Test
    fun `a prophet is found by his title`() = runTest {
        // The arm the other two catalogues do not have. Someone searching "Friend of Allah"
        // knows the epithet and not the spelling.
        val vm = prophetViewModel()
        advanceUntilIdle()

        vm.onEvent(CatalogEvent.Search("friend of allah"))

        assertThat(vm.listState.value.filteredItems).containsExactly(ibrahim)
    }

    @Test
    fun `a prophet is found by his era`() = runTest {
        // The other arm unique to this catalogue: "who was around in 1300 BCE" is a real
        // question and the only field that answers it.
        val vm = prophetViewModel()
        advanceUntilIdle()

        vm.onEvent(CatalogEvent.Search("1300 BCE"))

        assertThat(vm.listState.value.filteredItems).containsExactly(musa)
    }

    @Test
    fun `a query matching nothing in any field leaves the list empty`() = runTest {
        val vm = prophetViewModel()
        advanceUntilIdle()

        vm.onEvent(CatalogEvent.Search("zzzz"))

        assertThat(vm.listState.value.filteredItems).isEmpty()
        assertThat(vm.listState.value.items).hasSize(2)
    }

    @Test
    fun `clearing the query restores every prophet`() = runTest {
        val vm = prophetViewModel()
        advanceUntilIdle()

        vm.onEvent(CatalogEvent.Search("abraham"))
        assertThat(vm.listState.value.filteredItems).hasSize(1)

        vm.onEvent(CatalogEvent.ClearSearch)

        assertThat(vm.listState.value.filteredItems).hasSize(2)
        assertThat(vm.listState.value.searchQuery).isEmpty()
    }

    @Test
    fun `the favourites flow feeds its own field, not the filtered list`() = runTest {
        val useCases: ProphetUseCases = mockk(relaxed = true)
        every { useCases.getAllProphets() } returns MutableStateFlow(listOf(ibrahim, musa))
        every { useCases.getFavorites() } returns MutableStateFlow(listOf(musa))
        val vm = ProphetViewModel(useCases, RecordingTelemetry())
        advanceUntilIdle()

        assertThat(vm.listState.value.favorites).containsExactly(musa)
        assertThat(vm.listState.value.filteredItems).hasSize(2)
    }
}
