package com.arshadshah.nimaz.data.remote.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.data.remote.models.PrayerTimes
import com.arshadshah.nimaz.data.remote.repositories.PrayerTimesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PrayerTimesViewModel(context: Context) : ViewModel() {
    sealed class PrayerTimesListState {
        object Loading : PrayerTimesListState()
        data class Success(val prayerTimes: PrayerTimes?) : PrayerTimesListState()
        data class Error(val errorMessage: String) : PrayerTimesListState()
    }

    private var _prayerTimesListState =
        MutableStateFlow<PrayerTimesListState>(PrayerTimesListState.Loading)
    val prayerTimesListState = _prayerTimesListState.asStateFlow()


    init {
        loadPrayerTimes(context)
    }

    //load prayer times again
    fun loadPrayerTimes(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = PrayerTimesRepository.getPrayerTimes(context)
                if (response.data != null) {
                    _prayerTimesListState.value = PrayerTimesListState.Success(response.data)
                } else {
                    _prayerTimesListState.value = PrayerTimesListState.Error(response.message!!)
                }
            } catch (e: Exception) {
                _prayerTimesListState.value =
                    PrayerTimesListState.Error(e.message ?: "Unknown error")
            }
        }
    }

}
