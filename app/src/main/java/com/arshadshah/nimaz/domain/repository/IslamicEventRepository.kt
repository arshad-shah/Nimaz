package com.arshadshah.nimaz.domain.repository

import com.arshadshah.nimaz.domain.model.IslamicEvent
import kotlinx.coroutines.flow.Flow

interface IslamicEventRepository {
    fun getAllEvents(): Flow<List<IslamicEvent>>
}
