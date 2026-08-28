package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The attention slot: one banner on the screen, the rest behind a sheet.
 *
 * The overflow used to expand in place, so every queued interruption pushed the prayer card
 * further down the home screen. These assert the shape it was meant to have — the others are
 * *not on the screen* until asked for, and then they are in a sheet.
 */
@RunWith(RobolectricTestRunner::class)
class HomeBannerSlotTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private fun banner(
        id: String = "b1",
        title: String = "Notifications off",
        variant: HomeBannerVariant = HomeBannerVariant.WARNING,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null,
        dismissable: Boolean = false,
        onDismiss: (() -> Unit)? = null,
    ) = HomeBannerItem(
        id = id,
        icon = Icons.Default.Notifications,
        title = title,
        variant = variant,
        actionLabel = actionLabel,
        onAction = onAction,
        dismissable = dismissable,
        onDismiss = onDismiss,
    )

    @Test
    fun `a single item draws no overflow affordance`() {
        composeRule.setThemedContent {
            HomeBannerSlot(items = listOf(banner(title = "Only one")))
        }

        composeRule.onNodeWithText("Only one").assertExists()
        composeRule.onNodeWithText("0 more to deal with").assertDoesNotExist()
    }

    @Test
    fun `the overflow stays off the screen until it is asked for`() {
        composeRule.setThemedContent {
            HomeBannerSlot(
                items = listOf(
                    banner(id = "a", title = "First banner"),
                    banner(id = "b", title = "Second banner"),
                    banner(id = "c", title = "Third banner"),
                )
            )
        }

        composeRule.onNodeWithText("First banner").assertExists()
        // The point of the change. Expanding in place put these two on the home screen.
        composeRule.onNodeWithText("Second banner").assertDoesNotExist()
        composeRule.onNodeWithText("Third banner").assertDoesNotExist()
        composeRule.onNodeWithText("2 more to deal with").assertExists()
    }

    @Test
    fun `the affordance opens a sheet carrying every item`() {
        composeRule.setThemedContent {
            HomeBannerSlot(
                items = listOf(
                    banner(id = "a", title = "First banner"),
                    banner(id = "b", title = "Second banner"),
                )
            )
        }

        composeRule.onNodeWithText("1 more to deal with").performClick()

        composeRule.onNodeWithText("Needs your attention").assertIsDisplayed()
        composeRule.onNodeWithText("Second banner").assertExists()
    }

    @Test
    fun `acting on a banner runs its action`() {
        var acted = 0
        composeRule.setThemedContent {
            HomeBannerSlot(
                items = listOf(
                    banner(
                        id = "a",
                        title = "First banner",
                        actionLabel = "Fix it",
                        onAction = { acted++ },
                    ),
                    banner(id = "b", title = "Second banner"),
                )
            )
        }

        composeRule.onNodeWithText("Fix it").performClick()

        assertThat(acted).isEqualTo(1)
    }
}
