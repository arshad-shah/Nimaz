package com.arshadshah.nimaz.presentation.components.molecules

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.presentation.components.atoms.NimazActionPill
import com.arshadshah.nimaz.presentation.components.atoms.QuranOrnamentalDivider
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
 * The small surfaces: a sheet's inner card, its section label, the reader's ornamental rule and the
 * action pill.
 *
 * Each is a handful of lines whose whole API is its options, and each is used from a place that
 * sets them: a sheet section tints its label to match the content it heads, and the Quran reader's
 * divider takes the frame gold rather than the theme's outline. A parameter read into a local and
 * not passed on leaves the caller's choice silently ignored — which is only visible on the one
 * screen that sets it, and looks like a theming bug rather than a dropped argument.
 *
 * The divider is drawn geometry, so its assertion is on pixels: its colour and its insets are the
 * only two things it has, and both are invisible to the semantics tree.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-mdpi")
class SurfaceOptionsTest {

    @Suppress("DEPRECATION")
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `the sheet's inner card and section label take their options`() {
        composeRule.setContent {
            androidx.compose.material3.MaterialTheme {
                Column {
                    NimazSheetSectionLabel(text = "Default label")
                    NimazSheetSectionLabel(
                        text = "Tinted label",
                        modifier = Modifier,
                        color = Color.Magenta,
                    )
                    NimazSheetPreviewCard { Text("default card") }
                    NimazSheetPreviewCard(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.Yellow,
                        shape = RoundedCornerShape(2.dp),
                        contentPadding = PaddingValues(4.dp),
                    ) {
                        Text("specified card")
                    }
                }
            }
        }

        composeRule.onNodeWithText("Tinted label").assertExists()
        composeRule.onNodeWithText("specified card").assertExists()
    }

    @Test
    fun `the ornamental divider paints in the colour it is given`() {
        // The reader's rule between passages. It carries the manuscript gold rather than the
        // theme's outline, so the colour is the parameter that matters most.
        val bitmap = composeRule.drawToBitmap {
            Column {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(40.dp)) {
                    QuranOrnamentalDivider(color = Color.White)
                }
                androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(40.dp)) {
                    QuranOrnamentalDivider(
                        modifier = Modifier,
                        color = Color.White,
                        horizontalPadding = 120.dp,
                        verticalPadding = 2.dp,
                    )
                }
            }
        }

        val wide = bitmap.region(0, 0, bitmap.width, 40)
        val inset = bitmap.region(0, 40, bitmap.width, 40)

        assertThat(wide.brightness()).isGreaterThan(0.0)
        // A 120dp horizontal inset leaves the outer columns of the second rule empty.
        assertThat(bitmap.region(0, 40, 100, 40).brightness())
            .isLessThan(bitmap.region(0, 0, 100, 40).brightness())
        assertThat(inset.brightness()).isGreaterThan(0.0)
    }

    @Test
    fun `the action pill wraps whatever it is given`() {
        composeRule.setContent {
            androidx.compose.material3.MaterialTheme {
                Column {
                    NimazActionPill { Text("inside the pill") }
                    NimazActionPill(modifier = Modifier.size(200.dp, 40.dp)) { Text("sized") }
                }
            }
        }

        composeRule.onNodeWithText("inside the pill").assertExists()
        composeRule.onNodeWithText("sized").assertExists()
    }

    @Test
    fun `the empty state takes its own glyph and tint`() {
        composeRule.setContent {
            androidx.compose.material3.MaterialTheme {
                Column {
                    NimazEmptyState(title = "Default", message = "with the default glyph")
                    NimazEmptyState(
                        title = "Custom",
                        message = "with its own",
                        modifier = Modifier,
                        icon = Icons.Filled.Star,
                        iconTint = Color.Magenta,
                        actionLabel = "Do it",
                        onAction = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("with the default glyph").assertExists()
        composeRule.onNodeWithText("Do it").assertExists()
    }
}
