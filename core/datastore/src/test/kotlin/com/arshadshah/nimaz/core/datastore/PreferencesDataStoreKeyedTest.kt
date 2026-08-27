package com.arshadshah.nimaz.core.datastore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.domain.model.PrayerAlertStyle
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The preferences that take a prayer as an argument, and the one migration that rewrites them.
 *
 * `PreferencesDataStoreTest` covers the plain pairs. These are different in shape and in risk: a
 * `when (prayer.lowercase())` over six arms, written three times over, each arm one line long and
 * each line differing from its neighbour by one word. A crossed arm — Asr's toggle writing
 * Maghrib's key — is not visible in the settings screen (both switches move independently, one of
 * them just moves the wrong prayer) and it is not visible in the key golden, because both keys
 * exist and both are spelled right.
 *
 * So the property here is **isolation**: writing one prayer's setting must move that prayer's and
 * no other's. Six writes, thirty-six reads, per family.
 *
 * The `else -> return@edit` arm is the other half. An unknown prayer name reaches these from a
 * sync payload written by a newer build, and silently writing nothing is the intended answer —
 * but so is silently writing nothing *anywhere else*, which is what the test checks.
 */
@RunWith(RobolectricTestRunner::class)
class PreferencesDataStoreKeyedTest {

    private lateinit var store: PreferencesDataStore

    /** Sunrise has an adjustment and a notification, but never an adhan. */
    private val allPrayers = listOf("fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha")
    private val adhanPrayers = listOf("fajr", "dhuhr", "asr", "maghrib", "isha")

    @Before
    fun setUp() {
        store = PreferencesDataStore(ApplicationProvider.getApplicationContext<Context>())
    }

    private fun adjustment(prayer: String): Flow<Int> = when (prayer) {
        "fajr" -> store.fajrAdjustment
        "sunrise" -> store.sunriseAdjustment
        "dhuhr" -> store.dhuhrAdjustment
        "asr" -> store.asrAdjustment
        "maghrib" -> store.maghribAdjustment
        else -> store.ishaAdjustment
    }

    private fun notification(prayer: String): Flow<Boolean> = when (prayer) {
        "fajr" -> store.fajrNotificationEnabled
        "sunrise" -> store.sunriseNotificationEnabled
        "dhuhr" -> store.dhuhrNotificationEnabled
        "asr" -> store.asrNotificationEnabled
        "maghrib" -> store.maghribNotificationEnabled
        else -> store.ishaNotificationEnabled
    }

    private fun adhan(prayer: String): Flow<Boolean> = when (prayer) {
        "fajr" -> store.fajrAdhanEnabled
        "dhuhr" -> store.dhuhrAdhanEnabled
        "asr" -> store.asrAdhanEnabled
        "maghrib" -> store.maghribAdhanEnabled
        else -> store.ishaAdhanEnabled
    }

    // ---- Time adjustments ----

    @Test
    fun `each prayer's adjustment reaches that prayer and no other`() = runTest {
        store.clearAllData()

        allPrayers.forEachIndexed { index, prayer ->
            val minutes = (index + 1) * 3
            store.setPrayerAdjustment(prayer, minutes)

            allPrayers.forEach { other ->
                val expected = if (other == prayer) minutes else 0
                assertWithMessage("$other after adjusting $prayer")
                    .that(adjustment(other).first())
                    .isEqualTo(expected)
            }
            store.clearAllData()
        }
    }

    @Test
    fun `a negative adjustment is stored as given`() = runTest {
        // Adjustments run both ways: a user whose mosque prays five minutes early sets -5.
        store.clearAllData()

        store.setPrayerAdjustment("fajr", -5)

        assertThat(store.fajrAdjustment.first()).isEqualTo(-5)
    }

    @Test
    fun `the prayer name is matched without regard to case`() = runTest {
        // Callers pass a `PrayerName.name`, which is upper case.
        store.clearAllData()

        store.setPrayerAdjustment("FAJR", 4)
        store.setPrayerAdjustment("Maghrib", 6)

        assertThat(store.fajrAdjustment.first()).isEqualTo(4)
        assertThat(store.maghribAdjustment.first()).isEqualTo(6)
    }

    @Test
    fun `an adjustment for a prayer that does not exist changes nothing`() = runTest {
        store.clearAllData()

        store.setPrayerAdjustment("tahajjud", 30)

        allPrayers.forEach {
            assertWithMessage(it).that(adjustment(it).first()).isEqualTo(0)
        }
    }

    // ---- Per-prayer notification toggles ----

    @Test
    fun `each prayer's notification toggle reaches that prayer and no other`() = runTest {
        store.clearAllData()
        // Sunrise is the one that defaults off, so the table cannot assume a single default.
        val defaults = allPrayers.associateWith { it != "sunrise" }

        allPrayers.forEach { prayer ->
            store.setPrayerNotificationEnabled(prayer, !defaults.getValue(prayer))

            allPrayers.forEach { other ->
                val expected =
                    if (other == prayer) !defaults.getValue(other) else defaults.getValue(other)
                assertWithMessage("$other after toggling $prayer")
                    .that(notification(other).first())
                    .isEqualTo(expected)
            }
            store.clearAllData()
        }
    }

    @Test
    fun `sunrise is the one prayer whose notification is off to begin with`() = runTest {
        // It is not a prayer; the notification exists for people who want the fajr cut-off.
        store.clearAllData()

        assertThat(store.sunriseNotificationEnabled.first()).isFalse()
        assertThat(store.fajrNotificationEnabled.first()).isTrue()
    }

    @Test
    fun `a notification toggle for an unknown prayer changes nothing`() = runTest {
        store.clearAllData()

        store.setPrayerNotificationEnabled("tahajjud", false)

        assertThat(store.fajrNotificationEnabled.first()).isTrue()
        assertThat(store.ishaNotificationEnabled.first()).isTrue()
    }

    // ---- Per-prayer adhan toggles ----

    @Test
    fun `each prayer's adhan toggle reaches that prayer and no other`() = runTest {
        store.clearAllData()

        adhanPrayers.forEach { prayer ->
            store.setPrayerAdhanEnabled(prayer, false)

            adhanPrayers.forEach { other ->
                assertWithMessage("$other after toggling $prayer")
                    .that(adhan(other).first())
                    .isEqualTo(other != prayer)
            }
            store.clearAllData()
        }
    }

    @Test
    fun `sunrise never gets an adhan, whatever is written`() = runTest {
        // There is no sunrise adhan key; the setter has no arm for it and the reader answers
        // false outright, so a sync payload naming it cannot turn one on.
        store.clearAllData()

        store.setPrayerAdhanEnabled("sunrise", true)

        assertThat(store.isAdhanEnabledForPrayer("sunrise").first()).isFalse()
    }

    @Test
    fun `the adhan lookup answers per prayer`() = runTest {
        store.clearAllData()
        store.setPrayerAdhanEnabled("asr", false)

        adhanPrayers.forEach { prayer ->
            assertWithMessage(prayer)
                .that(store.isAdhanEnabledForPrayer(prayer).first())
                .isEqualTo(prayer != "asr")
        }
    }

    @Test
    fun `the adhan lookup answers no for a prayer it has never heard of`() = runTest {
        store.clearAllData()

        assertThat(store.isAdhanEnabledForPrayer("tahajjud").first()).isFalse()
    }

    // ---- Alert style and pre-reminder ----

    @Test
    fun `each prayer's alert style is its own`() = runTest {
        store.clearAllData()

        store.setPrayerAlertStyle("fajr", PrayerAlertStyle.SILENT)

        assertThat(store.prayerAlertStyle("fajr").first()).isEqualTo(PrayerAlertStyle.SILENT)
        assertThat(store.prayerAlertStyle("isha").first())
            .isNotEqualTo(PrayerAlertStyle.SILENT)
    }

    @Test
    fun `every alert style round-trips through storage`() = runTest {
        store.clearAllData()

        PrayerAlertStyle.entries.forEach { style ->
            store.setPrayerAlertStyle("dhuhr", style)
            assertWithMessage(style.name)
                .that(store.prayerAlertStyle("dhuhr").first())
                .isEqualTo(style)
        }
    }

    @Test
    fun `each prayer's pre-reminder is its own`() = runTest {
        store.clearAllData()

        store.setPrayerReminderEnabled("maghrib", true)
        store.setPrayerReminderMinutes("maghrib", 25)

        assertThat(store.prayerReminderEnabled("maghrib").first()).isTrue()
        assertThat(store.prayerReminderMinutes("maghrib").first()).isEqualTo(25)
        assertThat(store.prayerReminderEnabled("fajr").first()).isFalse()
        assertThat(store.prayerReminderMinutes("fajr").first())
            .isNotEqualTo(25)
    }

    // ---- Worship reminders, which are keyed by an arbitrary string ----

    @Test
    fun `a worship reminder is stored under the key it was given`() = runTest {
        store.clearAllData()

        store.setWorshipReminderEnabled("tahajjud", true)
        store.setWorshipReminderOffset("tahajjud", 45)
        store.setWorshipReminderMode("tahajjud", "notification")

        assertThat(store.worshipReminderEnabled("tahajjud").first()).isTrue()
        assertThat(store.worshipReminderOffset("tahajjud", default = 30).first()).isEqualTo(45)
        assertThat(store.worshipReminderMode("tahajjud", default = "off").first())
            .isEqualTo("notification")
    }

    @Test
    fun `one worship reminder does not reach another`() = runTest {
        store.clearAllData()

        store.setWorshipReminderEnabled("tahajjud", true)

        assertThat(store.worshipReminderEnabled("duha").first()).isFalse()
    }

    @Test
    fun `a worship reminder that has never been set reads the caller's default`() = runTest {
        // The default is a *parameter* here, because each window has its own sensible offset.
        store.clearAllData()

        assertThat(store.worshipReminderOffset("duha", default = 20).first()).isEqualTo(20)
        assertThat(store.worshipReminderMode("duha", default = "off").first()).isEqualTo("off")
    }

    // ---- Export and import ----

    @Test
    fun `an exported payload carries only what has been set`() = runTest {
        store.clearAllData()

        assertThat(store.exportAllPreferences()).isEmpty()

        store.setThemeMode("dark")

        assertThat(store.exportAllPreferences()).containsKey("theme_mode")
    }

    @Test
    fun `a payload round-trips through export and import`() = runTest {
        store.clearAllData()
        store.setThemeMode("dark")
        store.setQuranArabicFontSize(34f)
        store.setTasbihSelectedPresetId(9L)
        store.setZakatGoldPricePerGram(72.5)
        store.setPrayerAdjustment("fajr", -3)
        val exported = store.exportAllPreferences()

        store.clearAllData()
        store.importPreferences(exported)

        // Every type family: string, float, long, double, int.
        assertThat(store.themeMode.first()).isEqualTo("dark")
        assertThat(store.quranArabicFontSize.first()).isEqualTo(34f)
        assertThat(store.tasbihSelectedPresetId.first()).isEqualTo(9L)
        assertThat(store.zakatGoldPricePerGram.first()).isEqualTo(72.5)
        assertThat(store.fajrAdjustment.first()).isEqualTo(-3)
    }

    @Test
    fun `a key the codec does not know is skipped rather than throwing`() = runTest {
        // A payload from a newer build carries keys this one has never declared. DataStore keys
        // are typed, so guessing at one is how an import ends in a ClassCastException on read.
        store.clearAllData()

        store.importPreferences(mapOf("a_key_from_the_future" to "whatever", "theme_mode" to "dark"))

        assertThat(store.themeMode.first()).isEqualTo("dark")
    }

    @Test
    fun `importing nothing changes nothing`() = runTest {
        store.clearAllData()
        store.setThemeMode("dark")

        store.importPreferences(emptyMap())

        assertThat(store.themeMode.first()).isEqualTo("dark")
    }

    // ---- The one-shot migration ----

    @Test
    fun `the prayer-notification migration runs once and is then a no-op`() = runTest {
        store.clearAllData()
        store.setAdhanEnabled(true)

        store.migratePrayerNotificationPreferences()
        val afterFirst = store.prayerAlertStyle("fajr").first()

        // A reader who has since chosen something else must not have it reset on next launch.
        store.setPrayerAlertStyle("fajr", PrayerAlertStyle.SILENT)
        store.migratePrayerNotificationPreferences()

        assertThat(afterFirst).isNotNull()
        assertThat(store.prayerAlertStyle("fajr").first()).isEqualTo(PrayerAlertStyle.SILENT)
    }

    @Test
    fun `the migration gives every prayer an alert style`() = runTest {
        store.clearAllData()
        store.setAdhanEnabled(true)

        store.migratePrayerNotificationPreferences()

        listOf("fajr", "dhuhr", "asr", "maghrib", "isha").forEach { prayer ->
            assertWithMessage(prayer).that(store.prayerAlertStyle(prayer).first()).isNotNull()
        }
    }

    @Test
    fun `the migration carries the old global pre-reminder across`() = runTest {
        store.clearAllData()
        store.setShowReminderBefore(true)
        store.setNotificationReminderMinutes(20)

        store.migratePrayerNotificationPreferences()

        assertThat(store.prayerReminderEnabled("fajr").first()).isTrue()
        assertThat(store.prayerReminderMinutes("fajr").first()).isEqualTo(20)
    }

    @Test
    fun `the migration leaves the pre-reminder off when it was off`() = runTest {
        store.clearAllData()
        store.setShowReminderBefore(false)

        store.migratePrayerNotificationPreferences()

        assertThat(store.prayerReminderEnabled("fajr").first()).isFalse()
    }
}
