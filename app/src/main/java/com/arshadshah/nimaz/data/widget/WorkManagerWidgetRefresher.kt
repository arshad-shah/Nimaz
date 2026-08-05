package com.arshadshah.nimaz.data.widget

import android.content.Context
import com.arshadshah.nimaz.domain.repository.WidgetRefresher
import com.arshadshah.nimaz.widget.prayertracker.PrayerTrackerWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerWidgetRefresher @Inject constructor(
    @ApplicationContext private val context: Context
) : WidgetRefresher {
    override fun refreshPrayerTracker() = PrayerTrackerWorker.enqueueImmediateWork(context)
}
