package com.arshadshah.nimaz.ui.components.calender

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
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.ui.components.common.ToggleableItemRow
import com.arshadshah.nimaz.ui.components.common.placeholder.material.PlaceholderHighlight
import com.arshadshah.nimaz.ui.components.common.placeholder.material.placeholder
import com.arshadshah.nimaz.ui.components.common.placeholder.material.shimmer
import com.arshadshah.nimaz.viewModel.TrackerViewModel
import com.arshadshah.nimaz.widgets.prayertimestrackerthin.PrayerTimesTrackerWorker
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.reflect.KFunction3

@Composable
fun PrayersTrackerCard(
    isLoading: State<Boolean>,
    prayerTrackerState: State<TrackerViewModel.PrayerTrackerState>,
    dateState: State<LocalDate>,
    updateTracker: KFunction3<LocalDate, String, Boolean, Unit>,
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val updateWidgetTracker =
        { scope.launch { PrayerTimesTrackerWorker.enqueue(context, force = true) } }

    val prayerNames = listOf(
        AppConstants.PRAYER_NAME_FAJR,
        AppConstants.PRAYER_NAME_DHUHR,
        AppConstants.PRAYER_NAME_ASR,
        AppConstants.PRAYER_NAME_MAGHRIB,
        AppConstants.PRAYER_NAME_ISHA
    )
    val prayerStatuses = listOf(
        prayerTrackerState.value.fajr,
        prayerTrackerState.value.dhuhr,
        prayerTrackerState.value.asr,
        prayerTrackerState.value.maghrib,
        prayerTrackerState.value.isha
    )

    Card(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
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
                enabled = !prayerTrackerState.value.isMenstruating,
                text = prayerName,
                checked = prayerStatuses[index],
                onCheckedChange = {
                    updateTracker(dateState.value, prayerName, it)
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