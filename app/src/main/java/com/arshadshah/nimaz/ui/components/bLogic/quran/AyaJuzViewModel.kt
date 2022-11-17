package com.arshadshah.nimaz.ui.components.bLogic.quran

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.ui.components.utils.repositories.QuranRepository
import com.arshadshah.nimaz.ui.models.Aya
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AyaJuzViewModel : ViewModel() {
    sealed class AyaJuzState {
        object Loading : AyaJuzState()
        data class Success(val data: ArrayList<Aya>) : AyaJuzState()
        data class Error(val errorMessage: String) : AyaJuzState()
    }

    private var _ayaJuzstate = MutableStateFlow(AyaJuzState.Loading as AyaJuzState)
    val ayaJuzstate = _ayaJuzstate.asStateFlow()

    fun getAllAyaForJuz(juzNumber: Int, isEnglish: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = QuranRepository.getAyaForJuz(juzNumber, isEnglish)
                if (response.data != null) {
                    _ayaJuzstate.value = AyaJuzState.Success(response.data)
                } else {
                    _ayaJuzstate.value = AyaJuzState.Error(response.message!!)
                }

            } catch (e: Exception) {
                _ayaJuzstate.value = AyaJuzState.Error(e.message ?: "Unknown error")
            }
        }
    }
}