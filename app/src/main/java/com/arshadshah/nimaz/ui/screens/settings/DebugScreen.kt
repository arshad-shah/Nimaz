package com.arshadshah.nimaz.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.rememberDismissState
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.ui.components.tasbih.SwipeBackground
import com.arshadshah.nimaz.utils.PrivateSharedPreferences

//a screen for showing shared prefs and other debug stuff
@Composable
fun DebugScreen(paddingValues : PaddingValues)
{
	Column(
			 modifier = Modifier
				 .padding(paddingValues)
				 .fillMaxWidth() ,
			 verticalArrangement = Arrangement.Center ,
			 horizontalAlignment = Alignment.CenterHorizontally
		  ) {
		ShowSharedPrefsData()
	}
}

//composable to show the shared prefs data in a nice way
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ShowSharedPrefsData()
{
	val sharedPreferences = PrivateSharedPreferences(LocalContext.current)
	val allDataSaved = sharedPreferences.getAllData()

	ElevatedCard(
			 modifier = Modifier.padding(8.dp) ,
				) {
		Text(
				 text = "Shared Preferences Data" ,
				 style = MaterialTheme.typography.titleMedium ,
				 modifier = Modifier
					 .padding(8.dp)
					 .fillMaxWidth() ,
				 textAlign = TextAlign.Center
			)
		LazyColumn {
			allDataSaved.forEach {
				item {
					val currentItem = rememberUpdatedState(newValue = it)
					val dismissState = rememberDismissState(
							 confirmStateChange = {
								 if (it == DismissValue.DismissedToStart)
								 {
									 sharedPreferences.removeData(currentItem.value.key)
									 true
								 } else if (it == DismissValue.DismissedToEnd)
								 {
									 sharedPreferences.removeData(currentItem.value.key)
									 true
								 }
								 false
							 }
														   )
					SwipeToDismiss(
							 directions = setOf(DismissDirection.EndToStart) ,
							 state = dismissState ,
							 background = {
								 SwipeBackground(dismissState = dismissState)
							 } ,
							 dismissContent = {
								 ShowSharedPrefsDataItem(it.key , it.value.toString())
							 }
								  )
				}
			}
		}
	}

}

//composable to show the shared prefs data in a nice way
//it is the item in the list
@Composable
fun ShowSharedPrefsDataItem(key : String , value : String)
{
	Card(
			 modifier = Modifier
				 .padding(4.dp)
				 .fillMaxWidth() ,
		) {
		Row(
				 modifier = Modifier
					 .padding(4.dp)
					 .fillMaxWidth() ,
				 verticalAlignment = Alignment.CenterVertically ,
				 horizontalArrangement = Arrangement.SpaceBetween
		   ) {
			Text(
					 text = key ,
					 style = MaterialTheme.typography.titleMedium ,
					 modifier = Modifier
						 .padding(8.dp)
						 .weight(0.5f) ,
					 overflow = TextOverflow.Ellipsis ,
					 maxLines = 1
				)
			Text(
					 text = value ,
					 style = MaterialTheme.typography.bodyMedium ,
					 modifier = Modifier
						 .padding(8.dp)
						 .weight(0.5f) ,
					 overflow = TextOverflow.Ellipsis ,
					 maxLines = 1 ,
					 textAlign = TextAlign.End
				)
		}
	}
}