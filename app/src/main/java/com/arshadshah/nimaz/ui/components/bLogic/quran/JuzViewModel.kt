package com.arshadshah.nimaz.ui.components.bLogic.quran

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.ui.components.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.ui.components.utils.QuranApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class JuzViewModel(context: Context) : ViewModel() {
    sealed class JuzState {
        object Loading : JuzState()
        data class Success(val data: String) : JuzState()
        data class Error(val errorMessage: String) : JuzState()
    }

    private var _juzState = MutableStateFlow(JuzState.Loading as JuzState)
    val juzState = _juzState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                _juzState.value = JuzState.Loading
                val api = QuranApi(context)
                api.getAllJuz(context)
                val sharedPreferences = PrivateSharedPreferences(context)
                val juzList = sharedPreferences.getData("juzList", "")
                if (juzList != "") {
                    _juzState.value = JuzState.Success(juzList)
                }

            } catch (e: Exception) {
                _juzState.value = JuzState.Error(e.message ?: "Unknown error")
            }
        }
    }
}