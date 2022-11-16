package com.arshadshah.nimaz.ui.components.bLogic.prayerTimes.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.ui.components.utils.PrayerTimesAPI
import com.arshadshah.nimaz.ui.components.utils.PrivateSharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PrayerTimesViewModel(context: Context) : ViewModel() {
    sealed class PrayerTimesListState {
        object Loading : PrayerTimesListState()
        data class Success(val prayerTimes: String) : PrayerTimesListState()
        data class Error(val errorMessage: String) : PrayerTimesListState()
    }

    private var _prayerTimesListState =
        MutableStateFlow(PrayerTimesListState.Loading as PrayerTimesListState)
    val prayerTimesListState = _prayerTimesListState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                _prayerTimesListState.value = PrayerTimesListState.Loading
                val prayerTimeApi = PrayerTimesAPI(context)
                prayerTimeApi.requestPrayerTimes(context)
                val sharedPreferences = PrivateSharedPreferences(context)
                val prayerTimes = sharedPreferences.getData("prayer_times", "")
                if (prayerTimes != "") {
                    _prayerTimesListState.value = PrayerTimesListState.Success(prayerTimes)
                }
            } catch (e: Exception) {
                _prayerTimesListState.value =
                    PrayerTimesListState.Error(e.message ?: "Unknown error")
            }
        }
    }

}
