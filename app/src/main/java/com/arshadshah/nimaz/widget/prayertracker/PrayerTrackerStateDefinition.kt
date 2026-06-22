package com.arshadshah.nimaz.widget.prayertracker

import com.arshadshah.nimaz.widget.core.JsonGlanceStateDefinition

object PrayerTrackerStateDefinition : JsonGlanceStateDefinition<PrayerTrackerWidgetState>(
    fileName = "prayer_tracker_widget",
    serializer = PrayerTrackerWidgetState.serializer(),
    defaultValue = PrayerTrackerWidgetState.Success(PrayerTrackerData()),
    dataLabel = "PrayerTracker",
)
