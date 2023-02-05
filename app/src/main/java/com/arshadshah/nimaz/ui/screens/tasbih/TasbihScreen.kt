package com.arshadshah.nimaz.ui.screens.tasbih

import android.content.Context
import android.os.Vibrator
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.components.bLogic.tasbih.Counter
import com.arshadshah.nimaz.ui.theme.quranFont
import compose.icons.FeatherIcons
import compose.icons.feathericons.Minus
import es.dmoral.toasty.Toasty

@OptIn(ExperimentalMaterialApi::class , ExperimentalMaterial3Api::class)
@Composable
fun TasbihScreen(paddingValues : PaddingValues)
{


	val resources = LocalContext.current.resources
	val context = LocalContext.current

	val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
	val vibrationAllowed = remember { mutableStateOf(true) }
	val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
			bottomSheetState = BottomSheetState(BottomSheetValue.Collapsed)
																   )
	//a callback that get the count from the counter so that it can be displayed in the bottom sheet
	//count should not go below 0
	val count = remember {
		mutableStateOf(
				context.getSharedPreferences("tasbih" , 0).getInt("count" , 0)
					  )
	}
	val sharedPref = context.getSharedPreferences("tasbih" , 0)
	//if an  item is clicked, it will be highlighted and the and a count will be added to it the index of the item and a boolean to check if the item is highlighted
	val selected =
		remember { mutableStateOf(sharedPref.getBoolean("selected" , false)) }
	val indexSelected =
		remember { mutableStateOf(sharedPref.getInt("indexSelected" , - 1)) }

	//reset
	val reset = remember { mutableStateOf(false) }

	//if user leaves tis activity or the app, the selected item and indexSelected will be saved
	//buit if the count is 0, the selected item and indexSelected will be reset
	LaunchedEffect(
			key1 = selected.value ,
			key2 = indexSelected.value ,
			key3 = reset.value
				  ) {
		//if the count is 0, then the selected item will be reset
		if (count.value == 0 && reset.value == true)
		{
			selected.value = false
			indexSelected.value = - 1
			//set reset to false so that it will not reset again
			reset.value = false
		}
		//if the count is not 0, then the selected item will be saved
		else
		{
			sharedPref.edit().putBoolean("selected" , selected.value).apply()
			sharedPref.edit().putInt("indexSelected" , indexSelected.value).apply()
		}
	}

	//if a new item is selected, then scroll to that item
	val listState = rememberLazyListState()
	LaunchedEffect(key1 = indexSelected.value) {
		if (indexSelected.value != - 1)
		{
			listState.animateScrollToItem(indexSelected.value)
		}
	}

	var showResetDialog = remember { mutableStateOf(false) }

	//the state of the lazy column, it should scroll to the item where selected is true
	//get the arrays
	val englishNames = resources.getStringArray(R.array.tasbeehTransliteration)
	val arabicNames = resources.getStringArray(R.array.tasbeeharabic)
	val translationNames = resources.getStringArray(R.array.tasbeehTranslation)
	BottomSheetScaffold(
			backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.background ,
			contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground ,
			modifier = Modifier
				.shadow(16.dp , CardDefaults.elevatedShape)
				.padding(paddingValues) ,
			topBar = {
				TopAppBar(
						title = { } ,
						actions = {
							//vibration toggle button for tasbih to provide feedback
							IconButton(onClick = {
								vibrationAllowed.value = ! vibrationAllowed.value
								//mute the vibration
								if (! vibrationAllowed.value)
								{
									vibrator.cancel()
								}
							}) {
								Icon(
										painter = if (vibrationAllowed.value) painterResource(
												id = R.drawable.vibration
																							 )
										else painterResource(
												id = R.drawable.close
															) ,
										contentDescription = "Vibration"
									)
							}

							//a reset button to reset the count
							IconButton(onClick = {
								showResetDialog.value = true
							}) {
								Icon(
										imageVector = Icons.Filled.Refresh ,
										contentDescription = "Reset" ,
									)
							}
						}
						 )
			} ,
			scaffoldState = bottomSheetScaffoldState ,
			sheetShape = RoundedCornerShape(topStart = 16.dp , topEnd = 16.dp) ,
			sheetElevation = 8.dp ,
			sheetGesturesEnabled = true ,
			sheetContent = {
				//an icon to show where to pull the bottom sheet from
				Icon(
						imageVector = FeatherIcons.Minus ,
						contentDescription = "Pull to expand" ,
						modifier = Modifier
							.align(Alignment.CenterHorizontally)
							.size(48.dp) ,
					)
				//show the one where selected is true if none is selected, show the first one
				LazyColumn(state = listState) {
					items(englishNames.size) { index ->
						TasbihRow(
								englishNames[index] ,
								arabicNames[index] ,
								translationNames[index] ,
								count = count ,
								selected = selected ,
								index = index ,
								indexSelected = indexSelected ,
								 )
					}
				}


			} ,
			sheetPeekHeight = 200.dp ,
					   ) {
		it
		Counter(vibrator , it , vibrationAllowed , count , reset , showResetDialog)
	}
}

@Composable
fun TasbihRow(
	englishName : String ,
	arabicName : String ,
	translationName : String ,
	count : MutableState<Int> ,
	selected : MutableState<Boolean> ,
	index : Int ,
	indexSelected : MutableState<Int> ,
			 )
{

	val context = LocalContext.current
	ElevatedCard(
			modifier = Modifier
				.fillMaxWidth()
				.padding(8.dp)
				.clickable {
					//check if a tasbih is already selected
					//if yes then show toast saying that only one tasbih can be selected at a time
					//if a tasbih is selected already then the indexSelected should be equal to anything other than -1
					//if thats the case then show the toast
					if (indexSelected.value != - 1 && indexSelected.value != index)
					{
						Toasty
							.warning(
									context ,
									"Only one tasbih can be selected at a time" ,
									Toast.LENGTH_SHORT ,
									true
									)
							.show()
					} else
					{
						//if the item is clicked, it will be highlighted and the and a count will be added to it the index of the item and a boolean to check if the item is highlighted
						selected.value = ! selected.value
						if (selected.value)
						{
							indexSelected.value = index
						} else
						{
							indexSelected.value = - 1
						}
					}
				} ,
			colors = CardDefaults.elevatedCardColors(
					containerColor = if (index == indexSelected.value)
						androidx.compose.material3.MaterialTheme.colorScheme.secondary
					else androidx.compose.material3.MaterialTheme.colorScheme.surface
													) ,
				) {
		Column(
				horizontalAlignment = Alignment.Start ,
				verticalArrangement = Arrangement.Center
			  ) {
			if (index == indexSelected.value)
			{
				Text(
						modifier = Modifier.padding(8.dp) ,
						text = "Count: ${count.value}" ,
						style = androidx.compose.material3.MaterialTheme.typography.titleMedium ,
					)
			}
			CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
				Text(
						text = arabicName ,
						style = androidx.compose.material3.MaterialTheme.typography.titleLarge ,
						fontFamily = quranFont ,
						modifier = Modifier
							.padding(4.dp)
							.fillMaxWidth() ,
						color = if (index == indexSelected.value) androidx.compose.material3.MaterialTheme.colorScheme.onSecondary else androidx.compose.material3.MaterialTheme.colorScheme.onSurface ,
					)
			}
			Text(
					modifier = Modifier
						.padding(4.dp)
						.fillMaxWidth() ,
					text = englishName ,
					style = androidx.compose.material3.MaterialTheme.typography.titleSmall ,
					color = if (index == indexSelected.value) androidx.compose.material3.MaterialTheme.colorScheme.onSecondary else androidx.compose.material3.MaterialTheme.colorScheme.onSurface ,
				)
			Text(
					modifier = Modifier
						.padding(4.dp)
						.fillMaxWidth() ,
					text = translationName ,
					style = androidx.compose.material3.MaterialTheme.typography.titleSmall ,
					color = if (index == indexSelected.value) androidx.compose.material3.MaterialTheme.colorScheme.onSecondary else androidx.compose.material3.MaterialTheme.colorScheme.onSurface ,
				)
		}
	}

}