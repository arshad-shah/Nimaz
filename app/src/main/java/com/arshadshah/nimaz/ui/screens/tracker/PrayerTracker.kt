package com.arshadshah.nimaz.ui.screens.tracker

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_PRAYER_TRACKER
import com.arshadshah.nimaz.constants.AppConstants.TRACKING_VIEWMODEL_KEY
import com.arshadshah.nimaz.ui.components.common.ProgressBarCustom
import com.arshadshah.nimaz.ui.components.trackers.PrayerTrackerGrid
import com.arshadshah.nimaz.ui.components.trackers.PrayerTrackerListItems
import com.arshadshah.nimaz.viewModel.TrackerViewModel
import es.dmoral.toasty.Toasty
import java.time.LocalDate

@Composable
fun PrayerTracker(paddingValues: PaddingValues, isIntegrated: Boolean = false) {
    val viewModel = viewModel(
        key = TRACKING_VIEWMODEL_KEY,
        initializer = { TrackerViewModel() },
        viewModelStoreOwner = LocalContext.current as androidx.activity.ComponentActivity
    )

    val dateState = remember {
        viewModel.dateState
    }.collectAsState()

    LaunchedEffect(key1 = "getTrackerForDate") {
        viewModel.onEvent(TrackerViewModel.TrackerEvent.SHOW_DATE_SELECTOR(!isIntegrated))
        viewModel.onEvent(TrackerViewModel.TrackerEvent.GET_TRACKER_FOR_DATE(dateState.value))
        viewModel.onEvent(TrackerViewModel.TrackerEvent.GET_FAST_TRACKER_FOR_DATE(dateState.value))
    }

    val stateOfTrackerForToday = remember {
        viewModel.trackerState
    }.collectAsState()

    val showDateSelector = remember {
        viewModel.showDateSelector
    }.collectAsState()

    val fajrState = remember {
        viewModel.fajrState
    }.collectAsState()

    val zuhrState = remember {
        viewModel.zuhrState
    }.collectAsState()

    val asrState = remember {
        viewModel.asrState
    }.collectAsState()

    val maghribState = remember {
        viewModel.maghribState
    }.collectAsState()

    val ishaState = remember {
        viewModel.ishaState
    }.collectAsState()

    val progressState = remember {
        viewModel.progressState
    }.collectAsState()

    val isFasting = remember {
        viewModel.isFasting
    }.collectAsState()

    val fastingState = remember {
        viewModel.fastTrackerState
    }.collectAsState()

    val isMenstrauting = remember {
        viewModel.isMenstrauting
    }.collectAsState()

    Column(
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag(TEST_TAG_PRAYER_TRACKER),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        ElevatedCard(
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
                contentColor = MaterialTheme.colorScheme.onSurface,
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
            ),
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.padding(
                top = 4.dp,
                bottom = 8.dp,
                start = 0.dp,
                end = 0.dp
            ),
        ) {
            Column {
                PrayerTrackerList(
                    viewModel::onEvent,
                    stateOfTrackerForToday.value,
                    fajrState.value,
                    zuhrState.value,
                    asrState.value,
                    maghribState.value,
                    ishaState.value,
                    showDateSelector,
                    dateState,
                    progressState,
                    isMenstrauting
                )
                Fasting(
                    viewModel::onEvent,
                    dateState,
                    isFasting.value,
                    fastingState.value,
                    isMenstrauting
                )
            }
        }
        ElevatedCard(
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
                contentColor = MaterialTheme.colorScheme.onSurface,
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
            ),
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.padding(
                top = 4.dp,
                bottom = 8.dp,
                start = 0.dp,
                end = 0.dp
            ),
        ) {
            Column {
                Text(
                    text = "7 Day Trend",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                //the data
                SevenDayTrend()
            }
        }
        ElevatedCard(
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
                contentColor = MaterialTheme.colorScheme.onSurface,
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
            ),
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.padding(
                top = 4.dp,
                bottom = 8.dp,
                start = 0.dp,
                end = 0.dp
            ),
        ) {
            Column {
                Text(
                    text = "Monthly Progress",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                PrayerTrackerGrid()
            }
        }
    }
}

//composable to show the prayers for this week using 7 circular progress indicators
@Composable
fun SevenDayTrend() {

    val viewModelTracker = viewModel(
        key = TRACKING_VIEWMODEL_KEY,
        initializer = { TrackerViewModel() },
        viewModelStoreOwner = LocalContext.current as androidx.activity.ComponentActivity
    )
    LaunchedEffect(Unit) {
        viewModelTracker.onEvent(
            TrackerViewModel.TrackerEvent.GET_PROGRESS_FOR_WEEK(
                LocalDate.now().toString()
            )
        )
    }
    val progressForMonday = remember {
        viewModelTracker.progressForMonday
    }.collectAsState()

    val progressForTuesday = remember {
        viewModelTracker.progressForTuesday
    }.collectAsState()

    val progressForWednesday = remember {
        viewModelTracker.progressForWednesday
    }.collectAsState()

    val progressForThursday = remember {
        viewModelTracker.progressForThursday
    }.collectAsState()

    val progressForFriday = remember {
        viewModelTracker.progressForFriday
    }.collectAsState()

    val progressForSaturday = remember {
        viewModelTracker.progressForSaturday
    }.collectAsState()

    val progressForSunday = remember {
        viewModelTracker.progressForSunday
    }.collectAsState()

    val weeklyTrackers = remember {
        viewModelTracker.trackersForWeek
    }.collectAsState()

    Log.d("SevenDayTrend", "SevenDayTrend: ${weeklyTrackers.value[0]}")

    Column(
        modifier = Modifier.padding(
            vertical = 8.dp,
            horizontal = 4.dp
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            //monday
            ProgressBarCustom(
                progress = progressForMonday.value.toFloat(),
                //if menstrauting then show pink else show primary
                progressColor = MaterialTheme.colorScheme.primary,
                radius = 20.dp,
                label = "M",
                strokeWidth = 6.dp,
                strokeBackgroundWidth = 3.dp,
                startDelay = 0,
                labelColor = if (progressForMonday.value == 0 && !weeklyTrackers.value[0].isMenstruating) Color.Gray
                else if (weeklyTrackers.value[0].isMenstruating) Color(0xFFE91E63)
                else MaterialTheme.colorScheme.primary
            )
            //tuesday
            ProgressBarCustom(
                progress = progressForTuesday.value.toFloat(),
                progressColor = MaterialTheme.colorScheme.primary,
                radius = 20.dp,
                label = "T",
                strokeWidth = 6.dp,
                strokeBackgroundWidth = 3.dp,
                startDelay = 0,
                labelColor = if (progressForTuesday.value == 0 && !weeklyTrackers.value[1].isMenstruating) Color.Gray
                else if (weeklyTrackers.value[1].isMenstruating) Color(0xFFE91E63)
                else MaterialTheme.colorScheme.primary
            )
            //wednesday
            ProgressBarCustom(
                progress = progressForWednesday.value.toFloat(),
                progressColor = MaterialTheme.colorScheme.primary,
                radius = 20.dp,
                label = "W",
                strokeWidth = 6.dp,
                strokeBackgroundWidth = 3.dp,
                startDelay = 0,
                labelColor = if (progressForWednesday.value == 0 && !weeklyTrackers.value[2].isMenstruating) Color.Gray
                else if (weeklyTrackers.value[2].isMenstruating) Color(0xFFE91E63)
                else MaterialTheme.colorScheme.primary
            )

            //thursday
            ProgressBarCustom(
                progress = progressForThursday.value.toFloat(),
                progressColor = MaterialTheme.colorScheme.primary,
                radius = 20.dp,
                label = "T",
                strokeWidth = 6.dp,
                strokeBackgroundWidth = 3.dp,
                startDelay = 0,
                labelColor = if (progressForThursday.value == 0 && !weeklyTrackers.value[3].isMenstruating) Color.Gray
                else if (weeklyTrackers.value[3].isMenstruating) Color(0xFFE91E63)
                else MaterialTheme.colorScheme.primary
            )

            //friday
            ProgressBarCustom(
                progress = progressForFriday.value.toFloat(),
                progressColor = MaterialTheme.colorScheme.primary,
                radius = 20.dp,
                label = "F",
                strokeWidth = 6.dp,
                strokeBackgroundWidth = 3.dp,
                startDelay = 0,
                labelColor = if (progressForFriday.value == 0 && !weeklyTrackers.value[4].isMenstruating) Color.Gray
                else if (weeklyTrackers.value[4].isMenstruating) Color(0xFFE91E63)
                else MaterialTheme.colorScheme.primary
            )

            //saturday
            ProgressBarCustom(
                progress = progressForSaturday.value.toFloat(),
                progressColor = MaterialTheme.colorScheme.primary,
                radius = 20.dp,
                label = "S",
                strokeWidth = 6.dp,
                strokeBackgroundWidth = 3.dp,
                startDelay = 0,
                labelColor = if (progressForSaturday.value == 0 && !weeklyTrackers.value[5].isMenstruating) Color.Gray else if (weeklyTrackers.value[5].isMenstruating) Color(
                    0xFFE91E63
                ) else MaterialTheme.colorScheme.primary
            )

            //sunday
            ProgressBarCustom(
                progress = progressForSunday.value.toFloat(),
                progressColor = MaterialTheme.colorScheme.primary,
                radius = 20.dp,
                label = "S",
                strokeWidth = 6.dp,
                strokeBackgroundWidth = 3.dp,
                startDelay = 0,
                labelColor = if (progressForSunday.value == 0 && !weeklyTrackers.value[6].isMenstruating) Color.Gray else if (weeklyTrackers.value[6].isMenstruating) Color(
                    0xFFE91E63
                ) else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun Fasting(
    handleEvent: (TrackerViewModel.TrackerEvent) -> Unit,
    dateState: State<String>,
    isFasting: Boolean,
    fastingState: TrackerViewModel.FastTrackerState,
    isMenstrauting: State<Boolean>,
) {

    val state = fastingState
    val isFastingToday = remember { mutableStateOf(false) }
    when (state) {
        is TrackerViewModel.FastTrackerState.Loading -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        }

        is TrackerViewModel.FastTrackerState.Tracker -> {
            isFastingToday.value = isFasting
//            FastTrackerCard(
//                handleEvent = handleEvent,
//                dateState = dateState,
//                isFastingToday = isFastingToday,
//                isMenstrauting = isMenstrauting,
//                isLoading = isLoading
//            )

        }

        is TrackerViewModel.FastTrackerState.Error -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = "Error", style = MaterialTheme.typography.titleMedium)
            }
        }

    }
}

//Prayer tracker list
@Composable
fun PrayerTrackerList(
    handleEvent: (TrackerViewModel.TrackerEvent) -> Unit,
    stateOfTrackerForToday: TrackerViewModel.TrackerState,
    fajrState: Boolean,
    zuharState: Boolean,
    asrState: Boolean,
    maghribState: Boolean,
    ishaState: Boolean,
    showDateSelector: State<Boolean>,
    dateState: State<String>,
    progressState: State<Int>,
    isMenstrauting: State<Boolean>,
) {
    val context = LocalContext.current
    //a list of toggleable items
    val items = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")

    val state = stateOfTrackerForToday

    //a list of booleans to keep track of the state of the toggleable items
    val fajrChecked = remember { mutableStateOf(false) }
    val zuhrChecked = remember { mutableStateOf(false) }
    val asrChecked = remember { mutableStateOf(false) }
    val maghribChecked = remember { mutableStateOf(false) }
    val ishaChecked = remember { mutableStateOf(false) }
    val progress = remember { mutableStateOf(0f) }

    when (state) {
        is TrackerViewModel.TrackerState.Loading -> {
            Log.d("Tracker", "Loading")
            PrayerTrackerListItems(
                items = items,
                loading = true,
                fajrChecked = fajrChecked,
                zuhrChecked = zuhrChecked,
                asrChecked = asrChecked,
                maghribChecked = maghribChecked,
                ishaChecked = ishaChecked,
                handleEvent = handleEvent,
                showDateSelector = showDateSelector,
                dateState = dateState,
                progress = progress,
                isMenstrauting = isMenstrauting
            )
        }

        is TrackerViewModel.TrackerState.Tracker -> {
            fajrChecked.value = fajrState
            zuhrChecked.value = zuharState
            asrChecked.value = asrState
            maghribChecked.value = maghribState
            ishaChecked.value = ishaState
            progress.value = progressState.value.toFloat()
            Log.d("Tracker", "Loaded")
            PrayerTrackerListItems(
                items = items,
                loading = false,
                fajrChecked = fajrChecked,
                zuhrChecked = zuhrChecked,
                asrChecked = asrChecked,
                maghribChecked = maghribChecked,
                ishaChecked = ishaChecked,
                handleEvent = handleEvent,
                showDateSelector = showDateSelector,
                dateState = dateState,
                progress = progress,
                isMenstrauting = isMenstrauting
            )
        }

        is TrackerViewModel.TrackerState.Error -> {
            Toasty.error(
                context,
                state.message,
                Toast.LENGTH_SHORT,
                true
            ).show()
        }

        else -> {
        }
    }
}