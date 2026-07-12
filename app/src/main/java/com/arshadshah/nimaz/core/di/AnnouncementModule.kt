package com.arshadshah.nimaz.core.di

import com.arshadshah.nimaz.BuildConfig
import com.arshadshah.nimaz.core.navigation.announcementRoute
import com.arshadshah.nimaz.data.repository.AnnouncementRepositoryImpl
import com.arshadshah.nimaz.domain.repository.AnnouncementRepository
import com.arshadshah.nimaz.domain.usecase.AnnouncementUseCases
import com.arshadshah.nimaz.domain.usecase.DismissAnnouncementUseCase
import com.arshadshah.nimaz.domain.usecase.ObserveActiveAnnouncementUseCase
import com.arshadshah.nimaz.domain.usecase.ResolveAnnouncementRouteUseCase
import com.arshadshah.nimaz.domain.usecase.SetAnnouncementUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AnnouncementModule {

    @Provides
    @Singleton
    fun provideAnnouncementRepository(impl: AnnouncementRepositoryImpl): AnnouncementRepository =
        impl

    @Provides
    @Singleton
    fun provideAnnouncementUseCases(repository: AnnouncementRepository): AnnouncementUseCases =
        AnnouncementUseCases(
            observeActiveAnnouncement = ObserveActiveAnnouncementUseCase(
                repository = repository,
                currentVersionCode = BuildConfig.VERSION_CODE,
            ),
            setAnnouncement = SetAnnouncementUseCase(repository),
            dismissAnnouncement = DismissAnnouncementUseCase(repository),
            resolveAnnouncementRoute = ResolveAnnouncementRouteUseCase(
                isKnownFeatureKey = { key -> announcementRoute(key) != null },
            ),
        )
}
