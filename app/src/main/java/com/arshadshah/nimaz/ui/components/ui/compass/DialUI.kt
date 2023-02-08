package com.arshadshah.nimaz.ui.components.ui.compass

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.ui.components.bLogic.compass.SensorData
import com.arshadshah.nimaz.ui.components.ui.icons.Dot
import com.arshadshah.nimaz.ui.components.ui.icons.QiblaCompassMain
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlin.math.abs

@OptIn(DelicateCoroutinesApi::class)
@SuppressLint("UnrememberedMutableState")
@Composable
fun DialUI(bearing : Double , data : SensorData?)
{

	val hasSensor =
		LocalContext.current.packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER) && LocalContext.current.packageManager.hasSystemFeature(
				PackageManager.FEATURE_SENSOR_COMPASS
																																								 )
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
				imageVector = Icons.Dot ,
				contentDescription = "dot" ,
				modifier = Modifier
					.fillMaxWidth()
					.padding(vertical = 8.dp)
					.size(24.dp) ,
				tint = if (pointingToQibla) MaterialTheme.colorScheme.inversePrimary else Color.Red
			)
		//the dial
		Image(
				imageVector = Icons.QiblaCompassMain ,
				contentDescription = "Compass" ,
				modifier = Modifier
					.rotate(rotateAnim.value)
					.fillMaxWidth()
					.padding(vertical = 16.dp) ,
				alignment = Alignment.Center
			 )
	}
}