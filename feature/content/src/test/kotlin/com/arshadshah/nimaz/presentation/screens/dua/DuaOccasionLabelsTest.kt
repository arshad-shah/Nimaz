package com.arshadshah.nimaz.presentation.screens.dua

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.domain.model.DuaOccasion
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The occasion→resource mapping, asserted exhaustively.
 *
 * `DuaOccasion.displayName()` on the domain model returns hardcoded English; this mapping is
 * the presentation-layer replacement, and it is a seventeen-arm `when` written by hand. Two
 * arms pointing at the same resource is the failure it catches — "Entering the mosque" and
 * "Leaving the mosque" are one character apart in the source and opposite in meaning, and a
 * reader who sees the wrong one has no way to tell it is wrong.
 *
 * Exhaustive by construction: the assertion iterates `entries`, so an eighteenth occasion added
 * to the domain model without an arm here fails to compile in the mapping and fails here if it
 * is given a duplicate.
 */
@RunWith(RobolectricTestRunner::class)
class DuaOccasionLabelsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `every occasion has a label, and no two share one`() {
        val labels = DuaOccasion.entries.associateWith { context.getString(duaOccasionLabelRes(it)) }

        assertThat(labels).hasSize(DuaOccasion.entries.size)
        labels.forEach { (occasion, label) ->
            assertThat(label).isNotEmpty()
            // A label that is still the enum constant means the arm was never written.
            assertThat(label).isNotEqualTo(occasion.name)
        }
        assertThat(labels.values.toSet()).hasSize(DuaOccasion.entries.size)
    }

    @Test
    fun `the two mosque occasions are opposite, not interchangeable`() {
        // One character apart in the source; opposite on screen.
        val entering = context.getString(duaOccasionLabelRes(DuaOccasion.ENTERING_MOSQUE))
        val leaving = context.getString(duaOccasionLabelRes(DuaOccasion.LEAVING_MOSQUE))

        assertThat(entering).isNotEqualTo(leaving)
        assertThat(entering).ignoringCase().contains("enter")
        assertThat(leaving).ignoringCase().contains("leav")
    }

    @Test
    fun `the two home occasions are opposite, not interchangeable`() {
        val entering = context.getString(duaOccasionLabelRes(DuaOccasion.ENTERING_HOME))
        val leaving = context.getString(duaOccasionLabelRes(DuaOccasion.LEAVING_HOME))

        assertThat(entering).isNotEqualTo(leaving)
        assertThat(entering).ignoringCase().contains("enter")
        assertThat(leaving).ignoringCase().contains("leav")
    }
}
