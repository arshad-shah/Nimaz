package com.arshadshah.nimaz.data.remote.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.data.remote.models.Surah
import com.arshadshah.nimaz.data.remote.repositories.QuranRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SurahViewModel : ViewModel() {
    sealed class SurahState {
        object Loading : SurahState()
        data class Success(val data: ArrayList<Surah>?) : SurahState()
        data class Error(val errorMessage: String) : SurahState()
    }

    private var _surahState = MutableStateFlow(SurahState.Loading as SurahState)
    val surahState = _surahState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = QuranRepository.getSurahs()
                if (response.data != null) {
                    _surahState.value = SurahState.Success(response.data)
                } else {
                    _surahState.value = SurahState.Error(response.message!!)
                }
            } catch (e: Exception) {
                _surahState.value = SurahState.Error(e.message ?: "Unknown error")
            }
        }
    }
}