package com.arshadshah.nimaz.data.remote.viewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.data.remote.models.Tasbih
import com.arshadshah.nimaz.utils.LocalDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TasbihViewModel(context: Context): ViewModel()
{
	//state for the tasbih
	private var _tasbih = MutableStateFlow(
			Tasbih(
					id = 0 ,
		arabicName = "" ,
		englishName = "" ,
		translationName = "" ,
		goal = 0 ,
		completed = 0 ,
		isCompleted = false ,
				  )
										  )
	val tasbih = _tasbih.asStateFlow()

	sealed class TasbihEvent
	{
		data class SetTasbih(val tasbih : Tasbih) : TasbihEvent()
		//update the tasbih
		data class UpdateTasbih(val tasbih : Tasbih) : TasbihEvent()
		//get the tasbih by id
		data class GetTasbih(val id : Int) : TasbihEvent()

	}

	//event for the tasbih
	fun handleEvent(event : TasbihEvent)
	{
		when (event)
		{
			is TasbihEvent.SetTasbih ->
			{
				_tasbih.value = event.tasbih
				createTasbih(event.tasbih)
			}
			is TasbihEvent.UpdateTasbih ->
			{
				_tasbih.value = event.tasbih
				updateTasbih(event.tasbih)
			}
			is TasbihEvent.GetTasbih ->
			{
				getTasbih(event.id)
			}
		}
	}

	fun getTasbih(id: Int)
	{
		viewModelScope.launch(Dispatchers.IO) {
			val datastore = LocalDataStore.getDataStore()
			val tasbih = datastore.getTasbihById(id)
			_tasbih.value = tasbih
		}
	}


	fun createTasbih(tasbih: Tasbih)
	{
		viewModelScope.launch(Dispatchers.IO) {
			val datastore = LocalDataStore.getDataStore()
			datastore.saveTasbih(tasbih)
			//get the tasbih that was just created
			val tasbih = datastore.getTasbihById(tasbih.id)
			_tasbih.value = tasbih
		}
	}

	fun updateTasbih(tasbih: Tasbih)
	{
		viewModelScope.launch(Dispatchers.IO) {
			val datastore = LocalDataStore.getDataStore()
			datastore.updateTasbih(tasbih)
			//get the tasbih that was just updated
			val tasbih = datastore.getTasbihById(tasbih.id)
			_tasbih.value = tasbih
		}
	}
}