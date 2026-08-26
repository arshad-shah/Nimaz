package com.arshadshah.nimaz.presentation.components.atoms.qibla

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.testing.brightness
import com.arshadshah.nimaz.testing.distinctColours
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
 * The compass dial, drawn rather than composed.
 *
 * `CompassPrimitivesTest` already proves each of these composables does not throw. That is worth
 * having and it reaches almost none of the file: every ring, tick, needle and glyph is inside a
 * `Canvas` lambda, and composing the tree runs the `Canvas(modifier)` call and not the block.
 *
 * The claims worth pinning here are directional, and each of them is a bug a user would act on:
 * **the qibla needle points where the qibla is**, so a sign flipped in `drawNeedle`'s
 * `sin`/`cos` pair sends somebody to pray facing the opposite way, drawing a perfectly convincing
 * dial while it does it. **North is a separate, shorter hand**, so the two are distinguishable at a
 * glance. **Facing turns the needle green**, which is the only confirmation the screen gives.
 *
 * One draw per test — `setContent` may only be called once on a rule (#604) — so an
 * "at 0° versus at 90°" comparison puts both dials in one composition.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-mdpi")
class CompassPrimitivesArtTest {

    @Suppress("DEPRECATION")
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `the dial face paints its ring of ticks`() {
        // 72 notches every 5°, of which every sixth is a major. A loop that stepped wrong would
        // draw a dial with the wrong number of marks and no test would notice.
        val bitmap = composeRule.drawToBitmap {
            Box(Modifier.size(300.dp)) { CompassDialFace(Modifier.fillMaxSize()) }
        }

        assertThat(bitmap.region(0, 0, 300, 300).ink()).isGreaterThan(1_000)
    }

    @Test
    fun `the dial's ticks come in two weights`() {
        // Majors are longer, thicker and brighter than minors — that is what makes the cardinal
        // and 30° marks readable. One colour for both is a dial you cannot read a bearing off.
        val bitmap = composeRule.drawToBitmap {
            Box(Modifier.size(300.dp)) { CompassDialFace(Modifier.fillMaxSize()) }
        }

        // A band just inside the rim crosses both weights.
        val rim = bitmap.region(0, 4, 300, 12)
        assertThat(rim.distinctColours()).isAtLeast(3)
    }

    @Test
    fun `the rings are drawn round the edge, not across the middle`() {
        val bitmap = composeRule.drawToBitmap {
            Box(Modifier.size(300.dp)) { CompassRings(Modifier.fillMaxSize()) }
        }

        val edge = bitmap.region(0, 148, 300, 4).ink()
        val middle = bitmap.region(140, 140, 20, 20).ink()

        assertThat(edge).isGreaterThan(0)
        assertThat(middle).isEqualTo(0)
    }

    @Test
    fun `the direction markers put north at the top and south at the bottom`() {
        // `sin`/`cos` with the screen's inverted y — the single place a compass gets mirrored.
        // North is also drawn larger and bolder than the other three, which is why the top band
        // must carry more ink than the sides.
        val bitmap = composeRule.drawToBitmap {
            Box(Modifier.size(300.dp)) { DirectionMarkers(Modifier.fillMaxSize()) }
        }

        val top = bitmap.region(120, 30, 60, 40).ink()
        val bottom = bitmap.region(120, 230, 60, 40).ink()
        val leftBand = bitmap.region(0, 120, 60, 60).ink()

        assertThat(top).isGreaterThan(0)
        assertThat(bottom).isGreaterThan(0)
        assertThat(leftBand).isGreaterThan(0)
        assertThat(top).isGreaterThan(leftBand)
    }

    @Test
    fun `the qibla needle points where the qibla is`() {
        // The claim the whole screen rests on. Two dials, same size, qibla at 0° and at 180°: the
        // needle is longer in front than behind, so the half it occupies flips with the bearing.
        // A sign error in `drawNeedle` draws an equally convincing dial pointing the wrong way.
        val bitmap = composeRule.drawToBitmap {
            Column {
                Box(Modifier.size(300.dp)) {
                    CompassNeedles(
                        qiblaScreenAngle = 0f,
                        northScreenAngle = 0f,
                        isFacingQibla = false,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Box(Modifier.size(300.dp)) {
                    CompassNeedles(
                        qiblaScreenAngle = 180f,
                        northScreenAngle = 0f,
                        isFacingQibla = false,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        // Upper half of each dial against its lower half.
        val upNorth = bitmap.region(0, 0, 300, 150).ink()
        val upSouth = bitmap.region(0, 150, 300, 150).ink()
        val downNorth = bitmap.region(0, 300, 300, 150).ink()
        val downSouth = bitmap.region(0, 450, 300, 150).ink()

        assertThat(upNorth).isGreaterThan(upSouth)
        assertThat(downSouth).isGreaterThan(downNorth)
    }

    @Test
    fun `the north hand turns independently of the qibla hand`() {
        // Two hands on one pivot. A dial that drew both from one angle would look right whenever
        // the user happened to face the qibla and be wrong every other second.
        val bitmap = composeRule.drawToBitmap {
            Column {
                Box(Modifier.size(300.dp)) {
                    CompassNeedles(
                        qiblaScreenAngle = 0f,
                        northScreenAngle = 90f,
                        isFacingQibla = false,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Box(Modifier.size(300.dp)) {
                    CompassNeedles(
                        qiblaScreenAngle = 0f,
                        northScreenAngle = 270f,
                        isFacingQibla = false,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        val eastLeft = bitmap.region(0, 100, 100, 100).ink()
        val eastRight = bitmap.region(200, 100, 100, 100).ink()
        val westLeft = bitmap.region(0, 400, 100, 100).ink()
        val westRight = bitmap.region(200, 400, 100, 100).ink()

        assertThat(eastRight).isGreaterThan(eastLeft)
        assertThat(westLeft).isGreaterThan(westRight)
    }

    @Test
    fun `facing the qibla repaints the needle and lights the glyph`() {
        // `isFacingQibla` swaps the needle's colour to green *and* turns the Kaaba glyph's halo on
        // — two decisions on one flag. It is the only confirmation the screen gives that the user
        // has found the direction, so a flag read in one place and not the other is a dial that
        // half-agrees with itself.
        val bitmap = composeRule.drawToBitmap {
            Row {
                Box(Modifier.size(200.dp)) {
                    CompassNeedles(
                        qiblaScreenAngle = 0f,
                        northScreenAngle = 0f,
                        isFacingQibla = false,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Box(Modifier.size(200.dp)) {
                    CompassNeedles(
                        qiblaScreenAngle = 0f,
                        northScreenAngle = 0f,
                        isFacingQibla = true,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        val seeking = bitmap.region(0, 0, 200, 200)
        val facing = bitmap.region(200, 0, 200, 200)

        // The halo strictly adds paint, and green and gold differ in luminance.
        assertThat(facing.ink()).isGreaterThan(seeking.ink())
        assertThat(facing.brightness()).isNotEqualTo(seeking.brightness())
    }

    @Test
    fun `the lubber notch marks the top of the dial`() {
        // The fixed mark the user aims at. Drawn at the top of its own box, so all of its ink is
        // in the first few rows — a notch centred vertically would be a compass with no index.
        val bitmap = composeRule.drawToBitmap {
            Box(Modifier.size(200.dp, 60.dp)) { CompassLubberNotch(Modifier.fillMaxSize()) }
        }

        val top = bitmap.region(0, 0, 200, 20).ink()
        val bottom = bitmap.region(0, 40, 200, 20).ink()

        assertThat(top).isGreaterThan(bottom)
    }

    @Test
    fun `the facing glow paints only when it is visible`() {
        val bitmap = composeRule.drawToBitmap {
            Column {
                Box(Modifier.size(150.dp)) {
                    CompassFacingGlow(visible = true, modifier = Modifier.fillMaxSize())
                }
                Box(Modifier.size(150.dp)) {
                    CompassFacingGlow(visible = false, modifier = Modifier.fillMaxSize())
                }
            }
        }

        assertThat(bitmap.region(0, 0, 150, 150).ink()).isGreaterThan(0)
        assertThat(bitmap.region(0, 150, 150, 150).ink()).isEqualTo(0)
    }

    @Test
    fun `the centre dot grows and fills when the user is facing the qibla`() {
        // 20dp outlined against 28dp filled with a mosque glyph — the smallest of the three
        // confirmations, and the one closest to where the user is looking.
        val bitmap = composeRule.drawToBitmap {
            Row {
                Box(Modifier.size(60.dp)) { CompassCenterDot(isFacingQibla = false) }
                Box(Modifier.size(60.dp)) { CompassCenterDot(isFacingQibla = true) }
            }
        }

        val seeking = bitmap.region(0, 0, 60, 60).ink()
        val facing = bitmap.region(60, 0, 60, 60).ink()

        assertThat(facing).isGreaterThan(seeking)
    }
}
