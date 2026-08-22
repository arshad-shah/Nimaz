package com.arshadshah.nimaz.core.di

import android.content.Context
import com.arshadshah.nimaz.BuildConfig
import com.arshadshah.nimaz.data.ai.AiApiClient
import com.arshadshah.nimaz.data.ai.IntegrityTokenProvider
import com.arshadshah.nimaz.data.repository.AiRepositoryImpl
import com.arshadshah.nimaz.domain.repository.AiRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides
    @Singleton
    @Named("aiJson")
    fun provideAiJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    @Named("aiHttpClient")
    fun provideAiHttpClient(@Named("aiJson") json: Json): HttpClient =
        HttpClient(OkHttp) {
            expectSuccess = false
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 30_000
                socketTimeoutMillis = 30_000
            }
            // Wire logging only for debug builds — release builds stay silent.
            if (BuildConfig.DEBUG) {
                install(Logging) { level = LogLevel.INFO }
            }
        }

    @Provides
    @Singleton
    fun provideAiApiClient(
        @Named("aiHttpClient") client: HttpClient,
        @Named("aiJson") json: Json,
    ): AiApiClient = AiApiClient(
        client = client,
        baseUrl = BuildConfig.AI_WORKER_BASE_URL,
        json = json,
    )

    /**
     * The two `BuildConfig` values `IntegrityTokenProvider` needs are supplied here, for the same
     * reason `provideAiApiClient` above supplies `AI_WORKER_BASE_URL`: a library module's
     * `BuildConfig` carries only its own fields, so `:core:data` cannot read the app's. Defaulting
     * them there would be worse than a compile error — a wrong project number fails the Worker's
     * integrity check on every Ask-with-Proof call.
     */
    @Provides
    @Singleton
    fun provideIntegrityTokenProvider(
        @ApplicationContext context: Context,
    ): IntegrityTokenProvider = IntegrityTokenProvider(
        context = context,
        cloudProjectNumber = BuildConfig.PLAY_INTEGRITY_CLOUD_PROJECT_NUMBER,
        isDebugBuild = BuildConfig.DEBUG,
    )

    @Provides
    @Singleton
    fun provideAiRepository(impl: AiRepositoryImpl): AiRepository = impl
}
