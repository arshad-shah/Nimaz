package com.arshadshah.nimaz.presentation.foundation.geometry

import androidx.activity.ComponentActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.testing.drawToBitmap
import com.arshadshah.nimaz.testing.ink
import com.arshadshah.nimaz.testing.region
import com.google.common.truth.Truth.assertThat
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The shared qibla draw helpers, executed rather than compiled.
 *
 * `drawKaabaGlyph`, `drawBeam` and `drawArcToKaaba` are `DrawScope` extensions with no composable
 * state and no semantics — the Kaaba cube is eight hand-plotted quads in a 64-unit box, and the
 * beam and the off-screen arc are gradients positioned against the canvas size. Nothing about them
 * is reachable from the semantics tree, so they sat at 0% while the qibla screens above them were
 * tested: composing a `Canvas` runs the call and not the lambda.
 *
 * What these pin is *where the drawing lands*. `:feature:prayer` (#620) makes the same point from
 * the screen's side — "the beam lands where the qibla is, not in the middle of the screen" — and
 * this is the geometry that has to hold it up. A sign flipped in `drawArcToKaaba`'s `bulge`, or a
 * `pointRight` read the wrong way, sends the user to turn the wrong way and draws perfectly.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-mdpi")
class QiblaGeometryTest {

    @Suppress("DEPRECATION")
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `the kaaba glyph draws around the centre it is given`() {
        val bitmap = composeRule.drawToBitmap {
            Box(Modifier.fillMaxSize()) {
                Canvas(Modifier.fillMaxSize()) {
                    drawKaabaGlyph(
                        center = Offset(size.width / 2f, size.height / 2f),
                        size = 120f,
                        color = Color.Yellow,
                    )
                }
            }
        }

        val band = 40
        val middle = bitmap.region(0, bitmap.height / 2 - band / 2, bitmap.width, band)
        val top = bitmap.region(0, 0, bitmap.width, band)

        assertThat(middle.ink()).isGreaterThan(0)
        assertThat(middle.ink()).isGreaterThan(top.ink())
    }

    @Test
    fun `the glow is optional and adds paint around the cube`() {
        // Two glyphs, same size and colour, one with the halo and one without, drawn side by side
        // because a rule takes one `setContent`. The halo is a radial gradient behind the cube, so
        // it strictly adds painted pixels — `glow = false` skipping the wrong block makes these
        // equal.
        val bitmap = composeRule.drawToBitmap {
            androidx.compose.foundation.layout.Row {
                Box(Modifier.size(200.dp)) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawKaabaGlyph(
                            center = Offset(size.width / 2f, size.height / 2f),
                            size = 80f,
                            color = Color.Yellow,
                            glow = true,
                        )
                    }
                }
                Box(Modifier.size(200.dp)) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawKaabaGlyph(
                            center = Offset(size.width / 2f, size.height / 2f),
                            size = 80f,
                            color = Color.Yellow,
                            glow = false,
                        )
                    }
                }
            }
        }

        val pane = bitmap.width / 2
        val glowing = bitmap.region(0, 0, pane, bitmap.height).ink()
        val bare = bitmap.region(pane, 0, bitmap.width - pane, bitmap.height).ink()

        assertThat(glowing).isGreaterThan(bare)
    }

    @Test
    fun `the beam rises at the x it is handed`() {
        // The beam is the qibla's position on screen. Drawn a quarter of the way across, the left
        // half of the canvas has to carry it and the right half must not — which is exactly the
        // failure #620 describes from the screen's side.
        val bitmap = composeRule.drawToBitmap {
            Box(Modifier.fillMaxSize()) {
                Canvas(Modifier.fillMaxSize()) {
                    drawBeam(x = size.width * 0.25f, color = Color.Yellow, isFacing = false)
                }
            }
        }

        val half = bitmap.width / 2
        val left = bitmap.region(0, 0, half, bitmap.height).ink()
        val right = bitmap.region(half, 0, bitmap.width - half, bitmap.height).ink()

        assertThat(left).isGreaterThan(right)
    }

    @Test
    fun `facing the qibla lights the floor under the beam`() {
        // `isFacing` widens the beam and adds two glow pools where it meets the ground. Both
        // states drawn at the same x in two stacked panes, so the difference is the flag.
        val bitmap = composeRule.drawToBitmap {
            androidx.compose.foundation.layout.Column {
                Box(Modifier.size(300.dp)) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawBeam(x = size.width / 2f, color = Color.Yellow, isFacing = true)
                    }
                }
                Box(Modifier.size(300.dp)) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawBeam(x = size.width / 2f, color = Color.Yellow, isFacing = false)
                    }
                }
            }
        }

        // mdpi is pinned in @Config, so each 300dp pane is exactly 300px tall and the split
        // lands on the boundary rather than a few rows either side of it.
        val pane = 300
        val facing = bitmap.region(0, 0, bitmap.width, pane).ink()
        val seeking = bitmap.region(0, pane, bitmap.width, pane).ink()

        assertThat(facing).isGreaterThan(seeking)
    }

    @Test
    fun `the off-screen arc hugs the edge it points at`() {
        val bitmap = composeRule.drawToBitmap {
            Box(Modifier.fillMaxSize()) {
                Canvas(Modifier.fillMaxSize()) {
                    drawArcToKaaba(pointRight = true, color = Color.Yellow)
                }
            }
        }

        val third = bitmap.width / 3
        val leftThird = bitmap.region(0, 0, third, bitmap.height).ink()
        val rightThird = bitmap.region(bitmap.width - third, 0, third, bitmap.height).ink()

        assertThat(rightThird).isGreaterThan(leftThird)
    }

    @Test
    fun `pointing left mirrors the arc`() {
        val bitmap = composeRule.drawToBitmap {
            Box(Modifier.fillMaxSize()) {
                Canvas(Modifier.fillMaxSize()) {
                    drawArcToKaaba(pointRight = false, color = Color.Yellow)
                }
            }
        }

        val third = bitmap.width / 3
        val leftThird = bitmap.region(0, 0, third, bitmap.height).ink()
        val rightThird = bitmap.region(bitmap.width - third, 0, third, bitmap.height).ink()

        assertThat(leftThird).isGreaterThan(rightThird)
    }

    @Test
    fun `coordinates are formatted with their hemispheres`() {
        Locale.setDefault(Locale.UK)

        assertThat(formatCoordinates(51.5074, -0.1278)).isEqualTo("51.5074°N, 0.1278°W")
        assertThat(formatCoordinates(-33.8688, 151.2093)).isEqualTo("33.8688°S, 151.2093°E")
    }

    @Test
    fun `the equator and the prime meridian read as north and east`() {
        // `>= 0` on both, so zero is N and E rather than S and W. A `>` would print "0.0000°S".
        Locale.setDefault(Locale.UK)

        assertThat(formatCoordinates(0.0, 0.0)).isEqualTo("0.0000°N, 0.0000°E")
    }
}
