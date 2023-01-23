package com.arshadshah.nimaz.activities

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.screens.NamesOfAllah
import com.arshadshah.nimaz.ui.theme.NimazTheme
import compose.icons.FeatherIcons
import compose.icons.feathericons.Pause
import compose.icons.feathericons.Play
import compose.icons.feathericons.StopCircle

class NamesOfAllah : ComponentActivity()
{

	private val mediaPlayer = MediaPlayer()

	override fun onDestroy()
	{
		super.onDestroy()
		mediaPlayer.release()
	}

	override fun onPause()
	{
		super.onPause()
		mediaPlayer.release()
	}

	@OptIn(ExperimentalMaterial3Api::class)
	override fun onCreate(savedInstanceState : Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContent {
			NimazTheme {
				prepareMediaPlayer(this@NamesOfAllah)
				val isPlaying = remember { mutableStateOf(false) }
				Scaffold(
						topBar = {
							TopAppBar(
									title = { Text(text = "Allah") } ,
									navigationIcon = {
										IconButton(onClick = {
											finish()
										}) {
											Icon(
													imageVector = Icons.Filled.ArrowBack ,
													contentDescription = "Back"
												)
										}
									} ,
									//buttons for play and pause and stop
									actions = {
										if (isPlaying.value)
										{
											IconButton(onClick = {
												mediaPlayer.stop()
												mediaPlayer.reset()
												prepareMediaPlayer(this@NamesOfAllah)
												isPlaying.value = false
											}
													  ) {
												Icon(
														imageVector = FeatherIcons.StopCircle ,
														contentDescription = "Back"
													)
											}
										}
										IconButton(onClick = {
											if (! mediaPlayer.isPlaying)
											{
												//start the audio
												mediaPlayer.start()
												isPlaying.value = true
											} else
											{
												mediaPlayer.pause()
												isPlaying.value = false
											}
										}
												  ) {
											if (isPlaying.value)
											{
												Icon(
														imageVector = FeatherIcons.Pause ,
														contentDescription = "Pause"
													)
											} else
											{
												Icon(
														imageVector = FeatherIcons.Play ,
														contentDescription = "Play"
													)
											}
										}

									}
									 )
						} ,
						) {
					it
					NamesOfAllah(it)
				}
			}
		}
	}


	private fun prepareMediaPlayer(context : Context)
	{
		val myUri : Uri =
			Uri.parse("android.resource://" + context.packageName + "/" + R.raw.asmaulhusna)
		mediaPlayer.apply {
			setAudioAttributes(
					AudioAttributes.Builder()
						.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
						.setUsage(AudioAttributes.USAGE_MEDIA)
						.build()
							  )
			setDataSource(context , myUri)
			prepare()
		}
	}
}