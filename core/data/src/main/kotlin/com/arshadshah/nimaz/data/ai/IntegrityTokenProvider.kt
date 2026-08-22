package com.arshadshah.nimaz.data.ai

import android.content.Context
import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityManager.PrepareIntegrityTokenRequest
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenProvider
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Fetches a Play Integrity token for the current request via the **standard**
 * API (cloud project number from [cloudProjectNumber]).
 *
 * Standard, not classic: classic requests ([com.google.android.play.core.integrity.IntegrityTokenRequest])
 * are throttled per app-instance by Play services after a handful of calls in a
 * short window (TOO_MANY_REQUESTS). Making one per question meant a few asks
 * worked, then every token fetch failed → empty token → the Worker rejecting
 * perfectly legitimate installs. The standard API is
 * built for frequent, per-action checks: one heavier [prepareProvider] warm-up,
 * then cheap [StandardIntegrityTokenProvider.request] calls that Play services
 * serves from its own cache. The Worker decodes both token kinds through the
 * same `decodeIntegrityToken` endpoint, so nothing changes server-side.
 *
 * If a token still cannot be obtained (project number not configured yet, Play
 * services unavailable, offline) the behaviour depends on the build type:
 *  - debug builds fall back to the literal `"debug-skip"` token, which the
 *    Worker honours ONLY when it runs with `SKIP_ATTESTATION=true`;
 *  - release builds return an empty token, which the Worker rejects with
 *    `ATTESTATION_FAILED` → [com.arshadshah.nimaz.domain.model.AiError.Unverified].
 *    A missing token is not "verification could not run" server-side: anyone
 *    could omit it, so the Worker fails closed on it (see
 *    `worker/src/middleware/integrity.ts`). Hence the retry above — an empty
 *    token now costs the user the answer.
 */
/**
 * Constructed by `AiModule` in `:app` rather than by Hilt's `@Inject`, because two of its
 * arguments come from the application's `BuildConfig` — which a library module cannot see. That
 * is the same shape `provideAiApiClient` next to it already uses for `AI_WORKER_BASE_URL`.
 */
class IntegrityTokenProvider(
    private val context: Context,
    /**
     * The Play Integrity cloud project number, from `BuildConfig` — and the reason it is a
     * parameter is that a library module's `BuildConfig` carries only its own fields, never the
     * application's. `com.arshadshah.nimaz.BuildConfig` does not resolve here, and defaulting it
     * would be worse than not compiling: a wrong number means every Ask-with-Proof call fails the
     * Worker's integrity check. `AiModule` in `:app` supplies both of these.
     *
     * Zero or negative means Integrity is not wired up for this build, which is the placeholder
     * case [debugFallback] handles.
     */
    private val cloudProjectNumber: Long,
    /** `BuildConfig.DEBUG`, for the same reason. */
    private val isDebugBuild: Boolean,
) {
    private val providerMutex = Mutex()
    private var cachedProvider: StandardIntegrityTokenProvider? = null

    suspend fun getToken(): String {
        val projectNumber = cloudProjectNumber
        // Placeholder project number → Integrity isn't wired up yet.
        if (projectNumber <= 0L) return debugFallback()

        return try {
            try {
                requestToken(obtainProvider(projectNumber))
            } catch (e: Exception) {
                // The warmed-up provider can go stale (Play services updated or
                // evicted its state) — re-prepare once and retry before giving up.
                invalidateProvider()
                requestToken(obtainProvider(projectNumber))
            }
        } catch (e: Exception) {
            CrashReporter.recordException(e)
            debugFallback()
        }
    }

    private suspend fun obtainProvider(projectNumber: Long): StandardIntegrityTokenProvider =
        providerMutex.withLock {
            cachedProvider ?: prepareProvider(projectNumber).also { cachedProvider = it }
        }

    private suspend fun invalidateProvider() {
        providerMutex.withLock { cachedProvider = null }
    }

    private suspend fun prepareProvider(projectNumber: Long): StandardIntegrityTokenProvider =
        suspendCancellableCoroutine { cont ->
            IntegrityManagerFactory.createStandard(context)
                .prepareIntegrityToken(
                    PrepareIntegrityTokenRequest.builder()
                        .setCloudProjectNumber(projectNumber)
                        .build()
                )
                .addOnSuccessListener { provider ->
                    if (cont.isActive) cont.resume(provider)
                }
                .addOnFailureListener { e ->
                    if (cont.isActive) cont.resumeWithException(e)
                }
        }

    private suspend fun requestToken(provider: StandardIntegrityTokenProvider): String =
        suspendCancellableCoroutine { cont ->
            provider
                .request(StandardIntegrityTokenRequest.builder().build())
                .addOnSuccessListener { response ->
                    if (cont.isActive) cont.resume(response.token())
                }
                .addOnFailureListener { e ->
                    if (cont.isActive) cont.resumeWithException(e)
                }
        }

    private fun debugFallback(): String =
        if (isDebugBuild) "debug-skip" else ""
}
