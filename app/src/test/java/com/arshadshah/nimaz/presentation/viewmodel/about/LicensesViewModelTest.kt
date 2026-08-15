package com.arshadshah.nimaz.presentation.viewmodel.about

import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.model.LibraryLicense
import com.arshadshah.nimaz.domain.model.LicenseFamily
import com.arshadshah.nimaz.domain.model.OpenSourceLibrary
import com.arshadshah.nimaz.domain.usecase.licenses.LicensesUseCases
import com.arshadshah.nimaz.presentation.components.molecules.NimazErrorKind
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * The screens this ViewModel serves had no ViewModel at all: a `LaunchedEffect` built
 * `Libs` and a `remember { mutableStateOf }` held the result, so a failed parse left
 * `isLoading` true forever and none of it was reachable from a test.
 *
 * These are the cases that could not previously be written.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LicensesViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val telemetry = RecordingTelemetry()
    private lateinit var useCases: LicensesUseCases

    private val compose = library(1, "Compose UI", "androidx.compose.ui:ui", "Google", "Apache License 2.0")
    private val adhan = library(2, "Adhan", "com.batoulapps.adhan:adhan2", "Batoul Apps", "MIT License")
    private val amiri = library(3, "Amiri", "org.amirifont:amiri", "Khaled Hosny", "SIL Open Font License 1.1")

    private fun library(
        id: Int,
        name: String,
        coordinate: String,
        author: String?,
        licenseName: String?,
    ) = OpenSourceLibrary(
        id = id,
        name = name,
        coordinate = coordinate,
        version = "1.0.0",
        author = author,
        website = null,
        licenses = licenseName?.let { listOf(LibraryLicense(it, url = null, content = null)) }
            ?: emptyList(),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        useCases = mockk(relaxed = true)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `a loaded list clears loading and carries no error`() = runTest(dispatcher) {
        coEvery { useCases.getLibraries() } returns listOf(compose)

        val viewModel = LicensesViewModel(useCases, telemetry)
        viewModel.onEvent(LicensesEvent.LoadLibraries)
        advanceUntilIdle()

        val state = viewModel.listState.value
        assertThat(state.libraries).containsExactly(compose)
        assertThat(state.isLoading).isFalse()
        assertThat(state.error).isNull()
    }

    @Test
    fun `a failed load stops loading and says so`() = runTest(dispatcher) {
        coEvery { useCases.getLibraries() } throws IllegalStateException("asset missing")

        val viewModel = LicensesViewModel(useCases, telemetry)
        viewModel.onEvent(LicensesEvent.LoadLibraries)
        advanceUntilIdle()

        val state = viewModel.listState.value
        // The defect this pins: the old screen left the spinner running forever.
        assertThat(state.isLoading).isFalse()
        assertThat(state.error?.message).isEqualTo(R.string.licenses_load_failed)
        // The exception text is carried, but as detail — never as the readable message.
        assertThat(state.error?.details).isEqualTo("asset missing")
        assertThat(telemetry.errors).hasSize(1)
    }

    @Test
    fun `retry clears the error and asks again`() = runTest(dispatcher) {
        coEvery { useCases.getLibraries() } throws IllegalStateException("asset missing")
        val viewModel = LicensesViewModel(useCases, telemetry)
        viewModel.onEvent(LicensesEvent.LoadLibraries)
        advanceUntilIdle()

        coEvery { useCases.getLibraries() } returns listOf(compose)
        viewModel.onEvent(LicensesEvent.Retry)
        advanceUntilIdle()

        val state = viewModel.listState.value
        assertThat(state.error).isNull()
        assertThat(state.libraries).containsExactly(compose)
    }

    @Test
    fun `the list opens grouped by licence, largest family first`() = runTest(dispatcher) {
        // Two Apache libraries against one MIT, deliberately loaded MIT-first: an
        // insertion-ordered grouping would put the single-entry section at the top.
        coEvery { useCases.getLibraries() } returns listOf(
            adhan,
            compose,
            library(4, "Room", "androidx.room:room-runtime", "Google", "Apache License 2.0"),
        )

        val viewModel = loadedViewModel()

        val state = viewModel.listState.value
        assertThat(state.grouping).isEqualTo(LicenseGrouping.BY_LICENCE)
        assertThat(state.sections.map { it.family })
            .containsExactly(LicenseFamily.APACHE_2, LicenseFamily.MIT).inOrder()
        assertThat(state.sections.first().libraries.map { it.name })
            .containsExactly("Compose UI", "Room").inOrder()
    }

    @Test
    fun `every spelling of a licence lands in one family`() = runTest(dispatcher) {
        coEvery { useCases.getLibraries() } returns listOf(
            compose,
            library(4, "Okhttp", "com.squareup.okhttp3:okhttp", "Square", "Apache-2.0"),
            library(
                5, "Timber", "com.jakewharton.timber:timber", "Jake Wharton",
                "The Apache Software License, Version 2.0",
            ),
        )

        val state = loadedViewModel().listState.value

        // The defect this pins: grouping by the declared name put one licence in three sections.
        assertThat(state.sections).hasSize(1)
        assertThat(state.familyCounts)
            .containsExactly(LicenseFamilyCount(LicenseFamily.APACHE_2, 3))
    }

    @Test
    fun `search matches the coordinate, not only the display name`() = runTest(dispatcher) {
        coEvery { useCases.getLibraries() } returns listOf(compose, adhan)
        val viewModel = loadedViewModel()

        // Nothing displayed says "batoulapps" — the name is "Adhan" — but it is what a
        // developer looking for the dependency would type.
        viewModel.onEvent(LicensesEvent.Search("batoulapps"))

        val state = viewModel.listState.value
        assertThat(state.sections.flatMap { it.libraries }).containsExactly(adhan)
        assertThat(state.visibleCount).isEqualTo(1)
        assertThat(state.totalCount).isEqualTo(2)
    }

    @Test
    fun `a filter narrows the list but leaves the chip counts alone`() = runTest(dispatcher) {
        coEvery { useCases.getLibraries() } returns listOf(compose, adhan, amiri)
        val viewModel = loadedViewModel()

        viewModel.onEvent(LicensesEvent.SelectFamily(LicenseFamily.OFL))

        val state = viewModel.listState.value
        assertThat(state.sections.flatMap { it.libraries }).containsExactly(amiri)
        // Counts are over the whole list: a chip whose number moved when you pressed it
        // would be unreadable.
        assertThat(state.familyCounts.map { it.count }).containsExactly(1, 1, 1)
    }

    @Test
    fun `a query that matches nothing is an empty result, not an empty list`() =
        runTest(dispatcher) {
            coEvery { useCases.getLibraries() } returns listOf(compose)
            val viewModel = loadedViewModel()

            viewModel.onEvent(LicensesEvent.Search("nothing here"))

            val state = viewModel.listState.value
            assertThat(state.sections).isEmpty()
            // The screen must offer "nothing matches", not the load-failed error state.
            assertThat(state.isEmptyResult).isTrue()
            assertThat(state.error).isNull()
        }

    @Test
    fun `toggling the grouping re-sections without losing the filter`() = runTest(dispatcher) {
        coEvery { useCases.getLibraries() } returns listOf(compose, adhan, amiri)
        val viewModel = loadedViewModel()
        viewModel.onEvent(LicensesEvent.SelectFamily(LicenseFamily.APACHE_2))

        viewModel.onEvent(LicensesEvent.ToggleGrouping)

        val state = viewModel.listState.value
        assertThat(state.grouping).isEqualTo(LicenseGrouping.ALPHABETICAL)
        assertThat(state.sections.map { it.letter }).containsExactly("C")
        assertThat(state.selectedFamily).isEqualTo(LicenseFamily.APACHE_2)
    }

    @Test
    fun `a library declaring no licence is grouped, not dropped`() = runTest(dispatcher) {
        coEvery { useCases.getLibraries() } returns listOf(
            compose,
            library(9, "Mystery", "com.example:mystery", author = null, licenseName = null),
        )

        val state = loadedViewModel().listState.value

        assertThat(state.sections.flatMap { it.libraries }).hasSize(2)
        assertThat(state.familyCounts.map { it.family })
            .containsExactly(LicenseFamily.APACHE_2, LicenseFamily.OTHER)
    }

    private fun kotlinx.coroutines.test.TestScope.loadedViewModel(): LicensesViewModel {
        val viewModel = LicensesViewModel(useCases, telemetry)
        viewModel.onEvent(LicensesEvent.LoadLibraries)
        advanceUntilIdle()
        return viewModel
    }

    @Test
    fun `a library the id does not match is not found, not a crash`() = runTest(dispatcher) {
        coEvery { useCases.getLibrary(any()) } returns null

        val viewModel = LicensesViewModel(useCases, telemetry)
        viewModel.onEvent(LicensesEvent.LoadLibrary(404))
        advanceUntilIdle()

        val state = viewModel.detailState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.library).isNull()
        // A missing id is a real answer, not a thrown failure — so nothing is reported
        // to telemetry, but the reader is still told.
        assertThat(state.error?.kind).isEqualTo(NimazErrorKind.NOT_FOUND)
        assertThat(telemetry.errors).isEmpty()
    }

    @Test
    fun `a found library clears the not-found error from a previous lookup`() = runTest(dispatcher) {
        coEvery { useCases.getLibrary(404) } returns null
        coEvery { useCases.getLibrary(1) } returns compose

        val viewModel = LicensesViewModel(useCases, telemetry)
        viewModel.onEvent(LicensesEvent.LoadLibrary(404))
        advanceUntilIdle()
        viewModel.onEvent(LicensesEvent.LoadLibrary(1))
        advanceUntilIdle()

        val state = viewModel.detailState.value
        assertThat(state.library).isEqualTo(compose)
        assertThat(state.error).isNull()
    }
}
