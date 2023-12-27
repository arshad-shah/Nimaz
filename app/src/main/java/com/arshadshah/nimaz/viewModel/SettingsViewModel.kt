package com.arshadshah.nimaz.viewModel

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.APP_UPDATE_REQUEST_CODE
import com.arshadshah.nimaz.constants.AppConstants.LOCATION_TYPE
import com.arshadshah.nimaz.constants.AppConstants.THEME_SYSTEM
import com.arshadshah.nimaz.repositories.PrayerTimesRepository
import com.arshadshah.nimaz.repositories.LocationRepository
import com.arshadshah.nimaz.services.LocationService
import com.arshadshah.nimaz.services.PrayerTimesService
import com.arshadshah.nimaz.services.UpdateService
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class SettingsViewModel(context: Context) : ViewModel() {

    val sharedPreferences = PrivateSharedPreferences(context)
    private val locationRepository = LocationRepository(context)
    private val locationService = LocationService(context, LocationRepository(context))
    private val updateService = UpdateService(context)
    private val prayerTimesService = PrayerTimesService(context, PrayerTimesRepository)

    data class PrayerTimesState(
        val nextPrayerTime: LocalDateTime = LocalDateTime.now(),
        val fajrTime: LocalDateTime = LocalDateTime.now(),
        val sunriseTime: LocalDateTime = LocalDateTime.now(),
        val dhuhrTime: LocalDateTime = LocalDateTime.now(),
        val asrTime: LocalDateTime = LocalDateTime.now(),
        val maghribTime: LocalDateTime = LocalDateTime.now(),
        val ishaTime: LocalDateTime = LocalDateTime.now(),
    )

    private val _prayerTimesState = MutableStateFlow(PrayerTimesState())
    val prayerTimesState: StateFlow<PrayerTimesState> = _prayerTimesState.asStateFlow()

    //theme state
    //it has four states: light, dark and system , and dynamic if we are in Build.VERSION.SDK_INT >= Build.VERSION_CODES.S else it has three states: light, dark and system
    private var _theme =
        MutableStateFlow(sharedPreferences.getData(AppConstants.THEME, THEME_SYSTEM))
    val theme = _theme.asStateFlow()

    //dark mode state
    private var _isDarkMode =
        MutableStateFlow(sharedPreferences.getDataBoolean(AppConstants.DARK_MODE, false))
    val isDarkMode = _isDarkMode.asStateFlow()


    //state for switch of location toggle between manual and automatic
    private var _isLocationAuto =
        MutableStateFlow(sharedPreferences.getDataBoolean(LOCATION_TYPE, false))
    val isLocationAuto = _isLocationAuto.asStateFlow()

    //location name loading state
    private var _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    //location name error state
    private var _isError = MutableStateFlow("")
    val isError = _isError.asStateFlow()

    //location name state
    private var _locationName =
        MutableStateFlow(sharedPreferences.getData(AppConstants.LOCATION_INPUT, ""))
    val locationName = _locationName.asStateFlow()

    //latitude state
    private var _latitude =
        MutableStateFlow(sharedPreferences.getDataDouble(AppConstants.LATITUDE, 0.0))
    val latitude = _latitude.asStateFlow()

    //longitude state
    private var _longitude =
        MutableStateFlow(sharedPreferences.getDataDouble(AppConstants.LONGITUDE, 0.0))
    val longitude = _longitude.asStateFlow()

    //battery exempt state
    private var _isBatteryExempt = MutableStateFlow(
        sharedPreferences.getDataBoolean(
            AppConstants.BATTERY_OPTIMIZATION,
            false
        )
    )
    val isBatteryExempt = _isBatteryExempt.asStateFlow()

    //_areNotificationsAllowed
    private var _areNotificationsAllowed = MutableStateFlow(
        sharedPreferences.getDataBoolean(
            AppConstants.NOTIFICATION_ALLOWED,
            false
        )
    )
    val areNotificationsAllowed = _areNotificationsAllowed.asStateFlow()

    //prayer times adjustments state
    //calculation method state
    private var _calculationMethod =
        MutableStateFlow(sharedPreferences.getData(AppConstants.CALCULATION_METHOD, "MWL"))
    val calculationMethod = _calculationMethod.asStateFlow()

    //Madhab state
    private var _madhab = MutableStateFlow(sharedPreferences.getData(AppConstants.MADHAB, "SHAFI"))
    val madhab = _madhab.asStateFlow()

    //high latitude state
    private var _highLatitude = MutableStateFlow(
        sharedPreferences.getData(
            AppConstants.HIGH_LATITUDE_RULE,
            "MIDDLE_OF_THE_NIGHT"
        )
    )
    val highLatitude = _highLatitude.asStateFlow()

    private val _autoParams = MutableStateFlow(
        sharedPreferences.getDataBoolean(
            AppConstants.AUTO_PARAMETERS,
            false
        )
    )
    val autoParams = _autoParams.asStateFlow()

    //fajr angle state
    private var _fajrAngle =
        MutableStateFlow(sharedPreferences.getData(AppConstants.FAJR_ANGLE, "18"))
    val fajrAngle = _fajrAngle.asStateFlow()

    //isha angle state
    private var _ishaAngle =
        MutableStateFlow(sharedPreferences.getData(AppConstants.ISHA_ANGLE, "17"))
    val ishaAngle = _ishaAngle.asStateFlow()

    //ishaAngle visibility state
    private var _ishaAngleVisibility = MutableStateFlow(true)
    val ishaAngleVisibility = _ishaAngleVisibility.asStateFlow()

    //isha interval state
    private var _ishaInterval =
        MutableStateFlow(sharedPreferences.getData(AppConstants.ISHA_INTERVAL, "0"))
    val ishaInterval = _ishaInterval.asStateFlow()

    //offset state
    //fajr
    private var _fajrOffset =
        MutableStateFlow(sharedPreferences.getData(AppConstants.FAJR_ADJUSTMENT, "0"))
    val fajrOffset = _fajrOffset.asStateFlow()

    //sunrise
    private var _sunriseOffset =
        MutableStateFlow(sharedPreferences.getData(AppConstants.SUNRISE_ADJUSTMENT, "0"))
    val sunriseOffset = _sunriseOffset.asStateFlow()

    //dhuhr
    private var _dhuhrOffset =
        MutableStateFlow(sharedPreferences.getData(AppConstants.DHUHR_ADJUSTMENT, "0"))
    val dhuhrOffset = _dhuhrOffset.asStateFlow()

    //asr
    private var _asrOffset =
        MutableStateFlow(sharedPreferences.getData(AppConstants.ASR_ADJUSTMENT, "0"))
    val asrOffset = _asrOffset.asStateFlow()

    //maghrib
    private var _maghribOffset =
        MutableStateFlow(sharedPreferences.getData(AppConstants.MAGHRIB_ADJUSTMENT, "0"))
    val maghribOffset = _maghribOffset.asStateFlow()

    //isha
    private var _ishaOffset =
        MutableStateFlow(sharedPreferences.getData(AppConstants.ISHA_ADJUSTMENT, "0"))
    val ishaOffset = _ishaOffset.asStateFlow()

    private var _isUpdateAvailable = MutableStateFlow(false)
    val isUpdateAvailable = _isUpdateAvailable.asStateFlow()

    //events
    sealed class SettingsEvent {

        class LocationToggle(val context: Context, val checked: Boolean) : SettingsEvent()
        class LocationInput(val context: Context, val location: String) : SettingsEvent()

        //events to update latitude and longitude
        class Latitude(val context: Context, val latitude: Double) : SettingsEvent()
        class Longitude(val context: Context, val longitude: Double) : SettingsEvent()
        class LoadLocation(val context: Context) : SettingsEvent()
        class BatteryExempt(val exempt: Boolean) : SettingsEvent()
        class NotificationsAllowed(val allowed: Boolean) : SettingsEvent()

        //prayer times adjustments
        //calculation method
        class CalculationMethod(val method: String) : SettingsEvent()
        class Madhab(val madhab: String) : SettingsEvent()
        class HighLatitude(val rule: String) : SettingsEvent()
        class FajrAngle(val angle: String) : SettingsEvent()
        class IshaAngle(val angle: String) : SettingsEvent()
        class IshaAngleVisibility(val visible: Boolean) : SettingsEvent()
        class IshaInterval(val interval: String) : SettingsEvent()

        //offset
        class FajrOffset(val offset: String) : SettingsEvent()
        class SunriseOffset(val offset: String) : SettingsEvent()
        class DhuhrOffset(val offset: String) : SettingsEvent()
        class AsrOffset(val offset: String) : SettingsEvent()
        class MaghribOffset(val offset: String) : SettingsEvent()
        class IshaOffset(val offset: String) : SettingsEvent()

        object LoadSettings : SettingsEvent()

        //theme
        class Theme(val theme: String) : SettingsEvent()

        //dark mode
        class DarkMode(val darkMode: Boolean) : SettingsEvent()

        //update settings based on calculation method
        class UpdateSettings(val method: String) : SettingsEvent()
        class AutoParameters(val checked: Boolean) : SettingsEvent()

        class CheckUpdate(val context: Context, val doUpdate: Boolean) : SettingsEvent()

        object LoadPrayerTimes : SettingsEvent()
    }

    //events for the settings screen
    fun handleEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.LocationToggle -> {
                sharedPreferences.saveDataBoolean(LOCATION_TYPE, event.checked)
                _isLocationAuto.value = event.checked
                loadLocation(event.checked)
                Log.d("Nimaz: SettingsViewModel", "Location toggle : ${event.checked}")
            }

            is SettingsEvent.LocationInput -> {
                _locationName.value = event.location
                sharedPreferences.saveData(AppConstants.LOCATION_INPUT, event.location)
                val locationData = locationRepository.forwardGeocode(event.location)
                locationData.onSuccess {
                    _latitude.value = it.latitude
                    _longitude.value = it.longitude
                    _locationName.value = it.locationName
                    sharedPreferences.saveDataDouble(AppConstants.LATITUDE, it.latitude)
                    sharedPreferences.saveDataDouble(AppConstants.LONGITUDE, it.longitude)
                    Log.d("Nimaz: SettingsViewModel", "Location input : ${event.location}")
                }
                locationData.onFailure {
                    val locationLatitudeFromStorage =
                        sharedPreferences.getDataDouble(AppConstants.LATITUDE, 53.3498)
                    val locationLongitudeFromStorage =
                        sharedPreferences.getDataDouble(AppConstants.LONGITUDE, -6.2603)
                    _latitude.value = locationLatitudeFromStorage
                    _longitude.value = locationLongitudeFromStorage
                    Log.d("Nimaz: SettingsViewModel", "Location input : ${event.location}")
                }
            }

            is SettingsEvent.Latitude -> {
                _latitude.value = event.latitude
                sharedPreferences.saveData(AppConstants.LATITUDE, event.latitude.toString())
                loadLocation(sharedPreferences.getDataBoolean(LOCATION_TYPE, true))
                Log.d("Nimaz: SettingsViewModel", "Latitude : ${event.latitude}")
            }

            is SettingsEvent.Longitude -> {
                _longitude.value = event.longitude
                sharedPreferences.saveData(AppConstants.LONGITUDE, event.longitude.toString())
                loadLocation(sharedPreferences.getDataBoolean(LOCATION_TYPE, true))
                Log.d("Nimaz: SettingsViewModel", "Longitude : ${event.longitude}")
            }

            is SettingsEvent.LoadLocation -> {
                loadLocation(sharedPreferences.getDataBoolean(LOCATION_TYPE, true))
                Log.d("Nimaz: SettingsViewModel", "Load location")
            }

            is SettingsEvent.BatteryExempt -> {
                _isBatteryExempt.value = event.exempt
                sharedPreferences.saveDataBoolean(AppConstants.BATTERY_OPTIMIZATION, event.exempt)
                Log.d("Nimaz: SettingsViewModel", "Battery exempt : ${event.exempt}")
            }

            is SettingsEvent.NotificationsAllowed -> {
                _areNotificationsAllowed.value = event.allowed
                sharedPreferences.saveDataBoolean(AppConstants.NOTIFICATION_ALLOWED, event.allowed)
                Log.d("Nimaz: SettingsViewModel", "Notifications allowed : ${event.allowed}")
            }

            is SettingsEvent.CalculationMethod -> {
                _calculationMethod.value = event.method
                sharedPreferences.saveData(AppConstants.CALCULATION_METHOD, event.method)
                Log.d("Nimaz: SettingsViewModel", "Calculation method : ${event.method}")
            }

            is SettingsEvent.Madhab -> {
                _madhab.value = event.madhab
                sharedPreferences.saveData(AppConstants.MADHAB, event.madhab)
                Log.d("Nimaz: SettingsViewModel", "Madhab : ${event.madhab}")
            }

            is SettingsEvent.HighLatitude -> {
                _highLatitude.value = event.rule
                sharedPreferences.saveData(AppConstants.HIGH_LATITUDE_RULE, event.rule)
                Log.d("Nimaz: SettingsViewModel", "High latitude rule : ${event.rule}")
            }

            is SettingsEvent.FajrAngle -> {
                _fajrAngle.value = event.angle
                sharedPreferences.saveData(AppConstants.FAJR_ANGLE, event.angle)
                Log.d("Nimaz: SettingsViewModel", "Fajr angle : ${event.angle}")
            }

            is SettingsEvent.IshaAngle -> {
                _ishaAngle.value = event.angle
                sharedPreferences.saveData(AppConstants.ISHA_ANGLE, event.angle)
                Log.d("Nimaz: SettingsViewModel", "Isha angle : ${event.angle}")
            }

            is SettingsEvent.IshaAngleVisibility -> {
                _ishaAngleVisibility.value = event.visible
                if (!event.visible) {
                    _ishaAngle.value = "0"
                    sharedPreferences.saveData(AppConstants.ISHA_ANGLE, "0")
                }
                Log.d("Nimaz: SettingsViewModel", "Isha angle visibility : ${event.visible}")
            }

            is SettingsEvent.IshaInterval -> {
                _ishaInterval.value = event.interval
                sharedPreferences.saveData(AppConstants.ISHA_INTERVAL, event.interval)
                Log.d("Nimaz: SettingsViewModel", "Isha interval : ${event.interval}")
            }
            //offset
            is SettingsEvent.FajrOffset -> {
                _fajrOffset.value = event.offset
                sharedPreferences.saveData(AppConstants.FAJR_ADJUSTMENT, event.offset)
            }

            is SettingsEvent.SunriseOffset -> {
                _sunriseOffset.value = event.offset
                sharedPreferences.saveData(AppConstants.SUNRISE_ADJUSTMENT, event.offset)
            }

            is SettingsEvent.DhuhrOffset -> {
                _dhuhrOffset.value = event.offset
                sharedPreferences.saveData(AppConstants.DHUHR_ADJUSTMENT, event.offset)
            }

            is SettingsEvent.AsrOffset -> {
                _asrOffset.value = event.offset
                sharedPreferences.saveData(AppConstants.ASR_ADJUSTMENT, event.offset)
            }

            is SettingsEvent.MaghribOffset -> {
                _maghribOffset.value = event.offset
                sharedPreferences.saveData(AppConstants.MAGHRIB_ADJUSTMENT, event.offset)
            }

            is SettingsEvent.IshaOffset -> {
                _ishaOffset.value = event.offset
                sharedPreferences.saveData(AppConstants.ISHA_ADJUSTMENT, event.offset)
            }

            is SettingsEvent.LoadSettings -> {
                _isLoading.value = true
                _isLocationAuto.value = sharedPreferences.getDataBoolean(LOCATION_TYPE, false)
                _locationName.value = sharedPreferences.getData(AppConstants.LOCATION_INPUT, "")
                _latitude.value = sharedPreferences.getDataDouble(AppConstants.LATITUDE, 0.0)
                _longitude.value = sharedPreferences.getDataDouble(AppConstants.LONGITUDE, 0.0)
                _isBatteryExempt.value =
                    sharedPreferences.getDataBoolean(AppConstants.BATTERY_OPTIMIZATION, false)
                _calculationMethod.value =
                    sharedPreferences.getData(AppConstants.CALCULATION_METHOD, "MWL")
                _madhab.value = sharedPreferences.getData(AppConstants.MADHAB, "SHAFI")
                _highLatitude.value =
                    sharedPreferences.getData(
                        AppConstants.HIGH_LATITUDE_RULE,
                        "MIDDLE_OF_THE_NIGHT"
                    )
                _fajrAngle.value = sharedPreferences.getData(AppConstants.FAJR_ANGLE, "18")
                _ishaAngle.value = sharedPreferences.getData(AppConstants.ISHA_ANGLE, "17")
                val isNotAnIntervalMethod = when (_calculationMethod.value) {
                    "MAKKAH", "QATAR", "GULF" -> false
                    else -> true
                }
                _ishaAngleVisibility.value = isNotAnIntervalMethod
                _ishaInterval.value = sharedPreferences.getData(AppConstants.ISHA_INTERVAL, "0")
                _fajrOffset.value = sharedPreferences.getData(AppConstants.FAJR_ADJUSTMENT, "0")
                _sunriseOffset.value =
                    sharedPreferences.getData(AppConstants.SUNRISE_ADJUSTMENT, "0")
                _dhuhrOffset.value = sharedPreferences.getData(AppConstants.DHUHR_ADJUSTMENT, "0")
                _asrOffset.value = sharedPreferences.getData(AppConstants.ASR_ADJUSTMENT, "0")
                _maghribOffset.value =
                    sharedPreferences.getData(AppConstants.MAGHRIB_ADJUSTMENT, "0")
                _ishaOffset.value = sharedPreferences.getData(AppConstants.ISHA_ADJUSTMENT, "0")
                _isLoading.value = false
                Log.d("Nimaz: SettingsViewModel", "Settings loaded")
            }

            is SettingsEvent.Theme -> {
                _theme.value = event.theme
                sharedPreferences.saveData(AppConstants.THEME, event.theme)
                Log.d("Nimaz: SettingsViewModel", "Theme : ${event.theme}")
            }

            is SettingsEvent.DarkMode -> {
                _isDarkMode.value = event.darkMode
                sharedPreferences.saveDataBoolean(AppConstants.DARK_MODE, event.darkMode)
                Log.d("Nimaz: SettingsViewModel", "Dark mode : ${event.darkMode}")
            }

            is SettingsEvent.UpdateSettings -> {
                val defaultsForMethod = AppConstants.getDefaultParametersForMethod(event.method)
                _fajrAngle.value = defaultsForMethod["fajrAngle"]!!
                _ishaAngle.value = defaultsForMethod["ishaAngle"]!!
                val shouldBeVisible = defaultsForMethod["ishaInterval"] == "0"
                _ishaAngleVisibility.value = shouldBeVisible
                _ishaInterval.value = defaultsForMethod["ishaInterval"]!!
                _madhab.value = defaultsForMethod["madhab"]!!
                _highLatitude.value = defaultsForMethod["highLatitudeRule"]!!
                _fajrOffset.value = defaultsForMethod["fajrAdjustment"]!!
                _sunriseOffset.value = defaultsForMethod["sunriseAdjustment"]!!
                _dhuhrOffset.value = defaultsForMethod["dhuhrAdjustment"]!!
                _asrOffset.value = defaultsForMethod["asrAdjustment"]!!
                _maghribOffset.value = defaultsForMethod["maghribAdjustment"]!!
                _ishaOffset.value = defaultsForMethod["ishaAdjustment"]!!

                //save it to shared preferences
                sharedPreferences.saveData(AppConstants.CALCULATION_METHOD, event.method)
                sharedPreferences.saveData(AppConstants.MADHAB, defaultsForMethod["madhab"]!!)
                sharedPreferences.saveData(
                    AppConstants.HIGH_LATITUDE_RULE,
                    defaultsForMethod["highLatitudeRule"]!!
                )
                sharedPreferences.saveData(
                    AppConstants.FAJR_ANGLE,
                    defaultsForMethod["fajrAngle"]!!
                )
                sharedPreferences.saveData(
                    AppConstants.ISHA_ANGLE,
                    defaultsForMethod["ishaAngle"]!!
                )
                sharedPreferences.saveData(
                    AppConstants.ISHA_INTERVAL,
                    defaultsForMethod["ishaInterval"]!!
                )
                sharedPreferences.saveData(
                    AppConstants.FAJR_ADJUSTMENT,
                    defaultsForMethod["fajrAdjustment"]!!
                )
                sharedPreferences.saveData(
                    AppConstants.SUNRISE_ADJUSTMENT,
                    defaultsForMethod["sunriseAdjustment"]!!
                )
                sharedPreferences.saveData(
                    AppConstants.DHUHR_ADJUSTMENT,
                    defaultsForMethod["dhuhrAdjustment"]!!
                )
                sharedPreferences.saveData(
                    AppConstants.ASR_ADJUSTMENT,
                    defaultsForMethod["asrAdjustment"]!!
                )
                sharedPreferences.saveData(
                    AppConstants.MAGHRIB_ADJUSTMENT,
                    defaultsForMethod["maghribAdjustment"]!!
                )
                sharedPreferences.saveData(
                    AppConstants.ISHA_ADJUSTMENT,
                    defaultsForMethod["ishaAdjustment"]!!
                )
                Log.d("Nimaz: SettingsViewModel", "Settings updated")

            }

            is SettingsEvent.AutoParameters -> {
                sharedPreferences.saveDataBoolean(AppConstants.AUTO_PARAMETERS, event.checked)
                _autoParams.value = event.checked
                Log.d("Nimaz: SettingsViewModel", "Auto parameters : ${event.checked}")
            }

            is SettingsEvent.CheckUpdate -> {
                Log.d("Nimaz: SettingsViewModel", "Checking for update")
                updateService.checkForUpdate(event.doUpdate) { updateIsAvailable ->
                    _isUpdateAvailable.value = updateIsAvailable
                    if (event.doUpdate && updateIsAvailable) {
                        updateService.startUpdateFlowForResult(
                            event.context.applicationContext as Activity,
                            APP_UPDATE_REQUEST_CODE
                        )
                    }
                }
            }

            is SettingsEvent.LoadPrayerTimes -> {
                loadPrayerTimes()
            }
        }
    }

    //load location from shared preferences
    private fun loadLocation(checked: Boolean) {
        locationService.loadLocation(checked, onSuccess = {
            _locationName.value = it.locationName
            _latitude.value = it.latitude
            _longitude.value = it.longitude
        },
            onError = {
                _latitude.value =
                    sharedPreferences.getDataDouble(AppConstants.LATITUDE, 53.3498)
                _longitude.value =
                    sharedPreferences.getDataDouble(AppConstants.LONGITUDE, -6.2603)
                _locationName.value = sharedPreferences.getData(AppConstants.LOCATION_INPUT, "")
                _isError.value = it
            })
    }

    private fun loadPrayerTimes() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _isError.value = ""
            try {
                val prayerTimes = prayerTimesService.getPrayerTimes()

                _prayerTimesState.update {
                    it.copy(
                        fajrTime = prayerTimes?.fajr ?: LocalDateTime.now(),
                        sunriseTime = prayerTimes?.sunrise ?: LocalDateTime.now(),
                        dhuhrTime = prayerTimes?.dhuhr ?: LocalDateTime.now(),
                        asrTime = prayerTimes?.asr ?: LocalDateTime.now(),
                        maghribTime = prayerTimes?.maghrib ?: LocalDateTime.now(),
                        ishaTime = prayerTimes?.isha ?: LocalDateTime.now()
                    )
                }

                _isLoading.value = false

            } catch (e: Exception) {
                Log.d(
                    AppConstants.PRAYER_TIMES_SCREEN_TAG + "Viewmodel",
                    "loadPrayerTimes: ${e.message}"
                )
                _isError.value = e.message.toString()
                _isLoading.value = false
            }
        }
    }
}