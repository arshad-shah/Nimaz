package com.arshadshah.nimaz.presentation.components.molecules

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The bottom sheet itself, rather than the pieces `NimazSheetLayoutTest` covers.
 *
 * Its body takes `weight(1f, fill = false)`, which is the line that makes a short sheet hug its
 * content and a tall one scroll while the footer stays put. Both halves of that are worth having:
 * a sheet that always filled the screen would show a two-line confirmation as a full-height panel,
 * and one that never scrolled would put the confirm button off the bottom of a long form.
 *
 * The footer slot is the other. With no footer the sheet still reserves a navigation-bar inset —
 * eight dead pixels rather than content sitting under the system gesture bar.
 *
 * A sheet slides in, and its content is attached but parked below the viewport while it does, so
 * assertions use `assertExists` rather than `assertIsDisplayed` (#604).
 */
@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class NimazBottomSheetTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `a sheet with a header renders it above the body`() {
        composeRule.setThemedContent {
            NimazBottomSheet(
                onDismissRequest = {},
                title = "Bookmarks",
                subtitle = "12 saved",
                icon = Icons.Filled.Bookmark,
                badge = "12",
                onClose = {},
            ) {
                Text("body")
            }
        }

        composeRule.onNodeWithText("Bookmarks").assertExists()
        composeRule.onNodeWithText("12 saved").assertExists()
        composeRule.onNodeWithText("body").assertExists()
    }

    @Test
    fun `a headerless sheet is just its content`() {
        // `title == null` skips the whole header — the shape a sheet that is entirely a custom
        // surface takes.
        composeRule.setThemedContent {
            NimazBottomSheet(onDismissRequest = {}) { Text("only the body") }
        }

        composeRule.onNodeWithText("only the body").assertExists()
    }

    @Test
    fun `the close control reports a dismissal`() {
        var closed = 0
        composeRule.setThemedContent {
            NimazBottomSheet(
                onDismissRequest = {},
                title = "Bookmarks",
                onClose = { closed++ },
            ) { Text("body") }
        }

        composeRule.onNodeWithContentDescription(context.getString(R.string.cd_close)).performClick()

        assertThat(closed).isEqualTo(1)
    }

    @Test
    fun `a footer is pinned below the body`() {
        composeRule.setThemedContent {
            NimazBottomSheet(
                onDismissRequest = {},
                title = "Confirm",
                footer = {
                    NimazSheetFooterButtons(primaryText = "Save", onPrimary = {})
                },
            ) { Text("body") }
        }

        val body = composeRule.onNodeWithText("body").fetchSemanticsNode().positionInRoot.y
        val footer = composeRule.onNodeWithText("Save").fetchSemanticsNode().positionInRoot.y

        assertThat(footer).isGreaterThan(body)
    }

    @Test
    fun `every surface option a caller can set is accepted`() {
        // The sheet is the app's one modal container, so its chrome is parameterised rather than
        // forked: shape, colours, elevation, the drag handle, the body's padding and whether it
        // scrolls at all. A form passes `scrollable = false` because it manages its own scrolling,
        // and nesting two scrollables is a measurement error rather than a style choice.
        composeRule.setThemedContent {
            NimazBottomSheet(
                onDismissRequest = {},
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                containerColor = Color.Black,
                contentColor = Color.White,
                tonalElevation = 0.dp,
                showDragHandle = false,
                title = "Fully specified",
                subtitle = "every option",
                icon = Icons.Filled.Bookmark,
                badge = "9",
                onClose = {},
                scrollable = false,
                contentPadding = PaddingValues(horizontal = 4.dp),
                footer = { Text("footer") },
            ) {
                Text("body")
            }
        }

        composeRule.onNodeWithText("Fully specified").assertExists()
        composeRule.onNodeWithText("body").assertExists()
        composeRule.onNodeWithText("footer").assertExists()
    }
}
