package com.arshadshah.nimaz.presentation.viewmodel.location

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.domain.model.SearchLocation
import com.arshadshah.nimaz.domain.repository.Coordinates
import com.arshadshah.nimaz.domain.repository.DeviceLocationRepository
import com.arshadshah.nimaz.domain.repository.PermissionChecker
import com.arshadshah.nimaz.domain.repository.PrayerRepository
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.usecase.buildPrayerUseCases
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * The location search debounces, and a slower earlier query cannot overwrite a newer one.
 *
 * #365 S5 describes the defect: `UpdateSearchQuery` fires per keystroke and each call launched an
 * unhandled coroutine, so typing "london" put six network geocodes in the air with no ordering
 * between them — whichever resolved last won. The `lon` results (Lonavla, Long Beach) landing
 * after the `london` ones left the list describing a query the user had already finished typing,
 * and `isSearching` flickered false as soon as *any* of them returned.
 *
 * **The fix shipped in #427 and was flagged as untested, because it could not be tested.**
 * `LocationViewModel` built a `Geocoder` and a `FusedLocationProviderClient` from an injected
 * `@ApplicationContext`, so it could not be constructed on the JVM at all. Extracting
 * `DeviceLocationRepository` is what makes this suite possible — the debounce is the same code
 * it was, and this is the first thing to look at it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LocationSearchDebounceTest {

    private val dispatcher = StandardTestDispatcher()

    /** Every query the geocoder was actually asked for, in order. */
    private val queriesIssued = mutableListOf<String>()

    /** Per-query artificial latency, so a test can make an earlier query resolve last. */
    private val latency = mutableMapOf<String, Long>()

    private val deviceLocation = object : DeviceLocationRepository {
        override suspend fun currentCoordinates(): Coordinates? = null

        override suspend fun search(query: String, limit: Int): List<SearchLocation> {
            queriesIssued += query
            latency[query]?.let { delay(it) }
            return listOf(
                SearchLocation(
                    name = "result-for-$query",
                    country = "Testland",
                    latitude = 1.0,
                    longitude = 2.0,
                ),
            )
        }

        override suspend fun reverseGeocode(latitude: Double, longitude: Double): String? = null
    }

    private val permissions = object : PermissionChecker {
        override fun hasLocationPermission() = true
        override fun hasNotificationPermission() = true
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = LocationViewModel(
        deviceLocation,
        permissions,
        mockk<SettingsRepository>(relaxed = true),
        buildPrayerUseCases(mockk<PrayerRepository>(relaxed = true)),
        RecordingTelemetry(),
    )

    /** Typing a word issues **one** geocode, for the whole word. */
    @Test
    fun `typing a word issues one geocode`() = runTest(dispatcher) {
        val vm = viewModel()

        "london".forEachIndexed { index, _ ->
            vm.onEvent(LocationEvent.UpdateSearchQuery("london".take(index + 1)))
            advanceTimeBy(50) // faster than the 300ms debounce, as a typist is
        }
        advanceUntilIdle()

        assertThat(queriesIssued).containsExactly("london")
    }

    /**
     * The ordering half, which the debounce alone does not give you.
     *
     * "lon" is made slow and "london" fast, so the earlier query resolves *after* the later one.
     * Cancel-and-replace is what stops it winning; without it the list would end up describing a
     * query the user had already finished typing.
     */
    @Test
    fun `a slower earlier query cannot overwrite a newer one`() = runTest(dispatcher) {
        latency["lon"] = 5_000

        val vm = viewModel()
        vm.onEvent(LocationEvent.UpdateSearchQuery("lon"))
        advanceTimeBy(400) // past the debounce, so "lon" is genuinely in flight
        vm.onEvent(LocationEvent.UpdateSearchQuery("london"))
        advanceUntilIdle()

        assertThat(vm.state.value.searchResults.map { it.name })
            .containsExactly("result-for-london")
    }

    /**
     * `isSearching` must not flicker false while a newer query is still pending. It went false
     * as soon as *any* in-flight geocode returned, which is what made the spinner blink under a
     * fast typist.
     */
    @Test
    fun `isSearching stays true until the surviving query resolves`() = runTest(dispatcher) {
        latency["london"] = 2_000

        val vm = viewModel()
        vm.onEvent(LocationEvent.UpdateSearchQuery("lon"))
        advanceTimeBy(400)
        vm.onEvent(LocationEvent.UpdateSearchQuery("london"))
        advanceTimeBy(400) // debounce elapsed, geocode issued, still in flight

        assertThat(vm.state.value.isSearching).isTrue()

        advanceUntilIdle()
        assertThat(vm.state.value.isSearching).isFalse()
    }

    /** A one-character query is not worth a network round trip. */
    @Test
    fun `a single character issues nothing`() = runTest(dispatcher) {
        val vm = viewModel()

        vm.onEvent(LocationEvent.UpdateSearchQuery("l"))
        advanceUntilIdle()

        assertThat(queriesIssued).isEmpty()
    }
}
