package com.arshadshah.nimaz.ui.screens.tracker

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.data.local.models.LocalFastTracker
import com.arshadshah.nimaz.ui.components.calender.Calendar
import com.arshadshah.nimaz.ui.components.calender.rememberCalendarState
import com.arshadshah.nimaz.ui.components.common.BannerDuration
import com.arshadshah.nimaz.ui.components.common.BannerLarge
import com.arshadshah.nimaz.ui.components.common.BannerVariant
import com.arshadshah.nimaz.viewModel.CalendarViewModel

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

    // State for dialog visibility
    var showTrackerDialog by remember { mutableStateOf(false) }

    // Observe calendar state changes
    LaunchedEffect(calendarState.selectedDate) {
        viewModel.onDateSelected(calendarState.selectedDate)
        // Open the dialog when a date is selected
        showTrackerDialog = true
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
                    OutlinedIconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.showcaseToggle(true)
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
                    onShowcaseDismiss = {
                        viewModel.showcaseToggle(false)
                    },
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
                    },
                    onPrayerUpdate = viewModel::updatePrayer,
                    onFastingUpdate = { fastTracker: LocalFastTracker ->
                        viewModel.updateFasting(fastTracker.isFasting)
                    },
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