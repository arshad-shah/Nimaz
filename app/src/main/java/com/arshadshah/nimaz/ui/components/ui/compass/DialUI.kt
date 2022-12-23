package com.arshadshah.nimaz.ui.components.ui.compass

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.ui.components.bLogic.compass.SensorData
import kotlin.math.abs

@SuppressLint("UnrememberedMutableState")
@Composable
fun DialUI(bearing : Double , data : SensorData?)
{
	val yaw by derivedStateOf { (data?.yaw ?: 0f) }
	val pitch by derivedStateOf { (data?.pitch ?: 0f) }
	val roll by derivedStateOf { (data?.roll ?: 0f) }
	val degree = ((Math.toDegrees(yaw.toDouble()) + 360).toFloat() % 360)

	val currentAngle = 0f
	val rotateAnim = remember { Animatable(currentAngle) }
	val target = (bearing - degree).toFloat()

	LaunchedEffect(key1 = degree, key2 = pitch , key3 = roll){
		rotateAnim.animateTo(target, tween(300))
	}

	ElevatedCard(modifier = Modifier
		.fillMaxWidth()
		.padding(16.dp)) {
			//the dot
			Image(
				painter = painterResource(id = com.arshadshah.nimaz.R.drawable.ic_dot),
				contentDescription = "dot",
				modifier = Modifier
					.fillMaxWidth().padding(vertical = 8.dp).size(24.dp),
				alignment = Alignment.Center,
				colorFilter = ColorFilter.tint(
					if (abs(target) < 5) MaterialTheme.colorScheme.inversePrimary else Color.Red
											  )
			)
			//the dial
			Image(
					painter = painterResource(id = com.arshadshah.nimaz.R.drawable.ic_qibla_compass) ,
					contentDescription = "Compass" ,
					modifier = Modifier
						.rotate(rotateAnim.value)
						.fillMaxWidth().padding(vertical = 16.dp) ,
					alignment = Alignment.Center
				 )
	}
}

@Preview
@Composable
fun DialUIPreview()
{
	DialUI(bearing = 0.0 , data = null)
}