package com.arshadshah.nimaz.widgets.prayertimesthin

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class NimazWidgetReciever : GlanceAppWidgetReceiver()
{

	override val glanceAppWidget : GlanceAppWidget = NimazWidget()


	override fun onEnabled(context : Context)
	{
		super.onEnabled(context)
		PrayerTimeWorker.enqueue(context)
	}

	/**
	 * Called when the last instance of this widget is removed.
	 * Make sure to cancel all ongoing workers when user remove all widget instances
	 */
	override fun onDisabled(context : Context)
	{
		super.onDisabled(context)
		PrayerTimeWorker.cancel(context)
	}
}