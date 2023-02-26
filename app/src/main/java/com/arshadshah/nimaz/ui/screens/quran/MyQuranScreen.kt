package com.arshadshah.nimaz.ui.screens.quran

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.models.Aya
import com.arshadshah.nimaz.data.remote.viewModel.QuranViewModel
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import kotlin.reflect.KFunction1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyQuranScreen(
	bookmarks : State<List<Aya>> ,
	favorites : State<List<Aya>> ,
	notes : State<List<Aya>> ,
	onNavigateToAyatScreen : (String , Boolean , String , Int?) -> Unit ,
	handleEvents : KFunction1<QuranViewModel.AyaEvent , Unit> ,
				 )
{
	//execute the code below when the screen is loaded
	LaunchedEffect(Unit)
	{
		handleEvents(QuranViewModel.AyaEvent.getBookmarks)
		handleEvents(QuranViewModel.AyaEvent.getFavorites)
		handleEvents(QuranViewModel.AyaEvent.getNotes)
	}

	val bookMarkDropDown = remember { mutableStateOf(false) }
	val favoriteDropDown = remember { mutableStateOf(false) }
	val noteDropDown = remember { mutableStateOf(false) }

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

	LazyColumn(
			userScrollEnabled =true,
			  ) {
		item {
			FeaturesDropDown(
					nameOfFeature = "Bookmarks" ,
					items = bookmarks ,
					language = translation ,
					onNavigateToAyatScreen = onNavigateToAyatScreen
							)
		}
		item {
			FeaturesDropDown(
					nameOfFeature = "Favorites" ,
					items = favorites ,
					language = translation ,
					onNavigateToAyatScreen = onNavigateToAyatScreen
							)
		}
		item {
			FeaturesDropDown(
					nameOfFeature = "Notes" ,
					items = notes ,
					language = translation ,
					onNavigateToAyatScreen = onNavigateToAyatScreen
							)
		}
	}
}

//a dropdown that shows the bookmarks that are saved in the database
//it has a placeholder when there are no bookmarks
//it has a icon at the end that shows a dropdown menu
//the icon changes when the menu is expanded
//the menu has a list of bookmarks
//when a bookmark is clicked it navigates to the ayat screen
//the bookmark is highlighted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeaturesDropDown(
	items : State<List<Aya>> ,
	language : String ,
	onNavigateToAyatScreen : (String , Boolean , String , Int?) -> Unit ,
	nameOfFeature : String ,
						)
{
	val isExpanded = remember { mutableStateOf(false) }

	//the icon that is shown in the dropdown
	val icon = when (isExpanded.value)
	{
		true -> Icons.Filled.KeyboardArrowUp
		false -> Icons.Filled.KeyboardArrowDown
	}

	//the list of bookmarks
	val list = items.value

	ElevatedCard(
			modifier = Modifier
				.padding(8.dp)
				) {

			//an elevation card that shows the text and icon
			ElevatedCard(
					modifier = Modifier
						.padding(4.dp)
						.fillMaxWidth()
						.clickable {
							isExpanded.value = ! isExpanded.value
						},
					shape = MaterialTheme.shapes.medium ,
					content = {
						Row(
								modifier = Modifier
									.padding(8.dp)
									.fillMaxWidth(),
								verticalAlignment = Alignment.CenterVertically ,
								horizontalArrangement = Arrangement.SpaceBetween
						   ) {
							Row(
									modifier = Modifier
										.fillMaxWidth(0.9f) ,
									verticalAlignment = Alignment.CenterVertically ,
									horizontalArrangement = Arrangement.Start
							   ){
								//the text
								Text(
										modifier = Modifier
											.padding(8.dp) ,
										text = nameOfFeature ,
										textAlign = TextAlign.Start ,
										maxLines = 2 ,
										overflow = TextOverflow.Ellipsis ,
										style = MaterialTheme.typography.bodyLarge
									)
								//a bubble that shows the number of features
								//if the list is empty then the bubble is not shown
								if (list.isNotEmpty())
								{
									Badge()
									{
										Text(
												text = list.size.toString() ,
												style = MaterialTheme.typography.bodyLarge
											)
									}
								}
							}
							//the icon
							Icon(
									imageVector = icon ,
									contentDescription = "dropdown icon" ,
									modifier = Modifier
										.padding(8.dp)
								)
						}
					}
						)

			//when the card is clicked show the dropdown menu
			//the menu has a list of bookmarks
			//when a bookmark is clicked it navigates to the ayat screen
			//the bookmark is highlighted
			if(isExpanded.value)
			{
				//if the list is empty show a placeholder
				if(list.isEmpty()){
					Placeholder(nameOfDropdown = nameOfFeature)
				}
				else{
					for (i in list.indices)
					{
						//a card that shows the bookmark
						ElevatedCard(
								modifier = Modifier
									.padding(bottom = 8.dp , start = 8.dp , end = 8.dp)
									.fillMaxWidth()
									.clickable {
										onNavigateToAyatScreen(
												list[i].suraNumber.toString() ,
												true ,
												language,
												list[i].ayaNumberInSurah
															  )
									},
								shape = MaterialTheme.shapes.medium ,
								content = {
									Row(
											modifier = Modifier
												.padding(8.dp)
												.fillMaxWidth(),
											verticalAlignment = Alignment.CenterVertically ,
											horizontalArrangement = Arrangement.SpaceBetween
									   ) {
										//the text
										Text(
												modifier = Modifier
													.padding(8.dp) ,
												text = "Chapter " + list[i].suraNumber.toString() + ":" + "Verse " + list[i].ayaNumber.toString() ,
												textAlign = TextAlign.Start ,
												maxLines = 2 ,
												overflow = TextOverflow.Ellipsis ,
												style = MaterialTheme.typography.bodyLarge
											)
										//the icon
										Icon(
												painter = painterResource(id = R.drawable.angle_small_right_icon) ,
												contentDescription = "Navigate to ayat screen" ,
												modifier = Modifier
													.padding(8.dp)
													.size(24.dp)
											)
									}
								}
									)
					}
				}
			}
	}
}

//a composable that shows a placeholder when the data is not available in the database
@Composable
fun Placeholder(nameOfDropdown : String)
{
	ElevatedCard(
			modifier = Modifier
				.padding(8.dp)
				.fillMaxWidth(),
			shape = MaterialTheme.shapes.medium ,
			content = {
				Column(
						modifier = Modifier
							.padding(8.dp),
						verticalArrangement = Arrangement.Center ,
						horizontalAlignment = Alignment.CenterHorizontally
					  ) {
					Text(
							text = "No $nameOfDropdown available" ,
							maxLines = 2 ,
							modifier = Modifier
								.padding(8.dp)
								.fillMaxWidth() ,
							textAlign = TextAlign.Center ,
							color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) ,
							overflow = TextOverflow.Ellipsis ,
							style = MaterialTheme.typography.bodyLarge
						)
				}
			}
				)
}


//FeaturesDropDown preview
@Preview
@Composable
fun FeaturesDropDownPreview()
{
	//a dummy list of ayas
	//aya
	//al ayaNumberInQuran: Int,
	//    val ayaNumber: Int,
	//    val ayaArabic: String,
	//    val ayaTranslationEnglish: String,
	//    val ayaTranslationUrdu: String,
	//    val suraNumber: Int,
	//    val ayaNumberInSurah: Int,
	//    val bookmark: Boolean,
	//    val favorite: Boolean,
	//    val note: String,
	//    val audioFileLocation: String,
	//    val sajda: Boolean,
	//    val sajdaType: String,
	//    val ruku: Int,
	//    val juzNumber: Int,
val ayas = listOf(
			Aya(
					ayaNumberInQuran = 1 ,
					ayaNumber = 1 ,
					ayaArabic = "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ" ,
					ayaTranslationEnglish = "In the name of Allah, the Entirely Merciful, the Especially Merciful." ,
					ayaTranslationUrdu = "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ" ,
					suraNumber = 1 ,
					ayaNumberInSurah = 1 ,
					bookmark = false ,
					favorite = false ,
					note = "" ,
					audioFileLocation = "" ,
					sajda = false ,
					sajdaType = "" ,
					ruku = 1 ,
					juzNumber = 1
					) ,
				 )
	NimazTheme {
		FeaturesDropDown(
				items = remember { mutableStateOf(ayas) } ,
				language = "english" ,
				onNavigateToAyatScreen = { _ , _ , _ , _ -> } ,
				nameOfFeature = "Notes"
						)
	}
}

//a preview of the placeholder
@Preview
@Composable
fun PlaceholderPreview()
{
	NimazTheme {
		Placeholder(nameOfDropdown = "Notes")
	}
}