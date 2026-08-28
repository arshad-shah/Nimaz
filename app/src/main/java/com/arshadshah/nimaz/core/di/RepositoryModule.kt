package com.arshadshah.nimaz.core.di

import com.arshadshah.nimaz.core.util.PrayerNotificationScheduler
import com.arshadshah.nimaz.data.widget.WorkManagerWidgetRefresher
import com.arshadshah.nimaz.domain.repository.PrayerAlarmScheduler
import com.arshadshah.nimaz.domain.repository.PrayerNotificationTester
import com.arshadshah.nimaz.domain.repository.WidgetRefresher
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The three bindings whose **implementation** is still pinned to `:app`.
 *
 * `RepositoryModule` was 905 lines and bound forty-two pairs from every layer of the app. It was
 * seven after PR 22 of #551 and is three now, each here for a reason the module graph makes
 * visible:
 *
 * | Implementation | Why it cannot move |
 * |---|---|
 * | `WorkManagerWidgetRefresher` | the WorkManager half of the `WidgetRefresher` port |
 * | `PrayerNotificationScheduler` | two ports on one `:app` class |
 *
 * The four audio bindings left with their implementations, to `AudioBindingsModule` in
 * `:core:audio` — `QuranAudioManager` was pinned here by a `PendingIntent` naming `MainActivity`
 * and by `AppR.drawable.ic_stat_nimaz`, and both are gone. The other thirty-five moved earlier;
 * see `DataBindingsModule` in `:core:data` and `SettingsBindingsModule` in `:core:datastore`.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindWidgetRefresher(impl: WorkManagerWidgetRefresher): WidgetRefresher

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
