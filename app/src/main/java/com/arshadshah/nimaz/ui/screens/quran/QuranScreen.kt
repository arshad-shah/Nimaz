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
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.QURAN_VIEWMODEL_KEY
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_QURAN
import com.arshadshah.nimaz.ui.components.quran.JuzList
import com.arshadshah.nimaz.ui.components.quran.SurahList
import com.arshadshah.nimaz.viewModel.QuranViewModel
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
			 key = QURAN_VIEWMODEL_KEY ,
			 initializer = { QuranViewModel(context) } ,
			 viewModelStoreOwner = context as ComponentActivity
							 )

	viewModel.handleQuranMenuEvents(QuranViewModel.QuranMenuEvents.Initialize_Quran)

	val titles = listOf("Sura" , "Juz" , "My Quran")
	val pagerState = rememberPagerState(
			 initialPage = 0 ,
			 initialPageOffsetFraction = 0F ,
									   ) {
		titles.size
	}
	val scope = rememberCoroutineScope()
	Column(
			 modifier = Modifier
				 .padding(paddingValues)
				 .testTag(TEST_TAG_QURAN)
		  ) {

		TabRow(
				 selectedTabIndex = pagerState.currentPage ,
				 modifier = Modifier
					 .padding(vertical = 4.dp , horizontal = 4.dp)
					 .clip(MaterialTheme.shapes.extraLarge) ,
				 containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f) ,
				 indicator = { tabPositions : List<TabPosition> ->
					 val transition = updateTransition(pagerState.currentPage , label = "")
					 val indicatorStart by transition.animateDp(
							  transitionSpec = {
								  if (initialState < targetState)
								  {
									  spring(dampingRatio = 1f , stiffness = 50f)
								  } else
								  {
									  spring(dampingRatio = 1f , stiffness = 1000f)
								  }
							  } , label = ""
															   ) {
						 tabPositions[it].left
					 }

					 val indicatorEnd by transition.animateDp(
							  transitionSpec = {
								  if (initialState < targetState)
								  {
									  spring(dampingRatio = 1f , stiffness = 1000f)
								  } else
								  {
									  spring(dampingRatio = 1f , stiffness = 50f)
								  }
							  } , label = ""
															 ) {
						 tabPositions[it].right
					 }

					 Box(
							  Modifier
								  .offset(x = indicatorStart)
								  .wrapContentSize(align = Alignment.BottomStart)
								  .width(indicatorEnd - indicatorStart)
								  .padding(4.dp)
								  .fillMaxSize()
								  .background(
										   color = MaterialTheme.colorScheme.secondaryContainer ,
										   MaterialTheme.shapes.extraLarge
											 )
								  .zIndex(1f)
						)
				 } ,
				 divider = { }
			  ) {
			titles.forEachIndexed { index , title ->
				val selected = pagerState.currentPage == index
				Tab(
						 modifier = Modifier
							 .zIndex(2f)
							 .clip(MaterialTheme.shapes.extraLarge)
							 .testTag(
									  AppConstants.TEST_TAG_QURAN_TAB.replace(
											   "{number}" ,
											   index.toString()
																			 )
									 ) ,
						 selectedContentColor = MaterialTheme.colorScheme.onSecondaryContainer ,
						 unselectedContentColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(
								  alpha = 0.6f
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
									  maxLines = 1 ,
									  overflow = TextOverflow.Ellipsis ,
									  style = MaterialTheme.typography.titleMedium ,
									  fontWeight = if (selected) FontWeight.ExtraBold
									  else FontWeight.Normal ,
									  color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
									  else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
								 )
						 }
				   )
			}
		}
		HorizontalPager(
				 pageSize = PageSize.Fill ,
				 state = pagerState ,
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