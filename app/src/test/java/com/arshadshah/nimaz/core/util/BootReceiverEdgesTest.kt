package com.arshadshah.nimaz.core.util

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.common.NimazChannels
import com.arshadshah.nimaz.core.datastore.PreferencesDataStore
import com.arshadshah.nimaz.data.audio.AdhanAudioManager
import com.arshadshah.nimaz.data.audio.AdhanPlaybackService
import com.arshadshah.nimaz.data.audio.AdhanSound
import com.arshadshah.nimaz.domain.model.PrayerAlertStyle
import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerRecord
import com.arshadshah.nimaz.domain.model.PrayerStatus
import com.arshadshah.nimaz.domain.model.UserPreferences
import com.arshadshah.nimaz.domain.model.WorshipReminderType
import com.arshadshah.nimaz.domain.repository.KhatamRepository
import com.arshadshah.nimaz.domain.repository.PrayerRepository
import com.arshadshah.nimaz.testing.TestEntryPointApplication
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * `BootReceiver`'s recovery arms and its two remaining audio paths.
 *
 * Every handler in the receiver is wrapped in `catch (e: Exception)`, which is right — a
 * receiver has nowhere to propagate to and crashing on boot is worse than one missed
 * notification — and it also means every one of those arms is invisible unless a test forces it.
 * A handler that throws on its first line looks exactly like a quiet day.
 *
 * Sunrise's beep is the other gap: it is the one prayer that never takes the adhan and never
 * takes a style, so its audio path is reached from nowhere else.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestEntryPointApplication::class, sdk = [34])
class BootReceiverEdgesTest {

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager
    private lateinit var preferences: PreferencesDataStore
    private lateinit var prayerRepository: PrayerRepository
    private lateinit var khatamRepository: KhatamRepository
    private lateinit var adhanAudioManager: AdhanAudioManager

    private val prefsFlow = MutableStateFlow(userPrefs())
    private val adhanRespectDnd = MutableStateFlow(false)
    private val notificationVibration = MutableStateFlow(true)
    private val reminderMinutes = MutableStateFlow(15)
    private val adhanEnabled = MutableStateFlow(true)
    private val selectedAdhanSound = MutableStateFlow(AdhanSound.SIMPLE_BEEP.name)
    private val alertStyles = mutableMapOf<String, MutableStateFlow<PrayerAlertStyle>>()
    private val perPrayerEnabled = mutableMapOf<String, MutableStateFlow<Boolean>>()
    private val fridayReminderEnabled = MutableStateFlow(true)
    private val khatamReminderEnabled = MutableStateFlow(true)
    private val appLanguage = MutableStateFlow("")
    private val worshipEnabled = mutableMapOf<String, MutableStateFlow<Boolean>>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()

        PRAYERS.forEach {
            alertStyles[it] = MutableStateFlow(PrayerAlertStyle.NOTIFICATION)
            perPrayerEnabled[it] = MutableStateFlow(true)
        }
        WorshipReminderType.entries.forEach { worshipEnabled[it.key] = MutableStateFlow(true) }

        preferences = mockk(relaxed = true) {
            every { userPreferences } returns prefsFlow
            every { adhanRespectDnd } returns this@BootReceiverEdgesTest.adhanRespectDnd
            every { notificationVibration } returns this@BootReceiverEdgesTest.notificationVibration
            every { notificationReminderMinutes } returns reminderMinutes
            every { adhanEnabled } returns this@BootReceiverEdgesTest.adhanEnabled
            every { selectedAdhanSound } returns this@BootReceiverEdgesTest.selectedAdhanSound
            every { prayerAlertStyle(any()) } answers {
                alertStyles.getValue(firstArg<String>().uppercase())
            }
            every { fajrNotificationEnabled } returns perPrayerEnabled.getValue("FAJR")
            every { sunriseNotificationEnabled } returns perPrayerEnabled.getValue("SUNRISE")
            every { dhuhrNotificationEnabled } returns perPrayerEnabled.getValue("DHUHR")
            every { asrNotificationEnabled } returns perPrayerEnabled.getValue("ASR")
            every { maghribNotificationEnabled } returns perPrayerEnabled.getValue("MAGHRIB")
            every { ishaNotificationEnabled } returns perPrayerEnabled.getValue("ISHA")
            every { fridayReminderEnabled } returns this@BootReceiverEdgesTest.fridayReminderEnabled
            every { khatamReminderEnabled } returns this@BootReceiverEdgesTest.khatamReminderEnabled
            every { appLanguage } returns this@BootReceiverEdgesTest.appLanguage
            every { worshipReminderEnabled(any()) } answers { worshipEnabled.getValue(firstArg()) }
        }
        prayerRepository = mockk(relaxed = true) {
            every { getPrayerRecordsForDate(any()) } returns flowOf(emptyList())
        }
        khatamRepository = mockk(relaxed = true) {
            every { observeActiveKhatam() } returns flowOf(null)
        }
        adhanAudioManager = mockk(relaxed = true) {
            every { isDownloaded(any(), any()) } returns true
        }

        TestEntryPointApplication.Injector.reset()
        TestEntryPointApplication.Injector.bootReceiver = { receiver ->
            receiver.preferencesDataStore = preferences
            receiver.prayerNotificationScheduler = mockk(relaxed = true)
            receiver.prayerRepository = prayerRepository
            receiver.prayerRescheduler = mockk(relaxed = true)
            receiver.khatamRepository = khatamRepository
            receiver.adhanAudioManager = adhanAudioManager
        }
    }

    private fun receive(intent: Intent) = BootReceiver().onReceive(context, intent)

    private fun prayerIntent(type: String, name: String, time: String = LocalDateTime.now().toString()) =
        Intent(context, BootReceiver::class.java).apply {
            action = PrayerNotificationScheduler.ACTION_PRAYER_NOTIFICATION
            putExtra(PrayerNotificationScheduler.EXTRA_PRAYER_TYPE, type)
            putExtra(PrayerNotificationScheduler.EXTRA_PRAYER_NAME, name)
            putExtra(PrayerNotificationScheduler.EXTRA_PRAYER_TIME, time)
        }

    private fun awaitNotification(id: Int): Notification {
        repeat(200) {
            shadowOf(notificationManager).getNotification(id)?.let { return it }
            Thread.sleep(10)
        }
        throw AssertionError("no notification with id $id was posted within 2s")
    }

    private fun awaitNothing() {
        Thread.sleep(300)
        assertThat(shadowOf(notificationManager).allNotifications).isEmpty()
    }

    private fun startedServices() =
        generateSequence { shadowOf(context as Application).nextStartedService }.toList()

    // ── Sunrise's beep ──────────────────────────────────────────────────────────

    @Test
    fun `sunrise gets the beep when the global adhan switch is on`() {
        // It is the end of Fajr's window rather than a prayer, so it never takes the adhan —
        // but it is not silent either, and this is the only path that plays it.
        adhanEnabled.value = true
        every { adhanAudioManager.isDownloaded(AdhanSound.SIMPLE_BEEP, false) } returns true

        receive(prayerIntent("SUNRISE", "Sunrise"))

        repeat(200) {
            if (startedServicesContainPlay()) return
            Thread.sleep(10)
        }
        throw AssertionError("sunrise never asked for the beep")
    }

    @Test
    fun `sunrise falls back to a plain notification when the beep is not on disk`() {
        adhanEnabled.value = true
        every { adhanAudioManager.isDownloaded(any(), any()) } returns false

        receive(prayerIntent("SUNRISE", "Sunrise"))

        awaitNotification("Sunrise".hashCode())
    }

    @Test
    fun `do not disturb silences sunrise's beep too`() {
        adhanEnabled.value = true
        adhanRespectDnd.value = true
        notificationManager.setInterruptionFilter(
            NotificationManager.INTERRUPTION_FILTER_PRIORITY
        )

        receive(prayerIntent("SUNRISE", "Sunrise"))

        awaitNotification("Sunrise".hashCode())
        assertThat(startedServices()).isEmpty()
    }

    private fun startedServicesContainPlay(): Boolean =
        startedServices().any { it.action == AdhanPlaybackService.ACTION_PLAY }

    // ── The channel a played adhan posts on ─────────────────────────────────────

    @Test
    fun `an adhan notification with vibration off posts on the silent adhan channel`() {
        // Two independent preferences meet here: the alert style says adhan, the vibration
        // preference decides which of the two adhan channels carries it.
        notificationVibration.value = false
        alertStyles.getValue("DHUHR").value = PrayerAlertStyle.ADHAN
        every { adhanAudioManager.isDownloaded(any(), any()) } returns false
        adhanEnabled.value = true

        receive(prayerIntent("DHUHR", "Dhuhr"))

        val notification = awaitNotification("Dhuhr".hashCode())
        assertThat(notification.channelId)
            .isEqualTo(NimazChannels.PRAYER_SILENT)
    }

    // ── The failure arms ────────────────────────────────────────────────────────

    @Test
    fun `a preference read that throws still posts the prayer notification`() {
        // The catch is what turns "one preference could not be read" into "the reader is still
        // told it is time to pray". Without it the alarm fires and nothing appears.
        every { preferences.adhanRespectDnd } throws IllegalStateException("datastore corrupt")

        receive(prayerIntent("ASR", "Asr"))

        val notification = awaitNotification("Asr".hashCode())
        assertThat(notification.extras.getString(Notification.EXTRA_TITLE)).contains("Asr")
    }

    @Test
    fun `a daily summary whose records cannot be read posts nothing rather than crashing`() {
        every { prayerRepository.getPrayerRecordsForDate(any()) } throws
            IllegalStateException("db closed")

        receive(Intent(PrayerNotificationScheduler.ACTION_DAILY_SUMMARY))

        awaitNothing()
    }

    @Test
    fun `a friday reminder whose preferences throw posts nothing rather than crashing`() {
        every { preferences.fridayReminderEnabled } throws IllegalStateException("datastore gone")

        receive(Intent(PrayerNotificationScheduler.ACTION_FRIDAY_REMINDER))

        awaitNothing()
    }

    @Test
    fun `a khatam reminder whose repository throws posts nothing rather than crashing`() {
        every { khatamRepository.observeActiveKhatam() } throws IllegalStateException("db closed")

        receive(Intent(PrayerNotificationScheduler.ACTION_KHATAM_REMINDER))

        awaitNothing()
    }

    @Test
    fun `a worship reminder whose preferences throw posts nothing rather than crashing`() {
        every { preferences.worshipReminderEnabled(any()) } throws
            IllegalStateException("datastore gone")

        receive(
            Intent(context, BootReceiver::class.java).apply {
                action = PrayerNotificationScheduler.ACTION_WORSHIP_REMINDER
                putExtra(
                    PrayerNotificationScheduler.EXTRA_WORSHIP_TYPE,
                    WorshipReminderType.TAHAJJUD.name,
                )
            }
        )

        awaitNothing()
    }

    // ── The saved locale on a cold-start alarm ──────────────────────────────────

    @Test
    fun `a khatam alarm that cold-starts the process re-applies the saved language`() {
        // Below API 33 the per-app locale is process-local and set asynchronously at startup,
        // so an alarm that starts the process would otherwise format this in the system
        // language rather than the reader's.
        appLanguage.value = "ar"
        every { khatamRepository.observeActiveKhatam() } returns flowOf(null)

        receive(Intent(PrayerNotificationScheduler.ACTION_KHATAM_REMINDER))
        Thread.sleep(300)

        val locales = context.getSystemService(android.app.LocaleManager::class.java)
            ?.applicationLocales
        assertThat(locales?.toLanguageTags()).isEqualTo("ar")
    }

    // ── The daily summary's other colour ────────────────────────────────────────

    @Test
    fun `a day with missed prayers is coloured differently from a complete one`() {
        every { prayerRepository.getPrayerRecordsForDate(any()) } returns flowOf(
            PrayerName.entries.filter { it != PrayerName.SUNRISE }
                .map { record(it, PrayerStatus.NOT_PRAYED) }
        )

        receive(Intent(PrayerNotificationScheduler.ACTION_DAILY_SUMMARY))

        assertThat(awaitNotification("daily_summary".hashCode()).color)
            .isEqualTo(0xFFFF9800.toInt())
    }

    // ── The colour table ────────────────────────────────────────────────────────

    @Test
    fun `an unrecognised prayer type still gets a colour rather than a blank one`() {
        // Alarms are persisted across upgrades, so a type this build does not know is a real
        // input — and a notification with colorized set and no colour renders badly.
        receive(prayerIntent("SOMETHING_NEW", "Something"))

        val notification = awaitNotification("Something".hashCode())
        assertThat(notification.color).isEqualTo(0xFF4CAF50.toInt())
    }

    @Test
    fun `an unrecognised prayer type is not silently disabled either`() {
        // `isPrayerNotificationEnabled` falls through to true, which is the safe default: a
        // notification the reader did not ask to turn off must still arrive.
        receive(prayerIntent("SOMETHING_NEW", "Something"))

        awaitNotification("Something".hashCode())
    }

    private fun record(name: PrayerName, status: PrayerStatus) = PrayerRecord(
        id = name.ordinal.toLong(),
        date = todayEpochMillis(),
        prayerName = name,
        status = status,
        prayedAt = null,
        scheduledTime = todayEpochMillis(),
        isJamaah = false,
        isQadaFor = null,
        note = null,
        createdAt = todayEpochMillis(),
        updatedAt = todayEpochMillis(),
    )

    private fun todayEpochMillis() =
        java.time.LocalDate.now().atStartOfDay().toEpochSecond(java.time.ZoneOffset.UTC) * 1000

    private companion object {
        val PRAYERS = listOf("FAJR", "SUNRISE", "DHUHR", "ASR", "MAGHRIB", "ISHA")

        fun userPrefs() = UserPreferences(
            onboardingCompleted = true,
            themeMode = "system",
            dynamicColor = false,
            appLanguage = "en",
            calculationMethod = "MUSLIM_WORLD_LEAGUE",
            asrCalculation = "STANDARD",
            latitude = 51.5074,
            longitude = -0.1278,
            locationName = "London",
            prayerNotificationsEnabled = true,
            quranTranslatorId = "en",
            showTranslation = true,
        )
    }
}
