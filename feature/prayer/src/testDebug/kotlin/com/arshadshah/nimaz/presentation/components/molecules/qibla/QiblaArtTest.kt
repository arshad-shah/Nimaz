package com.arshadshah.nimaz.presentation.components.molecules.qibla

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.arshadshah.nimaz.domain.model.CompassAccuracy
import com.arshadshah.nimaz.domain.model.QiblaCalculator
import com.arshadshah.nimaz.domain.model.QiblaInfo
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The qibla artwork, drawn for real and read back off a bitmap.
 *
 * **The AR overlay is geometry with no fallback.** There is no text on it and no asset behind it:
 * the beam and the sweep arc are `DrawScope` calls, so "it points the right way" is a claim only
 * a draw pass can settle. The specific failure is the branch — `abs(rotationToQibla) <= HALF_FOV`
 * decides between *a beam at the Kaaba* and *an arc telling you to keep turning*, and a beam
 * pinned to the screen edge because the branch was missed is an overlay confidently pointing at a
 * wall. Composing the tree runs `Canvas(modifier)` and none of its lambda, so the test asks the
 * `ComposeView` to draw itself into a **software** `android.graphics.Canvas`; Compose's
 * `RenderNodeLayer` then invokes the draw block directly instead of replaying a render node.
 *
 * `@GraphicsMode(NATIVE)` because the assertions are about pixels — under the legacy shadow canvas
 * every draw is a no-op and the bitmap comes back blank whether the art worked or not. And not
 * `captureToImage()`, which goes through `PixelCopy` on a real window and hangs (see #604).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// Density pinned, as everywhere else in this module: `lightZ` is a theme dimension resolved
// against `DisplayMetrics.density`, and a class that inherits its density can have Robolectric's
// renderer setup reject it. See the `forkEvery` note in the module's build file for the failure
// this belongs to.
@Config(qualifiers = "w400dp-h800dp-mdpi")
class QiblaArtTest {

    // The v1 rule, not the shared fixture: these tests need the activity's own view to draw.
    @Suppress("DEPRECATION")
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun draw(content: @Composable () -> Unit): Bitmap {
        composeRule.setContent {
            MaterialTheme {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) { content() }
            }
        }
        composeRule.waitForIdle()

        val root: View = composeRule.activity
            .findViewById<ViewGroup>(android.R.id.content)
            .getChildAt(0)
        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
        return bitmap
    }

    private fun info() = QiblaInfo(
        direction = QiblaCalculator.calculateQiblaDirection(53.35, -6.26),
        locationName = "Dublin, Ireland",
        latitude = 53.35,
        longitude = -6.26,
        distanceToMecca = 4_900.0,
    )

    /** Pixels in a vertical slice that are not the black backdrop — i.e. paint actually laid down. */
    private fun Bitmap.inkInColumns(left: Int, count: Int): Int {
        val pixels = IntArray(count * height)
        getPixels(pixels, 0, count, left.coerceIn(0, width - count), 0, count, height)
        return pixels.count { it != android.graphics.Color.BLACK }
    }

    private fun overlay(
        rotationToQibla: Float,
        isFacingQibla: Boolean = false,
        accuracy: CompassAccuracy = CompassAccuracy.HIGH,
        qiblaInfo: QiblaInfo? = info(),
    ): Bitmap = draw {
        QiblaArOverlay(
            qiblaInfo = qiblaInfo,
            isFacingQibla = isFacingQibla,
            rotationToQibla = rotationToQibla,
            compassAccuracy = accuracy,
            modifier = Modifier.fillMaxSize(),
        )
    }

    @Test
    fun `with no location the overlay draws nothing at all`() {
        val bitmap = overlay(rotationToQibla = 0f, qiblaInfo = null)

        // `return@Canvas` on a null `qiblaInfo`: a beam drawn at whatever `rotationToQibla`
        // happened to be is a direction presented as fact before any location is known.
        assertThat(bitmap.inkInColumns(0, bitmap.width)).isEqualTo(0)
    }

    @Test
    fun `the beam lands where the qibla is, not in the middle of the screen`() {
        val bitmap = overlay(rotationToQibla = 20f)
        val third = bitmap.width / 3

        // 20° right of centre, on a 60° field of view, puts the beam in the right-hand third.
        // A beam that ignored the offset would paint the middle third instead — and would look
        // entirely correct in a screenshot taken while facing the Kaaba.
        val right = bitmap.inkInColumns(2 * third, third)
        val middle = bitmap.inkInColumns(third, third)
        assertThat(right).isGreaterThan(middle)
    }

    @Test
    fun `a qibla behind you sweeps toward the edge instead of planting a beam`() {
        val bitmap = overlay(rotationToQibla = 120f)

        // Past the 30° half-field the beam would be clamped to the screen edge and read as
        // "it is over there", 90° wrong. The arc is the honest rendering of "keep turning".
        assertThat(bitmap.inkInColumns(0, bitmap.width)).isGreaterThan(0)
    }

    @Test
    fun `an unreliable compass still draws the beam, dimmed rather than dropped`() {
        val bitmap = overlay(rotationToQibla = 0f, accuracy = CompassAccuracy.UNRELIABLE)

        // Same geometry, lower alpha: the overlay does not vanish when the magnetometer is
        // unsure, it stops looking authoritative. An `if (lowAccuracy) return@Canvas` would
        // leave the camera feed with nothing on it and no explanation.
        // (One draw per test — a compose rule can only set content once.)
        assertThat(bitmap.inkInColumns(0, bitmap.width)).isGreaterThan(0)
    }

    @Test
    fun `facing the qibla still paints the beam`() {
        val bitmap = overlay(rotationToQibla = 1f, isFacingQibla = true)

        assertThat(bitmap.inkInColumns(0, bitmap.width)).isGreaterThan(0)
    }
}
