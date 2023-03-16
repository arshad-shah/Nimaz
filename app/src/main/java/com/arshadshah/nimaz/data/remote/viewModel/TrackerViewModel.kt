package com.arshadshah.nimaz.data.remote.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.data.remote.models.FastTracker
import com.arshadshah.nimaz.data.remote.models.PrayerTracker
import com.arshadshah.nimaz.utils.LocalDataStore
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class TrackerViewModel : ViewModel()
{

	sealed class TrackerState
	{

		object Loading : TrackerState()
		data class Tracker(val tracker : PrayerTracker) : TrackerState()
		data class Error(val message : String) : TrackerState()
	}

	private var _trackerState = MutableStateFlow(TrackerState.Loading as TrackerState)
	val trackerState = _trackerState.asStateFlow()

	sealed class FastTrackerState
	{

		object Loading : FastTrackerState()
		data class Tracker(val tracker : FastTracker) : FastTrackerState()
		data class Error(val message : String) : FastTrackerState()
	}

	//chart data List<FloatEntry>
	internal val multiDataSetChartEntryModelProducer: ChartEntryModelProducer = ChartEntryModelProducer()

	private var _fastTrackerState = MutableStateFlow(FastTrackerState.Loading as FastTrackerState)
	val fastTrackerState = _fastTrackerState.asStateFlow()

	//state of date
	private var _dateState = MutableStateFlow(LocalDate.now().toString())
	val dateState = _dateState.asStateFlow()

	//state of the date selector component
	private var _showDateSelector = MutableStateFlow(true)
	val showDateSelector = _showDateSelector.asStateFlow()

	//fajr
	private var _fajrState = MutableStateFlow(false)
	val fajrState = _fajrState.asStateFlow()

	//zuhr
	private var _zuhrState = MutableStateFlow(false)
	val zuhrState = _zuhrState.asStateFlow()

	//asr
	private var _asrState = MutableStateFlow(false)
	val asrState = _asrState.asStateFlow()

	//maghrib
	private var _maghribState = MutableStateFlow(false)
	val maghribState = _maghribState.asStateFlow()

	//isha
	private var _ishaState = MutableStateFlow(false)
	val ishaState = _ishaState.asStateFlow()


	//state to show progress of completed prayers
	private var _progressState = MutableStateFlow(0)
	val progressState = _progressState.asStateFlow()

	//a progressfor a specific date
	private var _progressForDate = MutableStateFlow(Pair(LocalDate.now().toString() , 0))
	val progressForDate = _progressForDate.asStateFlow()

	//dates with trackers
	private var _allTrackers = MutableStateFlow(listOf<PrayerTracker>())
	val allTrackers = _allTrackers.asStateFlow()

	private var _isFasting = MutableStateFlow(false)
	val isFasting = _isFasting.asStateFlow()


	//event for the tracker for prayer
	sealed class TrackerEvent
	{

		class UPDATE_TRACKER(val tracker : PrayerTracker) : TrackerEvent()
		class UPDATE_FAST_TRACKER(val tracker : FastTracker) : TrackerEvent()
		class GET_TRACKER_FOR_DATE(val date : String) : TrackerEvent()

		class GET_FAST_TRACKER_FOR_DATE(val date : String) : TrackerEvent()

		class SAVE_TRACKER(val tracker : PrayerTracker) : TrackerEvent()

		class SAVE_FAST_TRACKER(val tracker : FastTracker) : TrackerEvent()

		//event for the date selector
		class SHOW_DATE_SELECTOR(val shouldShow : Boolean) : TrackerEvent()

		//set date event
		class SET_DATE(val date : String) : TrackerEvent()

		//progress event
		class SET_PROGRESS(val progress : Int) : TrackerEvent()

		//return dates with trackers
		class GET_PROGRESS_FOR_DATE(val date : String) : TrackerEvent()

		//update Chart Data
		object GET_ALL_TRACKERS : TrackerEvent()
	}

	fun onEvent(event : TrackerEvent)
	{
		when (event)
		{
			is TrackerEvent.UPDATE_TRACKER -> updateTracker(event.tracker)
			is TrackerEvent.GET_TRACKER_FOR_DATE -> getTrackerForDate(event.date)
			is TrackerEvent.SAVE_TRACKER -> saveTracker(event.tracker)
			is TrackerEvent.SHOW_DATE_SELECTOR -> _showDateSelector.value = event.shouldShow
			is TrackerEvent.SET_DATE -> _dateState.value = event.date
			is TrackerEvent.SET_PROGRESS -> _progressState.value = event.progress
			is TrackerEvent.GET_PROGRESS_FOR_DATE -> getProgressForDate(event.date)
			is TrackerEvent.GET_ALL_TRACKERS -> getAllTrackers()
			is TrackerEvent.UPDATE_FAST_TRACKER -> updateFastTracker(event.tracker)
			is TrackerEvent.GET_FAST_TRACKER_FOR_DATE -> getFastTrackerForDate(event.date)
			is TrackerEvent.SAVE_FAST_TRACKER -> saveFastTracker(event.tracker)
		}
	}

	fun updateFastTracker(tracker : FastTracker)
	{
		viewModelScope.launch {
			try
			{
				val dataStore = LocalDataStore.getDataStore()
				val trackerExists = dataStore.fastTrackerExistsForDate(tracker.date)
				if (! trackerExists)
				{
					dataStore.saveFastTracker(tracker)
					_isFasting.value = false
					_fastTrackerState.value = FastTrackerState.Tracker(tracker)
				} else
				{
					dataStore.updateFastTracker(tracker)
					_isFasting.value = tracker.isFasting
					_fastTrackerState.value = FastTrackerState.Tracker(tracker)
				}
			} catch (e : Exception)
			{
				_fastTrackerState.value =
					FastTrackerState.Error(e.message ?: "An unknown error occurred")
			}
		}
	}

	fun getFastTrackerForDate(date : String)
	{
		viewModelScope.launch {
			try
			{
				val dataStore = LocalDataStore.getDataStore()
				val trackerExists = dataStore.fastTrackerExistsForDate(date)
				if (! trackerExists)
				{
					val tracker = FastTracker(date)
					dataStore.saveFastTracker(tracker)
					_fastTrackerState.value = FastTrackerState.Tracker(tracker)
					_isFasting.value = false
				} else
				{
					val tracker = dataStore.getFastTrackerForDate(date)
					_fastTrackerState.value = FastTrackerState.Tracker(tracker)
					_isFasting.value = tracker.isFasting
				}
			} catch (e : Exception)
			{
				_fastTrackerState.value =
					FastTrackerState.Error(e.message ?: "An unknown error occurred")
			}
		}
	}

	fun saveFastTracker(tracker : FastTracker)
	{
		viewModelScope.launch {
			try
			{
				val dataStore = LocalDataStore.getDataStore()
				dataStore.saveFastTracker(tracker)
				_fastTrackerState.value = FastTrackerState.Tracker(tracker)
			} catch (e : Exception)
			{
				_fastTrackerState.value =
					FastTrackerState.Error(e.message ?: "An unknown error occurred")
			}
		}
	}

	private fun getAllTrackers()
	{
		viewModelScope.launch {
			try
			{
				val dataStore = LocalDataStore.getDataStore()
				val trackers = dataStore.getAllTrackers()
				val datesAndProgress = trackers.map {
					val date = LocalDate.parse(it.date)
					//get the day of the week
					val dayOfMonth = date.dayOfMonth
					val progress = it.progress
					FloatEntry(
							dayOfMonth.toFloat() ,
							progress.toFloat()
							  )
				}
				//check how many prayers are completed for each day
				val completedPrayers = mutableListOf<Int>()

				//loop through the dates and progress
				for (date in datesAndProgress)
				{
					//get the day of the week
					val dayOfMonth = date.x.toInt()
					//get the progress
					val progress = date.y
					//get the number of prayers completed
					val prayersCompleted = progress / 20
					//add the entry
					completedPrayers.add(prayersCompleted.toInt())
				}

				//for each of the completedPrayers createa  list of float entries
				val entriesByDay = mutableListOf<List<FloatEntry>>()
				//loop through the completed prayers
				for (prayer in completedPrayers)
				{
					//create a list of float entries
					val entries = mutableListOf<FloatEntry>()
					//loop through the number of prayers completed
					for (i in 0 until prayer)
					{
						//add the entry
						entries.add(FloatEntry(datesAndProgress[i].x , datesAndProgress[i].y))
					}
					//add the entries to the list
					entriesByDay.add(entries)
				}

				//log the entries
				Log.d("Entries" , entriesByDay.toString())

				multiDataSetChartEntryModelProducer.setEntries(entriesByDay)





				_allTrackers.value = trackers
			} catch (e : Exception)
			{
				_trackerState.value = TrackerState.Error(e.message ?: "An unknown error occurred")
			}
		}
	}

	private fun getProgressForDate(date : String)
	{
		viewModelScope.launch {
			try
			{
				val dataStore = LocalDataStore.getDataStore()
				val tracker = dataStore.getTrackerForDate(date)
				_progressForDate.value = Pair(date , tracker.progress)
			} catch (e : Exception)
			{
				_trackerState.value = TrackerState.Error(e.message ?: "An unknown error occurred")
			}
		}
	}

	//function to get the tracker for a specific date
	fun getTrackerForDate(date : String)
	{
		viewModelScope.launch {
			try
			{
				val dataStore = LocalDataStore.getDataStore()
				val trackerExists = dataStore.checkIfTrackerExists(date)
				if (! trackerExists)
				{
					//chyeck if the date is inthe future
					val today = LocalDate.now()
					val dateToCheck = LocalDate.parse(date)
					if (dateToCheck.isAfter(today))
					{
						//set all the values to false
						_trackerState.value = TrackerState.Tracker(PrayerTracker(date))
						_dateState.value = date
						_fajrState.value = false
						_zuhrState.value = false
						_asrState.value = false
						_maghribState.value = false
						_ishaState.value = false
						_progressState.value = 0
					}else{
						val tracker = PrayerTracker(date)
						dataStore.saveTracker(tracker)
						_trackerState.value = TrackerState.Tracker(tracker)
						_dateState.value = date
						_fajrState.value = false
						_zuhrState.value = false
						_asrState.value = false
						_maghribState.value = false
						_ishaState.value = false
						_progressState.value = 0
					}
				} else
				{
					val tracker = dataStore.getTrackerForDate(date)
					_dateState.value = date
					_fajrState.value = tracker.fajr
					_zuhrState.value = tracker.dhuhr
					_asrState.value = tracker.asr
					_maghribState.value = tracker.maghrib
					_ishaState.value = tracker.isha
					_trackerState.value = TrackerState.Tracker(tracker)
					_progressState.value = tracker.progress
				}
			} catch (e : Exception)
			{
				_trackerState.value = TrackerState.Error(e.message ?: "An unknown error occurred")
			}
		}
	}

	//function to update a tracker
	fun updateTracker(tracker : PrayerTracker)
	{
		viewModelScope.launch {
			try
			{
				val dataStore = LocalDataStore.getDataStore()
				//check if the tracker exists
				val trackerExists = dataStore.checkIfTrackerExists(tracker.date)
				if (trackerExists)
				{
					dataStore.updateTracker(tracker)
					//get the updated tracker
					val updatedTracker = dataStore.getTrackerForDate(tracker.date)
					_trackerState.value = TrackerState.Tracker(updatedTracker)
					_dateState.value = tracker.date
					_fajrState.value = tracker.fajr
					_zuhrState.value = tracker.dhuhr
					_asrState.value = tracker.asr
					_maghribState.value = tracker.maghrib
					_ishaState.value = tracker.isha
					_progressState.value = tracker.progress
				} else
				{
					dataStore.saveTracker(tracker)
					//get the updated tracker
					val updatedTracker = dataStore.getTrackerForDate(tracker.date)
					_trackerState.value = TrackerState.Tracker(updatedTracker)
					_dateState.value = tracker.date
					_fajrState.value = tracker.fajr
					_zuhrState.value = tracker.dhuhr
					_asrState.value = tracker.asr
					_maghribState.value = tracker.maghrib
					_ishaState.value = tracker.isha
					_progressState.value = tracker.progress
				}
			} catch (e : Exception)
			{
				_trackerState.value = TrackerState.Error(e.message ?: "An unknown error occurred")
			}
		}
	}

	//function to save a tracker
	fun saveTracker(tracker : PrayerTracker)
	{
		viewModelScope.launch {
			try
			{
				val dataStore = LocalDataStore.getDataStore()
				dataStore.saveTracker(tracker)
				//get the updated tracker
				val updatedTracker = dataStore.getTrackerForDate(tracker.date)
				_dateState.value = tracker.date
				_fajrState.value = tracker.fajr
				_zuhrState.value = tracker.dhuhr
				_asrState.value = tracker.asr
				_maghribState.value = tracker.maghrib
				_ishaState.value = tracker.isha
				_trackerState.value = TrackerState.Tracker(updatedTracker)
				_progressState.value = tracker.progress
			} catch (e : Exception)
			{
				_trackerState.value = TrackerState.Error(e.message ?: "An unknown error occurred")
			}
		}
	}
}