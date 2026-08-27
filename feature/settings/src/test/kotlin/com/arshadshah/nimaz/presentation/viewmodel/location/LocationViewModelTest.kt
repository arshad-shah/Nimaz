package com.arshadshah.nimaz.presentation.viewmodel.location

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.model.Location
import com.arshadshah.nimaz.domain.model.SearchLocation
import com.arshadshah.nimaz.domain.model.UserPreferences
import com.arshadshah.nimaz.domain.repository.Coordinates
import com.arshadshah.nimaz.domain.repository.DeviceLocationRepository
import com.arshadshah.nimaz.domain.repository.PermissionChecker
import com.arshadshah.nimaz.domain.repository.PrayerRepository
import com.arshadshah.nimaz.domain.repository.settings.LocationSettings
import com.arshadshah.nimaz.domain.usecase.PrayerUseCases
import com.arshadshah.nimaz.domain.usecase.buildPrayerUseCases
import com.arshadshah.nimaz.testing.testLocation
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Everything the location ViewModel does apart from the debounce, which
 * `LocationSearchDebounceTest` already owns.
 *
 * Two things carry real weight here.
 *
 * **Selecting a place writes twice, and both writes matter.** The preference is what the prayer
 * calculator reads; the database row is what the recents list and the location table are built
 * from. Writing only one leaves the app calculating for a city the settings screen does not show,
 * or showing a city the calculator has never heard of. It also goes through
 * `saveCurrentLocation` rather than composing a `Location` here — the previous version built one
 * with `id = 0` against an autogenerate key, which inserted a duplicate on every selection and
 * left several rows flagged current at once.
 *
 * **Every failure surfaces.** These four paths — load, search, select, detect — all touch the
 * network, the Geocoder or DataStore, and each one's `catch` decides whether the user sees an
 * explanation or a screen that quietly does nothing. Two of them are deliberately silent (the
 * initial load, and a geocode that returns nothing), and the difference is worth pinning: a
 * failed *load* leaves the previous value on screen, while a failed *save* must not.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LocationViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val telemetry = RecordingTelemetry()

    private val preferences = MutableStateFlow(
        UserPreferences(
            onboardingCompleted = true,
            themeMode = "system",
            dynamicColor = false,
            appLanguage = "en",
            calculationMethod = "MUSLIM_WORLD_LEAGUE",
            asrCalculation = "standard",
            latitude = 0.0,
            longitude = 0.0,
            locationName = "",
            prayerNotificationsEnabled = true,
            quranTranslatorId = "sahih_international",
            showTranslation = true,
        )
    )

    private val locationSettings = mockk<LocationSettings>(relaxed = true) {
        every { userPreferences } returns this@LocationViewModelTest.preferences
    }

    private val recents = MutableStateFlow<List<Location>>(emptyList())
    private val prayerRepository = mockk<PrayerRepository>(relaxed = true) {
        every { getRecentLocations(any()) } returns this@LocationViewModelTest.recents
    }
    private val prayerUseCases: PrayerUseCases = buildPrayerUseCases(prayerRepository)

    private var coordinates: Coordinates? = null
    private var reverseName: String? = "London, United Kingdom"
    private var searchResults: List<SearchLocation> = emptyList()
    private var searchThrows: Throwable? = null
    private var reverseThrows: Throwable? = null

    private val deviceLocation = object : DeviceLocationRepository {
        override suspend fun currentCoordinates(): Coordinates? = coordinates

        override suspend fun search(query: String, limit: Int): List<SearchLocation> {
            searchThrows?.let { throw it }
            return searchResults
        }

        override suspend fun reverseGeocode(latitude: Double, longitude: Double): String? {
            reverseThrows?.let { throw it }
            return reverseName
        }
    }

    private var permissionGranted = true
    private val permissions = object : PermissionChecker {
        override fun hasLocationPermission() = permissionGranted
        override fun hasNotificationPermission() = true
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = LocationViewModel(
        deviceLocation,
        permissions,
        locationSettings,
        prayerUseCases,
        telemetry,
    )

    private val london = SearchLocation("London", "United Kingdom", 51.5074, -0.1278)

    // ── Loading what is already stored ───────────────────────────────────────────────────────

    @Test
    fun `a stored location is loaded on open`() = runTest(dispatcher) {
        preferences.value = preferences.value.copy(
            latitude = 51.5074,
            longitude = -0.1278,
            locationName = "London, United Kingdom",
        )

        val vm = viewModel()
        advanceUntilIdle()

        val current = vm.state.value.currentLocation as CurrentLocationState.Set
        assertThat(current.name).isEqualTo("London, United Kingdom")
        assertThat(current.latitude).isEqualTo(51.5074)
    }

    @Test
    fun `coordinates of zero are treated as no location, not as the Atlantic`() = runTest(dispatcher) {
        // (0, 0) is what an unset preference reads back as. Honouring it would open the screen
        // claiming a location in the Gulf of Guinea.
        val vm = viewModel()
        advanceUntilIdle()

        assertThat(vm.state.value.currentLocation).isEqualTo(CurrentLocationState.NotSet)
    }

    @Test
    fun `a location stored without a name still gets one`() = runTest(dispatcher) {
        // The GPS path can store coordinates the Geocoder could not name. An empty title in the
        // card reads as a rendering failure.
        preferences.value = preferences.value.copy(
            latitude = 51.5,
            longitude = -0.12,
            locationName = "",
        )

        val vm = viewModel()
        advanceUntilIdle()

        assertThat((vm.state.value.currentLocation as CurrentLocationState.Set).name)
            .isEqualTo("Current Location")
    }

    // ── Recents ──────────────────────────────────────────────────────────────────────────────

    @Test
    fun `recent locations are taken in the order the database returns them`() = runTest(dispatcher) {
        // Taking the first five of `getAllLocations()` — sorted `isFavorite DESC, name ASC` —
        // produced an alphabetical "recent" row that a newly-saved location never entered.
        recents.value = listOf(
            testLocation(id = 1, name = "Zurich", latitude = 47.37, longitude = 8.54),
            testLocation(id = 2, name = "Amman", latitude = 31.95, longitude = 35.93),
        )

        val vm = viewModel()
        advanceUntilIdle()

        assertThat(vm.state.value.recentLocations.map { it.name })
            .containsExactly("Zurich", "Amman").inOrder()
    }

    @Test
    fun `recents are capped at five`() = runTest(dispatcher) {
        recents.value = (1..9).map {
            testLocation(id = it.toLong(), name = "City $it", latitude = it * 1.0, longitude = it * 2.0)
        }

        val vm = viewModel()
        advanceUntilIdle()

        assertThat(vm.state.value.recentLocations).hasSize(5)
    }

    @Test
    fun `two rows for the same place appear once`() = runTest(dispatcher) {
        // Deduplicated on coordinates rounded to ~110m, not on the name: the Geocoder and the
        // curated catalogue spell one place differently, and a recents row showing "London" and
        // "London, Greater London" is two entries for one tap.
        recents.value = listOf(
            testLocation(id = 1, name = "London", latitude = 51.50741, longitude = -0.12781),
            testLocation(id = 2, name = "London, Greater London", latitude = 51.50742, longitude = -0.12782),
        )

        val vm = viewModel()
        advanceUntilIdle()

        assertThat(vm.state.value.recentLocations).hasSize(1)
    }

    @Test
    fun `a recent row with no country renders with an empty one rather than crashing`() =
        runTest(dispatcher) {
            recents.value = listOf(
                testLocation(id = 1, name = "Somewhere", country = null, latitude = 5.0, longitude = 6.0)
            )

            val vm = viewModel()
            advanceUntilIdle()

            assertThat(vm.state.value.recentLocations.single().country).isEmpty()
        }

    // ── Search ───────────────────────────────────────────────────────────────────────────────

    @Test
    fun `an explicit search runs the current query and records it`() = runTest(dispatcher) {
        searchResults = listOf(london)
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(LocationEvent.UpdateSearchQuery("london"))
        vm.onEvent(LocationEvent.Search)
        advanceUntilIdle()

        assertThat(vm.state.value.searchResults).containsExactly(london)
        assertThat(telemetry.featureUsages.map { it.action }).contains("search")
    }

    @Test
    fun `the geocoder returning the same place twice yields one row`() = runTest(dispatcher) {
        // Distinct on "name, country", because the Geocoder happily returns a street, a
        // district and a city that all flatten to the same label.
        searchResults = listOf(london, london.copy(latitude = 51.6))
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(LocationEvent.UpdateSearchQuery("london"))
        advanceUntilIdle()

        assertThat(vm.state.value.searchResults).hasSize(1)
    }

    @Test
    fun `a geocoder that throws leaves an empty list rather than an error`() = runTest(dispatcher) {
        // Deliberately quiet: a failed geocode is a search that found nothing, and a red banner
        // on every keystroke over a flaky connection is worse than an empty list.
        searchThrows = IllegalStateException("geocoder unavailable")
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(LocationEvent.UpdateSearchQuery("london"))
        advanceUntilIdle()

        assertThat(vm.state.value.searchResults).isEmpty()
        assertThat(vm.state.value.error).isNull()
        assertThat(telemetry.errors.map { it.type }).contains("geocode_search")
    }

    @Test
    fun `clearing the search empties both the query and the results`() = runTest(dispatcher) {
        searchResults = listOf(london)
        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(LocationEvent.UpdateSearchQuery("london"))
        advanceUntilIdle()

        vm.onEvent(LocationEvent.ClearSearch)

        assertThat(vm.state.value.searchQuery).isEmpty()
        assertThat(vm.state.value.searchResults).isEmpty()
    }

    @Test
    fun `shortening the query below two characters clears the results`() = runTest(dispatcher) {
        // Otherwise the previous query's results stay on screen under a box that no longer
        // says what produced them.
        searchResults = listOf(london)
        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(LocationEvent.UpdateSearchQuery("london"))
        advanceUntilIdle()

        vm.onEvent(LocationEvent.UpdateSearchQuery("l"))
        advanceUntilIdle()

        assertThat(vm.state.value.searchResults).isEmpty()
    }

    // ── Selecting ────────────────────────────────────────────────────────────────────────────

    @Test
    fun `selecting a place writes the preference and the database row`() = runTest(dispatcher) {
        // Only one of the two leaves the calculator and the settings screen disagreeing about
        // where the user is.
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(LocationEvent.SelectLocation(london))
        advanceUntilIdle()

        coVerify {
            locationSettings.updateLocation(51.5074, -0.1278, "London, United Kingdom")
        }
        // Composed by the use case rather than here: the previous version built a `Location`
        // in the ViewModel with `id = 0` against an autogenerate key, which inserted a
        // duplicate on every selection and left several rows flagged current at once.
        val saved = slot<Location>()
        coVerify { prayerRepository.saveCurrentLocation(capture(saved), any()) }
        assertThat(saved.captured.name).isEqualTo("London")
        assertThat(saved.captured.country).isEqualTo("United Kingdom")
        assertThat(saved.captured.latitude).isEqualTo(51.5074)
        assertThat(saved.captured.longitude).isEqualTo(-0.1278)
    }

    @Test
    fun `selecting a place clears the search it was found through`() = runTest(dispatcher) {
        searchResults = listOf(london)
        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(LocationEvent.UpdateSearchQuery("london"))
        advanceUntilIdle()

        vm.onEvent(LocationEvent.SelectLocation(london))
        advanceUntilIdle()

        assertThat(vm.state.value.searchQuery).isEmpty()
        assertThat(vm.state.value.searchResults).isEmpty()
        assertThat((vm.state.value.currentLocation as CurrentLocationState.Set).name)
            .isEqualTo("London, United Kingdom")
    }

    @Test
    fun `a save that fails says so rather than showing the place as selected`() =
        runTest(dispatcher) {
            // The failure that must not be silent: the card would show the new city while every
            // prayer time still came from the old one.
            coEvery {
                locationSettings.updateLocation(any(), any(), any())
            } throws IllegalStateException("disk full")
            val vm = viewModel()
            advanceUntilIdle()

            vm.onEvent(LocationEvent.SelectLocation(london))
            advanceUntilIdle()

            assertThat(vm.state.value.error).contains("disk full")
            assertThat(vm.state.value.currentLocation).isEqualTo(CurrentLocationState.NotSet)
        }

    @Test
    fun `an error can be dismissed`() = runTest(dispatcher) {
        coEvery {
            locationSettings.updateLocation(any(), any(), any())
        } throws IllegalStateException("disk full")
        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(LocationEvent.SelectLocation(london))
        advanceUntilIdle()

        vm.onEvent(LocationEvent.DismissError)

        assertThat(vm.state.value.error).isNull()
    }

    // ── GPS ──────────────────────────────────────────────────────────────────────────────────

    @Test
    fun `detecting without permission says so and asks the device for nothing`() =
        runTest(dispatcher) {
            permissionGranted = false
            val vm = viewModel()
            advanceUntilIdle()

            vm.onEvent(LocationEvent.UseCurrentGpsLocation)
            advanceUntilIdle()

            assertThat(vm.state.value.error).contains("permission")
            assertThat(vm.state.value.isLoadingGps).isFalse()
        }

    @Test
    fun `a detected location is named, stored and shown`() = runTest(dispatcher) {
        coordinates = Coordinates(51.5074, -0.1278)
        reverseName = "London, United Kingdom"
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(LocationEvent.UseCurrentGpsLocation)
        advanceUntilIdle()

        coVerify { locationSettings.updateLocation(51.5074, -0.1278, "London, United Kingdom") }
        assertThat((vm.state.value.currentLocation as CurrentLocationState.Set).name)
            .isEqualTo("London, United Kingdom")
        assertThat(vm.state.value.isLoadingGps).isFalse()
    }

    @Test
    fun `a location the geocoder cannot name is still stored`() = runTest(dispatcher) {
        // Coordinates the Geocoder has no label for are still perfectly good for a prayer time.
        // Refusing to store them would leave someone in a remote place with no way to set one.
        coordinates = Coordinates(70.0, 25.0)
        reverseName = null
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(LocationEvent.UseCurrentGpsLocation)
        advanceUntilIdle()

        coVerify { locationSettings.updateLocation(70.0, 25.0, "Unknown Location") }
    }

    @Test
    fun `a reverse geocode that throws does not lose the detected coordinates`() =
        runTest(dispatcher) {
            reverseThrows = IllegalStateException("geocoder unavailable")
            coordinates = Coordinates(70.0, 25.0)
            val vm = viewModel()
            advanceUntilIdle()

            vm.onEvent(LocationEvent.UseCurrentGpsLocation)
            advanceUntilIdle()

            coVerify { locationSettings.updateLocation(70.0, 25.0, "Unknown Location") }
            assertThat(telemetry.errors.map { it.type }).contains("reverse_geocode")
        }

    @Test
    fun `a device that cannot fix a position says so and stops the spinner`() =
        runTest(dispatcher) {
            // Indoors, or with location services off at the OS level. A spinner that never
            // stops is the failure mode this replaces.
            coordinates = null
            val vm = viewModel()
            advanceUntilIdle()

            vm.onEvent(LocationEvent.UseCurrentGpsLocation)
            advanceUntilIdle()

            assertThat(vm.state.value.error).contains("Could not detect location")
            assertThat(vm.state.value.isLoadingGps).isFalse()
        }

    @Test
    fun `reloading the stored location is available as its own event`() = runTest(dispatcher) {
        // The screen re-reads after a permission grant, which changes what the OS will return
        // without anything on this side having changed.
        val vm = viewModel()
        advanceUntilIdle()
        preferences.value = preferences.value.copy(
            latitude = 30.0444,
            longitude = 31.2357,
            locationName = "Cairo, Egypt",
        )

        vm.onEvent(LocationEvent.LoadCurrentLocation)
        advanceUntilIdle()

        assertThat((vm.state.value.currentLocation as CurrentLocationState.Set).name)
            .isEqualTo("Cairo, Egypt")
    }

    @Test
    fun `filtering by region is recorded and reaches the state`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        val region = com.arshadshah.nimaz.domain.model.CityRegion.entries.first()

        vm.onEvent(LocationEvent.SelectRegion(region))

        assertThat(vm.state.value.selectedRegion).isEqualTo(region)
        assertThat(telemetry.featureUsages.map { it.action }).contains("filter_region")
    }

    @Test
    fun `clearing the region filter is also recorded`() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onEvent(LocationEvent.SelectRegion(com.arshadshah.nimaz.domain.model.CityRegion.entries.first()))

        vm.onEvent(LocationEvent.SelectRegion(null))

        assertThat(vm.state.value.selectedRegion).isNull()
    }
}
