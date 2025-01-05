package com.arshadshah.nimaz.viewModel

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.local.models.Parameters
import com.arshadshah.nimaz.repositories.LocationRepository
import com.arshadshah.nimaz.repositories.PrayerTimesRepository
import com.arshadshah.nimaz.repositories.UpdateState
import com.arshadshah.nimaz.services.LocationService
import com.arshadshah.nimaz.services.PrayerTimesService
import com.arshadshah.nimaz.services.UpdateService
import com.arshadshah.nimaz.utils.PrayerTimesParamMapper
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.widgets.prayertimesthin.PrayerTimeWorker
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sharedPreferences: PrivateSharedPreferences,
    private val locationRepository: LocationRepository,
    private val locationService: LocationService,
    private val updateService: UpdateService,
    private val prayerTimesService: PrayerTimesService
) : ViewModel() {

    // UI State
    data class UiState(
        val isLoading: Boolean = false,
        val error: String = "",
        val updateAvailable: Boolean = false,
        val isDarkMode: Boolean = false,
        val theme: String = AppConstants.THEME_SYSTEM,
        val updateState: UpdateState = UpdateState.Idle
    )

    // Location State
    data class LocationState(
        val isAuto: Boolean = false,
        val name: String = "",
        val latitude: Double = 0.0,
        val longitude: Double = 0.0,
        val isBatteryExempt: Boolean = false,
        val areNotificationsAllowed: Boolean = false
    )

    // Prayer Times State
    data class PrayerTimesState(
        val nextPrayerTime: LocalDateTime = LocalDateTime.now(),
        val fajrTime: LocalDateTime = LocalDateTime.now(),
        val sunriseTime: LocalDateTime = LocalDateTime.now(),
        val dhuhrTime: LocalDateTime = LocalDateTime.now(),
        val asrTime: LocalDateTime = LocalDateTime.now(),
        val maghribTime: LocalDateTime = LocalDateTime.now(),
        val ishaTime: LocalDateTime = LocalDateTime.now()
    )

    // StateFlows
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _locationState = MutableStateFlow(LocationState())
    val locationState: StateFlow<LocationState> = _locationState.asStateFlow()

    private val _prayerTimesState = MutableStateFlow(PrayerTimesState())
    val prayerTimesState: StateFlow<PrayerTimesState> = _prayerTimesState.asStateFlow()

    val locationName = locationState.map { it.name }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val latitude = locationState.map { it.latitude }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val longitude = locationState.map { it.longitude }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    // Initialize
    init {
        loadInitialState()
        viewModelScope.launch {
            updateService.updateState.collect { state ->
                _uiState.update { it.copy(updateState = state) }
            }
        }
        Log.d("Nimaz: SettingsViewModel", "SettingsViewModel initialized ${this.hashCode()}")
    }

    private fun loadInitialState() {
        viewModelScope.launch {
            // Load all states
            loadSettings()
            loadLocation(sharedPreferences.getDataBoolean(AppConstants.LOCATION_TYPE, false))
            loadPrayerTimes()
        }
    }


    // Part 2: Events and Event Handling

    sealed class SettingsEvent {
        // Location Events
        data class LocationToggle(val context: Context, val checked: Boolean) : SettingsEvent()
        data class LocationInput(val context: Context, val location: String) : SettingsEvent()
        data class Latitude(val context: Context, val latitude: Double) : SettingsEvent()
        data class Longitude(val context: Context, val longitude: Double) : SettingsEvent()
        data class LoadLocation(val context: Context) : SettingsEvent()

        // Permission and System Events
        data class BatteryExempt(val exempt: Boolean) : SettingsEvent()
        data class NotificationsAllowed(val allowed: Boolean) : SettingsEvent()

        // Theme Events
        data class Theme(val theme: String) : SettingsEvent()
        data class DarkMode(val darkMode: Boolean) : SettingsEvent()

        // Loading Events
        data object LoadSettings : SettingsEvent()
        data object LoadPrayerTimes : SettingsEvent()
        data class CheckUpdate(
            val activity: Activity,
            val updateType: Int = AppUpdateType.IMMEDIATE
        ) : SettingsEvent()

        data class StartUpdate(
            val activity: Activity,
            val requestCode: Int = AppConstants.APP_UPDATE_REQUEST_CODE,
            val updateType: Int = AppUpdateType.IMMEDIATE
        ) : SettingsEvent()

        data object RegisterUpdateListener : SettingsEvent()
        data object UnregisterUpdateListener : SettingsEvent()
    }

    fun handleEvent(event: SettingsEvent) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = "") }

                when (event) {
                    // Location Events
                    is SettingsEvent.LocationToggle -> handleLocationToggle(event)
                    is SettingsEvent.LocationInput -> handleLocationInput(event)
                    is SettingsEvent.Latitude -> handleLatitudeUpdate(event)
                    is SettingsEvent.Longitude -> handleLongitudeUpdate(event)
                    is SettingsEvent.LoadLocation -> handleLoadLocation(event)

                    // Permission Events
                    is SettingsEvent.BatteryExempt -> handleBatteryExempt(event)
                    is SettingsEvent.NotificationsAllowed -> handleNotificationsAllowed(event)

                    // Theme Events
                    is SettingsEvent.Theme -> handleTheme(event)
                    is SettingsEvent.DarkMode -> handleDarkMode(event)

                    // System Events
                    is SettingsEvent.CheckUpdate -> handleCheckUpdate(event)
                    is SettingsEvent.StartUpdate -> handleStartUpdate(event)
                    is SettingsEvent.RegisterUpdateListener -> handleRegisterUpdateListener()
                    is SettingsEvent.UnregisterUpdateListener -> handleUnregisterUpdateListener()

                    // Loading Events
                    SettingsEvent.LoadSettings -> loadSettings()
                    SettingsEvent.LoadPrayerTimes -> loadPrayerTimes()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Unknown error occurred") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun handleCheckUpdate(event: SettingsEvent.CheckUpdate) {
        updateService.checkForUpdate(event.updateType) { result ->
            result.onSuccess { isUpdateAvailable ->
                _uiState.update { it.copy(updateAvailable = isUpdateAvailable) }

                // If update is available and it's an immediate update, start the flow
                if (isUpdateAvailable && event.updateType == AppUpdateType.IMMEDIATE) {
                    handleEvent(
                        SettingsEvent.StartUpdate(
                            activity = event.activity,
                            updateType = event.updateType
                        )
                    )
                }
            }.onFailure { error ->
                _uiState.update { it.copy(error = "", updateAvailable = false) }
            }
        }
    }

    private suspend fun handleStartUpdate(event: SettingsEvent.StartUpdate) {
        updateService.startUpdateFlow(
            activity = event.activity,
            requestCode = event.requestCode,
            updateType = event.updateType
        ) { result ->
            result.onFailure { error ->
                _uiState.update { it.copy(error = error.message ?: "Update start failed") }
            }
        }
    }

    private fun handleRegisterUpdateListener() {
        updateService.registerInstallStateListener { statusCode ->
            when (statusCode.toString()) {
                InstallStatus.PENDING.toString() -> {
                    _uiState.update {
                        it.copy(
                            isLoading = true,
                            error = ""
                        )
                    }
                }

                InstallStatus.DOWNLOADING.toString() -> {
                    _uiState.update {
                        it.copy(
                            isLoading = true,
                            error = ""
                        )
                    }
                }

                InstallStatus.DOWNLOADED.toString() -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = ""
                        )
                    }
                }

                InstallStatus.INSTALLING.toString() -> {
                    _uiState.update {
                        it.copy(
                            isLoading = true,
                            error = ""
                        )
                    }
                }

                InstallStatus.INSTALLED.toString() -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            updateAvailable = false,
                            error = ""
                        )
                    }
                }

                InstallStatus.FAILED.toString() -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Update installation failed"
                        )
                    }
                }

                InstallStatus.CANCELED.toString() -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Update was canceled"
                        )
                    }
                }

                else -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Unknown update status"
                        )
                    }
                }
            }
        }
    }

    private fun handleUnregisterUpdateListener() {
        updateService.unregisterInstallStateListener()
    }

    override fun onCleared() {
        super.onCleared()
        handleUnregisterUpdateListener()
    }

    private suspend fun handleTheme(event: SettingsEvent.Theme) {
        _uiState.update { currentState ->
            currentState.copy(
                theme = event.theme
            )
        }
        sharedPreferences.saveData(AppConstants.THEME, event.theme)
    }

    private suspend fun handleDarkMode(event: SettingsEvent.DarkMode) {
        _uiState.update { currentState ->
            currentState.copy(
                isDarkMode = event.darkMode
            )
        }
        sharedPreferences.saveDataBoolean(AppConstants.DARK_MODE, event.darkMode)
    }

    // Example implementation of a few event handlers
    private suspend fun handleLocationToggle(event: SettingsEvent.LocationToggle) {
        sharedPreferences.saveDataBoolean(AppConstants.LOCATION_TYPE, event.checked)
        _locationState.update { it.copy(isAuto = event.checked) }
        loadLocation(event.checked)
    }

    private suspend fun handleLocationInput(event: SettingsEvent.LocationInput) {
        _locationState.update { it.copy(name = event.location) }
        sharedPreferences.saveData(AppConstants.LOCATION_INPUT, event.location)

        locationRepository.forwardGeocode(event.location)
            .onSuccess { locationData ->
                _locationState.update {
                    it.copy(
                        name = locationData.locationName,
                        latitude = locationData.latitude,
                        longitude = locationData.longitude
                    )
                }
                // Save to preferences
                with(sharedPreferences) {
                    saveDataDouble(AppConstants.LATITUDE, locationData.latitude)
                    saveDataDouble(AppConstants.LONGITUDE, locationData.longitude)
                    saveData(AppConstants.LOCATION_INPUT, locationData.locationName)
                }
            }
            .onFailure { error ->
                _uiState.update { it.copy(error = error.message ?: "Failed to get location") }
                // Load fallback values
                _locationState.update {
                    it.copy(
                        latitude = sharedPreferences.getDataDouble(AppConstants.LATITUDE, 53.3498),
                        longitude = sharedPreferences.getDataDouble(AppConstants.LONGITUDE, -6.2603)
                    )
                }
            }
    }

    private suspend fun handleLatitudeUpdate(event: SettingsEvent.Latitude) {
        _locationState.update { it.copy(latitude = event.latitude) }
        sharedPreferences.saveDataDouble(AppConstants.LATITUDE, event.latitude)
        loadLocation(sharedPreferences.getDataBoolean(AppConstants.LOCATION_TYPE, true))
        updateWidget(context)
        updatePrayerTimes(PrayerTimesParamMapper.getParams(context = context))
        loadPrayerTimes()
    }

    private suspend fun handleLongitudeUpdate(event: SettingsEvent.Longitude) {
        _locationState.update { it.copy(longitude = event.longitude) }
        sharedPreferences.saveDataDouble(AppConstants.LONGITUDE, event.longitude)
        loadLocation(sharedPreferences.getDataBoolean(AppConstants.LOCATION_TYPE, true))
        updateWidget(context)
        updatePrayerTimes(PrayerTimesParamMapper.getParams(context = context))
        loadPrayerTimes()
    }

    private suspend fun handleLoadLocation(event: SettingsEvent.LoadLocation) {
        loadLocation(sharedPreferences.getDataBoolean(AppConstants.LOCATION_TYPE, true))
    }

    private suspend fun handleBatteryExempt(event: SettingsEvent.BatteryExempt) {
        _locationState.update { it.copy(isBatteryExempt = event.exempt) }
        sharedPreferences.saveDataBoolean(AppConstants.BATTERY_OPTIMIZATION, event.exempt)
    }

    private suspend fun handleNotificationsAllowed(event: SettingsEvent.NotificationsAllowed) {
        _locationState.update { it.copy(areNotificationsAllowed = event.allowed) }
        sharedPreferences.saveDataBoolean(AppConstants.NOTIFICATION_ALLOWED, event.allowed)
    }

    private suspend fun loadSettings() {
        try {
            _uiState.update { it.copy(isLoading = true) }

            // Load Location Settings
            loadLocationSettings()

            // Load UI Settings
            loadUiSettings()

        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message ?: "Failed to load settings") }
        } finally {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun loadLocationSettings() {
        _locationState.update { currentState ->
            currentState.copy(
                isAuto = sharedPreferences.getDataBoolean(AppConstants.LOCATION_TYPE, false),
                name = sharedPreferences.getData(AppConstants.LOCATION_INPUT, ""),
                latitude = sharedPreferences.getDataDouble(AppConstants.LATITUDE, 53.3498),
                longitude = sharedPreferences.getDataDouble(AppConstants.LONGITUDE, -6.2603),
                isBatteryExempt = sharedPreferences.getDataBoolean(
                    AppConstants.BATTERY_OPTIMIZATION,
                    false
                ),
                areNotificationsAllowed = sharedPreferences.getDataBoolean(
                    AppConstants.NOTIFICATION_ALLOWED,
                    false
                )
            )
        }
    }

    private suspend fun loadUiSettings() {
        _uiState.update { currentState ->
            currentState.copy(
                theme = sharedPreferences.getData(AppConstants.THEME, AppConstants.THEME_SYSTEM),
                isDarkMode = sharedPreferences.getDataBoolean(AppConstants.DARK_MODE, false)
            )
        }
    }

    private suspend fun loadLocation(isAuto: Boolean) {
        try {
            _uiState.update { it.copy(isLoading = true) }

            locationService.loadLocation(isAuto)
                .onSuccess { location ->
                    _locationState.update { currentState ->
                        currentState.copy(
                            name = location.locationName,
                            latitude = location.latitude,
                            longitude = location.longitude
                        )
                    }
                    updateWidget(context)
                    // Reload prayer times with new location
                    updatePrayerTimes(PrayerTimesParamMapper.getParams(context = context))
                    loadPrayerTimes()
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            error = throwable.message ?: "Failed to load location"
                        )
                    }
                    loadFallbackLocation()
                }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message ?: "Unknown error occurred") }
            loadFallbackLocation()
        } finally {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun loadFallbackLocation() {
        _locationState.update { currentState ->
            currentState.copy(
                latitude = sharedPreferences.getDataDouble(AppConstants.LATITUDE, 53.3498),
                longitude = sharedPreferences.getDataDouble(AppConstants.LONGITUDE, -6.2603),
                name = sharedPreferences.getData(AppConstants.LOCATION_INPUT, "")
            )
        }
    }

    private suspend fun loadPrayerTimes() {
        try {
            _uiState.update { it.copy(isLoading = true) }

            val prayerTimes = prayerTimesService.getPrayerTimes()
            prayerTimes?.let { times ->
                _prayerTimesState.update { currentState ->
                    currentState.copy(
                        fajrTime = times.fajr ?: LocalDateTime.now(),
                        sunriseTime = times.sunrise ?: LocalDateTime.now(),
                        dhuhrTime = times.dhuhr ?: LocalDateTime.now(),
                        asrTime = times.asr ?: LocalDateTime.now(),
                        maghribTime = times.maghrib ?: LocalDateTime.now(),
                        ishaTime = times.isha ?: LocalDateTime.now(),
                    )
                }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message ?: "Failed to load prayer times") }
        } finally {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun updateWidget(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            PrayerTimeWorker.enqueue(context, true)
        }
    }

    private fun updatePrayerTimes(parameters: Parameters) = viewModelScope.launch {
        try {
            _uiState.update { it.copy(isLoading = true) }

            val response = withContext(Dispatchers.IO) {
                PrayerTimesRepository.updatePrayerTimes(parameters)
            }

            response.data?.let { data ->
                _prayerTimesState.update { currentState ->
                    currentState.copy(
                        fajrTime = data.fajr ?: LocalDateTime.now(),
                        sunriseTime = data.sunrise ?: LocalDateTime.now(),
                        dhuhrTime = data.dhuhr ?: LocalDateTime.now(),
                        asrTime = data.asr ?: LocalDateTime.now(),
                        maghribTime = data.maghrib ?: LocalDateTime.now(),
                        ishaTime = data.isha ?: LocalDateTime.now(),
                    )
                }

            } ?: run {
                _uiState.update { it.copy(error = "Failed to update prayer times. Data is null.") }
            }
        } finally {
        }
    }
}