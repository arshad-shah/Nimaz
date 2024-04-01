package com.arshadshah.nimaz.ui.components.calender.calenderday

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup

@Composable
fun ImportantDayDescriptionPopup(description: String, hasDescription: MutableState<Boolean>) {
    Popup(
        alignment = Alignment.TopCenter,
        offset = IntOffset(0, -120),
        onDismissRequest = { hasDescription.value = false }
    ) {
        ElevatedCard(
            modifier = Modifier
                .padding(8.dp)
                .clickable {
                    hasDescription.value = !hasDescription.value
                },
        ) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}