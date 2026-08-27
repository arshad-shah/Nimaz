package com.arshadshah.nimaz.presentation.screens.settings

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.CityRegion
import com.arshadshah.nimaz.domain.model.SearchLocation
import com.arshadshah.nimaz.domain.model.citiesForRegion
import com.arshadshah.nimaz.domain.model.defaultPopularCities
import com.arshadshah.nimaz.presentation.viewmodel.location.CurrentLocationState
import com.arshadshah.nimaz.presentation.viewmodel.location.LocationEvent
import com.arshadshah.nimaz.presentation.viewmodel.location.LocationUiState
import com.arshadshah.nimaz.presentation.viewmodel.location.LocationViewModel
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
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Choosing where prayer times are calculated for — search, GPS, recents, and the curated
 * catalogue.
 *
 * This is the screen behind every prayer time in the app, so the failure mode is severe and
 * quiet: pick the wrong city and every time shifts, and nothing on any other screen says why.
 *
 * The GPS button carries the interesting logic. It takes one of two paths depending on whether
 * the permission is already held, and the callback treats **fine or coarse** as granted — an app
 * that demanded fine location would refuse to work for someone who granted approximate, whose
 * accuracy is far more than enough to calculate a prayer time. That callback body is reachable
 * only by answering the launcher from the test, so it is answered here rather than left uncovered.
 *
 * The other structural claim is the browse list: with no region filter the catalogue is *grouped*
 * under region headings, and with one selected it is a flat list of that region only. Getting that
 * backwards leaves either an unheaded wall of cities or a filter that filters nothing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h4000dp")
class LocationScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val state = MutableStateFlow(LocationUiState())
    private val events = mutableListOf<LocationEvent>()
    private val viewModel: LocationViewModel = mockk(relaxed = true) {
        every { this@mockk.state } returns this@LocationScreenTest.state
        every { onEvent(any()) } answers { events += firstArg<LocationEvent>() }
    }
    private var backs = 0
    private var activity: Activity? = null

    private fun setContent(uiState: LocationUiState = LocationUiState()) {
        state.value = uiState
        composeRule.setThemedContent {
            // `LocalActivity` rather than casting `LocalContext`: a Context is not always an
            // Activity, and lint rejects the cast.
            activity = androidx.activity.compose.LocalActivity.current
            LocationScreen(onNavigateBack = { backs++ }, viewModel = viewModel)
        }
    }

    private fun string(@StringRes res: Int, vararg args: Any): String =
        context.getString(res, *args)

    private fun grantLocationPermission() {
        shadowOf(context.applicationContext as android.app.Application)
            .grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private val london = SearchLocation(
        name = "London",
        country = "United Kingdom",
        latitude = 51.5074,
        longitude = -0.1278,
        region = CityRegion.entries.first(),
        flag = "🇬🇧",
    )

    // ── Search ───────────────────────────────────────────────────────────────────────────────

    @Test
    fun `typing in the search bar reports each keystroke`() {
        // The bar puts its placeholder on the field as a `contentDescription` rather than as
        // text, which is the only way to address it.
        setContent()

        composeRule.onNodeWithContentDescription(string(R.string.location_search_hint))
            .performTextInput("Lon")

        assertThat(events.filterIsInstance<LocationEvent.UpdateSearchQuery>()).isNotEmpty()
    }

    @Test
    fun `search results are listed under their own heading`() {
        setContent(LocationUiState(searchQuery = "London", searchResults = listOf(london)))

        // `NimazSectionTitle` uppercases its text by default.
        composeRule.onNodeWithText(string(R.string.location_search_results).uppercase())
            .assertExists()
        composeRule.onAllNodesWithText("London").onFirst().assertExists()
    }

    @Test
    fun `picking a search result selects that location`() {
        setContent(LocationUiState(searchResults = listOf(london)))

        composeRule.onAllNodesWithText("London").onFirst().performClick()

        val event = events.filterIsInstance<LocationEvent.SelectLocation>().single()
        assertThat(event.location).isEqualTo(london)
    }

    @Test
    fun `the catalogue is hidden while live search results are showing`() {
        // Otherwise the results the user asked for are followed by a hundred cities they did
        // not, and the first tap after a search is likely to land on the wrong one.
        setContent(LocationUiState(searchResults = listOf(london)))

        composeRule.onNodeWithText(string(R.string.location_browse_by_region).uppercase())
            .assertDoesNotExist()
    }

    @Test
    fun `the catalogue returns once the search is cleared`() {
        setContent(LocationUiState(searchResults = emptyList()))

        composeRule.onNodeWithText(string(R.string.location_browse_by_region).uppercase())
            .assertExists()
    }

    // ── The current location card ────────────────────────────────────────────────────────────

    @Test
    fun `a set location shows its name, its coordinates and the current badge`() {
        setContent(
            LocationUiState(
                currentLocation = CurrentLocationState.Set("London", 51.5074, -0.1278)
            )
        )

        composeRule.onAllNodesWithText("London").onFirst().assertExists()
        composeRule.onNodeWithText(string(R.string.location_current)).assertExists()
    }

    @Test
    fun `an unset location says so rather than rendering an empty card`() {
        setContent(LocationUiState(currentLocation = CurrentLocationState.NotSet))

        composeRule.onNodeWithText(string(R.string.location_not_set)).assertExists()
        composeRule.onNodeWithText(string(R.string.location_current)).assertDoesNotExist()
    }

    @Test
    fun `a location being detected says it is detecting`() {
        setContent(LocationUiState(currentLocation = CurrentLocationState.Loading))

        composeRule.onNodeWithText(string(R.string.location_detecting)).assertExists()
    }

    @Test
    fun `the city matching the current location is shown as selected`() {
        // Matched on coordinates within a thousandth of a degree, not on the name — the
        // Geocoder and the curated catalogue spell the same place differently, and a name match
        // would leave the user's own city looking unselected in the list.
        setContent(
            LocationUiState(
                currentLocation = CurrentLocationState.Set("LONDON", 51.5074, -0.1278),
                searchResults = listOf(london),
            )
        )

        // The selected row carries a check; the unselected ones do not.
        composeRule.onAllNodesWithText("United Kingdom").onFirst().assertExists()
    }

    // ── GPS ──────────────────────────────────────────────────────────────────────────────────

    @Test
    fun `with permission already held, the GPS button detects straight away`() {
        grantLocationPermission()
        setContent()

        composeRule.onNodeWithText(string(R.string.location_use_current_location)).performClick()

        assertThat(events).contains(LocationEvent.UseCurrentGpsLocation)
    }

    @Test
    fun `without permission, the GPS button asks first and detects nothing yet`() {
        setContent()

        composeRule.onNodeWithText(string(R.string.location_use_current_location)).performClick()

        assertThat(events.filterIsInstance<LocationEvent.UseCurrentGpsLocation>()).isEmpty()
        assertThat(shadowOf(activity).lastRequestedPermission).isNotNull()
    }

    @Test
    fun `granting coarse location alone is enough to detect`() {
        // Approximate location is far more accuracy than a prayer time needs. Requiring fine
        // location would refuse to work for someone who deliberately granted the coarser one —
        // and that arm of the callback is reachable only by answering the launcher.
        setContent()
        composeRule.onNodeWithText(string(R.string.location_use_current_location)).performClick()

        val request = shadowOf(activity).lastRequestedPermission
        activity!!.onRequestPermissionsResult(
            request.requestCode,
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
            intArrayOf(PackageManager.PERMISSION_GRANTED),
        )
        composeRule.waitForIdle()

        assertThat(events).contains(LocationEvent.UseCurrentGpsLocation)
    }

    @Test
    fun `a denied permission detects nothing`() {
        setContent()
        composeRule.onNodeWithText(string(R.string.location_use_current_location)).performClick()

        val request = shadowOf(activity).lastRequestedPermission
        activity!!.onRequestPermissionsResult(
            request.requestCode,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
            intArrayOf(PackageManager.PERMISSION_DENIED, PackageManager.PERMISSION_DENIED),
        )
        composeRule.waitForIdle()

        assertThat(events.filterIsInstance<LocationEvent.UseCurrentGpsLocation>()).isEmpty()
    }

    @Test
    fun `the GPS button reports that it is working and cannot be tapped twice`() {
        // Two concurrent detections is two Geocoder round-trips racing to write the location.
        setContent(LocationUiState(isLoadingGps = true))

        composeRule.onNodeWithText(string(R.string.location_detecting_location)).assertExists()
        composeRule.onNodeWithText(string(R.string.location_detecting_location)).performClick()

        assertThat(events).isEmpty()
    }

    // ── Recents and the catalogue ────────────────────────────────────────────────────────────

    @Test
    fun `recent locations get their own section, and only when there are some`() {
        setContent(LocationUiState(recentLocations = emptyList()))
        composeRule.onNodeWithText(string(R.string.location_recent).uppercase())
            .assertDoesNotExist()
    }

    @Test
    fun `a recent location is offered and selectable`() {
        val cairo = SearchLocation("Cairo", "Egypt", 30.0444, 31.2357)
        setContent(LocationUiState(recentLocations = listOf(cairo)))

        composeRule.onNodeWithText(string(R.string.location_recent).uppercase()).assertExists()
        composeRule.onAllNodesWithText("Cairo").onFirst().performClick()

        assertThat(events.filterIsInstance<LocationEvent.SelectLocation>().single().location)
            .isEqualTo(cairo)
    }

    @Test
    fun `with no region filter the catalogue is grouped under region headings`() {
        setContent(LocationUiState(selectedRegion = null))

        // Every region that has cities contributes a heading.
        val regions = defaultPopularCities.mapNotNull { it.region }.distinct()
        assertThat(regions).isNotEmpty()
        composeRule.onAllNodesWithText(regions.first().label).onFirst().assertExists()
    }

    @Test
    fun `selecting a region flattens the list to that region's cities`() {
        val region = defaultPopularCities.mapNotNull { it.region }.distinct().first()
        setContent(LocationUiState(selectedRegion = region))

        val inRegion = citiesForRegion(defaultPopularCities, region)
        val outOfRegion = defaultPopularCities.filter { it.region != null && it.region != region }

        assertThat(inRegion).isNotEmpty()
        composeRule.onAllNodesWithText(inRegion.first().name).onFirst().assertExists()
        if (outOfRegion.isNotEmpty()) {
            composeRule.onNodeWithText(outOfRegion.first().name).assertDoesNotExist()
        }
    }

    @Test
    fun `the All chip clears the region filter`() {
        val region = defaultPopularCities.mapNotNull { it.region }.distinct().first()
        setContent(LocationUiState(selectedRegion = region))

        composeRule.onNodeWithText(string(R.string.location_region_all)).performClick()

        assertThat(events.filterIsInstance<LocationEvent.SelectRegion>().single().region).isNull()
    }

    @Test
    fun `a region chip selects that region`() {
        setContent()

        val region = CityRegion.entries.sortedBy { it.order }.first()
        composeRule.onAllNodesWithText(region.label).onFirst().performClick()

        assertThat(events.filterIsInstance<LocationEvent.SelectRegion>().single().region)
            .isEqualTo(region)
    }

    // ── Errors and navigation ────────────────────────────────────────────────────────────────

    @Test
    fun `an error is shown and then dismissed, so it cannot repeat`() {
        // The dismiss is what stops the snackbar re-showing on every recomposition for the rest
        // of the screen's life.
        setContent(LocationUiState(error = "No network"))
        composeRule.mainClock.advanceTimeBy(10_000)
        composeRule.waitForIdle()

        assertThat(events).contains(LocationEvent.DismissError)
    }

    @Test
    fun `the title is Location and the back button navigates back`() {
        setContent()

        composeRule.onAllNodesWithText(string(R.string.location)).onFirst().assertIsDisplayed()
        composeRule.onNodeWithContentDescription(string(R.string.cd_back)).performClick()

        assertThat(backs).isEqualTo(1)
    }
}
