package com.arshadshah.nimaz.widget.khatam

import com.arshadshah.nimaz.widget.core.JsonGlanceStateDefinition

object KhatamStateDefinition : JsonGlanceStateDefinition<KhatamWidgetState>(
    fileName = "khatam_widget",
    serializer = KhatamWidgetState.serializer(),
    defaultValue = KhatamWidgetState.Success(KhatamWidgetData()),
    dataLabel = "Khatam",
)
