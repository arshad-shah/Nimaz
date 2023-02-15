package com.arshadshah.nimaz.ui.components.ui

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.*
import com.arshadshah.nimaz.R
import java.util.*


@OptIn(ExperimentalMotionApi::class)
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

@OptIn(ExperimentalMotionApi::class)
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
	MotionLayout(
			motionScene = MotionScene(motionSceneJson) ,
			debug = EnumSet.of(MotionLayoutDebugFlags.SHOW_ALL) ,
			progress = progress ,
			modifier = Modifier
				.fillMaxWidth() ,
				) {
		val motionProperties = this.motionProperties(id = "bead")
		Beads(motionProperties = motionProperties)
	}
}

//beads
@Composable
fun Beads(motionProperties : MutableState<MotionLayoutScope.MotionProperties>)
{
	Box(
			modifier = Modifier.layoutId("prayer_beads")
	   ) {
		Box(
				modifier = Modifier
					.fillMaxWidth()
					.height(48.dp) ,
				contentAlignment = Alignment.Center
		   ) {
			Divider(
					modifier = Modifier
						.height(3.dp)
						.fillMaxWidth()
						.layoutId("thread") ,
					color = MaterialTheme.colorScheme.outline
				   )
		}
		Row {
			Box(
					modifier = Modifier
						.width(48.dp)
						.height(48.dp)
			   ) {
				Image(
						painter = painterResource(id = R.drawable.bead1) ,
						contentDescription = "Prayer Beads" ,
						modifier = Modifier
							.size(48.dp)
							.padding(vertical = 1.dp)
							.layoutId("bead")
					 )
			}
		}
	}
}

@Preview(showBackground = true)
@Composable
fun PrayerBeadsPreview()
{
	PrayerBeads()
}