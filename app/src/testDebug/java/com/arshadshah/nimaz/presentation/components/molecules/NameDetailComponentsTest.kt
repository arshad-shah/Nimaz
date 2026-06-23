package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NameDetailComponentsTest {
    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `favorite fab shows add description and invokes onClick`() {
        var fired = false
        composeRule.setThemedContent {
            FavoriteFab(
                isFavorite = false,
                accent = NamesAccents.allah(),
                onClick = { fired = true },
            )
        }
        composeRule.onNodeWithContentDescription("Add to favorites").performClick()
        assertThat(fired).isTrue()
    }

    @Test
    fun `favorite fab shows remove description when favorite`() {
        composeRule.setThemedContent {
            FavoriteFab(
                isFavorite = true,
                accent = NamesAccents.prophets(),
                onClick = {},
            )
        }
        composeRule.onNodeWithContentDescription("Remove from favorites").assertExists()
    }

    @Test
    fun `section card renders title and content`() {
        composeRule.setThemedContent {
            NameDetailSectionCard(
                title = "Meaning",
                content = "The Most Merciful.",
            )
        }
        composeRule.onNodeWithText("Meaning").assertExists()
        composeRule.onNodeWithText("The Most Merciful.").assertExists()
    }

    @Test
    fun `section card renders nothing when content blank`() {
        composeRule.setThemedContent {
            NameDetailSectionCard(
                title = "Benefits",
                content = "   ",
            )
        }
        composeRule.onNodeWithText("Benefits").assertDoesNotExist()
    }
}
