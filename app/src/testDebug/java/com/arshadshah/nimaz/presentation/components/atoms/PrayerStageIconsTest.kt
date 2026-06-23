package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PrayerStageIconsTest {
    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `prayer icons expose distinct vector names`() {
        assertThat(PrayerIconFajr.name).isEqualTo("PrayerFajr")
        assertThat(PrayerIconSunrise.name).isEqualTo("PrayerSunrise")
        assertThat(PrayerIconDhuhr.name).isEqualTo("PrayerDhuhr")
        assertThat(PrayerIconAsr.name).isEqualTo("PrayerAsr")
        assertThat(PrayerIconMaghrib.name).isEqualTo("PrayerMaghrib")
        assertThat(PrayerIconIsha.name).isEqualTo("PrayerIsha")
    }

    @Test
    fun `prayer icons render in an Icon without crashing`() {
        composeRule.setThemedContent {
            Icon(
                imageVector = PrayerIconFajr,
                contentDescription = "Fajr",
                modifier = Modifier.size(32.dp),
            )
        }
        composeRule.waitForIdle()
        composeRule.onRoot().assertExists()
    }
}
