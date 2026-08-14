package com.arshadshah.nimaz.presentation.viewmodel.location

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.Telemetry
import com.arshadshah.nimaz.core.monitoring.launchSafely
import com.arshadshah.nimaz.domain.model.SearchLocation
import com.arshadshah.nimaz.domain.model.isLocationSet
import com.arshadshah.nimaz.domain.repository.DeviceLocationRepository
import com.arshadshah.nimaz.domain.repository.PermissionChecker
import com.arshadshah.nimaz.domain.repository.settings.LocationSettings
import com.arshadshah.nimaz.domain.usecase.PrayerUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class LocationViewModel @Inject constructor(
    private val deviceLocation: DeviceLocationRepository,
    private val permissions: PermissionChecker,
    private val locationSettings: LocationSettings,
    private val prayerUseCases: PrayerUseCases,
    private val telemetry: Telemetry,
) : ViewModel() {

    private val _state = MutableStateFlow(LocationUiState())
    val state: StateFlow<LocationUiState> = _state.asStateFlow()

    init {
        loadCurrentLocation()
        loadRecentLocations()
    }

    fun onEvent(event: LocationEvent) {
        when (event) {
            is LocationEvent.UpdateSearchQuery -> {
                _state.update { it.copy(searchQuery = event.query) }
                if (event.query.length >= 2) {
                    searchLocations(event.query)
                } else {
                    _state.update { it.copy(searchResults = emptyList()) }
                }
            }

            LocationEvent.Search -> {
                telemetry.featureUsed(DOMAIN, "search")
                searchLocations(_state.value.searchQuery)
            }

            LocationEvent.ClearSearch -> _state.update {
                it.copy(searchQuery = "", searchResults = emptyList())
            }

            is LocationEvent.SelectRegion -> {
                telemetry.featureUsed(DOMAIN, "filter_region")
                _state.update { it.copy(selectedRegion = event.region) }
            }

            is LocationEvent.SelectLocation -> {
                telemetry.featureUsed(DOMAIN, "select_location")
                selectLocation(event.location)
            }

            LocationEvent.UseCurrentGpsLocation -> {
                telemetry.featureUsed(DOMAIN, "use_gps")
                detectCurrentLocation()
            }

            LocationEvent.LoadCurrentLocation -> loadCurrentLocation()
            LocationEvent.DismissError -> _state.update { it.copy(error = null) }
        }
    }

    private fun loadCurrentLocation() {
        launchSafely(telemetry, DOMAIN, "load_current_location") {
            try {
                val prefs = locationSettings.userPreferences.first()
                if (isLocationSet(prefs.latitude, prefs.longitude)) {
                    _state.update {
                        it.copy(
                            currentLocation = CurrentLocationState.Set(
                                name = prefs.locationName.ifEmpty { "Current Location" },
                                latitude = prefs.latitude,
                                longitude = prefs.longitude
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                telemetry.failure(DOMAIN, "load_current", e)
                // Silently fail - location not set
            }
        }
    }

    private fun loadRecentLocations() {
        launchSafely(telemetry, DOMAIN, "load_recent_locations") {
            try {
                // Ordered by the database, newest first. Taking the first five of
                // `getAllLocations()` — which sorts `isFavorite DESC, name ASC` — produced an
                // alphabetical "recent" row that a newly saved location never entered.
                prayerUseCases.getRecentLocations().collect { locations ->
                    val recentLocations = locations
                        .map { location ->
                            SearchLocation(
                                name = location.name,
                                country = location.country ?: "",
                                latitude = location.latitude,
                                longitude = location.longitude
                            )
                        }
                        // Deduplicate by coordinates rounded to 3 decimal places (~110m)
                        .distinctBy { loc ->
                            val roundedLat = "%.3f".format(loc.latitude)
                            val roundedLng = "%.3f".format(loc.longitude)
                            "$roundedLat,$roundedLng"
                        }
                        .take(5)
                    _state.update { it.copy(recentLocations = recentLocations) }
                }
            } catch (e: Exception) {
                telemetry.failure(DOMAIN, "load_recent", e)
                // Silently fail
            }
        }
    }

    /**
     * The in-flight geocode.
     *
     * `UpdateSearchQuery` fires per keystroke, and each call launched an unhandled coroutine, so
     * typing "london" put six network geocodes in the air with no ordering between them —
     * whichever resolved last won. The `lon` results (Lonavla, Long Beach) landing after the
     * `london` ones left the list describing a query the user had already finished typing, and
     * `isSearching` flickered false as soon as *any* of them returned. AP-7.1b, already fixed
     * this way in `HadithViewModel`.
     */
    private var searchJob: Job? = null

    private fun searchLocations(query: String) {
        if (query.length < 2) return

        searchJob?.cancel()
        searchJob = launchSafely(telemetry, DOMAIN, "search_locations") {
            // Inside the cancellable block, so it debounces and cancel-and-replaces with one
            // mechanism: a keystroke within the window kills the previous coroutine before it
            // has issued anything at all.
            delay(SEARCH_DEBOUNCE_MS)
            _state.update { it.copy(isSearching = true) }
            try {
                // No `withContext(Dispatchers.IO)` here any more. The geocode is dispatched
                // by `AndroidDeviceLocationRepository`, which is where the knowledge that it
                // blocks belongs — and a ViewModel that hardcodes a real dispatcher cannot be
                // driven by a test scheduler, which is half of why this debounce shipped
                // untested.
                val results = searchWithGeocoder(query)
                _state.update { it.copy(searchResults = results, isSearching = false) }
            } catch (e: Exception) {
                telemetry.failure(DOMAIN, "search", e)
                _state.update {
                    it.copy(
                        isSearching = false,
                        error = "Failed to search locations: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * What places match what the user typed.
     *
     * This was forty lines: build a `Geocoder` from an injected `Context`, branch on API 33,
     * wrap the listener form in `suspendCancellableCoroutine`, then flatten each `Address`
     * through a four-way name fallback — all of it duplicated verbatim in
     * `OnboardingViewModel`. It lives in `AndroidDeviceLocationRepository` now, so this asks a
     * question and the ViewModel can be constructed in a JVM test.
     */
    private suspend fun searchWithGeocoder(query: String): List<SearchLocation> =
        try {
            deviceLocation.search(query).distinctBy { "${it.name}, ${it.country}" }
        } catch (e: Exception) {
            telemetry.failure(DOMAIN, "geocode_search", e)
            emptyList()
        }

    private fun selectLocation(location: SearchLocation) {
        launchSafely(telemetry, DOMAIN, "select_location") {
            try {
                // Save to DataStore
                locationSettings.updateLocation(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    name = "${location.name}, ${location.country}"
                )

                // One transaction: clear the flag everywhere, then insert this place or
                // refresh the row already at these coordinates. Composing a `Location` here —
                // with `id = 0` against an autogenerate primary key — is what made every
                // selection insert a duplicate and left several rows flagged current.
                prayerUseCases.saveCurrentLocation(
                    name = location.name,
                    country = location.country,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    timezone = TimeZone.getDefault().id,
                )

                // Update state
                _state.update {
                    it.copy(
                        currentLocation = CurrentLocationState.Set(
                            name = "${location.name}, ${location.country}",
                            latitude = location.latitude,
                            longitude = location.longitude
                        ),
                        searchQuery = "",
                        searchResults = emptyList()
                    )
                }
            } catch (e: Exception) {
                telemetry.failure(DOMAIN, "select_location", e)
                _state.update { it.copy(error = "Failed to save location: ${e.message}") }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun detectCurrentLocation() {
        if (!hasLocationPermission()) {
            _state.update { it.copy(error = "Location permission not granted") }
            return
        }

        launchSafely(telemetry, DOMAIN, "detect_current_location") {
            _state.update { it.copy(isLoadingGps = true) }
            try {
                val location = getCurrentLocation()
                if (location != null) {
                    // Reverse geocode to get location name
                    val locationName = reverseGeocode(location.first, location.second)

                    // Save location
                    locationSettings.updateLocation(
                        latitude = location.first,
                        longitude = location.second,
                        name = locationName
                    )

                    _state.update {
                        it.copy(
                            currentLocation = CurrentLocationState.Set(
                                name = locationName,
                                latitude = location.first,
                                longitude = location.second
                            ),
                            isLoadingGps = false
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            isLoadingGps = false,
                            error = "Could not detect location"
                        )
                    }
                }
            } catch (e: Exception) {
                telemetry.failure(DOMAIN, "detect_gps", e)
                _state.update {
                    it.copy(
                        isLoadingGps = false,
                        error = "Failed to detect location: ${e.message}"
                    )
                }
            }
        }
    }

    private fun hasLocationPermission(): Boolean = permissions.hasLocationPermission()

    private suspend fun getCurrentLocation(): Pair<Double, Double>? =
        deviceLocation.currentCoordinates()?.let { it.latitude to it.longitude }

    private suspend fun reverseGeocode(latitude: Double, longitude: Double): String =
        try {
            deviceLocation.reverseGeocode(latitude, longitude) ?: UNKNOWN_LOCATION
        } catch (e: Exception) {
            telemetry.failure(DOMAIN, "reverse_geocode", e)
            UNKNOWN_LOCATION
        }
}

/** Long enough that a fast typist issues one geocode, short enough to feel immediate. */
private const val SEARCH_DEBOUNCE_MS = 300L

/** The analytics feature name for everything in this ViewModel. */
private const val DOMAIN = AppAnalytics.Feature.LOCATION

/** Shown when the geocoder has nothing to call a place. */
private const val UNKNOWN_LOCATION = "Unknown Location"
