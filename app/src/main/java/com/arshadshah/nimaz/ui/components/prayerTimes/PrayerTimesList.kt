package com.arshadshah.nimaz.ui.components.prayerTimes

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
        PrayerTime(
            PRAYER_NAME_SUNRISE,
            prayerTimesState.sunriseTime,
            Icons.Default.WbSunny,
            description = "Sunrise"
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column {
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
}

@Composable
fun PrayerTimeRow(
    prayerTime: PrayerTime,
    isLastItem: Boolean,
    loading: Boolean
) {
    val backgroundColor = when {
        prayerTime.isHighlighted -> MaterialTheme.colorScheme.primaryContainer
        prayerTime.isRamadan -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f)
        else -> Color.Transparent
    }

    val textColor = when {
        prayerTime.isHighlighted -> MaterialTheme.colorScheme.onPrimaryContainer
        prayerTime.isRamadan -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurface
    }

    val screensize = LocalContext.current.resources.displayMetrics.widthPixels

    val paddingByScreenSize = if (screensize > 720) {
        PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    } else {
        PaddingValues(horizontal = 8.dp, vertical = 2.dp)
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(paddingByScreenSize),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = prayerTime.icon,
                    contentDescription = "${prayerTime.name} prayer time",
                    tint = textColor,
                    modifier = Modifier.size(28.dp)
                )

                Column {
                    Text(
                        modifier = Modifier.placeholder(
                            visible = loading,
                            highlight = PlaceholderHighlight.shimmer()
                        ),
                        text = prayerTime.name.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(Locale.getDefault())
                            else it.toString()
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = textColor,
                        fontWeight = if (prayerTime.isHighlighted || prayerTime.isRamadan)
                            FontWeight.Bold else FontWeight.Medium
                    )

                    AnimatedVisibility(
                        visible = prayerTime.description.isNotEmpty(),
                        enter = fadeIn(spring()),
                        exit = fadeOut(spring())
                    ) {
                        Text(
                            text = prayerTime.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor.copy(alpha = 0.7f)
                        )
                    }

                    if (prayerTime.isRamadan) {
                        Text(
                            text = "Ramadan",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Text(
                text = prayerTime.time?.format(
                    DateTimeFormatter.ofPattern(
                        if (DateFormat.is24HourFormat(LocalContext.current))
                            "HH:mm" else "hh:mm a"
                    )
                ) ?: "",
                style = MaterialTheme.typography.titleMedium,
                color = textColor,
                fontWeight = if (prayerTime.isHighlighted || prayerTime.isRamadan)
                    FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier
                    .padding(start = 16.dp)
                    .placeholder(
                        visible = loading,
                        highlight = PlaceholderHighlight.shimmer(),
                    )
            )
        }

        if (!isLastItem) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                thickness = 0.5.dp
            )
        }
    }
}