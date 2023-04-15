package com.arshadshah.nimaz.ui.components.prayerTimes

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter

@Composable
fun DatesContainer(onNavigateToTracker : () -> Unit)
{

	//localDate
	val currentDate = LocalDate.now()
	// Gregorian Date
	val Gregformat = DateTimeFormatter.ofPattern(" EEEE, dd - MMMM - yyyy")
	val GregDate = Gregformat.format(currentDate)

	// hijri date
	val islamicDate = HijrahDate.now()
	val islamformat = DateTimeFormatter.ofPattern(" dd - MMMM - yyyy G")
	val islamDate = islamformat.format(islamicDate)

	ElevatedCard(
			shape = MaterialTheme.shapes.extraLarge ,
			modifier = Modifier
				.padding(vertical = 8.dp , horizontal = 8.dp)
				.fillMaxWidth()
				.clickable {
					onNavigateToTracker()
				}
				) {
		Column(horizontalAlignment = Alignment.CenterHorizontally) {
			Text(
					text = GregDate ,
					Modifier.padding(vertical = 16.dp , horizontal = 8.dp) ,
					style = MaterialTheme.typography.titleLarge
				)
			Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
			Text(
					text = islamDate ,
					Modifier.padding(16.dp) ,
					style = MaterialTheme.typography.titleLarge
				)
		}
	}
}