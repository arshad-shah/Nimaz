package com.arshadshah.nimaz

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.arshadshah.nimaz.repositories.PrayerTimesRepository
import com.arshadshah.nimaz.repositories.PrayerTrackerRepository
import com.arshadshah.nimaz.widgets.prayertimesthin.PrayerTimeWorker
import com.arshadshah.nimaz.widgets.prayertimestrackerthin.PrayerTimesTrackerWorker
import javax.inject.Inject

class NimazWorkerFactory @Inject constructor(
    private val prayerTimesRepository: PrayerTimesRepository,
    private val prayerTrackerRepository: PrayerTrackerRepository
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            PrayerTimeWorker::class.java.name -> {
                PrayerTimeWorker(appContext, workerParameters, prayerTimesRepository)
            }

            PrayerTimesTrackerWorker::class.java.name -> {
                PrayerTimesTrackerWorker(
                    appContext,
                    workerParameters,
                    prayerTimesRepository,
                    prayerTrackerRepository
                )
            }

            else -> null
        }
    }
}