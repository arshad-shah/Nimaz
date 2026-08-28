package com.arshadshah.nimaz.core.di

import com.arshadshah.nimaz.core.util.PrayerNotificationScheduler
import com.arshadshah.nimaz.domain.repository.PrayerAlarmScheduler
import com.arshadshah.nimaz.domain.repository.PrayerNotificationTester
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The two ports on `PrayerNotificationScheduler`, in the module that owns it.
 *
 * **A binding lives with its implementation** — the rule `DataBindingsModule` in `:core:data`,
 * `SettingsBindingsModule` in `:core:datastore` and `AudioBindingsModule` in `:core:audio` follow.
 * These were the last two entries in `:app`'s `RepositoryModule` that named a class, which leaves
 * it holding one.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationBindingsModule {

    /**
     * The Android `AlarmManager` scheduler behind the domain's [PrayerAlarmScheduler] port.
     * Callers that still hold the concrete type (`AppInitializer`, `PrayerRescheduler`, the
     * instrumented scheduler test) are unaffected — only the domain use case depends on the
     * interface, which is the point.
     */
    @Binds
    @Singleton
    abstract fun bindPrayerAlarmScheduler(
        impl: PrayerNotificationScheduler
    ): PrayerAlarmScheduler

    /**
     * The same class behind the domain's [PrayerNotificationTester] port. Two interfaces, one
     * implementation: `sendTestNotification` posts a notification now, which is not what a
     * scheduler does, so the seams are separate even though the Android class is not.
     */
    @Binds
    @Singleton
    abstract fun bindPrayerNotificationTester(
        scheduler: PrayerNotificationScheduler,
    ): PrayerNotificationTester
}
