package com.arshadshah.nimaz.presentation.components.atoms

import androidx.compose.ui.graphics.Color
import com.arshadshah.nimaz.domain.model.PrayerType
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PrayerVisualsTest {

    @get:Rule
    val composeRule = createComponentComposeRule()

    @Test
    fun `getArabicPrayerName maps every prayer type`() {
        assertThat(
            _root_ide_package_.com.arshadshah.nimaz.presentation.foundation.tokens.getArabicPrayerName(
                PrayerType.FAJR
            )
        ).isEqualTo("الفجر")
        assertThat(
            _root_ide_package_.com.arshadshah.nimaz.presentation.foundation.tokens.getArabicPrayerName(
                PrayerType.SUNRISE
            )
        ).isEqualTo("الشروق")
        assertThat(
            _root_ide_package_.com.arshadshah.nimaz.presentation.foundation.tokens.getArabicPrayerName(
                PrayerType.DHUHR
            )
        ).isEqualTo("الظهر")
        assertThat(
            _root_ide_package_.com.arshadshah.nimaz.presentation.foundation.tokens.getArabicPrayerName(
                PrayerType.ASR
            )
        ).isEqualTo("العصر")
        assertThat(
            _root_ide_package_.com.arshadshah.nimaz.presentation.foundation.tokens.getArabicPrayerName(
                PrayerType.MAGHRIB
            )
        ).isEqualTo("المغرب")
        assertThat(
            _root_ide_package_.com.arshadshah.nimaz.presentation.foundation.tokens.getArabicPrayerName(
                PrayerType.ISHA
            )
        ).isEqualTo("العشاء")
        assertThat(
            _root_ide_package_.com.arshadshah.nimaz.presentation.foundation.tokens.getArabicPrayerName(
                null
            )
        ).isEmpty()
    }

    @Test
    fun `getPrayerIcon maps every prayer type`() {
        // Each prayer maps to its custom sun-stage icon (see PrayerStageIcons);
        // null falls back to the Dhuhr (apex) icon.
        assertThat(
            _root_ide_package_.com.arshadshah.nimaz.presentation.foundation.tokens.getPrayerIcon(
                PrayerType.FAJR
            ).name).isEqualTo("PrayerFajr")
        assertThat(
            _root_ide_package_.com.arshadshah.nimaz.presentation.foundation.tokens.getPrayerIcon(
                PrayerType.SUNRISE
            ).name).isEqualTo("PrayerSunrise")
        assertThat(
            _root_ide_package_.com.arshadshah.nimaz.presentation.foundation.tokens.getPrayerIcon(
                PrayerType.DHUHR
            ).name).isEqualTo("PrayerDhuhr")
        assertThat(
            _root_ide_package_.com.arshadshah.nimaz.presentation.foundation.tokens.getPrayerIcon(
                PrayerType.ASR
            ).name).isEqualTo("PrayerAsr")
        assertThat(
            _root_ide_package_.com.arshadshah.nimaz.presentation.foundation.tokens.getPrayerIcon(
                PrayerType.MAGHRIB
            ).name).isEqualTo("PrayerMaghrib")
        assertThat(
            _root_ide_package_.com.arshadshah.nimaz.presentation.foundation.tokens.getPrayerIcon(
                PrayerType.ISHA
            ).name).isEqualTo("PrayerIsha")
        assertThat(
            _root_ide_package_.com.arshadshah.nimaz.presentation.foundation.tokens.getPrayerIcon(
                null
            ).name).isEqualTo("PrayerDhuhr")
    }

    @Test
    fun `getPrayerColor maps known prayer types and falls back to primary`() {
        val colors = mutableMapOf<String, Color>()
        val all: List<PrayerType?> = listOf(
            PrayerType.FAJR, PrayerType.SUNRISE, PrayerType.DHUHR,
            PrayerType.ASR, PrayerType.MAGHRIB, PrayerType.ISHA, null
        )
        composeRule.setThemedContent {
            all.forEach { type ->
                colors[type?.name ?: "NULL"] =
                    _root_ide_package_.com.arshadshah.nimaz.presentation.foundation.tokens.getPrayerColor(
                        type
                    )
            }
        }
        composeRule.waitForIdle()

        assertThat(colors["FAJR"]).isEqualTo(Color(0xFF6366F1))
        assertThat(colors["SUNRISE"]).isEqualTo(Color(0xFFF59E0B))
        assertThat(colors["DHUHR"]).isEqualTo(Color(0xFFEAB308))
        assertThat(colors["ASR"]).isEqualTo(Color(0xFFF97316))
        assertThat(colors["MAGHRIB"]).isEqualTo(Color(0xFFEF4444))
        assertThat(colors["ISHA"]).isEqualTo(Color(0xFF8B5CF6))
        // The null/fallback branch resolves to the theme primary, which differs
        // from all of the explicit prayer colours above.
        assertThat(colors["NULL"]).isNotEqualTo(Color(0xFF6366F1))
    }
}
