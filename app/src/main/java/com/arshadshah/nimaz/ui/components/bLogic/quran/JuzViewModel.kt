package com.arshadshah.nimaz.ui.components.bLogic.quran

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.ui.components.utils.repositories.QuranRepository
import com.arshadshah.nimaz.ui.models.Juz
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class JuzViewModel() : ViewModel() {
    sealed class JuzState {
        object Loading : JuzState()
        data class Success(val data: ArrayList<Juz>) : JuzState()
        data class Error(val errorMessage: String) : JuzState()
    }

    private var _juzState = MutableStateFlow(JuzState.Loading as JuzState)
    val juzState = _juzState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = QuranRepository.getJuzs()
                if (response.data != null) {
                    _juzState.value = JuzState.Success(response.data)
                } else {
                    _juzState.value = JuzState.Error(response.message!!)
                }
            } catch (e: Exception) {
                _juzState.value = JuzState.Error(e.message ?: "Unknown error")
            }
        }
    }
}