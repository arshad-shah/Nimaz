package com.arshadshah.nimaz.viewModel

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NamesOfAllahViewModel : ViewModel()
{

	private val mediaPlayer = MediaPlayer()

	//playing audio state
	private var _isPlaying = MutableStateFlow(false)
	val isPlaying = _isPlaying.asStateFlow()

	//pause audio satte
	private var _isPaused = MutableStateFlow(false)
	val isPaused = _isPaused.asStateFlow()

	//stop audio state
	private var _isStopped = MutableStateFlow(true)
	val isStopped = _isStopped.asStateFlow()

	sealed class AudioEvent
	{

		class Play(val context : Context) : AudioEvent()
		object Pause : AudioEvent()
		object Stop : AudioEvent()
	}

	//events for the audio
	fun handleAudioEvent(audioEvent : AudioEvent)
	{
		when (audioEvent)
		{
			is AudioEvent.Play -> playAudio(audioEvent.context)
			AudioEvent.Pause -> pauseAudio()
			AudioEvent.Stop -> stopAudio()
		}
	}

	//play audio
	private fun playAudio(context : Context)
	{
		viewModelScope.launch(Dispatchers.IO) {
			//if the audio is paused
			if (isPaused.value)
			{
				//start the audio
				mediaPlayer.start()
				//set the playing state to true
				_isPlaying.value = true
				//set the paused state to false
				_isPaused.value = false
				//set the stopped state to false
				_isStopped.value = false
			} else
			{
				//prepare the audio
				prepareMediaPlayer(context)
				//start the audio
				mediaPlayer.start()
				//set the playing state to true
				_isPlaying.value = true
				//set the paused state to false
				_isPaused.value = false
				//set the stopped state to false
				_isStopped.value = false
			}
		}
	}

	//pause audio
	private fun pauseAudio()
	{
		viewModelScope.launch(Dispatchers.IO) {
			//if the audio is playing
			if (mediaPlayer.isPlaying)
			{
				//pause the audio
				mediaPlayer.pause()
				//set the playing state to false
				_isPlaying.value = false
				//set the paused state to true
				_isPaused.value = true
				//set the stopped state to false
				_isStopped.value = false
			}
		}
	}

	//stop audio
	private fun stopAudio()
	{
		viewModelScope.launch(Dispatchers.IO) {
			//stop the audio
			mediaPlayer.stop()
			//reset the audio
			mediaPlayer.reset()
			//set the playing state to false
			_isPlaying.value = false
			//set the paused state to false
			_isPaused.value = false
			//set the stopped state to true
			_isStopped.value = true
		}
	}

	private fun prepareMediaPlayer(context : Context)
	{
		mediaPlayer.reset()
		val myUri : Uri =
			Uri.parse("android.resource://" + context.packageName + "/" + R.raw.asmaulhusna)
		mediaPlayer.setAudioAttributes(
				 AudioAttributes.Builder()
					 .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
					 .setUsage(AudioAttributes.USAGE_MEDIA)
					 .build()
									  )
		mediaPlayer.setDataSource(context , myUri)
		mediaPlayer.prepare()
	}

	//release the audio
	override fun onCleared()
	{
		super.onCleared()
		mediaPlayer.release()
	}
}