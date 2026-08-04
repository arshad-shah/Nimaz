package com.arshadshah.nimaz.presentation.viewmodel.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.core.monitoring.AppAnalytics
import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.arshadshah.nimaz.domain.model.AsrCalculation
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.Location
import com.arshadshah.nimaz.domain.model.isLocationSet
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.arshadshah.nimaz.domain.usecase.PrayerUseCases
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import com.arshadshah.nimaz.domain.model.SearchLocation

@HiltViewModel
class LocationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val prayerUseCases: PrayerUseCases
) : ViewModel() {

    private val _state = MutableStateFlow(LocationUiState())
    val state: StateFlow<LocationUiState> = _state.asStateFlow()

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

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
                AppAnalytics.logFeatureUsed(AppAnalytics.Feature.LOCATION, "search")
                searchLocations(_state.value.searchQuery)
            }

            LocationEvent.ClearSearch -> _state.update {
                it.copy(searchQuery = "", searchResults = emptyList())
            }

            is LocationEvent.SelectRegion -> {
                AppAnalytics.logFeatureUsed(AppAnalytics.Feature.LOCATION, "filter_region")
                _state.update { it.copy(selectedRegion = event.region) }
            }

            is LocationEvent.SelectLocation -> {
                AppAnalytics.logFeatureUsed(AppAnalytics.Feature.LOCATION, "select_location")
                selectLocation(event.location)
            }

            LocationEvent.UseCurrentGpsLocation -> {
                AppAnalytics.logFeatureUsed(AppAnalytics.Feature.LOCATION, "use_gps")
                detectCurrentLocation()
            }

            LocationEvent.LoadCurrentLocation -> loadCurrentLocation()
            LocationEvent.DismissError -> _state.update { it.copy(error = null) }
        }
    }

    private fun loadCurrentLocation() {
        viewModelScope.launch {
            try {
                val prefs = settingsRepository.userPreferences.first()
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
                CrashReporter.recordException(e)
                AppAnalytics.logError(AppAnalytics.Feature.LOCATION, "load_current", e.message)
                // Silently fail - location not set
            }
        }
    }

    private fun loadRecentLocations() {
        viewModelScope.launch {
            try {
                prayerUseCases.getAllLocations().collect { locations ->
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
                CrashReporter.recordException(e)
                AppAnalytics.logError(AppAnalytics.Feature.LOCATION, "load_recent", e.message)
                // Silently fail
            }
        }
    }

    private fun searchLocations(query: String) {
        if (query.length < 2) return

        viewModelScope.launch {
            _state.update { it.copy(isSearching = true) }
            try {
                val results = withContext(Dispatchers.IO) {
                    searchWithGeocoder(query)
                }
                _state.update { it.copy(searchResults = results, isSearching = false) }
            } catch (e: Exception) {
                CrashReporter.recordException(e)
                AppAnalytics.logError(AppAnalytics.Feature.LOCATION, "search", e.message)
                _state.update {
                    it.copy(
                        isSearching = false,
                        error = "Failed to search locations: ${e.message}"
                    )
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun searchWithGeocoder(query: String): List<SearchLocation> {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocationName(query, 10) { addresses ->
                        continuation.resume(addresses)
                    }
                }
            } else {
                geocoder.getFromLocationName(query, 10) ?: emptyList()
            }

            addresses.mapNotNull { address ->
                val name = buildString {
                    address.locality?.let { append(it) }
                    if (isEmpty() && address.subAdminArea != null) {
                        append(address.subAdminArea)
                    }
                    if (isEmpty() && address.adminArea != null) {
                        append(address.adminArea)
                    }
                    if (isEmpty()) {
                        address.featureName?.let { append(it) }
                    }
                }

                if (name.isNotEmpty()) {
                    SearchLocation(
                        name = name,
                        country = address.countryName ?: "",
                        latitude = address.latitude,
                        longitude = address.longitude
                    )
                } else null
            }.distinctBy { "${it.name}, ${it.country}" }
        } catch (e: Exception) {
            CrashReporter.recordException(e)
            AppAnalytics.logError(AppAnalytics.Feature.LOCATION, "geocode_search", e.message)
            emptyList()
        }
    }

    private fun selectLocation(location: SearchLocation) {
        viewModelScope.launch {
            try {
                // Save to DataStore
                settingsRepository.updateLocation(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    name = "${location.name}, ${location.country}"
                )

                // Save to database for recent locations
                val domainLocation = Location(
                    id = 0,
                    name = location.name,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    timezone = TimeZone.getDefault().id,
                    country = location.country,
                    city = location.name,
                    isCurrentLocation = true,
                    isFavorite = false,
                    calculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE,
                    asrCalculation = AsrCalculation.STANDARD,
                    highLatitudeRule = null,
                    fajrAngle = null,
                    ishaAngle = null
                )
                prayerUseCases.insertLocation(domainLocation)

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
                CrashReporter.recordException(e)
                AppAnalytics.logError(AppAnalytics.Feature.LOCATION, "select_location", e.message)
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

        viewModelScope.launch {
            _state.update { it.copy(isLoadingGps = true) }
            try {
                val location = getCurrentLocation()
                if (location != null) {
                    // Reverse geocode to get location name
                    val locationName = withContext(Dispatchers.IO) {
                        reverseGeocode(location.first, location.second)
                    }

                    // Save location
                    settingsRepository.updateLocation(
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
                CrashReporter.recordException(e)
                AppAnalytics.logError(AppAnalytics.Feature.LOCATION, "detect_gps", e.message)
                _state.update {
                    it.copy(
                        isLoadingGps = false,
                        error = "Failed to detect location: ${e.message}"
                    )
                }
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocation(): Pair<Double, Double>? {
        return suspendCancellableCoroutine { continuation ->
            val cancellationTokenSource = CancellationTokenSource()

            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    continuation.resume(Pair(location.latitude, location.longitude))
                } else {
                    continuation.resume(null)
                }
            }.addOnFailureListener { e ->
                continuation.resumeWithException(e)
            }

            continuation.invokeOnCancellation {
                cancellationTokenSource.cancel()
            }
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun reverseGeocode(latitude: Double, longitude: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        continuation.resume(addresses)
                    }
                }
            } else {
                geocoder.getFromLocation(latitude, longitude, 1) ?: emptyList()
            }

            val address = addresses.firstOrNull()
            if (address != null) {
                buildString {
                    address.locality?.let { append(it) }
                    if (isEmpty() && address.subAdminArea != null) {
                        append(address.subAdminArea)
                    }
                    if (isEmpty() && address.adminArea != null) {
                        append(address.adminArea)
                    }
                    address.countryName?.let { country ->
                        if (isNotEmpty()) append(", ")
                        append(country)
                    }
                }.ifEmpty { "Unknown Location" }
            } else {
                "Unknown Location"
            }
        } catch (e: Exception) {
            CrashReporter.recordException(e)
            AppAnalytics.logError(AppAnalytics.Feature.LOCATION, "reverse_geocode", e.message)
            "Unknown Location"
        }
    }
}
