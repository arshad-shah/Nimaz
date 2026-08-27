package com.arshadshah.nimaz.presentation.screens.about

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.LibraryLicense
import com.arshadshah.nimaz.domain.model.LicenseFamily
import com.arshadshah.nimaz.domain.model.OpenSourceLibrary
import com.arshadshah.nimaz.presentation.viewmodel.UiError
import com.arshadshah.nimaz.presentation.viewmodel.about.LicenseGrouping
import com.arshadshah.nimaz.presentation.viewmodel.about.LicensesEvent
import com.arshadshah.nimaz.presentation.viewmodel.about.LicensesListUiState
import com.arshadshah.nimaz.presentation.viewmodel.about.LicensesViewModel
import com.arshadshah.nimaz.presentation.viewmodel.about.familyCounts
import com.arshadshah.nimaz.presentation.viewmodel.about.regrouped
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The licence list, driven from a supplied catalogue.
 *
 * **Not from the real plugin output, deliberately.** AboutLibraries reads the *applying project's*
 * runtime classpath, so it stays in `:app` — this module renders a catalogue it cannot itself
 * produce, and `LicenceCatalogueTest` over there floors the real entry count. What is testable
 * here is the rendering: that the screen shows what it is handed, in the sections the ViewModel
 * grouped, and that the three controls above the list (search, family filter, grouping toggle)
 * dispatch rather than reaching into the list themselves.
 *
 * Two shapes are worth the assertions. The **counts** on the credit card and the chips are read
 * off the *whole* list rather than the filtered one — a filter chip whose own number moves when
 * you use it is unreadable, and "12 of 272" above a list of twelve is the only thing telling a
 * reader the other 260 still exist. And **an empty result is not an empty screen**: query, filter
 * and load-failure all end with no rows, and the reader has to be able to tell which happened.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class LicensesScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val listState = MutableStateFlow(LicensesListUiState())
    private val events = mutableListOf<LicensesEvent>()

    private val viewModel: LicensesViewModel = mockk(relaxed = true) {
        every { this@mockk.listState } returns this@LicensesScreenTest.listState
        every { onEvent(any()) } answers { events += firstArg<LicensesEvent>() }
    }

    private val detailsOpened = mutableListOf<Int>()
    private var backs = 0

    private val compose = library(1, "Compose UI", "androidx.compose.ui:ui", "Google", "Apache License 2.0")
    private val core = library(2, "Core Ktx", "androidx.core:core-ktx", "Google", "Apache License 2.0")
    private val adhan = library(3, "Adhan", "com.batoulapps.adhan:adhan2", "Batoul Apps", "MIT License")
    private val amiri = library(4, "Amiri", "org.amirifont:amiri", "Khaled Hosny", "SIL Open Font License 1.1")

    private fun library(
        id: Int,
        name: String,
        coordinate: String,
        author: String?,
        licenseName: String,
        version: String? = "1.0.0",
    ) = OpenSourceLibrary(
        id = id,
        name = name,
        coordinate = coordinate,
        version = version,
        author = author,
        website = null,
        licenses = listOf(LibraryLicense(licenseName, url = null, content = null)),
    )

    private fun loaded(
        libraries: List<OpenSourceLibrary> = listOf(compose, core, adhan, amiri),
        query: String = "",
        selectedFamily: LicenseFamily? = null,
        grouping: LicenseGrouping = LicenseGrouping.BY_LICENCE,
    ) = LicensesListUiState(
        libraries = libraries,
        query = query,
        selectedFamily = selectedFamily,
        grouping = grouping,
        familyCounts = libraries.familyCounts(),
        isLoading = false,
    ).regrouped()

    private fun setContent() {
        composeRule.setThemedContent {
            LicensesScreen(
                onNavigateBack = { backs++ },
                onNavigateToDetail = { detailsOpened += it },
                viewModel = viewModel,
            )
        }
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    @Test
    fun `the screen asks for the catalogue as it opens`() {
        listState.value = loaded()
        setContent()

        assertThat(events).contains(LicensesEvent.LoadLibraries)
    }

    @Test
    fun `every supplied library is on the list, under its licence family`() {
        listState.value = loaded()
        setContent()

        composeRule.onNodeWithText("Compose UI").assertExists()
        composeRule.onNodeWithText("Core Ktx").assertExists()
        composeRule.onNodeWithText("Adhan").assertExists()
        composeRule.onNodeWithText("Amiri").assertExists()

        // Largest family first, so the screen opens on Apache rather than on whichever
        // one-library section sorts first alphabetically.
        composeRule.onAllNodesWithText(string(R.string.license_family_apache)).onFirst()
            .assertExists()
    }

    @Test
    fun `a row renders what the library published and nothing more`() {
        // Author, version and Maven group are each optional in AboutLibraries' output, and a
        // row that renders an empty line for a missing one is how a licence list starts
        // looking broken halfway down.
        val bare = OpenSourceLibrary(
            id = 9,
            name = "Timber",
            coordinate = "timber",
            version = null,
            author = null,
            website = null,
            licenses = listOf(LibraryLicense("MIT License", url = null, content = null)),
        )
        listState.value = loaded(libraries = listOf(bare))
        setContent()

        composeRule.onNodeWithText("Timber").assertExists()
        composeRule.onNodeWithText("Timber").performClick()

        assertThat(detailsOpened).containsExactly(9)
    }

    @Test
    fun `the totals count the whole catalogue, not the visible rows`() {
        listState.value = loaded(query = "adhan")
        setContent()

        // One row visible, four libraries, three licences — and the header says so.
        composeRule.onNodeWithText(string(R.string.licenses_subtitle_format, 4, 3)).assertExists()
        composeRule.onNodeWithText(string(R.string.licenses_visible_count_format, 1, 4)).assertExists()
    }

    @Test
    fun `a library row opens its detail by id`() {
        listState.value = loaded()
        setContent()

        composeRule.onNodeWithText("Adhan").performClick()

        // The stable id derived from the coordinate, not the row's position: a version bump
        // must not change which library a detail route points at.
        assertThat(detailsOpened).containsExactly(3)
    }

    @Test
    fun `typing dispatches a search rather than filtering in place`() {
        listState.value = loaded()
        setContent()

        composeRule.onNodeWithContentDescription(string(R.string.licenses_search_placeholder))
            .performTextInput("amiri")

        assertThat(events).contains(LicensesEvent.Search("amiri"))
    }

    @Test
    fun `a family chip filters to that family and the All chip clears it`() {
        listState.value = loaded()
        setContent()

        composeRule.onNodeWithText(
            string(R.string.licenses_chip_format, string(R.string.license_family_mit), 1)
        ).performClick()
        composeRule.onNodeWithText(
            string(R.string.licenses_chip_format, string(R.string.licenses_filter_all), 4)
        ).performClick()

        assertThat(events).containsAtLeast(
            LicensesEvent.SelectFamily(LicenseFamily.MIT),
            LicensesEvent.SelectFamily(null),
        ).inOrder()
    }

    @Test
    fun `tapping the selected family chip clears the filter rather than reselecting it`() {
        listState.value = loaded(selectedFamily = LicenseFamily.MIT)
        setContent()

        composeRule.onNodeWithText(
            string(R.string.licenses_chip_format, string(R.string.license_family_mit), 1)
        ).performClick()

        assertThat(events).contains(LicensesEvent.SelectFamily(null))
    }

    @Test
    fun `the filter row is hidden when everything carries one licence`() {
        // The common case for this app — a row of one chip is a control that cannot do anything.
        listState.value = loaded(libraries = listOf(compose, core))
        setContent()

        composeRule.onNodeWithText(string(R.string.licenses_filter_section)).assertDoesNotExist()
    }

    @Test
    fun `the grouping toggle offers the other arrangement`() {
        listState.value = loaded()
        setContent()

        composeRule.onNodeWithText(string(R.string.licenses_grouped_by_licence)).assertExists()
        composeRule.onNodeWithText(string(R.string.licenses_action_sort_alphabetically)).performClick()

        assertThat(events).contains(LicensesEvent.ToggleGrouping)
    }

    @Test
    fun `grouped alphabetically the sections are initials and the toggle reads the other way`() {
        listState.value = loaded(grouping = LicenseGrouping.ALPHABETICAL)
        setContent()

        composeRule.onNodeWithText(string(R.string.licenses_grouped_alphabetically)).assertExists()
        composeRule.onNodeWithText(string(R.string.licenses_action_group_by_licence)).assertExists()
        // "A" for Adhan and Amiri, "C" for the two Compose/Core entries.
        composeRule.onAllNodesWithText("A").onFirst().assertExists()
        composeRule.onAllNodesWithText("C").onFirst().assertExists()
    }

    @Test
    fun `a query that matches nothing says so, and keeps the search bar`() {
        listState.value = loaded(query = "zzzz")
        setContent()

        composeRule.onNodeWithText(string(R.string.licenses_no_matches_title)).assertExists()
        composeRule.onNodeWithContentDescription(string(R.string.licenses_search_placeholder))
            .assertExists()
        composeRule.onNodeWithText("Compose UI").assertDoesNotExist()
    }

    @Test
    fun `an empty catalogue is not the same as a query with no hits`() {
        // Nothing loaded at all: no rows, but nothing was narrowed either, so "no libraries
        // match" would be a claim about a search nobody ran.
        listState.value = LicensesListUiState(isLoading = false)
        setContent()

        composeRule.onNodeWithText(string(R.string.licenses_no_matches_title)).assertDoesNotExist()
    }

    @Test
    fun `a failed load offers a retry`() {
        listState.value = LicensesListUiState(
            isLoading = false,
            error = UiError(message = R.string.licenses_load_failed),
        )
        setContent()

        composeRule.onNodeWithText(string(R.string.licenses_load_failed)).assertExists()
        composeRule.onNodeWithText(string(R.string.try_again)).performClick()

        assertThat(events).contains(LicensesEvent.Retry)
    }

    @Test
    fun `the back arrow leaves`() {
        listState.value = loaded()
        setContent()

        composeRule.onNodeWithContentDescription(string(R.string.cd_back)).performClick()

        assertThat(backs).isEqualTo(1)
    }

    @Test
    fun `a catalogue still loading shows neither rows nor an empty state`() {
        composeRule.mainClock.autoAdvance = false
        listState.value = LicensesListUiState(isLoading = true)
        setContent()

        composeRule.onNodeWithText(string(R.string.licenses_no_matches_title)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.licenses_credit_title)).assertDoesNotExist()
    }
}
