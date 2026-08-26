package com.arshadshah.nimaz.presentation.components.atoms

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.testing.brightness
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
 * The Qaida's harakat highlighting — the component that colours a vowel mark differently from the
 * letter it sits on.
 *
 * That is the entire teaching device of the Qaida: a learner is shown the *same* letter with a
 * fatha, a kasra and a damma, and the mark is what changes. The colour is chosen by a four-arm
 * `when` over a free-text group name, matched by substring and case-insensitively because the
 * group names arrive from the shipped content rather than from an enum. An arm that stopped
 * matching leaves the mark drawn in the letter's own colour — the lesson still renders, and the
 * thing it is teaching becomes invisible.
 *
 * `playing` outranks all of it, because a cell being recited is inverted onto the accent and the
 * mark has to stay legible against it.
 *
 * The colouring is a `SpanStyle` inside an `AnnotatedString`, which the semantics tree flattens to
 * plain text — so the assertions are on pixels.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-mdpi")
class HarakatColouringTest {

    @Suppress("DEPRECATION")
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /** A bāʾ carrying a fatha — one letter, one mark. */
    private val syllable = "بَ"

    @Test
    fun `a kasra group and a fatha group colour the mark differently`() {
        // Two arms of the `when`, drawn side by side so the difference is the group name and
        // nothing else. Collapsing them into one colour removes the distinction the lesson makes.
        val bitmap = composeRule.drawToBitmap {
            Row {
                Box(Modifier.size(120.dp)) {
                    HarakatArabicText(
                        text = syllable,
                        highlightGroup = "Kasra",
                        baseColor = Color.White,
                    )
                }
                Box(Modifier.size(120.dp)) {
                    HarakatArabicText(
                        text = syllable,
                        highlightGroup = "fatha",
                        baseColor = Color.White,
                    )
                }
            }
        }

        val kasra = bitmap.region(0, 0, 120, 120).brightness()
        val fatha = bitmap.region(120, 0, 120, 120).brightness()

        assertThat(kasra).isNotEqualTo(fatha)
    }

    @Test
    fun `damma shares the fatha colour and an unknown group falls back to the base`() {
        // `damma` is deliberately grouped with `fatha`; anything the content ships that matches
        // neither falls to the base colour rather than to an arbitrary one.
        val bitmap = composeRule.drawToBitmap {
            Row {
                Box(Modifier.size(120.dp)) {
                    HarakatArabicText(
                        text = syllable,
                        highlightGroup = "damma",
                        baseColor = Color.White,
                    )
                }
                Box(Modifier.size(120.dp)) {
                    HarakatArabicText(
                        text = syllable,
                        highlightGroup = "sukun",
                        baseColor = Color.White,
                    )
                }
                Box(Modifier.size(120.dp)) {
                    HarakatArabicText(
                        text = syllable,
                        highlightGroup = null,
                        baseColor = Color.White,
                    )
                }
            }
        }

        val damma = bitmap.region(0, 0, 120, 120).brightness()
        val unknown = bitmap.region(120, 0, 120, 120).brightness()
        val none = bitmap.region(240, 0, 120, 120).brightness()

        assertThat(damma).isNotEqualTo(unknown)
        assertThat(unknown).isWithin(0.5).of(none)
    }

    @Test
    fun `matching a group ignores case and matches inside a longer name`() {
        // `contains(..., ignoreCase = true)` — the group names come from the shipped content, not
        // from an enum, so "Lesson 4 — Kasrah" has to match as readily as "kasra".
        val bitmap = composeRule.drawToBitmap {
            Row {
                Box(Modifier.size(120.dp)) {
                    HarakatArabicText(
                        text = syllable,
                        highlightGroup = "Lesson 4 - KASRA marks",
                        baseColor = Color.White,
                    )
                }
                Box(Modifier.size(120.dp)) {
                    HarakatArabicText(
                        text = syllable,
                        highlightGroup = null,
                        baseColor = Color.White,
                    )
                }
            }
        }

        val matched = bitmap.region(0, 0, 120, 120).brightness()
        val unmatched = bitmap.region(120, 0, 120, 120).brightness()

        assertThat(matched).isNotEqualTo(unmatched)
    }

    @Test
    fun `a cell being recited paints both the letter and the mark in the playing ink`() {
        // `playing` outranks the group entirely — a cell inverted onto the accent must not keep a
        // mark coloured for the white background it is no longer on.
        val bitmap = composeRule.drawToBitmap {
            Row {
                Box(Modifier.size(120.dp)) {
                    HarakatArabicText(
                        text = syllable,
                        highlightGroup = "kasra",
                        baseColor = Color.White,
                        playing = true,
                        playingColor = Color.Red,
                        size = ArabicTextSize.LARGE,
                    )
                }
                Box(Modifier.size(120.dp)) {
                    HarakatArabicText(
                        text = syllable,
                        highlightGroup = "kasra",
                        baseColor = Color.White,
                        playing = false,
                        playingColor = Color.Red,
                        size = ArabicTextSize.LARGE,
                    )
                }
            }
        }

        val playing = bitmap.region(0, 0, 120, 120).brightness()
        val idle = bitmap.region(120, 0, 120, 120).brightness()

        assertThat(playing).isNotEqualTo(idle)
    }
}
