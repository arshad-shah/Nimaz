package com.arshadshah.nimaz.ui.components.calender

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek

@Composable
fun CalenderWeekHeader(
	weekState : List<DayOfWeek>
					  )
{
	ElevatedCard(
			shape = MaterialTheme.shapes.small,
			modifier = Modifier.padding(top = 4.dp)
				) {
		Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 4.dp , vertical = 4.dp) ,
				horizontalArrangement = Arrangement.Center
		   ) {
			weekState.forEach { dayOfWeek ->
				Text(
						text = dayOfWeek.name.substring(0 , 3) ,
						style = MaterialTheme.typography.titleMedium ,
						color = if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY)
						{
							MaterialTheme.colorScheme.error
						} else
						{
							MaterialTheme.colorScheme.onSurface
						} ,
						maxLines = 1 ,
						overflow = TextOverflow.Ellipsis ,
						textAlign = TextAlign.Center ,
						modifier = Modifier
							.weight(1f)
							.padding(4.dp)
					)
			}
		}
	}
}