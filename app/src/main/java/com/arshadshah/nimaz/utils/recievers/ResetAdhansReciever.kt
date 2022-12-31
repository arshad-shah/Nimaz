package com.arshadshah.nimaz.utils.recievers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.arshadshah.nimaz.utils.alarms.CreateAlarms

class ResetAdhansReciever : BroadcastReceiver()
{

	override fun onReceive(context : Context , intent : Intent)
	{
		val fajr = intent.extras!!.getLong("fajrTime")
		val sunrise = intent.extras!!.getLong("sunriseTime")
		val zuhar = intent.extras!!.getLong("zuharTime")
		val asar = intent.extras!!.getLong("asarTime")
		val maghrib = intent.extras!!.getLong("maghribTime")
		val ishaa = intent.extras!!.getLong("ishaaTime")

		//create alarms
		CreateAlarms().scheduleAlarms(context , fajr , sunrise , zuhar , asar , maghrib , ishaa)

		Log.i("Alarm Reset" , "All alarms reset")
	}
}