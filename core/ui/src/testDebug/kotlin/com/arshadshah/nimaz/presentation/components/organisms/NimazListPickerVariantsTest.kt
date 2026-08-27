package com.arshadshah.nimaz.presentation.components.organisms

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
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
 * The list picker — how every long single-choice setting is presented.
 *
 * `searchable` defaults to `items.size >= 8`, which is the decision that makes the component
 * usable without every call site thinking about it: the calculation-method picker has eleven
 * entries and needs a search field, the madhab picker has two and would look absurd with one. A
 * default read the wrong way puts a search box above a two-item list on every settings screen.
 *
 * The search **corpus includes the description**, deliberately — people remember a calculation
 * method by where it is used ("Karachi", "Egypt") rather than by its formal name. Filtering on the
 * title alone makes those searches return nothing, which reads as the list being wrong.
 *
 * `autoDismiss` is the other axis: a picker that closes on selection has no confirm button, and
 * one that does not must offer both. Getting it backwards leaves a picker the user cannot leave.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class NimazListPickerVariantsTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    /**
     * The search field's content description.
     *
     * Not the caller's `searchPlaceholder`: the picker does **not** forward it to `NimazSearchBar`,
     * so the field carries the search bar's own default. That is a real (if small) gap in the
     * component, and pinning the behaviour that ships is more useful than asserting the one that
     * does not.
     */
    private val SEARCH_FIELD = "Search..."

    private fun items(n: Int) = (1..n).map {
        NimazPickerItem(
            value = "v$it",
            title = "Method $it",
            description = if (it == 1) "Used in Karachi" else null,
            icon = if (it == 2) Icons.Filled.Star else null,
            iconTint = if (it == 2) Color.Magenta else null,
            group = if (n > 3) (if (it <= n / 2) "First half" else "Second half") else null,
        )
    }

    @Test
    fun `a short list is offered without a search field`() {
        // `searchable = items.size >= 8`. A search box above two options is the visible failure.
        composeRule.setThemedContent {
            NimazListPicker(
                title = "Madhab",
                items = items(2),
                selected = "v1",
                onSelected = {},
                onDismiss = {},
            )
        }

        composeRule.onNodeWithText("Method 1").assertExists()
        composeRule.onNodeWithContentDescription(SEARCH_FIELD).assertDoesNotExist()
    }

    @Test
    fun `a long list gets one by default`() {
        composeRule.setThemedContent {
            NimazListPicker(
                title = "Calculation method",
                items = items(9),
                selected = "v1",
                onSelected = {},
                onDismiss = {},
            )
        }

        composeRule.onNodeWithContentDescription(SEARCH_FIELD).assertExists()
    }

    @Test
    fun `search matches the description, not only the title`() {
        // People remember a method by where it is used. Filtering on the title alone makes
        // "Karachi" return nothing, which reads as the list being wrong rather than the search.
        composeRule.setThemedContent {
            NimazListPicker(
                title = "Calculation method",
                items = items(9),
                selected = "v1",
                onSelected = {},
                onDismiss = {},
            )
        }

        composeRule.onNodeWithContentDescription(SEARCH_FIELD).performTextInput("Karachi")

        composeRule.onNodeWithText("Method 1").assertExists()
        composeRule.onNodeWithText("Method 3").assertDoesNotExist()
    }

    @Test
    fun `a selection closes the picker when it is set to`() {
        var chosen: String? = null
        var dismissed = 0
        composeRule.setThemedContent {
            NimazListPicker(
                title = "Madhab",
                items = items(3),
                selected = "v1",
                onSelected = { chosen = it },
                onDismiss = { dismissed++ },
            )
        }

        composeRule.onNodeWithText("Method 2").performClick()

        assertThat(chosen).isEqualTo("v2")
        assertThat(dismissed).isEqualTo(1)
    }

    @Test
    fun `a confirm-style picker offers both buttons and does not close on selection`() {
        // `autoDismiss = false` — a picker that neither closes on tap nor offers a confirm is one
        // the user cannot leave.
        var dismissed = 0
        composeRule.setThemedContent {
            NimazListPicker(
                title = "Madhab",
                items = items(3),
                selected = "v1",
                onSelected = {},
                onDismiss = { dismissed++ },
                autoDismiss = false,
                confirmText = "Use this",
                cancelText = "Never mind",
            )
        }

        composeRule.onNodeWithText("Method 2").performClick()
        assertThat(dismissed).isEqualTo(0)

        composeRule.onNodeWithText("Use this").assertExists()
        composeRule.onNodeWithText("Never mind").performClick()
        assertThat(dismissed).isEqualTo(1)
    }

    @Test
    fun `a search with no matches says so in the caller's words`() {
        composeRule.setThemedContent {
            NimazListPicker(
                title = "Calculation method",
                items = items(9),
                selected = "v1",
                onSelected = {},
                onDismiss = {},
                searchPlaceholder = "Find a method",
                emptySearchText = "Nothing matches that",
            )
        }

        composeRule.onNodeWithContentDescription(SEARCH_FIELD).performTextInput("zzz")

        composeRule.onNodeWithText("Nothing matches that").assertExists()
    }

    @Test
    fun `grouped items keep their headings and a caller can add trailing content`() {
        composeRule.setThemedContent {
            NimazListPicker(
                title = "Calculation method",
                items = items(6),
                selected = "v1",
                onSelected = {},
                onDismiss = {},
                searchable = false,
                trailingContent = { Text("t-${it.title}") },
            )
        }

        // The header uppercases its text, so the group name is not what is on screen.
        composeRule.onNodeWithText("FIRST HALF").assertExists()
        composeRule.onNodeWithText("SECOND HALF").assertExists()
        composeRule.onNodeWithText("t-Method 1").assertExists()
    }

    @Test
    fun `a picker with nothing selected still renders every option`() {
        // `selected: T?` — a setting that has never been chosen has no current value, and a picker
        // that assumed one would either highlight the wrong row or fail to render.
        composeRule.setThemedContent {
            NimazListPicker(
                title = "Madhab",
                items = items(3),
                selected = null,
                onSelected = {},
                onDismiss = {},
            )
        }

        composeRule.onNodeWithText("Method 1").assertExists()
        composeRule.onNodeWithText("Method 3").assertExists()
    }
}
