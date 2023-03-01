package com.arshadshah.nimaz.ui.components.bLogic.tasbih

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.data.remote.models.Tasbih
import com.arshadshah.nimaz.data.remote.viewModel.TasbihViewModel
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont
import java.time.LocalDate


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasbihRow(
	englishName : String ,
	arabicName : String ,
	translationName : String ,
	onNavigateToTasbihScreen : ((String , String , String) -> Unit)? = null
			 )
{
	val context = LocalContext.current
	val viewModel = viewModel(key = "TasbihViewModel", initializer = { TasbihViewModel(context) }, viewModelStoreOwner = LocalContext.current as androidx.activity.ComponentActivity)
	val showTasbihDialog = remember {
		mutableStateOf(false)
	}
	ElevatedCard(
			modifier = Modifier
				.fillMaxWidth()
				.padding(8.dp)
				.clickable(
						//disable it if onNavigateToTasbihScreen has no implementation
						enabled = onNavigateToTasbihScreen != null ,
						  ) {
					if (onNavigateToTasbihScreen != null)
					{
						showTasbihDialog.value = true
					}
				} ,
				) {
		Row(
				modifier = Modifier
					.padding(8.dp)
					.fillMaxWidth()
		   ) {
			Column(
					modifier = Modifier
						.fillMaxWidth()
						.padding(4.dp)
						.weight(0.80f),
					horizontalAlignment = Alignment.Start ,
					verticalArrangement = Arrangement.Center ,
				  ) {
				CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
					Text(
							text = arabicName ,
							style = MaterialTheme.typography.titleLarge ,
							fontSize = 28.sp ,
							fontFamily = utmaniQuranFont ,
							modifier = Modifier
								.padding(4.dp)
								.fillMaxWidth() ,
							color = MaterialTheme.colorScheme.onSurface ,
						)
				}
				Text(
						modifier = Modifier
							.padding(4.dp)
							.fillMaxWidth() ,
						text = englishName ,
						style = MaterialTheme.typography.titleSmall ,
						color = MaterialTheme.colorScheme.onSurface ,
					)
				Text(
						modifier = Modifier
							.padding(4.dp)
							.fillMaxWidth() ,
						text = translationName ,
						style = MaterialTheme.typography.titleSmall ,
						color = MaterialTheme.colorScheme.onSurface ,
					)
			}
			if(onNavigateToTasbihScreen != null)
			{
				Icon(
						painter = painterResource(id = R.drawable.angle_small_right_icon) ,
						contentDescription = "Navigate to chapter" ,
						modifier = Modifier
							.size(24.dp)
							.fillMaxWidth()
							.align(Alignment.CenterVertically)
					)
			}
		}
	}

	if (showTasbihDialog.value)
	{
		val goal = remember {
			mutableStateOf("")
		}
		//an alert dialog with text field to enter the tasbih goal
		AlertDialog(
				onDismissRequest = { showTasbihDialog.value = false } ,
				title = { Text(text = "Set Daily Goal") } ,
				text = {
					Column(
							modifier = Modifier.padding(8.dp) ,
							verticalArrangement = Arrangement.spacedBy(8.dp) ,
							horizontalAlignment = Alignment.CenterHorizontally ,
						  ) {
						OutlinedTextField(
								value = goal.value,
								onValueChange = {
									goal.value = it
								} ,
								label = { Text(text = "Daily Goal") } ,
										 )
					}
				} ,
				confirmButton = {
					Button(
							onClick = {
									viewModel.handleEvent(
											TasbihViewModel.TasbihEvent.SetTasbih(
													Tasbih(
															date = LocalDate.now().toString() ,
															arabicName = arabicName ,
															englishName = englishName ,
															translationName = translationName ,
															goal = goal.value.toInt() ,
															count = 0 ,
														  )
																				 )
														 )
								if (onNavigateToTasbihScreen != null)
								{
									onNavigateToTasbihScreen(arabicName , englishName , translationName)
								}
								showTasbihDialog.value = false
							} ,
							content = { Text(text = "Confirm") } ,
						)
				} ,
				dismissButton = {
					Button(
							onClick = { showTasbihDialog.value = false } ,
							content = { Text(text = "Cancel") } ,
						)
				} ,
			)
	}

}

@Preview
@Composable
fun TasbihRowPreview()
{
	TasbihRow(
			englishName = "Tasbih" ,
			arabicName = "تسبيح" ,
			translationName = "Praise" ,
			onNavigateToTasbihScreen = null
			 )
}