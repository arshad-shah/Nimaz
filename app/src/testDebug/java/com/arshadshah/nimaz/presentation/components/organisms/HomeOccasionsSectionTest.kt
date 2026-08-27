package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.arshadshah.nimaz.domain.model.CelebrationEvent
import com.arshadshah.nimaz.domain.model.HomeEventCard
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Home's "today's occasion" section.
 *
 * The regression this guards: compact Home rendered no celebration cards at all between #528 and
 * the introduction of this section, so on 12 Rabi' al-Awwal 1448 (25 Aug 2026) a phone showed
 * nothing about Mawlid while a tablet showed the card. The mapping lived inline in the tablet
 * branch, which is why nothing failed — there was no second copy to disagree with.
 */
@RunWith(RobolectricTestRunner::class)
class HomeOccasionsSectionTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val mawlid = HomeEventCard(
        event = CelebrationEvent.MAWLID,
        eyebrow = "Mawlid an-Nabi",
        headline = "Mawlid an-Nabi",
        body = "Birth of Prophet Muhammad (PBUH)",
        arabic = "المولد النبوي",
        priority = 1,
    )

    @Test
    fun `renders today's occasion`() {
        composeRule.setThemedContent {
            HomeOccasionsSection(mawlid.let(::listOf), onOpenRoute = {}, onDismiss = {})
        }

        composeRule.onNodeWithTag(HomeOccasionsTestTag).assertExists()
        composeRule.onNodeWithText("Mawlid an-Nabi").assertExists()
        composeRule.onNodeWithText("Birth of Prophet Muhammad (PBUH)").assertExists()
    }

    @Test
    fun `renders nothing when today carries no occasion`() {
        composeRule.setThemedContent {
            HomeOccasionsSection(emptyList(), onOpenRoute = {}, onDismiss = {})
        }

        composeRule.onNodeWithTag(HomeOccasionsTestTag).assertDoesNotExist()
    }

    @Test
    fun `a pushed card's CTA reports its route`() {
        val opened = mutableListOf<String>()
        composeRule.setThemedContent {
            HomeOccasionsSection(
                listOf(mawlid.copy(ctaLabel = "Read more", route = "dua/12")),
                onOpenRoute = { opened += it },
                onDismiss = {},
            )
        }

        composeRule.onNodeWithText("Read more").assertHasClickAction().performClick()

        assertEquals(listOf("dua/12"), opened)
    }

    @Test
    fun `a local occasion is not dismissable`() {
        // A local calendar occasion has no announcement id to remember a dismissal against, so
        // offering the control would only make it vanish until the next recomposition.
        val cards = occasionEventCards(listOf(mawlid.copy(dismissable = true)), {}, {})

        assertEquals(1, cards.size)
        assertEquals(null, cards.single().onDismiss)
    }

    @Test
    fun `a pushed occasion is dismissable`() {
        var dismissed = 0
        val cards = occasionEventCards(
            listOf(mawlid.copy(dismissable = true, announcementId = "ann-1")),
            {},
        ) { dismissed++ }

        cards.single().onDismiss?.invoke()

        assertEquals(1, dismissed)
    }
}
