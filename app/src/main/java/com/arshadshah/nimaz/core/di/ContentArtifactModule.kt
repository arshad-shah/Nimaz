package com.arshadshah.nimaz.core.di

import com.arshadshah.nimaz.BuildConfig
import com.arshadshah.nimaz.data.local.content.InstalledContentArtifact
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The one thing `DatabaseModule` could not take with it when it moved to `:core:database`.
 *
 * `CONTENT_ARTIFACT_SHA256` is a field of the *application's* `BuildConfig`, generated from the
 * pinned nimaz-data lock file. A library's `BuildConfig` carries only its own fields, so the read
 * stays in `:app` and the value crosses the boundary behind [InstalledContentArtifact].
 */
@Module
@InstallIn(SingletonComponent::class)
object ContentArtifactModule {

    @Provides
    @Singleton
    @InstalledContentArtifact
    fun provideInstalledContentArtifact(): String = BuildConfig.CONTENT_ARTIFACT_SHA256
}
