package com.arshadshah.nimaz.ui.components.ui.dashboard

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.data.remote.models.Tasbih
import com.arshadshah.nimaz.data.remote.viewModel.TasbihViewModel
import com.arshadshah.nimaz.ui.components.bLogic.tasbih.DeleteDialog
import com.arshadshah.nimaz.ui.components.ui.FeaturesDropDown
import com.arshadshah.nimaz.ui.components.ui.trackers.DropDownHeader
import com.arshadshah.nimaz.ui.components.ui.trackers.GoalEditDialog
import com.arshadshah.nimaz.ui.components.ui.trackers.TasbihDropdownItem
import java.time.LocalDate

@Composable
fun DashboardTasbihTracker(
	onNavigateToTasbihScreen : (String , String , String , String) -> Unit ,
	onNavigateToTasbihListScreen : () -> Unit
						  )
{
	val context = LocalContext.current
	val viewModel = viewModel(
			key = "TasbihViewModel" ,
			initializer = { TasbihViewModel(context) } ,
			viewModelStoreOwner = LocalContext.current as ComponentActivity
							 )
	//run only once
	LaunchedEffect(key1 = true) {
		viewModel.handleEvent(TasbihViewModel.TasbihEvent.RecreateTasbih(LocalDate.now().toString()))
	}
	val listOfTasbih = remember {
		viewModel.tasbihList
	}.collectAsState()

	if(listOfTasbih.value.isEmpty())
	{
		//a message to the user that there are no tasbih for the day
		ElevatedCard(
				modifier = Modifier
					.padding(8.dp)
					.fillMaxWidth()
					.clickable {
						onNavigateToTasbihListScreen()
					}
					) {
			Text(
					text = "No Tasbih set for today" ,
					modifier = Modifier
						.padding(8.dp)
						.fillMaxWidth() ,
					textAlign = TextAlign.Center ,
					style = MaterialTheme.typography.titleMedium
				)
			Spacer(modifier = Modifier.height(8.dp))
			Text(
					text = "click here to add a tasbih" ,
					modifier = Modifier
						.padding(8.dp)
						.fillMaxWidth() ,
					textAlign = TextAlign.Center ,
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
				)
		}
	}else{
		val showTasbihDialog = remember {
			mutableStateOf(false)
		}
		val showDeleteDialog = remember {
			mutableStateOf(false)
		}
		val tasbihToEdit = remember {
			mutableStateOf(
					Tasbih(
							0 ,
							"" ,
							"" ,
							"" ,
							"" ,
							0 ,
							0 ,
						  )
						  )
		}

		FeaturesDropDown(
				header = {
					DropDownHeader(
							headerLeft = "Name" ,
							headerRight = "Count" ,
							headerMiddle = "Goal"
								  )
				} ,
				//the list of tasbih for the date at the index
				items = listOfTasbih.value,
				label = "Tasbih" ,
				dropDownItem = {
					TasbihDropdownItem(
							it ,
							onClick = {tasbih ->
								onNavigateToTasbihScreen(
										tasbih.id.toString() ,
										tasbih.arabicName ,
										tasbih.englishName ,
										tasbih.translationName
														)
							},
							onDelete = { tasbih ->
								showDeleteDialog.value = true
								tasbihToEdit.value = tasbih
							},
							onEdit = { tasbih ->
								showTasbihDialog.value = true
								tasbihToEdit.value = tasbih
							} ,
									  )
				}

						)

		GoalEditDialog(tasbihToEdit.value, showTasbihDialog)
		DeleteDialog(tasbihToEdit.value, showDeleteDialog)
	}
}