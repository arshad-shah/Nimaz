package com.arshadshah.nimaz.ui.components.trackers

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.ui.components.common.ToggleableItemColumn
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

    Card(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
    ) {
        ToggleableItemColumn(
            enabled = !isMenstrauting && !isAfterToday,
            text = when {
                date.isBefore(LocalDate.now()) -> "Did not fast"
                else -> "Not fasting today"
            },
            selectedText = when {
                date.isBefore(LocalDate.now()) -> "Fasted on ${formatter.format(date)}"
                else -> "Fasting today"
            },
            checked = isFastingToday.value,
            onCheckedChange = { isChecked ->
                if (isAfterToday) {
                    Toasty.warning(
                        context,
                        "Cannot track fasting for future dates",
                        Toasty.LENGTH_SHORT
                    ).show()
                    return@ToggleableItemColumn
                }
                handleEvent(date, isChecked)
            },
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .placeholder(
                    visible = isLoading.value,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                    highlight = PlaceholderHighlight.shimmer(
                        highlightColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    )
                ),
        )
    }
}