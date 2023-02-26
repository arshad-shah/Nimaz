package com.arshadshah.nimaz.data.remote.viewModel

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.LOCATION_TYPE
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.google.android.gms.location.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

class SettingsViewModel(context: Context) : ViewModel()
{
	val sharedPreferences = PrivateSharedPreferences(context)

	private val geocoder = Geocoder(context , Locale.getDefault())

	private val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

	//theme state
	//it has four states: light, dark and system , and dynamic if we are in Build.VERSION.SDK_INT >= Build.VERSION_CODES.S else it has three states: light, dark and system
	private var _theme = MutableStateFlow(sharedPreferences.getData(AppConstants.THEME, "SYSTEM"))
	val theme = _theme.asStateFlow()


	//state for switch of location toggle between manual and automatic
	private var _isLocationAuto = MutableStateFlow(sharedPreferences.getDataBoolean(LOCATION_TYPE, false))
	val isLocationAuto = _isLocationAuto.asStateFlow()

	//location name loading state
	private var _isLoading = MutableStateFlow(false)
	val isLoading = _isLoading.asStateFlow()

	//location name error state
	private var _isError = MutableStateFlow("")
	val isError = _isError.asStateFlow()

	//location name state
	private var _locationName = MutableStateFlow(sharedPreferences.getData(AppConstants.LOCATION_INPUT, ""))
	val locationName = _locationName.asStateFlow()

	//latitude state
	private var _latitude = MutableStateFlow(sharedPreferences.getDataDouble(AppConstants.LATITUDE, 0.0))
	val latitude = _latitude.asStateFlow()

	//longitude state
	private var _longitude = MutableStateFlow(sharedPreferences.getDataDouble(AppConstants.LONGITUDE, 0.0))
	val longitude = _longitude.asStateFlow()

	//battery exempt state
	private var _isBatteryExempt = MutableStateFlow(sharedPreferences.getDataBoolean(AppConstants.BATTERY_OPTIMIZATION, false))
	val isBatteryExempt = _isBatteryExempt.asStateFlow()

	//prayer times adjustments state
	//calculation method state
	private var _calculationMethod = MutableStateFlow(sharedPreferences.getData(AppConstants.CALCULATION_METHOD, "MWL"))
	val calculationMethod = _calculationMethod.asStateFlow()

	//Madhab state
	private var _madhab = MutableStateFlow(sharedPreferences.getData(AppConstants.MADHAB, "SHAFI"))
	val madhab = _madhab.asStateFlow()

	//high latitude state
	private var _highLatitude = MutableStateFlow(sharedPreferences.getData(AppConstants.HIGH_LATITUDE_RULE, "MIDDLE_OF_THE_NIGHT"))
	val highLatitude = _highLatitude.asStateFlow()

	//fajr angle state
	private var _fajrAngle = MutableStateFlow(sharedPreferences.getData(AppConstants.FAJR_ANGLE, "18"))
	val fajrAngle = _fajrAngle.asStateFlow()

	//isha angle state
	private var _ishaAngle = MutableStateFlow(sharedPreferences.getData(AppConstants.ISHA_ANGLE, "17"))
	val ishaAngle = _ishaAngle.asStateFlow()

	//ishaAngle visibility state
	private var _ishaAngleVisibility = MutableStateFlow(true)
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
		//events to update latitude and longitude
		class Latitude(val context : Context,val latitude : Double) : SettingsEvent()
		class Longitude(val context : Context,val longitude : Double) : SettingsEvent()
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
		//theme
		class Theme(val theme : String) : SettingsEvent()
		//update settings based on calculation method
		class UpdateSettings(val method : String) : SettingsEvent()
	}
	//events for the settings screen
	fun handleEvent(event : SettingsEvent)
	{
		when (event)
		{
			is SettingsEvent.LocationToggle ->
			{
				_isLocationAuto.value = event.checked
				sharedPreferences.saveDataBoolean(LOCATION_TYPE , event.checked)
				loadLocation(event.context , event.checked)
			}

			is SettingsEvent.LocationInput ->
			{
				_locationName.value = event.location
				sharedPreferences.saveData(AppConstants.LOCATION_INPUT , event.location)
				loadLocation(event.context , sharedPreferences.getDataBoolean(LOCATION_TYPE , true))
			}
			is SettingsEvent.Latitude ->
			{
				_latitude.value = event.latitude
				sharedPreferences.saveData(AppConstants.LATITUDE , event.latitude.toString())
				loadLocation(event.context , sharedPreferences.getDataBoolean(LOCATION_TYPE , true))
			}

			is SettingsEvent.Longitude ->
			{
				_longitude.value = event.longitude
				sharedPreferences.saveData(AppConstants.LONGITUDE , event.longitude.toString())
				loadLocation(event.context , sharedPreferences.getDataBoolean(LOCATION_TYPE , true))
			}

			is SettingsEvent.LoadLocation ->
			{
				loadLocation(event.context , sharedPreferences.getDataBoolean(LOCATION_TYPE , true))
			}

			is SettingsEvent.BatteryExempt ->
			{
				_isBatteryExempt.value = event.exempt
				sharedPreferences.saveDataBoolean(AppConstants.BATTERY_OPTIMIZATION , event.exempt)
			}

			is SettingsEvent.CalculationMethod ->
			{
				_calculationMethod.value = event.method
				sharedPreferences.saveData(AppConstants.CALCULATION_METHOD , event.method)
			}

			is SettingsEvent.Madhab ->
			{
				_madhab.value = event.madhab
				sharedPreferences.saveData(AppConstants.MADHAB , event.madhab)
			}

			is SettingsEvent.HighLatitude ->
			{
				_highLatitude.value = event.rule
				sharedPreferences.saveData(AppConstants.HIGH_LATITUDE_RULE , event.rule)
			}

			is SettingsEvent.FajrAngle ->
			{
				_fajrAngle.value = event.angle
				sharedPreferences.saveData(AppConstants.FAJR_ANGLE , event.angle)
			}

			is SettingsEvent.IshaAngle ->
			{
				_ishaAngle.value = event.angle
				sharedPreferences.saveData(AppConstants.ISHA_ANGLE , event.angle)
			}

			is SettingsEvent.IshaAngleVisibility ->
			{
				_ishaAngleVisibility.value = event.visible
				if (! event.visible)
				{
					_ishaAngle.value = "0"
					sharedPreferences.saveData(AppConstants.ISHA_ANGLE , "0")
				}
			}

			is SettingsEvent.IshaInterval ->
			{
				_ishaInterval.value = event.interval
				sharedPreferences.saveData(AppConstants.ISHA_INTERVAL , event.interval)
			}
			//offset
			is SettingsEvent.FajrOffset ->
			{
				_fajrOffset.value = event.offset
				sharedPreferences.saveData(AppConstants.FAJR_ADJUSTMENT , event.offset)
			}

			is SettingsEvent.SunriseOffset ->
			{
				_sunriseOffset.value = event.offset
				sharedPreferences.saveData(AppConstants.SUNRISE_ADJUSTMENT , event.offset)
			}

			is SettingsEvent.DhuhrOffset ->
			{
				_dhuhrOffset.value = event.offset
				sharedPreferences.saveData(AppConstants.DHUHR_ADJUSTMENT , event.offset)
			}

			is SettingsEvent.AsrOffset ->
			{
				_asrOffset.value = event.offset
				sharedPreferences.saveData(AppConstants.ASR_ADJUSTMENT , event.offset)
			}

			is SettingsEvent.MaghribOffset ->
			{
				_maghribOffset.value = event.offset
				sharedPreferences.saveData(AppConstants.MAGHRIB_ADJUSTMENT , event.offset)
			}

			is SettingsEvent.IshaOffset ->
			{
				_ishaOffset.value = event.offset
				sharedPreferences.saveData(AppConstants.ISHA_ADJUSTMENT , event.offset)
			}

			is SettingsEvent.LoadSettings ->
			{
				_isLoading.value = true
				_isLocationAuto.value = sharedPreferences.getDataBoolean(LOCATION_TYPE , false)
				_locationName.value = sharedPreferences.getData(AppConstants.LOCATION_INPUT , "")
				_latitude.value = sharedPreferences.getDataDouble(AppConstants.LATITUDE , 0.0)
				_longitude.value = sharedPreferences.getDataDouble(AppConstants.LONGITUDE , 0.0)
				_isBatteryExempt.value =
					sharedPreferences.getDataBoolean(AppConstants.BATTERY_OPTIMIZATION , false)
				_calculationMethod.value =
					sharedPreferences.getData(AppConstants.CALCULATION_METHOD , "MWL")
				_madhab.value = sharedPreferences.getData(AppConstants.MADHAB , "SHAFI")
				_highLatitude.value =
					sharedPreferences.getData(AppConstants.HIGH_LATITUDE_RULE , "MIDDLE_OF_THE_NIGHT")
				_fajrAngle.value = sharedPreferences.getData(AppConstants.FAJR_ANGLE , "18")
				_ishaAngle.value = sharedPreferences.getData(AppConstants.ISHA_ANGLE , "17")
				val isNotAnIntervalMethod = when (_calculationMethod.value)
				{
					"MAKKAH" , "QATAR" , "GULF" -> false
					else -> true
				}
				_ishaAngleVisibility.value = isNotAnIntervalMethod
				_ishaInterval.value = sharedPreferences.getData(AppConstants.ISHA_INTERVAL , "0")
				_fajrOffset.value = sharedPreferences.getData(AppConstants.FAJR_ADJUSTMENT , "0")
				_sunriseOffset.value =
					sharedPreferences.getData(AppConstants.SUNRISE_ADJUSTMENT , "0")
				_dhuhrOffset.value = sharedPreferences.getData(AppConstants.DHUHR_ADJUSTMENT , "0")
				_asrOffset.value = sharedPreferences.getData(AppConstants.ASR_ADJUSTMENT , "0")
				_maghribOffset.value =
					sharedPreferences.getData(AppConstants.MAGHRIB_ADJUSTMENT , "0")
				_ishaOffset.value = sharedPreferences.getData(AppConstants.ISHA_ADJUSTMENT , "0")
				_isLoading.value = false
			}
			is SettingsEvent.Theme ->
			{
				_theme.value = event.theme
				sharedPreferences.saveData(AppConstants.THEME, event.theme)
			}

			is SettingsEvent.UpdateSettings ->
			{
				val defaultsForMethod = AppConstants.getDefaultParametersForMethod(event.method)
				_fajrAngle.value = defaultsForMethod["fajrAngle"] !!
				_ishaAngle.value = defaultsForMethod["ishaAngle"] !!
				val shouldBeVisible = defaultsForMethod["ishaInterval"] == "0"
				_ishaAngleVisibility.value = shouldBeVisible
				_ishaInterval.value = defaultsForMethod["ishaInterval"] !!
				_madhab.value = defaultsForMethod["madhab"] !!
				_highLatitude.value = defaultsForMethod["highLatitudeRule"] !!
				_fajrOffset.value = defaultsForMethod["fajrAdjustment"] !!
				_sunriseOffset.value = defaultsForMethod["sunriseAdjustment"] !!
				_dhuhrOffset.value = defaultsForMethod["dhuhrAdjustment"] !!
				_asrOffset.value = defaultsForMethod["asrAdjustment"] !!
				_maghribOffset.value = defaultsForMethod["maghribAdjustment"] !!
				_ishaOffset.value = defaultsForMethod["ishaAdjustment"] !!

				//save it to shared preferences
				sharedPreferences.saveData(AppConstants.CALCULATION_METHOD , event.method)
				sharedPreferences.saveData(AppConstants.MADHAB , defaultsForMethod["madhab"] !!)
				sharedPreferences.saveData(AppConstants.HIGH_LATITUDE_RULE , defaultsForMethod["highLatitudeRule"] !!)
				sharedPreferences.saveData(AppConstants.FAJR_ANGLE , defaultsForMethod["fajrAngle"] !!)
				sharedPreferences.saveData(AppConstants.ISHA_ANGLE , defaultsForMethod["ishaAngle"] !!)
				sharedPreferences.saveData(AppConstants.ISHA_INTERVAL , defaultsForMethod["ishaInterval"] !!)
				sharedPreferences.saveData(AppConstants.FAJR_ADJUSTMENT , defaultsForMethod["fajrAdjustment"] !!)
				sharedPreferences.saveData(AppConstants.SUNRISE_ADJUSTMENT , defaultsForMethod["sunriseAdjustment"] !!)
				sharedPreferences.saveData(AppConstants.DHUHR_ADJUSTMENT , defaultsForMethod["dhuhrAdjustment"] !!)
				sharedPreferences.saveData(AppConstants.ASR_ADJUSTMENT , defaultsForMethod["asrAdjustment"] !!)
				sharedPreferences.saveData(AppConstants.MAGHRIB_ADJUSTMENT , defaultsForMethod["maghribAdjustment"] !!)
				sharedPreferences.saveData(AppConstants.ISHA_ADJUSTMENT , defaultsForMethod["ishaAdjustment"] !!)

			}
		}
	}
	//load location from shared preferences
	private fun loadLocation(context : Context , checked : Boolean)
	{
		viewModelScope.launch(Dispatchers.IO) {
			try
			{
				if (checked)
				{
					_isLoading.value = true
					startLocationUpdates()
					_isLoading.value = false

				} else
				{
					_isLoading.value = true
					stopLocationUpdates()
					forwardGeocode(sharedPreferences.getData(AppConstants.LOCATION_INPUT , ""))
					_isLoading.value = false
				}
			} catch (e : Exception)
			{
				Log.d(
						AppConstants.PRAYER_TIMES_SCREEN_TAG + "Viewmodel" ,
						"loadLocation: ${e.message}"
					 )
				_isLoading.value = false
				_isError.value = e.message.toString()
			}
		}
	}

	fun reverseGeocode(latitude : Double , longitude : Double)
	{
		Log.d("Nimaz: reverseGeocode" , "reverseGeocode")
		try
		{
			val gcd = geocoder.getFromLocation(latitude , longitude , 1)
			val addresses : List<Address> = gcd as List<Address>
			if (addresses.isNotEmpty())
			{
				val address : Address = addresses[0]
				_locationName.value = address.locality
				sharedPreferences.saveData(AppConstants.LOCATION_INPUT , address.locality)
				_latitude.value = latitude
				_longitude.value = longitude
				sharedPreferences.saveDataDouble(AppConstants.LATITUDE , latitude)
				sharedPreferences.saveDataDouble(AppConstants.LONGITUDE , longitude)

				Log.i("Location" , "Location Found From value $latitude $longitude")
			} else
			{
				_latitude.value =
					sharedPreferences.getDataDouble(AppConstants.LATITUDE , 53.3498)
				_longitude.value =
					sharedPreferences.getDataDouble(AppConstants.LONGITUDE , - 6.2603)
				val cityNameFromStorage =
					sharedPreferences.getData(AppConstants.LOCATION_INPUT , "")
				Log.i("Location" , "Location Found From Storage $cityNameFromStorage")
			}
		} catch (e : Exception)
		{
			Log.e("Geocoder" , "Geocoder has failed")
			_latitude.value =
				sharedPreferences.getDataDouble(AppConstants.LATITUDE , 53.3498)
			_longitude.value =
				sharedPreferences.getDataDouble(AppConstants.LONGITUDE , - 6.2603)
			val cityNameFromStorage =
				sharedPreferences.getData(AppConstants.LOCATION_INPUT , "")
			Log.i("Location" , "Location Found From Storage $cityNameFromStorage")
		}
	}

	fun forwardGeocode(cityName : String)
	{
		Log.d("Nimaz: forwardGeocode" , "forwardGeocode")
		try
		{
			val addresses : List<Address> =
				geocoder.getFromLocationName(cityName , 1) as List<Address>
			if (addresses.isNotEmpty())
			{
				val address : Address = addresses[0]
				_locationName.value = address.locality
				sharedPreferences.saveData(AppConstants.LOCATION_INPUT , address.locality)
				_latitude.value = address.latitude
				_longitude.value = address.longitude
				sharedPreferences.saveDataDouble(AppConstants.LATITUDE , address.latitude)
				sharedPreferences.saveDataDouble(AppConstants.LONGITUDE , address.longitude)
				Log.i("Location" , "Location Found From value $cityName")
			} else
			{
				_latitude.value =
					sharedPreferences.getDataDouble(AppConstants.LATITUDE , 53.3498)
				_longitude.value =
					sharedPreferences.getDataDouble(AppConstants.LONGITUDE , - 6.2603)
				val cityNameFromStorage =
					sharedPreferences.getData(AppConstants.LOCATION_INPUT , "")
				Log.i("Location" , "Location Found From Storage $cityNameFromStorage")
			}
		} catch (e : Exception)
		{
			Log.e("Geocoder" , "Geocoder has failed")
			_latitude.value =
				sharedPreferences.getDataDouble(AppConstants.LATITUDE , 53.3498)
			_longitude.value =
				sharedPreferences.getDataDouble(AppConstants.LONGITUDE , - 6.2603)
			val cityNameFromStorage =
				sharedPreferences.getData(AppConstants.LOCATION_INPUT , "")
			Log.i("Location" , "Location Found From Storage $cityNameFromStorage")
		}
	}


	private val ONE_MINUTE = 1000 * 60
	val locationRequest : LocationRequest = LocationRequest.Builder(
			Priority.PRIORITY_BALANCED_POWER_ACCURACY ,
			ONE_MINUTE.toLong()
																   ).build()

	@SuppressLint("MissingPermission")
	private fun startLocationUpdates()
	{
		fusedLocationProviderClient.requestLocationUpdates(
				locationRequest ,
				locationCallback ,
				Looper.getMainLooper()
														  )
	}

	private fun stopLocationUpdates()
	{
		fusedLocationProviderClient.removeLocationUpdates(locationCallback)
	}

	private val locationCallback  = object : LocationCallback()
	{
		override fun onLocationResult(p0 : LocationResult)
		{
			super.onLocationResult(p0)
			locationRequest ?: return
			for (location in p0.locations)
			{
				setLocationData(location)
				Log.d("Nimaz: Location" , "Latitude: ${location.latitude} Longitude: ${location.longitude}")
			}
		}
	}

	private fun setLocationData(location : Location?)
	{
		location?.let {
			_latitude.value = location.latitude
			_longitude.value = location.longitude
			reverseGeocode(
						 location.latitude ,
						 location.longitude
						  )
		}
	}


}