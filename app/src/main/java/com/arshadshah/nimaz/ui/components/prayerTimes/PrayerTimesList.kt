package com.arshadshah.nimaz.ui.components.prayerTimes

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    val description: String = "",
    val gradientColors: Pair<Color, Color> = Pair(Color.Transparent, Color.Transparent)
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
            description = "Dawn Prayer",
            gradientColors = Pair(
                Color(0xFF1A237E).copy(alpha = 0.8f),
                Color(0xFF3949AB).copy(alpha = 0.6f)
            )
        ),
        PrayerTime(
            PRAYER_NAME_SUNRISE,
            prayerTimesState.sunriseTime,
            ImageVector.vectorResource(id = R.drawable.sunrise),
            description = "",
            gradientColors = Pair(
                Color(0xFFFF6F00).copy(alpha = 0.8f),
                Color(0xFFFFB74D).copy(alpha = 0.6f)
            )
        ),
        PrayerTime(
            PRAYER_NAME_DHUHR,
            prayerTimesState.dhuhrTime,
            Icons.Default.WbSunny,
            description = "Noon Prayer",
            gradientColors = Pair(
                Color(0xFFFFC107).copy(alpha = 0.8f),
                Color(0xFFFFE082).copy(alpha = 0.6f)
            )
        ),
        PrayerTime(
            PRAYER_NAME_ASR,
            prayerTimesState.asrTime,
            Icons.Default.WbSunny,
            description = "Afternoon Prayer",
            gradientColors = Pair(
                Color(0xFFFF8F00).copy(alpha = 0.8f),
                Color(0xFFFFC947).copy(alpha = 0.6f)
            )
        ),
        PrayerTime(
            PRAYER_NAME_MAGHRIB,
            prayerTimesState.maghribTime,
            Icons.Default.WbTwilight,
            isRamadan = isRamadan,
            description = "Sunset Prayer",
            gradientColors = Pair(
                Color(0xFFE91E63).copy(alpha = 0.8f),
                Color(0xFFF8BBD9).copy(alpha = 0.6f)
            )
        ),
        PrayerTime(
            PRAYER_NAME_ISHA,
            prayerTimesState.ishaTime,
            Icons.Default.NightsStay,
            description = "Night Prayer",
            gradientColors = Pair(
                Color(0xFF4A148C).copy(alpha = 0.8f),
                Color(0xFF7B1FA2).copy(alpha = 0.6f)
            )
        )
    ).map { it.copy(isHighlighted = it.name == currentPrayerName) }

    ElevatedCard(
        modifier = modifier
            .padding(8.dp)
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
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
            isHighlighted -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
            isRamadan -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.1f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
        }
    }

    val contentColor by transition.animateColor(label = "contentColor") { (isHighlighted, isRamadan, _) ->
        when {
            isHighlighted -> MaterialTheme.colorScheme.onPrimaryContainer
            isRamadan -> MaterialTheme.colorScheme.onSurface
            else -> MaterialTheme.colorScheme.onSurface
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        tonalElevation = if (prayerTime.isHighlighted) 8.dp else 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Enhanced Prayer Icon with gradient background
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(44.dp),
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .background(
                                if (prayerTime.isHighlighted) {
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                        )
                                    )
                                } else {
                                    Brush.linearGradient(
                                        colors = listOf(
                                            prayerTime.gradientColors.first,
                                            prayerTime.gradientColors.second
                                        )
                                    )
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = prayerTime.icon,
                            contentDescription = null,
                            tint = if (prayerTime.isHighlighted)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Prayer Info with enhanced typography
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = prayerTime.name.replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(Locale.getDefault())
                                else it.toString()
                            },
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = if (prayerTime.isHighlighted) FontWeight.SemiBold else FontWeight.Medium
                            ),
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (prayerTime.isRamadan) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(8.dp),
                                tonalElevation = 2.dp
                            ) {
                                Text(
                                    text = "Ramadan",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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

            // Enhanced time display
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (prayerTime.isHighlighted)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                tonalElevation = if (prayerTime.isHighlighted) 4.dp else 1.dp
            ) {
                Text(
                    text = prayerTime.time?.format(
                        DateTimeFormatter.ofPattern(
                            if (DateFormat.is24HourFormat(LocalContext.current))
                                "HH:mm"
                            else
                                "h:mm a"
                        )
                    ) ?: "",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (prayerTime.isHighlighted)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        contentColor,
                    modifier = Modifier
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .placeholder(
                            visible = loading,
                            highlight = PlaceholderHighlight.shimmer()
                        )
                )
            }
        }
    }
}