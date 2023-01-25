package com.arshadshah.nimaz.utils.recievers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.utils.NotificationHelper
import es.dmoral.toasty.Toasty

class AdhanReciever : BroadcastReceiver()
{

	@RequiresApi(Build.VERSION_CODES.TIRAMISU)
	override fun onReceive(context : Context , intent : Intent)
	{
		// When the reciever is called, it will send a notification to the user
		//send notification
		// This method is called when the BroadcastReceiver is receiving an Intent broadcast.
		val title = intent.extras !!.getString("title").toString()
		val CHANNEL_ID = intent.extras !!.getString("channelid").toString()
		val Notify_Id = intent.extras !!.getInt("notifyid")
		val Time_of_alarm = intent.extras !!.getLong("time")

		val current_time = System.currentTimeMillis()
		val diff = current_time - Time_of_alarm
		val graceP = 60000 * 2

		Log.d(AppConstants.ADHAN_RECEIVER_TAG, "Alarm for $title is being executed!")
		// check if it is time to notify
		if (diff in 1 until graceP)
		{
			Log.d(AppConstants.ADHAN_RECEIVER_TAG , "Notification for $title is being executed!")
			when (title)
			{
				"Test Adhan" ->
				{
					Toasty.info(context , "Test Adhan is being executed!").show()
				}

				"Sunrise" , "شروق" ->
				{
					Toasty.info(context , "The Sun is rising!").show()
				}

				else ->
				{
					Toasty.info(context , "Time to pray $title").show()
				}
			}
			NotificationHelper().createNotification(
					context ,
					CHANNEL_ID ,
					title ,
					Notify_Id ,
					Time_of_alarm
												   )

			Log.d(AppConstants.ADHAN_RECEIVER_TAG, "Alarm for $title is Successfully executed!")
		} // end of if
		else
		{
			Log.d(AppConstants.ADHAN_RECEIVER_TAG,
					"Notification for $title is not executed! The time has passed"
				 )
		}
	}
}