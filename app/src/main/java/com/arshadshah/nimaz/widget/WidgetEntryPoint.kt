package com.arshadshah.nimaz.widget

import com.arshadshah.nimaz.data.local.database.dao.PrayerDao
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun prayerDao(): PrayerDao
}
