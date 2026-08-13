package com.arshadshah.nimaz.data.widget

import android.content.Context
import com.arshadshah.nimaz.domain.repository.WidgetRefresher
import com.arshadshah.nimaz.widget.hijricalendar.HijriCalendarWorker
import com.arshadshah.nimaz.widget.hijridate.HijriDateWorker
import com.arshadshah.nimaz.widget.khatam.KhatamWorker
import com.arshadshah.nimaz.widget.nextprayer.NextPrayerWorker
import com.arshadshah.nimaz.widget.prayertimes.PrayerTimesWorker
import com.arshadshah.nimaz.widget.prayertracker.PrayerTrackerWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerWidgetRefresher @Inject constructor(
    @ApplicationContext private val context: Context
) : WidgetRefresher {
    override fun refreshPrayerTracker() = PrayerTrackerWorker.enqueueImmediateWork(context)

    override fun refreshAll() {
        NextPrayerWorker.enqueueImmediateWork(context)
        PrayerTimesWorker.enqueueImmediateWork(context)
        PrayerTrackerWorker.enqueueImmediateWork(context)
        HijriDateWorker.enqueueImmediateWork(context)
        HijriCalendarWorker.enqueueImmediateWork(context)
        KhatamWorker.enqueueImmediateWork(context)
    }
}
