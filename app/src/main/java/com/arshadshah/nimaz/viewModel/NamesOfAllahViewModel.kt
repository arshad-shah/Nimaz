package com.arshadshah.nimaz.viewModel

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arshadshah.nimaz.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NamesOfAllahViewModel @Inject constructor() : ViewModel() {

    private val mediaPlayer = MediaPlayer()

    enum class PlaybackState {
        PLAYING, PAUSED, STOPPED
    }

    private val _playbackState = MutableStateFlow(PlaybackState.STOPPED)
    val playbackState = _playbackState.asStateFlow()

    sealed class AudioEvent {
        class Play(val context: Context) : AudioEvent()
        object Pause : AudioEvent()
        object Stop : AudioEvent()
    }

    fun handleAudioEvent(audioEvent: AudioEvent) {
        when (audioEvent) {
            is AudioEvent.Play -> playAudio(audioEvent.context)
            AudioEvent.Pause -> pauseAudio()
            AudioEvent.Stop -> stopAudio()
        }
    }

    private fun playAudio(context: Context) {
        viewModelScope.launch {
            try {
                if (_playbackState.value == PlaybackState.PAUSED) {
                    mediaPlayer.start()
                } else {
                    prepareMediaPlayer(context)
                    mediaPlayer.start()
                }
                _playbackState.value = PlaybackState.PLAYING
            } catch (e: Exception) {
                Log.e("NamesOfAllahViewModel", "Error playing audio", e)
            }
        }
    }

    private fun pauseAudio() {
        viewModelScope.launch {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
                _playbackState.value = PlaybackState.PAUSED
            }
        }
    }

    private fun stopAudio() {
        viewModelScope.launch {
            mediaPlayer.stop()
            mediaPlayer.reset()
            _playbackState.value = PlaybackState.STOPPED
        }
    }

    private fun prepareMediaPlayer(context: Context) {
        try {
            mediaPlayer.reset()
            val myUri: Uri =
                Uri.parse("android.resource://" + context.packageName + "/" + R.raw.asmaulhusna)
            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            mediaPlayer.setDataSource(context, myUri)
            mediaPlayer.prepare()
        } catch (e: Exception) {
            Log.e("NamesOfAllahViewModel", "Error preparing MediaPlayer", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer.release()
    }
}
