package com.arshadshah.nimaz.domain.repository

import com.arshadshah.nimaz.domain.model.BugReportSubmission
import com.arshadshah.nimaz.domain.model.BugSubmitResult

/**
 * Submits user bug reports to the Firebase backend (Cloud Firestore + Storage,
 * scoped by Anonymous Auth). Implementations must handle the offline case
 * gracefully and never lose a report.
 */
interface BugReportRepository {

    /**
     * Persists [submission] to the `bug_reports` collection and uploads any
     * screenshot to Cloud Storage.
     *
     * @return a [BugSubmitResult] on success (with [BugSubmitResult.queuedOffline]
     * set when the write was persisted locally to sync later), or a failure when
     * the backend is unavailable in this build / variant.
     */
    suspend fun submit(submission: BugReportSubmission): Result<BugSubmitResult>
}
