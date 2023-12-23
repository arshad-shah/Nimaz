package com.arshadshah.nimaz.data.remote.models

import java.time.LocalDate

data class FastTracker(
    val date: String = LocalDate.now().toString(),
    val isFasting: Boolean = false,
    val isMenstruating: Boolean = false,
)
