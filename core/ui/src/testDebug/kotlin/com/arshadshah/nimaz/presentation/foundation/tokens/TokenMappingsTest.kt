package com.arshadshah.nimaz.presentation.foundation.tokens

import com.arshadshah.nimaz.domain.model.CelebrationEvent
import com.arshadshah.nimaz.domain.model.SurahOverviewGroup
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Two small lookup tables that decide what the user sees, and that nothing else in the app reads.
 *
 * `CelebrationEvent.toOccasion()` is the join between the FCM announcement vocabulary and the
 * card the app draws for it — an Eid push arriving as an Ashura card is a mistake the sender
 * cannot see and the recipient cannot report. Both Ramadan events deliberately fold onto one
 * occasion, which is the only many-to-one in the table and therefore the one an "every event gets
 * its own" assertion would wrongly reject.
 *
 * `SurahOverviewGroup`'s icon and label decide the section headings of the surah info sheet. Two
 * groups sharing a heading is a sheet with two identical sections; two sharing an icon is a list a
 * reader cannot scan.
 */
@RunWith(RobolectricTestRunner::class)
class TokenMappingsTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `every celebration event maps to an occasion`() {
        CelebrationEvent.entries.forEach { event ->
            assertThat(event.toOccasion()).isNotNull()
        }
    }

    @Test
    fun `the two Ramadan events share one occasion and nothing else does`() {
        // Start and end are both "Ramadan" as far as the card is concerned. Every other event has
        // its own, so a second collision means an arm was copy-pasted.
        val byOccasion = CelebrationEvent.entries.groupBy { it.toOccasion() }
        val shared = byOccasion.filterValues { it.size > 1 }

        assertThat(shared.values.flatten()).containsExactly(
            CelebrationEvent.RAMADAN_START,
            CelebrationEvent.RAMADAN_END,
        )
    }

    @Test
    fun `the two Eids do not collide with each other`() {
        // The pair most likely to be confused, and the one where the ornament differs most — a
        // burst for Fitr, a patterned card for Adha.
        assertThat(CelebrationEvent.EID_AL_FITR.toOccasion())
            .isNotEqualTo(CelebrationEvent.EID_AL_ADHA.toOccasion())
    }

    @Test
    fun `every surah overview group has its own heading`() {
        val labels = mutableListOf<String>()
        composeRule.setThemedContent {
            SurahOverviewGroup.entries.forEach {
                labels += androidx.compose.ui.res.stringResource(it.labelRes)
            }
        }

        assertThat(labels).hasSize(SurahOverviewGroup.entries.size)
        assertThat(labels.toSet()).hasSize(SurahOverviewGroup.entries.size)
        assertThat(labels.none { it.isBlank() }).isTrue()
    }

    @Test
    fun `every surah overview group has its own icon`() {
        val icons = SurahOverviewGroup.entries.map { it.icon }

        assertThat(icons.toSet()).hasSize(SurahOverviewGroup.entries.size)
    }
}
