package com.arshadshah.nimaz.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.data.local.DataStore
import com.arshadshah.nimaz.data.local.models.LocalTasbih
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class TasbihViewModel @Inject constructor(
    private val datastore: DataStore
) : ViewModel() {

    //state for the tasbih
    private var _tasbihLoading = MutableStateFlow(false)

    //tasbijh error
    private var _tasbihError = MutableStateFlow("")

    private var _tasbihCreated = MutableStateFlow(
        LocalTasbih(
            id = 0,
            arabicName = "",
            englishName = "",
            translationName = "",
            count = 0,
            date = LocalDate.now(),
            goal = 0
        )
    )

    val tasbihCreated = _tasbihCreated.asStateFlow()

    //list of tasbih for today
    private var _tasbihList = MutableStateFlow(
        listOf<LocalTasbih>()
    )
    val tasbihList = _tasbihList.asStateFlow()

    //state for the reset button in top app bar
    private var _resetButtonState = MutableStateFlow(false)
    val resetButtonState = _resetButtonState.asStateFlow()

    //state for the vibration button in top app bar
    private var _vibrationButtonState = MutableStateFlow(true)
    val vibrationButtonState = _vibrationButtonState.asStateFlow()

    //state for the orientation button in top app bar
    private var _orientationButtonState = MutableStateFlow(true)

    private var _counter = MutableStateFlow(0)
    val counter = _counter.asStateFlow()

    private var _objective = MutableStateFlow(33)
    val objective = _objective.asStateFlow()

    private var _lap = MutableStateFlow(0)
    val lap = _lap.asStateFlow()

    private var _lapCounter = MutableStateFlow(0)
    val lapCounter = _lapCounter.asStateFlow()

    init {
        getTasbihList(LocalDate.now())
    }

    fun setLapCounter(value: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _lapCounter.value = value
        }
    }

    fun setLap(value: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _lap.value = value
        }
    }

    fun setCounter(value: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _counter.value = value
        }
    }

    fun setObjective(value: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _objective.value = value
        }
    }

    fun incrementCounter() {
        viewModelScope.launch(Dispatchers.IO) {
            _counter.value += 1
            if (_counter.value == _objective.value) {
                _lapCounter.value += 1
                if (_lapCounter.value == _objective.value) {
                    _lap.value += 1
                    _lapCounter.value = 0
                }
            }
        }
    }

    fun decrementCounter() {
        viewModelScope.launch(Dispatchers.IO) {
            if (_counter.value > 0) {
                _counter.value -= 1
                //if count has reached the objective then decrement the lap
                if (_counter.value == _objective.value) {
                    _lap.value -= 1
                }

                if (_counter.value == 0) {
                    _lapCounter.value = 0
                    _lap.value = 0
                }
            }
        }
    }


    //tasbih vibration button function
    fun toggleVibration() {
        viewModelScope.launch(Dispatchers.IO) {
            _vibrationButtonState.value = !_vibrationButtonState.value
        }
    }

    //tasbih reset button function
    fun resetTasbih() {
        viewModelScope.launch(Dispatchers.IO) {
            _resetButtonState.value = !_resetButtonState.value
        }
    }

    fun deleteTasbih(tasbih: LocalTasbih) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _tasbihLoading.value = true
                _tasbihError.value = ""
                _tasbihCreated.value = LocalTasbih(
                    id = 0,
                    arabicName = "",
                    englishName = "",
                    translationName = "",
                    count = 0,
                    date = LocalDate.now(),
                    goal = 0
                )
                datastore.deleteTasbih(tasbih)
                //refresh the tasbih list
                getTasbihList(tasbih.date)
                _tasbihLoading.value = false
            } catch (e: Exception) {
                _tasbihError.value = e.message.toString()
            }
        }
    }

    //get the tasbih list for today
    fun getTasbihList(date: LocalDate) {
        viewModelScope.launch(Dispatchers.IO) {
            val tasbihList = datastore.getTasbihForDate(date)
            _tasbihList.value = tasbihList
        }
    }

    //get all the tasbih
    fun getAllTasbih() {
        viewModelScope.launch(Dispatchers.IO) {
            val tasbihList = datastore.getAllTasbih()
            _tasbihList.value = tasbihList
        }
    }


    fun createTasbih(tasbih: LocalTasbih) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _tasbihLoading.value = true
                _tasbihError.value = ""
                val idOfTasbih = datastore.saveTasbih(tasbih)
                Log.d("Nimaz: TasbihViewModel", "id of tasbih is ${idOfTasbih.toInt()}")
                //get the tasbih that was just created
                val tasbihJustCreated = datastore.getTasbihById(idOfTasbih.toInt())
                //update the tasbih state on the main thread
                _tasbihCreated.value = tasbihJustCreated
                _tasbihLoading.value = false
            } catch (e: Exception) {
                _tasbihError.value = e.message.toString()
            }
        }
    }

    fun updateTasbih(tasbih: LocalTasbih) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _tasbihLoading.value = true
                _tasbihError.value = ""
                datastore.updateTasbih(tasbih)
                //get the tasbih that was just created
                val tasbihJustUpdated = datastore.getTasbihById(tasbih.id)
                //refresh the tasbih list
                getTasbihList(tasbih.date)
                //update the tasbih state on the main thread
                _tasbihCreated.value = tasbihJustUpdated
                _tasbihLoading.value = false
            } catch (e: Exception) {
                _tasbihError.value = e.message.toString()
            }
        }
    }

}