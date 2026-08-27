package com.arshadshah.nimaz.presentation.foundation.tokens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
 * The khatam ring and bar, drawn rather than composed.
 *
 * `KhatamVisualsTest` asserts the percentage label and the announcement. Neither reaches the arc
 * and the fill: both live in `drawBehind`, which runs on the draw pass and not on composition, so
 * the geometry that *is* the progress indicator was untested while its caption was pinned.
 *
 * The claim worth making is that the sweep tracks the number beside it. A ring showing "72%" over
 * a track with no arc on it, or a bar whose fill ignores its fraction, is a reading that
 * contradicts itself — and the khatam feature's entire job is telling the reader how far through
 * the Quran they are.
 *
 * `if (sweep > 0f)` / `if (animatedProgress > 0f)` are the arms that skip the fill entirely, which
 * is what a khatam looks like on the day it is created.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-mdpi")
class KhatamProgressDrawTest {

    @Suppress("DEPRECATION")
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `the ring's arc grows with the progress it reports`() {
        // Three rings of the same size at 0, 38% and 100%. An arc that ignored its sweep would
        // paint the same amount in all three while the captions read differently.
        val bitmap = composeRule.drawToBitmap {
            Row {
                Box(Modifier.size(100.dp)) {
                    KhatamProgressRing(progress = 0f, size = 100.dp, animated = false)
                }
                Box(Modifier.size(100.dp)) {
                    KhatamProgressRing(progress = 0.5f, size = 100.dp, animated = false)
                }
                Box(Modifier.size(100.dp)) {
                    KhatamProgressRing(progress = 1f, size = 100.dp, animated = false)
                }
            }
        }

        val empty = bitmap.region(0, 0, 100, 100).ink()
        val half = bitmap.region(100, 0, 100, 100).ink()
        val full = bitmap.region(200, 0, 100, 100).ink()

        // The empty ring still draws its track — a khatam on day one is a visible ring, not a gap.
        assertThat(empty).isGreaterThan(0)
        assertThat(half).isGreaterThan(empty)
        assertThat(full).isGreaterThan(half)
    }

    @Test
    fun `a complete ring is drawn in the completion accent, not the progress one`() {
        // `isComplete` swaps a two-stop gradient for a flat gold. Same sweep either side, so any
        // difference in the pixels is the brush.
        val bitmap = composeRule.drawToBitmap {
            Row {
                Box(Modifier.size(100.dp)) {
                    KhatamProgressRing(progress = 1f, size = 100.dp, animated = false)
                }
                Box(Modifier.size(100.dp)) {
                    KhatamProgressRing(
                        progress = 1f,
                        size = 100.dp,
                        isComplete = true,
                        animated = false,
                    )
                }
            }
        }

        val inProgress = bitmap.region(0, 0, 100, 100)
        val complete = bitmap.region(100, 0, 100, 100)

        assertThat(complete.toSet()).isNotEqualTo(inProgress.toSet())
    }

    @Test
    fun `a thicker stroke paints more of the ring`() {
        val bitmap = composeRule.drawToBitmap {
            Row {
                Box(Modifier.size(100.dp)) {
                    KhatamProgressRing(
                        progress = 0.7f, size = 100.dp, strokeWidth = 4.dp, animated = false,
                    )
                }
                Box(Modifier.size(100.dp)) {
                    KhatamProgressRing(
                        progress = 0.7f, size = 100.dp, strokeWidth = 14.dp, animated = false,
                    )
                }
            }
        }

        assertThat(bitmap.region(100, 0, 100, 100).ink())
            .isGreaterThan(bitmap.region(0, 0, 100, 100).ink())
    }

    @Test
    fun `the bar's fill tracks its fraction`() {
        // Left-anchored, so a bar at 25% must carry paint on its left and its track on the right.
        val bitmap = composeRule.drawToBitmap {
            Column {
                Box(Modifier.fillMaxWidth().height(20.dp)) {
                    KhatamProgressBar(progress = 0.25f, height = 20.dp)
                }
                Box(Modifier.fillMaxWidth().height(20.dp)) {
                    KhatamProgressBar(progress = 1f, height = 20.dp, isComplete = true)
                }
                Box(Modifier.fillMaxWidth().height(20.dp)) {
                    KhatamProgressBar(progress = 0f, height = 20.dp)
                }
            }
        }

        val quarterLeft = bitmap.region(4, 4, 60, 12)
        val quarterRight = bitmap.region(bitmap.width - 64, 4, 60, 12)
        val fullRight = bitmap.region(bitmap.width - 64, 24, 60, 12)
        val emptyLeft = bitmap.region(4, 44, 60, 12)

        // The track is painted everywhere, so both halves have ink; what differs is the colour.
        assertThat(quarterLeft.toSet()).isNotEqualTo(quarterRight.toSet())
        assertThat(fullRight.toSet()).isNotEqualTo(quarterRight.toSet())
        assertThat(emptyLeft.toSet()).isNotEqualTo(quarterLeft.toSet())
    }
}
