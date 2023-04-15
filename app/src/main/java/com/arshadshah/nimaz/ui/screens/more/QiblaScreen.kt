package com.arshadshah.nimaz.ui.screens.more

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.QIBLA_VIEWMODEL_KEY
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_QIBLA
import com.arshadshah.nimaz.ui.components.compass.BearingAndLocationContainer
import com.arshadshah.nimaz.ui.components.compass.Dial
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.viewModel.QiblaViewModel

@Composable
fun QiblaScreen(paddingValues : PaddingValues)
{
	val context = LocalContext.current
	val viewModel = viewModel(
			key = QIBLA_VIEWMODEL_KEY ,
			initializer = { QiblaViewModel(context) } ,
			viewModelStoreOwner = context as ComponentActivity
							 )

	val state = remember { viewModel.qiblaState }.collectAsState()
	Log.d(AppConstants.QIBLA_COMPASS_SCREEN_TAG , "QiblaScreen: ${state.value}")

	val sharedPreferences = PrivateSharedPreferences(context)
	val imageIndexFromStorage = sharedPreferences.getDataInt("QiblaImageIndex")

	val imagesMapped = mapOf(
			0 to painterResource(id = R.drawable.qibla1) ,
			1 to painterResource(id = R.drawable.qibla2) ,
			2 to painterResource(id = R.drawable.qibla3) ,
			3 to painterResource(id = R.drawable.qibla4) ,
			4 to painterResource(id = R.drawable.qibla5) ,
			5 to painterResource(id = R.drawable.qibla6) ,
							)
	//create a mu	 that will be used to switch between the images
	var imageToDisplay by remember { mutableStateOf(imagesMapped[imageIndexFromStorage]) }


	//a function that will change the image index to the index given
	val changeImageIndex = { index : Int ->
		imageToDisplay = imagesMapped[index]
		sharedPreferences.saveDataInt("QiblaImageIndex" , index)
	}


	Column(
			modifier = Modifier
				.padding(paddingValues)
				.fillMaxSize()
				.verticalScroll(rememberScrollState())
				.testTag(TEST_TAG_QIBLA) ,
			horizontalAlignment = Alignment.CenterHorizontally ,
			verticalArrangement = Arrangement.Top
		  ) {
		BearingAndLocationContainer(state)
		Dial(state = state , imageToDisplay = imageToDisplay !!)
		ImageSwitcherCard(changeImageIndex = changeImageIndex)
	}
}

@Composable
fun ImageSwitcherCard(changeImageIndex : (Int) -> Unit)
{


	val sharedPreferences = PrivateSharedPreferences(LocalContext.current)
	val imageIndexFromStorage = sharedPreferences.getDataInt("QiblaImageIndex")
	val state = rememberLazyListState(initialFirstVisibleItemIndex = imageIndexFromStorage)

	//an is selected state that will be used to change the size of the image
	//it tracks the index of the image
	val isSelected = remember { mutableStateOf(imageIndexFromStorage) }

	//map the images to a number
	val imagesMapped = mapOf(
			0 to painterResource(id = R.drawable.qibla1) ,
			1 to painterResource(id = R.drawable.qibla2) ,
			2 to painterResource(id = R.drawable.qibla3) ,
			3 to painterResource(id = R.drawable.qibla4) ,
			4 to painterResource(id = R.drawable.qibla5) ,
			5 to painterResource(id = R.drawable.qibla6) ,
							)

	ElevatedCard(
			shape = MaterialTheme.shapes.extraLarge ,
				) {
		LazyRow(
				state = state ,
				modifier = Modifier
					.padding(horizontal = 8.dp)
					.fillMaxWidth() ,
				horizontalArrangement = Arrangement.Center ,
				verticalAlignment = Alignment.CenterVertically
			   ) {
				imagesMapped.forEach { (index , image) ->
					item{
						Image(
								painter = image ,
								contentDescription = "Compass" ,
								modifier = Modifier
									.scale(animateFloatAsState(if (isSelected.value == index) 1.5f else 1f).value)
									.size(80.dp)
									.padding(vertical = 16.dp , horizontal = 8.dp)
									.clickable(
											role = Role.RadioButton ,
											  ) {
										changeImageIndex(index)
										isSelected.value = index
									} ,
								alignment = Alignment.Center
							 )
					}
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
			4 to painterResource(id = R.drawable.qibla5) ,
			5 to painterResource(id = R.drawable.qibla6) ,
							)


	//a function that will change the image index to the index given
	val changeImageIndex = { index : Int ->
		imageToDisplay = imagesMapped[index] ?: defaultImage
	}

	ImageSwitcherCard(changeImageIndex)
}
