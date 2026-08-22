package com.arshadshah.nimaz.core.di

import com.arshadshah.nimaz.core.util.PrayerNotificationScheduler
import com.arshadshah.nimaz.data.audio.AyahAudioDownloader
import com.arshadshah.nimaz.data.audio.HttpAyahAudioDownloader
import com.arshadshah.nimaz.data.audio.NextSurahPlaylistSource
import com.arshadshah.nimaz.data.audio.QuranAudioManager
import com.arshadshah.nimaz.data.audio.QuranNextSurahPlaylistSource
import com.arshadshah.nimaz.data.platform.ServiceAdhanDownloader
import com.arshadshah.nimaz.data.widget.WorkManagerWidgetRefresher
import com.arshadshah.nimaz.domain.repository.AdhanDownloader
import com.arshadshah.nimaz.domain.repository.PrayerAlarmScheduler
import com.arshadshah.nimaz.domain.repository.PrayerNotificationTester
import com.arshadshah.nimaz.domain.repository.QuranPlayback
import com.arshadshah.nimaz.domain.repository.WidgetRefresher
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * What is left after PR 22 of #551: the seven bindings whose **implementation** is pinned to
 * `:app`.
 *
 * `RepositoryModule` was 905 lines and bound forty-two pairs from every layer of the app. It is
 * seven now, and each one is here for a reason the module graph makes visible:
 *
 * | Implementation | Why it cannot move |
 * |---|---|
 * | `QuranAudioManager` | `MainActivity` holds one too; behind the `QuranPlayback` port |
 * | `HttpAyahAudioDownloader`, `QuranNextSurahPlaylistSource` | collaborators of the above |
 * | `ServiceAdhanDownloader` | starts `AdhanDownloadService`, a manifest entry point |
 * | `WorkManagerWidgetRefresher` | the WorkManager half of the `WidgetRefresher` port |
 * | `PrayerNotificationScheduler` | one `AppR.drawable.ic_stat_nimaz` line; two ports |
 *
 * The other thirty-five moved to the modules that own their implementations — see
 * `DataBindingsModule` in `:core:data` and `SettingsBindingsModule` in `:core:datastore`.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * The Quran playback seam. `QuranAudioManager` stays in `:app` — it is ExoPlayer-backed and
     * inseparable from `QuranAudioService`, whose notification uses `R.drawable.ic_stat_nimaz`
     * and a content intent aimed at `MainActivity` — while `QuranViewModel` moved to
     * `:feature:quran` in PR 19 of #551 and sees only the port.
     */
    // `QuranAudioManager` is annotated `@UnstableApi` because it holds an ExoPlayer; naming the
    // type in a binding is a use of that API even though nothing here touches media3.
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    @Binds
    @Singleton
    abstract fun bindQuranPlayback(impl: QuranAudioManager): QuranPlayback

    /**
     * The byte transfer behind an ayah download, and only that — see [AyahAudioDownloader].
     * A seam rather than a direct `URL.openConnection()` so the download path in
     * `QuranAudioManager` can be driven by a test without a network.
     */
    @Binds
    @Singleton
    abstract fun bindAyahAudioDownloader(
        impl: HttpAyahAudioDownloader
    ): AyahAudioDownloader

    /**
     * What continuous playback rolls into when a surah ends — the other half of a setting that
     * promised the next surah and could only ever deliver the next verse.
     */
    @Binds
    @Singleton
    abstract fun bindNextSurahPlaylistSource(
        impl: QuranNextSurahPlaylistSource
    ): NextSurahPlaylistSource

    @Binds
    @Singleton
    abstract fun bindAdhanDownloader(impl: ServiceAdhanDownloader): AdhanDownloader

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
