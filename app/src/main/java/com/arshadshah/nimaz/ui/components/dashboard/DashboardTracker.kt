package com.arshadshah.nimaz.ui.components.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.TransformOrigin
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

    ElevatedCard(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Prayer Tracker",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                val completedCount = prayers.count { it.second }
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = "$completedCount/5",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                prayers.forEach { (name, status) ->
                    EnhancedPrayerToggle(
                        name = name,
                        checked = status,
                        enabled = !dashboardPrayerTracker.isMenstruating,
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
                        isLoading = isLoading.value
                    )
                }
            }
        }
    }
}

@Composable
private fun EnhancedPrayerToggle(
    name: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = updateTransition(checked, label = "checked")

    val scale by transition.animateFloat(
        label = "scale",
        transitionSpec = {
            if (false isTransitioningTo true) {
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            } else {
                spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow
                )
            }
        }
    ) { if (it) 1.2f else 1f }

    val backgroundColor by transition.animateColor(
        label = "backgroundColor",
        transitionSpec = {
            spring(stiffness = Spring.StiffnessLow)
        }
    ) {
        when {
            !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            it -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
    }

    val iconColor by transition.animateColor(
        label = "iconColor",
        transitionSpec = {
            spring(stiffness = Spring.StiffnessLow)
        }
    ) {
        if (it) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    Surface(
        modifier = modifier
            .placeholder(
                visible = isLoading,
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
                highlight = PlaceholderHighlight.shimmer(
                    highlightColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                )
            ),
        onClick = { if (enabled) onCheckedChange(!checked) },
        enabled = enabled,
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = iconColor,
                modifier = Modifier
                    .size(40.dp)
                    .scale(scale),
                tonalElevation = if (checked) 4.dp else 2.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column {
                        AnimatedVisibility(
                            visible = checked,
                            enter = scaleIn(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            ) + fadeIn(),
                            exit = scaleOut() + fadeOut()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Completed",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        AnimatedVisibility(
                            visible = !checked,
                            enter = scaleIn() + fadeIn(),
                            exit = scaleOut() + fadeOut()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.RadioButtonUnchecked,
                                contentDescription = "Not completed",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }
            }

            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                color = if (checked)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}