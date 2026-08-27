package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The breadcrumb trail above a drilled-into list.
 *
 * The indexing is the whole risk. Home reports **-1**, not 0, because 0 is the first real crumb —
 * so a caller popping "back to index n" and a bar reporting home as 0 would take the user one
 * level too deep instead of to the root, silently. `HOME_INDEX` is public precisely so the two
 * sides cannot disagree, and this is the only test that holds them together.
 *
 * The second thing worth pinning is which chip reads as selected: the deepest crumb when there is
 * one, home when there is not. Selection here is not decoration — it is the only thing on screen
 * saying where the list below comes from.
 *
 * A `LazyRow` composes a screenful, so the wide qualifier is needed for a trail whose crumbs would
 * otherwise fall off the right edge (#604).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w2000dp-h891dp")
class NimazBreadcrumbBarTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `the trail renders home and every crumb`() {
        composeRule.setThemedContent {
            NimazBreadcrumbBar(
                home = "All subjects",
                crumbs = listOf("Doctrine", "God", "The names of God"),
                onCrumbClick = {},
            )
        }

        composeRule.onNodeWithText("All subjects").assertExists()
        composeRule.onNodeWithText("Doctrine").assertExists()
        composeRule.onNodeWithText("God").assertExists()
        composeRule.onNodeWithText("The names of God").assertExists()
    }

    @Test
    fun `home reports the sentinel index, not zero`() {
        // -1 vs 0 is the difference between "go to the root" and "go to the first subject". Both
        // are plausible-looking integers and only one is right.
        var clicked: Int? = null
        composeRule.setThemedContent {
            NimazBreadcrumbBar(
                home = "All subjects",
                crumbs = listOf("Doctrine"),
                onCrumbClick = { clicked = it },
            )
        }

        composeRule.onNodeWithText("All subjects").performClick()
        assertThat(clicked).isEqualTo(HOME_INDEX)
        assertThat(HOME_INDEX).isEqualTo(-1)
    }

    @Test
    fun `each crumb reports its own depth`() {
        var clicked: Int? = null
        composeRule.setThemedContent {
            NimazBreadcrumbBar(
                home = "All subjects",
                crumbs = listOf("Doctrine", "God", "The names of God"),
                onCrumbClick = { clicked = it },
            )
        }

        composeRule.onNodeWithText("Doctrine").performClick()
        assertThat(clicked).isEqualTo(0)

        composeRule.onNodeWithText("God").performClick()
        assertThat(clicked).isEqualTo(1)

        composeRule.onNodeWithText("The names of God").performClick()
        assertThat(clicked).isEqualTo(2)
    }

    @Test
    fun `at the root only home is offered`() {
        // The empty-crumbs arm: no separators, no chips, and home reads as the selected one.
        composeRule.setThemedContent {
            NimazBreadcrumbBar(home = "All subjects", crumbs = emptyList(), onCrumbClick = {})
        }

        composeRule.onNodeWithText("All subjects").assertIsSelected()
    }

    @Test
    fun `the deepest crumb is the selected one`() {
        // `index == crumbs.lastIndex`. Selecting the first instead would say the list below is
        // Doctrine's when it is actually the names of God's.
        composeRule.setThemedContent {
            NimazBreadcrumbBar(
                home = "All subjects",
                crumbs = listOf("Doctrine", "God"),
                onCrumbClick = {},
            )
        }

        composeRule.onNodeWithText("God").assertIsSelected()
        composeRule.onNodeWithText("Doctrine").assertIsNotSelected()
        composeRule.onNodeWithText("All subjects").assertIsNotSelected()
    }

    @Test
    fun `a home icon renders alongside its label`() {
        composeRule.setThemedContent {
            NimazBreadcrumbBar(
                home = "All subjects",
                crumbs = emptyList(),
                onCrumbClick = {},
                homeIcon = Icons.Filled.Home,
            )
        }

        composeRule.onNodeWithText("All subjects").assertExists()
    }
}
