package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HomeBannerCarouselTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun banner(
        id: String = "b1",
        title: String = "Notifications off",
        variant: HomeBannerVariant = HomeBannerVariant.WARNING,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null,
        isLoading: Boolean = false,
    ) = HomeBannerItem(
        id = id,
        icon = Icons.Default.Notifications,
        title = title,
        variant = variant,
        actionLabel = actionLabel,
        onAction = onAction,
        isLoading = isLoading,
    )

    @Test
    fun `renders banner title`() {
        composeRule.setThemedContent {
            HomeBannerCarousel(
                banners = listOf(banner(title = "Location permission needed"))
            )
        }

        composeRule.onNodeWithText("Location permission needed").assertExists()
    }

    @Test
    fun `renders multiple banner titles`() {
        composeRule.setThemedContent {
            HomeBannerCarousel(
                banners = listOf(
                    banner(id = "a", title = "First banner"),
                    banner(id = "b", title = "Second banner"),
                )
            )
        }

        composeRule.onNodeWithText("First banner").assertExists()
        composeRule.onNodeWithText("Second banner").assertExists()
    }

    @Test
    fun `empty banner list renders nothing`() {
        composeRule.setThemedContent {
            HomeBannerCarousel(banners = emptyList())
        }

        composeRule.onNodeWithText("First banner").assertDoesNotExist()
    }

    @Test
    fun `action label renders and onAction fires when tapped`() {
        var fired = false
        composeRule.setThemedContent {
            HomeBannerCarousel(
                banners = listOf(
                    banner(
                        title = "Update available",
                        actionLabel = "Update",
                        onAction = { fired = true }
                    )
                )
            )
        }

        composeRule.onNodeWithText("Update").assertExists()
        composeRule.onNodeWithText("Update").performClick()
        assertThat(fired).isTrue()
    }

    @Test
    fun `whole pill is tappable when actionLabel absent but onAction present`() {
        var fired = false
        composeRule.setThemedContent {
            HomeBannerCarousel(
                banners = listOf(
                    banner(
                        title = "Tap me",
                        actionLabel = null,
                        onAction = { fired = true }
                    )
                )
            )
        }

        composeRule.onNodeWithText("Tap me").performClick()
        assertThat(fired).isTrue()
    }

    @Test
    fun `loading banner hides action label`() {
        composeRule.setThemedContent {
            HomeBannerCarousel(
                banners = listOf(
                    banner(
                        title = "Downloading update",
                        actionLabel = "Update",
                        onAction = {},
                        isLoading = true
                    )
                )
            )
        }

        // When loading, the spinner replaces the action label.
        composeRule.onNodeWithText("Downloading update").assertExists()
        composeRule.onNodeWithText("Update").assertDoesNotExist()
    }

    @Test
    fun `HomeBannerVariant has warning and update values`() {
        assertThat(HomeBannerVariant.values().toList())
            .containsExactly(HomeBannerVariant.WARNING, HomeBannerVariant.UPDATE)
    }

    @Test
    fun `HomeBannerItem defaults are null action and not loading`() {
        val item = HomeBannerItem(
            id = "x",
            icon = Icons.Default.Notifications,
            title = "Title",
            variant = HomeBannerVariant.UPDATE,
        )
        assertThat(item.actionLabel).isNull()
        assertThat(item.onAction).isNull()
        assertThat(item.isLoading).isFalse()
    }
}
