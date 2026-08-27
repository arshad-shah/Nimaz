package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The zakat hero, addressed the only way it can be: by what TalkBack reads.
 *
 * `ZakatSummaryHero` sets `clearAndSetSemantics` over its plinth and over every stat tile, so the
 * whole plinth is announced as one phrase — "Zakat Due, $1,284.50, Above nisab" — and the label,
 * the amount, the status and the subtitle have no nodes of their own. Every `onNodeWithText`
 * against them fails, and the subtitle is unreachable entirely. That is deliberate: four
 * unrelated fragments is the wrong announcement for one figure. It also makes the a11y string the
 * better contract to pin, because it is the thing a reader actually receives.
 *
 * What is worth catching here is the collapse. The first attempt at it interpolated paddings and
 * alpha against a 0..1 float, which animated everything *except* the height — `offset` and
 * `alpha` are draw-time modifiers, so the tile row kept its full measured height at every
 * intermediate value. The tiles disappearing is therefore the behaviour, and it is asserted by
 * their announcements being gone rather than by a pixel.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class ZakatSummaryHeroTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val stats = listOf(
        ZakatHeroStat("\$51,380", "Net"),
        ZakatHeroStat("\$5,847", "Nisab", accented = true),
        ZakatHeroStat("2.5%", "Rate"),
    )

    @Test
    fun `the plinth is announced as one phrase, not four fragments`() {
        composeRule.setThemedContent {
            ZakatSummaryHero(
                label = "Zakat Due",
                amount = "\$1,284.50",
                subtitle = "2.5% of eligible wealth",
                status = ZakatHeroStatus(text = "Above nisab", met = true),
            )
        }

        composeRule.onNodeWithContentDescription("Zakat Due, \$1,284.50, Above nisab")
            .assertExists()
    }

    @Test
    fun `a plinth with no status drops the status clause from its announcement`() {
        // The `status == null` arm picks the two-part format string. Reading it the other way
        // leaves a trailing comma in what TalkBack says.
        composeRule.setThemedContent {
            ZakatSummaryHero(
                label = "Total Paid",
                amount = "\$820.00",
                subtitle = "Across 3 payments",
            )
        }

        composeRule.onNodeWithContentDescription("Total Paid, \$820.00").assertExists()
    }

    @Test
    fun `each stat tile is announced with its own label and figure`() {
        composeRule.setThemedContent {
            ZakatSummaryHero(
                label = "Zakat Due",
                amount = "\$1,284.50",
                subtitle = "2.5% of eligible wealth",
                stats = stats,
            )
        }

        composeRule.onNodeWithContentDescription("Net, \$51,380").assertExists()
        composeRule.onNodeWithContentDescription("Nisab, \$5,847").assertExists()
        composeRule.onNodeWithContentDescription("Rate, 2.5%").assertExists()
    }

    @Test
    fun `a hero with no stats renders as a plinth alone`() {
        composeRule.setThemedContent {
            ZakatSummaryHero(
                label = "Zakat Due",
                amount = "\$0.00",
                subtitle = "Nothing is owed",
                stats = emptyList(),
            )
        }

        composeRule.onNodeWithContentDescription("Zakat Due, \$0.00").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Net, \$51,380").assertDoesNotExist()
    }

    @Test
    fun `collapsing folds the tiles away and keeps the amount`() {
        // The one thing the collapse must never do is lose the figure — on the calculator it is
        // what the whole task is about, and a hero that scrolls it away is the bug this replaced.
        composeRule.setThemedContent {
            ZakatSummaryHero(
                label = "Zakat Due",
                amount = "\$1,284.50",
                subtitle = "2.5% of eligible wealth",
                status = ZakatHeroStatus(text = "Above nisab", met = true),
                stats = stats,
                collapsed = true,
            )
        }

        composeRule.onNodeWithContentDescription("Zakat Due, \$1,284.50, Above nisab")
            .assertExists()
        composeRule.onNodeWithContentDescription("Net, \$51,380").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Rate, 2.5%").assertDoesNotExist()
    }

    @Test
    fun `a status pill renders its text whether or not the threshold is met`() {
        // `met` picks ACCENT over NEUTRAL. The pill is outside the plinth's
        // `clearAndSetSemantics`… it is not — the badge sits inside it, so the assertion is on
        // the announcement, which is the same string either way and must appear for both.
        composeRule.setThemedContent {
            androidx.compose.foundation.layout.Column {
                ZakatSummaryHero(
                    label = "Above",
                    amount = "\$1,284.50",
                    subtitle = "eligible",
                    status = ZakatHeroStatus(text = "Above nisab", met = true),
                )
                ZakatSummaryHero(
                    label = "Below",
                    amount = "\$0.00",
                    subtitle = "not eligible",
                    status = ZakatHeroStatus(text = "Below nisab", met = false),
                    muteAmount = true,
                )
            }
        }

        composeRule.onNodeWithContentDescription("Above, \$1,284.50, Above nisab").assertExists()
        composeRule.onNodeWithContentDescription("Below, \$0.00, Below nisab").assertExists()
    }

    @Test
    fun `an accented stat is still announced like any other`() {
        // `accented` changes ink only — it must not change what is read out, or the nisab tile
        // would announce differently from the two beside it.
        composeRule.setThemedContent {
            ZakatSummaryHero(
                label = "Zakat Due",
                amount = "\$1,284.50",
                subtitle = "2.5%",
                stats = listOf(ZakatHeroStat("\$5,847", "Nisab", accented = true)),
            )
        }

        composeRule.onNodeWithContentDescription("Nisab, \$5,847").assertExists()
    }

    @Test
    fun `the subtitle has no node of its own`() {
        // Not a limitation to work around — the plinth deliberately reads as one phrase, and a
        // change that gave the subtitle its own node would make TalkBack read the hero twice.
        composeRule.setThemedContent {
            ZakatSummaryHero(
                label = "Zakat Due",
                amount = "\$1,284.50",
                subtitle = "2.5% of eligible wealth",
            )
        }

        composeRule.onNodeWithText("2.5% of eligible wealth").assertDoesNotExist()
    }
}
