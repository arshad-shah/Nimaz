package com.arshadshah.nimaz.ui.components.ui

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R


@Composable
fun PrayerBeads()
{
	val progress = remember { mutableStateOf(0f) }
	//a prayer beads animation that is used in the prayer screen
	//we have a motion layout that has a constraint set that is used to animate the beads
	//each swipe of the beads starts a new animation that is defined in the motion scene
	//the animation can be forwared or backward for increasing or decreasing the prayer count

	//json for the motion scene

	Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(8.dp) ,
			verticalArrangement = Arrangement.SpaceEvenly ,
		  ) {

		PrayerBeadsMotionLayout(progress = progress.value)

		Slider(value = progress.value , onValueChange = { progress.value = it })
	}
}

@Composable
fun PrayerBeadsMotionLayout(progress : Float)
{
	Log.d("progress" , progress.toString())
	val context = LocalContext.current
	val motionSceneJson = remember {
		context.resources.openRawResource(R.raw.prayer_beads_motion_scene).readBytes()
			.decodeToString()
	}
	Log.d("motionSceneJson" , motionSceneJson)
}


@Preview(showBackground = true)
@Composable
fun PrayerBeadsPreview()
{
	PrayerBeads()
}