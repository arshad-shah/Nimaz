package com.arshadshah.nimaz.utils

import android.annotation.SuppressLint
import android.app.Notification.VISIBILITY_PUBLIC
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.graphics.Color.GREEN
import android.media.AudioAttributes
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.activities.RoutingActivity
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.constants.AppConstants.NOTIFICATION_PENDING_INTENT_REQUEST_CODE
import java.text.DateFormat
import java.util.*

/**
 * A Helper class that creates both the Notifications and the notification channels
 * @author Arshad Shah
 */
class NotificationHelper {

    /**
     * Creates Notification Channel in Android O and Above
     * @author Arshad Shah
     * @param context The Context of the Application
     * @param importance The importance of the Channel
     * @param showBadge Show Badge for Channel
     * @param name The name of the channel
     * @param description The Description for the channel
     * @param channel_id The id of the Channel Creates a Channel
     * @param sound the sound to be used for the notification
     */
    fun createNotificationChannel(
        context: Context,
        importance: Int,
        showBadge: Boolean,
        name: String,
        description: String,
        channel_id: String,
        sound: String,
    ) {
        val notificationManager: NotificationManager =
            context.getSystemService(NOTIFICATION_SERVICE) as
                    NotificationManager

        val Adhan: Uri = Uri.parse(sound)

        val attributes =
            AudioAttributes.Builder()
                .setContentType(
                    AudioAttributes
                        .CONTENT_TYPE_SONIFICATION
                )
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()
        // 1
        // 2
        val channel = NotificationChannel(channel_id, name, importance)
        channel.description = description
        channel.setShowBadge(showBadge)
        channel.enableLights(true)
        channel.lightColor = GREEN
        channel.setSound(Adhan, attributes)
        channel.lockscreenVisibility = VISIBILITY_PUBLIC
        channel.enableVibration(true)
        channel.vibrationPattern =
            longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
        // 3
        notificationManager.createNotificationChannel(channel)
        Log.d(AppConstants.NOTIFICATION_TAG, "Notification Channel $name Successfully created")
    }


    /**
     * Creates Notification Channel in Android O and Above
     * @author Arshad Shah
     * @param context The Context of the Application
     * @param importance The importance of the Channel
     * @param showBadge Show Badge for Channel
     * @param name The name of the channel
     * @param description The Description for the channel
     * @param channel_id The id of the Channel Creates a Channel
     */
    fun notificationChannelSilent(
        context: Context,
        importance: Int,
        showBadge: Boolean,
        name: String,
        description: String,
        channel_id: String,
    ) {
        val notificationManager: NotificationManager =
            context.getSystemService(NOTIFICATION_SERVICE) as
                    NotificationManager
        // 1
        // 2
        val channel = NotificationChannel(channel_id, name, importance)
        channel.description = description
        channel.setShowBadge(showBadge)
        channel.enableLights(true)
        channel.lightColor = GREEN
        channel.lockscreenVisibility = VISIBILITY_PUBLIC
        channel.enableVibration(true)
        channel.vibrationPattern =
            longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
        // 3
        notificationManager.createNotificationChannel(channel)
        Log.d(AppConstants.NOTIFICATION_TAG, "Notification Channel $name Successfully created")
    }


    /**
     * Creates a Notification
     * @author Arshad Shah
     * @param context The Context of the Application
     * @param channel_id The id of the Channel
     * @param title the title of the notification
     * @param notification_id The id of the Notification ( Unique Integer)
     */
    @SuppressLint("MissingPermission")
    fun createNotification(
        context: Context,
        channel_id: String,
        title: String,
        notification_id: Int,
        Time_of_alarm: Long,
    ) {
        // Create an explicit intent for an Activity in your app
        val notificationIntent =
            Intent(context, RoutingActivity::class.java).apply {
                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent
                                .FLAG_ACTIVITY_CLEAR_TASK
            }
        val notificationPendingIntent: PendingIntent =
            PendingIntent.getActivity(
                context,
                NOTIFICATION_PENDING_INTENT_REQUEST_CODE,
                notificationIntent,
                FLAG_IMMUTABLE
            )

        val time = Date(Time_of_alarm)
        //convert the time to a date
        val formatter = DateFormat.getTimeInstance((DateFormat.SHORT))
        val timeFormated = formatter.format(time)

        val builder =
            NotificationCompat.Builder(context, channel_id).apply {
                setSmallIcon(R.mipmap.ic_launcher_round)
                setContentTitle("$title at $timeFormated")
                when (title) {
                    "Test Adhan" -> {
                        setContentText("This is a test Adhan")
                    }

                    "Sunrise", "شروق" -> {
                        setContentText("The sun is rising!!")
                    }

                    else -> {
                        setContentText("It is time to pray $title")
                    }
                }
                priority = NotificationCompat.PRIORITY_HIGH
                setContentIntent(notificationPendingIntent)
                setAutoCancel(true)
            }
        with(NotificationManagerCompat.from(context)) {
            notify(notification_id, builder.build())
        }
        Log.d(AppConstants.NOTIFICATION_TAG, "Notification $title Successfully Displayed")
    }

    @SuppressLint("MissingPermission")
    fun createNotificationForMissedPrayer(
        context: Context,
        channel_id: String,
        notification_id: Int,
        title: String,
        message: String,
    ) {
        // Create an explicit intent for an Activity in your app
        val notificationIntent =
            Intent(context, RoutingActivity::class.java).apply {
                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent
                                .FLAG_ACTIVITY_CLEAR_TASK
            }
        val notificationPendingIntent: PendingIntent =
            PendingIntent.getActivity(
                context,
                NOTIFICATION_PENDING_INTENT_REQUEST_CODE,
                notificationIntent,
                FLAG_IMMUTABLE
            )
        val builder =
            NotificationCompat.Builder(context, channel_id).apply {
                setSmallIcon(R.mipmap.ic_launcher_round)
                setContentTitle(title)
                setContentText(message)
                priority = NotificationCompat.PRIORITY_HIGH
                setContentIntent(notificationPendingIntent)
                setAutoCancel(true)
            }
        with(NotificationManagerCompat.from(context)) {
            notify(notification_id, builder.build())
        }
        Log.d(AppConstants.NOTIFICATION_TAG, "Notification $message Successfully Displayed")
    }
}
