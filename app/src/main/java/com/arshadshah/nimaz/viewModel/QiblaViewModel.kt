package com.arshadshah.nimaz.viewModel

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

	private var _qiblaState = MutableStateFlow(0.0)
	val qiblaState = _qiblaState.asStateFlow()

	private var _isLoading = MutableStateFlow(false)
	val isLoading = _isLoading.asStateFlow()

	private var _errorMessage = MutableStateFlow("")
	val errorMessage = _errorMessage.asStateFlow()

	fun loadQibla(context : Context)
	{
		viewModelScope.launch(Dispatchers.IO) {
			_isLoading.value = true
			try
			{
				val sharedPreferences = PrivateSharedPreferences(context)
				val latitude = sharedPreferences.getDataDouble(AppConstants.LATITUDE , 53.3498)
				val longitude = sharedPreferences.getDataDouble(AppConstants.LONGITUDE , - 6.2603)
				val qiblaBearing = Qibla().calculateQiblaDirection(latitude , longitude)
				_qiblaState.value = qiblaBearing
			} catch (e : Exception)
			{
				_errorMessage.value = e.message.toString()
			} finally
			{
				_isLoading.value = false
			}
		}
	}
}