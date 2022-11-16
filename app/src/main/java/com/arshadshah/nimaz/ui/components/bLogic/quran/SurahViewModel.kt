package com.arshadshah.nimaz.ui.components.bLogic.quran

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.ui.components.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.ui.components.utils.QuranApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SurahViewModel(context: Context) : ViewModel() {
    sealed class SurahState {
        object Loading : SurahState()
        data class Success(val data: String) : SurahState()
        data class Error(val errorMessage: String) : SurahState()
    }

    private var _surahState = MutableStateFlow(SurahState.Loading as SurahState)
    val surahState = _surahState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                _surahState.value = SurahState.Loading
                val api = QuranApi(context)
                api.getAllSurahs(context)
                val sharedPreferences = PrivateSharedPreferences(context)
                val surahList = sharedPreferences.getData("surahList", "")
                if (surahList != "") {
                    _surahState.value = SurahState.Success(surahList)
                }

            } catch (e: Exception) {
                _surahState.value = SurahState.Error(e.message ?: "Unknown error")
            }
        }
    }
}