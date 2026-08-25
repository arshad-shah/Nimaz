package com.arshadshah.nimaz.presentation.components.organisms.qibla

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.CompassAccuracy
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The compass view's two layouts, and the three things it decides for itself.
 *
 * `CompassQiblaViewTest` renders it and checks it does not throw. This asserts what it *shows* —
 * which is not the same question, because everything interesting here is conditional:
 *
 * - **The phone/tablet split** is chosen from the window size class, and the two branches are
 *   near-duplicates. A tablet branch that drops the instruction row or the facts row is a
 *   regression only a wide viewport can see, and every other test in this module runs on a phone.
 * - **`needsCalibration` is derived here**, not passed in: `UNRELIABLE || LOW`. A magnetometer
 *   reporting `LOW` and no banner offering to fix it is a compass that is quietly wrong.
 * - **`hasQiblaInfo` gates the readouts**, so that a compass with no location still draws its
 *   dial rather than showing a bearing of zero as though it meant north.
 */
@RunWith(RobolectricTestRunner::class)
class CompassQiblaViewLayoutTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private var calibrations = 0

    private fun str(@StringRes id: Int): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id)

    private fun render(
        isFacingQibla: Boolean = false,
        rotationToQibla: Float = 12f,
        isCompassReady: Boolean = true,
        accuracy: CompassAccuracy = CompassAccuracy.HIGH,
        hasQiblaInfo: Boolean = true,
    ) {
        composeRule.setThemedContent {
            CompassQiblaView(
                qiblaBearing = 119f,
                animatedAzimuth = 30f,
                isFacingQibla = isFacingQibla,
                rotationToQibla = rotationToQibla,
                isCompassReady = isCompassReady,
                accuracy = accuracy,
                hasQiblaInfo = hasQiblaInfo,
                onCalibrate = { calibrations++ },
            )
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `on a phone the readouts sit under the dial`() {
        render(isFacingQibla = true)

        composeRule.onNodeWithText(str(R.string.facing_qibla)).assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w1000dp-h1200dp-mdpi")
    fun `on a tablet the readouts move beside the dial and none of them are lost`() {
        render(isFacingQibla = true)

        // The wide branch is a second copy of the same children. A copy that forgets one is a
        // tablet-only regression, and nothing else in this module runs wide enough to see it.
        composeRule.onNodeWithText(str(R.string.facing_qibla)).assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w1000dp-h1200dp-mdpi")
    fun `the tablet layout still offers calibration when the compass is unreliable`() {
        render(accuracy = CompassAccuracy.UNRELIABLE)

        composeRule.onNodeWithText(str(R.string.calibrate)).performClick()

        assertThat(calibrations).isEqualTo(1)
    }

    @Test
    fun `a low reading is treated as needing calibration, not just an unreliable one`() {
        render(accuracy = CompassAccuracy.LOW)

        // `UNRELIABLE || LOW`. Narrowing this to `UNRELIABLE` alone leaves the common case — a
        // phone that has simply not been waved yet — pointing somewhere plausible and wrong,
        // with nothing on screen suggesting otherwise.
        composeRule.onNodeWithText(str(R.string.calibrate)).performClick()

        assertThat(calibrations).isEqualTo(1)
    }

    @Test
    fun `a settled, accurate compass is not nagged about calibration`() {
        render(accuracy = CompassAccuracy.HIGH)

        composeRule.onNodeWithText(str(R.string.calibrate)).assertDoesNotExist()
    }

    @Test
    fun `with no location the dial still draws but claims no bearing`() {
        render(hasQiblaInfo = false, isFacingQibla = false)

        // Neither the instruction nor the facts: a bearing of 0° presented as a fact reads as
        // "face north", which is a real direction and the wrong one.
        composeRule.onNodeWithText(str(R.string.finding_direction)).assertDoesNotExist()
        composeRule.onNodeWithText(str(R.string.facing_qibla)).assertDoesNotExist()
    }

    @Test
    fun `while the compass is still finding itself the row says so`() {
        render(hasQiblaInfo = true, isCompassReady = false)

        composeRule.onNodeWithText(str(R.string.finding_direction)).assertIsDisplayed()
    }
}
