package com.arshadshah.nimaz.helperClasses.prayertimes

import android.content.Context
import android.content.res.Resources
import android.os.CountDownTimer
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.fragments.HomeFragment

/**
 * class for timer creation
 * @author Arshad shah
 * */
class TimerCreater {

    /**
     * method to create timer using countdown timer funtion
     * @param context context of the activity
     * @param endTime end time of the timer
     * @param textView textview to display the timer
     */
    fun createTimer(context: Context, endTime: Long, textView: TextView, nextPrayerName: String , ishaaLong: Boolean = false) {
        // countdown timer for prayers
        var countDownTimer: CountDownTimer? = null
        // declare and initialize variables
        val start_time_in_milli = System.currentTimeMillis()

        val difference = endTime - start_time_in_milli

        // countdown timer object
        countDownTimer =
            object : CountDownTimer(difference, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    var diff = millisUntilFinished
                    val secondsInMilli: Long = 1000
                    val minutesInMilli = secondsInMilli * 60
                    val hoursInMilli = minutesInMilli * 60

                    // elapsed hours
                    val elapsedHours = diff / hoursInMilli
                    diff %= hoursInMilli

                    // elapsed minutes
                    val elapsedMinutes = diff / minutesInMilli
                    diff %= minutesInMilli

                    // elapsed seconds
                    val elapsedSeconds = diff / secondsInMilli
                    diff %= secondsInMilli

                    val res: Resources = context.resources
                    val filledText: String
                    if(ishaaLong){
                        filledText = nextPrayerName
                    }else{
                        filledText = when {
                            elapsedHours > 1 && elapsedMinutes.toInt() != 0 -> {
                                val text = res.getString(R.string.timerWithText)
                                String.format(text, elapsedHours, elapsedMinutes, nextPrayerName)
                            }
                            elapsedHours > 1 && elapsedMinutes.toInt() == 0 -> {
                                val text = res.getString(R.string.timerSeconds)
                                String.format(text, elapsedHours, "hrs", nextPrayerName)
                            }
                            elapsedHours.toInt() == 1 && elapsedMinutes.toInt() in 2..59 -> {
                                val text = res.getString(R.string.timerWithTextSingle)
                                String.format(text, elapsedHours, elapsedMinutes, nextPrayerName)
                            }
                            elapsedHours.toInt() == 1 && elapsedMinutes < 2 && elapsedMinutes > 0 -> {
                                val text = res.getString(R.string.timerWithTextSingleMin)
                                String.format(text, elapsedHours, elapsedMinutes, nextPrayerName)
                            }
                            elapsedHours.toInt() == 1 && elapsedMinutes.toInt() == 0 -> {
                                val text = res.getString(R.string.timerSeconds)
                                String.format(text, elapsedHours, "hr", nextPrayerName)
                            }
                            elapsedHours.toInt() == 0 && elapsedMinutes in 2..59 -> {
                                val text = res.getString(R.string.timerSeconds)
                                String.format(text, elapsedMinutes, "mins", nextPrayerName)
                            }
                            elapsedHours.toInt() == 0 && elapsedMinutes < 2 && elapsedMinutes > 0 -> {
                                val text = res.getString(R.string.timerWithTextSingleMinAndSec)
                                String.format(text, elapsedMinutes, elapsedSeconds, nextPrayerName)
                            }
                            elapsedHours.toInt() == 0 && elapsedMinutes.toInt() == 0 -> {
                                val text = res.getString(R.string.timerSeconds)
                                String.format(text, elapsedSeconds, "seconds", nextPrayerName)
                            }
                            //if there is less than 1 minute left
                            else -> {
                                val text = res.getString(R.string.timerSeconds)
                                String.format(text, elapsedSeconds, "seconds", nextPrayerName)
                            }
                        }
                    }
                    textView.text = filledText
                }

                override fun onFinish() {
                    if (countDownTimer != null) {
                        //refresh fragment
                        val fragment = HomeFragment()
                        val fragmentManager = (context as AppCompatActivity).supportFragmentManager
                        val fragmentTransaction = fragmentManager.beginTransaction()
                        fragmentTransaction.replace(R.id.nav_host_fragment, fragment)
                        fragmentTransaction.commit()

                        countDownTimer?.cancel()

                        textView.isVisible = false
                    }
                }
            }.start()
    }
}