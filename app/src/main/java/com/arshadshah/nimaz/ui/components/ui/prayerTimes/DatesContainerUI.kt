package com.arshadshah.nimaz.ui.components.ui.prayerTimes


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp

@Composable
fun DatesContainerUI(GregDate : String , islamDate : String)
{
	ElevatedCard(
			modifier = Modifier
				.fillMaxWidth()
				.shadow(5.dp , shape = CardDefaults.elevatedShape , clip = true)
				) {
		Column(horizontalAlignment = Alignment.CenterHorizontally) {
			Text(text = GregDate , Modifier.padding(12.dp))
			Divider(color = MaterialTheme.colorScheme.outline)
			Text(text = islamDate , Modifier.padding(16.dp))
		}
	}
}