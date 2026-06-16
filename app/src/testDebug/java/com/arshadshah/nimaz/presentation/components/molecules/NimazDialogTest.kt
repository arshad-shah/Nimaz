package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NimazDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    // ──── NimazDialog (base primitive) ──────────────────────────────────────

    @Test
    fun `base dialog renders title and content`() {
        composeRule.setThemedContent {
            NimazDialog(
                title = "Dialog Title",
                onDismiss = {},
                content = {
                    Text("Body content")
                }
            )
        }
        composeRule.onNodeWithText("Dialog Title").assertExists()
        composeRule.onNodeWithText("Body content").assertExists()
    }

    @Test
    fun `base dialog renders subtitle when provided`() {
        composeRule.setThemedContent {
            NimazDialog(
                title = "Dialog Title",
                subtitle = "A subtitle",
                onDismiss = {},
                content = { Text("Body") }
            )
        }
        composeRule.onNodeWithText("A subtitle").assertExists()
    }

    @Test
    fun `base dialog close button triggers onDismiss`() {
        var dismissed = false
        composeRule.setThemedContent {
            NimazDialog(
                title = "Dialog Title",
                onDismiss = { dismissed = true },
                content = { Text("Body") }
            )
        }
        // showCloseButton defaults to true; its Icon has contentDescription "Close".
        composeRule.onNodeWithContentDescription("Close").performClick()
        assertThat(dismissed).isTrue()
    }

    @Test
    fun `base dialog hides close button when showCloseButton false`() {
        composeRule.setThemedContent {
            NimazDialog(
                title = "Dialog Title",
                onDismiss = {},
                showCloseButton = false,
                content = { Text("Body") }
            )
        }
        composeRule.onNodeWithContentDescription("Close").assertDoesNotExist()
    }

    @Test
    fun `base dialog renders actions slot`() {
        var confirmed = false
        composeRule.setThemedContent {
            NimazDialog(
                title = "Dialog Title",
                onDismiss = {},
                content = { Text("Body") },
                actions = {
                    NimazDialogConfirmButton(text = "Okay", onClick = { confirmed = true })
                }
            )
        }
        composeRule.onNodeWithText("Okay").assertExists()
        composeRule.onNodeWithText("Okay").performClick()
        assertThat(confirmed).isTrue()
    }

    @Test
    fun `base dialog gated by visible flag does not render when false`() {
        var visible by mutableStateOf(false)
        composeRule.setThemedContent {
            if (visible) {
                NimazDialog(
                    title = "Gated Title",
                    onDismiss = {},
                    content = { Text("Gated body") }
                )
            }
        }
        composeRule.onNodeWithText("Gated Title").assertDoesNotExist()

        // Flip to true and confirm it then renders.
        composeRule.runOnUiThread { visible = true }
        composeRule.onNodeWithText("Gated Title").assertExists()
    }

    // ──── Action-button helpers ─────────────────────────────────────────────

    @Test
    fun `cancel button renders default text and triggers click`() {
        var clicked = false
        composeRule.setThemedContent {
            NimazDialogCancelButton(onClick = { clicked = true })
        }
        composeRule.onNodeWithText("Cancel").assertExists()
        composeRule.onNodeWithText("Cancel").performClick()
        assertThat(clicked).isTrue()
    }

    @Test
    fun `confirm button renders text and triggers click`() {
        var clicked = false
        composeRule.setThemedContent {
            NimazDialogConfirmButton(text = "Save", onClick = { clicked = true })
        }
        composeRule.onNodeWithText("Save").assertExists()
        composeRule.onNodeWithText("Save").performClick()
        assertThat(clicked).isTrue()
    }

    @Test
    fun `destructive button renders text and triggers click`() {
        var clicked = false
        composeRule.setThemedContent {
            NimazDialogDestructiveButton(text = "Delete", onClick = { clicked = true })
        }
        composeRule.onNodeWithText("Delete").assertExists()
        composeRule.onNodeWithText("Delete").performClick()
        assertThat(clicked).isTrue()
    }

    // ──── NimazConfirmDialog ────────────────────────────────────────────────

    @Test
    fun `confirm dialog renders title and message`() {
        composeRule.setThemedContent {
            NimazConfirmDialog(
                title = "Mark all as prayed?",
                message = "This marks all remaining prayers.",
                onConfirm = {},
                onDismiss = {}
            )
        }
        composeRule.onNodeWithText("Mark all as prayed?").assertExists()
        composeRule.onNodeWithText("This marks all remaining prayers.").assertExists()
    }

    @Test
    fun `confirm dialog confirm button triggers onConfirm and onDismiss`() {
        var confirmed = false
        var dismissed = false
        composeRule.setThemedContent {
            NimazConfirmDialog(
                title = "Proceed?",
                message = "Body",
                confirmText = "Proceed",
                onConfirm = { confirmed = true },
                onDismiss = { dismissed = true }
            )
        }
        composeRule.onNodeWithText("Proceed").performClick()
        assertThat(confirmed).isTrue()
        assertThat(dismissed).isTrue()
    }

    @Test
    fun `confirm dialog cancel button triggers onDismiss only`() {
        var confirmed = false
        var dismissed = false
        composeRule.setThemedContent {
            NimazConfirmDialog(
                title = "Proceed?",
                message = "Body",
                onConfirm = { confirmed = true },
                onDismiss = { dismissed = true }
            )
        }
        composeRule.onNodeWithText("Cancel").performClick()
        assertThat(dismissed).isTrue()
        assertThat(confirmed).isFalse()
    }

    @Test
    fun `confirm dialog destructive branch confirm triggers callbacks`() {
        var confirmed = false
        var dismissed = false
        composeRule.setThemedContent {
            NimazConfirmDialog(
                title = "Reset settings?",
                message = "Body",
                confirmText = "Reset",
                isDestructive = true,
                onConfirm = { confirmed = true },
                onDismiss = { dismissed = true }
            )
        }
        composeRule.onNodeWithText("Reset").performClick()
        assertThat(confirmed).isTrue()
        assertThat(dismissed).isTrue()
    }

    // ──── NimazInfoDialog ───────────────────────────────────────────────────

    @Test
    fun `info dialog renders title and message`() {
        composeRule.setThemedContent {
            NimazInfoDialog(
                title = "Location updated",
                message = "Prayer times recalculated.",
                onDismiss = {}
            )
        }
        composeRule.onNodeWithText("Location updated").assertExists()
        composeRule.onNodeWithText("Prayer times recalculated.").assertExists()
    }

    @Test
    fun `info dialog dismiss button triggers onDismiss`() {
        var dismissed = false
        composeRule.setThemedContent {
            NimazInfoDialog(
                title = "Synced",
                message = "Your data is up to date.",
                dismissText = "Got it",
                onDismiss = { dismissed = true }
            )
        }
        composeRule.onNodeWithText("Got it").performClick()
        assertThat(dismissed).isTrue()
    }
}
