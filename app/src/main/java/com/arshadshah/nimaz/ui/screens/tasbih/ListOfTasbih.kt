package com.arshadshah.nimaz.ui.screens.tasbih

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.ui.components.bLogic.tasbih.TasbihRow

@Composable
fun ListOfTasbih(
	paddingValues : PaddingValues ,
	onNavigateToTasbihScreen : (String , String , String) -> Unit
				)
{
	val resources = LocalContext.current.resources
	val context = LocalContext.current
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
						contentPadding = paddingValues
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
				LazyColumn(
						modifier = Modifier.testTag(AppConstants.TEST_TAG_TASBIH_LIST) ,
						state = listState ,
						contentPadding = paddingValues
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
		}
	}
}