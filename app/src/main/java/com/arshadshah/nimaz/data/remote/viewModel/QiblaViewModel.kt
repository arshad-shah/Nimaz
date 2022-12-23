package com.arshadshah.nimaz.data.remote.viewModel

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.data.remote.models.PrayerTimes
import com.arshadshah.nimaz.data.remote.repositories.PrayerTimesRepository
import com.arshadshah.nimaz.utils.network.NimazServicesImpl
import com.google.android.material.snackbar.Snackbar
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class QiblaViewModel(context: Context): ViewModel()
{
	sealed class QiblaState
	{

		object Loading : QiblaState()
		data class Success(val bearing : Double?) : QiblaState()
		data class Error(val errorMessage : String) : QiblaState()
	}

	private var _qiblaState = MutableStateFlow(QiblaState.Loading as QiblaState)
	val qiblaState = _qiblaState.asStateFlow()

	init
	{
		loadQibla(context)
	}

	fun loadQibla(context:Context)
	{
		viewModelScope.launch(Dispatchers.IO) {
			_qiblaState.value = QiblaState.Loading
			try
			{
				val response = PrayerTimesRepository.getQiblaDirection(context)
				if (response.data != null)
				{
					_qiblaState.value = QiblaState.Success(response.data)
				} else
				{
					_qiblaState.value = QiblaState.Error(response.message !!)
				}
			} catch (e : Exception)
			{
				_qiblaState.value = QiblaState.Error(e.message.toString())
			}
		}
	}
}