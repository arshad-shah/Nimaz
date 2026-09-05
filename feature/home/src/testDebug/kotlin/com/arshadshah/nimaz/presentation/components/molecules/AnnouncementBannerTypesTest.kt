package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.domain.model.Announcement
import com.arshadshah.nimaz.domain.model.AnnouncementType
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner

/**
 * Every announcement type renders with its own tone and icon.
 *
 * Parameterised rather than a loop inside one test: a Compose rule only works as a `@Rule`, so
 * one composition per type means one *test* per type.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
class AnnouncementBannerTypesTest(private val type: AnnouncementType) {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `the banner renders for this type`() {
        composeRule.setThemedContent {
            AnnouncementBanner(
                announcement = Announcement(
                    id = "a1",
                    type = type,
                    title = "Something happened",
                    body = "And here is what it was.",
                ),
                showCta = false,
                onCtaClick = {},
                onDismiss = {},
            )
        }

        composeRule.onNodeWithText("Something happened").assertIsDisplayed()
        composeRule.onNodeWithText("And here is what it was.").assertIsDisplayed()
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun types(): List<Array<Any>> = AnnouncementType.entries.map { arrayOf<Any>(it) }
    }
}
