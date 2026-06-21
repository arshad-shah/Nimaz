package com.arshadshah.nimaz.widget.hijridate

import com.arshadshah.nimaz.widget.core.JsonGlanceStateDefinition

object HijriDateStateDefinition : JsonGlanceStateDefinition<HijriDateWidgetState>(
    fileName = "hijri_date_widget",
    serializer = HijriDateWidgetState.serializer(),
    defaultValue = HijriDateWidgetState.Success(HijriDateData()),
    dataLabel = "HijriDate",
)
