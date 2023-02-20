package com.arshadshah.nimaz.data.remote.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.data.remote.models.PrayerTracker
import com.arshadshah.nimaz.utils.LocalDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TrackerViewModel : ViewModel()
{

	sealed class TrackerState
	{
		object Loading : TrackerState()
		data class Tracker(val tracker : PrayerTracker) : TrackerState()
		data class Error(val message : String) : TrackerState()
	}

	private var _trackerState = MutableStateFlow<TrackerState>(TrackerState.Loading as TrackerState)
	val trackerState = _trackerState.asStateFlow()

	//state of date
	private var _dateState = MutableStateFlow<String>("")
	val dateState = _dateState.asStateFlow()

	//fajr
	private var _fajrState = MutableStateFlow<Boolean>(false)
	val fajrState = _fajrState.asStateFlow()

	//zuhr
	private var _zuhrState = MutableStateFlow<Boolean>(false)
	val zuhrState = _zuhrState.asStateFlow()

	//asr
	private var _asrState = MutableStateFlow<Boolean>(false)
	val asrState = _asrState.asStateFlow()

	//maghrib
	private var _maghribState = MutableStateFlow<Boolean>(false)
	val maghribState = _maghribState.asStateFlow()

	//isha
	private var _ishaState = MutableStateFlow<Boolean>(false)
	val ishaState = _ishaState.asStateFlow()

	//event for the tracker for prayer
	sealed class TrackerEvent
	{
		class UPDATE_TRACKER(val tracker :PrayerTracker) : TrackerEvent()
		class GET_TRACKER_FOR_DATE(val date : String) : TrackerEvent()
		class SAVE_TRACKER(val tracker : PrayerTracker) : TrackerEvent()
	}

	fun onEvent(event : TrackerEvent)
	{
		when(event)
		{
			is TrackerEvent.UPDATE_TRACKER -> updateTracker(event.tracker)
			is TrackerEvent.GET_TRACKER_FOR_DATE -> getTrackerForDate(event.date)
			is TrackerEvent.SAVE_TRACKER -> saveTracker(event.tracker)
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

				}else{
					val tracker = dataStore.getTrackerForDate(date)
					_dateState.value = date
					_fajrState.value = tracker.fajr
					_zuhrState.value = tracker.dhuhr
					_asrState.value = tracker.asr
					_maghribState.value = tracker.maghrib
					_ishaState.value = tracker.isha
					_trackerState.value = TrackerState.Tracker(tracker)
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
			}
			catch (e : Exception)
			{
				_trackerState.value = TrackerState.Error(e.message ?: "An unknown error occurred")
			}
		}
	}
}