package com.arshadshah.nimaz.presentation.components.atoms.qibla

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.domain.model.CompassAccuracy
import com.arshadshah.nimaz.presentation.foundation.tokens.AccuracyVisuals
import com.arshadshah.nimaz.presentation.foundation.tokens.accuracyVisuals
import com.arshadshah.nimaz.presentation.foundation.tokens.needsCalibration
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The small text pieces of the qibla screen: the turn hint, the fact cards, and the compass
 * accuracy verdict.
 *
 * The turn hint is the one that matters most and is the easiest to get backwards. It renders both
 * a **glyph** (rotate-right or rotate-left) and a **sentence** ("Turn right 47°"), from one
 * boolean, and the two are separate expressions in the same composable — so a change touching one
 * and not the other produces an arrow pointing one way over words saying the other. It also takes
 * `abs(degrees)`, because the caller's signed bearing arrives negative for a left turn and "Turn
 * left −47°" is nonsense.
 *
 * `accuracyVisuals` is the compass's honesty: LOW and UNRELIABLE both need calibration, and the
 * `needsCalibration` predicate is read separately from the visuals, so the two can disagree.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class QiblaAtomsTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `the turn hint names the direction and the number of degrees`() {
        composeRule.setThemedContent {
            Column {
                QiblaTurnHint(turnRight = true, degrees = 47, color = Color.Unspecified)
                QiblaTurnHint(turnRight = false, degrees = 112, color = Color.Unspecified)
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.turn_right_format, 47)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.turn_left_format, 112)).assertExists()
    }

    @Test
    fun `a negative bearing reads as a positive number of degrees`() {
        // The caller's bearing is signed — negative means turn left — and `abs` is what keeps
        // "Turn left −47°" off the screen. It is one call away from being dropped.
        composeRule.setThemedContent {
            QiblaTurnHint(turnRight = false, degrees = -47, color = Color.Unspecified)
        }

        composeRule.onNodeWithText(context.getString(R.string.turn_left_format, 47)).assertExists()
    }

    @Test
    fun `left and right are different sentences`() {
        // Two resources. One string for both — "Turn 47°" — leaves the user guessing which way,
        // which is the entire question the hint exists to answer.
        assertThat(context.getString(R.string.turn_right_format, 47))
            .isNotEqualTo(context.getString(R.string.turn_left_format, 47))
    }

    @Test
    fun `a fact card shouts its label and prints its value as given`() {
        // The label is uppercased by the component and the value is not — the value is a bearing
        // or a distance, and uppercasing "118° SE" would be wrong for units that carry case.
        composeRule.setThemedContent {
            QiblaFactCard(label = "Qibla bearing", value = "118° SE")
        }

        composeRule.onNodeWithText("QIBLA BEARING").assertExists()
        composeRule.onNodeWithText("118° SE").assertExists()
    }

    @Test
    fun `every compass accuracy has its own words and its own hint`() {
        // Four verdicts the user acts on. Two sharing a label is a compass that cannot tell the
        // user whether to trust it.
        val visuals = mutableMapOf<CompassAccuracy, AccuracyVisuals>()
        composeRule.setThemedContent {
            MaterialTheme {
                CompassAccuracy.entries.forEach { visuals[it] = accuracyVisuals(it) }
            }
        }

        assertThat(visuals).hasSize(CompassAccuracy.entries.size)
        assertThat(visuals.values.map { it.label }.toSet())
            .hasSize(CompassAccuracy.entries.size)
        assertThat(visuals.values.map { it.hint }.toSet())
            .hasSize(CompassAccuracy.entries.size)
        visuals.values.forEach {
            assertThat(it.label).isNotEmpty()
            assertThat(it.hint).isNotEmpty()
        }
    }

    @Test
    fun `high accuracy is not painted like a warning`() {
        // Green for good, gold for middling, error for the two that need calibration — the colour
        // is what the user reads before the words.
        val visuals = mutableMapOf<CompassAccuracy, AccuracyVisuals>()
        composeRule.setThemedContent {
            MaterialTheme {
                CompassAccuracy.entries.forEach { visuals[it] = accuracyVisuals(it) }
            }
        }

        assertThat(visuals.getValue(CompassAccuracy.HIGH).color)
            .isNotEqualTo(visuals.getValue(CompassAccuracy.LOW).color)
        assertThat(visuals.getValue(CompassAccuracy.MEDIUM).color)
            .isNotEqualTo(visuals.getValue(CompassAccuracy.HIGH).color)
    }

    @Test
    fun `only the two untrustworthy readings ask for calibration`() {
        // Read separately from the visuals, so the two can disagree — a MEDIUM reading that
        // demanded calibration would nag on every phone with an ordinary magnetometer.
        val asking = CompassAccuracy.entries.filter { needsCalibration(it) }

        assertThat(asking).containsExactly(CompassAccuracy.LOW, CompassAccuracy.UNRELIABLE)
    }
}
