package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.onNodeWithText
import com.arshadshah.nimaz.presentation.theme.LocalUse24HourFormat
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
import com.google.common.truth.Truth.assertThat
import java.time.ZoneId
import kotlin.time.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A prayer time, written the way the reader's device writes times.
 *
 * `LocalUse24HourFormat` is a *user setting* rather than a locale read, because the app lets
 * somebody choose independently of their system format. Every prayer row, widget and notification
 * goes through this one helper so the choice cannot be honoured on some surfaces and not others —
 * which is exactly what happened before it existed.
 *
 * The zone is a parameter for the same reason the date picker's is: a prayer instant converted in
 * UTC rather than the device's zone is off by hours, and it looks like a calculation bug rather
 * than a formatting one.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class ClockTextTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    /** 2026-01-01T21:30:00Z. */
    private val evening = Instant.fromEpochSeconds(1_767_303_000)

    @Test
    fun `the same instant reads differently under the two clock settings`() {
        val texts = mutableListOf<String>()
        composeRule.setThemedContent {
            Column {
                CompositionLocalProvider(LocalUse24HourFormat provides true) {
                    texts += clockTimeText(evening, zone = ZoneId.of("UTC"))
                    NimazClockText(instant = evening, zone = ZoneId.of("UTC"))
                }
                CompositionLocalProvider(LocalUse24HourFormat provides false) {
                    texts += clockTimeText(evening, zone = ZoneId.of("UTC"))
                    NimazClockText(
                        instant = evening,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.Magenta,
                        zone = ZoneId.of("UTC"),
                    )
                }
            }
        }

        assertThat(texts).hasSize(2)
        assertThat(texts[0]).isNotEqualTo(texts[1])
        texts.forEach { composeRule.onNodeWithText(it).assertExists() }
    }

    @Test
    fun `the zone decides which hour is shown`() {
        // Not a formatting nicety: a prayer instant converted in the wrong zone is hours out and
        // reads as a calculation error rather than a display one.
        val texts = mutableListOf<String>()
        composeRule.setThemedContent {
            CompositionLocalProvider(LocalUse24HourFormat provides true) {
                texts += clockTimeText(evening, zone = ZoneId.of("UTC"))
                texts += clockTimeText(evening, zone = ZoneId.of("Asia/Karachi"))
            }
        }

        assertThat(texts[0]).isNotEqualTo(texts[1])
    }
}
