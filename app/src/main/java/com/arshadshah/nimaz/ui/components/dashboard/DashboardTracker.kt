package com.arshadshah.nimaz.ui.components.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
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
import com.arshadshah.nimaz.viewModel.DashboardViewmodel
import com.arshadshah.nimaz.widgets.prayertimestrackerthin.PrayerTimesTrackerWorker
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun DashboardPrayerTracker(
    dashboardPrayerTracker: DashboardViewmodel.DashboardTrackerState,
    handleEvents: (DashboardViewmodel.DashboardEvent) -> Unit,
    isLoading: State<Boolean>
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val updateWidgetTracker =
        { scope.launch { PrayerTimesTrackerWorker.enqueue(context, force = true) } }

    val prayerNames = listOf(
        PRAYER_NAME_FAJR,
        PRAYER_NAME_DHUHR,
        PRAYER_NAME_ASR,
        PRAYER_NAME_MAGHRIB,
        PRAYER_NAME_ISHA
    )
    val prayerStatuses = listOf(
        dashboardPrayerTracker.fajr,
        dashboardPrayerTracker.dhuhr,
        dashboardPrayerTracker.asr,
        dashboardPrayerTracker.maghrib,
        dashboardPrayerTracker.isha
    )

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(32.dp),
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
        ),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            prayerNames.forEachIndexed { index, prayerName ->
                ToggleableItemRow(
                    enabled = !dashboardPrayerTracker.isMenstruating,
                    text = prayerName,
                    checked = prayerStatuses[index],
                    onCheckedChange = {
                        handleEvents(
                            DashboardViewmodel.DashboardEvent.UpdatePrayerTracker(
                                date = LocalDate.now().toString(),
                                prayerName = prayerName,
                                prayerDone = it
                            )
                        )
                        updateWidgetTracker()
                    },
                    modifier = Modifier.placeholder(
                        visible = isLoading.value,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(4.dp),
                        highlight = PlaceholderHighlight.shimmer(highlightColor = Color.White)
                    )
                )
            }
        }
    }
}