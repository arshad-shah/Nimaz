package com.arshadshah.nimaz.ui.components.ui.dashboard

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.data.remote.models.Tasbih
import com.arshadshah.nimaz.data.remote.viewModel.TasbihViewModel
import com.arshadshah.nimaz.ui.components.bLogic.tasbih.DeleteDialog
import com.arshadshah.nimaz.ui.components.ui.FeaturesDropDown
import com.arshadshah.nimaz.ui.components.ui.trackers.*
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardTasbihTracker(
	onNavigateToTasbihScreen : (String , String , String , String) -> Unit ,
	onNavigateToTasbihListScreen : () -> Unit ,
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
		viewModel.handleEvent(
				TasbihViewModel.TasbihEvent.RecreateTasbih(
						LocalDate.now().toString()
														  )
							 )
	}
	val listOfTasbih = remember {
		viewModel.tasbihList
	}.collectAsState()

	if (listOfTasbih.value.isEmpty())
	{
		Box(
				modifier = Modifier.clickable {
					onNavigateToTasbihListScreen()
				}
		   ) {
			Placeholder(nameOfDropdown = "Tasbih")
		}
	} else
	{
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
				items = listOfTasbih.value ,
				label = "Tasbih" ,
				dropDownItem = {
					TasbihDropdownItem(
							it ,
							onClick = { tasbih ->
								onNavigateToTasbihScreen(
										tasbih.id.toString() ,
										tasbih.arabicName ,
										tasbih.englishName ,
										tasbih.translationName
														)
							} ,
							onDelete = { tasbih ->
								showDeleteDialog.value = true
								tasbihToEdit.value = tasbih
							} ,
							onEdit = { tasbih ->
								showTasbihDialog.value =
									true
								tasbihToEdit.value =
									tasbih
							})
				}

						)

		GoalEditDialog(tasbihToEdit.value , showTasbihDialog)
		DeleteDialog(tasbihToEdit.value , showDeleteDialog)
	}
}