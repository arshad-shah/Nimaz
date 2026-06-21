package com.arshadshah.nimaz.widget.prayertimes

import com.arshadshah.nimaz.widget.core.JsonGlanceStateDefinition

object PrayerTimesStateDefinition : JsonGlanceStateDefinition<PrayerTimesWidgetState>(
    fileName = "prayer_times_widget",
    serializer = PrayerTimesWidgetState.serializer(),
    defaultValue = PrayerTimesWidgetState.Success(PrayerTimesData()),
    dataLabel = "PrayerTimes",
)
