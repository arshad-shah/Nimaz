package com.arshadshah.nimaz.ui.screens.tracker

import PrayerTrackerGrid
import SevenDayTrend
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.activities.MainActivity
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_PRAYER_TRACKER
import com.arshadshah.nimaz.constants.AppConstants.TRACKING_VIEWMODEL_KEY
import com.arshadshah.nimaz.data.local.models.LocalFastTracker
import com.arshadshah.nimaz.ui.components.calender.PrayersTrackerCard
import com.arshadshah.nimaz.ui.components.trackers.DateSelector
import com.arshadshah.nimaz.ui.components.trackers.FastTrackerCard
import com.arshadshah.nimaz.viewModel.TrackerViewModel
import es.dmoral.toasty.Toasty
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTracker(
    viewModel: TrackerViewModel = viewModel(
        key = TRACKING_VIEWMODEL_KEY,
        viewModelStoreOwner = LocalContext.current as MainActivity
    ),
    navController: NavHostController
) {

    val dateState = viewModel.dateState.collectAsState()

    val prayerTrackerState = viewModel.prayerTrackerState.collectAsState()

    val isFasting = viewModel.isFasting.collectAsState()

    val isMenstruating = viewModel.isMenstruating.collectAsState()

    val trackersForWeek = viewModel.trackersForWeek.collectAsState()

    val progressForMonth = viewModel.progressForMonth.collectAsState()

    val isLoading = viewModel.isLoading.collectAsState()
    val isErrored = viewModel.isError.collectAsState()
    val error = viewModel.errorMessage.collectAsState()

    if (isErrored.value) {
        Toasty.error(LocalContext.current, error.value, Toasty.LENGTH_LONG, true).show()
    }
    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text("Tracker", style = MaterialTheme.typography.titleLarge)
            },
                navigationIcon = {
                    OutlinedIconButton(
                        modifier = Modifier
                            .testTag("backButton")
                            .padding(start = 8.dp),
                        onClick = {
                            navController.popBackStack()
                        }) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            painter = painterResource(id = R.drawable.back_icon),
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.updateMenstruatingState(!isMenstruating.value)
                    }) {
                        Icon(
                            modifier = Modifier.size(
                                24.dp
                            ),
                            painter = painterResource(
                                id = R.drawable.menstruation_icon
                            ),
                            contentDescription = "Menstruation",
                            //color it pink
                            tint = Color(0xFFE91E63)
                        )
                    }
                }
            )
        },
    ) {
        Column(
            modifier = Modifier
                .padding(it)
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
                    6.dp,
                ),
            ) {
                Column {
                    DateSelector(
                        dateState = dateState,
                        updateDate = viewModel::updateDate,
                    )
                    PrayersTrackerCard(
                        isLoading = isLoading,
                        prayerTrackerState = prayerTrackerState,
                        dateState = dateState,
                        updateTracker = viewModel::updateTracker,
                    )
                    FastTrackerCard(
                        dateState = dateState,
                        isFastingToday = isFasting,
                        isMenstrauting = isMenstruating.value,
                        isLoading = isLoading,
                    ) { date: LocalDate, isFasting: Boolean ->
                        viewModel.updateFastTracker(
                            LocalFastTracker(
                                date = date,
                                isFasting = isFasting
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
                    6.dp,
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
                    6.dp,
                ),
            ) {
                PrayerTrackerGrid(progressForMonth, dateState)
            }
        }
    }
}
