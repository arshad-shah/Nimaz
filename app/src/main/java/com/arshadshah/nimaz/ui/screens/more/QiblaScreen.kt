package com.arshadshah.nimaz.ui.screens.more

import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.QIBLA_VIEWMODEL_KEY
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_QIBLA
import com.arshadshah.nimaz.ui.components.compass.BearingAndLocationContainer
import com.arshadshah.nimaz.ui.components.compass.Dial
import com.arshadshah.nimaz.ui.components.compass.ImageSwitcherCard
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.viewModel.QiblaViewModel

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun QiblaScreen(paddingValues : PaddingValues)
{
	val context = LocalContext.current
	val viewModel = viewModel(
			 key = QIBLA_VIEWMODEL_KEY ,
			 initializer = { QiblaViewModel(context) } ,
			 viewModelStoreOwner = context as ComponentActivity
							 )

	LaunchedEffect(Unit) {
		viewModel.loadQibla(context)
	}

	val state = remember { viewModel.qiblaState }.collectAsState()
	val isLoading = remember { viewModel.isLoading }.collectAsState()
	val errorMessage = remember { viewModel.errorMessage }.collectAsState()
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
		BearingAndLocationContainer(state , isLoading , errorMessage)
		Dial(
				 state = state ,
				 imageToDisplay = imageToDisplay !! ,
				 isLoading = isLoading.value ,
				 errorMessage = errorMessage.value
			)
		ImageSwitcherCard(changeImageIndex = changeImageIndex)
	}
}
