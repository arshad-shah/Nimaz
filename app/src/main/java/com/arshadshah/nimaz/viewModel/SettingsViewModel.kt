package com.arshadshah.nimaz.viewModel

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.CHANNEL_DESC_TEST
import com.arshadshah.nimaz.constants.AppConstants.CHANNEL_TEST
import com.arshadshah.nimaz.constants.AppConstants.TEST_CHANNEL_ID
import com.arshadshah.nimaz.constants.AppConstants.TEST_NOTIFY_ID
import com.arshadshah.nimaz.constants.AppConstants.TEST_PI_REQUEST_CODE
import com.arshadshah.nimaz.data.local.models.Parameters
import com.arshadshah.nimaz.repositories.Location
import com.arshadshah.nimaz.repositories.PrayerTimesRepository
import com.arshadshah.nimaz.repositories.UpdateState
import com.arshadshah.nimaz.services.LocationService
import com.arshadshah.nimaz.services.LocationStateManager
import com.arshadshah.nimaz.services.PrayerTimesService
import com.arshadshah.nimaz.services.UpdateService
import com.arshadshah.nimaz.utils.NotificationHelper
import com.arshadshah.nimaz.utils.PrayerTimesParamMapper
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.ThemeDataStore
import com.arshadshah.nimaz.utils.alarms.Alarms
import com.arshadshah.nimaz.utils.alarms.CreateAlarms
import com.arshadshah.nimaz.widgets.prayertimesthin.PrayerTimeWorker
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sharedPreferences: PrivateSharedPreferences,
    private val locationService: LocationService,
    private val themeDataStore: ThemeDataStore,
    private val updateService: UpdateService,
    private val prayerTimesService: PrayerTimesService,
    private val prayerTimesRepository: PrayerTimesRepository,
    private val locationStateManager: LocationStateManager,
    private val createAlarms: CreateAlarms
) : ViewModel() {

    // UI State
    data class UiState(
        val isLoading: Boolean = false,
        val error: String = "",
        val updateAvailable: Boolean = false,
        val updateState: UpdateState = UpdateState.Idle,
        val location: String = "",
        val latitude: Double = 0.0,
        val longitude: Double = 0.0
    )

    // Location State
    data class LocationSettingsState(
        val isAuto: Boolean = false,
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

    val themeName = themeDataStore.themeFlow
    val isDarkMode = themeDataStore.darkModeFlow

    // StateFlows
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    val locationState = locationStateManager.locationState

    private val _locationSettingsState = MutableStateFlow(LocationSettingsState())
    val locationSettingsState: StateFlow<LocationSettingsState> =
        _locationSettingsState.asStateFlow()


    private val _prayerTimesState = MutableStateFlow(PrayerTimesState())

    // Initialize
    init {
        viewModelScope.launch {
            loadSettings()
            updateService.updateState.collect { state ->
                _uiState.update { it.copy(updateState = state) }
            }
        }
        Log.d("Nimaz: SettingsViewModel", "SettingsViewModel initialized ${this.hashCode()}")
    }


    // Part 2: Events and Event Handling

    sealed class SettingsEvent {
        // Location Events
        data class LocationToggle(val context: Context, val checked: Boolean) : SettingsEvent()
        data class LocationInput(val context: Context, val location: String) : SettingsEvent()

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
        data object ForceResetAlarms : SettingsEvent()
        data object SetTestAlarm : SettingsEvent()
    }

    fun handleEvent(event: SettingsEvent) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = "") }

                when (event) {
                    // Location Events
                    is SettingsEvent.LocationToggle -> handleLocationToggle(event)
                    is SettingsEvent.LocationInput -> handleLocationInput(event)

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
                    SettingsEvent.ForceResetAlarms -> handleForceResetAlarms()
                    SettingsEvent.SetTestAlarm -> handleSetTestAlarm()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Unknown error occurred") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun handleForceResetAlarms() {
        viewModelScope.launch {
            sharedPreferences.saveDataBoolean(AppConstants.ALARM_LOCK, false)
            val alarmLock =
                sharedPreferences.getDataBoolean(AppConstants.ALARM_LOCK, false)
            loadPrayerTimes()
            if (!alarmLock) {
                createAlarms.exact(
                    context,
                    _prayerTimesState.value.fajrTime,
                    _prayerTimesState.value.sunriseTime,
                    _prayerTimesState.value.dhuhrTime,
                    _prayerTimesState.value.asrTime,
                    _prayerTimesState.value.maghribTime,
                    _prayerTimesState.value.ishaTime
                )
                sharedPreferences.saveDataBoolean(AppConstants.ALARM_LOCK, true)
            }
        }
    }

    private fun handleSetTestAlarm() {
        viewModelScope.launch {
            val zuharAdhan =
                "android.resource://" + context.packageName + "/" + R.raw.zuhar
            //create notification channels
            val notificationHelper = NotificationHelper()
            //test channel
            notificationHelper.createNotificationChannel(
                context,
                NotificationManager.IMPORTANCE_MAX,
                true,
                CHANNEL_TEST,
                CHANNEL_DESC_TEST,
                TEST_CHANNEL_ID,
                zuharAdhan
            )
            val currentTime = LocalDateTime.now()
            val timeToNotify =
                currentTime.plusSeconds(10).atZone(ZoneId.systemDefault())
                    .toInstant().toEpochMilli()
            val testPendingIntent = createAlarms.createPendingIntent(
                context,
                TEST_PI_REQUEST_CODE,
                TEST_NOTIFY_ID,
                timeToNotify,
                "Test Adhan",
                TEST_CHANNEL_ID
            )
            Alarms().setExactAlarm(context, timeToNotify, testPendingIntent)
        }
    }

    private fun handleCheckUpdate(event: SettingsEvent.CheckUpdate) {
        updateService.checkForUpdate(event.updateType) { result ->
            result.onSuccess { isUpdateAvailable ->
                _uiState.update { it.copy(updateAvailable = isUpdateAvailable) }
            }.onFailure {
                _uiState.update { it.copy(error = "", updateAvailable = false) }
            }
        }
    }

    private fun handleStartUpdate(event: SettingsEvent.StartUpdate) {
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
        // Add location cleanup
        viewModelScope.launch {
            locationStateManager.cleanupRequest()
        }
    }

    private suspend fun handleTheme(event: SettingsEvent.Theme) {
        themeDataStore.updateTheme(event.theme)
    }

    // Modify handleDarkMode
    private suspend fun handleDarkMode(event: SettingsEvent.DarkMode) {
        themeDataStore.updateDarkMode(event.darkMode)
    }

    private suspend fun handleLocationToggle(event: SettingsEvent.LocationToggle) {
        sharedPreferences.saveDataBoolean(AppConstants.LOCATION_TYPE, event.checked)
        _locationSettingsState.update { it.copy(isAuto = event.checked) }
        loadLocation(true)
        val params = PrayerTimesParamMapper.getParams(context = context)
        updatePrayerTimes(params)
        handleForceResetAlarms()
        updateWidget(context)
    }

    private suspend fun handleLocationInput(event: SettingsEvent.LocationInput) {
        try {
            with(sharedPreferences) {
                saveData(AppConstants.LOCATION_INPUT, event.location)
            }
            locationService.loadLocation(false)
                .onSuccess { locationData ->
                    _uiState.update {
                        it.copy(
                            location = locationData.locationName,
                            latitude = locationData.latitude,
                            longitude = locationData.longitude
                        )
                    }
                    with(sharedPreferences) {
                        saveDataDouble(AppConstants.LATITUDE, locationData.latitude)
                        saveDataDouble(AppConstants.LONGITUDE, locationData.longitude)
                        saveData(AppConstants.LOCATION_INPUT, locationData.locationName)
                    }
                    val params = PrayerTimesParamMapper.getParams(context = context)
                    updatePrayerTimes(params)
                    updateWidget(context)
                    handleForceResetAlarms()
                }
                .onFailure { error ->
                    locationStateManager.updateLocationState(
                        LocationStateManager.LocationState.Error(
                            error.message ?: "Failed to update location"
                        )
                    )
                    loadFallbackLocation()
                }
        } catch (e: Exception) {
            locationStateManager.updateLocationState(
                LocationStateManager.LocationState.Error(
                    e.message ?: "Failed to update location"
                )
            )
            loadFallbackLocation()
        }
    }


    private fun handleBatteryExempt(event: SettingsEvent.BatteryExempt) {
        _locationSettingsState.update { it.copy(isBatteryExempt = event.exempt) }
        sharedPreferences.saveDataBoolean(AppConstants.BATTERY_OPTIMIZATION, event.exempt)
    }

    private fun handleNotificationsAllowed(event: SettingsEvent.NotificationsAllowed) {
        _locationSettingsState.update { it.copy(areNotificationsAllowed = event.allowed) }
        sharedPreferences.saveDataBoolean(AppConstants.NOTIFICATION_ALLOWED, event.allowed)
        val channelLock =
            sharedPreferences.getDataBoolean(AppConstants.CHANNEL_LOCK, false)
        if (!channelLock) {
            createAlarms.createAllNotificationChannels(context)
            sharedPreferences.saveDataBoolean(AppConstants.CHANNEL_LOCK, true)
        }
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
        _locationSettingsState.update { currentState ->
            currentState.copy(
                isAuto = sharedPreferences.getDataBoolean(AppConstants.LOCATION_TYPE, false),
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

    private fun loadUiSettings() {
        _uiState.update { currentState ->
            currentState.copy(
                location = sharedPreferences.getData(AppConstants.LOCATION_INPUT, ""),
                latitude = sharedPreferences.getDataDouble(AppConstants.LATITUDE, 0.0),
                longitude = sharedPreferences.getDataDouble(AppConstants.LONGITUDE, 0.0)
            )
        }
    }


    private suspend fun loadLocation(isAuto: Boolean) {
        try {
            locationService.loadLocation(isAuto)
                .onSuccess { location ->
                    _uiState.update {
                        it.copy(
                            location = location.locationName,
                            latitude = location.latitude,
                            longitude = location.longitude
                        )
                    }
                    ViewModelLogger.d(
                        "Nimaz: SettingsViewModel",
                        "inside sucess Updating location state: ${locationState.value}"
                    )
                    updateWidget(context)
                    val params = PrayerTimesParamMapper.getParams(context = context)
                    updatePrayerTimes(params)
                    loadPrayerTimes()
                }
                .onFailure { error ->
                    locationStateManager.updateLocationState(
                        LocationStateManager.LocationState.Error(
                            error.message ?: "Location fetch failed"
                        )
                    )
                    loadFallbackLocation()
                }
        } catch (e: Exception) {
            locationStateManager.updateLocationState(
                LocationStateManager.LocationState.Error(
                    e.message ?: "Location fetch failed"
                )
            )
            loadFallbackLocation()
        }
    }


    private suspend fun loadFallbackLocation() {
        val fallbackLocation = Location(
            latitude = sharedPreferences.getDataDouble(AppConstants.LATITUDE, 53.3498),
            longitude = sharedPreferences.getDataDouble(AppConstants.LONGITUDE, -6.2603),
            locationName = sharedPreferences.getData(AppConstants.LOCATION_INPUT, "")
        )
        locationStateManager.updateLocationState(
            LocationStateManager.LocationState.Success(
                fallbackLocation
            )
        )
        _uiState.update {
            it.copy(
                location = fallbackLocation.locationName,
                latitude = fallbackLocation.latitude,
                longitude = fallbackLocation.longitude
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
            val response = withContext(Dispatchers.IO) {
                prayerTimesRepository.updatePrayerTimes(parameters)
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