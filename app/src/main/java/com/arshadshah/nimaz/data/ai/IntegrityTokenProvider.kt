package com.arshadshah.nimaz.data.ai

import android.content.Context
import com.arshadshah.nimaz.BuildConfig
import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Fetches a Play Integrity token for the current request. Uses the standard
 * request (cloud project number from [BuildConfig.PLAY_INTEGRITY_CLOUD_PROJECT_NUMBER]).
 *
 * If a token cannot be obtained (project number not configured yet, Play
 * services unavailable, offline) the behaviour depends on the build type:
 *  - debug builds fall back to the literal `"debug-skip"` token, which the
 *    Worker honours ONLY when it runs with `SKIP_ATTESTATION=true`;
 *  - release builds return an empty token, which the Worker rejects with
 *    ATTESTATION_FAILED.
 */
@Singleton
class IntegrityTokenProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    suspend fun getToken(): String {
        val projectNumber = BuildConfig.PLAY_INTEGRITY_CLOUD_PROJECT_NUMBER
        // Placeholder project number → Integrity isn't wired up yet.
        if (projectNumber <= 0L) return debugFallback()

        return try {
            requestToken(projectNumber)
        } catch (e: Exception) {
            CrashReporter.recordException(e)
            debugFallback()
        }
    }

    private suspend fun requestToken(projectNumber: Long): String =
        suspendCancellableCoroutine { cont ->
            val manager = IntegrityManagerFactory.create(context)
            manager
                .requestIntegrityToken(
                    IntegrityTokenRequest.builder()
                        .setCloudProjectNumber(projectNumber)
                        .build()
                )
                .addOnSuccessListener { response ->
                    if (cont.isActive) cont.resume(response.token())
                }
                .addOnFailureListener { e ->
                    if (cont.isActive) {
                        CrashReporter.recordException(e)
                        cont.resume(debugFallback())
                    }
                }
        }

    private fun debugFallback(): String =
        if (BuildConfig.DEBUG) "debug-skip" else ""
}
