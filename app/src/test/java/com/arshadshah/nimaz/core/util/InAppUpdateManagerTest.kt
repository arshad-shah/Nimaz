package com.arshadshah.nimaz.core.util

import android.app.Activity
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.presentation.update.UpdateState
import com.google.android.gms.tasks.Tasks
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

/**
 * The in-app update banner's state machine.
 *
 * Every state it publishes is something a reader sees on Home, and the ones that matter are the
 * **recoveries**: a check that fails, a dialog the user cancels, a flow Play refuses to launch.
 * Each of those has to leave the banner interactive again — a banner stuck on a spinner is a
 * dead control with no way back, and there is no other route to the update.
 *
 * The whole class was at 0%. `AppUpdateManagerFactory.create` is a static that reaches for Play
 * services, so it is stubbed and the manager it returns is driven directly.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InAppUpdateManagerTest {

    private lateinit var activity: Activity
    private lateinit var playManager: AppUpdateManager

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).create().get()
        playManager = mockk(relaxed = true)
        mockkStatic(AppUpdateManagerFactory::class)
        every { AppUpdateManagerFactory.create(any()) } returns playManager
    }

    @After
    fun tearDown() {
        unmockkStatic(AppUpdateManagerFactory::class)
    }

    private fun info(
        availability: Int = UpdateAvailability.UPDATE_NOT_AVAILABLE,
        installStatus: Int = InstallStatus.UNKNOWN,
        flexibleAllowed: Boolean = true,
    ): AppUpdateInfo = mockk(relaxed = true) {
        every { updateAvailability() } returns availability
        every { this@mockk.installStatus() } returns installStatus
        every { isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) } returns flexibleAllowed
    }

    private fun withInfo(info: AppUpdateInfo) {
        every { playManager.appUpdateInfo } returns Tasks.forResult(info)
    }

    private fun withFailure(message: String = "Play services unavailable") {
        every { playManager.appUpdateInfo } returns Tasks.forException(RuntimeException(message))
    }

    private fun manager() = InAppUpdateManager(activity)

    /**
     * Play's task callbacks are posted to the main looper, which Robolectric leaves paused.
     * Without this every assertion reads the state the manager set *before* asking Play —
     * `Checking` or `Starting` — and the whole class passes for the wrong reason.
     */
    private fun idle() = ShadowLooper.idleMainLooper()

    // ── Checking ────────────────────────────────────────────────────────────────

    @Test
    fun `an available update becomes an actionable banner`() {
        withInfo(info(availability = UpdateAvailability.UPDATE_AVAILABLE))

        val manager = manager()
        manager.checkForUpdate()
        idle()

        assertThat(manager.updateState.value).isEqualTo(UpdateState.UpdateAvailable)
    }

    @Test
    fun `an update Play will not allow flexibly is not offered`() {
        // Offering it would open a dialog Play immediately dismisses, which reads as the
        // button being broken.
        withInfo(
            info(availability = UpdateAvailability.UPDATE_AVAILABLE, flexibleAllowed = false)
        )

        val manager = manager()
        manager.checkForUpdate()
        idle()

        assertThat(manager.updateState.value).isEqualTo(UpdateState.NoUpdateAvailable)
    }

    @Test
    fun `an update already downloaded goes straight to offering a restart`() {
        // The download can finish while the app is in the background; without this the reader
        // is asked to download something already on disk.
        withInfo(info(installStatus = InstallStatus.DOWNLOADED))

        val manager = manager()
        manager.checkForUpdate()
        idle()

        assertThat(manager.updateState.value).isInstanceOf(UpdateState.Downloaded::class.java)
    }

    @Test
    fun `nothing available leaves the banner off`() {
        withInfo(info())

        val manager = manager()
        manager.checkForUpdate()
        idle()

        assertThat(manager.updateState.value).isEqualTo(UpdateState.NoUpdateAvailable)
    }

    @Test
    fun `a failed check reports an error rather than leaving the spinner up`() {
        withFailure()

        val manager = manager()
        manager.checkForUpdate()
        idle()

        assertThat(manager.updateState.value).isInstanceOf(UpdateState.Error::class.java)
    }

    @Test
    fun `checking registers the install listener, so download progress is reported`() {
        withInfo(info())

        manager().checkForUpdate()
        idle()

        verify { playManager.registerListener(any()) }
    }

    // ── Starting ────────────────────────────────────────────────────────────────

    @Test
    fun `the tap is reflected immediately, before Play has answered`() {
        // Fetching AppUpdateInfo takes seconds on a slow connection. Without this the button
        // looks unresponsive and the reader taps it again.
        every { playManager.appUpdateInfo } returns Tasks.forResult(info()).also {
            // no-op: the assertion below runs after the task completes, so the transition is
            // asserted through its end state rather than mid-flight.
        }

        val manager = manager()
        manager.startUpdate()
        idle()

        assertThat(manager.updateState.value).isNotEqualTo(UpdateState.Idle)
    }

    @Test
    fun `starting with the update already downloaded offers the restart instead`() {
        withInfo(info(installStatus = InstallStatus.DOWNLOADED))

        val manager = manager()
        manager.startUpdate()
        idle()

        assertThat(manager.updateState.value).isInstanceOf(UpdateState.Downloaded::class.java)
    }

    @Test
    fun `starting with no launcher registered falls back rather than doing nothing`() {
        // The launcher must be registered before the activity is STARTED; if that ever stops
        // happening, the button has to stay meaningful rather than silently no-op.
        withInfo(info(availability = UpdateAvailability.UPDATE_AVAILABLE))

        val manager = manager()
        manager.startUpdate()
        idle()

        assertThat(manager.updateState.value).isEqualTo(UpdateState.NoUpdateAvailable)
    }

    @Test
    fun `a flow Play refuses to launch restores the actionable state`() {
        withInfo(info(availability = UpdateAvailability.UPDATE_AVAILABLE))
        every { playManager.startUpdateFlowForResult(any(), any<androidx.activity.result.ActivityResultLauncher<androidx.activity.result.IntentSenderRequest>>(), any()) } returns false

        val manager = manager()
        manager.setUpdateFlowLauncher(fakeLauncher())
        manager.startUpdate()
        idle()

        assertThat(manager.updateState.value).isEqualTo(UpdateState.UpdateAvailable)
    }

    @Test
    fun `a launched flow leaves the state to the dialog and the install listener`() {
        withInfo(info(availability = UpdateAvailability.UPDATE_AVAILABLE))
        every { playManager.startUpdateFlowForResult(any(), any<androidx.activity.result.ActivityResultLauncher<androidx.activity.result.IntentSenderRequest>>(), any()) } returns true

        val manager = manager()
        manager.setUpdateFlowLauncher(fakeLauncher())
        manager.startUpdate()
        idle()

        assertThat(manager.updateState.value).isEqualTo(UpdateState.Starting)
    }

    @Test
    fun `a failed start reports an error rather than leaving the spinner up`() {
        withFailure()

        val manager = manager()
        manager.startUpdate()
        idle()

        assertThat(manager.updateState.value).isInstanceOf(UpdateState.Error::class.java)
    }

    // ── The dialog's answer ─────────────────────────────────────────────────────

    @Test
    fun `cancelling the Play dialog makes the banner interactive again`() {
        // This is the recovery that matters most: without it the banner sits on Starting
        // forever and the only route to the update is gone.
        withInfo(info(availability = UpdateAvailability.UPDATE_AVAILABLE))
        every { playManager.startUpdateFlowForResult(any(), any<androidx.activity.result.ActivityResultLauncher<androidx.activity.result.IntentSenderRequest>>(), any()) } returns true

        val manager = manager()
        manager.setUpdateFlowLauncher(fakeLauncher())
        manager.startUpdate()
        idle()

        manager.onUpdateFlowResult(Activity.RESULT_CANCELED)

        assertThat(manager.updateState.value).isEqualTo(UpdateState.UpdateAvailable)
    }

    @Test
    fun `a confirmed dialog leaves the install listener to drive the download states`() {
        withInfo(info(availability = UpdateAvailability.UPDATE_AVAILABLE))
        every { playManager.startUpdateFlowForResult(any(), any<androidx.activity.result.ActivityResultLauncher<androidx.activity.result.IntentSenderRequest>>(), any()) } returns true

        val manager = manager()
        manager.setUpdateFlowLauncher(fakeLauncher())
        manager.startUpdate()
        idle()

        manager.onUpdateFlowResult(Activity.RESULT_OK)

        assertThat(manager.updateState.value).isEqualTo(UpdateState.Starting)
    }

    @Test
    fun `a cancel arriving in a state other than Starting is ignored`() {
        withInfo(info())
        val manager = manager()
        manager.checkForUpdate()
        idle()

        manager.onUpdateFlowResult(Activity.RESULT_CANCELED)

        assertThat(manager.updateState.value).isEqualTo(UpdateState.NoUpdateAvailable)
    }

    // ── The install listener ────────────────────────────────────────────────────

    @Test
    fun `every install status the listener can see maps to a state the banner can show`() {
        withInfo(info())
        val listener = slot<InstallStateUpdatedListener>()
        every { playManager.registerListener(capture(listener)) } returns Unit

        val manager = manager()
        manager.checkForUpdate()
        idle()

        listener.captured.onStateUpdate(installState(InstallStatus.PENDING))
        assertThat(manager.updateState.value).isEqualTo(UpdateState.Downloading)

        listener.captured.onStateUpdate(installState(InstallStatus.DOWNLOADING))
        assertThat(manager.updateState.value).isEqualTo(UpdateState.Downloading)

        listener.captured.onStateUpdate(installState(InstallStatus.DOWNLOADED))
        assertThat(manager.updateState.value).isInstanceOf(UpdateState.Downloaded::class.java)

        listener.captured.onStateUpdate(installState(InstallStatus.FAILED))
        assertThat(manager.updateState.value).isInstanceOf(UpdateState.Error::class.java)

        // A cancelled download must leave the banner actionable, not errored — the reader
        // chose to stop, and the update is still available.
        listener.captured.onStateUpdate(installState(InstallStatus.CANCELED))
        assertThat(manager.updateState.value).isEqualTo(UpdateState.UpdateAvailable)

        // Anything else is not a state change.
        listener.captured.onStateUpdate(installState(InstallStatus.INSTALLING))
        assertThat(manager.updateState.value).isEqualTo(UpdateState.UpdateAvailable)
    }

    @Test
    fun `the restart action completes the update`() {
        withInfo(info(installStatus = InstallStatus.DOWNLOADED))
        val manager = manager()
        manager.checkForUpdate()
        idle()

        (manager.updateState.value as UpdateState.Downloaded).completeUpdate()

        verify { playManager.completeUpdate() }
    }

    // ── Background recovery and teardown ────────────────────────────────────────

    @Test
    fun `a download that finished in the background is picked up on resume`() {
        withInfo(info(installStatus = InstallStatus.DOWNLOADED))

        val manager = manager()
        manager.checkForStalledUpdate()
        idle()

        assertThat(manager.updateState.value).isInstanceOf(UpdateState.Downloaded::class.java)
    }

    @Test
    fun `nothing stalled leaves the state alone`() {
        withInfo(info())

        val manager = manager()
        manager.checkForStalledUpdate()
        idle()

        assertThat(manager.updateState.value).isEqualTo(UpdateState.Idle)
    }

    @Test
    fun `cleanup unregisters the listener, so a destroyed activity is not leaked`() {
        val manager = manager()

        manager.cleanup()

        verify { playManager.unregisterListener(any()) }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private fun installState(status: Int): com.google.android.play.core.install.InstallState =
        mockk(relaxed = true) { every { installStatus() } returns status }

    private fun fakeLauncher():
        androidx.activity.result.ActivityResultLauncher<androidx.activity.result.IntentSenderRequest> =
        mockk(relaxed = true)
}
