package com.arshadshah.nimaz.widget.nextprayer

import com.arshadshah.nimaz.widget.core.JsonGlanceStateDefinition

object NextPrayerStateDefinition : JsonGlanceStateDefinition<NextPrayerWidgetState>(
    fileName = "next_prayer_widget",
    serializer = NextPrayerWidgetState.serializer(),
    defaultValue = NextPrayerWidgetState.Success(NextPrayerData()),
    dataLabel = "NextPrayer",
)
