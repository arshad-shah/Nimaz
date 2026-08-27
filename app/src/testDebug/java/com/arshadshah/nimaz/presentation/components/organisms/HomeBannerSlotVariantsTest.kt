package com.arshadshah.nimaz.presentation.components.organisms

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner

/**
 * The same four-arm mapping in the slot, which is what compact Home actually renders.
 *
 * The slot and the carousel are two separate copies of the variant `when`, and only the slot
 * is on the phone path — so an arm broken here is broken for almost every reader.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
class HomeBannerSlotVariantsTest(private val variant: HomeBannerVariant) {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `the slot renders this variant as its lead banner`() {
        composeRule.setThemedContent {
            HomeBannerSlot(
                items = listOf(
                    HomeBannerItem(
                        id = "b1",
                        icon = Icons.Default.Info,
                        title = "Something needs attention",
                        variant = variant,
                        subtitle = "And here is what it is",
                    )
                )
            )
        }

        composeRule.onNodeWithText("Something needs attention").assertIsDisplayed()
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun variants(): List<Array<Any>> = HomeBannerVariant.entries.map { arrayOf<Any>(it) }
    }
}
