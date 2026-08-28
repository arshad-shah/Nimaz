package com.arshadshah.nimaz.presentation.viewmodel.settings

import app.cash.turbine.test
import com.arshadshah.nimaz.domain.model.WorshipReminderType
import com.arshadshah.nimaz.testing.SettingsViewModelHarness
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * The notifications hub, against a setting changed somewhere other than the hub.
 *
 * **The bug this is written for.** `hiltViewModel()` in a destination body is scoped to that
 * destination's `NavBackStackEntry`, so the hub and each of its five subscreens hold *different*
 * `SettingsViewModel` instances. `_notificationState` is loaded once per instance, in
 * `loadSettings`, and nothing re-reads it when a destination is returned to. So switching a
 * worship reminder on updated the subscreen's copy and wrote DataStore — and the hub, which had
 * loaded its copy on the way in, went on reporting the count from before the edit until the app
 * was restarted.
 *
 * It looked like a rendering fault rather than a stale read because **one row was always right**:
 * the prayer row was the only one already reading `notificationSummary`, which collects DataStore
 * directly. Every other row read the snapshot. The fix is to widen the summary to carry all of
 * them, so the asymmetry stops existing.
 *
 * The tests below drive the underlying preference flow rather than sending an event, because that
 * is what the other instance's write reduces to: `SettingsRepositoryStub` keeps its writes on the
 * mockk so `coVerify` still works (see its KDoc), and DataStore is a singleton, so *whichever*
 * instance writes it, every collector in the process sees the same new value. Setting the flow is
 * that, without a second ViewModel to make the point.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationSummaryTest {

    private val dispatcher = StandardTestDispatcher()
    private val harness = SettingsViewModelHarness()
    private val repo get() = harness.repo
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = harness.build()
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    /**
     * `notificationSummary` is `stateIn(…, WhileSubscribed)`, so its upstream does not run until
     * something collects it. Every test below therefore reads through Turbine rather than off
     * `.value` — a `.value` read returns the seed forever and would pass against the bug.
     */

    // ── The rows that were stale ────────────────────────────────────────────────

    @Test
    fun `a worship reminder switched on elsewhere raises the hub's count`() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.notificationSummary.test {
            awaitItem()

            repo.worshipEnabled.getValue(WorshipReminderType.TAHAJJUD.key).value = true
            repo.worshipEnabled.getValue(WorshipReminderType.WITR.key).value = true
            advanceUntilIdle()

            assertThat(expectMostRecentItem().worshipRemindersOn).isEqualTo(2)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `switching one back off lowers it again`() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.notificationSummary.test {
            awaitItem()
            repo.worshipEnabled.getValue(WorshipReminderType.TAHAJJUD.key).value = true
            advanceUntilIdle()
            assertThat(expectMostRecentItem().worshipRemindersOn).isEqualTo(1)

            repo.worshipEnabled.getValue(WorshipReminderType.TAHAJJUD.key).value = false
            advanceUntilIdle()
            assertThat(expectMostRecentItem().worshipRemindersOn).isEqualTo(0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the weekly row follows both of its reminders`() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.notificationSummary.test {
            awaitItem()

            repo.fridayReminderEnabled.value = true
            repo.khatamReminderEnabled.value = true
            advanceUntilIdle()

            val summary = expectMostRecentItem()
            assertThat(summary.fridayReminderEnabled).isTrue()
            assertThat(summary.khatamReminderEnabled).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the sound row follows the adhan, the vibration and the DND choice`() =
        runTest(dispatcher) {
            // Three preferences behind one subtitle, each set on a different screen — the sound on
            // Notification Sound, the other two on the same screen but through different events.
            advanceUntilIdle()

            viewModel.notificationSummary.test {
                awaitItem()

                repo.selectedAdhanSound.value = "ABDULBASIT"
                repo.notificationVibration.value = false
                repo.adhanRespectDnd.value = false
                advanceUntilIdle()

                val summary = expectMostRecentItem()
                assertThat(summary.selectedAdhanSound).isEqualTo("ABDULBASIT")
                assertThat(summary.vibrationEnabled).isFalse()
                assertThat(summary.respectDnd).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `the master switch reads the preference, not a snapshot`() = runTest(dispatcher) {
        // The hub gates every row below it on this one. Read from a snapshot, a master switch
        // turned off anywhere else leaves the hub showing a full list of live settings.
        advanceUntilIdle()

        viewModel.notificationSummary.test {
            awaitItem()

            repo.prayerNotificationsEnabled.value = false
            advanceUntilIdle()

            assertThat(expectMostRecentItem().notificationsMasterEnabled).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── The contrast that explains the fix ──────────────────────────────────────

    @Test
    fun `the one-shot snapshot does not see the same change`() = runTest(dispatcher) {
        // Not a defect in `notificationState` — it is what a snapshot is, and the optimistic
        // updates that make a toggle feel instant depend on it staying one. It is only wrong as
        // a source for a *screen that did not make the edit*, which is the whole finding.
        advanceUntilIdle()

        viewModel.notificationSummary.test {
            awaitItem()
            repo.worshipEnabled.getValue(WorshipReminderType.TAHAJJUD.key).value = true
            advanceUntilIdle()

            assertThat(expectMostRecentItem().worshipRemindersOn).isEqualTo(1)
            assertThat(viewModel.notificationState.value.worshipReminders.count { it.value })
                .isEqualTo(0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── The floor ───────────────────────────────────────────────────────────────

    @Test
    fun `every worship reminder type is counted, not a hand-written subset`() =
        runTest(dispatcher) {
            // The summary iterates `WorshipReminderType.entries`. A reminder added to the enum
            // and forgotten here would be switchable and uncounted; this fails the day that
            // happens rather than the day someone notices the number is short.
            advanceUntilIdle()

            viewModel.notificationSummary.test {
                awaitItem()

                WorshipReminderType.entries.forEach { type ->
                    repo.worshipEnabled.getValue(type.key).value = true
                }
                advanceUntilIdle()

                assertThat(expectMostRecentItem().worshipRemindersOn)
                    .isEqualTo(WorshipReminderType.entries.size)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
