package com.arshadshah.nimaz.presentation.foundation.tokens

import androidx.compose.ui.graphics.Color
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.testing.compose.createComponentComposeRule
import com.arshadshah.nimaz.testing.compose.setThemedContent
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
        assertThat(getArabicPrayerName(PrayerType.FAJR)).isEqualTo("الفجر")
        assertThat(getArabicPrayerName(PrayerType.SUNRISE)).isEqualTo("الشروق")
        assertThat(getArabicPrayerName(PrayerType.DHUHR)).isEqualTo("الظهر")
        assertThat(getArabicPrayerName(PrayerType.ASR)).isEqualTo("العصر")
        assertThat(getArabicPrayerName(PrayerType.MAGHRIB)).isEqualTo("المغرب")
        assertThat(getArabicPrayerName(PrayerType.ISHA)).isEqualTo("العشاء")
        assertThat(getArabicPrayerName(null)).isEmpty()
    }

    @Test
    fun `getPrayerIcon maps every prayer type`() {
        assertThat(getPrayerIcon(PrayerType.FAJR).name).isEqualTo("PrayerFajr")
        assertThat(getPrayerIcon(PrayerType.SUNRISE).name).isEqualTo("PrayerSunrise")
        assertThat(getPrayerIcon(PrayerType.DHUHR).name).isEqualTo("PrayerDhuhr")
        assertThat(getPrayerIcon(PrayerType.ASR).name).isEqualTo("PrayerAsr")
        assertThat(getPrayerIcon(PrayerType.MAGHRIB).name).isEqualTo("PrayerMaghrib")
        assertThat(getPrayerIcon(PrayerType.ISHA).name).isEqualTo("PrayerIsha")
        assertThat(getPrayerIcon(null).name).isEqualTo("PrayerDhuhr")
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
                colors[type?.name ?: "NULL"] = getPrayerColor(type)
            }
        }
        composeRule.waitForIdle()

        assertThat(colors["FAJR"]).isEqualTo(Color(0xFF6366F1))
        assertThat(colors["SUNRISE"]).isEqualTo(Color(0xFFF59E0B))
        assertThat(colors["DHUHR"]).isEqualTo(Color(0xFFEAB308))
        assertThat(colors["ASR"]).isEqualTo(Color(0xFFF97316))
        assertThat(colors["MAGHRIB"]).isEqualTo(Color(0xFFEF4444))
        assertThat(colors["ISHA"]).isEqualTo(Color(0xFF8B5CF6))
        assertThat(colors["NULL"]).isNotEqualTo(Color(0xFF6366F1))
    }
}
