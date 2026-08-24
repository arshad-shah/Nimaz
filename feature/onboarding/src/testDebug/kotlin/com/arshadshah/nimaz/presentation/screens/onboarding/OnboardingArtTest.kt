package com.arshadshah.nimaz.presentation.screens.onboarding

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The onboarding artwork, drawn for real and read back off a bitmap.
 *
 * **These are the first pixels anyone sees of the app**, and there is no image asset behind them
 * to eyeball in a resource folder — the mihrab, the four emblems and the khatam band are all
 * `Canvas` geometry, so "the arch renders" is a claim only a draw pass can settle. A `when` that
 * fell through to the wrong emblem, or a `return@Canvas` lost from the shield branch, compiles
 * and previews fine and ships a first-run screen with the wrong drawing on it.
 *
 * The draw pass is the whole technique here. Composing the tree runs the `Canvas(modifier)` call
 * and nothing inside its `DrawScope` lambda, which is where every line of this file lives, so the
 * test asks the `ComposeView` to draw itself into a software `android.graphics.Canvas`: Compose's
 * `RenderNodeLayer` invokes the draw block directly rather than replaying a render node when the
 * canvas it is handed is not hardware-accelerated. `NATIVE` graphics because the assertions are
 * about *pixels* — under Robolectric's legacy shadow canvas every draw is a no-op and the bitmap
 * would come back uniformly blank whether the art worked or not.
 *
 * `captureToImage()` is the route this does **not** take: it goes through `PixelCopy` on a real
 * window and hangs under Robolectric (see #604).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp")
class OnboardingArtTest {

    // The v1 rule, as everywhere else in the repo: `createComponentComposeRule()` cannot be used
    // here because these tests need the activity's view to draw, and migrating the whole codebase
    // to the v2 rule swaps the test dispatcher and is a separate change.
    @Suppress("DEPRECATION")
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /** Composes [content] over a black field, then draws the result into a bitmap. */
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

    private fun Bitmap.rows(top: Int, count: Int): IntArray =
        IntArray(width * count).also { getPixels(it, 0, width, 0, top, width, count) }

    private fun Bitmap.columns(left: Int, count: Int): IntArray =
        IntArray(count * height).also { getPixels(it, 0, count, left, 0, count, height) }

    /** Pixels that are not the black backdrop — i.e. paint the art actually laid down. */
    private fun IntArray.ink(): Int = count { it != android.graphics.Color.BLACK }

    /** Mean luminance, for comparing how strongly a region was painted. */
    private fun IntArray.brightness(): Double = map {
        (android.graphics.Color.red(it) + android.graphics.Color.green(it) +
            android.graphics.Color.blue(it)) / 3.0
    }.average()

    private fun emblemBands(): List<IntArray> {
        val kinds = OnboardingEmblem.entries
        val bitmap = draw {
            Column(modifier = Modifier.fillMaxSize()) {
                kinds.forEach { kind ->
                    OnboardingEmblem(
                        kind = kind,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                }
            }
        }
        val band = bitmap.height / kinds.size
        return kinds.indices.map { bitmap.rows(top = it * band, count = band) }
    }

    @Test
    fun `every emblem paints something`() {
        // A kind that drew nothing is the failure this catches: the page still lays out, the
        // titles and the feature list still render, and the user gets an empty rectangle where
        // the mihrab should be.
        emblemBands().forEach { assertThat(it.ink()).isGreaterThan(0) }
    }

    @Test
    fun `each emblem draws its own artwork`() {
        // The four kinds share one Canvas and are told apart by a `when`. If a branch fell
        // through — or the shield's `return@Canvas` were dropped, which would leave it drawing
        // the mihrab arch as well — two pages would show the same picture. Comparing the drawn
        // pixels is the only way to see that; both versions type-check.
        val bands = emblemBands()

        bands.indices.forEach { a ->
            (a + 1 until bands.size).forEach { b ->
                assertThat(bands[a]).isNotEqualTo(bands[b])
            }
        }
    }

    @Test
    fun `the emblem is letterboxed rather than stretched when its box is too tall`() {
        // Art is authored in a 116x150 box and scaled by `min(w/116, h/150)`. Take the height
        // as the limit instead of the smaller of the two and the mihrab is drawn wider than its
        // arch is tall — a squashed arch, on a portrait phone, on first launch.
        val bitmap = draw { OnboardingEmblem(OnboardingEmblem.MOSQUE, Modifier.fillMaxSize()) }
        val margin = bitmap.height / 8

        assertThat(bitmap.rows(top = 0, count = margin).ink()).isEqualTo(0)
        assertThat(bitmap.rows(top = bitmap.height - margin, count = margin).ink()).isEqualTo(0)
        // ...and it is genuinely drawn in between, so the blank margins are not a blank screen.
        assertThat(bitmap.rows(top = bitmap.height / 2, count = 1).ink()).isGreaterThan(0)
    }

    @Test
    @Config(qualifiers = "w1000dp-h400dp")
    fun `the emblem stays centred when its box is too wide`() {
        // The other half of the same arithmetic: `ox = (width - 116*scale) / 2`. Drop the
        // halving and the art hugs the left edge of a tablet-width page.
        val bitmap = draw { OnboardingEmblem(OnboardingEmblem.QURAN, Modifier.fillMaxSize()) }
        val margin = bitmap.width / 8

        assertThat(bitmap.columns(left = 0, count = margin).ink()).isEqualTo(0)
        assertThat(bitmap.columns(left = bitmap.width - margin, count = margin).ink()).isEqualTo(0)
        assertThat(bitmap.columns(left = bitmap.width / 2, count = 1).ink()).isGreaterThan(0)
    }

    @Test
    fun `the khatam band tiles the full width`() {
        // The band is a `while (x < size.width)` over a 34dp tile. A loop that ran once — or one
        // that stopped at the authored width rather than the measured one — leaves the motif in
        // the top-left corner and bare background across the rest of the header.
        val bitmap = draw { KhatamBand(Modifier.fillMaxWidth().height(96.dp)) }
        val strip = bitmap.width / 10

        assertThat(bitmap.columns(left = 0, count = strip).ink()).isGreaterThan(0)
        assertThat(bitmap.columns(left = bitmap.width - strip, count = strip).ink())
            .isGreaterThan(0)
    }

    @Test
    fun `the khatam band fades downwards into the background`() {
        // `fade = 1 - y/height` is what lets the band sit under the page title without competing
        // with it. Inverted or dropped, the motif reads as a solid gold bar across the top.
        val bitmap = draw { KhatamBand(Modifier.fillMaxWidth().height(96.dp)) }
        val slice = bitmap.height / 4

        val top = bitmap.rows(top = 0, count = slice).brightness()
        val bottom = bitmap.rows(top = bitmap.height - slice, count = slice).brightness()

        assertThat(top).isGreaterThan(bottom)
    }

    @Test
    fun `the illuminated field is a vertical gradient, not a flat fill`() {
        // Three stops, top to bottom. A brush that collapsed to one colour is the difference
        // between the depth the intro is designed around and a flat teal rectangle.
        val bitmap = draw { Box(Modifier.fillMaxSize().background(illuminatedBackground)) }

        val top = bitmap.rows(top = 0, count = 1).brightness()
        val middle = bitmap.rows(top = bitmap.height / 2, count = 1).brightness()
        val bottom = bitmap.rows(top = bitmap.height - 1, count = 1).brightness()

        assertThat(top).isNotEqualTo(middle)
        assertThat(middle).isNotEqualTo(bottom)
    }
}
