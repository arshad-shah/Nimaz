package com.arshadshah.nimaz.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.viewModel.QiblaViewModel
import com.arshadshah.nimaz.ui.components.bLogic.compass.BearingAndLocationContainer
import com.arshadshah.nimaz.ui.components.bLogic.compass.Dial

@Composable
fun QiblaScreen(paddingValues : PaddingValues)
{
	val context = LocalContext.current
	val viewModel = QiblaViewModel(context)

	val state = remember { viewModel.qiblaState }.collectAsState()
	Log.d(AppConstants.QIBLA_COMPASS_SCREEN_TAG , "QiblaScreen: ${state.value}")

	val defaultImage = painterResource(id = R.drawable.qibla1)
	//create a mu	 that will be used to switch between the images
	var imageToDisplay by remember { mutableStateOf(defaultImage) }

	val imagesMapped = mapOf(
			0 to painterResource(id = R.drawable.qibla1) ,
			1 to painterResource(id = R.drawable.qibla2) ,
			2 to painterResource(id = R.drawable.qibla3) ,
			3 to painterResource(id = R.drawable.qibla4) ,
							)


	//a function that will change the image index to the index given
	val changeImageIndex = { index : Int ->
		imageToDisplay = imagesMapped[index] ?: defaultImage
	}


	Column(
			modifier = Modifier
				.padding(paddingValues)
				.fillMaxSize() ,
			horizontalAlignment = Alignment.CenterHorizontally ,
			verticalArrangement = Arrangement.Center
		  ) {
		BearingAndLocationContainer(state)
		Dial(state = state , imageToDisplay = imageToDisplay)
		ImageSwitcherCard(changeImageIndex)
	}
}

@Composable
fun ImageSwitcherCard(changeImageIndex : (Int) -> Unit)
{

	//an is selected state that will be used to change the size of the image
	//it tracks the index of the image
	val isSelected = remember { mutableStateOf(0) }

	//map the images to a number
	val imagesMapped = mapOf(
			0 to painterResource(id = R.drawable.qibla1) ,
			1 to painterResource(id = R.drawable.qibla2) ,
			2 to painterResource(id = R.drawable.qibla3) ,
			3 to painterResource(id = R.drawable.qibla4) ,
							)

	ElevatedCard {
		Row(
				modifier = Modifier
					.fillMaxWidth() ,
				horizontalArrangement = Arrangement.Center ,
				verticalAlignment = Alignment.CenterVertically
		   ) {
			imagesMapped.forEach { (index , image) ->
				Image(
						painter = image ,
						contentDescription = "Compass" ,
						modifier = Modifier
							.size(
									if (isSelected.value == index) 100.dp
									else 80.dp
								 )
							.padding(vertical = 16.dp)
							.clickable {
								changeImageIndex(index)
								isSelected.value = index
							} ,
						alignment = Alignment.Center
					 )
			}
		}
	}

}

//preview
@Preview
@Composable
fun ImageSwitcherCardPreview()
{
	val defaultImage = painterResource(id = R.drawable.qibla1)
	//create a mu	 that will be used to switch between the images
	var imageToDisplay by remember { mutableStateOf(defaultImage) }
	val imagesMapped = mapOf(
			0 to painterResource(id = R.drawable.qibla1) ,
			1 to painterResource(id = R.drawable.qibla2) ,
			2 to painterResource(id = R.drawable.qibla3) ,
			3 to painterResource(id = R.drawable.qibla4) ,
							)


	//a function that will change the image index to the index given
	val changeImageIndex = { index : Int ->
		imageToDisplay = imagesMapped[index] ?: defaultImage
	}

	ImageSwitcherCard(changeImageIndex)
}
