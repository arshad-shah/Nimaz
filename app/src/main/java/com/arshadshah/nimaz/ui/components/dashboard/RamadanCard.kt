package com.arshadshah.nimaz.ui.components.dashboard

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField

@Composable
fun RamadanCard(
    onNavigateToCalender: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val todayHijri = HijrahDate.from(today)
    val currentYear = todayHijri[ChronoField.YEAR]

    val ramadanStart = HijrahDate.of(currentYear, 9, 1)
    val ramadanEnd = HijrahDate.of(currentYear, 9, ramadanStart.lengthOfMonth())
    val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")
    val isRamadanStarted = todayHijri.isAfter(ramadanStart)

    val daysLeft = if (isRamadanStarted) {
        ramadanEnd.toEpochDay() - todayHijri.toEpochDay()
    } else {
        ramadanStart.toEpochDay() - todayHijri.toEpochDay()
    }

    val images = remember {
        listOf(
            R.drawable.ramadan,
            R.drawable.ramadan2,
            R.drawable.ramadan3,
            R.drawable.ramadan4,
            R.drawable.ramadan5
        )
    }
    val selectedImage = remember { mutableIntStateOf(images.random()) }

    if (todayHijri[ChronoField.MONTH_OF_YEAR] < 10 && daysLeft < 40) {
        val scale by animateFloatAsState(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "scale"
        )

        ElevatedCard(
            modifier = modifier
                .fillMaxWidth()
                .padding(8.dp)
                .scale(scale)
                .clickable(onClick = onNavigateToCalender),
            shape = MaterialTheme.shapes.extraLarge,
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (todayHijri[ChronoField.MONTH_OF_YEAR] == 9
                        )
                            "Ramadan Mubarak"
                        else
                            "Ramadan is Coming",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(100.dp)
                        ) {
                            Image(
                                painter = painterResource(selectedImage.value),
                                contentDescription = "Ramadan",
                                modifier = Modifier
                                    .padding(12.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (isRamadanStarted) "Estimated end" else "Estimated start",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = when (daysLeft) {
                                        0L -> "Today"
                                        1L -> "Tomorrow"
                                        else -> "In $daysLeft days"
                                    },
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = LocalDate.from(
                                        if (isRamadanStarted) ramadanEnd else ramadanStart
                                    ).format(formatter),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}