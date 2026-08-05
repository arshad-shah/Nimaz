package com.arshadshah.nimaz.domain.repository

/**
 * Asks the home-screen widgets to redraw.
 *
 * `HomeViewModel` called `PrayerTrackerWorker.enqueueImmediateWork(context)` directly — a
 * ViewModel reaching into the widget layer's WorkManager plumbing, and one of the reasons it
 * needed an `@ApplicationContext` at all. What it actually wants to say is "the tracker
 * changed, redraw"; which worker does that, and how it is scheduled, is not its business.
 */
interface WidgetRefresher {
    /** Redraw the prayer-tracker widget now. */
    fun refreshPrayerTracker()
}
