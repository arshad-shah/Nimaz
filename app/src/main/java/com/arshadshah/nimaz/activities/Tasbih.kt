package com.arshadshah.nimaz.activities

import android.content.Context
import android.os.Bundle
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.components.bLogic.tasbih.Counter
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.ui.theme.quranFont
import compose.icons.FeatherIcons
import compose.icons.feathericons.Minus
import es.dmoral.toasty.Toasty

class Tasbih : ComponentActivity()
{

	@OptIn(ExperimentalMaterial3Api::class , ExperimentalMaterialApi::class)
	override fun onCreate(savedInstanceState : Bundle?)
	{
		val vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
		super.onCreate(savedInstanceState)
		setContent {
			NimazTheme {
				val vibrationAllowed = remember { mutableStateOf(true) }
				val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
						bottomSheetState = BottomSheetState(BottomSheetValue.Collapsed)
																			   )

				val resources = LocalContext.current.resources
				//a callback that get the count from the counter so that it can be displayed in the bottom sheet
				//count should not go below 0
				val count = remember {
					mutableStateOf(
							this.getSharedPreferences("tasbih" , 0).getInt("count" , 0)
								  )
				}
				val sharedPref = this.getSharedPreferences("tasbih" , 0)
				//if an  item is clicked, it will be highlighted and the and a count will be added to it the index of the item and a boolean to check if the item is highlighted
				val selected = remember { mutableStateOf(sharedPref.getBoolean("selected" , false)) }
				val indexSelected = remember { mutableStateOf(sharedPref.getInt("indexSelected" , -1)) }
				
				//reset
				val reset = remember { mutableStateOf(false) }

				//if user leaves tis activity or the app, the selected item and indexSelected will be saved
				//buit if the count is 0, the selected item and indexSelected will be reset
				LaunchedEffect(key1 = selected.value, key2 = indexSelected.value, key3 = reset.value) {
					//if the count is 0, then the selected item will be reset
					if (count.value == 0 && reset.value)
					{
						selected.value = false
						indexSelected.value = -1
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
					if (indexSelected.value != -1)
					{
						listState.animateScrollToItem(indexSelected.value)
					}
				}

				//the state of the lazy column, it should scroll to the item where selected is true
				//get the arrays
				val englishNames = resources.getStringArray(R.array.tasbeehTransliteration)
				val arabicNames = resources.getStringArray(R.array.tasbeeharabic)
				val translationNames = resources.getStringArray(R.array.tasbeehTranslation)
				BottomSheetScaffold(
						backgroundColor= MaterialTheme.colorScheme.background,
						contentColor = MaterialTheme.colorScheme.onBackground,
						modifier = Modifier
							.shadow(16.dp , CardDefaults.elevatedShape) ,
						topBar = {
							TopAppBar(
									title = { androidx.compose.material3.Text(text = "Tasbih") } ,
									navigationIcon = {
										IconButton(onClick = {
											finish()
										}) {
											Icon(
													imageVector = Icons.Filled.ArrowBack ,
													contentDescription = "Back"
												)
										}
									} ,
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
							LazyColumn(state= listState) {
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
					Counter(vibrator , it , vibrationAllowed , count,reset)
				}
			}
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
						if (indexSelected.value != -1 && indexSelected.value != index)
						{
							Toasty.warning(
									context ,
									"Only one tasbih can be selected at a time" ,
									Toast.LENGTH_SHORT ,
									true
										  ).show()
						}
						else
						{
							//if the item is clicked, it will be highlighted and the and a count will be added to it the index of the item and a boolean to check if the item is highlighted
							selected.value = ! selected.value
							if (selected.value)
							{
								indexSelected.value = index
							}
							else
							{
								indexSelected.value = -1
							}
						}
					} ,
				colors = CardDefaults.elevatedCardColors(
						containerColor = if (index == indexSelected.value)
							MaterialTheme.colorScheme.secondary
						else MaterialTheme.colorScheme.surface
															) ,
					) {
			Column(
					horizontalAlignment = Alignment.Start ,
					verticalArrangement = Arrangement.Center
				  ) {
				if (index == indexSelected.value)
				{
					androidx.compose.material3.Text(
							modifier = Modifier.padding(8.dp) ,
							text = "Count: ${count.value}" ,
							style = MaterialTheme.typography.titleMedium ,
						)
				}
				CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
					androidx.compose.material3.Text(
							text = arabicName ,
							style = MaterialTheme.typography.titleLarge ,
							fontFamily = quranFont ,
							modifier = Modifier
								.padding(4.dp)
								.fillMaxWidth() ,
							color = if (index == indexSelected.value) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
												   )
				}
				androidx.compose.material3.Text(
						modifier = Modifier
							.padding(4.dp)
							.fillMaxWidth() ,
						text = englishName ,
						style = MaterialTheme.typography.titleSmall ,
						color = if (index == indexSelected.value) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
											   )
				androidx.compose.material3.Text(
						modifier = Modifier
							.padding(4.dp)
							.fillMaxWidth() ,
						text = translationName ,
						style = MaterialTheme.typography.titleSmall ,
						color = if (index == indexSelected.value) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
											   )
			}
		}

	}
	@Preview
	@Composable
//tasbih row
	fun TasbihRowPreview() {
		NimazTheme {
			TasbihRow(
					englishName = "Subhan Allah" ,
					arabicName = "سبحان الله" ,
					translationName = "Glory be to Allah" ,
					count = remember { mutableStateOf(0) } ,
					selected = remember { mutableStateOf(false) } ,
					index = 0 ,
					indexSelected = remember { mutableStateOf(0) } ,
					 )
		}
	}
}