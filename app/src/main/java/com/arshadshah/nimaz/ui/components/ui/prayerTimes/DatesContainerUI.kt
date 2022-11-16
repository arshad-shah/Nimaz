package com.arshadshah.nimaz.ui.components.ui.prayerTimes


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp

@Composable
fun DatesContainerUI(GregDate: String, islamDate: String) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(5.dp, shape = MaterialTheme.shapes.medium, clip = true)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = GregDate, Modifier.padding(8.dp))
            Divider(color = MaterialTheme.colorScheme.outline)
            Text(text = islamDate, Modifier.padding(8.dp))
        }
    }
}