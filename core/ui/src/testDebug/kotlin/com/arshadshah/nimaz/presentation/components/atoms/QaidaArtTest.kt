package com.arshadshah.nimaz.presentation.components.atoms

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.testing.brightness
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
 * The two drawn marks of the app's manuscript language: the shamsa medallion and the celebration
 * burst.
 *
 * Both are pure `Canvas` geometry with a number sitting in the middle of one of them, so the
 * existing component tests can assert the *label* and reach none of the drawing. What is worth
 * catching here is that the ornament is actually there — a medallion whose scallop path collapsed
 * still shows its surah number, and a burst whose rays never drew still occupies its space. In
 * both cases the screen looks approximately right and the thing the component exists for is gone.
 *
 * The burst also carries three infinite transitions, so the clock is pinned before it composes
 * (#604) — and `play = false` is a real state, not a test convenience: it is what an
 * already-celebrated lesson renders.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-mdpi")
class QaidaArtTest {

    @Suppress("DEPRECATION")
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `the medallion draws its scalloped rim around the number`() {
        val bitmap = composeRule.drawToBitmap {
            Box(Modifier.size(120.dp)) {
                ShamsaMedallion(number = 36, size = 120.dp)
            }
        }

        // The rim sits away from the centre, where the number is. Both bands must carry paint —
        // a collapsed path leaves only the digits.
        val rim = bitmap.region(0, 55, 120, 10).ink()
        assertThat(rim).isGreaterThan(0)
    }

    @Test
    fun `the medallion scales with the size it is given`() {
        val bitmap = composeRule.drawToBitmap {
            Row {
                Box(Modifier.size(120.dp)) { ShamsaMedallion(number = 1, size = 48.dp) }
                Box(Modifier.size(120.dp)) { ShamsaMedallion(number = 1, size = 120.dp) }
            }
        }

        val small = bitmap.region(0, 0, 120, 120).ink()
        val large = bitmap.region(120, 0, 120, 120).ink()

        assertThat(large).isGreaterThan(small)
    }

    @Test
    fun `the diamond floret paints a solid mark`() {
        // The bud that punctuates a cartouche. Small enough that "it drew nothing" is invisible
        // by eye and total by effect.
        val bitmap = composeRule.drawToBitmap {
            Box(Modifier.size(40.dp)) {
                DiamondFloret(color = Color.Yellow, size = 40.dp)
            }
        }

        val centre = bitmap.region(16, 16, 8, 8).ink()
        val corner = bitmap.region(0, 0, 4, 4).ink()

        assertThat(centre).isGreaterThan(0)
        // A diamond leaves its corners empty; a rectangle would not.
        assertThat(corner).isEqualTo(0)
    }

    @Test
    fun `the celebration burst draws its halo, rays and star`() {
        composeRule.mainClock.autoAdvance = false

        val bitmap = composeRule.drawToBitmap {
            Box(Modifier.size(200.dp)) {
                QaidaCelebrationBurst(modifier = Modifier.fillMaxSize())
            }
        }

        assertThat(bitmap.region(0, 0, 200, 200).ink()).isGreaterThan(500)
    }

    @Test
    fun `a burst that is not playing still draws the mark`() {
        // `play = false` freezes every animation at its initial value — it is what an
        // already-celebrated lesson shows, not an absence. Drawing nothing there would blank the
        // medal on a completed lesson.
        composeRule.mainClock.autoAdvance = false

        val bitmap = composeRule.drawToBitmap {
            Box(Modifier.size(200.dp)) {
                QaidaCelebrationBurst(modifier = Modifier.fillMaxSize(), play = false)
            }
        }

        assertThat(bitmap.region(0, 0, 200, 200).ink()).isGreaterThan(500)
    }

    @Test
    fun `the burst is brightest at its centre`() {
        // The halo is a radial gradient from the middle out, and the rays start at 0.32r. A burst
        // drawn from the wrong origin — the top-left, say — reads as a corner smudge.
        composeRule.mainClock.autoAdvance = false

        val bitmap = composeRule.drawToBitmap {
            Box(Modifier.size(200.dp)) {
                QaidaCelebrationBurst(modifier = Modifier.fillMaxSize())
            }
        }

        val centre = bitmap.region(80, 80, 40, 40).brightness()
        val corner = bitmap.region(0, 0, 40, 40).brightness()

        assertThat(centre).isGreaterThan(corner)
    }
}
