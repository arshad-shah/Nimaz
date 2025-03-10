package com.arshadshah.nimaz.viewModel

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.IS_FIRST_INSTALL
import com.arshadshah.nimaz.repositories.Location
import com.arshadshah.nimaz.services.LocationService
import com.arshadshah.nimaz.services.LocationStateManager
import com.arshadshah.nimaz.services.PrayerTimesService
import com.arshadshah.nimaz.utils.FirebaseLogger
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.alarms.CreateAlarms
import com.arshadshah.nimaz.utils.sunMoonUtils.AutoAnglesCalc
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IntroductionViewModel @Inject constructor(
    private val sharedPreferences: PrivateSharedPreferences,
    @ApplicationContext private val context: Context,
    private val prayerTimesService: PrayerTimesService,
    private val locationService: LocationService,
    private val createAlarms: CreateAlarms,
    private val locationStateManager: LocationStateManager,
    val firebaseLogger: FirebaseLogger // Added Firebase Logger
) : ViewModel() {

    // UI State
    data class UiState(
        val isLoading: Boolean = false,
        val error: String? = null,
        val currentPage: Int = 0,
        val isSetupComplete: Boolean = false,
        val showLegalError: Boolean = false  // Added for displaying legal acceptance error
    )

    // Location Settings State
    data class LocationSettingsState(
        val isAuto: Boolean = false,
        val locationName: String = "Abbeyleix",
        val latitude: Double = 0.0,
        val longitude: Double = 0.0,
        val locationPermissionGranted: Boolean = false
    )

    // Notification Settings State
    data class NotificationSettingsState(
        val notificationPermissionGranted: Boolean = false,
        val areNotificationsAllowed: Boolean = false,
        val batteryOptimizationExempted: Boolean = false
    )

    // Calculation Settings State
    data class CalculationSettingsState(
        val autoParams: Boolean = false,
        val calculationMethod: String = "MWL",
        val isAutoCalculation: Boolean = false,
        val availableCalculationMethods: Map<String, String> = AppConstants.getMethods()
    )

    // Legal Acceptance State - Added new state class
    data class LegalSettingsState(
        val termsAccepted: Boolean = false,
        val privacyPolicyAccepted: Boolean = false,
        val lastAcceptedVersion: String = ""
    )

    // StateFlows
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _locationSettingsState = MutableStateFlow(LocationSettingsState())
    val locationSettingsState: StateFlow<LocationSettingsState> =
        _locationSettingsState.asStateFlow()

    private val _notificationSettingsState = MutableStateFlow(NotificationSettingsState())
    val notificationSettingsState: StateFlow<NotificationSettingsState> =
        _notificationSettingsState.asStateFlow()

    private val _calculationSettingsState = MutableStateFlow(CalculationSettingsState())
    val calculationSettingsState: StateFlow<CalculationSettingsState> =
        _calculationSettingsState.asStateFlow()

    // Added new StateFlow for legal settings
    private val _legalSettingsState = MutableStateFlow(LegalSettingsState())
    val legalSettingsState: StateFlow<LegalSettingsState> =
        _legalSettingsState.asStateFlow()

    // Location state from manager
    val locationState = locationStateManager.locationState

    // Derived location states for backwards compatibility
    val locationName = locationState.map { state ->
        when (state) {
            is LocationStateManager.LocationState.Success -> state.location.locationName
            is LocationStateManager.LocationState.Error -> "Location unavailable"
            LocationStateManager.LocationState.Loading -> ""
            LocationStateManager.LocationState.Idle -> sharedPreferences.getData(
                AppConstants.LOCATION_INPUT,
                ""
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val latitude = locationState.map { state ->
        when (state) {
            is LocationStateManager.LocationState.Success -> state.location.latitude
            else -> sharedPreferences.getDataDouble(AppConstants.LATITUDE, 53.3498)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val longitude = locationState.map { state ->
        when (state) {
            is LocationStateManager.LocationState.Success -> state.location.longitude
            else -> sharedPreferences.getDataDouble(AppConstants.LONGITUDE, -6.2603)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // New convenience property for checking if legal terms have been accepted
    val termsAccepted: Boolean
        get() = legalSettingsState.value.termsAccepted && legalSettingsState.value.privacyPolicyAccepted

    init {
        loadInitialState()

        // Log that onboarding has started
        firebaseLogger.logEvent(
            "onboarding_started",
            mapOf("app_version" to getAppVersion()),
            FirebaseLogger.Companion.EventCategory.USER_ACTION
        )
    }

    private fun loadInitialState() {
        viewModelScope.launch {
            safeOperation("load initial state") {
                loadSettings()
                locationStateManager.cleanupRequest()

                // Log initial state loaded
                firebaseLogger.logEvent(
                    "onboarding_state_loaded",
                    null,
                    FirebaseLogger.Companion.EventCategory.PERFORMANCE
                )
            }
        }
    }

    // Event definitions
    sealed class IntroEvent {
        // Navigation Events
        data object ClearError : IntroEvent()
        data class NavigateToPage(val page: Int) : IntroEvent()

        // Location Events
        data class UpdateLocationPermission(val granted: Boolean) : IntroEvent()
        data class HandleLocationToggle(val enabled: Boolean) : IntroEvent()
        data class LocationInput(val location: String) : IntroEvent()
        data object LoadLocation : IntroEvent()

        // Settings Events
        data class UpdateAutoParams(val enabled: Boolean) : IntroEvent()
        data class UpdateNotificationPermission(val granted: Boolean) : IntroEvent()
        data class UpdateBatteryOptimization(val exempted: Boolean) : IntroEvent()
        data class UpdateCalculationMethod(val method: String) : IntroEvent()
        data class ToggleAutoCalculation(val enabled: Boolean) : IntroEvent()
        data class NotificationsAllowed(val allowed: Boolean) : IntroEvent()
        data class HandleNotificationToggle(val enabled: Boolean) : IntroEvent()
        data class HandleBatteryOptimization(val enable: Boolean) : IntroEvent()

        // Legal Events - Added new event types
        data class AcceptTerms(val accepted: Boolean) : IntroEvent()
        data class AcceptPrivacyPolicy(val accepted: Boolean) : IntroEvent()
        data class AcceptAllLegalTerms(val accepted: Boolean) : IntroEvent()
        data object ShowLegalError : IntroEvent()
        data object ClearLegalError : IntroEvent()

        // System Events
        data object CompleteSetup : IntroEvent()
        data object LoadState : IntroEvent()
        data object CreateNotificationChannels : IntroEvent()
    }

    fun handleEvent(event: IntroEvent) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                when (event) {
                    // Navigation Events
                    is IntroEvent.ClearError -> {
                        _uiState.update { it.copy(error = null) }
                    }

                    is IntroEvent.NavigateToPage -> {
                        _uiState.update { it.copy(currentPage = event.page) }
                        sharedPreferences.saveDataInt("current_intro_page", event.page)

                        // Log page navigation
                        firebaseLogger.logScreenView(
                            "onboarding_page_${event.page}",
                            "IntroPage"
                        )
                    }

                    // Location Events
                    is IntroEvent.HandleLocationToggle -> {
                        handleLocationToggle(event.enabled)

                        // Log location toggle event
                        firebaseLogger.logEvent(
                            "location_auto_toggle",
                            mapOf("enabled" to event.enabled),
                            FirebaseLogger.Companion.EventCategory.USER_ACTION
                        )
                    }

                    is IntroEvent.LocationInput -> {
                        handleLocationInput(event.location)

                        // Log location input event
                        firebaseLogger.logEvent(
                            "location_manual_input",
                            mapOf("location" to event.location),
                            FirebaseLogger.Companion.EventCategory.USER_ACTION
                        )
                    }

                    is IntroEvent.LoadLocation -> {
                        loadLocation(locationSettingsState.value.isAuto)

                        // Log location loading
                        firebaseLogger.logEvent(
                            "location_loading",
                            mapOf("auto_location" to locationSettingsState.value.isAuto),
                            FirebaseLogger.Companion.EventCategory.USER_ACTION
                        )
                    }

                    is IntroEvent.UpdateLocationPermission -> {
                        handleLocationPermissionUpdate(event.granted)

                        // Log permission update
                        firebaseLogger.logEvent(
                            "location_permission_update",
                            mapOf("granted" to event.granted),
                            FirebaseLogger.Companion.EventCategory.USER_ACTION
                        )
                    }

                    // Settings Events
                    is IntroEvent.UpdateAutoParams -> {
                        handleAutoParams(event.enabled)

                        // Log auto params change
                        firebaseLogger.logEvent(
                            "calculation_auto_params",
                            mapOf("enabled" to event.enabled),
                            FirebaseLogger.Companion.EventCategory.USER_ACTION
                        )
                    }

                    is IntroEvent.UpdateCalculationMethod -> {
                        handleMethodSelection(event.method)

                        // Log method selection
                        firebaseLogger.logEvent(
                            "calculation_method_selected",
                            mapOf("method" to event.method),
                            FirebaseLogger.Companion.EventCategory.USER_ACTION
                        )
                    }

                    is IntroEvent.ToggleAutoCalculation -> {
                        handleAutoCalculation(event.enabled)

                        // Log auto calculation toggle
                        firebaseLogger.logEvent(
                            "auto_calculation_toggle",
                            mapOf("enabled" to event.enabled),
                            FirebaseLogger.Companion.EventCategory.USER_ACTION
                        )
                    }

                    is IntroEvent.UpdateNotificationPermission -> {
                        handleNotificationPermissionUpdate(event.granted)

                        // Log notification permission update
                        firebaseLogger.logEvent(
                            "notification_permission_update",
                            mapOf("granted" to event.granted),
                            FirebaseLogger.Companion.EventCategory.USER_ACTION
                        )
                    }

                    is IntroEvent.UpdateBatteryOptimization -> {
                        handleBatteryOptimizationUpdate(event.exempted)

                        // Log battery optimization update
                        firebaseLogger.logEvent(
                            "battery_optimization_update",
                            mapOf("exempted" to event.exempted),
                            FirebaseLogger.Companion.EventCategory.USER_ACTION
                        )
                    }

                    is IntroEvent.NotificationsAllowed -> {
                        handleNotificationsAllowedUpdate(event.allowed)

                        // Log notifications allowed update
                        firebaseLogger.logEvent(
                            "notifications_allowed_update",
                            mapOf("allowed" to event.allowed),
                            FirebaseLogger.Companion.EventCategory.USER_ACTION
                        )
                    }

                    is IntroEvent.HandleNotificationToggle -> {
                        handleNotificationToggle(event.enabled)

                        // Log notification toggle
                        firebaseLogger.logEvent(
                            "notification_toggle",
                            mapOf("enabled" to event.enabled),
                            FirebaseLogger.Companion.EventCategory.USER_ACTION
                        )
                    }

                    is IntroEvent.HandleBatteryOptimization -> {
                        handleBatteryOptimizationToggle(event.enable)

                        // Log battery optimization toggle
                        firebaseLogger.logEvent(
                            "battery_optimization_toggle",
                            mapOf("enable" to event.enable),
                            FirebaseLogger.Companion.EventCategory.USER_ACTION
                        )
                    }

                    // Legal Events - Handling for new events
                    is IntroEvent.AcceptTerms -> {
                        handleTermsAcceptance(event.accepted)

                        // Log terms acceptance
                        firebaseLogger.logEvent(
                            "terms_acceptance",
                            mapOf("accepted" to event.accepted),
                            FirebaseLogger.Companion.EventCategory.USER_ACTION
                        )
                    }

                    is IntroEvent.AcceptPrivacyPolicy -> {
                        handlePrivacyPolicyAcceptance(event.accepted)

                        // Log privacy policy acceptance
                        firebaseLogger.logEvent(
                            "privacy_policy_acceptance",
                            mapOf("accepted" to event.accepted),
                            FirebaseLogger.Companion.EventCategory.USER_ACTION
                        )
                    }

                    is IntroEvent.AcceptAllLegalTerms -> {
                        handleAllLegalTermsAcceptance(event.accepted)

                        // Log all legal terms acceptance
                        firebaseLogger.logEvent(
                            "all_legal_terms_acceptance",
                            mapOf("accepted" to event.accepted),
                            FirebaseLogger.Companion.EventCategory.USER_ACTION
                        )
                    }

                    is IntroEvent.ShowLegalError -> {
                        _uiState.update { it.copy(showLegalError = true) }

                        // Log legal error shown
                        firebaseLogger.logEvent(
                            "legal_error_shown",
                            null,
                            FirebaseLogger.Companion.EventCategory.APP_ERROR
                        )
                    }

                    is IntroEvent.ClearLegalError -> {
                        _uiState.update { it.copy(showLegalError = false) }
                    }

                    // System Events
                    is IntroEvent.CompleteSetup -> {
                        if (legalSettingsState.value.termsAccepted && legalSettingsState.value.privacyPolicyAccepted) {
                            _uiState.update { it.copy(isSetupComplete = true) }
                            sharedPreferences.saveDataBoolean(IS_FIRST_INSTALL, false)

                            // Log setup completion
                            firebaseLogger.logEvent(
                                "onboarding_completed",
                                mapOf(
                                    "auto_location" to locationSettingsState.value.isAuto,
                                    "calculation_method" to calculationSettingsState.value.calculationMethod,
                                    "auto_calculation" to calculationSettingsState.value.isAutoCalculation,
                                    "notifications_enabled" to notificationSettingsState.value.areNotificationsAllowed,
                                    "battery_optimized" to notificationSettingsState.value.batteryOptimizationExempted
                                ),
                                FirebaseLogger.Companion.EventCategory.USER_ACTION
                            )

                            // Set user properties for later analysis
                            firebaseLogger.setUserProperty(
                                "user_location_type",
                                if (locationSettingsState.value.isAuto) "auto" else "manual"
                            )
                            firebaseLogger.setUserProperty(
                                "user_calc_method",
                                calculationSettingsState.value.calculationMethod
                            )
                            firebaseLogger.setUserProperty(
                                "user_has_notifications",
                                notificationSettingsState.value.areNotificationsAllowed.toString()
                            )
                        } else {
                            _uiState.update { it.copy(showLegalError = true) }

                            // Log legal acceptance error
                            firebaseLogger.logEvent(
                                "onboarding_completion_blocked",
                                mapOf("reason" to "legal_terms_not_accepted"),
                                FirebaseLogger.Companion.EventCategory.APP_ERROR
                            )
                        }
                    }

                    is IntroEvent.LoadState -> loadInitialState()

                    is IntroEvent.CreateNotificationChannels -> {
                        createNotificationChannels()

                        // Log notification channels creation
                        firebaseLogger.logEvent(
                            "notification_channels_created",
                            null,
                            FirebaseLogger.Companion.EventCategory.USER_ACTION
                        )
                    }
                }
            } catch (e: Exception) {
                handleError("event handling", e)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun safeOperation(
        operation: String,
        block: suspend () -> Unit
    ) {
        try {
            _uiState.update { it.copy(isLoading = true, error = null) }
            block()
        } catch (e: Exception) {
            handleError(operation, e)
        } finally {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun handleError(operation: String, e: Exception) {
        val errorMessage = "Failed to $operation: ${e.localizedMessage}"
        _uiState.update { it.copy(error = errorMessage) }

        // Log error to Firebase
        firebaseLogger.logError(
            "onboarding_error",
            errorMessage,
            mapOf(
                "operation" to operation,
                "exception_type" to e.javaClass.simpleName,
                "current_page" to _uiState.value.currentPage
            )
        )
    }

    private suspend fun loadSettings() {
        safeOperation("load settings") {
            // Load Location Settings
            _locationSettingsState.update {
                it.copy(
                    isAuto = sharedPreferences.getDataBoolean(AppConstants.LOCATION_TYPE, false),
                    locationName = sharedPreferences.getData(
                        AppConstants.LOCATION_INPUT,
                        "Abbeyleix"
                    ),
                    latitude = sharedPreferences.getDataDouble(AppConstants.LATITUDE, 53.3498),
                    longitude = sharedPreferences.getDataDouble(AppConstants.LONGITUDE, -6.2603),
                    locationPermissionGranted = sharedPreferences.getDataBoolean(
                        "location_permission",
                        false
                    )
                )
            }

            // Load Notification Settings
            _notificationSettingsState.update {
                it.copy(
                    notificationPermissionGranted = sharedPreferences.getDataBoolean(
                        "notification_permission",
                        false
                    ),
                    areNotificationsAllowed = notificationManager.areNotificationsEnabled(),
                    batteryOptimizationExempted = sharedPreferences.getDataBoolean(
                        "battery_optimization",
                        false
                    )
                )
            }

            // Load Calculation Settings
            _calculationSettingsState.update {
                it.copy(
                    autoParams = sharedPreferences.getDataBoolean(
                        AppConstants.AUTO_PARAMETERS,
                        false
                    ),
                    calculationMethod = sharedPreferences.getData(
                        AppConstants.CALCULATION_METHOD,
                        "MWL"
                    ),
                    isAutoCalculation = sharedPreferences.getDataBoolean(
                        AppConstants.AUTO_PARAMETERS,
                        false
                    )
                )
            }

            // Load Legal Settings
            _legalSettingsState.update {
                it.copy(
                    termsAccepted = sharedPreferences.getDataBoolean(
                        "terms_accepted",
                        false
                    ),
                    privacyPolicyAccepted = sharedPreferences.getDataBoolean(
                        "privacy_policy_accepted",
                        false
                    ),
                    lastAcceptedVersion = sharedPreferences.getData(
                        "legal_version_accepted",
                        ""
                    )
                )
            }

            // Load UI State
            _uiState.update {
                it.copy(
                    currentPage = sharedPreferences.getDataInt("current_intro_page"),
                    isSetupComplete = sharedPreferences.getDataBoolean("setup_complete", false)
                )
            }
        }
    }

    // Helper function to get app version for analytics
    private fun getAppVersion(): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    // Legal handling methods - New section
    private suspend fun handleTermsAcceptance(accepted: Boolean) {
        safeOperation("handle terms acceptance") {
            _legalSettingsState.update { it.copy(termsAccepted = accepted) }
            sharedPreferences.saveDataBoolean("terms_accepted", accepted)
            if (accepted) {
                _uiState.update { it.copy(showLegalError = false) }
                // Store app version to track future terms updates
                val appVersion =
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                sharedPreferences.saveData("legal_version_accepted", appVersion ?: "unknown")
            }
        }
    }

    private suspend fun handlePrivacyPolicyAcceptance(accepted: Boolean) {
        safeOperation("handle privacy policy acceptance") {
            _legalSettingsState.update { it.copy(privacyPolicyAccepted = accepted) }
            sharedPreferences.saveDataBoolean("privacy_policy_accepted", accepted)
            if (accepted) {
                _uiState.update { it.copy(showLegalError = false) }
                // Store app version to track future policy updates
                val appVersion =
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                sharedPreferences.saveData("legal_version_accepted", appVersion ?: "unknown")
            }
        }
    }

    private suspend fun handleAllLegalTermsAcceptance(accepted: Boolean) {
        safeOperation("handle all legal terms acceptance") {
            _legalSettingsState.update {
                it.copy(
                    termsAccepted = accepted,
                    privacyPolicyAccepted = accepted
                )
            }
            sharedPreferences.saveDataBoolean("terms_accepted", accepted)
            sharedPreferences.saveDataBoolean("privacy_policy_accepted", accepted)

            if (accepted) {
                _uiState.update { it.copy(showLegalError = false) }
                // Store app version to track future updates
                val appVersion =
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                sharedPreferences.saveData("legal_version_accepted", appVersion ?: "unknown")
            }
        }
    }

    // Location handling methods
    private suspend fun handleLocationToggle(enabled: Boolean) {
        safeOperation("handle location toggle") {
            // Update state
            _locationSettingsState.update { it.copy(isAuto = enabled) }
            sharedPreferences.saveDataBoolean(AppConstants.LOCATION_TYPE, enabled)
            sharedPreferences.saveDataBoolean(AppConstants.ALARM_LOCK, false)

            if (enabled && locationSettingsState.value.locationPermissionGranted) {
                loadLocation(true)
            } else if (!enabled) {
                if (_uiState.value.currentPage == 4) { // Location page
                    launchLocationSettings()
                }
            }

            updatePrayerTimes()
        }
    }

    private suspend fun handleLocationInput(location: String) {
        safeOperation("handle location input") {
            sharedPreferences.saveData(AppConstants.LOCATION_INPUT, location)

            try {
                locationService.loadLocation(false)
            } catch (e: Exception) {
                locationStateManager.updateLocationState(
                    LocationStateManager.LocationState.Error(
                        e.message ?: "Failed to process location"
                    )
                )
                loadFallbackLocation()

                // Log location loading error
                firebaseLogger.logError(
                    "location_loading_error",
                    "Failed to load location: ${e.message}",
                    mapOf("input_location" to location)
                )
            }
        }
    }

    private suspend fun loadLocation(isAuto: Boolean) {
        safeOperation("load location") {
            try {
                locationStateManager.cleanupRequest()
                locationService.loadLocation(isAuto)
                    .onSuccess { location ->
                        // Update local state
                        _locationSettingsState.update {
                            it.copy(
                                locationName = location.locationName,
                                latitude = location.latitude,
                                longitude = location.longitude
                            )
                        }

                        updatePrayerTimes()

                        // Log successful location loading
                        firebaseLogger.logEvent(
                            "location_loaded_success",
                            mapOf(
                                "location_name" to location.locationName,
                                "auto_location" to isAuto
                            ),
                            FirebaseLogger.Companion.EventCategory.PERFORMANCE
                        )
                    }
                    .onFailure { throwable ->
                        locationStateManager.updateLocationState(
                            LocationStateManager.LocationState.Error(
                                throwable.message ?: "Failed to load location"
                            )
                        )
                        loadFallbackLocation()

                        // Log location loading failure
                        firebaseLogger.logError(
                            "location_loading_failure",
                            throwable.message ?: "Failed to load location",
                            mapOf("auto_location" to isAuto)
                        )
                    }
            } catch (e: Exception) {
                locationStateManager.updateLocationState(
                    LocationStateManager.LocationState.Error(e.message ?: "Unknown error occurred")
                )
                loadFallbackLocation()

                // Log exception in location loading
                firebaseLogger.logError(
                    "location_loading_exception",
                    e.message ?: "Unknown error occurred",
                    mapOf("auto_location" to isAuto)
                )
            }
        }
    }

    private suspend fun loadFallbackLocation() {
        safeOperation("load fallback location") {
            val fallbackLocation = Location(
                latitude = sharedPreferences.getDataDouble(AppConstants.LATITUDE, 53.3498),
                longitude = sharedPreferences.getDataDouble(AppConstants.LONGITUDE, -6.2603),
                locationName = sharedPreferences.getData(AppConstants.LOCATION_INPUT, "Abbeyleix")
            )

            locationStateManager.updateLocationState(
                LocationStateManager.LocationState.Success(fallbackLocation)
            )

            _locationSettingsState.update {
                it.copy(
                    latitude = fallbackLocation.latitude,
                    longitude = fallbackLocation.longitude,
                    locationName = fallbackLocation.locationName
                )
            }

            // Log fallback location loaded
            firebaseLogger.logEvent(
                "fallback_location_loaded",
                mapOf(
                    "location_name" to fallbackLocation.locationName
                ),
                FirebaseLogger.Companion.EventCategory.USER_ACTION
            )
        }
    }

    private suspend fun handleLocationPermissionUpdate(granted: Boolean) {
        safeOperation("update location permission") {
            _locationSettingsState.update { it.copy(locationPermissionGranted = granted) }
            sharedPreferences.saveDataBoolean("location_permission", granted)

            if (granted) {
                loadLocation(true)
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

        // Log location settings launch
        firebaseLogger.logEvent(
            "location_settings_launched",
            null,
            FirebaseLogger.Companion.EventCategory.USER_ACTION
        )
    }

    private suspend fun updatePrayerTimes() {
        safeOperation("update prayer times") {
            prayerTimesService.getPrayerTimes()

            // Log prayer times update
            firebaseLogger.logEvent(
                "prayer_times_updated",
                mapOf(
                    "location" to locationSettingsState.value.locationName,
                    "calculation_method" to calculationSettingsState.value.calculationMethod
                ),
                FirebaseLogger.Companion.EventCategory.PERFORMANCE
            )
        }
    }

    // Notification handling methods
    private suspend fun handleNotificationPermissionUpdate(granted: Boolean) {
        safeOperation("update notification permission") {
            _notificationSettingsState.update { it.copy(notificationPermissionGranted = granted) }
            sharedPreferences.saveDataBoolean("notification_permission", granted)
        }
    }

    private suspend fun handleNotificationsAllowedUpdate(allowed: Boolean) {
        safeOperation("update notifications allowed") {
            _notificationSettingsState.update { it.copy(areNotificationsAllowed = allowed) }
        }
    }

    private suspend fun handleNotificationToggle(enabled: Boolean) {
        safeOperation("handle notification toggle") {
            if (enabled) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    sharedPreferences.saveDataBoolean(AppConstants.NOTIFICATION_ALLOWED, true)
                } else {
                    launchNotificationSettings()
                }
            } else {
                sharedPreferences.removeData(AppConstants.NOTIFICATION_ALLOWED)
                launchNotificationSettings()
            }
        }
    }

    private suspend fun handleBatteryOptimizationUpdate(exempted: Boolean) {
        safeOperation("update battery optimization") {
            _notificationSettingsState.update { it.copy(batteryOptimizationExempted = exempted) }
            sharedPreferences.saveDataBoolean(AppConstants.BATTERY_OPTIMIZATION, exempted)
        }
    }

    private suspend fun handleBatteryOptimizationToggle(enable: Boolean) {
        safeOperation("handle battery optimization") {
            if (enable) {
                val intent = Intent().apply {
                    action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    data = "package:${context.packageName}".toUri()
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)

                // Log battery optimization request
                firebaseLogger.logEvent(
                    "battery_optimization_requested",
                    null,
                    FirebaseLogger.Companion.EventCategory.USER_ACTION
                )
            } else {
                val intent = Intent().apply {
                    action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)

                // Log battery settings opened
                firebaseLogger.logEvent(
                    "battery_settings_opened",
                    null,
                    FirebaseLogger.Companion.EventCategory.USER_ACTION
                )
            }
        }
    }

    private suspend fun createNotificationChannels() {
        safeOperation("create notification channels") {
            val channelLock = sharedPreferences.getDataBoolean(AppConstants.CHANNEL_LOCK, false)
            if (!channelLock) {
                createAlarms.createAllNotificationChannels(context)
                sharedPreferences.saveDataBoolean(AppConstants.CHANNEL_LOCK, true)

                // Log notification channels created
                firebaseLogger.logEvent(
                    "notification_channels_created",
                    null,
                    FirebaseLogger.Companion.EventCategory.USER_ACTION
                )
            }
        }
    }

    private fun launchNotificationSettings() {
        val intent = Intent().apply {
            action = "android.settings.APP_NOTIFICATION_SETTINGS"
            putExtra("android.provider.extra.APP_PACKAGE", context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)

        // Log notification settings launched
        firebaseLogger.logEvent(
            "notification_settings_launched",
            null,
            FirebaseLogger.Companion.EventCategory.USER_ACTION
        )
    }

    // Calculation method handling
    private suspend fun handleMethodSelection(method: String) {
        safeOperation("update calculation method") {
            _calculationSettingsState.update { it.copy(calculationMethod = method) }

            // Save method to preferences
            sharedPreferences.saveData(AppConstants.CALCULATION_METHOD, method)

            // Apply default parameters for the method
            val defaults = AppConstants.getDefaultParametersForMethod(method)
            saveCalculationDefaults(defaults)

            // Update prayer times
            updatePrayerTimes()
        }
    }

    private suspend fun handleAutoCalculation(enabled: Boolean) {
        safeOperation("handle auto calculation") {
            _calculationSettingsState.update {
                it.copy(
                    isAutoCalculation = enabled,
                    calculationMethod = if (enabled) "OTHER" else it.calculationMethod
                )
            }

            sharedPreferences.saveDataBoolean(AppConstants.AUTO_PARAMETERS, enabled)

            if (enabled) {
                // Calculate and save auto angles
                val latitude = locationSettingsState.value.latitude
                val longitude = locationSettingsState.value.longitude

                val fajrAngle = AutoAnglesCalc().calculateFajrAngle(context, latitude, longitude)
                val ishaAngle = AutoAnglesCalc().calculateIshaaAngle(context, latitude, longitude)

                // Save calculated values
                sharedPreferences.apply {
                    saveData(AppConstants.CALCULATION_METHOD, "OTHER")
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

                // Log auto calculation enabled with angles
                firebaseLogger.logEvent(
                    "auto_calculation_applied",
                    mapOf(
                        "fajr_angle" to fajrAngle,
                        "isha_angle" to ishaAngle,
                        "latitude" to latitude,
                        "longitude" to longitude
                    ),
                    FirebaseLogger.Companion.EventCategory.USER_ACTION
                )
            }

            updatePrayerTimes()
        }
    }

    private suspend fun handleAutoParams(enabled: Boolean) {
        safeOperation("handle auto params") {
            _calculationSettingsState.update { it.copy(autoParams = enabled) }
            sharedPreferences.saveDataBoolean(AppConstants.AUTO_PARAMETERS, enabled)

            if (enabled) {
                val fajrAngle = AutoAnglesCalc().calculateFajrAngle(
                    context,
                    locationSettingsState.value.latitude,
                    locationSettingsState.value.longitude
                )
                val ishaAngle = AutoAnglesCalc().calculateIshaaAngle(
                    context,
                    locationSettingsState.value.latitude,
                    locationSettingsState.value.longitude
                )

                // Update calculation parameters
                sharedPreferences.apply {
                    saveData(AppConstants.CALCULATION_METHOD, "OTHER")
                    saveData(AppConstants.FAJR_ANGLE, fajrAngle.toString())
                    saveData(AppConstants.ISHA_ANGLE, ishaAngle.toString())
                    saveData(AppConstants.HIGH_LATITUDE_RULE, "TWILIGHT_ANGLE")
                }

                // Log auto parameters applied
                firebaseLogger.logEvent(
                    "auto_params_applied",
                    mapOf(
                        "fajr_angle" to fajrAngle,
                        "isha_angle" to ishaAngle
                    ),
                    FirebaseLogger.Companion.EventCategory.USER_ACTION
                )
            }

            updatePrayerTimes()
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

        // Log calculation defaults applied
        firebaseLogger.logEvent(
            "calculation_defaults_applied",
            mapOf("method_defaults" to defaults.keys.joinToString(",")),
            FirebaseLogger.Companion.EventCategory.USER_ACTION
        )
    }
}