package com.arshadshah.nimaz.ui.components.compass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.utils.PrivateSharedPreferences

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
				item {
					Image(
							painter = image ,
							contentDescription = "Compass option $index" ,
							modifier = Modifier
								//its a circle so clip it
								.clip(CircleShape)
								//scale the image based on the index
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