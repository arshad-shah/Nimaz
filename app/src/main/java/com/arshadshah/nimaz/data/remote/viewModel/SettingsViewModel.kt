package com.arshadshah.nimaz.data.remote.viewModel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.LOCATION_TYPE
import com.arshadshah.nimaz.utils.Location
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.location.NetworkChecker
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(context: Context) : ViewModel()
{
	val sharedPreferences = PrivateSharedPreferences(context)


	//state for switch of location toggle between manual and automatic
	private var _isLocationManual = MutableStateFlow(sharedPreferences.getDataBoolean(LOCATION_TYPE, false))
	val isLocationManual = _isLocationManual.asStateFlow()

	//location name loading state
	private var _isLocationNameLoading = MutableStateFlow(false)
	val isLocationNameLoading = _isLocationNameLoading.asStateFlow()

	//location name state
	private var _locationName = MutableStateFlow(sharedPreferences.getData(AppConstants.LOCATION_INPUT, "Dublin"))
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


	//events
	sealed class SettingsEvent
	{
		class LocationToggle(val checked : Boolean) : SettingsEvent()
		class LocationInput(val location : String) : SettingsEvent()
		object LoadLocation : SettingsEvent()
		class LocationManual(val context : Context , val locationName : String) : SettingsEvent()
		class LocationAutomatic(val context : Context) : SettingsEvent()
		class BatteryExempt(val exempt : Boolean) : SettingsEvent()
	}
	//events for the settings screen
	fun handleEvent(event : SettingsEvent)
	{
		when (event)
		{
			is SettingsEvent.LocationToggle ->
			{
				_isLocationManual.value = event.checked
				sharedPreferences.saveDataBoolean(LOCATION_TYPE, event.checked)
			}
			is SettingsEvent.LocationInput ->
			{
				_locationName.value = event.location
				sharedPreferences.saveData(AppConstants.LOCATION_INPUT, event.location)
			}
			is SettingsEvent.LoadLocation ->
			{
				loadLocation()
			}
			is SettingsEvent.LocationManual ->
			{
				loadLocationManual(event.context, event.locationName)
			}
			is SettingsEvent.LocationAutomatic ->
			{
				loadLocationAuto(event.context)
			}
			is SettingsEvent.BatteryExempt ->
			{
				_isBatteryExempt.value = event.exempt
				sharedPreferences.saveDataBoolean(AppConstants.BATTERY_OPTIMIZATION, event.exempt)
			}
		}
	}



	fun loadLocationManual(context : Context, locationName : String)
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
						Toasty.success(context, "Location found").show()
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

	fun loadLocationAuto(context : Context)
	{
		viewModelScope.launch(Dispatchers.IO) {
			try
			{
				_isLocationNameLoading.value = true
				val listener = { latitudeValue : Double , longitudeValue : Double ->
					//save location
					_latitude.value = latitudeValue
					_longitude.value = longitudeValue
				}

				val locationFoundCallbackManual =
					{ latitudeValue : Double, longitudeValue : Double , name : String ->
						//save location
						_locationName.value = name
						_latitude.value = latitudeValue
						_longitude.value = longitudeValue
						_isLocationNameLoading.value = false
						Toasty.success(context, "Location found").show()
					}


				if (NetworkChecker().networkCheck(context))
				{
					Location().getAutomaticLocation(
							context ,
							listener ,
							locationFoundCallbackManual
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
	fun loadLocation()
	{
		viewModelScope.launch(Dispatchers.IO) {
			try
			{
				//load location type
				_isLocationManual.value = sharedPreferences.getDataBoolean(LOCATION_TYPE, false)

				//load location name
				_locationName.value = sharedPreferences.getData(AppConstants.LOCATION_INPUT, "")

				//load latitude
				_latitude.value = sharedPreferences.getDataDouble(AppConstants.LATITUDE, 53.0)

				//load longitude
				_longitude.value = sharedPreferences.getDataDouble(AppConstants.LONGITUDE, -7.0)
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