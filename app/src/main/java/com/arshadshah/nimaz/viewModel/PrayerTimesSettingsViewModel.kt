package com.arshadshah.nimaz.viewModel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.getDefaultParametersForMethod
import com.arshadshah.nimaz.data.local.models.Parameters
import com.arshadshah.nimaz.libs.prayertimes.enums.CalculationMethod
import com.arshadshah.nimaz.libs.prayertimes.enums.HighLatitudeRule
import com.arshadshah.nimaz.libs.prayertimes.enums.Madhab
import com.arshadshah.nimaz.repositories.PrayerTimesRepository
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.sunMoonUtils.AutoAnglesCalc
import com.arshadshah.nimaz.widgets.prayertimesthin.PrayerTimeWorker
import com.arshadshah.nimaz.widgets.prayertimestrackerthin.PrayerTimesTrackerWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class PrayerTimesSettingsViewModel @Inject constructor(
    private val sharedPreferences: PrivateSharedPreferences,
    @ApplicationContext private val context: Context,
    private val prayerTimesRepository: PrayerTimesRepository
) : ViewModel() {

    private val viewModelScopeRun = viewModelScope

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _calculationMethod = MutableStateFlow("KARACHI")
    val calculationMethod: StateFlow<String> = _calculationMethod

    private val _autoParams = MutableStateFlow(
        sharedPreferences.getDataBoolean(
            AppConstants.AUTO_PARAMETERS,
            false
        )
    )
    val autoParams = _autoParams.asStateFlow()

    // MutableStateFlow for all settings
    private val _madhab = MutableStateFlow("SHAFI")
    val madhab: StateFlow<String> = _madhab

    private val _highLatitude = MutableStateFlow("MIDDLE_OF_THE_NIGHT")
    val highLatitude: StateFlow<String> = _highLatitude

    private val _fajrAngle = MutableStateFlow("18")
    val fajrAngle: StateFlow<String> = _fajrAngle

    private val _ishaAngle = MutableStateFlow("18")
    val ishaAngle: StateFlow<String> = _ishaAngle

    private val _ishaInterval = MutableStateFlow("0")
    val ishaInterval: StateFlow<String> = _ishaInterval

    private val _ishaAngleVisibility = MutableStateFlow(true)
    val ishaAngleVisibility: StateFlow<Boolean> = _ishaAngleVisibility

    // Prayer time adjustments
    private val _fajrOffset = MutableStateFlow("0")
    val fajrOffset: StateFlow<String> = _fajrOffset

    private val _sunriseOffset = MutableStateFlow("0")
    val sunriseOffset: StateFlow<String> = _sunriseOffset

    private val _dhuhrOffset = MutableStateFlow("0")
    val dhuhrOffset: StateFlow<String> = _dhuhrOffset

    private val _asrOffset = MutableStateFlow("0")
    val asrOffset: StateFlow<String> = _asrOffset

    private val _maghribOffset = MutableStateFlow("0")
    val maghribOffset: StateFlow<String> = _maghribOffset

    private val _ishaOffset = MutableStateFlow("0")
    val ishaOffset: StateFlow<String> = _ishaOffset

    sealed class SettingsEvent {
        data object LoadSettings : SettingsEvent()
        data object ClearError : SettingsEvent()
        data class CalculationMethod(val value: String) : SettingsEvent()
        data class AutoParameters(val checked: Boolean) : SettingsEvent()
        data class Madhab(val value: String) : SettingsEvent()
        data class HighLatitude(val value: String) : SettingsEvent()
        data class FajrAngle(val value: String) : SettingsEvent()
        data class IshaAngle(val value: String) : SettingsEvent()
        data class IshaInterval(val value: String) : SettingsEvent()
        data class FajrOffset(val value: String) : SettingsEvent()
        data class SunriseOffset(val value: String) : SettingsEvent()
        data class DhuhrOffset(val value: String) : SettingsEvent()
        data class AsrOffset(val value: String) : SettingsEvent()
        data class MaghribOffset(val value: String) : SettingsEvent()
        data class IshaOffset(val value: String) : SettingsEvent()
    }

    private val _parameters = MutableStateFlow<Parameters?>(null)
    val parameters: StateFlow<Parameters?> = _parameters

    init {
        loadSettings()
        updateParameters()
    }

    // Helper method to handle errors consistently
    private fun handleError(operation: String, e: Exception) {
        val errorMessage = "Failed to $operation: ${e.localizedMessage}"
        _error.value = errorMessage
        Log.e("PrayerTimesSettingsVM", errorMessage, e)
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
            delay(1000)
            _isLoading.value = false
        }
    }

    private fun updateWidget(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            safeOperation("updating parameters") {
                PrayerTimeWorker.enqueue(context, true)
                PrayerTimesTrackerWorker.enqueue(context, true)
            }
        }
    }

    private fun updateParameters() {
        viewModelScope.launch {
            safeOperation("updating parameters") {
                _parameters.value = createParameters()
                prayerTimesRepository.updatePrayerTimes(createParameters())
                updateWidget(context)
            }
        }
    }

    private fun createParameters(): Parameters {
        val latitude = sharedPreferences.getDataDouble(AppConstants.LATITUDE, 0.0)
        val longitude = sharedPreferences.getDataDouble(AppConstants.LONGITUDE, 0.0)

        return Parameters(
            latitude = latitude,
            longitude = longitude,
            date = LocalDateTime.now().toString(),
            fajrAngle = _fajrAngle.value.toDouble(),
            ishaAngle = _ishaAngle.value.toDouble(),
            method = CalculationMethod.valueOf(
                sharedPreferences.getData(AppConstants.CALCULATION_METHOD, "IRELAND")
            ),
            madhab = Madhab.valueOf(_madhab.value),
            highLatitudeRule = HighLatitudeRule.valueOf(_highLatitude.value),
            fajrAdjustment = _fajrOffset.value.toInt(),
            sunriseAdjustment = _sunriseOffset.value.toInt(),
            dhuhrAdjustment = _dhuhrOffset.value.toInt(),
            asrAdjustment = _asrOffset.value.toInt(),
            maghribAdjustment = _maghribOffset.value.toInt(),
            ishaAdjustment = _ishaOffset.value.toInt(),
            ishaInterval = _ishaInterval.value.toInt()
        )
    }

    private fun loadSettings() {
        viewModelScope.launch {
            safeOperation("Loading settings") {
                _autoParams.value =
                    sharedPreferences.getDataBoolean(AppConstants.AUTO_PARAMETERS, false)
                _calculationMethod.value =
                    sharedPreferences.getData(AppConstants.CALCULATION_METHOD, "KARACHI")
                _madhab.value = sharedPreferences.getData(AppConstants.MADHAB, "SHAFI")
                _highLatitude.value = sharedPreferences.getData(
                    AppConstants.HIGH_LATITUDE_RULE,
                    "MIDDLE_OF_THE_NIGHT"
                )
                _fajrAngle.value = sharedPreferences.getData(AppConstants.FAJR_ANGLE, "18")
                _ishaAngle.value = sharedPreferences.getData(AppConstants.ISHA_ANGLE, "18")
                _ishaInterval.value = sharedPreferences.getData(AppConstants.ISHA_INTERVAL, "0")

                // Load prayer time adjustments
                _fajrOffset.value = sharedPreferences.getData(AppConstants.FAJR_ADJUSTMENT, "0")
                _sunriseOffset.value =
                    sharedPreferences.getData(AppConstants.SUNRISE_ADJUSTMENT, "0")
                _dhuhrOffset.value = sharedPreferences.getData(AppConstants.DHUHR_ADJUSTMENT, "0")
                _asrOffset.value = sharedPreferences.getData(AppConstants.ASR_ADJUSTMENT, "0")
                _maghribOffset.value =
                    sharedPreferences.getData(AppConstants.MAGHRIB_ADJUSTMENT, "0")
                _ishaOffset.value = sharedPreferences.getData(AppConstants.ISHA_ADJUSTMENT, "0")

                val isNotAnIntervalMethod = when (_calculationMethod.value) {
                    "MAKKAH", "QATAR", "GULF" -> false
                    else -> true
                }
                _ishaAngleVisibility.value = isNotAnIntervalMethod
                sharedPreferences.saveDataBoolean(
                    AppConstants.ALARM_LOCK,
                    false
                )
            }
        }
    }

    fun handleEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.LoadSettings -> loadSettings()
            is SettingsEvent.ClearError -> _error.value = null
            is SettingsEvent.CalculationMethod -> {
                viewModelScopeRun.launch {
                    safeOperation("Updating calculation method") {
                        _calculationMethod.value = event.value
                        sharedPreferences.saveData(AppConstants.CALCULATION_METHOD, event.value)
                    }
                }
                applyDefaultParameters(event.value)
                loadSettings()
            }

            is SettingsEvent.AutoParameters -> {
                viewModelScopeRun.launch {
                    safeOperation("Updating auto parameters") {
                        _autoParams.value = event.checked
                        sharedPreferences.saveDataBoolean(
                            AppConstants.AUTO_PARAMETERS,
                            event.checked
                        )
                        if (event.checked) {
                            val autoFajrAngle = AutoAnglesCalc().calculateFajrAngle(
                                context,
                                sharedPreferences.getDataDouble(AppConstants.LATITUDE, 0.0),
                                sharedPreferences.getDataDouble(AppConstants.LONGITUDE, 0.0)
                            )
                            val autoIshaAngle = AutoAnglesCalc().calculateIshaaAngle(
                                context,
                                sharedPreferences.getDataDouble(AppConstants.LATITUDE, 0.0),
                                sharedPreferences.getDataDouble(AppConstants.LONGITUDE, 0.0)
                            )

                            _fajrAngle.value = autoFajrAngle.toString()
                            _ishaAngle.value = autoIshaAngle.toString()
                        } else {
                            applyDefaultParameters(_calculationMethod.value)
                        }
                    }
                }
                updateParameters()
                loadSettings()
            }

            is SettingsEvent.Madhab -> {
                viewModelScopeRun.launch {
                    safeOperation("Updating madhab") {
                        _madhab.value = event.value
                        sharedPreferences.saveData(AppConstants.MADHAB, event.value)
                    }
                }
                updateParameters()
                loadSettings()
            }

            is SettingsEvent.HighLatitude -> {
                viewModelScopeRun.launch {
                    safeOperation("Updating high latitude rule") {
                        _highLatitude.value = event.value
                        sharedPreferences.saveData(AppConstants.HIGH_LATITUDE_RULE, event.value)
                    }
                }
                updateParameters()
                loadSettings()
            }

            is SettingsEvent.FajrAngle -> {
                viewModelScopeRun.launch {
                    safeOperation("Updating fajr angle") {
                        _fajrAngle.value = event.value
                        sharedPreferences.saveData(AppConstants.FAJR_ANGLE, event.value)
                    }
                }
                updateParameters()
                loadSettings()
            }

            is SettingsEvent.IshaAngle -> {
                viewModelScopeRun.launch {
                    safeOperation("Updating isha angle") {
                        _ishaAngle.value = event.value
                        sharedPreferences.saveData(AppConstants.ISHA_ANGLE, event.value)
                    }
                }
                updateParameters()
                loadSettings()
            }

            is SettingsEvent.IshaInterval -> {
                viewModelScopeRun.launch {
                    safeOperation("Updating isha interval") {
                        _ishaInterval.value = event.value
                        sharedPreferences.saveData(AppConstants.ISHA_INTERVAL, event.value)
                    }
                }
                updateParameters()
                loadSettings()
            }

            is SettingsEvent.FajrOffset -> {
                viewModelScopeRun.launch {
                    safeOperation("Updating fajr offset") {
                        _fajrOffset.value = event.value
                        sharedPreferences.saveData(AppConstants.FAJR_ADJUSTMENT, event.value)
                    }
                }
                updateParameters()
                loadSettings()
            }

            is SettingsEvent.SunriseOffset -> {
                viewModelScopeRun.launch {
                    safeOperation("Updating sunrise offset") {
                        _sunriseOffset.value = event.value
                        sharedPreferences.saveData(AppConstants.SUNRISE_ADJUSTMENT, event.value)
                    }
                }
                updateParameters()
                loadSettings()
            }

            is SettingsEvent.DhuhrOffset -> {
                viewModelScopeRun.launch {
                    safeOperation("Updating dhuhr offset") {
                        _dhuhrOffset.value = event.value
                        sharedPreferences.saveData(AppConstants.DHUHR_ADJUSTMENT, event.value)
                    }
                }
                updateParameters()
                loadSettings()
            }

            is SettingsEvent.AsrOffset -> {
                viewModelScopeRun.launch {
                    safeOperation("Updating asr offset") {
                        _asrOffset.value = event.value
                        sharedPreferences.saveData(AppConstants.ASR_ADJUSTMENT, event.value)
                    }
                }
                updateParameters()
                loadSettings()
            }

            is SettingsEvent.MaghribOffset -> {
                viewModelScopeRun.launch {
                    safeOperation("Updating maghrib offset") {
                        _maghribOffset.value = event.value
                        sharedPreferences.saveData(AppConstants.MAGHRIB_ADJUSTMENT, event.value)
                    }
                }
                updateParameters()
                loadSettings()
            }

            is SettingsEvent.IshaOffset -> {
                viewModelScopeRun.launch {
                    safeOperation("Updating isha offset") {
                        _ishaOffset.value = event.value
                        sharedPreferences.saveData(AppConstants.ISHA_ADJUSTMENT, event.value)
                    }
                }
                updateParameters()
                loadSettings()
            }
        }
    }

    private fun applyDefaultParameters(method: String) {
        val defaultParams = getDefaultParametersForMethod(method)

        viewModelScope.launch {
            safeOperation("Applying default parameters") {
                // Update all StateFlows with default values
                defaultParams["fajrAngle"]?.let { _fajrAngle.value = it }
                defaultParams["ishaAngle"]?.let { _ishaAngle.value = it }
                defaultParams["ishaInterval"]?.let { _ishaInterval.value = it }
                defaultParams["madhab"]?.let { _madhab.value = it }
                defaultParams["highLatitudeRule"]?.let { _highLatitude.value = it }
                defaultParams["fajrAdjustment"]?.let { _fajrOffset.value = it }
                defaultParams["sunriseAdjustment"]?.let { _sunriseOffset.value = it }
                defaultParams["dhuhrAdjustment"]?.let { _dhuhrOffset.value = it }
                defaultParams["asrAdjustment"]?.let { _asrOffset.value = it }
                defaultParams["maghribAdjustment"]?.let { _maghribOffset.value = it }
                defaultParams["ishaAdjustment"]?.let { _ishaOffset.value = it }

                // Save all values to SharedPreferences
                defaultParams.forEach { (key, value) ->
                    when (key) {
                        "fajrAngle" -> sharedPreferences.saveData(AppConstants.FAJR_ANGLE, value)
                        "ishaAngle" -> sharedPreferences.saveData(AppConstants.ISHA_ANGLE, value)
                        "ishaInterval" -> sharedPreferences.saveData(
                            AppConstants.ISHA_INTERVAL,
                            value
                        )

                        "madhab" -> sharedPreferences.saveData(AppConstants.MADHAB, value)
                        "highLatitudeRule" -> sharedPreferences.saveData(
                            AppConstants.HIGH_LATITUDE_RULE,
                            value
                        )

                        "fajrAdjustment" -> sharedPreferences.saveData(
                            AppConstants.FAJR_ADJUSTMENT,
                            value
                        )

                        "sunriseAdjustment" -> sharedPreferences.saveData(
                            AppConstants.SUNRISE_ADJUSTMENT,
                            value
                        )

                        "dhuhrAdjustment" -> sharedPreferences.saveData(
                            AppConstants.DHUHR_ADJUSTMENT,
                            value
                        )

                        "asrAdjustment" -> sharedPreferences.saveData(
                            AppConstants.ASR_ADJUSTMENT,
                            value
                        )

                        "maghribAdjustment" -> sharedPreferences.saveData(
                            AppConstants.MAGHRIB_ADJUSTMENT,
                            value
                        )

                        "ishaAdjustment" -> sharedPreferences.saveData(
                            AppConstants.ISHA_ADJUSTMENT,
                            value
                        )
                    }
                }
            }

            // Update parameters after applying defaults
            updateParameters()
        }
    }
}