package com.arshadshah.nimaz.presentation.components.organisms.qibla

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.CompassAccuracy
import com.arshadshah.nimaz.domain.model.QiblaCalculator
import com.arshadshah.nimaz.domain.model.QiblaInfo
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The AR view's chrome — everything drawn over the camera feed that is *not* the beam.
 *
 * The camera preview itself is not exercised: `ArQiblaView` is the app's only CameraX surface,
 * and binding a provider under Robolectric has no device to bind to. What is exercised is the
 * layer the reader actually reads, and its one real decision:
 *
 * **When the Kaaba is outside the camera's 60° field of view, the overlay has to say so in
 * words.** A beam clamped to the screen edge is indistinguishable from a beam pointing at
 * something just off frame, so someone standing with their back to Mecca would be shown an
 * arrow that looks correct and is 150° wrong. The hint also has to name the *shorter* way round —
 * the sign of `rotationToQibla`, the same sign the compass capsule depends on.
 *
 * The bottom HUD is the other half: it renders only with a resolved location, so an AR view
 * opened before one is known shows the camera and no numbers, rather than a bearing of zero.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp-mdpi")
class ArQiblaViewTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private fun str(@StringRes id: Int): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id)

    private fun info() = QiblaInfo(
        direction = QiblaCalculator.calculateQiblaDirection(53.35, -6.26),
        locationName = "Dublin, Ireland",
        latitude = 53.35,
        longitude = -6.26,
        distanceToMecca = 4_900.0,
    )

    private fun render(
        rotationToQibla: Float,
        isFacingQibla: Boolean = false,
        qiblaInfo: QiblaInfo? = info(),
    ) {
        composeRule.setThemedContent {
            ArQiblaView(
                azimuth = 30f,
                qiblaInfo = qiblaInfo,
                isFacingQibla = isFacingQibla,
                rotationToQibla = rotationToQibla,
                isCompassReady = true,
                compassAccuracy = CompassAccuracy.HIGH,
                animatedAzimuth = 30f,
            )
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `a qibla within the frame needs no turn hint`() {
        render(rotationToQibla = 12f)

        // Inside the 30° half-field the beam is the instruction; adding words on top of it would
        // tell a reader already pointing the right way to keep turning.
        composeRule.onNodeWithText(str(R.string.qibla_turn_right_hint)).assertDoesNotExist()
        composeRule.onNodeWithText(str(R.string.qibla_turn_left_hint)).assertDoesNotExist()
    }

    @Test
    fun `a qibla off to the right says to turn right`() {
        render(rotationToQibla = 95f)

        composeRule.onNodeWithText(str(R.string.qibla_turn_right_hint)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.qibla_turn_left_hint)).assertDoesNotExist()
    }

    @Test
    fun `a qibla off to the left says to turn left`() {
        render(rotationToQibla = -95f)

        // The sign again: a hint that read the magnitude would send everyone the same way, and
        // be right half the time.
        composeRule.onNodeWithText(str(R.string.qibla_turn_left_hint)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.qibla_turn_right_hint)).assertDoesNotExist()
    }

    @Test
    fun `with no location there is neither a hint nor a readout`() {
        render(rotationToQibla = 120f, qiblaInfo = null)

        // `qiblaOffScreen` is gated on `qiblaInfo != null`: without a location the rotation is
        // meaningless, and a "turn right" over a live camera feed is a confident instruction
        // derived from nothing.
        composeRule.onNodeWithText(str(R.string.qibla_turn_right_hint)).assertDoesNotExist()
    }

    @Test
    fun `facing the qibla renders without disturbing the hint state`() {
        render(rotationToQibla = 1f, isFacingQibla = true)

        composeRule.onNodeWithText(str(R.string.qibla_turn_right_hint)).assertDoesNotExist()
    }
}
