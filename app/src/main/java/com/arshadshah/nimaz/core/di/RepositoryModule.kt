package com.arshadshah.nimaz.core.di

import com.arshadshah.nimaz.data.widget.WorkManagerWidgetRefresher
import com.arshadshah.nimaz.domain.repository.WidgetRefresher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The one binding whose **implementation** is still pinned to `:app`.
 *
 * `RepositoryModule` was 905 lines and bound forty-two pairs from every layer of the app. It was
 * seven after PR 22 of #551, four once the audio bindings left with `:core:audio`, and is one now
 * that `PrayerNotificationScheduler`'s two ports are `NotificationBindingsModule`'s in
 * `:core:notifications`.
 *
 * `WorkManagerWidgetRefresher` is what remains: the WorkManager half of the `WidgetRefresher`
 * port, and the one implementation with no module of its own to go to — `:core:data` cannot see
 * WorkManager's widget side and `:core:*` may not name `:feature:widget`, so `:app` is where the
 * two meet. **One binding is a fair question about whether this file should exist**, and the
 * answer is that a `@Module` has to live somewhere and the alternative is a file with a different
 * name holding exactly the same thing.
 *
 * The other forty-one are with their implementations: `DataBindingsModule` in `:core:data`,
 * `SettingsBindingsModule` in `:core:datastore`, `AudioBindingsModule` in `:core:audio`,
 * `NotificationBindingsModule` in `:core:notifications`.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindWidgetRefresher(impl: WorkManagerWidgetRefresher): WidgetRefresher
}
