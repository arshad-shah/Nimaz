package com.arshadshah.nimaz.ui.screens.tracker

import android.util.Log
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
import com.arshadshah.nimaz.data.remote.models.FastTracker
import com.arshadshah.nimaz.data.remote.models.PrayerTracker
import com.arshadshah.nimaz.ui.components.calender.PrayersTrackerCard
import com.arshadshah.nimaz.ui.components.common.ProgressBarCustom
import com.arshadshah.nimaz.ui.components.trackers.FastTrackerCard
import com.arshadshah.nimaz.ui.components.trackers.PrayerTrackerGrid
import com.arshadshah.nimaz.viewModel.TrackerViewModel
import java.time.LocalDate

@Composable
fun PrayerTracker(paddingValues: PaddingValues, isIntegrated: Boolean = false) {
    val viewModel = viewModel(
        key = TRACKING_VIEWMODEL_KEY,
        initializer = { TrackerViewModel() },
        viewModelStoreOwner = LocalContext.current as androidx.activity.ComponentActivity
    )

    LaunchedEffect(Unit) {
        viewModel.onEvent(TrackerViewModel.TrackerEvent.SET_DATE(LocalDate.now().toString()))
        viewModel.onEvent(TrackerViewModel.TrackerEvent.SHOW_DATE_SELECTOR(!isIntegrated))
        viewModel.onEvent(
            TrackerViewModel.TrackerEvent.GET_TRACKER_FOR_DATE(
                LocalDate.now().toString()
            )
        )
        viewModel.onEvent(
            TrackerViewModel.TrackerEvent.GET_FAST_TRACKER_FOR_DATE(
                LocalDate.now().toString()
            )
        )
        viewModel.onEvent(
            TrackerViewModel.TrackerEvent.GET_PROGRESS_FOR_WEEK(
                LocalDate.now().toString()
            )
        )

        Log.d("PrayertrackerCard", "PrayerTracker: LaunchedEffect")
    }

    val dateState = viewModel.dateState.collectAsState()

    val prayerTrackerState = viewModel.prayerTrackerState.collectAsState()

    val showDateSelector = viewModel.showDateSelector.collectAsState()

    val isFasting = viewModel.isFasting.collectAsState()

    val isMenstrauting = viewModel.isMenstrauting.collectAsState()

    val trackersForWeek = viewModel.trackersForWeek.collectAsState()

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
                PrayersTrackerCard(
                    isLoading = remember {
                        mutableStateOf(false)
                    },
                    prayerTrackerState = prayerTrackerState,
                    handleEvents = viewModel::onEvent,
                    dateState = dateState,
                    showDateSelector = showDateSelector
                )
                FastTrackerCard(
                    dateState = dateState,
                    isFastingToday = isFasting,
                    isMenstrauting = isMenstrauting.value,
                    isLoading = remember {
                        mutableStateOf(false)
                    },
                    handleEvent = { date: String, isFasting: Boolean ->
                        viewModel.onEvent(
                            TrackerViewModel.TrackerEvent.UPDATE_FAST_TRACKER(
                                FastTracker(
                                    date = date,
                                    isFasting = isFasting
                                )
                            )
                        )
                    },
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
                SevenDayTrend(trackersForWeek)
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
fun SevenDayTrend(trackersForWeek: State<List<PrayerTracker>>) {

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
            trackersForWeek.value.forEachIndexed { index, prayerTracker ->
                ProgressBarCustom(
                    progress = prayerTracker.progress.toFloat(),
                    //if menstrauting then show pink else show primary
                    progressColor = MaterialTheme.colorScheme.primary,
                    radius = 20.dp,
                    label = LocalDate.parse(prayerTracker.date).dayOfWeek.name.first().toString(),
                    strokeWidth = 6.dp,
                    strokeBackgroundWidth = 3.dp,
                    startDelay = 0,
                    labelColor = if (prayerTracker.progress == 0 && !prayerTracker.isMenstruating) Color.Gray
                    else if (prayerTracker.isMenstruating) Color(0xFFE91E63)
                    else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
