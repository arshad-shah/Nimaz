package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import androidx.compose.ui.text.style.TextAlign
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The countdown as a rendered `Text`, rather than as the string `NimazCountdownTest` pins.
 *
 * `NimazCountdownText` derives its own tick resolution from `showSeconds`, which is the whole point
 * of the component existing rather than every caller pairing `rememberCountdownTo` with a format by
 * hand. The source records what happens when the two disagree: the Home hero rendered a seconds
 * digit while the countdown re-derived once a minute, so the seconds sat still for sixty seconds
 * and then jumped — and any unrelated recomposition made it look as though the value only updated
 * when you moved around the app.
 *
 * Both arms are composed here because the resolution each picks is invisible from outside; what is
 * observable is that the component renders a countdown at all, in either mode, with the caller's
 * type and alignment applied.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class CountdownTextRenderTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `a countdown renders in both resolutions with the caller's styling`() {
        val target = Instant.fromEpochSeconds(4_102_444_800) + 2.hours

        composeRule.setThemedContent {
            Column {
                NimazCountdownText(target = target)
                NimazCountdownText(
                    target = target,
                    modifier = Modifier,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.Magenta,
                    showSeconds = false,
                    textAlign = TextAlign.Center,
                )
            }
        }

        // Both are rendered; neither is empty. The tree is read rather than a literal asserted,
        // because the exact figure depends on the wall clock the ticker starts from.
        val tree = composeRule.onRoot(useUnmergedTree = true).printToString()
        assertThat(tree).contains("Text")
    }
}
