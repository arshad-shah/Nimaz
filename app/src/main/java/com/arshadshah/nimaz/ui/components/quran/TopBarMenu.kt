package com.arshadshah.nimaz.ui.components.ui.quran

import androidx.activity.ComponentActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import com.arshadshah.nimaz.viewModel.QuranViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarMenu(number : Int , isSurah : Boolean)
{

	val context = LocalContext.current
	val viewModel = viewModel(
			key = AppConstants.QURAN_VIEWMODEL_KEY ,
			initializer = { QuranViewModel(context) } ,
			viewModelStoreOwner = context as ComponentActivity
							 )

	val translationType =
		PrivateSharedPreferences(LocalContext.current).getData(
				key = AppConstants.TRANSLATION_LANGUAGE ,
				s = "English"
															  )
	val translation = when (translationType)
	{
		"English" -> "english"
		"Urdu" -> "urdu"
		else -> "english"
	}

	//create a list with numbers from 1 to 114
	//its expensive to create a list every time the composable is recomposed
	//so we use remember to create the list only once
	val surahList = remember { mutableListOf<Int>() }
	val juzList = remember { mutableListOf<Int>() }
	val (selectedSurah , setSelectedSurah) = remember { mutableStateOf(number) }

	//create the list using a coroutine
	val coroutineScope = rememberCoroutineScope()
	LaunchedEffect(Unit) {
		coroutineScope.launch {
			if (isSurah)
			{
				for (i in 1 .. 114)
				{
					surahList.add(i)
				}
			} else
			{
				for (i in 1 .. 30)
				{
					juzList.add(i)
				}
			}
		}
	}

	val label = when (isSurah)
	{
		true -> "Surah"
		false -> "Juz"
	}

	val list = when (isSurah)
	{
		true -> surahList
		false -> juzList
	}

	val expanded = remember { mutableStateOf(false) }
	//size of the main button that opens the dropdown
	ElevatedCard(
			modifier = Modifier
				.width(150.dp)
				.padding(start = 8.dp)
				) {
		//an elevation card that shows the text and icon
		ElevatedCard(
				modifier = Modifier
					.fillMaxWidth()
					.clickable {
						expanded.value = ! expanded.value
					} ,
				shape = MaterialTheme.shapes.medium ,
				content = {
					Row(
							modifier = Modifier.fillMaxWidth() ,
							verticalAlignment = Alignment.CenterVertically ,
							horizontalArrangement = Arrangement.SpaceBetween
					   ) {
						Row(
								modifier = Modifier.padding(horizontal = 4.dp) ,
								verticalAlignment = Alignment.CenterVertically ,
								horizontalArrangement = Arrangement.Start
						   ) {
							//the text
							Text(
									modifier = Modifier
										.padding(8.dp) ,
									text = label ,
									textAlign = TextAlign.Start ,
									maxLines = 2 ,
									overflow = TextOverflow.Ellipsis ,
									style = MaterialTheme.typography.bodyLarge
								)
							Badge(
									containerColor = MaterialTheme.colorScheme.primary ,
									contentColor = MaterialTheme.colorScheme.onPrimary ,
								 )
							{
								Text(
										text = selectedSurah.toString() ,
										style = MaterialTheme.typography.bodyMedium ,
										textAlign = TextAlign.Center ,
									)
							}
						}
						Crossfade(
								targetState = expanded.value ,
								animationSpec = tween(durationMillis = 300)
								 ) { expanded ->
							if (expanded)
							{
								Icon(
										painter = painterResource(id = R.drawable.arrow_up_icon) ,
										contentDescription = "dropdown icon" ,
										modifier = Modifier
											.padding(horizontal = 8.dp)
											.size(18.dp)
									)
							} else
							{
								Icon(
										painter = painterResource(id = R.drawable.arrow_down_icon) ,
										contentDescription = "dropdown icon" ,
										modifier = Modifier
											.padding(horizontal = 8.dp)
											.size(18.dp)
									)
							}
						}
					}
				} ,
					)


		DropdownMenuQuranSection(
				getAllAyats = if (isSurah) viewModel::getAllAyaForSurah else viewModel::getAllAyaForJuz ,
				setSelectedSurah = setSelectedSurah ,
				expanded = expanded ,
				list = list ,
				translation = translation ,
				label = label
								)
	}
}

//drop down menu
@Composable
fun DropdownMenuQuranSection(
	getAllAyats : (Int , String) -> Unit ,
	expanded : MutableState<Boolean> ,
	translation : String ,
	label : String ,
	list : List<Int> ,
	setSelectedSurah : (Int) -> Unit ,
							)
{

	val scope = rememberCoroutineScope()
	val onSelected = { surah : Int ->
		scope.launch {
			setSelectedSurah(surah)
			getAllAyats(surah , translation)
			expanded.value = false
		}
	}
	DropdownMenu(
			modifier = Modifier
				.width(120.dp)
				.height(300.dp) ,
			expanded = expanded.value ,
			onDismissRequest = {
				expanded.value = false
			}) {
		//the list of numbers in a dropdown menu
		list.forEach { surah ->
			DropdownMenuItem(
					modifier = Modifier
						.fillMaxWidth()
						.height(48.dp) ,
					onClick = {
						onSelected(surah)
					} ,
					text = {
						Text(
								text = "$label $surah" ,
								style = MaterialTheme.typography.bodyLarge ,
								textAlign = TextAlign.Center ,
							)
					} ,
							)
		}
	}
}

@Preview(showBackground = true)
@Composable
fun DropdownMenuSurahPreview()
{
	NimazTheme {
		DropdownMenuQuranSection(
				expanded = remember { mutableStateOf(false) } ,
				list = (1 .. 114).toList() ,
				translation = "english" ,
				label = "Surah" ,
				getAllAyats = { _ , _ -> } ,
				setSelectedSurah = { }
								)
	}
}

@Preview
@Composable
fun TopBarSurahMenuPreviewDark()
{
	NimazTheme(darkTheme = true) {
		TopBarMenu(1 , isSurah = true)
	}
}

@Preview
@Composable
fun TopBarSurahMenuPreview()
{
	NimazTheme {
		TopBarMenu(1 , isSurah = true)
	}
}