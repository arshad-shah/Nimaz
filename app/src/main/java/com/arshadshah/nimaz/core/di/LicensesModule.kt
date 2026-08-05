package com.arshadshah.nimaz.core.di

import com.arshadshah.nimaz.data.repository.LibraryRepositoryImpl
import com.arshadshah.nimaz.domain.repository.LibraryRepository
import com.arshadshah.nimaz.domain.usecase.licenses.GetLibrariesUseCase
import com.arshadshah.nimaz.domain.usecase.licenses.GetLibraryUseCase
import com.arshadshah.nimaz.domain.usecase.licenses.LicensesUseCases
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LicensesModule {

    @Binds
    @Singleton
    abstract fun bindLibraryRepository(impl: LibraryRepositoryImpl): LibraryRepository

    companion object {

        @Provides
        @Singleton
        fun provideLicensesUseCases(repo: LibraryRepository): LicensesUseCases = LicensesUseCases(
            getLibraries = GetLibrariesUseCase(repo),
            getLibrary = GetLibraryUseCase(repo),
        )
    }
}
