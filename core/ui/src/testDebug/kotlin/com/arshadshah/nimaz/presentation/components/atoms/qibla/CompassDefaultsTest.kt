package com.arshadshah.nimaz.presentation.components.atoms.qibla

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The compass primitives composed at their defaults.
 *
 * `CompassPrimitivesArtTest` draws each of them into a bitmap, and to do that it has to hand every
 * one a `Modifier` sizing it — which means the *default* call, the one a screen makes when it lets
 * the dial size itself from its parent, is never made. Sizing a `Canvas` to zero is the classic way
 * for a drawn component to disappear, and it does not throw.
 *
 * The needle's and the glow's own colour defaults are the other half: `QiblaGold` and `QiblaGreen`
 * are what the dial uses unless a screen overrides them, and nothing else composes them undecorated.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class CompassDefaultsTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `every primitive composes without being handed a modifier`() {
        composeRule.setThemedContent {
            Column {
                Box(Modifier.size(200.dp)) { CompassDialFace() }
                Box(Modifier.size(200.dp)) { CompassRings() }
                Box(Modifier.size(200.dp)) { DirectionMarkers() }
                Box(Modifier.size(200.dp)) { CompassLubberNotch() }
                Box(Modifier.size(200.dp)) {
                    CompassNeedles(
                        qiblaScreenAngle = 118f,
                        northScreenAngle = 0f,
                        isFacingQibla = false,
                    )
                }
                Box(Modifier.size(60.dp)) { CompassCenterDot(isFacingQibla = false) }
                Box(Modifier.size(60.dp)) { CompassFacingGlow(visible = true) }
                Box(Modifier.size(200.dp)) {
                    CompassTurnArc(rotationToQibla = 45f, isFacingQibla = false)
                }
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun `a screen can override the accent colours the dial defaults to`() {
        // `QiblaGold` while seeking, `QiblaGreen` when facing — the parameters exist so the AR
        // view and the dial can share the primitives while tinting them for their own backdrop.
        composeRule.setThemedContent {
            Column {
                Box(Modifier.size(200.dp)) {
                    CompassNeedles(
                        qiblaScreenAngle = 0f,
                        northScreenAngle = 0f,
                        isFacingQibla = true,
                        modifier = Modifier,
                        goldColor = Color.Magenta,
                    )
                }
                Box(Modifier.size(60.dp)) {
                    CompassCenterDot(
                        isFacingQibla = true,
                        modifier = Modifier,
                        greenColor = Color.Magenta,
                    )
                }
                Box(Modifier.size(60.dp)) {
                    CompassFacingGlow(
                        visible = true,
                        modifier = Modifier,
                        greenColor = Color.Magenta,
                    )
                }
            }
        }

        composeRule.waitForIdle()
    }
}
