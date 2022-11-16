package com.arshadshah.nimaz.ui.components.bLogic.quran

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.ui.components.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.ui.components.utils.QuranApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AyaJuzViewModel : ViewModel() {
    sealed class AyaJuzState {
        object Loading : AyaJuzState()
        data class Success(val data: String) : AyaJuzState()
        data class Error(val errorMessage: String) : AyaJuzState()
    }

    private var _ayaJuzstate = MutableStateFlow(AyaJuzState.Loading as AyaJuzState)
    val ayaJuzstate = _ayaJuzstate.asStateFlow()

    fun getAllAyaForJuz(context: Context, juzNumber: Int, isEnglish: Boolean) {
        viewModelScope.launch {
            try {
                _ayaJuzstate.value = AyaJuzState.Loading
                val api = QuranApi(context)
                api.getAyatForJuz(context, juzNumber, isEnglish)
                val sharedPreferences = PrivateSharedPreferences(context)
                val ayaList = sharedPreferences.getData("juzAyatList", "")
                if (ayaList != "") {
                    _ayaJuzstate.value = AyaJuzState.Success(ayaList)
                }
            } catch (e: Exception) {
                _ayaJuzstate.value = AyaJuzState.Error(e.message ?: "Unknown error")
            }
        }
    }
}