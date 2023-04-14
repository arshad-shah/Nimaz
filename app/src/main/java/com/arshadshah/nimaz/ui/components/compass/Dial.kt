package com.arshadshah.nimaz.ui.components.compass

import android.annotation.SuppressLint
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.viewModel.QiblaViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun Dial(state : State<QiblaViewModel.QiblaState> , imageToDisplay : Painter)
{

	val context = LocalContext.current
	val scope = rememberCoroutineScope()

	var data by remember { mutableStateOf<SensorData?>(null) }

	DisposableEffect(Unit) {
		val dataManager = SensorDataManager(context)
		dataManager.init(context)

		val job = scope.launch {
			dataManager.data
				.receiveAsFlow()
				.onEach { data = it }
				.collect {
					// do nothing
				}
		}

		onDispose {
			dataManager.cancel()
			job.cancel()
		}
	}
	when (val qiblaState = state.value)
	{
		is QiblaViewModel.QiblaState.Loading ->
		{
			DialUI(0.0 , data , imageToDisplay)
		}

		is QiblaViewModel.QiblaState.Error ->
		{
			DialUI(0.0 , data , imageToDisplay)
		}

		is QiblaViewModel.QiblaState.Success ->
		{
			qiblaState.bearing?.let { DialUI(it , data , imageToDisplay) }
		}
	}
}

@OptIn(DelicateCoroutinesApi::class)
@SuppressLint("UnrememberedMutableState")
@Composable
fun DialUI(bearing : Double , data : SensorData? , imageToDisplay : Painter)
{
	val yaw by derivedStateOf { (data?.yaw ?: 0f) }
	val pitch by derivedStateOf { (data?.pitch ?: 0f) }
	val roll by derivedStateOf { (data?.roll ?: 0f) }
	val degree = ((Math.toDegrees(yaw.toDouble()) + 360).toFloat() % 360)

	val currentAngle = 0f
	val rotateAnim = remember { Animatable(currentAngle) }
	val target = (bearing - degree).toFloat()
	val context = LocalContext.current
	val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

	val pointingToQibla = abs(target) < 5f

	val directionToTurn = when
	{
		abs(target) < 5f -> "You are facing the Qibla"
		target > 0f -> "Turn Right"
		else -> "Turn Left"
	}

	//if the user is facing the qibla, vibrate the phone and show a message to the user and stop the vibration after 1 second and stop animating the dial until the user turns away from the qibla
	LaunchedEffect(key1 = target) {
		if (abs(target) < 5f)
		{
			//animate to 0f
			rotateAnim.animateTo(0f , tween(100))
			//stop the animation
			rotateAnim.stop()
		} else
		{
			//start the animation
			rotateAnim.animateTo(target , tween(200))
		}
	}

	LaunchedEffect(pointingToQibla) {
		if (pointingToQibla)
		{
			//create a single shot vibration
			vibrator.vibrate(VibrationEffect.createOneShot(100 , VibrationEffect.DEFAULT_AMPLITUDE))
		} else
		{
			vibrator.cancel()
		}
	}

	ElevatedCard(
			shape = MaterialTheme.shapes.extraLarge ,
			modifier = Modifier
				.fillMaxWidth()
				.padding(16.dp) ,
				) {

		Text(
				modifier = Modifier
					.padding(16.dp)
					.fillMaxWidth()
					.align(Alignment.CenterHorizontally) ,
				text = directionToTurn ,
				style = MaterialTheme.typography.headlineMedium ,
				textAlign = TextAlign.Center
			)

		Icon(
				painter = painterResource(id = R.drawable.circle_close_icon) ,
				contentDescription = "dot" ,
				modifier = Modifier
					.fillMaxWidth()
					.padding(vertical = 8.dp)
					.size(24.dp) ,
				tint = if (pointingToQibla) MaterialTheme.colorScheme.inversePrimary else Color.Red
			)
		//the dial
		Image(
				painter = imageToDisplay ,
				contentDescription = "Compass" ,
				modifier = Modifier
					.rotate(rotateAnim.value)
					.fillMaxWidth()
					.padding(16.dp) ,
				alignment = Alignment.Center
			 )
	}
}