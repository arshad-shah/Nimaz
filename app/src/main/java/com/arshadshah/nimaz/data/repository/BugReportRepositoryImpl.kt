package com.arshadshah.nimaz.data.repository

import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.arshadshah.nimaz.domain.model.BugReportSubmission
import com.arshadshah.nimaz.domain.model.BugSubmitResult
import com.arshadshah.nimaz.domain.repository.BugReportRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase-backed [BugReportRepository].
 *
 * The Firebase handles are nullable and injected via guarded providers, so on
 * builds without `google-services.json` (where Firebase is not initialized) this
 * repository reports a clean failure instead of crashing — matching the rest of
 * the app's guarded-no-op approach to Firebase.
 */
@Singleton
class BugReportRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore?,
    private val storage: FirebaseStorage?,
    private val auth: FirebaseAuth?,
) : BugReportRepository {

    override suspend fun submit(submission: BugReportSubmission): Result<BugSubmitResult> {
        val firestore = firestore
            ?: return Result.failure(IllegalStateException("Firebase is not available in this build"))

        return try {
            ensureSignedIn()

            val docRef = firestore.collection(COLLECTION).document()
            val reportId = docRef.id

            // Upload any screenshot first so we can store its path on the doc.
            // Cloud Storage has no offline cache, so bound the attempt and fall
            // back to submitting the text-only report rather than blocking.
            val screenshotPath = submission.screenshotUri?.let { uri ->
                withTimeoutOrNull(SCREENSHOT_UPLOAD_TIMEOUT_MS) {
                    runCatching { uploadScreenshot(reportId, uri) }.getOrNull()
                }
            }

            val data = buildMap<String, Any> {
                put("category", submission.category.firestoreValue)
                put("description", submission.description)
                put("stepsToReproduce", submission.stepsToReproduce)
                put("contactEmail", submission.contactEmail)
                auth?.currentUser?.uid?.let { put("uid", it) }
                submission.diagnostics?.let { put("diagnostics", it.toMap()) }
                screenshotPath?.let { put("screenshotPath", it) }
                put("createdAt", FieldValue.serverTimestamp())
            }

            // Firestore persists the write to the local cache immediately and syncs
            // when online. The returned Task only completes on server ack, so we
            // bound the wait: completion means synced, a timeout means it is safely
            // queued offline and will sync later — both are a successful submit.
            val setTask = docRef.set(data)
            val syncedOnline = try {
                withTimeout(WRITE_SYNC_TIMEOUT_MS) {
                    setTask.await()
                    true
                }
            } catch (_: TimeoutCancellationException) {
                false
            }

            Result.success(BugSubmitResult(reportId = reportId, queuedOffline = !syncedOnline))
        } catch (e: Exception) {
            CrashReporter.recordException(e)
            Result.failure(e)
        }
    }

    /**
     * Signs in anonymously so security rules can scope the write. If the device
     * is offline and there is no cached user, the sign-in is kicked off without
     * blocking — the queued Firestore write attaches the auth token once it syncs.
     */
    private suspend fun ensureSignedIn() {
        val auth = auth ?: return
        if (auth.currentUser != null) return
        runCatching {
            withTimeout(AUTH_TIMEOUT_MS) { auth.signInAnonymously().await() }
        }
    }

    /** Uploads the screenshot under `bug_reports/{reportId}/` and returns its path. */
    private suspend fun uploadScreenshot(reportId: String, uri: android.net.Uri): String {
        val storage = storage ?: throw IllegalStateException("Storage unavailable")
        val ref = storage.reference.child("$COLLECTION/$reportId/screenshot.jpg")
        ref.putFile(uri).await()
        return ref.path
    }

    private companion object {
        const val COLLECTION = "bug_reports"
        const val AUTH_TIMEOUT_MS = 10_000L
        const val SCREENSHOT_UPLOAD_TIMEOUT_MS = 30_000L
        const val WRITE_SYNC_TIMEOUT_MS = 8_000L
    }
}
