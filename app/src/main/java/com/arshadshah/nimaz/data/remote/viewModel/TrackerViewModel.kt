package com.arshadshah.nimaz.data.remote.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.data.remote.models.PrayerTracker
import com.arshadshah.nimaz.utils.LocalDataStore
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

	//dates with trackers
	private var _datesWithTrackers = MutableStateFlow(listOf<String>())
	val datesWithTrackers = _datesWithTrackers.asStateFlow()

	//event for the tracker for prayer
	sealed class TrackerEvent
	{
		class UPDATE_TRACKER(val tracker :PrayerTracker) : TrackerEvent()
		class GET_TRACKER_FOR_DATE(val date : String) : TrackerEvent()
		class SAVE_TRACKER(val tracker : PrayerTracker) : TrackerEvent()

		//event for the date selector
		class SHOW_DATE_SELECTOR(val shouldShow : Boolean) : TrackerEvent()

		//set date event
		class SET_DATE(val date : String) : TrackerEvent()

		//progress event
		class SET_PROGRESS(val progress : Int) : TrackerEvent()

		//return dates with trackers
		class GET_PROGRESS_FOR_DATE(val date : String) : TrackerEvent()
	}

	fun onEvent(event : TrackerEvent)
	{
		when(event)
		{
			is TrackerEvent.UPDATE_TRACKER -> updateTracker(event.tracker)
			is TrackerEvent.GET_TRACKER_FOR_DATE -> getTrackerForDate(event.date)
			is TrackerEvent.SAVE_TRACKER -> saveTracker(event.tracker)
			is TrackerEvent.SHOW_DATE_SELECTOR -> _showDateSelector.value = event.shouldShow
			is TrackerEvent.SET_DATE -> _dateState.value = event.date
			is TrackerEvent.SET_PROGRESS -> _progressState.value = event.progress
			is TrackerEvent.GET_PROGRESS_FOR_DATE -> getProgressForDate(event.date)
		}
	}

	private fun getProgressForDate(date : String)
	{
		viewModelScope.launch {
			try
			{
				val dataStore = LocalDataStore.getDataStore()
				val tracker = dataStore.getTrackerForDate(date)
				_progressState.value = tracker.progress
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
				if (!trackerExists)
				{
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

				}else{
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
			}
			catch (e : Exception)
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
				}
				else
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
			}
			catch (e : Exception)
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
			}
			catch (e : Exception)
			{
				_trackerState.value = TrackerState.Error(e.message ?: "An unknown error occurred")
			}
		}
	}
}