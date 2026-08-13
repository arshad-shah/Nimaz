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

    /**
     * Recompute every widget now, because something they all read has changed.
     *
     * Only the tracker had a hook, so a change of location, calculation method, clock format or
     * Hijri offset left every widget showing the old answer until its own periodic run came round
     * — fifteen minutes for the prayer widgets and six hours for the Hijri ones. A user who fixed
     * their calculation method and then looked at their home screen saw the wrong times still
     * sitting there, with no way to tell whether the setting had taken.
     *
     * A widget with no instance placed costs nothing: its worker returns immediately.
     */
    fun refreshAll()
}
