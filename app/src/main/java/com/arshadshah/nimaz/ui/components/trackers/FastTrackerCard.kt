package com.arshadshah.nimaz.ui.components.trackers

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.ui.components.common.placeholder.material.PlaceholderHighlight
import com.arshadshah.nimaz.ui.components.common.placeholder.material.placeholder
import com.arshadshah.nimaz.ui.components.common.placeholder.material.shimmer
import es.dmoral.toasty.Toasty
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun FastTrackerCard(
    dateState: State<LocalDate>,
    isFastingToday: State<Boolean>,
    isMenstrauting: Boolean,
    isLoading: State<Boolean>,
    handleEvent: (LocalDate, Boolean) -> Unit,
) {
    val context = LocalContext.current
    val date = dateState.value
    val isAfterToday = date.isAfter(LocalDate.now())
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
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
                            text = "Fasting Tracker",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Fasting Status
            Surface(
                onClick = {
                    if (!isMenstrauting && !isAfterToday) {
                        handleEvent(date, !isFastingToday.value)
                    }
                    if (isAfterToday) {
                        Toasty.warning(
                            context,
                            "Cannot track fasting for future dates",
                            Toasty.LENGTH_SHORT
                        ).show()
                    }
                },
                enabled = !isMenstrauting,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .placeholder(
                        visible = isLoading.value,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        highlight = PlaceholderHighlight.shimmer(
                            highlightColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = when {
                                date.isBefore(LocalDate.now()) -> if (isFastingToday.value)
                                    "Fasted on ${formatter.format(date)}"
                                else
                                    "Did not fast"

                                else -> if (isFastingToday.value)
                                    "Fasting today"
                                else
                                    "Not fasting today"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = if (isFastingToday.value)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isFastingToday.value)
                                Icons.Rounded.Check
                            else
                                Icons.Rounded.RadioButtonUnchecked,
                            contentDescription = if (isFastingToday.value) "Fasting" else "Not fasting",
                            tint = if (isFastingToday.value)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(6.dp)
                                .size(16.dp)
                        )
                    }
                }
            }
        }
    }
}