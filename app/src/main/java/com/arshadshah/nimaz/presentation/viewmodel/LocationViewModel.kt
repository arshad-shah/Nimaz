package com.arshadshah.nimaz.presentation.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.data.local.datastore.PreferencesDataStore
import com.arshadshah.nimaz.domain.model.AsrCalculation
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.Location
import com.arshadshah.nimaz.domain.repository.PrayerRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class LocationUiState(
    val searchQuery: String = "",
    val searchResults: List<SearchLocation> = emptyList(),
    val currentLocation: CurrentLocationState = CurrentLocationState.NotSet,
    val recentLocations: List<SearchLocation> = emptyList(),
    val popularCities: List<SearchLocation> = defaultPopularCities,
    val isSearching: Boolean = false,
    val isLoadingGps: Boolean = false,
    val error: String? = null
)

sealed interface CurrentLocationState {
    data object NotSet : CurrentLocationState
    data object Loading : CurrentLocationState
    data class Set(
        val name: String,
        val latitude: Double,
        val longitude: Double
    ) : CurrentLocationState
}

data class SearchLocation(
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double
)

val defaultPopularCities = listOf(
    SearchLocation("Madinah", "Saudi Arabia", 24.4686, 39.6142),
    SearchLocation("Istanbul", "Turkey", 41.0082, 28.9784),
    SearchLocation("Cairo", "Egypt", 30.0444, 31.2357),
    SearchLocation("Kuala Lumpur", "Malaysia", 3.1390, 101.6869),
    SearchLocation("London", "United Kingdom", 51.5074, -0.1278),
    SearchLocation("New York", "United States", 40.7128, -74.0060),
    SearchLocation("Jakarta", "Indonesia", -6.2088, 106.8456),
    SearchLocation("Riyadh", "Saudi Arabia", 24.7136, 46.6753)
)

sealed interface LocationEvent {
    data class UpdateSearchQuery(val query: String) : LocationEvent
    data object Search : LocationEvent
    data object ClearSearch : LocationEvent
    data class SelectLocation(val location: SearchLocation) : LocationEvent
    data object UseCurrentGpsLocation : LocationEvent
    data object LoadCurrentLocation : LocationEvent
    data object DismissError : LocationEvent
}

@HiltViewModel
class LocationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesDataStore: PreferencesDataStore,
    private val prayerRepository: PrayerRepository
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
            LocationEvent.Search -> searchLocations(_state.value.searchQuery)
            LocationEvent.ClearSearch -> _state.update {
                it.copy(searchQuery = "", searchResults = emptyList())
            }
            is LocationEvent.SelectLocation -> selectLocation(event.location)
            LocationEvent.UseCurrentGpsLocation -> detectCurrentLocation()
            LocationEvent.LoadCurrentLocation -> loadCurrentLocation()
            LocationEvent.DismissError -> _state.update { it.copy(error = null) }
        }
    }

    private fun loadCurrentLocation() {
        viewModelScope.launch {
            try {
                val prefs = preferencesDataStore.userPreferences.first()
                if (prefs.latitude != 0.0 && prefs.longitude != 0.0) {
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
                // Silently fail - location not set
            }
        }
    }

    private fun loadRecentLocations() {
        viewModelScope.launch {
            try {
                prayerRepository.getAllLocations().collect { locations ->
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
            emptyList()
        }
    }

    private fun selectLocation(location: SearchLocation) {
        viewModelScope.launch {
            try {
                // Save to DataStore
                preferencesDataStore.updateLocation(
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
                prayerRepository.insertLocation(domainLocation)

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
                    preferencesDataStore.updateLocation(
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
            "Unknown Location"
        }
    }
}
