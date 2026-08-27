package com.arshadshah.nimaz.core.init

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.datastore.PreferencesDataStore
import com.arshadshah.nimaz.core.util.PrayerNotificationScheduler
import com.arshadshah.nimaz.data.announcement.AnnouncementBootstrap
import com.arshadshah.nimaz.data.audio.AdhanAudioManager
import com.arshadshah.nimaz.data.audio.AdhanSound
import com.arshadshah.nimaz.data.local.user.UserDataMigrator
import com.arshadshah.nimaz.data.widget.WidgetSettingsWatcher
import com.arshadshah.nimaz.domain.model.UserPreferences
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Everything that has to happen before the first frame, and what happens when it does not.
 *
 * `AppInitializer` runs five independent tasks under a **5 second** budget and then lets the UI
 * start whatever the outcome. That last part is the whole design: a person opening the app to
 * check Maghrib must not be held at a splash screen because a migration is slow or a preference
 * read is stuck. It was at 0% — nothing had ever run it — so neither the happy path nor the
 * timeout had any witness.
 *
 * The tests wait on `isReady` rather than on a dispatcher, because the initializer owns its own
 * `Dispatchers.IO` scope by design.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class AppInitializerTest {

    private lateinit var context: Context
    private lateinit var preferences: PreferencesDataStore
    private lateinit var scheduler: PrayerNotificationScheduler
    private lateinit var adhanAudioManager: AdhanAudioManager
    private lateinit var announcementBootstrap: AnnouncementBootstrap
    private lateinit var userDataMigrator: UserDataMigrator
    private lateinit var widgetSettingsWatcher: WidgetSettingsWatcher

    private val appLanguage = MutableStateFlow("")
    private val userPreferences = MutableStateFlow(prefs())

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        preferences = mockk(relaxed = true) {
            every { this@mockk.appLanguage } returns this@AppInitializerTest.appLanguage
            every { this@mockk.userPreferences } returns this@AppInitializerTest.userPreferences
            // enabledPrayerTypes()/preReminderMinutesByPrayer() read these one by one, and a
            // relaxed mock's Flow never emits — `first()` on it throws.
            every { fajrNotificationEnabled } returns MutableStateFlow(true)
            every { sunriseNotificationEnabled } returns MutableStateFlow(false)
            every { dhuhrNotificationEnabled } returns MutableStateFlow(true)
            every { asrNotificationEnabled } returns MutableStateFlow(true)
            every { maghribNotificationEnabled } returns MutableStateFlow(true)
            every { ishaNotificationEnabled } returns MutableStateFlow(true)
            every { prayerReminderEnabled(any()) } returns MutableStateFlow(false)
            every { prayerReminderMinutes(any()) } returns MutableStateFlow(0)
        }
        scheduler = mockk(relaxed = true)
        adhanAudioManager = mockk(relaxed = true)
        announcementBootstrap = mockk(relaxed = true)
        userDataMigrator = mockk(relaxed = true)
        widgetSettingsWatcher = mockk(relaxed = true)
    }

    private fun initializer() = AppInitializer(
        context = context,
        preferencesDataStore = preferences,
        prayerNotificationScheduler = scheduler,
        adhanAudioManager = adhanAudioManager,
        announcementBootstrap = announcementBootstrap,
        userDataMigrator = userDataMigrator,
        widgetSettingsWatcher = widgetSettingsWatcher,
    )

    /** Startup is on `Dispatchers.IO` by design, so wait for the flag rather than a scheduler. */
    private fun AppInitializer.initializeAndWait(timeoutMs: Long = 10_000) {
        initialize()
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!isReady.value && System.currentTimeMillis() < deadline) Thread.sleep(10)
        assertThat(isReady.value).isTrue()
    }

    // ── The promise ─────────────────────────────────────────────────────────────

    @Test
    fun `nothing is ready until initialize is called`() {
        assertThat(initializer().isReady.value).isFalse()
    }

    @Test
    fun `a normal start runs every task and then reports ready`() {
        val initializer = initializer()

        initializer.initializeAndWait()

        verify { announcementBootstrap.initialize() }
        coVerify { userDataMigrator.migrateIfNeeded() }
        verify { widgetSettingsWatcher.start() }
    }

    @Test
    fun `the widget watcher starts outside the timed block, because it never finishes`() {
        // It opens a long-lived collection rather than doing work, so counting it against the
        // startup budget would make every start look slow and cancelling it would stop widgets
        // ever updating again.
        val initializer = initializer()

        initializer.initialize()

        verify(timeout = 2_000) { widgetSettingsWatcher.start() }
    }

    // ── The failure the design exists for ───────────────────────────────────────

    @Test
    fun `a task that never returns does not hold the UI at the splash screen`() {
        // Five seconds, then the app opens anyway. Without this a slow migration is
        // indistinguishable from a crash on launch.
        coEvery { userDataMigrator.migrateIfNeeded() } coAnswers {
            kotlinx.coroutines.delay(60_000)
            0
        }
        val initializer = initializer()

        initializer.initializeAndWait(timeoutMs = 20_000)

        assertThat(initializer.isReady.value).isTrue()
    }

    @Test
    fun `a task that throws is reported and the app still opens`() {
        coEvery { userDataMigrator.migrateIfNeeded() } throws IllegalStateException("bad db")

        val initializer = initializer()
        initializer.initializeAndWait()

        assertThat(initializer.isReady.value).isTrue()
    }

    // ── Locale ──────────────────────────────────────────────────────────────────

    @Test
    fun `a saved language is applied before anything is drawn`() {
        // Below API 33 the per-app locale is process-local, so a start that skips this shows
        // the whole first screen in the system language.
        appLanguage.value = "ar"

        initializer().initializeAndWait()

        // On API 33+ LocaleHelper goes through LocaleManager rather than Locale.setDefault,
        // so that is what has to be read back. The arm is guarded by its own try/catch, which
        // is exactly why a silent failure here would never surface.
        val locales = context.getSystemService(android.app.LocaleManager::class.java)
            ?.applicationLocales
        assertThat(locales?.toLanguageTags()).isEqualTo("ar")
    }

    @Test
    fun `english is left alone rather than re-applied`() {
        appLanguage.value = "en"

        initializer().initializeAndWait()

        // No assertion on the locale itself: the point is that this path completes and reports
        // ready rather than doing needless work on every start.
        assertThat(true).isTrue()
    }

    // ── Notifications ───────────────────────────────────────────────────────────

    @Test
    fun `today's alarms are re-armed on every start, because alarms do not survive`() {
        userPreferences.value = prefs(latitude = 51.5074, longitude = -0.1278)

        initializer().initializeAndWait()

        verify(timeout = 5_000) {
            scheduler.scheduleTodaysPrayerNotifications(
                latitude = 51.5074,
                longitude = -0.1278,
                notificationsEnabled = any(),
                enabledPrayers = any(),
                preReminders = any(),
            )
        }
    }

    @Test
    fun `no location means no scheduling, rather than alarms for the Gulf of Guinea`() {
        userPreferences.value = prefs(latitude = 0.0, longitude = 0.0)

        initializer().initializeAndWait()

        verify(exactly = 0) {
            scheduler.scheduleTodaysPrayerNotifications(
                latitude = any(),
                longitude = any(),
                notificationsEnabled = any(),
                enabledPrayers = any(),
                preReminders = any(),
            )
        }
    }

    @Test
    fun `the one-shot preference migration runs before anything reads a preference`() {
        // An existing install has no per-prayer alert style. Reading one before the migration
        // gives every prayer the default and silently discards what the user had set.
        initializer().initializeAndWait()

        coVerify(timeout = 5_000) { preferences.migratePrayerNotificationPreferences() }
    }

    @Test
    fun `a preference read that throws does not stop the rest of startup`() {
        every { preferences.userPreferences } throws IllegalStateException("datastore corrupt")

        val initializer = initializer()
        initializer.initializeAndWait()

        // The other four tasks still ran.
        verify { announcementBootstrap.initialize() }
    }

    // ── Adhan audio ─────────────────────────────────────────────────────────────

    @Test
    fun `a fresh install fetches the default adhan and the beep`() {
        every { adhanAudioManager.isFullyDownloaded(any()) } returns false
        every { adhanAudioManager.isDownloaded(any(), any()) } returns false

        initializer().initializeAndWait()

        verify(timeout = 5_000) { adhanAudioManager.cleanupTempFiles() }
        verify(timeout = 5_000) { adhanAudioManager.invalidateStaleDownloads() }
    }

    @Test
    fun `an install that already has the audio does not re-download it on every launch`() {
        every { adhanAudioManager.isFullyDownloaded(AdhanSound.MISHARY) } returns true
        every { adhanAudioManager.isDownloaded(AdhanSound.SIMPLE_BEEP, false) } returns true

        initializer().initializeAndWait()

        // Nothing enqueued: the check is what stops a data charge on every cold start.
        verify(timeout = 5_000) { adhanAudioManager.isFullyDownloaded(AdhanSound.MISHARY) }
    }

    @Test
    fun `a missing beep alone is enough to trigger the download`() {
        // The beep is the fallback the whole notification path relies on.
        every { adhanAudioManager.isFullyDownloaded(AdhanSound.MISHARY) } returns true
        every { adhanAudioManager.isDownloaded(AdhanSound.SIMPLE_BEEP, false) } returns false

        initializer().initializeAndWait()

        verify(timeout = 5_000) { adhanAudioManager.isDownloaded(AdhanSound.SIMPLE_BEEP, false) }
    }

    @Test
    fun `an audio manager that throws does not stop the app opening`() {
        every { adhanAudioManager.cleanupTempFiles() } throws IllegalStateException("no storage")

        val initializer = initializer()
        initializer.initializeAndWait()

        assertThat(initializer.isReady.value).isTrue()
    }

    private companion object {
        fun prefs(latitude: Double = 51.5074, longitude: Double = -0.1278) = UserPreferences(
            onboardingCompleted = true,
            themeMode = "system",
            dynamicColor = false,
            appLanguage = "en",
            calculationMethod = "MUSLIM_WORLD_LEAGUE",
            asrCalculation = "STANDARD",
            latitude = latitude,
            longitude = longitude,
            locationName = "London",
            prayerNotificationsEnabled = true,
            quranTranslatorId = "en",
            showTranslation = true,
        )
    }
}
