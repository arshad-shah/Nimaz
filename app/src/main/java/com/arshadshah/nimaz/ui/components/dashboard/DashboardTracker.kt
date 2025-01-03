package com.arshadshah.nimaz.ui.components.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_ASR
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_DHUHR
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_FAJR
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_ISHA
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_MAGHRIB
import com.arshadshah.nimaz.ui.components.common.ToggleableItemRow
import com.arshadshah.nimaz.ui.components.common.placeholder.material.PlaceholderHighlight
import com.arshadshah.nimaz.ui.components.common.placeholder.material.placeholder
import com.arshadshah.nimaz.ui.components.common.placeholder.material.shimmer
import com.arshadshah.nimaz.viewModel.DashboardViewModel
import com.arshadshah.nimaz.widgets.prayertimestrackerthin.PrayerTimesTrackerWorker
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun DashboardPrayerTracker(
    dashboardPrayerTracker: DashboardViewModel.DashboardTrackerState,
    handleEvents: (DashboardViewModel.DashboardEvent) -> Unit,
    isLoading: State<Boolean>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val updateWidget = { scope.launch { PrayerTimesTrackerWorker.enqueue(context, force = true) } }

    val prayers = listOf(
        PRAYER_NAME_FAJR to dashboardPrayerTracker.fajr,
        PRAYER_NAME_DHUHR to dashboardPrayerTracker.dhuhr,
        PRAYER_NAME_ASR to dashboardPrayerTracker.asr,
        PRAYER_NAME_MAGHRIB to dashboardPrayerTracker.maghrib,
        PRAYER_NAME_ISHA to dashboardPrayerTracker.isha
    )

    Card(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            prayers.forEach { (name, status) ->
                ToggleableItemRow(
                    enabled = !dashboardPrayerTracker.isMenstruating,
                    text = name,
                    checked = status,
                    onCheckedChange = { isChecked ->
                        handleEvents(
                            DashboardViewModel.DashboardEvent.UpdatePrayerTracker(
                                date = LocalDate.now(),
                                prayerName = name,
                                prayerDone = isChecked
                            )
                        )
                        updateWidget()
                    },
                    modifier = Modifier.placeholder(
                        visible = isLoading.value,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                        highlight = PlaceholderHighlight.shimmer(
                            highlightColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                        )
                    ),
                )
            }
        }
    }
}