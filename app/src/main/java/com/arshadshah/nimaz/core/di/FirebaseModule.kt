package com.arshadshah.nimaz.core.di

import com.arshadshah.nimaz.data.repository.BugReportRepositoryImpl
import com.arshadshah.nimaz.domain.repository.BugReportRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the Firebase services that back the in-app bug report form.
 *
 * Each provider is guarded with [runCatching] and returns null when Firebase is
 * not initialized — for example debug / PR-check builds that ship without
 * `google-services.json`. Consumers handle the null by failing gracefully, so
 * these builds compile and run without a Firebase project attached.
 *
 * App Check (Play Integrity) is the recommended abuse guard for these
 * unauthenticated-but-anonymous writes; enable it in the Firebase console and add
 * the `firebase-appcheck-playintegrity` dependency to harden against scripted spam.
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore? =
        runCatching { FirebaseFirestore.getInstance() }.getOrNull()

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage? =
        runCatching { FirebaseStorage.getInstance() }.getOrNull()

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth? =
        runCatching { FirebaseAuth.getInstance() }.getOrNull()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BugReportRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindBugReportRepository(
        impl: BugReportRepositoryImpl
    ): BugReportRepository
}
