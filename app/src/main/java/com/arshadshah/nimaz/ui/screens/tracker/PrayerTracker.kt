package com.arshadshah.nimaz.ui.screens.tracker

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.activities.MainActivity
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_PRAYER_TRACKER
import com.arshadshah.nimaz.constants.AppConstants.TRACKING_VIEWMODEL_KEY
import com.arshadshah.nimaz.data.local.models.LocalFastTracker
import com.arshadshah.nimaz.ui.components.calender.PrayersTrackerCard
import com.arshadshah.nimaz.ui.components.trackers.FastTrackerCard
import com.arshadshah.nimaz.ui.components.trackers.PrayerTrackerGrid
import com.arshadshah.nimaz.ui.components.trackers.SevenDayTrend
import com.arshadshah.nimaz.viewModel.TrackerViewModel
import java.time.LocalDate

@Composable
fun PrayerTracker(
    paddingValues: PaddingValues,
    isIntegrated: Boolean = false,
    viewModel: TrackerViewModel = viewModel(
        key = TRACKING_VIEWMODEL_KEY,
        viewModelStoreOwner = LocalContext.current as MainActivity
    )
) {

    LaunchedEffect(Unit) {
        viewModel.onEvent(TrackerViewModel.TrackerEvent.SET_DATE(LocalDate.now()))
        viewModel.onEvent(TrackerViewModel.TrackerEvent.SHOW_DATE_SELECTOR(!isIntegrated))
        viewModel.onEvent(
            TrackerViewModel.TrackerEvent.GET_TRACKER_FOR_DATE(
                LocalDate.now()
            )
        )
        viewModel.onEvent(
            TrackerViewModel.TrackerEvent.GET_FAST_TRACKER_FOR_DATE(
                LocalDate.now()
            )
        )
        viewModel.onEvent(
            TrackerViewModel.TrackerEvent.GET_PROGRESS_FOR_WEEK(
                LocalDate.now()
            )
        )

        viewModel.onEvent(
            TrackerViewModel.TrackerEvent.GET_PROGRESS_FOR_MONTH(
                LocalDate.now()
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

    val progressForMonth = viewModel.progressForMonth.collectAsState()

    Column(
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag(TEST_TAG_PRAYER_TRACKER),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
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
                ) { date: LocalDate, isFasting: Boolean ->
                    viewModel.onEvent(
                        TrackerViewModel.TrackerEvent.UPDATE_FAST_TRACKER(
                            LocalFastTracker(
                                date = date,
                                isFasting = isFasting
                            )
                        )
                    )
                }
            }
        }
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            modifier = Modifier.padding(
                top = 4.dp,
                bottom = 8.dp,
                start = 0.dp,
                end = 0.dp
            ),
        ) {
                //the data
                SevenDayTrend(trackersForWeek, dateState)
        }
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            modifier = Modifier.padding(
                top = 4.dp,
                bottom = 8.dp,
                start = 0.dp,
                end = 0.dp
            ),
        ) {
                PrayerTrackerGrid(progressForMonth, dateState)
        }
    }
}
