package com.arshadshah.nimaz.presentation.screens.prayer

import androidx.compose.ui.res.stringResource
import com.arshadshah.nimaz.presentation.theme.NimazColors
import com.arshadshah.nimaz.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arshadshah.nimaz.presentation.components.atoms.IconBadge
import com.arshadshah.nimaz.presentation.components.molecules.NimazEmptyState
import com.arshadshah.nimaz.presentation.components.molecules.NimazQadaPrayerItem
import com.arshadshah.nimaz.presentation.components.organisms.NimazBackTopAppBar
import com.arshadshah.nimaz.presentation.viewmodel.PrayerTrackerEvent
import com.arshadshah.nimaz.presentation.viewmodel.PrayerTrackerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QadaPrayersScreen(
    onNavigateBack: () -> Unit,
    viewModel: PrayerTrackerViewModel = hiltViewModel()
) {
    val qadaState by viewModel.qadaState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NimazBackTopAppBar(
                title = stringResource(R.string.qada_prayers),
                onBackClick = onNavigateBack,
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Card
            item {
                QadaSummaryCard(totalMissed = qadaState.totalMissed)
            }

            // Empty State
            if (qadaState.missedPrayers.isEmpty() && !qadaState.isLoading) {
                item {
                    NimazEmptyState(
                        title = stringResource(R.string.all_caught_up),
                        message = stringResource(R.string.all_caught_up_message)
                    )
                }
            }

            // Grouped by Month
            qadaState.groupedByMonth.forEach { (monthYear, prayers) ->
                item {
                    Text(
                        text = monthYear,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(prayers, key = { it.id }) { prayer ->
                    NimazQadaPrayerItem(
                        prayer = prayer,
                        onMarkCompleted = {
                            viewModel.onEvent(PrayerTrackerEvent.MarkQadaCompleted(prayer))
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun QadaSummaryCard(totalMissed: Int) {
    val warningOrange = NimazColors.PrayerColors.Asr
    val warningOrangeDark = NimazColors.OrangeDark

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(warningOrange, warningOrangeDark)
                )
            )
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconBadge(
                imageVector = Icons.Default.Warning,
                backgroundColor = Color.Black.copy(alpha = 0.15f),
                iconColor = Color.White,
                containerSize = 48.dp,
                iconSize = 28.dp,
                cornerRadius = 12.dp
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.prayers_to_make_up),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = if (totalMissed == 0) {
                        "All caught up!"
                    } else {
                        "$totalMissed missed prayer${if (totalMissed != 1) "s" else ""} pending"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            Text(
                text = "$totalMissed",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
