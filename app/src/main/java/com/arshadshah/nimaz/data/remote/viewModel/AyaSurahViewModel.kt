package com.arshadshah.nimaz.data.remote.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.data.remote.models.Aya
import com.arshadshah.nimaz.data.remote.repositories.QuranRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AyaSurahViewModel : ViewModel() {
    sealed class AyaSurahState {
        object Loading : AyaSurahState()
        data class Success(val data: ArrayList<Aya>) : AyaSurahState()
        data class Error(val errorMessage: String) : AyaSurahState()
    }

    private var _ayaSurahstate = MutableStateFlow(AyaSurahState.Loading as AyaSurahState)
    val ayaSurahState = _ayaSurahstate.asStateFlow()

    fun getAllAyaForSurah(surahNumber: Int, isEnglish: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = QuranRepository.getAyaForSurah(surahNumber, isEnglish)
                if (response.data != null) {
                    _ayaSurahstate.value = AyaSurahState.Success(response.data)
                } else {
                    _ayaSurahstate.value = AyaSurahState.Error(response.message!!)
                }
            } catch (e: Exception) {
                _ayaSurahstate.value = AyaSurahState.Error(e.message ?: "Unknown error")
            }
        }
    }
}