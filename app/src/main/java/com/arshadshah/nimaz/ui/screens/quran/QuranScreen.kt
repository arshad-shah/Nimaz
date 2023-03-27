package com.arshadshah.nimaz.ui.screens.quran

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_QURAN
import com.arshadshah.nimaz.data.remote.viewModel.QuranViewModel
import com.arshadshah.nimaz.ui.components.bLogic.quran.JuzList
import com.arshadshah.nimaz.ui.components.bLogic.quran.SurahList
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuranScreen(
	paddingValues : PaddingValues ,
	onNavigateToAyatScreen : (String , Boolean , String , Int?) -> Unit ,
			   )
{
	val context = LocalContext.current
	val viewModel = viewModel(
			key = "QuranViewModel" ,
			initializer = { QuranViewModel(context) } ,
			viewModelStoreOwner = context as ComponentActivity
							 )

	viewModel.handleQuranMenuEvents(QuranViewModel.QuranMenuEvents.Initialize_Quran)

	val titles = listOf("Sura" , "Juz" , "My Quran")
	val pagerState = rememberPagerState()
	val scope = rememberCoroutineScope()
	val transition = updateTransition(pagerState.currentPage , label = "quranTabTransition")
	Column(
			modifier = Modifier
				.padding(paddingValues)
				.testTag(TEST_TAG_QURAN)
		  ) {

		TabRow(
				selectedTabIndex = pagerState.currentPage,
				modifier = Modifier
					.padding(vertical = 4.dp, horizontal = 4.dp)
					.clip(RoundedCornerShape(50))
					.padding(1.dp),
				containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
				indicator = { tabPositions: List<TabPosition> ->
					val indicatorStart by transition.animateDp(
							transitionSpec = {
								// Handle directionality here, if we are moving to the right, we
								// want the right side of the indicator to move faster, if we are
								// moving to the left, we want the left side to move faster.
								if (initialState < targetState) {
									spring(dampingRatio = 1f, stiffness = 50f)
								} else {
									spring(dampingRatio = 1f, stiffness = 1000f)
								}
							} , label = "quranTabTransitionStart"
															  ) {
						tabPositions[it].left
					}

					val indicatorEnd by transition.animateDp(
							transitionSpec = {
								// Handle directionality here, if we are moving to the right, we
								// want the right side of the indicator to move faster, if we are
								// moving to the left, we want the left side to move faster.
								if (initialState < targetState) {
									spring(dampingRatio = 1f, stiffness = 1000f)
								} else {
									spring(dampingRatio = 1f, stiffness = 50f)
								}
							} , label = "quranTabTransitionEnd"
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
				},
				divider = { }
			  ) {
			titles.forEachIndexed { index , title ->
				val selected = pagerState.currentPage == index
				Tab(
						modifier =
						if (selected) Modifier
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
									)
						,
						selected = pagerState.currentPage == index,
						onClick = {
							scope.launch {
								pagerState.animateScrollToPage(index)
							}
						} ,
						text = {
							Text(
									text = title ,
									maxLines = 1 ,
									overflow = TextOverflow.Ellipsis ,
									style = MaterialTheme.typography.titleSmall,
									color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
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
					Log.d(AppConstants.QURAN_SURAH_SCREEN_TAG , "Surah Screen")
					val surahListState = remember { viewModel.surahListState }.collectAsState()
					val isLoadingSurah = remember { viewModel.loadingState }.collectAsState()
					val errorSurah = remember { viewModel.errorState }.collectAsState()
					Log.d(
							AppConstants.QURAN_SURAH_SCREEN_TAG ,
							"surahListState.value = ${surahListState.value}"
						 )
					SurahList(
							onNavigateToAyatScreen = onNavigateToAyatScreen ,
							state = surahListState ,
							loading = isLoadingSurah.value ,
							error = errorSurah.value
							 )
				}

				1 ->
				{
					Log.d(AppConstants.QURAN_JUZ_SCREEN_TAG , "Juz Screen")
					val juzListState = remember { viewModel.juzListState }.collectAsState()
					val isLoadingJuz = remember { viewModel.loadingState }.collectAsState()
					val errorJuz = remember { viewModel.errorState }.collectAsState()
					Log.d(
							AppConstants.QURAN_JUZ_SCREEN_TAG ,
							"juzListState.value = ${juzListState.value}"
						 )
					JuzList(
							onNavigateToAyatScreen = onNavigateToAyatScreen ,
							state = juzListState ,
							loading = isLoadingJuz.value ,
							error = errorJuz.value
						   )
				}

				2 ->
				{
					val bookmarks = remember { viewModel.bookmarks }.collectAsState()
					val favorites = remember { viewModel.favorites }.collectAsState()
					val notes = remember { viewModel.notes }.collectAsState()
					MyQuranScreen(
							bookmarks = bookmarks ,
							favorites = favorites ,
							notes = notes ,
							onNavigateToAyatScreen = onNavigateToAyatScreen ,
							handleEvents = viewModel::handleAyaEvent
								 )
				}
			}
		}
	}
}