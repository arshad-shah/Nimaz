package com.arshadshah.nimaz.data.remote.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LocationViewModel(context: Context) : ViewModel() {
    sealed class LocationState {
        object Loading : LocationState()
        data class Success(val location: String) : LocationState()
        data class Error(val errorMessage: String) : LocationState()
    }

    private var _location = MutableStateFlow(LocationState.Loading as LocationState)
    val location = _location.asStateFlow()

    init {
        viewModelScope.launch {
            val sharedPreferences = PrivateSharedPreferences(context)
            val location = sharedPreferences.getData("location_input", "Abbeyleix")
            if (location == "") {
                _location.value = LocationState.Error("No location found")
            } else {
                _location.value = LocationState.Success(location)
            }
        }
    }

}
