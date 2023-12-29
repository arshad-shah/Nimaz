package com.arshadshah.nimaz.ui.screens.tracker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.activities.MainActivity
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_CALENDER
import com.arshadshah.nimaz.constants.AppConstants.TRACKING_VIEWMODEL_KEY
import com.arshadshah.nimaz.data.local.models.LocalFastTracker
import com.arshadshah.nimaz.ui.components.calender.Calender
import com.arshadshah.nimaz.ui.components.calender.PrayersTrackerCard
import com.arshadshah.nimaz.ui.components.trackers.FastTrackerCard
import com.arshadshah.nimaz.viewModel.TrackerViewModel
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalenderScreen(
    paddingValues: PaddingValues,
    viewModel: TrackerViewModel = viewModel(
        key = TRACKING_VIEWMODEL_KEY,
        viewModelStoreOwner = LocalContext.current as MainActivity
    )
) {

    //call this effect only once
    LaunchedEffect(Unit) {
        viewModel.onEvent(TrackerViewModel.TrackerEvent.SET_DATE(LocalDate.now()))
        viewModel.onEvent(
            TrackerViewModel.TrackerEvent.GET_PROGRESS_FOR_MONTH(
                LocalDate.now()
            )
        )
        viewModel.onEvent(
            TrackerViewModel.TrackerEvent.GET_FAST_PROGRESS_FOR_MONTH(
                YearMonth.from(
                    LocalDate.now()
                )
            )
        )
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
    }
    val dateState = viewModel.dateState.collectAsState()

    val progressForMonth = viewModel.progressForMonth.collectAsState()

    val fastProgressForMonth = viewModel.fastProgressForMonth.collectAsState()

    val prayerTrackerState = viewModel.prayerTrackerState.collectAsState()

    val isFastingToday = viewModel.isFasting.collectAsState()
    val isMenstruatingToday = viewModel.isMenstrauting.collectAsState()

    val isLoading = remember {
        mutableStateOf(false)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .testTag(TEST_TAG_CALENDER),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        item {
            Calender(
                handleEvents = viewModel::onEvent,
                progressForMonth = progressForMonth,
                fastProgressForMonth = fastProgressForMonth
            )
        }
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                        elevation = 8.dp
                    ),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
                ),
                shape = MaterialTheme.shapes.extraLarge.copy(
                    topStart = CornerSize(0.dp),
                    topEnd = CornerSize(0.dp),
                ),
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                PrayersTrackerCard(
                    isLoading = isLoading,
                    prayerTrackerState = prayerTrackerState,
                    handleEvents = viewModel::onEvent,
                    dateState = dateState
                )

                FastTrackerCard(
                    dateState = dateState,
                    isFastingToday = isFastingToday,
                    isMenstrauting = isMenstruatingToday.value,
                    isLoading = isLoading
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
    }
}