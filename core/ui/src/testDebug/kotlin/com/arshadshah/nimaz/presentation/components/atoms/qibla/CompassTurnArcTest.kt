package com.arshadshah.nimaz.presentation.components.atoms.qibla

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.testing.drawToBitmap
import com.arshadshah.nimaz.testing.ink
import com.arshadshah.nimaz.testing.region
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The arc that sweeps from twelve o'clock toward the qibla — "turn this far, this way".
 *
 * It is drawn, so nothing about it is reachable from the semantics tree, and the two claims worth
 * making are directional. A **positive sweep goes clockwise** from the top, a negative one
 * anti-clockwise: the sign is the only thing distinguishing "turn right" from "turn left", and an
 * inverted one draws a confident arc pointing the wrong way. And the arc **disappears once the
 * user is facing the qibla** — `isFacingQibla || abs(rotation) < 2f` collapses the sweep to zero,
 * with an early `return@Canvas` below one degree, because an arc that never quite closed would
 * keep asking the user to turn after they had arrived.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-mdpi")
class CompassTurnArcTest {

    @Suppress("DEPRECATION")
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `a right turn sweeps clockwise and a left turn the other way`() {
        val bitmap = composeRule.drawToBitmap {
            Row {
                Box(Modifier.size(200.dp)) {
                    CompassTurnArc(
                        rotationToQibla = 90f,
                        isFacingQibla = false,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Box(Modifier.size(200.dp)) {
                    CompassTurnArc(
                        rotationToQibla = -90f,
                        isFacingQibla = false,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        // A +90° sweep from twelve o'clock covers the top-right quadrant; -90° covers top-left.
        val rightTurnRight = bitmap.region(100, 0, 100, 100).ink()
        val rightTurnLeft = bitmap.region(0, 0, 100, 100).ink()
        val leftTurnRight = bitmap.region(300, 0, 100, 100).ink()
        val leftTurnLeft = bitmap.region(200, 0, 100, 100).ink()

        assertThat(rightTurnRight).isGreaterThan(rightTurnLeft)
        assertThat(leftTurnLeft).isGreaterThan(leftTurnRight)
    }

    @Test
    fun `facing the qibla removes the arc entirely`() {
        // Both halves of the guard: the flag, and the sub-2° dead band that stops the arc
        // flickering back as the phone drifts a degree.
        val bitmap = composeRule.drawToBitmap {
            Row {
                Box(Modifier.size(200.dp)) {
                    CompassTurnArc(
                        rotationToQibla = 90f,
                        isFacingQibla = true,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Box(Modifier.size(200.dp)) {
                    CompassTurnArc(
                        rotationToQibla = 0.5f,
                        isFacingQibla = false,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        assertThat(bitmap.region(0, 0, 200, 200).ink()).isEqualTo(0)
        assertThat(bitmap.region(200, 0, 200, 200).ink()).isEqualTo(0)
    }

    @Test
    fun `a bigger turn draws a longer arc`() {
        // The sweep is the angle, so the amount of paint tracks how far there is to go — which is
        // the whole information the arc carries.
        val bitmap = composeRule.drawToBitmap {
            Row {
                Box(Modifier.size(200.dp)) {
                    CompassTurnArc(
                        rotationToQibla = 30f,
                        isFacingQibla = false,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Box(Modifier.size(200.dp)) {
                    CompassTurnArc(
                        rotationToQibla = 270f,
                        isFacingQibla = false,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        val small = bitmap.region(0, 0, 200, 200).ink()
        val large = bitmap.region(200, 0, 200, 200).ink()

        assertThat(large).isGreaterThan(small)
    }
}
