package com.arshadshah.nimaz.ui.screens.tasbih

import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.models.Tasbih
import com.arshadshah.nimaz.data.remote.viewModel.TasbihViewModel
import com.arshadshah.nimaz.ui.components.bLogic.tasbih.DeleteDialog
import com.arshadshah.nimaz.ui.components.bLogic.tasbih.TasbihRow
import com.arshadshah.nimaz.ui.components.ui.FeaturesDropDown
import com.arshadshah.nimaz.ui.components.ui.trackers.DropDownHeader
import com.arshadshah.nimaz.ui.components.ui.trackers.GoalEditDialog
import com.arshadshah.nimaz.ui.components.ui.trackers.TasbihDropdownItem
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterialApi::class , ExperimentalFoundationApi::class)
@Composable
fun ListOfTasbih(
	paddingValues : PaddingValues ,
	onNavigateToTasbihScreen : (String , String , String , String) -> Unit ,
				)
{
	val resources = LocalContext.current.resources
	val context = LocalContext.current

	val viewModel = viewModel(
			key = "TasbihViewModel" ,
			initializer = { TasbihViewModel(context) } ,
			viewModelStoreOwner = LocalContext.current as ComponentActivity
							 )
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

	val titles = listOf("Tasbih List" , "My Tasbih")
	val pagerState = rememberPagerState()
	val scope = rememberCoroutineScope()
	val transition = updateTransition(pagerState.currentPage , label = "tasbihTabTransition")
	Column(
			modifier = Modifier
				.padding(paddingValues)
				.testTag(AppConstants.TEST_TAG_QURAN)
		  ) {

		TabRow(
				selectedTabIndex = pagerState.currentPage ,
				modifier = Modifier
					.padding(vertical = 4.dp , horizontal = 4.dp)
					.clip(RoundedCornerShape(50))
					.padding(1.dp) ,
				containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f) ,
				indicator = { tabPositions : List<TabPosition> ->
					val indicatorStart by transition.animateDp(
							transitionSpec = {
								// Handle directionality here, if we are moving to the right, we
								// want the right side of the indicator to move faster, if we are
								// moving to the left, we want the left side to move faster.
								if (initialState < targetState)
								{
									spring(dampingRatio = 1f , stiffness = 50f)
								} else
								{
									spring(dampingRatio = 1f , stiffness = 1000f)
								}
							} , label = "tasbihTabTransitionStart"
															  ) {
						tabPositions[it].left
					}

					val indicatorEnd by transition.animateDp(
							transitionSpec = {
								// Handle directionality here, if we are moving to the right, we
								// want the right side of the indicator to move faster, if we are
								// moving to the left, we want the left side to move faster.
								if (initialState < targetState)
								{
									spring(dampingRatio = 1f , stiffness = 1000f)
								} else
								{
									spring(dampingRatio = 1f , stiffness = 50f)
								}
							} , label = "tasbihTabTransitionEnd"
															) {
						tabPositions[it].right
					}
					Box(
							modifier = Modifier
								// Apply an offset from the start to correctly position the indicator around the tab
								.offset(x = indicatorStart)
								// Make the width of the indicator follow the animated width as we move between tabs
								.width(indicatorEnd - indicatorStart)
					   ) {}
				} ,
				divider = { }
			  ) {
			titles.forEachIndexed { index , title ->
				val selectedTabIndx = pagerState.currentPage == index
				Tab(
						modifier = if (selectedTabIndx) Modifier
							.clip(RoundedCornerShape(50))
							.background(MaterialTheme.colorScheme.secondaryContainer)
							.testTag(
									AppConstants.TEST_TAG_QURAN_TAB.replace(
											"{number}" ,
											index.toString()
																		   )
									)
						else Modifier
							.clip(RoundedCornerShape(50))
							.testTag(
									AppConstants.TEST_TAG_QURAN_TAB.replace(
											"{number}" ,
											index.toString()
																		   )
									) ,
						selected = pagerState.currentPage == index ,
						onClick = {
							scope.launch {
								pagerState.animateScrollToPage(index)
							}
						} ,
						text = {
							Text(
									text = title ,
									maxLines = 2 ,
									overflow = TextOverflow.Ellipsis ,
									style = MaterialTheme.typography.titleMedium ,
									color = if (selectedTabIndx) MaterialTheme.colorScheme.onSecondaryContainer
									else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
								)
						}
				   )
			}
		}

		HorizontalPager(
				pageCount = titles.size,
				state = pagerState,
					   ) { page ->
		when (page)
		{
			0 ->
			{
				LazyColumn(
						modifier = Modifier.testTag(AppConstants.TEST_TAG_TASBIH_LIST) ,
						state = listState ,
						  ) {
					items(englishNames.size) { index ->
						TasbihRow(
								arabicNames[index] ,
								englishNames[index] ,
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
					LazyColumn(
							modifier = Modifier.fillMaxSize(),
							userScrollEnabled = true ,
							  ) {
						//extract the dates from the list of tasbih
						val dates = listOfTasbih.value.map { tasbih ->
							tasbih.date
						}.distinct()

						//find out what year the dates are in
						val years = dates.map { date ->
							LocalDate.parse(date)
								.format(DateTimeFormatter.ofPattern("YYYY"))
						}.distinct()

						//find out for each year in what month the tasbih are in
						val months = years.map { year ->
							dates.filter { date ->
								LocalDate.parse(date)
									.format(DateTimeFormatter.ofPattern("YYYY")) == year
							}.map { date ->
								LocalDate.parse(date)
									.format(DateTimeFormatter.ofPattern("MMMM"))
							}.distinct()
						}

						//if we have less than a months worth of tasbih, then we don't need to show the year header
						if (dates.size < 20)
						{
							item {
								//for each date in the month, render the date header
								for (dateIndex in dates.indices)
								{
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
												tasbih.date == dates[dateIndex]
											} ,
											label = LocalDate.parse(dates[dateIndex])
												.format(
														DateTimeFormatter.ofPattern(
																"E dd MMMM"
																				   )
													   ) ,
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
														} ,
																  )
											}

													)
								}
							}
						} else
						{
							//for each year, render the year header
							for (index in years.indices)
							{
								item {
									FeaturesDropDown(
											//the list of tasbih for the date at the index
											items = months[index] ,
											label = years[index] ,
											dropDownItem = {
												//for each month in the year, render the month header
												for (monthIndex in months[index].indices)
												{
													FeaturesDropDown(
															//the list of tasbih for the date at the index
															items = dates ,
															label = months[index][monthIndex] ,
															dropDownItem = {
																//for each date in the month, render the date header
																for (dateIndex in dates.indices)
																{
																	if (LocalDate.parse(dates[dateIndex])
																			.format(
																					DateTimeFormatter.ofPattern(
																							"MMMM"
																											   )
																				   ) == months[index][monthIndex]
																		&& LocalDate.parse(dates[dateIndex])
																			.format(
																					DateTimeFormatter.ofPattern(
																							"YYYY"
																											   )
																				   ) == years[index]
																	)
																	{
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
																					tasbih.date == dates[dateIndex]
																				} ,
																				label = LocalDate.parse(
																						dates[dateIndex]
																									   )
																					.format(
																							DateTimeFormatter.ofPattern(
																									"E dd "
																													   )
																						   ) ,
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
																								showDeleteDialog.value =
																									true
																								tasbihToEdit.value =
																									tasbih
																							} ,
																							onEdit = { tasbih ->
																								showTasbihDialog.value =
																									true
																								tasbihToEdit.value =
																									tasbih
																							} ,
																									  )
																				}

																						)
																	}
																}
															}

																	)
												}
											}
													)
								}
							}
						}
					}
					GoalEditDialog(tasbihToEdit.value , showTasbihDialog)
					DeleteDialog(tasbih = tasbihToEdit.value , showDeleteDialog)
				}
			}

		}
	}
		}
	}
@Preview(
		showBackground = true ,
		uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
		)
@Composable
//MyTasbihDropDownItem
fun DefaultPreview()
{
	DropDownHeader(headerLeft = "Name" , headerMiddle = "Goal" , headerRight = "Count")
}
