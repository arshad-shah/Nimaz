package com.arshadshah.nimaz.ui.components.calender

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontWeight
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
import es.dmoral.toasty.Toasty
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

    val date = dateState.value
    val isAfterToday = date.isAfter(LocalDate.now())

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Prayer Tracker",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${prayerTrackerState.value.progress / 20}/5",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Prayer Items Grid
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
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
                                if (!prayerTrackerState.value.isMenstruating && !isAfterToday) {
                                    updateTracker(dateState.value, name, isChecked)
                                    updateWidgetTracker()
                                }

                                if (isAfterToday) {
                                    Toasty.warning(
                                        context,
                                        "Cannot track prayers for future dates",
                                        Toasty.LENGTH_SHORT
                                    ).show()
                                }
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
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .placeholder(
                visible = isLoading,
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
                highlight = PlaceholderHighlight.shimmer(
                    highlightColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                )
            )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isCompleted)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
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
                        modifier = Modifier.size(18.dp)
                    )
                }
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