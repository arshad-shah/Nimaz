package com.arshadshah.nimaz.widget.core

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration

/**
 * Shared WorkManager plumbing for the widget workers.
 *
 * Each worker used to carry an identical companion object with
 * `enqueuePeriodicWork` / `enqueueImmediateWork` / `cancel`. They now delegate
 * to these helpers and supply only their unique work names and refresh interval.
 */
object WidgetWork {

    inline fun <reified W : CoroutineWorker> enqueuePeriodic(
        context: Context,
        uniqueWorkName: String,
        interval: Duration,
        force: Boolean = false,
    ) {
        val request = PeriodicWorkRequestBuilder<W>(interval).build()
        val policy = if (force) {
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
        } else {
            ExistingPeriodicWorkPolicy.KEEP
        }
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(uniqueWorkName, policy, request)
    }

    inline fun <reified W : CoroutineWorker> enqueueImmediate(
        context: Context,
        uniqueWorkName: String,
    ) {
        val request = OneTimeWorkRequestBuilder<W>().build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(uniqueWorkName, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(context: Context, vararg uniqueWorkNames: String) {
        val manager = WorkManager.getInstance(context)
        uniqueWorkNames.forEach { manager.cancelUniqueWork(it) }
    }
}
