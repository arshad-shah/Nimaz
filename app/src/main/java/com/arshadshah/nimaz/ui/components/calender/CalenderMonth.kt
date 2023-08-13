package com.arshadshah.nimaz.ui.components.calender

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun CalenderMonth(
	monthState : @Composable (PaddingValues) -> Unit ,
				 )
{
	ElevatedCard(
			shape = MaterialTheme.shapes.extraLarge.copy(
					topStart = CornerSize(8.dp) ,
					topEnd = CornerSize(8.dp) ,
					bottomStart = CornerSize(0.dp) ,
					bottomEnd = CornerSize(0.dp)
														) ,
				) {
		monthState(PaddingValues(0.dp))
	}
}