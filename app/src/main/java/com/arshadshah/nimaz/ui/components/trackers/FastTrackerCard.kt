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
    val dateForTracker = dateState.value
    val isAfterToday = dateForTracker.isAfter(LocalDate.now())

    val formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy")

    Card(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
    ) {
        ToggleableItemColumn(
            enabled = !isMenstrauting,
            text = if (dateForTracker.isBefore(LocalDate.now())) "Did not fast"
            else "Not Fasting",
            //if date state is in the fast then shoow the date
            // like this:
            // Fasted on 2021-09-01
            selectedText = if (dateForTracker.isBefore(LocalDate.now())) "Fasted on ${
                formatter.format(
                    dateForTracker
                )
            }" else "Fasting Today",
            checked = isFastingToday.value,
            onCheckedChange = {
                //if the date is after today then don't allow the user to change the value
                if (isAfterToday) {
                    Toasty.info(
                        context,
                        "Oops! you cant update the tracker for a date in the future",
                        Toasty.LENGTH_SHORT,
                        true
                    ).show()
                    return@ToggleableItemColumn
                }
                handleEvent(dateState.value, it)
            },
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                .fillMaxWidth()
                .placeholder(
                    visible = isLoading.value,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(4.dp),
                    highlight = PlaceholderHighlight.shimmer(
                        highlightColor = Color.White,
                    )
                ),
        )
    }
}