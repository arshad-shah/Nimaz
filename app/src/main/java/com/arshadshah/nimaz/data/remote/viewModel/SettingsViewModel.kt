package com.arshadshah.nimaz.data.remote.viewModel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.LOCATION_TYPE
import com.arshadshah.nimaz.utils.Location
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.location.LocationFinder
import com.arshadshah.nimaz.utils.location.LocationFinderAuto
import com.arshadshah.nimaz.utils.location.NetworkChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(context: Context) : ViewModel()
{
	val sharedPreferences = PrivateSharedPreferences(context)


	//state for switch of location toggle between manual and automatic
	private var _isLocationAuto = MutableStateFlow(sharedPreferences.getDataBoolean(LOCATION_TYPE, false))
	val isLocationAuto = _isLocationAuto.asStateFlow()

	//location name loading state
	private var _isLocationNameLoading = MutableStateFlow(false)
	val isLocationNameLoading = _isLocationNameLoading.asStateFlow()

	//location name state
	private var _locationName = MutableStateFlow(sharedPreferences.getData(AppConstants.LOCATION_INPUT, ""))
	val locationName = _locationName.asStateFlow()

	//latitude state
	private var _latitude = MutableStateFlow(sharedPreferences.getDataDouble(AppConstants.LATITUDE, 53.3498))
	val latitude = _latitude.asStateFlow()

	//longitude state
	private var _longitude = MutableStateFlow(sharedPreferences.getDataDouble(AppConstants.LONGITUDE, -6.2603))
	val longitude = _longitude.asStateFlow()

	//battery exempt state
	private var _isBatteryExempt = MutableStateFlow(sharedPreferences.getDataBoolean(AppConstants.BATTERY_OPTIMIZATION, false))
	val isBatteryExempt = _isBatteryExempt.asStateFlow()

	//prayer times adjustments state
	//calculation method state
	private var _calculationMethod = MutableStateFlow(sharedPreferences.getData(AppConstants.CALCULATION_METHOD, "IRELAND"))
	val calculationMethod = _calculationMethod.asStateFlow()

	//Madhab state
	private var _madhab = MutableStateFlow(sharedPreferences.getData(AppConstants.MADHAB, "HANAFI"))
	val madhab = _madhab.asStateFlow()

	//high latitude state
	private var _highLatitude = MutableStateFlow(sharedPreferences.getData(AppConstants.HIGH_LATITUDE_RULE, "MIDDLE_OF_THE_NIGHT"))
	val highLatitude = _highLatitude.asStateFlow()

	//fajr angle state
	private var _fajrAngle = MutableStateFlow(sharedPreferences.getData(AppConstants.FAJR_ANGLE, "18"))
	val fajrAngle = _fajrAngle.asStateFlow()

	//isha angle state
	private var _ishaAngle = MutableStateFlow(sharedPreferences.getData(AppConstants.ISHA_ANGLE, "18"))
	val ishaAngle = _ishaAngle.asStateFlow()

	//ishaAngle visibility state
	private var _ishaAngleVisibility = MutableStateFlow(sharedPreferences.getData(AppConstants.CALCULATION_METHOD, "IRELAND") == "MAKKAH" || sharedPreferences.getData(AppConstants.CALCULATION_METHOD, "IRELAND") == "QATAR" || sharedPreferences.getData(AppConstants.CALCULATION_METHOD, "IRELAND") == "GULF")
	val ishaAngleVisibility = _ishaAngleVisibility.asStateFlow()

	//isha interval state
	private var _ishaInterval = MutableStateFlow(sharedPreferences.getData(AppConstants.ISHA_INTERVAL, "0"))
	val ishaInterval = _ishaInterval.asStateFlow()

	//offset state
	//fajr
	private var _fajrOffset = MutableStateFlow(sharedPreferences.getData(AppConstants.FAJR_ADJUSTMENT, "0"))
	val fajrOffset = _fajrOffset.asStateFlow()

	//sunrise
	private var _sunriseOffset = MutableStateFlow(sharedPreferences.getData(AppConstants.SUNRISE_ADJUSTMENT, "0"))
	val sunriseOffset = _sunriseOffset.asStateFlow()

	//dhuhr
	private var _dhuhrOffset = MutableStateFlow(sharedPreferences.getData(AppConstants.DHUHR_ADJUSTMENT, "0"))
	val dhuhrOffset = _dhuhrOffset.asStateFlow()

	//asr
	private var _asrOffset = MutableStateFlow(sharedPreferences.getData(AppConstants.ASR_ADJUSTMENT, "0"))
	val asrOffset = _asrOffset.asStateFlow()

	//maghrib
	private var _maghribOffset = MutableStateFlow(sharedPreferences.getData(AppConstants.MAGHRIB_ADJUSTMENT, "0"))
	val maghribOffset = _maghribOffset.asStateFlow()

	//isha
	private var _ishaOffset = MutableStateFlow(sharedPreferences.getData(AppConstants.ISHA_ADJUSTMENT, "0"))
	val ishaOffset = _ishaOffset.asStateFlow()

	//events
	sealed class SettingsEvent
	{
		class LocationToggle(val context : Context, val checked : Boolean) : SettingsEvent()
		class LocationInput(val context : Context, val location : String) : SettingsEvent()
		class LoadLocation(val context: Context) : SettingsEvent()
		class BatteryExempt(val exempt : Boolean) : SettingsEvent()

		//prayer times adjustments
		//calculation method
		class CalculationMethod(val method : String) : SettingsEvent()
		class Madhab(val madhab : String) : SettingsEvent()
		class HighLatitude(val rule : String) : SettingsEvent()
		class FajrAngle(val angle : String) : SettingsEvent()
		class IshaAngle(val angle : String) : SettingsEvent()
		class IshaAngleVisibility(val visible : Boolean) : SettingsEvent()
		class IshaInterval(val interval : String) : SettingsEvent()
		//offset
		class FajrOffset(val offset : String) : SettingsEvent()
		class SunriseOffset(val offset : String) : SettingsEvent()
		class DhuhrOffset(val offset : String) : SettingsEvent()
		class AsrOffset(val offset : String) : SettingsEvent()
		class MaghribOffset(val offset : String) : SettingsEvent()
		class IshaOffset(val offset : String) : SettingsEvent()

		object LoadSettings : SettingsEvent()
	}
	//events for the settings screen
	fun handleEvent(event : SettingsEvent)
	{
		when (event)
		{
			is SettingsEvent.LocationToggle ->
			{
				_isLocationAuto.value = event.checked
				sharedPreferences.saveDataBoolean(LOCATION_TYPE, event.checked)
				loadLocation(event.context, event.checked)
			}
			is SettingsEvent.LocationInput ->
			{
				_locationName.value = event.location
				sharedPreferences.saveData(AppConstants.LOCATION_INPUT, event.location)
				loadLocation(event.context , sharedPreferences.getDataBoolean(LOCATION_TYPE, true))
			}
			is SettingsEvent.LoadLocation ->
			{
				loadLocation(event.context , sharedPreferences.getDataBoolean(LOCATION_TYPE, true))
			}
			is SettingsEvent.BatteryExempt ->
			{
				_isBatteryExempt.value = event.exempt
				sharedPreferences.saveDataBoolean(AppConstants.BATTERY_OPTIMIZATION, event.exempt)
			}
			is SettingsEvent.CalculationMethod ->
			{
				_calculationMethod.value = event.method
				sharedPreferences.saveData(AppConstants.CALCULATION_METHOD, event.method)
			}
			is SettingsEvent.Madhab ->
			{
				_madhab.value = event.madhab
				sharedPreferences.saveData(AppConstants.MADHAB, event.madhab)
			}
			is SettingsEvent.HighLatitude ->
			{
				_highLatitude.value = event.rule
				sharedPreferences.saveData(AppConstants.HIGH_LATITUDE_RULE, event.rule)
			}
			is SettingsEvent.FajrAngle ->
			{
				_fajrAngle.value = event.angle
				sharedPreferences.saveData(AppConstants.FAJR_ANGLE, event.angle)
			}
			is SettingsEvent.IshaAngle ->
			{
				_ishaAngle.value = event.angle
				sharedPreferences.saveData(AppConstants.ISHA_ANGLE, event.angle)
			}
			is SettingsEvent.IshaAngleVisibility ->
			{
				_ishaAngleVisibility.value = event.visible
				if (!event.visible)
				{
					_ishaAngle.value = "0"
					sharedPreferences.saveData(AppConstants.ISHA_ANGLE, "0")
				}
			}
			is SettingsEvent.IshaInterval ->
			{
				_ishaInterval.value = event.interval
				sharedPreferences.saveData(AppConstants.ISHA_INTERVAL, event.interval)
			}
			//offset
			is SettingsEvent.FajrOffset ->
			{
				_fajrOffset.value = event.offset
				sharedPreferences.saveData(AppConstants.FAJR_ADJUSTMENT, event.offset)
			}
			is SettingsEvent.SunriseOffset ->
			{
				_sunriseOffset.value = event.offset
				sharedPreferences.saveData(AppConstants.SUNRISE_ADJUSTMENT, event.offset)
			}
			is SettingsEvent.DhuhrOffset ->
			{
				_dhuhrOffset.value = event.offset
				sharedPreferences.saveData(AppConstants.DHUHR_ADJUSTMENT, event.offset)
			}
			is SettingsEvent.AsrOffset ->
			{
				_asrOffset.value = event.offset
				sharedPreferences.saveData(AppConstants.ASR_ADJUSTMENT, event.offset)
			}
			is SettingsEvent.MaghribOffset ->
			{
				_maghribOffset.value = event.offset
				sharedPreferences.saveData(AppConstants.MAGHRIB_ADJUSTMENT, event.offset)
			}
			is SettingsEvent.IshaOffset ->
			{
				_ishaOffset.value = event.offset
				sharedPreferences.saveData(AppConstants.ISHA_ADJUSTMENT, event.offset)
			}
			is SettingsEvent.LoadSettings ->
			{
				_isLocationAuto.value = sharedPreferences.getDataBoolean(LOCATION_TYPE, false)
				_locationName.value = sharedPreferences.getData(AppConstants.LOCATION_INPUT, "")
				_isBatteryExempt.value = sharedPreferences.getDataBoolean(AppConstants.BATTERY_OPTIMIZATION, false)
				_calculationMethod.value = sharedPreferences.getData(AppConstants.CALCULATION_METHOD, "ISNA")
				_madhab.value = sharedPreferences.getData(AppConstants.MADHAB, "Shafi")
				_highLatitude.value = sharedPreferences.getData(AppConstants.HIGH_LATITUDE_RULE, "AngleBased")
				_fajrAngle.value = sharedPreferences.getData(AppConstants.FAJR_ANGLE, "18")
				_ishaAngle.value = sharedPreferences.getData(AppConstants.ISHA_ANGLE, "17")
				_ishaAngleVisibility.value = _calculationMethod.value != "MAKKAH" && _calculationMethod.value != "QATAR" && _calculationMethod.value != "GULF"
				_ishaInterval.value = sharedPreferences.getData(AppConstants.ISHA_INTERVAL, "0")
				_fajrOffset.value = sharedPreferences.getData(AppConstants.FAJR_ADJUSTMENT, "0")
				_sunriseOffset.value = sharedPreferences.getData(AppConstants.SUNRISE_ADJUSTMENT, "0")
				_dhuhrOffset.value = sharedPreferences.getData(AppConstants.DHUHR_ADJUSTMENT, "0")
				_asrOffset.value = sharedPreferences.getData(AppConstants.ASR_ADJUSTMENT, "0")
				_maghribOffset.value = sharedPreferences.getData(AppConstants.MAGHRIB_ADJUSTMENT, "0")
				_ishaOffset.value = sharedPreferences.getData(AppConstants.ISHA_ADJUSTMENT, "0")
			}
		}
	}



	private fun loadLocationManual(context : Context , locationName : String)
	{
		viewModelScope.launch(Dispatchers.IO) {
			try
			{
				_isLocationNameLoading.value = true
				//callback for location
				val locationFoundCallbackManual =
					{ latitudeValue : Double, longitudeValue : Double , name : String ->
						//save location
						_locationName.value = name
						_latitude.value = latitudeValue
						_longitude.value = longitudeValue
						_isLocationNameLoading.value = false
					}

				if (NetworkChecker().networkCheck(context))
				{
						Location().getManualLocation(
								locationName ,
								context ,
								locationFoundCallbackManual,
													)
						Log.d(
								AppConstants.PRAYER_TIMES_SCREEN_TAG + "Viewmodel" ,
								"loadLocation: manual"
							 )
				} else
				{
					Log.d(
							AppConstants.PRAYER_TIMES_SCREEN_TAG + "Viewmodel" ,
							"loadLocation: no network"
						 )
				}
			} catch (e : Exception)
			{
				Log.d(
						AppConstants.PRAYER_TIMES_SCREEN_TAG + "Viewmodel" ,
						"loadLocation: ${e.message}"
					 )
			}
		}
	}

	private fun loadLocationAuto(context : Context)
	{
		viewModelScope.launch(Dispatchers.Main) {
			try
			{
				_isLocationNameLoading.value = true
				val listener = { latitudeValue : Double , longitudeValue : Double ->
					//save location
					_latitude.value = latitudeValue
					_longitude.value = longitudeValue

					val locationFoundCallbackManual =
						{ latitude : Double, longitude : Double , name : String ->
							//save location
							_locationName.value = name
							_latitude.value = latitude
							_longitude.value = longitude
							_isLocationNameLoading.value = false
						}


					//get the location name
					val locationFinder = LocationFinder()
					locationFinder.findCityName(
							context ,
							latitude = latitudeValue ,
							longitude = longitudeValue ,
							locationFoundCallbackManual = locationFoundCallbackManual
											   )
					_isLocationNameLoading.value = false
				}

				if (NetworkChecker().networkCheck(context))
				{
					Location().getAutomaticLocation(
							context ,
							listener
												   )
					Log.d(
							AppConstants.PRAYER_TIMES_SCREEN_TAG + "Viewmodel" ,
							"loadLocation: manual"
						 )
				} else
				{
					Log.d(
							AppConstants.PRAYER_TIMES_SCREEN_TAG + "Viewmodel" ,
							"loadLocation: no network"
						 )
				}
			} catch (e : Exception)
			{
				Log.d(
						AppConstants.PRAYER_TIMES_SCREEN_TAG + "Viewmodel" ,
						"loadLocation: ${e.message}"
					 )
			}
		}
	}

	//load location from shared preferences
	private fun loadLocation(context : Context , checked : Boolean)
	{
		viewModelScope.launch(Dispatchers.Main) {
			try
			{
				if (checked)
				{
					loadLocationAuto(context)
				} else
				{
					LocationFinderAuto().stopLocationUpdates()
					loadLocationManual(context, sharedPreferences.getData(AppConstants.LOCATION_INPUT, ""))
				}
			} catch (e : Exception)
			{
				Log.d(
						AppConstants.PRAYER_TIMES_SCREEN_TAG + "Viewmodel" ,
						"loadLocation: ${e.message}"
					 )
			}
		}
	}
}