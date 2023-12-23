package com.arshadshah.nimaz.ui.components.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.arshadshah.nimaz.ui.components.trackers.FastTrackerCard
import com.arshadshah.nimaz.viewModel.DashboardViewmodel
import java.time.LocalDate

@Composable
fun DashboardFastTracker(
    isFasting: State<Boolean>,
    handleEvents: (DashboardViewmodel.DashboardEvent) -> Unit,
    isLoading: State<Boolean>,
    menstruating: Boolean
) {

    val dateState = remember { mutableStateOf(LocalDate.now().toString()) }
    FastTrackerCard(
        handleEvent = { date, isFastingToday ->
            handleEvents(DashboardViewmodel.DashboardEvent.UpdateFastTracker(date, isFastingToday))
        },
        dateState = dateState,
        isFastingToday = isFasting,
        isMenstrauting = menstruating,
        isLoading = isLoading
    )
}