package com.arshadshah.nimaz.data.remote.viewModel

import android.content.Context
import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.arshadshah.nimaz.activities.MainActivity
import com.arshadshah.nimaz.data.remote.models.CountDownTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class CountTimeViewModel : ViewModel() {

    private var countDownTimer: CountDownTimer? = null

    private val _countDownTimeState = MutableLiveData<CountDownTime>()
    val timer : LiveData<CountDownTime> = _countDownTimeState

    fun startTimer(context: Context, timeToNextPrayer: Long) {
        countDownTimer = object : CountDownTimer(timeToNextPrayer, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                var diff = millisUntilFinished
                val secondsInMilli: Long = 1000
                val minutesInMilli = secondsInMilli * 60
                val hoursInMilli = minutesInMilli * 60

                val elapsedHours = diff / hoursInMilli
                diff %= hoursInMilli

                val elapsedMinutes = diff / minutesInMilli
                diff %= minutesInMilli

                val elapsedSeconds = diff / secondsInMilli
                diff %= secondsInMilli

                val countDownTime = CountDownTime(elapsedHours, elapsedMinutes, elapsedSeconds)
                _countDownTimeState.value = countDownTime
            }

            override fun onFinish() {
               PrayerTimesViewModel(context)
            }
        }.start()
    }

    fun stopTimer() {
        countDownTimer?.cancel()
    }
}
