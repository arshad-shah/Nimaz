package com.arshadshah.nimaz.presentation.viewmodel.tracker

import com.arshadshah.nimaz.core.monitoring.RecordingTelemetry
import com.arshadshah.nimaz.core.time.FakeTodayProvider
import com.arshadshah.nimaz.core.util.toUtcMidnightMillis
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.usecase.PrayerUseCases
import com.google.common.truth.Truth.assertThat
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * [PrayerTrackerEvent.SetPrayerStatus] and [PrayerTrackerEvent.ConfirmUnrecordedAsMissed].
 *
 * `SetPrayerStatus` replaces `MarkPrayerPrayed`/`MarkPrayerMissed`, which between them reached
 * only two of the four statuses the app already stores and renders. `ConfirmUnrecordedAsMissed`
 * is the only way a prayer now enters the qada list.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PrayerTrackerViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var prayerUseCases: PrayerUseCases
    private lateinit var viewModel: PrayerTrackerViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        prayerUseCases = mockk<PrayerUseCases>(relaxed = true)

        every { prayerUseCases.getPrayerRecordsForDate(any()) } returns flowOf(emptyList())
        every { prayerUseCases.getPrayerRecordsInRange(any(), any()) } returns flowOf(emptyList())
        every { prayerUseCases.getMissedPrayersRequiringQada() } returns flowOf(emptyList())
        every { prayerUseCases.getCurrentLocation() } returns flowOf(null)

        viewModel = PrayerTrackerViewModel(
            prayerUseCases,
            FakeTodayProvider(LocalDate.of(2026, 8, 13)),
            RecordingTelemetry()
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `SetPrayerStatus writes the status the user chose and stamps prayedAt for a fulfilled prayer`() =
        runTest {
            val prayedAt = slot<Long>()

            viewModel.onEvent(
                PrayerTrackerEvent.SetPrayerStatus(PrayerName.ASR, PrayerStatus.LATE)
            )
            advanceUntilIdle()

            coVerify {
                prayerUseCases.updatePrayerStatus(
                    any(), PrayerName.ASR, PrayerStatus.LATE, capture(prayedAt), false
                )
            }
            // LATE is a fulfilled prayer, so it must be timestamped — the sibling test below
            // pins the other half (MISSED must NOT be). Asserting non-null here, rather than
            // `any()`, means deleting the PRAYED/LATE timestamp branch fails this test.
            assertThat(prayedAt.captured).isNotNull()
        }

    @Test
    fun `SetPrayerStatus with a null status clears the record back to unrecorded`() = runTest {
        viewModel.onEvent(PrayerTrackerEvent.SetPrayerStatus(PrayerName.ASR, null))
        advanceUntilIdle()

        // Clearing is a write of NOT_PRAYED, not a delete: the derivation treats NOT_PRAYED as
        // absence, so the row reads back as "not recorded" with no new DAO method.
        coVerify {
            prayerUseCases.updatePrayerStatus(
                any(), PrayerName.ASR, PrayerStatus.NOT_PRAYED, null, false
            )
        }
    }

    @Test
    fun `SetPrayerStatus does not stamp prayedAt for an unfulfilled prayer`() = runTest {
        viewModel.onEvent(
            PrayerTrackerEvent.SetPrayerStatus(PrayerName.FAJR, PrayerStatus.MISSED)
        )
        advanceUntilIdle()

        // Paired with the LATE test above, which asserts prayedAt is genuinely non-null: this
        // one asserts it is exactly null for MISSED, so together they pin both halves of
        // "stamped only for a fulfilled prayer".
        coVerify {
            prayerUseCases.updatePrayerStatus(any(), PrayerName.FAJR, PrayerStatus.MISSED, null, false)
        }
    }

    @Test
    fun `ConfirmUnrecordedAsMissed passes the inclusive range it was given`() = runTest {
        val from = LocalDate.of(2026, 8, 6)
        val to = LocalDate.of(2026, 8, 12)

        viewModel.onEvent(PrayerTrackerEvent.ConfirmUnrecordedAsMissed(from, to))
        advanceUntilIdle()

        coVerify {
            prayerUseCases.markUnrecordedAsMissed(
                from.toUtcMidnightMillis(), to.toUtcMidnightMillis()
            )
        }
    }
}
