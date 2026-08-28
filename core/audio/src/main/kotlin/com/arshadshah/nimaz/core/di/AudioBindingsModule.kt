package com.arshadshah.nimaz.core.di

import com.arshadshah.nimaz.data.audio.AyahAudioDownloader
import com.arshadshah.nimaz.data.audio.HttpAyahAudioDownloader
import com.arshadshah.nimaz.data.audio.NextSurahPlaylistSource
import com.arshadshah.nimaz.data.audio.QuranAudioManager
import com.arshadshah.nimaz.data.audio.QuranNextSurahPlaylistSource
import com.arshadshah.nimaz.data.platform.ServiceAdhanDownloader
import com.arshadshah.nimaz.domain.repository.AdhanDownloader
import com.arshadshah.nimaz.domain.repository.QuranPlayback
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The four audio bindings, in the module that owns their implementations.
 *
 * **A binding lives with its implementation** — the rule `DataBindingsModule` in `:core:data` and
 * `SettingsBindingsModule` in `:core:datastore` already follow. These four were the last audio
 * entries in `:app`'s `RepositoryModule`, which is down to three: the WorkManager widget refresher
 * and the two ports on `PrayerNotificationScheduler`.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AudioBindingsModule {

    /**
     * The Quran playback seam.
     *
     * `QuranAudioManager` was pinned to `:app` by two things, and both are gone: its service's
     * notification named `MainActivity` in a `PendingIntent` (it resolves the launcher component
     * now, as the widgets do) and drew `R.drawable.ic_stat_nimaz` from the app's `R` (the icon is
     * `:core:ui`'s). `MainActivity` still drives playback, but through [QuranPlayback] like every
     * other caller.
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
}
