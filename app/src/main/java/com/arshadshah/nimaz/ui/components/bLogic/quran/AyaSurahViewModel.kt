package com.arshadshah.nimaz.ui.components.bLogic.quran

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.ui.components.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.ui.components.utils.QuranApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AyaSurahViewModel : ViewModel() {
    sealed class AyaSurahState {
        object Loading : AyaSurahState()
        data class Success(val data: String) : AyaSurahState()
        data class Error(val errorMessage: String) : AyaSurahState()
    }

    private var _ayaSurahstate = MutableStateFlow(AyaSurahState.Loading as AyaSurahState)
    val ayaSurahState = _ayaSurahstate.asStateFlow()

    fun getAllAyaForSurah(context: Context, surahNumber: Int, isEnglish: Boolean) {
        viewModelScope.launch {
            try {
                _ayaSurahstate.value = AyaSurahState.Loading
                val api = QuranApi(context)
                api.getAyatForSurah(context, surahNumber, isEnglish)
                val sharedPreferences = PrivateSharedPreferences(context)
                val ayaList = sharedPreferences.getData("surahAyatList", "")
                if (ayaList != "") {
                    _ayaSurahstate.value = AyaSurahState.Success(ayaList)
                }
            } catch (e: Exception) {
                _ayaSurahstate.value = AyaSurahState.Error(e.message ?: "Unknown error")
            }
        }
    }
}