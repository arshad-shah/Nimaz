package com.arshadshah.nimaz.presentation.screens.qibla

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.CompassAccuracy
import com.arshadshah.nimaz.domain.model.CompassData
import com.arshadshah.nimaz.domain.model.QiblaCalculator
import com.arshadshah.nimaz.domain.model.QiblaInfo
import com.arshadshah.nimaz.presentation.components.molecules.NimazErrorKind
import com.arshadshah.nimaz.presentation.viewmodel.UiError
import com.arshadshah.nimaz.presentation.viewmodel.prayer.QiblaEvent
import com.arshadshah.nimaz.presentation.viewmodel.prayer.QiblaUiState
import com.arshadshah.nimaz.presentation.viewmodel.prayer.QiblaViewModel
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The qibla screen's own logic: the compass lifecycle, the error surface, and the camera gate.
 *
 * The compass drawing itself is `CompassQiblaView`'s (tested separately). What is *here* is the
 * wiring around it, and every piece of it fails silently:
 *
 * - **The sensor lifecycle.** `StartCompass`/`StopCompass` are dispatched from a
 *   `DisposableEffect`. A screen that never sends `Stop` leaves the magnetometer registered after
 *   the reader has navigated away — a battery drain nobody can see and nothing else will catch.
 * - **The error surface only shows when there is nothing to draw.** A refresh that fails while a
 *   bearing is already on screen must not replace a working compass with a red message.
 * - **The camera gate.** AR mode is `state.isArMode && cameraPermissionGranted`, held in local
 *   state the ViewModel cannot see, so no ViewModel test can reach it. Getting it wrong points
 *   the camera surface at a permission that was never granted, or strands a reader who denied it
 *   with no explanation.
 */
@RunWith(RobolectricTestRunner::class)
// Density pinned deliberately: `lightZ` is a theme dimension resolved against
// `DisplayMetrics.density`, and a class that inherits its density can have Robolectric's renderer
// setup reject it as `Infinity` and throw out of the compose rule's `before` — before a line of
// this screen runs. See the `forkEvery` note in the module's build file.
@Config(qualifiers = "w411dp-h891dp-mdpi")
class QiblaScreenTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val state = MutableStateFlow(QiblaUiState())
    private val events = mutableListOf<QiblaEvent>()

    private val viewModel: QiblaViewModel = mockk(relaxed = true) {
        every { qiblaState } returns this@QiblaScreenTest.state
        every { onEvent(any()) } answers { events += firstArg<QiblaEvent>() }
    }

    private var backs = 0
    private var activity: ComponentActivity? = null

    /** Drives the screen out of the composition, which is the `onDispose` the sensors hang on. */
    private val onScreen = mutableStateOf(true)

    private fun str(@StringRes id: Int): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id)

    private fun grantCamera() {
        shadowOf(ApplicationProvider.getApplicationContext<Application>())
            .grantPermissions(Manifest.permission.CAMERA)
    }

    private fun render() {
        composeRule.setThemedContent {
            val context = LocalContext.current
            SideEffect { activity = context as? ComponentActivity }
            if (onScreen.value) {
                QiblaScreen(onNavigateBack = { backs++ }, viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()
    }

    private fun pointing(
        bearing: Double = 119.0,
        accuracy: CompassAccuracy = CompassAccuracy.HIGH,
    ) = QiblaUiState(
        qiblaDirection = QiblaCalculator.calculateQiblaDirection(53.35, -6.26),
        qiblaInfo = QiblaInfo(
            direction = QiblaCalculator.calculateQiblaDirection(53.35, -6.26),
            locationName = "Dublin, Ireland",
            latitude = 53.35,
            longitude = -6.26,
            distanceToMecca = 4_900.0,
        ),
        compassData = CompassData(azimuth = bearing.toFloat(), accuracy = accuracy),
        isCompassReady = true,
        isLoading = false,
    )

    @Test
    fun `the compass is started on entry and stopped when the screen leaves`() {
        state.value = pointing()
        render()

        assertThat(events).containsExactly(QiblaEvent.StartCompass)

        // Leaving the screen is a disposal, not a lifecycle callback the ViewModel can observe.
        onScreen.value = false
        composeRule.waitForIdle()

        assertThat(events).contains(QiblaEvent.StopCompass)
    }

    @Test
    fun `with no bearing at all, the error offers a retry and a way to set a location`() {
        state.value = QiblaUiState(
            isLoading = false,
            error = UiError(message = R.string.qibla_no_location, kind = NimazErrorKind.LOCATION),
        )
        render()

        composeRule.onNodeWithText(str(R.string.qibla_no_location)).assertIsDisplayed()

        // Before this the screen showed the sentence and nothing else — no retry, no route to
        // settings, and a compass that could not be recovered from without leaving the screen.
        composeRule.onNodeWithText(str(R.string.try_again)).performClick()
        composeRule.onNodeWithText(str(R.string.location_set_prompt)).performClick()

        assertThat(events).containsAtLeast(
            QiblaEvent.RefreshLocation,
            QiblaEvent.ShowLocationPicker,
        )
    }

    @Test
    fun `an error beside a working bearing does not replace the compass`() {
        state.value = pointing().copy(
            error = UiError(message = R.string.qibla_failed, kind = NimazErrorKind.LOCATION),
        )
        render()

        // A compass already pointing somewhere is more use than a message about a refresh that
        // failed; the error arm is gated on `qiblaInfo == null` for exactly this.
        composeRule.onNodeWithText(str(R.string.qibla_no_location_body)).assertDoesNotExist()
        composeRule.onNodeWithText("Dublin, Ireland").assertIsDisplayed()
    }

    @Test
    fun `the calibrate action opens the sheet and dismissing it is reported`() {
        composeRule.mainClock.autoAdvance = false
        state.value = pointing(accuracy = CompassAccuracy.LOW)
        render()

        composeRule.onNodeWithContentDescription(str(R.string.calibrate_compass)).performClick()
        assertThat(events).contains(QiblaEvent.ShowCalibrationDialog)

        // The sheet is rendered from state, not from a local `remember`, so the dismissal has to
        // travel back or it can never be re-opened.
        state.value = state.value.copy(showCalibrationDialog = true)
        composeRule.mainClock.advanceTimeBy(500)
        composeRule.onNodeWithText(str(R.string.got_it)).performClick()

        assertThat(events).contains(QiblaEvent.DismissCalibrationDialog)
    }

    @Test
    fun `with the camera already granted, the toggle goes straight to AR`() {
        grantCamera()
        state.value = pointing()
        render()

        composeRule.onNodeWithContentDescription(str(R.string.point_with_camera)).performClick()

        assertThat(events).contains(QiblaEvent.SetArMode(true))
    }

    @Test
    fun `without the camera, the toggle asks for it rather than entering AR`() {
        state.value = pointing()
        render()

        composeRule.onNodeWithContentDescription(str(R.string.point_with_camera)).performClick()
        composeRule.waitForIdle()

        val requested = shadowOf(activity!!).lastRequestedPermission
        assertThat(requested.requestedPermissions.toList())
            .contains(Manifest.permission.CAMERA)
        // AR is not entered on the strength of the request — only on the grant.
        assertThat(events).doesNotContain(QiblaEvent.SetArMode(true))
    }

    @Test
    fun `denying the camera explains why AR did not open`() {
        state.value = pointing()
        render()

        composeRule.onNodeWithContentDescription(str(R.string.point_with_camera)).performClick()
        composeRule.waitForIdle()

        val requested = shadowOf(activity!!).lastRequestedPermission
        activity!!.onRequestPermissionsResult(
            requested.requestCode,
            arrayOf(Manifest.permission.CAMERA),
            intArrayOf(android.content.pm.PackageManager.PERMISSION_DENIED),
        )
        composeRule.waitForIdle()

        // A denial that says nothing reads as a broken button.
        composeRule.onNodeWithText(str(R.string.camera_permission_title)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.not_now)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(str(R.string.camera_permission_title)).assertDoesNotExist()
    }

    @Test
    fun `granting the camera from the prompt enters AR`() {
        state.value = pointing()
        render()

        composeRule.onNodeWithContentDescription(str(R.string.point_with_camera)).performClick()
        composeRule.waitForIdle()

        val requested = shadowOf(activity!!).lastRequestedPermission
        grantCamera()
        activity!!.onRequestPermissionsResult(
            requested.requestCode,
            arrayOf(Manifest.permission.CAMERA),
            intArrayOf(android.content.pm.PackageManager.PERMISSION_GRANTED),
        )
        composeRule.waitForIdle()

        assertThat(events).contains(QiblaEvent.SetArMode(true))
    }

    @Test
    fun `AR mode is refused while the permission is not held, whatever state says`() {
        state.value = pointing().copy(isArMode = true)
        render()

        // `isArMode && cameraPermissionGranted`: a state flag alone must not put the camera
        // surface on screen — the top bar still offers to *enter* AR, not to leave it.
        composeRule.onNodeWithContentDescription(str(R.string.point_with_camera))
            .assertIsDisplayed()
    }

    @Test
    fun `in AR mode the toggle offers the way back to the compass`() {
        grantCamera()
        state.value = pointing().copy(isArMode = true)
        render()

        composeRule.onNodeWithContentDescription(str(R.string.back_to_compass)).performClick()

        assertThat(events).contains(QiblaEvent.SetArMode(false))
    }
}
