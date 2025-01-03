package com.arshadshah.nimaz.ui.components.dashboard

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.ui.components.trackers.FastTrackerCard
import com.arshadshah.nimaz.viewModel.DashboardViewModel
import java.time.LocalDate

@Composable
fun DashboardFastTracker(
    isFasting: State<Boolean>,
    handleEvents: (DashboardViewModel.DashboardEvent) -> Unit,
    isLoading: State<Boolean>,
    menstruating: Boolean
) {

    val dateState = remember { mutableStateOf(LocalDate.now()) }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        modifier = Modifier.padding(
           horizontal = 16.dp, vertical = 6.dp
        ),
    ) {
        FastTrackerCard(
            handleEvent = { date, isFastingToday ->
                handleEvents(
                    DashboardViewModel.DashboardEvent.UpdateFastTracker(
                        date,
                        isFastingToday
                    )
                )
            },
            dateState = dateState,
            isFastingToday = isFasting,
            isMenstrauting = menstruating,
            isLoading = isLoading
        )
    }
}