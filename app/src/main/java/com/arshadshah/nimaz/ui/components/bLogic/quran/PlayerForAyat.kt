package com.arshadshah.nimaz.ui.components.bLogic.quran

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PlayerForAyat(
	duration : MutableState<Int> ,
	isPlaying : MutableState<Boolean> ,
	isPaused : MutableState<Boolean> ,
	isStopped : MutableState<Boolean> ,
	isDownloaded : MutableState<Boolean> ,
	hasAudio : MutableState<Boolean> ,
	onPlayClicked : () -> Unit ,
	onPauseClicked : () -> Unit ,
	onStopClicked : () -> Unit ,
	isLoading : Boolean ,
				 )
{

	//a linear progress to show the audio player progress
	if (isPlaying.value || isPaused.value)
	{

		//get seconds from the duration
		val seconds = duration.value / 1000f

		//every second update the progress until seconds is reached
		val currentProgress = remember { mutableStateOf(0f) }
		LaunchedEffect(key1 = isPlaying.value , key2 = isPaused.value) {
			//start the timer make sure to pause the timer when the audio is paused
			if (isPlaying.value && ! isPaused.value)
			{
				//start the timer
				launch {
					while (currentProgress.value < seconds)
					{
						delay(100)
						currentProgress.value += 0.1f
						//when the progress reaches the duration stop the timer
						if (currentProgress.value >= seconds || isStopped.value)
						{
							isPlaying.value = false
							cancel(
									cause = CancellationException(
											"Audio finished playing"
																 )
								  )
						}
					}
				}
			}
		}
		//log the current progress
		Log.d("AyaListItemUI" , "current progress is ${currentProgress.value / seconds}")
		LinearProgressIndicator(
				progress = currentProgress.value / seconds ,
				modifier = Modifier
					.fillMaxWidth()
					.padding(4.dp)
					.height(8.dp) ,
				strokeCap = StrokeCap.Round ,
							   )
	}


	if (isDownloaded.value || hasAudio.value)
	{
		//a row to show th play button and the audio player
		Row(
				horizontalArrangement = Arrangement.Center ,
				verticalAlignment = Alignment.CenterVertically ,
				modifier = Modifier
					.fillMaxWidth()
					.padding(vertical = 4.dp)
		   ) {
			if (isPaused.value || isStopped.value || ! isPlaying.value)
			{
				//play and pause button
				IconButton(
						onClick = { onPlayClicked() } ,
						enabled = ! isLoading ,
						modifier = Modifier
							.align(Alignment.CenterVertically)
						  ) {
					Icon(
							painter = painterResource(id = R.drawable.play_icon) ,
							contentDescription = "Play" ,
							tint = MaterialTheme.colorScheme.primary ,
							modifier = Modifier
								.size(24.dp)
								.padding(horizontal = 4.dp)
								.placeholder(
										visible = isLoading ,
										color = MaterialTheme.colorScheme.outline ,
										shape = RoundedCornerShape(4.dp) ,
										highlight = PlaceholderHighlight.shimmer(
												highlightColor = Color.White ,
																				)
											)
						)
				}
			}

			if (isPlaying.value && ! isStopped.value)
			{
				//play and puase button
				IconButton(
						onClick = { onPauseClicked() } ,
						enabled = true ,
						modifier = Modifier
							.align(Alignment.CenterVertically)
						  ) {
					Icon(
							painter = painterResource(id = R.drawable.pause_icon) ,
							contentDescription = "Pause" ,
							tint = MaterialTheme.colorScheme.primary ,
							modifier = Modifier
								.size(24.dp)
								.padding(horizontal = 4.dp)
						)
				}
			}

			if (isPlaying.value || isPaused.value)
			{
				//stop button
				IconButton(
						onClick = { onStopClicked() } ,
						enabled = true ,
						modifier = Modifier
							.align(Alignment.CenterVertically)
						  ) {
					Icon(
							painter = painterResource(id = R.drawable.stop_icon) ,
							contentDescription = "Stop" ,
							tint = MaterialTheme.colorScheme.primary ,
							modifier = Modifier
								.size(24.dp)
								.padding(horizontal = 4.dp)
						)
				}
			}
		}
	}

}

@Preview
@Composable
fun PlayerForAyatPreview()
{
	PlayerForAyat(
			duration = remember { mutableStateOf(5) } ,
			isPlaying = remember { mutableStateOf(true) } ,
			isPaused = remember { mutableStateOf(true) } ,
			isStopped = remember { mutableStateOf(true) } ,
			isDownloaded = remember { mutableStateOf(true) } ,
			hasAudio = remember { mutableStateOf(true) } ,
			onPlayClicked = { } ,
			onPauseClicked = { } ,
			onStopClicked = { } ,
			isLoading = false ,
				 )
}