package com.arshadshah.nimaz.presentation.components.molecules

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
 * The dialog, and the two prefabs built on it.
 *
 * `wrapContent` is the option worth naming: by default the content slot is wrapped in a tinted card
 * so prose reads against a surface, and a picker or a form passes `false` because it does its own
 * structuring. Wrapping a calendar or a list picker doubles the container and inset — the whole
 * dialog gains a second border and loses a fifth of its width.
 *
 * `NimazConfirmDialog`'s `isDestructive` is the other: it swaps the confirm button for the
 * destructive variant, which is the only visual difference between "save" and "delete for ever" in
 * an otherwise identical dialog.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2200dp")
class NimazDialogVariantsTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `a dialog renders its title, subtitle, content and actions`() {
        var confirmed = 0
        composeRule.setThemedContent {
            NimazDialog(
                title = "Delete khatam",
                subtitle = "This cannot be undone",
                titleIcon = Icons.Filled.Delete,
                accentColor = Color.Magenta,
                onDismiss = {},
                actions = {
                    NimazDialogCancelButton(onClick = {})
                    NimazDialogDestructiveButton(text = "Delete", onClick = { confirmed++ })
                },
                content = { Text("body copy") },
            )
        }

        composeRule.onNodeWithText("Delete khatam").assertExists()
        composeRule.onNodeWithText("This cannot be undone").assertExists()
        composeRule.onNodeWithText("body copy").assertExists()
        composeRule.onNodeWithText("Delete").performClick()
        assertThat(confirmed).isEqualTo(1)
    }

    @Test
    fun `a bare dialog is just its title`() {
        // Every optional slot left out at once: no icon, no subtitle, no content, no actions.
        composeRule.setThemedContent {
            NimazDialog(title = "Nothing else", onDismiss = {})
        }

        composeRule.onNodeWithText("Nothing else").assertExists()
    }

    @Test
    fun `content can opt out of the wrapping card`() {
        // `wrapContent = false` is what a picker or a calendar passes — wrapping it doubles the
        // container and the inset. Both arms render the content; the difference is the chrome.
        composeRule.setThemedContent {
            NimazDialog(
                title = "Picker",
                onDismiss = {},
                wrapContent = false,
                showCloseButton = false,
                showActionsDivider = false,
                actions = { NimazDialogConfirmButton(text = "OK", onClick = {}) },
                content = { Text("unwrapped") },
            )
        }

        composeRule.onNodeWithText("unwrapped").assertExists()
        composeRule.onNodeWithText("OK").assertExists()
    }

    @Test
    fun `a confirm dialog runs the right button`() {
        var confirmed = 0
        var dismissed = 0
        composeRule.setThemedContent {
            NimazConfirmDialog(
                title = "Reset progress",
                message = "Your khatam will start again.",
                onConfirm = { confirmed++ },
                onDismiss = { dismissed++ },
            )
        }

        composeRule.onNodeWithText("Your khatam will start again.").assertExists()
        composeRule.onNodeWithText("Cancel").performClick()
        assertThat(dismissed).isEqualTo(1)
        assertThat(confirmed).isEqualTo(0)

        composeRule.onNodeWithText("Confirm").performClick()
        assertThat(confirmed).isEqualTo(1)
    }

    @Test
    fun `a destructive confirm dialog takes its own labels`() {
        // `isDestructive` swaps the confirm button's variant — the one visual difference between
        // "save" and "delete for ever".
        var confirmed = 0
        composeRule.setThemedContent {
            NimazConfirmDialog(
                title = "Delete",
                message = "Gone for good.",
                onConfirm = { confirmed++ },
                onDismiss = {},
                confirmText = "Delete it",
                cancelText = "Keep it",
                titleIcon = Icons.Filled.Delete,
                isDestructive = true,
            )
        }

        composeRule.onNodeWithText("Keep it").assertExists()
        composeRule.onNodeWithText("Delete it").performClick()
        assertThat(confirmed).isEqualTo(1)
    }

    @Test
    fun `an info dialog dismisses on its single button`() {
        var dismissed = 0
        composeRule.setThemedContent {
            NimazInfoDialog(
                title = "About nisab",
                message = "The threshold above which zakat is due.",
                onDismiss = { dismissed++ },
            )
        }

        composeRule.onNodeWithText("The threshold above which zakat is due.").assertExists()
        composeRule.onNodeWithText(context.getString(R.string.got_it)).performClick()
        assertThat(dismissed).isEqualTo(1)
    }

    @Test
    fun `an info dialog takes its own label, icon and accent`() {
        composeRule.setThemedContent {
            NimazInfoDialog(
                title = "About nisab",
                message = "Explained.",
                onDismiss = {},
                dismissText = "Understood",
                titleIcon = null,
                accentColor = Color.Magenta,
            )
        }

        composeRule.onNodeWithText("Understood").assertExists()
    }
}
