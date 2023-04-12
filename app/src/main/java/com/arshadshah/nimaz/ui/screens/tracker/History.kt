package com.arshadshah.nimaz.ui.screens.tracker

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants.TRACKING_VIEWMODEL_KEY
import com.arshadshah.nimaz.data.remote.models.PrayerTracker
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.utils.LocalDataStore
import com.arshadshah.nimaz.viewModel.TrackerViewModel
import com.kizitonwose.calendar.compose.CalendarLayoutInfo
import com.kizitonwose.calendar.compose.HeatMapCalendar
import com.kizitonwose.calendar.compose.heatmapcalendar.HeatMapCalendarState
import com.kizitonwose.calendar.compose.heatmapcalendar.HeatMapWeek
import com.kizitonwose.calendar.compose.heatmapcalendar.rememberHeatMapCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.kizitonwose.calendar.core.yearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

private enum class Level(val color: Color) {
	Zero(Color(0xFFEBEDF0)),
	One(Color(0xFF9BE9A8)),
	Two(Color(0xFF40C463)),
	Three(Color(0xFF30A14E)),
	Four(Color(0xFF216E3A)),
}

private fun generateRandomData(startDate: LocalDate, endDate: LocalDate): Map<LocalDate, Level> {
	val levels = Level.values()
	return (0.. ChronoUnit.DAYS.between(startDate , endDate))
		.associateTo(hashMapOf()) { count ->
			startDate.plusDays(count) to levels.random()
		}
}

@Composable
fun History()
{
	val viewModel = viewModel(
			key = TRACKING_VIEWMODEL_KEY ,
			initializer = { TrackerViewModel() } ,
			viewModelStoreOwner = LocalContext.current as ComponentActivity
							 )

	LaunchedEffect(key1 = "getTrackerForDate") {
		viewModel.onEvent(TrackerViewModel.TrackerEvent.GET_ALL_TRACKERS)
	}

	val allTracers = remember {
		viewModel.allTrackers
	}.collectAsState()

	//filter the data and get tracker for today
	val trackerForToday = allTracers.value.filter { it.date == LocalDate.now().toString() }
	Log.d("trackerForToday" , trackerForToday.toString())

	Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(start = 6.dp , end = 6.dp , top = 4.dp , bottom = 4.dp) ,
			horizontalAlignment = Alignment.CenterHorizontally
		  ) {
//		if (trackerForToday.isNotEmpty())
//		{
//			ElevatedCard(
//					shape = MaterialTheme.shapes.extraLarge ,
//					modifier = Modifier
//						.padding(8.dp)
//						.fillMaxWidth()
//						) {
//				Row(
//						modifier = Modifier
//							.padding(8.dp)
//							.fillMaxWidth() ,
//						horizontalArrangement = Arrangement.SpaceEvenly ,
//						verticalAlignment = Alignment.CenterVertically
//				   ) {
//					Column {
//						Text(
//								text = "Your Progress Today" ,
//								style = MaterialTheme.typography.titleLarge
//							)
//						Text(
//								text = "${getCompletedPrayers(trackerForToday[0])} of 5 Completed" ,
//								style = MaterialTheme.typography.titleSmall
//							)
//					}
//					ProgressBarCustom(
//							progress = trackerForToday[0].progress.toFloat() ,
//							radius = 50.dp ,
//									 )
//				}
//			}
//		}
		HeatMapCalenderUI()
	}
}

@Composable
fun HeatMapCalenderUI(){
	var refreshKey by remember { mutableStateOf(1) }
	//get the dates in current mont
	val startDate = LocalDate.now().withDayOfMonth(1)
	//last day of the month
	val endDate = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth())
	val data = remember { mutableStateOf<Map<LocalDate, Level>>(emptyMap()) }
	var selection by remember { mutableStateOf<Pair<LocalDate, Level>?>(null) }
	LaunchedEffect(startDate, endDate, refreshKey) {
		selection = null
		data.value = withContext(Dispatchers.IO) {
			generateRandomData(startDate, endDate)
		}
	}
	Column(
			modifier = Modifier
				.fillMaxSize()
				.background(Color.White),
		  ) {
		val state = rememberHeatMapCalendarState(
				startMonth = startDate.yearMonth,
				endMonth = endDate.yearMonth,
				firstVisibleMonth = endDate.yearMonth,
				firstDayOfWeek = firstDayOfWeekFromLocale(),
												)
		HeatMapCalendar(
				state = state,
				contentPadding = PaddingValues(end = 6.dp),
				dayContent = { day, week ->
					Day(
							day = day,
							startDate = startDate,
							endDate = endDate,
							week = week,
							level = data.value[day.date] ?: Level.Zero,
					   ) { clicked ->
						selection = Pair(clicked, data.value[clicked] ?: Level.Zero)
					}
				},
				weekHeader = { WeekHeader(it) },
				monthHeader = { MonthHeader(it, endDate, state) },
					   )
		CalendarInfo(
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 44.dp),
					)
	}
}

//function to get completed prayers from a tracker
fun getCompletedPrayers(tracker : PrayerTracker) : Int
{
	var completedPrayers = 0
	if (tracker.fajr) completedPrayers ++
	if (tracker.dhuhr) completedPrayers ++
	if (tracker.asr) completedPrayers ++
	if (tracker.maghrib) completedPrayers ++
	if (tracker.isha) completedPrayers ++
	return completedPrayers
}

private val daySize = 16.dp


@Composable
private fun CalendarInfo(modifier: Modifier = Modifier) {
	Row(
			modifier = modifier,
			horizontalArrangement = Arrangement.End,
			verticalAlignment = Alignment.CenterVertically,
	   ) {
		Text(text = "0", fontSize = 10.sp)
		Level.values().forEach { level ->
			LevelBox(level.color)
		}
		Text(text = "5", fontSize = 10.sp)
	}
}

@Composable
private fun Day(
	day: CalendarDay ,
	startDate: LocalDate ,
	endDate: LocalDate ,
	week: HeatMapWeek ,
	level: Level ,
	onClick: (LocalDate) -> Unit ,
			   ) {
	// We only want to draw boxes on the days that are in the
	// past 12 months. Since the calendar is month-based, we ignore
	// the future dates in the current month and those in the start
	// month that are older than 12 months from today.
	// We draw a transparent box on the empty spaces in the first week
	// so the items are laid out properly as the column is top to bottom.
	val weekDates = week.days.map { it.date }
	if (day.date in startDate..endDate) {
		LevelBox(level.color) { onClick(day.date) }
	} else if (weekDates.contains(startDate)) {
		LevelBox(Color.Transparent)
	}
}

@Composable
private fun LevelBox(color: Color, onClick: (() -> Unit)? = null) {
	Box(
			modifier = Modifier
				.size(daySize) // Must set a size on the day.
				.padding(2.dp)
				.clip(RoundedCornerShape(2.dp))
				.background(color = color)
				.clickable(enabled = onClick != null) { onClick?.invoke() },
	   )
}

@Composable
private fun WeekHeader(dayOfWeek: DayOfWeek) {
	//strip the day name to first three letters
	val dayName = dayOfWeek.name.substring(0, 3)
	Box(
			modifier = Modifier
				.height(daySize) // Must set a height on the day of week so it aligns with the day.
				.padding(horizontal = 4.dp),
	   ) {
		Text(
				text = dayName,
				modifier = Modifier.align(Alignment.Center),
				fontSize = 10.sp,
			)
	}
}

@Composable
private fun MonthHeader(
	calendarMonth: CalendarMonth ,
	endDate: LocalDate ,
	state: HeatMapCalendarState ,
					   ) {
	val density = LocalDensity.current
	val firstFullyVisibleMonth by remember {
		// Find the first index with at most one box out of bounds.
		derivedStateOf { getMonthWithYear(state.layoutInfo, daySize, density) }
	}
	if (calendarMonth.weekDays.first().first().date <= endDate) {
		val month = calendarMonth.yearMonth
		val title = month.month.name
		Box(
				modifier = Modifier
					.fillMaxWidth()
					.padding(bottom = 1.dp, start = 2.dp),
		   ) {
			Text(text = title, fontSize = 10.sp)
		}
	}
}

// Find the first index with at most one box out of bounds.
private fun getMonthWithYear(
	layoutInfo: CalendarLayoutInfo ,
	daySize: Dp ,
	density: Density ,
							): YearMonth? {
	val visibleItemsInfo = layoutInfo.visibleMonthsInfo
	return when {
		visibleItemsInfo.isEmpty() -> null
		visibleItemsInfo.count() == 1 -> visibleItemsInfo.first().month.yearMonth
		else -> {
			val firstItem = visibleItemsInfo.first()
			val daySizePx = with(density) { daySize.toPx() }
			if (
				firstItem.size < daySizePx * 3 || // Ensure the Month + Year text can fit.
				firstItem.offset < layoutInfo.viewportStartOffset && // Ensure the week row size - 1 is visible.
				(layoutInfo.viewportStartOffset - firstItem.offset > daySizePx)
			) {
				visibleItemsInfo[1].month.yearMonth
			} else {
				firstItem.month.yearMonth
			}
		}
	}
}
//preview
@Preview
@Composable
fun HistoryPreview()
{
	LocalDataStore.init(LocalContext.current)
	NimazTheme {
		History()
	}
}