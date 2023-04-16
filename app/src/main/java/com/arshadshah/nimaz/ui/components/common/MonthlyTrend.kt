package com.arshadshah.nimaz.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@Composable
fun MonthlyTrend()
{
	//a composable that shows a grid of dots per prayer time
	//for example:
	//the UI will look like this:
	// fajr | *
	// dhuhr | *
	// asr | *
	// maghrib | *
	// isha | *
	// here the dots represent a prayer times in a day so the first column is day 1 of the current month
	//and the last column is the last day of the current month
	// all days have the dots but they are greyed out until the user has performed the prayer
	// the dots are colored in when the user has performed the prayer
	//amount of days in current month
	val currentDate = LocalDate.now()
	val daysInMonth = currentDate.lengthOfMonth()
	Row {
		//first column is the prayer name
		//the second column is the dots grid
		//each column in the grid is a day of the month
		Column {
			Text("Fajr")
			Text("Dhuhr")
			Text("Asr")
			Text("Maghrib")
			Text("Isha")
		}
		Column {
			LazyHorizontalGrid(
					rows = GridCells.Fixed(5) ,
					content = {
						items(daysInMonth) { day ->
							Box(
									modifier = Modifier
										.size(10.dp)
										.background(Color.Gray)
							   )
						}
					}
							  )
		}
	}
}

@Preview
@Composable
fun MonthlyTrendPreview()
{
	MonthlyTrend()
}