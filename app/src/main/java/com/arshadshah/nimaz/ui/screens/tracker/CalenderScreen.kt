package com.arshadshah.nimaz.ui.screens.tracker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_CALENDER
import com.arshadshah.nimaz.constants.AppConstants.TRACKING_VIEWMODEL_KEY
import com.arshadshah.nimaz.data.local.models.LocalFastTracker
import com.arshadshah.nimaz.ui.components.calender.Calender
import com.arshadshah.nimaz.ui.components.calender.PrayersTrackerCard
import com.arshadshah.nimaz.ui.components.trackers.FastTrackerCard
import com.arshadshah.nimaz.viewModel.TrackerViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalenderScreen(
    viewModel: TrackerViewModel = viewModel(
        key = TRACKING_VIEWMODEL_KEY,
        viewModelStoreOwner = LocalContext.current as MainActivity
    ),
    navController: NavHostController
) {
    val dateState = viewModel.dateState.collectAsState()

    val progressForMonth = viewModel.progressForMonth.collectAsState()

    val fastProgressForMonth = viewModel.fastProgressForMonth.collectAsState()

    val prayerTrackerState = viewModel.prayerTrackerState.collectAsState()

    val isFastingToday = viewModel.isFasting.collectAsState()
    val isMenstruatingToday = viewModel.isMenstruating.collectAsState()

    val isLoading = remember {
        mutableStateOf(false)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text("Calender", style = MaterialTheme.typography.titleLarge)
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
                        viewModel.updateMenstruatingState(!isMenstruatingToday.value)
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .testTag(TEST_TAG_CALENDER),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            item {
                Calender(
                    handleEvents = viewModel::updateDate,
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
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    PrayersTrackerCard(
                        isLoading = isLoading,
                        prayerTrackerState = prayerTrackerState,
                        updateTracker = viewModel::updateTracker,
                        dateState = dateState
                    )
                }
                FastTrackerCard(
                    dateState = dateState,
                    isFastingToday = isFastingToday,
                    isMenstrauting = isMenstruatingToday.value,
                    isLoading = isLoading
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
    }
}