package com.arshadshah.nimaz.viewModel

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.repositories.LocationRepository
import com.arshadshah.nimaz.repositories.PrayerTimesRepository
import com.arshadshah.nimaz.services.LocationService
import com.arshadshah.nimaz.services.PrayerTimesService
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.alarms.CreateAlarms
import com.arshadshah.nimaz.utils.sunMoonUtils.AutoAnglesCalc
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IntroductionViewModel @Inject constructor(
    private val sharedPreferences: PrivateSharedPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val locationRepository = LocationRepository(context)
    private val locationService = LocationService(context, locationRepository)
    private val prayerTimesService = PrayerTimesService(context, PrayerTimesRepository)


    // Existing state management
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    // Permission states
    private val _isLocationAuto = MutableStateFlow(false)
    val isLocationAuto: StateFlow<Boolean> = _isLocationAuto.asStateFlow()

    private val _locationName = MutableStateFlow("")
    val locationName: StateFlow<String> = _locationName.asStateFlow()

    private val _latitude = MutableStateFlow(0.0)
    val latitude: StateFlow<Double> = _latitude.asStateFlow()

    private val _longitude = MutableStateFlow(0.0)
    val longitude: StateFlow<Double> = _longitude.asStateFlow()

    private val _locationPermissionGranted = MutableStateFlow(false)
    val locationPermissionGranted: StateFlow<Boolean> = _locationPermissionGranted.asStateFlow()


    private val _notificationPermissionGranted = MutableStateFlow(false)
    val notificationPermissionGranted: StateFlow<Boolean> =
        _notificationPermissionGranted.asStateFlow()

    // New notification-specific states
    private val _areNotificationsAllowed = MutableStateFlow(false)
    val areNotificationsAllowed: StateFlow<Boolean> = _areNotificationsAllowed.asStateFlow()

    private val _batteryOptimizationExempted = MutableStateFlow(false)
    val batteryOptimizationExempted: StateFlow<Boolean> = _batteryOptimizationExempted.asStateFlow()

    private val _isSetupComplete = MutableStateFlow(false)
    val isSetupComplete: StateFlow<Boolean> = _isSetupComplete.asStateFlow()

    private val _autoParams = MutableStateFlow(false)
    val autoParams: StateFlow<Boolean> = _autoParams.asStateFlow()

    private val _calculationMethod = MutableStateFlow("MWL")
    val calculationMethod: StateFlow<String> = _calculationMethod.asStateFlow()

    private val _isAutoCalculation = MutableStateFlow(false)
    val isAutoCalculation: StateFlow<Boolean> = _isAutoCalculation.asStateFlow()

    private val _availableCalculationMethods = MutableStateFlow(AppConstants.getMethods())
    val availableCalculationMethods: StateFlow<Map<String, String>> =
        _availableCalculationMethods.asStateFlow()


    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        loadInitialState()
    }

    sealed class IntroEvent {
        data object ClearError : IntroEvent()
        data class NavigateToPage(val page: Int) : IntroEvent()
        data class UpdateLocationPermission(val granted: Boolean) : IntroEvent()
        data class HandleLocationToggle(val enabled: Boolean) : IntroEvent()
        data class LocationInput(val location: String) : IntroEvent()
        data class UpdateLatitude(val latitude: Double) : IntroEvent()
        data class UpdateLongitude(val longitude: Double) : IntroEvent()
        data object LoadLocation : IntroEvent()
        data class UpdateAutoParams(val enabled: Boolean) : IntroEvent()
        data class UpdateNotificationPermission(val granted: Boolean) : IntroEvent()
        data class UpdateBatteryOptimization(val exempted: Boolean) : IntroEvent()
        data class UpdateCalculationMethod(val method: String) : IntroEvent()
        data class ToggleAutoCalculation(val enabled: Boolean) : IntroEvent()
        data class NotificationsAllowed(val allowed: Boolean) : IntroEvent()
        data class HandleNotificationToggle(val enabled: Boolean) : IntroEvent()
        data class HandleBatteryOptimization(val enable: Boolean) : IntroEvent()
        data object CompleteSetup : IntroEvent()
        data object LoadState : IntroEvent()
        data object CreateNotificationChannels : IntroEvent()
    }

    private fun loadInitialState() {
        viewModelScope.launch {
            safeOperation("load initial state") {
                _isLocationAuto.value =
                    sharedPreferences.getDataBoolean(AppConstants.LOCATION_TYPE, false)
                _locationName.value = sharedPreferences.getData(AppConstants.LOCATION_INPUT, "")
                _latitude.value = sharedPreferences.getDataDouble(AppConstants.LATITUDE, 53.3498)
                _longitude.value = sharedPreferences.getDataDouble(AppConstants.LONGITUDE, -6.2603)
                _locationPermissionGranted.value =
                    sharedPreferences.getDataBoolean("location_permission", false)
                _autoParams.value =
                    sharedPreferences.getDataBoolean(AppConstants.AUTO_PARAMETERS, false)

                _isSetupComplete.value = sharedPreferences.getDataBoolean("setup_complete", false)
                _currentPage.value = sharedPreferences.getDataInt("current_intro_page")
                _locationPermissionGranted.value =
                    sharedPreferences.getDataBoolean("location_permission", false)
                _notificationPermissionGranted.value =
                    sharedPreferences.getDataBoolean("notification_permission", false)
                _batteryOptimizationExempted.value =
                    sharedPreferences.getDataBoolean("battery_optimization", false)
                _areNotificationsAllowed.value = notificationManager.areNotificationsEnabled()

                _calculationMethod.value =
                    sharedPreferences.getData(AppConstants.CALCULATION_METHOD, "MWL")
                _isAutoCalculation.value =
                    sharedPreferences.getDataBoolean(AppConstants.AUTO_PARAMETERS, false)

                loadLocation(_isLocationAuto.value)
            }
        }
    }

    fun handleEvent(event: IntroEvent) {
        when (event) {
            is IntroEvent.ClearError -> _error.value = null

            is IntroEvent.NavigateToPage -> {
                viewModelScope.launch {
                    safeOperation("navigate to page") {
                        _currentPage.value = event.page
                        sharedPreferences.saveDataInt("current_intro_page", event.page)
                    }
                }
            }

            is IntroEvent.NotificationsAllowed -> {
                viewModelScope.launch {
                    safeOperation("update notifications allowed") {
                        _areNotificationsAllowed.value = event.allowed
                    }
                }
            }

            is IntroEvent.HandleBatteryOptimization -> {
                viewModelScope.launch {
                    safeOperation("handle battery optimization") {
                        if (event.enable) {
                            val intent = Intent().apply {
                                action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                                data = Uri.parse("package:${context.packageName}")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } else {
                            val intent = Intent().apply {
                                action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                    }
                }
            }

            is IntroEvent.UpdateBatteryOptimization -> {
                viewModelScope.launch {
                    safeOperation("update battery optimization") {
                        _batteryOptimizationExempted.value = event.exempted
                        sharedPreferences.saveDataBoolean(
                            AppConstants.BATTERY_OPTIMIZATION,
                            event.exempted
                        )
                    }
                }
            }

            is IntroEvent.HandleNotificationToggle -> {
                viewModelScope.launch {
                    safeOperation("handle notification toggle") {
                        if (event.enabled) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                // Permission handling will be done in the UI layer
                                sharedPreferences.saveDataBoolean(
                                    AppConstants.NOTIFICATION_ALLOWED,
                                    true
                                )
                            } else {
                                // Launch notification settings for older Android versions
                                launchNotificationSettings()
                            }
                        } else {
                            sharedPreferences.removeData(AppConstants.NOTIFICATION_ALLOWED)
                            launchNotificationSettings()
                        }
                    }
                }
            }

            is IntroEvent.CreateNotificationChannels -> {
                viewModelScope.launch {
                    safeOperation("create notification channels") {
                        val channelLock =
                            sharedPreferences.getDataBoolean(AppConstants.CHANNEL_LOCK, false)
                        if (!channelLock) {
                            CreateAlarms().createAllNotificationChannels(context)
                            sharedPreferences.saveDataBoolean(AppConstants.CHANNEL_LOCK, true)
                        }
                    }
                }
            }

            is IntroEvent.HandleLocationToggle -> handleLocationToggle(event.enabled)
            is IntroEvent.LocationInput -> handleLocationInput(event.location)
            is IntroEvent.UpdateLatitude -> updateLatitude(event.latitude)
            is IntroEvent.UpdateLongitude -> updateLongitude(event.longitude)
            is IntroEvent.LoadLocation -> loadLocation(_isLocationAuto.value)
            is IntroEvent.UpdateAutoParams -> handleAutoParams(event.enabled)
            is IntroEvent.UpdateLocationPermission -> {
                viewModelScope.launch {
                    safeOperation("update location permission") {
                        _locationPermissionGranted.value = event.granted
                        sharedPreferences.saveDataBoolean("location_permission", event.granted)
                        if (event.granted) {
                            loadLocation(true)
                        }
                    }
                }
            }

            is IntroEvent.UpdateNotificationPermission -> {
                viewModelScope.launch {
                    safeOperation("update notification permission") {
                        _notificationPermissionGranted.value = event.granted
                        sharedPreferences.saveDataBoolean("notification_permission", event.granted)
                    }
                }
            }

            is IntroEvent.UpdateBatteryOptimization -> {
                viewModelScope.launch {
                    safeOperation("update battery optimization") {
                        _batteryOptimizationExempted.value = event.exempted
                        sharedPreferences.saveDataBoolean("battery_optimization", event.exempted)
                    }
                }
            }

            is IntroEvent.UpdateCalculationMethod -> {
                handleMethodSelection(event.method)
            }

            is IntroEvent.ToggleAutoCalculation -> {
                handleAutoCalculation(event.enabled)
            }

            is IntroEvent.CompleteSetup -> {
                viewModelScope.launch {
                    safeOperation("complete setup") {
                        _isSetupComplete.value = true
                        sharedPreferences.saveDataBoolean("setup_complete", true)
                    }
                }
            }

            is IntroEvent.LoadState -> loadInitialState()
        }
    }


    private fun handleMethodSelection(method: String) {
        viewModelScope.launch {
            safeOperation("update calculation method") {
                // Save method to preferences
                sharedPreferences.saveData(AppConstants.CALCULATION_METHOD, method)

                // Apply default parameters for the method
                val defaults = AppConstants.getDefaultParametersForMethod(method)
                saveCalculationDefaults(defaults)

                // Update prayer times
                updatePrayerTimes()
            }
        }
    }

    private fun handleAutoCalculation(enabled: Boolean) {
        viewModelScope.launch {
            safeOperation("handle auto calculation") {
                _isAutoCalculation.value = enabled
                sharedPreferences.saveDataBoolean(AppConstants.AUTO_PARAMETERS, enabled)

                if (enabled) {
                    // Set method to OTHER
                    sharedPreferences.saveData(AppConstants.CALCULATION_METHOD, "OTHER")

                    // Calculate and save auto angles
                    val latitude = sharedPreferences.getDataDouble(AppConstants.LATITUDE, 0.0)
                    val longitude = sharedPreferences.getDataDouble(AppConstants.LONGITUDE, 0.0)

                    val fajrAngle =
                        AutoAnglesCalc().calculateFajrAngle(context, latitude, longitude)
                    val ishaAngle =
                        AutoAnglesCalc().calculateIshaaAngle(context, latitude, longitude)

                    // Save calculated values
                    sharedPreferences.apply {
                        saveData(AppConstants.FAJR_ANGLE, fajrAngle.toString())
                        saveData(AppConstants.ISHA_ANGLE, ishaAngle.toString())
                        saveData(AppConstants.HIGH_LATITUDE_RULE, "TWILIGHT_ANGLE")

                        // Reset all offsets to 0
                        saveData(AppConstants.FAJR_ADJUSTMENT, "0")
                        saveData(AppConstants.SUNRISE_ADJUSTMENT, "0")
                        saveData(AppConstants.DHUHR_ADJUSTMENT, "0")
                        saveData(AppConstants.ASR_ADJUSTMENT, "0")
                        saveData(AppConstants.MAGHRIB_ADJUSTMENT, "0")
                        saveData(AppConstants.ISHA_ADJUSTMENT, "0")
                    }
                }

                // Update prayer times after changes
                updatePrayerTimes()
            }
        }
    }

    private fun saveCalculationDefaults(defaults: Map<String, String>) {
        sharedPreferences.apply {
            defaults["fajrAngle"]?.let { saveData(AppConstants.FAJR_ANGLE, it) }
            defaults["ishaAngle"]?.let { saveData(AppConstants.ISHA_ANGLE, it) }
            defaults["ishaInterval"]?.let { saveData(AppConstants.ISHA_INTERVAL, it) }
            defaults["madhab"]?.let { saveData(AppConstants.MADHAB, it) }
            defaults["highLatitudeRule"]?.let { saveData(AppConstants.HIGH_LATITUDE_RULE, it) }
            defaults["fajrAdjustment"]?.let { saveData(AppConstants.FAJR_ADJUSTMENT, it) }
            defaults["sunriseAdjustment"]?.let { saveData(AppConstants.SUNRISE_ADJUSTMENT, it) }
            defaults["dhuhrAdjustment"]?.let { saveData(AppConstants.DHUHR_ADJUSTMENT, it) }
            defaults["asrAdjustment"]?.let { saveData(AppConstants.ASR_ADJUSTMENT, it) }
            defaults["maghribAdjustment"]?.let { saveData(AppConstants.MAGHRIB_ADJUSTMENT, it) }
            defaults["ishaAdjustment"]?.let { saveData(AppConstants.ISHA_ADJUSTMENT, it) }
        }
    }


    private fun handleLocationToggle(enabled: Boolean) {
        viewModelScope.launch {
            safeOperation("handle location toggle") {
                _isLocationAuto.value = enabled
                sharedPreferences.saveDataBoolean(AppConstants.LOCATION_TYPE, enabled)
                sharedPreferences.saveDataBoolean(AppConstants.ALARM_LOCK, false)

                if (enabled && _locationPermissionGranted.value) {
                    loadLocation(true)
                } else if (!enabled) {
                    // Handle manual location mode
                    if (_currentPage.value == 4) { // Location page
                        launchLocationSettings()
                    }
                }

                updatePrayerTimes()
            }
        }
    }

    private fun handleLocationInput(location: String) {
        viewModelScope.launch {
            safeOperation("handle location input") {
                _locationName.value = location
                sharedPreferences.saveData(AppConstants.LOCATION_INPUT, location)

                locationRepository.forwardGeocode(location)
                    .onSuccess { locationData ->
                        _latitude.value = locationData.latitude
                        _longitude.value = locationData.longitude
                        _locationName.value = locationData.locationName
                        sharedPreferences.saveDataDouble(
                            AppConstants.LATITUDE,
                            locationData.latitude
                        )
                        sharedPreferences.saveDataDouble(
                            AppConstants.LONGITUDE,
                            locationData.longitude
                        )
                        updatePrayerTimes()
                    }
                    .onFailure {
                        handleError("geocoding", Exception("Failed to find location"))
                    }
            }
        }
    }

    private fun loadLocation(isAuto: Boolean) {
        viewModelScope.launch {
            safeOperation("load location") {
                locationService.loadLocation(
                    isAuto,
                    onSuccess = { location ->
                        _locationName.value = location.locationName
                        _latitude.value = location.latitude
                        _longitude.value = location.longitude
                        updatePrayerTimes()
                    },
                    onError = { errorMessage ->
                        handleError("load location", Exception(errorMessage))
                    }
                )
            }
        }
    }

    private fun handleAutoParams(enabled: Boolean) {
        viewModelScope.launch {
            safeOperation("handle auto params") {
                _autoParams.value = enabled
                sharedPreferences.saveDataBoolean(AppConstants.AUTO_PARAMETERS, enabled)

                if (enabled) {
                    val fajrAngle = AutoAnglesCalc().calculateFajrAngle(
                        context,
                        _latitude.value,
                        _longitude.value
                    )
                    val ishaAngle = AutoAnglesCalc().calculateIshaaAngle(
                        context,
                        _latitude.value,
                        _longitude.value
                    )

                    // Update calculation parameters
                    sharedPreferences.saveData(AppConstants.CALCULATION_METHOD, "OTHER")
                    sharedPreferences.saveData(AppConstants.FAJR_ANGLE, fajrAngle.toString())
                    sharedPreferences.saveData(AppConstants.ISHA_ANGLE, ishaAngle.toString())
                    sharedPreferences.saveData(AppConstants.HIGH_LATITUDE_RULE, "TWILIGHT_ANGLE")
                }

                updatePrayerTimes()
            }
        }
    }

    private fun updateLatitude(latitude: Double) {
        viewModelScope.launch {
            safeOperation("update latitude") {
                _latitude.value = latitude
                sharedPreferences.saveDataDouble(AppConstants.LATITUDE, latitude)
                if (_autoParams.value) {
                    handleAutoParams(true)
                }
                updatePrayerTimes()
            }
        }
    }

    private fun updateLongitude(longitude: Double) {
        viewModelScope.launch {
            safeOperation("update longitude") {
                _longitude.value = longitude
                sharedPreferences.saveDataDouble(AppConstants.LONGITUDE, longitude)
                if (_autoParams.value) {
                    handleAutoParams(true)
                }
                updatePrayerTimes()
            }
        }
    }

    private fun updatePrayerTimes() {
        viewModelScope.launch {
            safeOperation("update prayer times") {
                // Update prayer times using the service
                prayerTimesService.getPrayerTimes()
            }
        }
    }

    private fun launchLocationSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addCategory(Intent.CATEGORY_DEFAULT)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun launchNotificationSettings() {
        val intent = Intent().apply {
            action = "android.settings.APP_NOTIFICATION_SETTINGS"
            putExtra("android.provider.extra.APP_PACKAGE", context.packageName)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private suspend fun <T> safeOperation(
        operation: String,
        block: suspend () -> T
    ): T? {
        return try {
            _isLoading.value = true
            _error.value = null
            block()
        } catch (e: Exception) {
            handleError(operation, e)
            null
        } finally {
            _isLoading.value = false
        }
    }

    private fun handleError(operation: String, e: Exception) {
        val errorMessage = "Failed to $operation: ${e.localizedMessage}"
        _error.value = errorMessage
    }

    fun canNavigateNext(): Boolean {
        return when (_currentPage.value) {
            4 -> _locationPermissionGranted.value // Location page
            5 -> true // Calculation method page
            6 -> _notificationPermissionGranted.value // Notification page
            7 -> _batteryOptimizationExempted.value // Battery optimization page
            else -> true
        }
    }

    fun isLastPage(): Boolean {
        return _currentPage.value == 7 // Adjust based on your total pages
    }
}