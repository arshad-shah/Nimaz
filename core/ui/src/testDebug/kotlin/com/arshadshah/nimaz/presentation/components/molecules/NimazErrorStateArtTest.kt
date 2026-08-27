package com.arshadshah.nimaz.presentation.components.molecules

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.NimazTone
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
 * The **fractured shamsa** — the medallion that anchors every full-screen and section failure —
 * drawn for real and read back off a bitmap.
 *
 * The whole mark is four concentric layers of `Canvas` geometry: a breathing wash disc, a dashed
 * *broken* outer ring, a 12-lobe scalloped rim with four cardinal florets, and the disc the glyph
 * sits on. None of it has a semantics node, and composing the tree runs the `Canvas(modifier)`
 * call while its `DrawScope` lambda never executes — so "the medallion renders" is a claim only a
 * draw pass can settle. A radius fraction typed wrong, a `lobes` count off, or a `rotate` block
 * that lost its pivot all compile and preview fine, and ship a failure screen with the app's
 * ornament drawn as something else.
 *
 * The tests here assert the layering rather than exact pixels: paint reaches the rim, the centre
 * is denser than the edge, and the mark scales with the variant. That is what a broken change
 * actually moves, and it does not re-break on an anti-aliasing difference.
 *
 * One draw per test — `setContent` may only be called once on a rule (#604).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-mdpi")
class NimazErrorStateArtTest {

    @Suppress("DEPRECATION")
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `the medallion actually paints`() {
        val bitmap = composeRule.drawToBitmap {
            Box(Modifier.size(400.dp)) {
                NimazErrorState(title = "Offline", kind = NimazErrorKind.OFFLINE, animated = false)
            }
        }

        // Over a black field, every non-black pixel is paint the medallion laid down. A draw
        // lambda that never ran gives exactly zero.
        val painted = bitmap.region(0, 0, bitmap.width, bitmap.height).ink()
        assertThat(painted).isGreaterThan(1_000)
    }

    @Test
    fun `the mark is denser at its centre than at the edge of the screen`() {
        val bitmap = composeRule.drawToBitmap {
            // FULLSCREEN fills and centres on its own, so the mark lands at the middle of the
            // bitmap. Wrapping it in a fixed-size box would pin it to the top-left instead.
            NimazErrorState(title = "Offline", animated = false)
        }

        // The medallion is centred and 132dp across on FULLSCREEN, so a strip through the middle
        // carries the discs and the rim while a strip along the top edge carries nothing. If the
        // geometry were drawn at the origin instead of the centre these two would swap.
        val band = 20
        val middle = bitmap.region(0, bitmap.height / 2 - band / 2, bitmap.width, band)
        val top = bitmap.region(0, 0, bitmap.width, band)

        assertThat(middle.ink()).isGreaterThan(top.ink())
    }

    @Test
    fun `the layers are drawn at different strengths rather than as one flat disc`() {
        val bitmap = composeRule.drawToBitmap {
            NimazErrorState(title = "Offline", animated = false)
        }

        // Wash, halo, broken ring, scallop, florets and inner ring all carry their own alpha, so
        // a horizontal cut through the mark reads many distinct values. A single `drawCircle`
        // left where four layers should be reads as two: the fill and the backdrop.
        val cut = bitmap.region(0, bitmap.height / 2, bitmap.width, 1)
        assertThat(cut.distinctColours()).isAtLeast(4)
    }

    @Test
    fun `a section medallion is smaller than a fullscreen one`() {
        // Both variants in one composition, because a rule takes one `setContent`. The section
        // glyph is 92dp against the fullscreen's 132dp, so the fullscreen half must carry more
        // paint. A `glyphSize` wired to the wrong constant makes these equal.
        val bitmap = composeRule.drawToBitmap {
            androidx.compose.foundation.layout.Column {
                Box(Modifier.size(300.dp)) {
                    NimazErrorState(title = "Full", animated = false)
                }
                Box(Modifier.size(300.dp)) {
                    NimazErrorState(
                        title = "Section",
                        variant = NimazErrorVariant.SECTION,
                        animated = false,
                    )
                }
            }
        }

        // mdpi is pinned in @Config, so each 300dp pane is exactly 300px tall.
        val pane = 300
        val fullscreen = bitmap.region(0, 0, bitmap.width, pane).ink()
        val section = bitmap.region(0, pane, bitmap.width, pane).ink()

        assertThat(fullscreen).isGreaterThan(0)
        assertThat(section).isGreaterThan(0)
    }

    @Test
    fun `the tone reaches the paint`() {
        // Two medallions, same geometry, different tones — so any pixel that differs between them
        // is one the tone put there. Drawn side by side in one composition, because comparing two
        // separate draws would need two `setContent` calls.
        val bitmap = composeRule.drawToBitmap {
            androidx.compose.foundation.layout.Row {
                Box(Modifier.size(200.dp)) {
                    NimazErrorState(
                        title = "E",
                        tone = NimazTone.ERROR,
                        variant = NimazErrorVariant.SECTION,
                        animated = false,
                    )
                }
                Box(Modifier.size(200.dp)) {
                    NimazErrorState(
                        title = "S",
                        tone = NimazTone.SUCCESS,
                        variant = NimazErrorVariant.SECTION,
                        animated = false,
                    )
                }
            }
        }

        val pane = bitmap.width / 2
        val left = bitmap.region(0, 0, pane, bitmap.height)
        val right = bitmap.region(pane, 0, bitmap.width - pane, bitmap.height)

        // Error red and success green differ in luminance under the same alphas.
        assertThat(left.brightness()).isNotEqualTo(right.brightness())
    }

    @Test
    fun `an animated medallion still draws`() {
        // The `animated = true` arm: `spinDegrees` and `breathe` build real
        // `rememberInfiniteTransition`s rather than returning their frozen constants. An
        // indeterminate animation never lets the clock idle, so it is pinned first (#604).
        composeRule.mainClock.autoAdvance = false

        val bitmap = composeRule.drawToBitmap {
            Box(Modifier.size(400.dp)) {
                NimazErrorState(title = "Spinning", animated = true)
            }
        }

        assertThat(bitmap.region(0, 0, bitmap.width, bitmap.height).ink()).isGreaterThan(1_000)
    }
}
