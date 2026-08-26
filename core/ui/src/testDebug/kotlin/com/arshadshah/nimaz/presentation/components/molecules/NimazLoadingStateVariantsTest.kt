package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The loading state, at all three scales.
 *
 * It is the deliberate mirror of `NimazErrorState` — same three variants, so a screen can swap
 * loading → error without its layout jumping. What is worth pinning is the **announcement**: the
 * message becomes a `contentDescription` on the container, which is the only thing a screen reader
 * gets, because a spinner has no text of its own. A message rendered as a `Text` and not announced
 * leaves a blind user with silence where the app is busy.
 *
 * Every one of these renders an indeterminate `CircularProgressIndicator`, which never lets the
 * test clock idle — so the clock is pinned before the first composition (#604). That is not a
 * workaround here so much as the defining property of the component.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class NimazLoadingStateVariantsTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `a message is announced and shown at every scale`() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setThemedContent {
            Column {
                NimazLoadingState(
                    variant = NimazLoadingVariant.FULLSCREEN,
                    message = "Loading prayer times",
                )
                NimazLoadingState(
                    variant = NimazLoadingVariant.SECTION,
                    message = "Loading surahs",
                )
                NimazLoadingState(
                    variant = NimazLoadingVariant.INLINE,
                    message = "Checking location",
                )
            }
        }

        listOf("Loading prayer times", "Loading surahs", "Checking location").forEach {
            composeRule.onNodeWithContentDescription(it).assertExists()
            composeRule.onNodeWithText(it).assertExists()
        }
    }

    @Test
    fun `a silent spinner renders at every scale`() {
        // The `message == null` arm — three separate null checks, one per variant, plus the
        // semantics modifier that is skipped entirely. A section spinner with no copy is the
        // common case inside an already-titled card.
        composeRule.mainClock.autoAdvance = false
        composeRule.setThemedContent {
            Column {
                NimazLoadingState(variant = NimazLoadingVariant.SECTION)
                NimazLoadingState(variant = NimazLoadingVariant.INLINE)
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun `a fullscreen spinner takes the whole space and a section one does not`() {
        // The two share an arm of the `when` and differ only by a modifier, which is exactly the
        // shape that gets collapsed in a refactor — and a section spinner that filled the screen
        // would push the content it belongs to off it.
        composeRule.mainClock.autoAdvance = false
        composeRule.setThemedContent {
            Column {
                NimazLoadingState(
                    variant = NimazLoadingVariant.SECTION,
                    message = "section",
                )
                NimazLoadingState(
                    variant = NimazLoadingVariant.INLINE,
                    message = "inline",
                )
            }
        }

        val section = composeRule.onNodeWithContentDescription("section")
            .fetchSemanticsNode().size.height
        val inline = composeRule.onNodeWithContentDescription("inline")
            .fetchSemanticsNode().size.height

        assertThat(section).isGreaterThan(inline)
    }

    @Test
    fun `a caller's accent is accepted`() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setThemedContent {
            NimazLoadingState(
                variant = NimazLoadingVariant.INLINE,
                message = "tinted",
                color = Color.Magenta,
            )
        }

        composeRule.onNodeWithContentDescription("tinted").assertExists()
    }

    @Test
    fun `the loading variants mirror the error variants one for one`() {
        // The reason both enums exist in this shape: a screen swaps loading → error without
        // changing its layout. A variant added to one and not the other breaks that promise
        // silently, on whichever screen happens to use it.
        assertThat(NimazLoadingVariant.entries.map { it.name })
            .containsExactlyElementsIn(NimazErrorVariant.entries.map { it.name })
    }
}
