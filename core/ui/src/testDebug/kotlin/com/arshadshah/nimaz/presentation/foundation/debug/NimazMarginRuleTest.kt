package com.arshadshah.nimaz.presentation.foundation.debug

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
 * The hairline rules that show how deep a tree row sits.
 *
 * They are the whole reason the hadith topic tree and the Quran outline read as hierarchies rather
 * than as indented lists — one vertical rule per level, drawn behind the row. Being `drawBehind`
 * they run on the draw pass, so `NimazTreeRowTest` composing rows at depth 0, 1 and 2 proves the
 * rows lay out and reaches none of the drawing.
 *
 * Two things matter and neither is visible from the semantics tree. **`count <= 0` returns the
 * modifier untouched** — a top-level row must draw no rule at all, or every list in the app gains a
 * stray line down its left edge. And **`rtl` mirrors which side they are drawn on**: Arabic is a
 * first-class layout direction here, and rules pinned to the left of a right-to-left tree run
 * through the text instead of beside it.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-mdpi")
class NimazMarginRuleTest {

    @Suppress("DEPRECATION")
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `a top-level row draws no rule, and each level adds one`() {
        val bitmap = composeRule.drawToBitmap {
            Column {
                listOf(0, 1, 3).forEach { depth ->
                    Box(
                        Modifier
                            .size(300.dp, 60.dp)
                            .nimazMarginRules(count = depth, color = Color.White, start = 20.dp)
                    )
                }
            }
        }

        val none = bitmap.region(0, 0, 300, 60).ink()
        val one = bitmap.region(0, 60, 300, 60).ink()
        val three = bitmap.region(0, 120, 300, 60).ink()

        assertThat(none).isEqualTo(0)
        assertThat(one).isGreaterThan(0)
        assertThat(three).isGreaterThan(one)
    }

    @Test
    fun `a right-to-left tree draws its rules on the other side`() {
        // Arabic is a first-class direction here. Rules pinned to the left of an RTL tree run
        // through the text rather than beside it.
        val bitmap = composeRule.drawToBitmap {
            Column {
                Box(
                    Modifier
                        .size(300.dp, 60.dp)
                        .nimazMarginRules(count = 2, color = Color.White, start = 20.dp)
                )
                Box(
                    Modifier
                        .size(300.dp, 60.dp)
                        .nimazMarginRules(
                            count = 2,
                            color = Color.White,
                            start = 20.dp,
                            rtl = true,
                        )
                )
            }
        }

        val ltrLeft = bitmap.region(0, 0, 60, 60).ink()
        val ltrRight = bitmap.region(240, 0, 60, 60).ink()
        val rtlLeft = bitmap.region(0, 60, 60, 60).ink()
        val rtlRight = bitmap.region(240, 60, 60, 60).ink()

        assertThat(ltrLeft).isGreaterThan(ltrRight)
        assertThat(rtlRight).isGreaterThan(rtlLeft)
    }

    @Test
    fun `a caller's own step and width are used`() {
        // The step is what makes two levels read as two levels; a width past a hairline turns the
        // rule into a divider, which is a different mark entirely.
        val bitmap = composeRule.drawToBitmap {
            Column {
                Box(
                    Modifier
                        .size(300.dp, 60.dp)
                        .nimazMarginRules(
                            count = 3,
                            color = Color.White,
                            start = 10.dp,
                            step = 10.dp,
                            width = 1.dp,
                        )
                )
                Box(
                    Modifier
                        .size(300.dp, 60.dp)
                        .nimazMarginRules(
                            count = 3,
                            color = Color.White,
                            start = 10.dp,
                            step = 10.dp,
                            width = 4.dp,
                        )
                )
            }
        }

        val thin = bitmap.region(0, 0, 300, 60).ink()
        val thick = bitmap.region(0, 60, 300, 60).ink()

        assertThat(thick).isGreaterThan(thin)
    }

    @Test
    fun `the tick is hollow until the row is marked`() {
        // The mark that sits on the rule: hollow for a passage not being read, filled for the one
        // that is. It is the only per-row state the outline shows.
        val bitmap = composeRule.drawToBitmap {
            Row {
                Box(Modifier.size(40.dp).background(Color.Black)) {
                    NimazMarginTick(filled = false, accent = Color.Red, ruleColor = Color.Blue)
                }
                Box(Modifier.size(40.dp).background(Color.Black)) {
                    NimazMarginTick(filled = true, accent = Color.Red, ruleColor = Color.Blue)
                }
            }
        }

        val hollow = bitmap.region(0, 0, 40, 40)
        val filled = bitmap.region(40, 0, 40, 40)

        assertThat(hollow.ink()).isGreaterThan(0)
        assertThat(filled.ink()).isGreaterThan(0)
        // Both marks occupy the same disc, so the count of painted pixels is the same either way —
        // the hollow one fills with the surface colour rather than leaving a hole. What separates
        // them is the *centre*: filled takes the accent, hollow takes the surface behind it.
        assertThat(hollow.toSet()).isNotEqualTo(filled.toSet())
    }
}
