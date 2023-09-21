package com.arshadshah.nimaz.utils.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class Alarms
{

	/**
	 * Sets a Exact alarm that is allowed in doze mode
	 * @author Arshad Shah
	 * @param context The Context of the Application
	 * @param timeToNotify The Alarm time in milliseconds
	 * @param pendingIntent The pending Intent for the alarm
	 * @return Alarm
	 * */
	fun setExactAlarm(context : Context , timeToNotify : Long , pendingIntent : PendingIntent)
	{
		// get alarm manager
		val alarmManager = context.getSystemService(ComponentActivity.ALARM_SERVICE) as AlarmManager
		alarmManager.setAlarmClock(
				 AlarmManager.AlarmClockInfo(timeToNotify , pendingIntent) ,
				 pendingIntent
								  )

		//format time by converting to LocalDateTime
		val time = LocalDateTime.ofInstant(
				 Instant.ofEpochMilli(timeToNotify) ,
				 ZoneId.systemDefault()
										  )
			.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"))
		//logs
		Log.d("Nimaz: Alarms for Adhan" , "Alarm for $time is successfully created")
	} // end of alarm set
}