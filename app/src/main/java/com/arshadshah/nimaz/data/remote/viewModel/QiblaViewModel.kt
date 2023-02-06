package com.arshadshah.nimaz.data.remote.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.utils.Qibla
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class QiblaViewModel(context : Context) : ViewModel()
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

	fun loadQibla(context : Context)
	{
		viewModelScope.launch(Dispatchers.IO) {
			_qiblaState.value = QiblaState.Loading
			try
			{
				val sharedPreferences = PrivateSharedPreferences(context)
				val latitude = sharedPreferences.getDataDouble(AppConstants.LATITUDE , 53.3498)
				val longitude = sharedPreferences.getDataDouble(AppConstants.LONGITUDE , - 6.2603)
				val qiblaBearing = Qibla().calculateQiblaDirection(latitude , longitude)
				_qiblaState.value = QiblaState.Success(qiblaBearing)
			} catch (e : Exception)
			{
				_qiblaState.value = QiblaState.Error(e.message.toString())
			}
		}
	}
}