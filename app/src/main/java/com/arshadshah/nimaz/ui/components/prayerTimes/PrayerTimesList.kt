package com.arshadshah.nimaz.ui.components.prayerTimes

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_ASR
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_DHUHR
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_FAJR
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_ISHA
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_MAGHRIB
import com.arshadshah.nimaz.constants.AppConstants.PRAYER_NAME_SUNRISE
import com.arshadshah.nimaz.ui.components.common.placeholder.material.PlaceholderHighlight
import com.arshadshah.nimaz.ui.components.common.placeholder.material.placeholder
import com.arshadshah.nimaz.ui.components.common.placeholder.material.shimmer
import com.arshadshah.nimaz.viewModel.PrayerTimesViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.util.Locale

data class PrayerTime(
    val name: String,
    val time: LocalDateTime?,
    val icon: ImageVector,
    val isHighlighted: Boolean = false,
    val isRamadan: Boolean = false,
    val description: String = ""
)

@Composable
fun PrayerTimesList(
    prayerTimesState: PrayerTimesViewModel.PrayerTimesState,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val currentPrayerName = prayerTimesState.currentPrayerName
    val today = LocalDate.now()
    val todayHijri = HijrahDate.from(today)
    val isRamadan = todayHijri[ChronoField.MONTH_OF_YEAR] == 9

    val prayerTimes = listOf(
        PrayerTime(
            PRAYER_NAME_FAJR,
            prayerTimesState.fajrTime,
            Icons.Default.NightsStay,
            isRamadan = isRamadan,
            description = "Dawn Prayer"
        ),
        //R.drawable.ic_sunrise
        PrayerTime(
            PRAYER_NAME_SUNRISE,
            prayerTimesState.sunriseTime,
            ImageVector.vectorResource(id = R.drawable.sunrise),
            description = ""
        ),
        PrayerTime(
            PRAYER_NAME_DHUHR,
            prayerTimesState.dhuhrTime,
            Icons.Default.WbSunny,
            description = "Noon Prayer"
        ),
        PrayerTime(
            PRAYER_NAME_ASR,
            prayerTimesState.asrTime,
            Icons.Default.WbSunny,
            description = "Afternoon Prayer"
        ),
        PrayerTime(
            PRAYER_NAME_MAGHRIB,
            prayerTimesState.maghribTime,
            Icons.Default.WbTwilight,
            isRamadan = isRamadan,
            description = "Sunset Prayer"
        ),
        PrayerTime(
            PRAYER_NAME_ISHA,
            prayerTimesState.ishaTime,
            Icons.Default.NightsStay,
            description = "Night Prayer"
        )
    ).map { it.copy(isHighlighted = it.name == currentPrayerName) }

    ElevatedCard(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
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
                .padding(vertical = 8.dp)
        ) {
            prayerTimes.forEachIndexed { index, prayerTime ->
                PrayerTimeRow(
                    prayerTime = prayerTime,
                    isLastItem = index == prayerTimes.lastIndex,
                    loading = isLoading
                )
            }
        }
    }
}

@Composable
fun PrayerTimeRow(
    prayerTime: PrayerTime,
    isLastItem: Boolean,
    loading: Boolean
) {
    val transition = updateTransition(
        targetState = Triple(prayerTime.isHighlighted, prayerTime.isRamadan, loading),
        label = "prayerTime"
    )

    val backgroundColor by transition.animateColor(label = "backgroundColor") { (isHighlighted, isRamadan, _) ->
        when {
            isHighlighted -> MaterialTheme.colorScheme.primaryContainer
            isRamadan -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f)
            else -> MaterialTheme.colorScheme.surface
        }
    }

    val contentColor by transition.animateColor(label = "contentColor") { (isHighlighted, isRamadan, _) ->
        when {
            isHighlighted -> MaterialTheme.colorScheme.onPrimaryContainer
            isRamadan -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.onSurface
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 0.dp),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        tonalElevation = if (prayerTime.isHighlighted) 8.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Prayer Icon
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (prayerTime.isHighlighted)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = prayerTime.icon,
                        contentDescription = null,
                        tint = if (prayerTime.isHighlighted)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            contentColor,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(24.dp)
                    )
                }

                // Prayer Info
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = prayerTime.name.replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(Locale.getDefault())
                                else it.toString()
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = contentColor
                        )

                        if (prayerTime.isRamadan) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Ramadan",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = prayerTime.description.isNotEmpty(),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Text(
                            text = prayerTime.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (prayerTime.isHighlighted)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Text(
                    text = prayerTime.time?.format(
                        DateTimeFormatter.ofPattern(
                            if (DateFormat.is24HourFormat(LocalContext.current))
                                "HH:mm"
                            else
                                "hh:mm a"
                        )
                    ) ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (prayerTime.isHighlighted)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        contentColor,
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .placeholder(
                            visible = loading,
                            highlight = PlaceholderHighlight.shimmer()
                        )
                )
            }
        }
    }

    if (!isLastItem) {
        Spacer(modifier = Modifier.height(4.dp))
    }
}