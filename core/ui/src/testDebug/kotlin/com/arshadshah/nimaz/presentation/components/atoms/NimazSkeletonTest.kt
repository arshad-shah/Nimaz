package com.arshadshah.nimaz.presentation.components.atoms

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.theme.LocalAnimationsEnabled
import com.arshadshah.nimaz.testing.brightness
import com.arshadshah.nimaz.testing.distinctColours
import com.arshadshah.nimaz.testing.drawToBitmap
import com.arshadshah.nimaz.testing.region
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The loading placeholder, which has deliberately nothing for the semantics tree to find.
 *
 * `NimazSkeleton` sets `clearAndSetSemantics { }` — a placeholder carries no information, and
 * announcing it reads to a screen reader as a stray empty element between real content. That makes
 * every ordinary assertion useless here and the drawing the only observable behaviour, which is
 * fitting: what a skeleton *is* is a shimmer gradient, and "the shimmer stopped shimmering" is
 * exactly the regression a reduce-motion setting can introduce for everyone.
 *
 * So these are pixel tests. `LocalAnimationsEnabled` picks between a three-stop travelling
 * gradient and a flat two-stop fill of the same colour, and the difference between those two is
 * the whole contract.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-mdpi")
class NimazSkeletonTest {

    @Suppress("DEPRECATION")
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `both the shimmering and the reduce-motion block paint the same base`() {
        // `LocalAnimationsEnabled` picks between a three-stop travelling gradient and a two-stop
        // flat fill *of the same base colour*. What must hold either way is that the block is
        // painted: a skeleton that renders as nothing is a loading screen that looks broken
        // rather than busy, and honouring reduce-motion by drawing no placeholder at all is the
        // easy way to get there.
        //
        // Drawn as two blocks in one composition, so "same base" is compared against the other
        // arm rather than against a hard-coded colour that would change with the theme. The
        // highlight's *position* is deliberately not asserted: the sweep's offset is driven by an
        // infinite transition, and a pinned clock leaves it parked at its initial value off the
        // block's left edge — a test that claimed to see the highlight would in fact be reading
        // the base and passing for a shimmer that never moved.
        composeRule.mainClock.autoAdvance = false

        val bitmap = composeRule.drawToBitmap {
            androidx.compose.foundation.layout.Column {
                Box(Modifier.height(60.dp)) {
                    NimazSkeleton(modifier = Modifier.fillMaxWidth().height(60.dp))
                }
                CompositionLocalProvider(LocalAnimationsEnabled provides false) {
                    Box(Modifier.height(60.dp)) {
                        NimazSkeleton(modifier = Modifier.fillMaxWidth().height(60.dp))
                    }
                }
            }
        }

        val shimmering = bitmap.region(0, 30, bitmap.width, 1)
        val flat = bitmap.region(0, 90, bitmap.width, 1)

        assertThat(flat.brightness()).isGreaterThan(0.0)
        assertThat(shimmering.brightness()).isGreaterThan(0.0)
        assertThat(shimmering.brightness()).isWithin(2.0).of(flat.brightness())
    }

    @Test
    fun `a skeleton says nothing to a screen reader`() {
        // `clearAndSetSemantics { }`. Three placeholder lines announcing themselves is three
        // empty elements a reader has to swipe past before reaching the content.
        composeRule.setContent {
            CompositionLocalProvider(LocalAnimationsEnabled provides false) {
                androidx.compose.material3.MaterialTheme { NimazSkeletonText(lines = 3) }
            }
        }

        assertThat(composeRule.onRoot(useUnmergedTree = true).printToString())
            .doesNotContain("ContentDescription")
    }

    @Test
    fun `a text skeleton draws one block per line with the last one short`() {
        // `lastLineFraction` is what makes three grey bars read as a paragraph rather than a
        // table. Measured as paint per row band: the last band must carry less than the first.
        val bitmap = composeRule.drawToBitmap {
            CompositionLocalProvider(LocalAnimationsEnabled provides false) {
                NimazSkeletonText(lines = 3, lineHeight = 12.dp, lastLineFraction = 0.4f)
            }
        }

        val first = bitmap.region(0, 0, bitmap.width, 12).brightness()
        val last = bitmap.region(0, 40, bitmap.width, 12).brightness()

        assertThat(first).isGreaterThan(last)
    }

    @Test
    fun `a row skeleton drops its leading circle when asked`() {
        // The avatar/icon well is the difference between a list-row placeholder and a text one,
        // and a list that shows a circle where its rows have no icon is a layout that jumps when
        // the content arrives.
        val bitmap = composeRule.drawToBitmap {
            CompositionLocalProvider(LocalAnimationsEnabled provides false) {
                androidx.compose.foundation.layout.Column {
                    Box(Modifier.height(70.dp)) { NimazSkeletonRow(showLeading = true) }
                    Box(Modifier.height(70.dp)) { NimazSkeletonRow(showLeading = false) }
                }
            }
        }

        // The leading column is the leftmost 40dp; only the first row paints there.
        val withLeading = bitmap.region(0, 0, 40, 70).brightness()
        val withoutLeading = bitmap.region(0, 70, 40, 70).brightness()

        assertThat(withLeading).isGreaterThan(withoutLeading)
    }

    @Test
    fun `a caller's shape is honoured`() {
        // The circle for an avatar, the 16dp rounding for a card. A skeleton clipped to the wrong
        // shape is a placeholder that does not match what replaces it.
        val bitmap = composeRule.drawToBitmap {
            CompositionLocalProvider(LocalAnimationsEnabled provides false) {
                androidx.compose.foundation.layout.Row {
                    NimazSkeleton(
                        modifier = Modifier.size(60.dp),
                        shape = androidx.compose.foundation.shape.CircleShape,
                    )
                    NimazSkeleton(
                        modifier = Modifier.size(60.dp),
                        shape = RoundedCornerShape(0.dp),
                    )
                }
            }
        }

        // A circle leaves its corners unpainted; a square does not.
        val circleCorner = bitmap.region(0, 0, 4, 4).brightness()
        val squareCorner = bitmap.region(62, 0, 4, 4).brightness()

        assertThat(squareCorner).isGreaterThan(circleCorner)
    }
}
