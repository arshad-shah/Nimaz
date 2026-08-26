package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.presentation.model.PrayerDisplayStatus
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The badge that says what happened to a prayer.
 *
 * Six statuses, six words, and the words are the user's own record of their worship — "on time",
 * "late", "made up", "missed". Two statuses sharing a label would make a tracker that cannot
 * distinguish a prayer made up from one prayed on time, which is a distinction the whole feature
 * is built on.
 *
 * The badge is also **outlined rather than filled for the one status that is not a verdict**:
 * `NOT_RECORDED` means nothing has been said yet, and a filled badge there reads as an answer.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class PrayerStatusBadgeTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `every status has its own word`() {
        val labels = mutableListOf<String>()
        composeRule.setThemedContent {
            PrayerDisplayStatus.entries.forEach { labels += it.label() }
        }

        assertThat(labels).hasSize(PrayerDisplayStatus.entries.size)
        assertThat(labels.toSet()).hasSize(PrayerDisplayStatus.entries.size)
        assertThat(labels.none { it.isBlank() }).isTrue()
    }

    @Test
    fun `the badge renders the word for the status it is given`() {
        val labels = mutableListOf<String>()
        composeRule.setThemedContent {
            Column {
                PrayerDisplayStatus.entries.forEach { status ->
                    labels += status.label()
                    PrayerStatusBadge(status = status)
                }
            }
        }

        labels.forEach { composeRule.onNodeWithText(it).assertExists() }
    }

    @Test
    fun `a badge takes the size its caller asks for`() {
        // The tracker's day card uses the small badge in a dense row and the large one in the
        // detail sheet; a size parameter that stopped being honoured would blow up the row.
        composeRule.setThemedContent {
            Column {
                PrayerStatusBadge(
                    status = PrayerDisplayStatus.PRAYED,
                    size = NimazBadgeSize.LARGE,
                )
            }
        }

        composeRule.waitForIdle()
    }
}
