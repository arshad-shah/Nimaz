package com.arshadshah.nimaz.viewModel

import android.content.Context
import android.os.Vibrator
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.data.remote.models.Tasbih
import com.arshadshah.nimaz.utils.LocalDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class TasbihViewModel(context : Context) : ViewModel()
{

	val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

	//state for the tasbih
	private var _tasbihLoading = MutableStateFlow(false)
	val tasbihLoading = _tasbihLoading.asStateFlow()

	//tasbijh error
	private var _tasbihError = MutableStateFlow("")
	val tasbihError = _tasbihError.asStateFlow()

	private var _tasbihCreated = MutableStateFlow(
			Tasbih(
					id = 0 ,
					arabicName = "" ,
					englishName = "" ,
					translationName = "" ,
					count = 0 ,
					date = "" ,
					goal = 0
				  )
												 )

	val tasbihCreated = _tasbihCreated.asStateFlow()

	//list of tasbih for today
	private var _tasbihList = MutableStateFlow(
			listOf<Tasbih>()
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

	sealed class TasbihEvent
	{

		data class SetTasbih(val tasbih : Tasbih) : TasbihEvent()

		//update the tasbih
		data class UpdateTasbih(val tasbih : Tasbih) : TasbihEvent()

		//get the tasbih by id
		data class GetTasbih(val id : Int) : TasbihEvent()

		//get the tasbih list for today
		data class GetTasbihList(val date : String) : TasbihEvent()

		//get all the tasbih
		object GetAllTasbih : TasbihEvent()

		//delete the tasbih
		data class DeleteTasbih(val tasbih : Tasbih) : TasbihEvent()

		//recreate the tasbih from last date for today
		data class RecreateTasbih(val date : String) : TasbihEvent()

		//update tasbih goal
		data class UpdateTasbihGoal(val tasbih : Tasbih) : TasbihEvent()

		//update the reset button state
		data class UpdateResetButtonState(val state : Boolean) : TasbihEvent()

		//update the vibration button state
		data class UpdateVibrationButtonState(val state : Boolean) : TasbihEvent()

		//update the orientation button state
		data class UpdateOrientationButtonState(val state : Boolean) : TasbihEvent()
	}

	//event for the tasbih
	fun handleEvent(event : TasbihEvent)
	{
		when (event)
		{
			is TasbihEvent.SetTasbih ->
			{
				createTasbih(event.tasbih)
			}

			is TasbihEvent.UpdateTasbih ->
			{
				updateTasbih(event.tasbih)
			}

			is TasbihEvent.GetTasbih ->
			{
				getTasbih(event.id)
			}

			is TasbihEvent.GetTasbihList ->
			{
				getTasbihList(event.date)
			}

			is TasbihEvent.GetAllTasbih ->
			{
				getAllTasbih()
			}

			is TasbihEvent.DeleteTasbih ->
			{
				deleteTasbih(event.tasbih)
			}

			is TasbihEvent.RecreateTasbih ->
			{
				recreateTasbih(event.date)
			}

			is TasbihEvent.UpdateTasbihGoal ->
			{
				updateTasbihGoal(event.tasbih)
			}

			is TasbihEvent.UpdateResetButtonState ->
			{
				_resetButtonState.value = event.state
			}

			is TasbihEvent.UpdateVibrationButtonState ->
			{
				_vibrationButtonState.value = event.state
				//vibrate if the vibration button is on
				if (! event.state)
				{
					vibrator.cancel()
				}
			}

			is TasbihEvent.UpdateOrientationButtonState ->
			{
				_orientationButtonState.value = event.state
			}
		}
	}

	private fun recreateTasbih(date : String)
	{
		viewModelScope.launch(Dispatchers.IO) {
			try
			{
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
				val yesterday = LocalDate.parse(date).minusDays(1).toString()
				val yesterdayTasbihList = tasbihListByDate[yesterday]
				if (yesterdayTasbihList != null)
				{
					for (tasbih in yesterdayTasbihList)
					{
						//check if the tasbih for today already exists by checking both arabic name and goal
						val todayTasbihList = tasbihListByDate[date]
						if (todayTasbihList != null)
						{
							val tasbihExists = todayTasbihList.find {
								it.arabicName == tasbih.arabicName || it.goal == tasbih.goal && it.date == date
							}
							if (tasbihExists != null)
							{
								//if the tasbih exists for today then we don't need to recreate it
								continue
							}
						}
						val newTasbih = Tasbih(
								id = 0 ,
								arabicName = tasbih.arabicName ,
								englishName = tasbih.englishName ,
								translationName = tasbih.translationName ,
								count = 0 ,
								date = date ,
								goal = tasbih.goal
											  )
						datastore.saveTasbih(newTasbih)
					}
				} else
				{
					_tasbihError.value = "No tasbih for yesterday"
				}
				//get the tasbih list for today
				getTasbihList(date)
				_tasbihLoading.value = false
			} catch (e : Exception)
			{
				_tasbihError.value = e.message.toString()
			}
		}
	}

	private fun deleteTasbih(tasbih : Tasbih)
	{
		viewModelScope.launch(Dispatchers.IO) {
			try
			{
				_tasbihLoading.value = true
				_tasbihError.value = ""
				_tasbihCreated.value = Tasbih(
						id = 0 ,
						arabicName = "" ,
						englishName = "" ,
						translationName = "" ,
						count = 0 ,
						date = "" ,
						goal = 0
											 )
				val datastore = LocalDataStore.getDataStore()
				datastore.deleteTasbih(tasbih)
				//refresh the tasbih list
				getTasbihList(tasbih.date)
				_tasbihLoading.value = false
			} catch (e : Exception)
			{
				_tasbihError.value = e.message.toString()
			}
		}
	}

	//get the tasbih list for today
	private fun getTasbihList(date : String)
	{
		viewModelScope.launch(Dispatchers.IO) {
			val datastore = LocalDataStore.getDataStore()
			val tasbihList = datastore.getTasbihForDate(date)
			_tasbihList.value = tasbihList
		}
	}

	//get all the tasbih
	private fun getAllTasbih()
	{
		viewModelScope.launch(Dispatchers.IO) {
			val datastore = LocalDataStore.getDataStore()
			val tasbihList = datastore.getAllTasbih()
			_tasbihList.value = tasbihList
		}
	}

	fun getTasbih(id : Int)
	{
		viewModelScope.launch(Dispatchers.IO) {
			try
			{
				_tasbihLoading.value = true
				_tasbihError.value = ""
				val datastore = LocalDataStore.getDataStore()
				val tasbih = datastore.getTasbihById(id)
				_tasbihCreated.value = tasbih
				_tasbihLoading.value = false
			} catch (e : Exception)
			{
				_tasbihError.value = e.message.toString()
			}
		}
	}


	private fun createTasbih(tasbih : Tasbih)
	{
		viewModelScope.launch(Dispatchers.IO) {
			try
			{
				_tasbihLoading.value = true
				_tasbihError.value = ""
				val datastore = LocalDataStore.getDataStore()
				val idOfTasbih = datastore.saveTasbih(tasbih)
				Log.d("Nimaz: TasbihViewModel" , "id of tasbih is ${idOfTasbih.toInt()}")
				//get the tasbih that was just created
				val tasbihJustCreated = datastore.getTasbihById(idOfTasbih.toInt())
				//update the tasbih state on the main thread
				_tasbihCreated.value = tasbihJustCreated
				_tasbihLoading.value = false
			} catch (e : Exception)
			{
				_tasbihError.value = e.message.toString()
			}
		}
	}

	private fun updateTasbih(tasbih : Tasbih)
	{
		viewModelScope.launch(Dispatchers.IO) {
			try
			{
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
			} catch (e : Exception)
			{
				_tasbihError.value = e.message.toString()
			}
		}
	}

	private fun updateTasbihGoal(tasbih : Tasbih)
	{
		viewModelScope.launch(Dispatchers.IO) {
			try
			{
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
			} catch (e : Exception)
			{
				_tasbihError.value = e.message.toString()
			}
		}
	}
}