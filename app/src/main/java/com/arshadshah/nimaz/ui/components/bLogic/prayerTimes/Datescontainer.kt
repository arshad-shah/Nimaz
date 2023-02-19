package com.arshadshah.nimaz.ui.components.bLogic.prayerTimes

import androidx.compose.runtime.Composable
import com.arshadshah.nimaz.ui.components.ui.prayerTimes.DatesContainerUI
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

	DatesContainerUI(GregDate , islamDate , onNavigateToTracker)
}