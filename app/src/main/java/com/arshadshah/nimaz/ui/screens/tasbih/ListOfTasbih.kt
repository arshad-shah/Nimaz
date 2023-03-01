package com.arshadshah.nimaz.ui.screens.tasbih

import android.content.res.Configuration
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.viewModel.TasbihViewModel
import com.arshadshah.nimaz.ui.components.bLogic.tasbih.TasbihRow
import com.arshadshah.nimaz.ui.components.ui.FeatureDropdownItem
import com.arshadshah.nimaz.ui.components.ui.FeaturesDropDown
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ListOfTasbih(
	paddingValues : PaddingValues ,
	onNavigateToTasbihScreen : (String , String , String) -> Unit
				)
{
	val resources = LocalContext.current.resources
	val context = LocalContext.current
	val viewModel = viewModel(key = "TasbihViewModel", initializer = { TasbihViewModel(context) }, viewModelStoreOwner = LocalContext.current as ComponentActivity)
	val sharedPref = context.getSharedPreferences("tasbih" , 0)
	val selected =
		remember { mutableStateOf(sharedPref.getBoolean("selected" , false)) }
	val indexSelected =
		remember { mutableStateOf(sharedPref.getInt("indexSelected" , - 1)) }
	//if user leaves tis activity or the app, the selected item and indexSelected will be saved
	//buit if the count is 0, the selected item and indexSelected will be reset
	LaunchedEffect(
			key1 = selected.value ,
			key2 = indexSelected.value ,
				  ) {
			sharedPref.edit().putBoolean("selected" , selected.value).apply()
			sharedPref.edit().putInt("indexSelected" , indexSelected.value).apply()

	}

	//if a new item is selected, then scroll to that item
	val listState = rememberLazyListState()
	LaunchedEffect(key1 = indexSelected.value) {
		if (indexSelected.value != - 1)
		{
			listState.animateScrollToItem(indexSelected.value)
		}
	}

	//the state of the lazy column, it should scroll to the item where selected is true
	//get the arrays
	val englishNames = resources.getStringArray(R.array.tasbeehTransliteration)
	val arabicNames = resources.getStringArray(R.array.tasbeeharabic)
	val translationNames = resources.getStringArray(R.array.tasbeehTranslation)


	val (selectedTab , setSelectedTab) = rememberSaveable { mutableStateOf(0) }
	val titles = listOf("Tasbih List" , "My Tasbih")
	Column(modifier = Modifier
		.padding(paddingValues)
		.testTag(AppConstants.TEST_TAG_QURAN)) {

		TabRow(selectedTabIndex = selectedTab) {
			titles.forEachIndexed { index , title ->
				Tab(
						modifier = Modifier.testTag(
								AppConstants.TEST_TAG_QURAN_TAB.replace(
										"{number}" ,
										index.toString()
																	   )
												   ) ,
						selected = selectedTab == index ,
						onClick = { setSelectedTab(index) } ,
						text = {
							Text(
									text = title ,
									maxLines = 2 ,
									overflow = TextOverflow.Ellipsis ,
									style = MaterialTheme.typography.titleSmall
								)
						}
				   )
			}
		}


		when (selectedTab)
		{
			0 ->
			{
				LazyColumn(
						modifier = Modifier.testTag(AppConstants.TEST_TAG_TASBIH_LIST) ,
						state = listState ,
						  ) {
					items(englishNames.size) { index ->
						TasbihRow(
								englishNames[index] ,
								arabicNames[index] ,
								translationNames[index] ,
								onNavigateToTasbihScreen
								 )
					}
				}
			}
			1 ->
			{
				viewModel.handleEvent(TasbihViewModel.TasbihEvent.GetAllTasbih)
				val listOfTasbih = remember {
					viewModel.tasbihList
				}.collectAsState()
				//if the list is empty, show a message
				if (listOfTasbih.value.isEmpty())
				{
					Box(
							modifier = Modifier
								.fillMaxSize()
								.background(MaterialTheme.colorScheme.surface)
					   ) {
						Text(
								text = "No Tasbih Added" ,
								textAlign = TextAlign.Center ,
								modifier = Modifier
									.fillMaxWidth()
									.align(Alignment.Center)
							)
					}
				} else
				{
					//if the list is not empty, show the list

					LazyColumn(
							userScrollEnabled = true ,
							  ) {
						//extract the dates from the list of tasbih
						val dates = listOfTasbih.value.map { tasbih ->
							tasbih.date
						}.distinct()

						//check how many tasbih are there for each date
						val tasbihCountForDate = dates.map { date ->
							listOfTasbih.value.filter { tasbih ->
								tasbih.date == date
							}.size
						}
						//create a list of tasbih for each date
						val tasbihForDate = dates.mapIndexed { index , date ->
							listOfTasbih.value.filterIndexed { tasbihIndex , tasbih ->
								tasbihIndex < tasbihCountForDate[index] && tasbih.date == date
							}
						}
						//log everything
						Log.d("Tasbih" , "dates: $dates")
						Log.d("Tasbih" , "tasbihCountForDate: $tasbihCountForDate")
						Log.d("Tasbih" , "tasbihForDate: $tasbihForDate")

						//for each date, render the tasbih drop down
						for (index in dates.indices)
						{
							item {
								FeaturesDropDown(
										header = {
											DropDownHeader(
													headerLeft = "Name" ,
													headerRight = "Count" ,
													headerMiddle = "Goal"
														  )
										} ,
										//the list of tasbih for the date at the index
										items = listOfTasbih.value.filter { tasbih ->
											tasbih.date == dates[index]
										} ,
										label = "Tasbih for " + LocalDate.parse(dates[index])
											.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")) ,
										dropDownItem = {
											FeatureDropdownItem(
													item = it ,
													onClick = { tasbih ->
														onNavigateToTasbihScreen(
																tasbih.englishName ,
																tasbih.arabicName ,
																tasbih.translationName
																				)
													} ,
													itemContent = { tasbih ->
														Text(
																modifier = Modifier
																	.padding(8.dp) ,
																text = tasbih.englishName + " - " + tasbih.goal + " - " + tasbih.count ,
																textAlign = TextAlign.Start ,
																maxLines = 2 ,
																overflow = TextOverflow.Ellipsis ,
																style = MaterialTheme.typography.bodyLarge
															)
													} ,
															   )
										}
												)
							}
						}
					}
				}
			}
		}
	}
}

//my tasbih drop down item for each tasbih
@Composable
fun DropDownHeader(headerLeft : String , headerMiddle : String , headerRight : String)
{
	// a three section row for the tasbih name, goal and count
	//divider between each section
	//should look like this
	//Name - Goal - Count

	Row(
			modifier = Modifier
				.fillMaxWidth()
				.background(MaterialTheme.colorScheme.surface),
			verticalAlignment = Alignment.CenterVertically
		  ) {
		//name
		Text(
				modifier = Modifier
					.weight(1f)
					.padding(8.dp) ,
				text = headerLeft ,
				textAlign = TextAlign.Center ,
				maxLines = 2 ,
				overflow = TextOverflow.Ellipsis ,
				style = MaterialTheme.typography.bodyLarge
			)
		//divider
		Divider(
				modifier = Modifier.width(1.dp).height(24.dp) ,
				color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f) ,
				thickness = 1.dp ,
			   )
		//goal
		Text(
				modifier = Modifier
					.weight(1f)
					.padding(8.dp) ,
				text = headerMiddle ,
				textAlign = TextAlign.Center ,
				maxLines = 2 ,
				overflow = TextOverflow.Ellipsis ,
				style = MaterialTheme.typography.bodyLarge
			)
		//divider
		Divider(
				modifier = Modifier.width(1.dp).height(24.dp) ,
				color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f) ,
				thickness = 1.dp ,
			   )
		//count
		Text(
				modifier = Modifier
					.weight(1f)
					.padding(8.dp) ,
				text = headerRight ,
				textAlign = TextAlign.Center ,
				maxLines = 2 ,
				overflow = TextOverflow.Ellipsis ,
				style = MaterialTheme.typography.bodyLarge
			)
	}
}

@Preview(showBackground = true ,
		 uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
		)
@Composable
//MyTasbihDropDownItem
fun DefaultPreview() {
	DropDownHeader(headerLeft = "Name" , headerMiddle = "Goal" , headerRight = "Count")
}
