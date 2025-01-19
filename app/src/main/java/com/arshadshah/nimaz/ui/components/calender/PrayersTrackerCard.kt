package com.arshadshah.nimaz.ui.components.calender

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_ASR
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_DHUHR
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_FAJR
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_ISHA
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_MAGHRIB
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

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Prayer Tracker",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${prayerTrackerState.value.progress / 20}/5",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Prayer Items Grid
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val prayers = listOf(
                        PRAYER_NAME_FAJR to prayerTrackerState.value.fajr,
                        PRAYER_NAME_DHUHR to prayerTrackerState.value.dhuhr,
                        PRAYER_NAME_ASR to prayerTrackerState.value.asr,
                        PRAYER_NAME_MAGHRIB to prayerTrackerState.value.maghrib,
                        PRAYER_NAME_ISHA to prayerTrackerState.value.isha
                    )

                    prayers.forEach { (name, status) ->
                        CompactPrayerItem(
                            name = name,
                            isCompleted = status,
                            enabled = !prayerTrackerState.value.isMenstruating,
                            onStatusChange = { isChecked ->
                                updateTracker(dateState.value, name, isChecked)
                                updateWidgetTracker()
                            },
                            isLoading = isLoading.value
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactPrayerItem(
    name: String,
    isCompleted: Boolean,
    enabled: Boolean,
    onStatusChange: (Boolean) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = { if (enabled) onStatusChange(!isCompleted) },
        enabled = enabled,
        color = when {
            !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            isCompleted -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surface
        },
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .placeholder(
                visible = isLoading,
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                highlight = PlaceholderHighlight.shimmer(
                    highlightColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                )
            )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (isCompleted)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (isCompleted)
                        Icons.Rounded.Check
                    else
                        Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = if (isCompleted) "Completed" else "Not completed",
                    tint = if (isCompleted)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(6.dp)
                        .size(16.dp)
                )
            }

            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                color = if (isCompleted)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurface
            )
        }
    }
}