package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.arshadshah.nimaz.core.common.mapItems
import com.arshadshah.nimaz.data.local.database.dao.IslamicEventDao
import com.arshadshah.nimaz.data.local.database.entity.IslamicEventEntity
import com.arshadshah.nimaz.domain.model.IslamicEvent
import com.arshadshah.nimaz.domain.model.IslamicEventType
import com.arshadshah.nimaz.domain.repository.IslamicEventRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IslamicEventRepositoryImpl @Inject constructor(
    private val islamicEventDao: IslamicEventDao
) : IslamicEventRepository {

    override fun getAllEvents(): Flow<List<IslamicEvent>> {
        return islamicEventDao.getAllEvents().mapItems { it.toDomain() }
    }
}

private fun IslamicEventEntity.toDomain(): IslamicEvent {
    return IslamicEvent(
        id = id.toString(),
        nameArabic = nameArabic,
        nameEnglish = nameEnglish,
        description = description,
        hijriMonth = hijriMonth,
        hijriDay = hijriDay,
        eventType = try {
            IslamicEventType.valueOf(eventType.uppercase())
        } catch (e: IllegalArgumentException) {
            CrashReporter.recordException(e)
            IslamicEventType.HOLIDAY
        },
        isHoliday = isHoliday == 1,
        isFastingDay = eventType.equals("fast", ignoreCase = true),
        isNightOfPower = eventType.equals("night", ignoreCase = true),
        gregorianDate = null,
        year = null,
        notes = null,
        priority = 0
    )
}
