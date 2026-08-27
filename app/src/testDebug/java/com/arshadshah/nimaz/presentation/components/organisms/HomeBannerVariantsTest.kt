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
 * Every banner variant maps to a rendered banner.
 *
 * The mapping is a four-arm `when` over an enum, and three of its arms had never run: the
 * carousel is only ever handed WARNING banners by `HomeScreen`'s own builder, so UPDATE, INFO
 * and EVENT reach it only through an announcement. An arm that fails to map is not a compile
 * error and shows as a banner with the wrong colour, which is exactly the kind of thing nobody
 * reports.
 *
 * Parameterised because a compose rule only works as a `@Rule` — one composition per variant
 * means one test per variant.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
class HomeBannerVariantsTest(private val variant: HomeBannerVariant) {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private fun item(loading: Boolean = false) = HomeBannerItem(
        id = "b1",
        icon = Icons.Default.Info,
        title = "Something needs attention",
        variant = variant,
        subtitle = "And here is what it is",
        actionLabel = "Fix",
        onAction = {},
        isLoading = loading,
    )

    @Test
    fun `the carousel renders this variant with its title and subtitle`() {
        composeRule.setThemedContent { HomeBannerCarousel(banners = listOf(item())) }

        composeRule.onNodeWithText("Something needs attention").assertIsDisplayed()
        composeRule.onNodeWithText("And here is what it is").assertIsDisplayed()
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun variants(): List<Array<Any>> = HomeBannerVariant.entries.map { arrayOf<Any>(it) }
    }
}
