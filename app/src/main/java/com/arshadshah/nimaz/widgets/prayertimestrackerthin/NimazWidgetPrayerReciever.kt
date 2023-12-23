package com.arshadshah.nimaz.widgets.prayertimestrackerthin

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class NimazWidgetPrayerReciever : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = NimazWidgetPrayerTracker()


    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        PrayerTimesTrackerWorker.enqueue(context)
    }

    /**
     * Called when the last instance of this widget is removed.
     * Make sure to cancel all ongoing workers when user remove all widget instances
     */
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        PrayerTimesTrackerWorker.cancel(context)
    }
}