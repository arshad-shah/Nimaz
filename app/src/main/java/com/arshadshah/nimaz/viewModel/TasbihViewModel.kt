package com.arshadshah.nimaz.viewModel

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.data.local.models.LocalTasbih
import com.arshadshah.nimaz.utils.LocalDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class TasbihViewModel(context: Context) : ViewModel() {

    //state for the tasbih
     private var _tasbihLoading = MutableStateFlow(false)
    val tasbihLoading = _tasbihLoading.asStateFlow()

    //tasbijh error
     private var _tasbihError = MutableStateFlow("")
    val tasbihError = _tasbihError.asStateFlow()

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
    val orientationButtonState = _orientationButtonState.asStateFlow()

     private var _counter = MutableStateFlow(0)
    val counter = _counter.asStateFlow()

     private var _objective = MutableStateFlow(33)
    val objective = _objective.asStateFlow()

    private var _lap = MutableStateFlow(0)
    val lap = _lap.asStateFlow()

    private var _lapCounter = MutableStateFlow(0)
    val lapCounter = _lapCounter.asStateFlow()

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

    //tasbih orientation button function
    fun toggleOrientation() {
        viewModelScope.launch(Dispatchers.IO) {
            _orientationButtonState.value = !_orientationButtonState.value
        }
    }

     fun recreateTasbih(date: LocalDate) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _tasbihLoading.value = true
                _tasbihError.value = ""
                val datastore = LocalDataStore.getDataStore()
                //get all the tasbih
                val tasbihList = datastore.getAllTasbih()
                //create a unique list of tasbih by date
                val tasbihListByDate = tasbihList.groupBy { it.date }
                //recreate the tasbih for today from the yesterday tasbih
                //we need to basically copy the tasbih from yesterday to today
                //alter the date, id and count where date is today, id is 0 and count is 0
                //then insert the tasbih into the database
                //then get the tasbih list for today
                //yersterday date
                val yesterday = date.minusDays(1)
                val yesterdayTasbihList = tasbihListByDate[yesterday]
                if (yesterdayTasbihList != null) {
                    for (tasbih in yesterdayTasbihList) {
                        //check if the tasbih for today already exists by checking both arabic name and goal
                        val todayTasbihList = tasbihListByDate[date]
                        if (todayTasbihList != null) {
                            val tasbihExists = todayTasbihList.find {
                                it.arabicName == tasbih.arabicName || it.goal == tasbih.goal && it.date == date
                            }
                            if (tasbihExists != null) {
                                //if the tasbih exists for today then we don't need to recreate it
                                continue
                            }
                        }
                        val newTasbih = LocalTasbih(
                            id = 0,
                            arabicName = tasbih.arabicName,
                            englishName = tasbih.englishName,
                            translationName = tasbih.translationName,
                            count = 0,
                            date = date,
                            goal = tasbih.goal
                        )
                        datastore.saveTasbih(newTasbih)
                    }
                } else {
                    _tasbihError.value = "No tasbih for yesterday"
                }
                //get the tasbih list for today
                getTasbihList(date)
                _tasbihLoading.value = false
            } catch (e: Exception) {
                _tasbihError.value = e.message.toString()
            }
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
                val datastore = LocalDataStore.getDataStore()
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
            val datastore = LocalDataStore.getDataStore()
            val tasbihList = datastore.getTasbihForDate(date)
            _tasbihList.value = tasbihList
        }
    }

    //get all the tasbih
     fun getAllTasbih() {
        viewModelScope.launch(Dispatchers.IO) {
            val datastore = LocalDataStore.getDataStore()
            val tasbihList = datastore.getAllTasbih()
            _tasbihList.value = tasbihList
        }
    }

     fun getTasbih(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _tasbihLoading.value = true
                _tasbihError.value = ""
                val datastore = LocalDataStore.getDataStore()
                val tasbih = datastore.getTasbihById(id)
                _tasbihCreated.value = tasbih
                _tasbihLoading.value = false
            } catch (e: Exception) {
                _tasbihError.value = e.message.toString()
            }
        }
    }


     fun createTasbih(tasbih: LocalTasbih) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _tasbihLoading.value = true
                _tasbihError.value = ""
                val datastore = LocalDataStore.getDataStore()
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
                val datastore = LocalDataStore.getDataStore()
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

     fun updateTasbihGoal(tasbih: LocalTasbih) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _tasbihLoading.value = true
                _tasbihError.value = ""
                val datastore = LocalDataStore.getDataStore()
                datastore.updateTasbihGoal(tasbih)
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