package com.arshadshah.nimaz.widget.hijricalendar

import com.arshadshah.nimaz.widget.core.JsonGlanceStateDefinition

object HijriCalendarStateDefinition : JsonGlanceStateDefinition<HijriCalendarWidgetState>(
    fileName = "hijri_calendar_widget",
    serializer = HijriCalendarWidgetState.serializer(),
    defaultValue = HijriCalendarWidgetState.Success(HijriCalendarData()),
    dataLabel = "HijriCalendar",
)
