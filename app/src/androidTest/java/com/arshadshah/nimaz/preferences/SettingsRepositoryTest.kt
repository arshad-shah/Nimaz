package com.arshadshah.nimaz.preferences

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Round-trips a representative slice of the DataStore-backed [SettingsRepository] —
 * the persistence behind every toggle on the settings screens. Verifies writes are
 * observable on the corresponding flow, and that the export → import path preserves
 * values (used by the device-to-device sync feature).
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SettingsRepositoryTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var settings: SettingsRepository

    @Before
    fun setup() = hiltRule.inject()

    @Test
    fun themeMode_writeIsObservable() = runTest {
        settings.setThemeMode("dark")
        assertThat(settings.themeMode.first()).isEqualTo("dark")

        settings.setThemeMode("light")
        assertThat(settings.themeMode.first()).isEqualTo("light")
    }

    @Test
    fun booleanToggles_roundTrip() = runTest {
        settings.setHapticFeedback(false)
        settings.setUse24HourFormat(true)
        settings.setDynamicColor(false)

        assertThat(settings.hapticFeedback.first()).isFalse()
        assertThat(settings.use24HourFormat.first()).isTrue()
        assertThat(settings.dynamicColor.first()).isFalse()
    }

    @Test
    fun prayerCalculation_settingsPersist() = runTest {
        settings.setCalculationMethod("ISNA")
        settings.setAsrCalculation("hanafi")
        settings.setPrayerNotificationsEnabled(true)

        assertThat(settings.calculationMethod.first()).isEqualTo("ISNA")
        assertThat(settings.asrCalculation.first()).isEqualTo("hanafi")
        assertThat(settings.prayerNotificationsEnabled.first()).isTrue()
    }

    @Test
    fun location_updatePersistsLatLngAndName() = runTest {
        settings.updateLocation(latitude = 21.4225, longitude = 39.8262, name = "Makkah")

        assertThat(settings.latitude.first()).isWithin(0.0001).of(21.4225)
        assertThat(settings.longitude.first()).isWithin(0.0001).of(39.8262)
        assertThat(settings.locationName.first()).isEqualTo("Makkah")
    }

    @Test
    fun quranMushafScript_defaultsToMadaniAndRoundTrips() = runTest {
        // Off by default: the 16-line IndoPak view must be opt-in (#270).
        assertThat(settings.quranMushafScript.first()).isEqualTo("MADANI")

        settings.setQuranMushafScript("INDOPAK_16")
        assertThat(settings.quranMushafScript.first()).isEqualTo("INDOPAK_16")

        settings.setQuranMushafScript("MADANI")
        assertThat(settings.quranMushafScript.first()).isEqualTo("MADANI")
    }

    @Test
    fun exportThenImport_preservesValues() = runTest {
        settings.setThemeMode("dark")
        settings.setCalculationMethod("Egypt")

        val exported = settings.exportAllPreferences()
        // Mutate, then restore from the export.
        settings.setThemeMode("light")
        settings.importPreferences(exported)

        assertThat(settings.themeMode.first()).isEqualTo("dark")
        assertThat(settings.calculationMethod.first()).isEqualTo("Egypt")
    }
}
