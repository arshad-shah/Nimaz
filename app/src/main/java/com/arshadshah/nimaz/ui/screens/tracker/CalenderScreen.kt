package com.arshadshah.nimaz.ui.screens.tracker

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_ASR
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_DHUHR
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_FAJR
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_ISHA
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_MAGHRIB
import com.arshadshah.nimaz.data.local.models.LocalFastTracker
import com.arshadshah.nimaz.data.local.models.LocalPrayersTracker
import com.arshadshah.nimaz.ui.components.calender.Calendar
import com.arshadshah.nimaz.ui.components.calender.rememberCalendarState
import com.arshadshah.nimaz.ui.components.common.BannerDuration
import com.arshadshah.nimaz.ui.components.common.BannerLarge
import com.arshadshah.nimaz.ui.components.common.BannerVariant
import com.arshadshah.nimaz.viewModel.CalendarViewModel
import es.dmoral.toasty.Toasty
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel(),
    navController: NavHostController
) {
    val uiState by viewModel.uiState.collectAsState()
    val calendarData by viewModel.calendarData.collectAsState()
    val showcaseState by viewModel.showcaseState.collectAsState()
    val calendarState = rememberCalendarState()

    // Observe calendar state changes
    LaunchedEffect(calendarState.selectedDate) {
        viewModel.onDateSelected(calendarState.selectedDate)
    }

    LaunchedEffect(calendarState.currentMonth) {
        viewModel.updateMonth(calendarState.currentMonth)
    }

    Log.d("CalendarScreen", "CalendarScreen: $uiState")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Calendar", style = MaterialTheme.typography.titleLarge)
                },
                navigationIcon = {
                    OutlinedIconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.back_icon),
                            contentDescription = "Back",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.showcaseToggle()
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.info_icon),
                            contentDescription = "Settings",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 16.dp)
                        )
                    } else {
                        IconButton(
                            onClick = {
                                //get the current state of the isMenstruating from the calendarData and toggle it
                                val currentMenstruating =
                                    calendarData.monthlyFasts.find { it.date == uiState.selectedDate }?.isMenstruating
                                        ?: false
                                viewModel.updateMenstruating(!currentMenstruating)
                            }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.menstruation_icon),
                                contentDescription = "Toggle Menstruation",
                                modifier = Modifier.size(24.dp),
                                tint = Color(0xFFE91E63)
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Calendar(
                    showcaseState = showcaseState,
                    onShowcaseDismiss = viewModel::showcaseToggle,
                    selectedDate = calendarState.selectedDate,
                    currentMonth = calendarState.currentMonth,
                    trackers = calendarData.monthlyTrackers,
                    onDateSelected = calendarState::onDateSelected,
                    onMonthChanged = calendarState::onMonthChanged,
                    isMenstruatingProvider = { date ->
                        calendarData.monthlyTrackers
                            .find { it.date == date }
                            ?.isMenstruating == true
                    },
                    isFastingProvider = { date ->
                        calendarData.monthlyFasts.find { it.date == date }?.isFasting == true
                    }
                )

                if (calendarData.currentTracker != null) {
                    CalendarPrayersTrackerCard(
                        selectedDate = uiState.selectedDate,
                        tracker = calendarData.monthlyTrackers.find { it.date == uiState.selectedDate },
                        isMenstruating = calendarData.monthlyFasts.find { it.date == uiState.selectedDate }?.isMenstruating
                            ?: false,
                        onPrayerUpdate = viewModel::updatePrayer
                    )
                }

                CalendarFastTrackerCard(
                    selectedDate = uiState.selectedDate,
                    isMenstruating = calendarData.monthlyFasts.find { it.date == uiState.selectedDate }?.isMenstruating
                        ?: false,
                    isFasting = calendarData.monthlyFasts.find { it.date == uiState.selectedDate }?.isFasting
                        ?: false,
                    onFastingUpdate = { fastTracker ->
                        viewModel.updateFasting(fastTracker.isFasting)
                    }
                )
            }

            uiState.error?.let { error ->
                BannerLarge(
                    title = "Error",
                    message = error,
                    isOpen = remember { mutableStateOf(true) },
                    onDismiss = viewModel::clearError,
                    variant = BannerVariant.Error,
                    showFor = BannerDuration.FOREVER.value
                )
            }
        }
    }
}


@Composable
fun CalendarPrayersTrackerCard(
    selectedDate: LocalDate,
    tracker: LocalPrayersTracker?,
    isMenstruating: Boolean,
    onPrayerUpdate: (LocalDate, String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = tracker?.let {
        listOf(
            it.fajr,
            it.dhuhr,
            it.asr,
            it.maghrib,
            it.isha
        ).count { completed -> completed }
    } ?: 0

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Prayer Tracker",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${progress}/5",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

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
                        PRAYER_NAME_FAJR to tracker?.fajr,
                        PRAYER_NAME_DHUHR to tracker?.dhuhr,
                        PRAYER_NAME_ASR to tracker?.asr,
                        PRAYER_NAME_MAGHRIB to tracker?.maghrib,
                        PRAYER_NAME_ISHA to tracker?.isha
                    )

                    prayers.forEach { (name, completed) ->
                        PrayerIndicator(
                            name = name,
                            isCompleted = completed ?: false,
                            enabled = !isMenstruating,
                            onStatusChange = { isChecked ->
                                onPrayerUpdate(selectedDate, name, isChecked)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarFastTrackerCard(
    selectedDate: LocalDate,
    isMenstruating: Boolean,
    isFasting: Boolean,
    onFastingUpdate: (LocalFastTracker) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isAfterToday = selectedDate.isAfter(LocalDate.now())
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Fasting Tracker",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            // Fasting Status
            Surface(
                onClick = {
                    when {
                        isMenstruating -> {
                            Toasty.info(
                                context,
                                "Cannot track fasting during menstruation",
                                Toasty.LENGTH_SHORT
                            ).show()
                        }

                        isAfterToday -> {
                            Toasty.warning(
                                context,
                                "Cannot track fasting for future dates",
                                Toasty.LENGTH_SHORT
                            ).show()
                        }

                        else -> {
                            onFastingUpdate(
                                LocalFastTracker(
                                    date = selectedDate,
                                    isFasting = !isFasting  // Toggle the current status
                                )
                            )
                        }
                    }
                },
                enabled = !isMenstruating && !isAfterToday,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            selectedDate.isBefore(LocalDate.now()) -> if (isFasting)
                                "Fasted on ${formatter.format(selectedDate)}"
                            else
                                "Did not fast"

                            selectedDate.isEqual(LocalDate.now()) -> if (isFasting)
                                "Fasting today"
                            else
                                "Not fasting today"

                            else -> "Cannot track future dates"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            isAfterToday -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )

                    FastingStatusIndicator(
                        isFasting = isFasting,
                        enabled = !isMenstruating && !isAfterToday
                    )
                }
            }
        }
    }
}

@Composable
private fun PrayerIndicator(
    name: String,
    isCompleted: Boolean,
    enabled: Boolean,
    onStatusChange: (Boolean) -> Unit,
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

@Composable
private fun FastingStatusIndicator(
    isFasting: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = when {
            !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            isFasting -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        modifier = modifier.size(28.dp)
    ) {
        Icon(
            imageVector = if (isFasting)
                Icons.Rounded.Check
            else
                Icons.Rounded.RadioButtonUnchecked,
            contentDescription = if (isFasting) "Fasting" else "Not fasting",
            tint = if (isFasting)
                MaterialTheme.colorScheme.onPrimary
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(6.dp)
                .size(16.dp)
        )
    }
}
