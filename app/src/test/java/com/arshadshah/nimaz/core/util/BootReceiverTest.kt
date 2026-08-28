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
import com.arshadshah.nimaz.domain.model.Khatam
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
import io.mockk.coEvery
import io.mockk.coVerify
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
 * Where every prayer notification is actually produced.
 *
 * `BootReceiver` decides whether a prayer makes a sound, plays the adhan, posts silently or is
 * suppressed entirely — and it does all of it from a `BroadcastReceiver` with no UI, no return
 * value and a `catch (e: Exception)` around each branch. Every failure here is silent by
 * construction: the user simply is not told it is time to pray.
 *
 * It sat at **0% covered** because `@AndroidEntryPoint` makes `onReceive` unreachable without a
 * Hilt application. [TestEntryPointApplication] is the smallest thing that satisfies that,
 * so these tests drive the real `onReceive` and read back what Android was asked to do.
 *
 * The receiver launches its work on `Dispatchers.IO`, so each assertion waits for the effect
 * rather than assuming it has already landed — see [awaitNotification] / [awaitNothing].
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestEntryPointApplication::class, sdk = [34])
class BootReceiverTest {

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager

    private lateinit var preferences: PreferencesDataStore
    private lateinit var prayerRepository: PrayerRepository
    private lateinit var khatamRepository: KhatamRepository
    private lateinit var adhanAudioManager: AdhanAudioManager
    private lateinit var rescheduler: PrayerRescheduler
    private lateinit var scheduler: PrayerNotificationScheduler

    // Every preference the receiver reads, as a real flow. A relaxed mock answers a `Flow`
    // property with a relaxed `Flow` whose `collect` returns without emitting, so `first()`
    // throws and the whole handler dies inside its own `catch` — green test, nothing exercised.
    private val prayerNotificationsEnabled = MutableStateFlow(true)
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
        WorshipReminderType.entries.forEach {
            worshipEnabled[it.key] = MutableStateFlow(true)
        }

        preferences = mockk(relaxed = true) {
            every { userPreferences } returns MutableStateFlow(userPrefs())
            every { adhanRespectDnd } returns this@BootReceiverTest.adhanRespectDnd
            every { notificationVibration } returns this@BootReceiverTest.notificationVibration
            every { notificationReminderMinutes } returns reminderMinutes
            every { adhanEnabled } returns this@BootReceiverTest.adhanEnabled
            every { selectedAdhanSound } returns this@BootReceiverTest.selectedAdhanSound
            every { prayerAlertStyle(any()) } answers {
                alertStyles.getValue(firstArg<String>().uppercase())
            }
            every { fajrNotificationEnabled } returns perPrayerEnabled.getValue("FAJR")
            every { sunriseNotificationEnabled } returns perPrayerEnabled.getValue("SUNRISE")
            every { dhuhrNotificationEnabled } returns perPrayerEnabled.getValue("DHUHR")
            every { asrNotificationEnabled } returns perPrayerEnabled.getValue("ASR")
            every { maghribNotificationEnabled } returns perPrayerEnabled.getValue("MAGHRIB")
            every { ishaNotificationEnabled } returns perPrayerEnabled.getValue("ISHA")
            every { fridayReminderEnabled } returns this@BootReceiverTest.fridayReminderEnabled
            every { khatamReminderEnabled } returns this@BootReceiverTest.khatamReminderEnabled
            every { appLanguage } returns this@BootReceiverTest.appLanguage
            every { worshipReminderEnabled(any()) } answers {
                worshipEnabled.getValue(firstArg())
            }
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
        rescheduler = mockk(relaxed = true)
        scheduler = mockk(relaxed = true)

        TestEntryPointApplication.Injector.reset()
        TestEntryPointApplication.Injector.bootReceiver = { receiver ->
            receiver.preferencesDataStore = preferences
            receiver.prayerNotificationScheduler = scheduler
            receiver.prayerRepository = prayerRepository
            receiver.prayerRescheduler = rescheduler
            receiver.khatamRepository = khatamRepository
            receiver.adhanAudioManager = adhanAudioManager
        }
    }

    private fun userPrefs(notificationsEnabled: Boolean = true) = UserPreferences(
        onboardingCompleted = true,
        themeMode = "system",
        dynamicColor = false,
        appLanguage = "en",
        calculationMethod = "MUSLIM_WORLD_LEAGUE",
        asrCalculation = "STANDARD",
        latitude = 51.5074,
        longitude = -0.1278,
        locationName = "London",
        prayerNotificationsEnabled = notificationsEnabled,
        quranTranslatorId = "en",
        showTranslation = true,
    )

    private fun receive(intent: Intent) = BootReceiver().onReceive(context, intent)

    private fun prayerIntent(
        type: String = "DHUHR",
        name: String = "Dhuhr",
        time: String = LocalDateTime.now().toString(),
        preReminder: Boolean = false,
        leadMinutes: Int? = null,
    ) = Intent(context, BootReceiver::class.java).apply {
        action = PrayerNotificationScheduler.ACTION_PRAYER_NOTIFICATION
        putExtra(PrayerNotificationScheduler.EXTRA_PRAYER_TYPE, type)
        putExtra(PrayerNotificationScheduler.EXTRA_PRAYER_NAME, name)
        putExtra(PrayerNotificationScheduler.EXTRA_PRAYER_TIME, time)
        if (preReminder) putExtra(PrayerNotificationScheduler.EXTRA_IS_PRE_REMINDER, true)
        leadMinutes?.let { putExtra(PrayerNotificationScheduler.EXTRA_REMINDER_MINUTES, it) }
    }

    /** Wait for the receiver's `Dispatchers.IO` coroutine to post [id], or fail saying it never did. */
    private fun awaitNotification(id: Int): Notification {
        repeat(200) {
            shadowOf(notificationManager).getNotification(id)?.let { return it }
            Thread.sleep(10)
        }
        throw AssertionError("no notification with id $id was posted within 2s")
    }

    /** Give the coroutine a fair chance to post, then assert it did not. */
    private fun awaitNothing() {
        Thread.sleep(300)
        assertThat(shadowOf(notificationManager).allNotifications).isEmpty()
    }

    private fun startedServices() =
        generateSequence { shadowOf(context as Application).nextStartedService }.toList()

    // ── Routing ─────────────────────────────────────────────────────────────────

    @Test
    fun `a reboot re-arms todays alarms, because nothing else does`() {
        // Alarms do not survive a reboot. If this stops calling the rescheduler, prayer
        // notifications simply never fire again and nothing anywhere reports it.
        receive(Intent(Intent.ACTION_BOOT_COMPLETED))

        coVerify(timeout = 2_000) { rescheduler.rescheduleToday() }
    }

    @Test
    fun `every boot spelling a manufacturer uses is handled, not just the standard one`() {
        // HTC and the "quickboot" OEMs broadcast their own actions instead of
        // ACTION_BOOT_COMPLETED; missing one means that vendor's users lose notifications.
        listOf(
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
        ).forEach { receive(Intent(it)) }

        coVerify(timeout = 2_000, exactly = 3) { rescheduler.rescheduleToday() }
    }

    @Test
    fun `the midnight chain reschedules and does nothing else`() {
        receive(Intent(PrayerNotificationScheduler.ACTION_MIDNIGHT_RESCHEDULE))

        coVerify(timeout = 2_000) { rescheduler.rescheduleToday() }
        awaitNothing()
    }

    @Test
    fun `an action the receiver does not own is ignored rather than crashing`() {
        receive(Intent("com.example.SOMETHING_ELSE"))

        awaitNothing()
    }

    // ── A prayer notification ───────────────────────────────────────────────────

    @Test
    fun `a prayer whose own switch is off posts nothing`() {
        // Per-prayer switches are re-read at fire time on purpose: an alarm armed yesterday
        // must not fire for a prayer the user turned off this morning.
        perPrayerEnabled.getValue("DHUHR").value = false

        receive(prayerIntent(type = "DHUHR"))

        awaitNothing()
    }

    @Test
    fun `a plain prayer notification carries the prayer name and its own colour`() {
        alertStyles.getValue("DHUHR").value = PrayerAlertStyle.NOTIFICATION

        receive(prayerIntent(type = "DHUHR", name = "Dhuhr"))

        val notification = awaitNotification("Dhuhr".hashCode())
        assertThat(notification.extras.getString(Notification.EXTRA_TITLE)).contains("Dhuhr")
        // Each prayer has its own accent; a shared default would make the shade unreadable.
        assertThat(notification.color).isEqualTo(0xFF2196F3.toInt())
    }

    @Test
    fun `a silenced prayer posts at low priority on the muted channel`() {
        // The whole point of SILENT: the notification still appears in the shade, but nothing
        // about it interrupts. Priority and channel have to agree — old launchers read the
        // former, Android 8+ reads the latter.
        alertStyles.getValue("ASR").value = PrayerAlertStyle.SILENT

        receive(prayerIntent(type = "ASR", name = "Asr"))

        val notification = awaitNotification("Asr".hashCode())
        assertThat(notification.priority).isEqualTo(NotificationCompat.PRIORITY_LOW)
        assertThat(notification.channelId)
            .isEqualTo(NimazChannels.PRAYER_MUTED)
    }

    @Test
    fun `an audible prayer is a full-priority alarm`() {
        alertStyles.getValue("MAGHRIB").value = PrayerAlertStyle.NOTIFICATION

        receive(prayerIntent(type = "MAGHRIB", name = "Maghrib"))

        val notification = awaitNotification("Maghrib".hashCode())
        assertThat(notification.priority).isEqualTo(NotificationCompat.PRIORITY_HIGH)
        assertThat(notification.category).isEqualTo(NotificationCompat.CATEGORY_ALARM)
    }

    @Test
    fun `sunrise cannot be silenced, because it is not a prayer with settings`() {
        // SILENT on sunrise must not mute it — it is the end of Fajr's window, and the app
        // deliberately does not let it take a style of its own.
        alertStyles.getValue("SUNRISE").value = PrayerAlertStyle.SILENT
        adhanEnabled.value = false

        receive(prayerIntent(type = "SUNRISE", name = "Sunrise"))

        val notification = awaitNotification("Sunrise".hashCode())
        assertThat(notification.channelId)
            .isNotEqualTo(NimazChannels.PRAYER_MUTED)
    }

    @Test
    fun `turning vibration off posts on the no-vibration sibling channel`() {
        // Android ignores enableVibration() on a channel that already exists, so the
        // preference is honoured by posting somewhere else entirely.
        notificationVibration.value = false
        alertStyles.getValue("ISHA").value = PrayerAlertStyle.NOTIFICATION

        receive(prayerIntent(type = "ISHA", name = "Isha"))

        val notification = awaitNotification("Isha".hashCode())
        assertThat(notification.channelId)
            .isEqualTo(NimazChannels.PRAYER_SILENT)
    }

    // ── The adhan ───────────────────────────────────────────────────────────────

    @Test
    fun `the adhan style hands playback to the service rather than posting a notification`() {
        alertStyles.getValue("FAJR").value = PrayerAlertStyle.ADHAN
        adhanEnabled.value = true
        every { adhanAudioManager.isDownloaded(any(), any()) } returns true

        receive(prayerIntent(type = "FAJR", name = "Fajr"))

        val services = awaitStartedService()
        assertThat(services.action).isEqualTo(AdhanPlaybackService.ACTION_PLAY)
        // The service notification *is* the prayer notification — posting both would leave
        // two entries in the shade for one prayer.
        assertThat(shadowOf(notificationManager).allNotifications).isEmpty()
    }

    @Test
    fun `a missing adhan file falls back to a notification and asks for a re-download`() {
        // Silence is the worst outcome: the user set the adhan and hears nothing at Fajr with
        // no clue why. Falling back keeps the prayer visible and repairs the file for next time.
        alertStyles.getValue("FAJR").value = PrayerAlertStyle.ADHAN
        selectedAdhanSound.value = AdhanSound.entries.first { it != AdhanSound.SIMPLE_BEEP }.name
        every { adhanAudioManager.isDownloaded(any(), any()) } returns false

        receive(prayerIntent(type = "FAJR", name = "Fajr"))

        awaitNotification("Fajr".hashCode())
        val download = startedServices().map { it.component?.className }
        assertThat(download).contains(
            com.arshadshah.nimaz.data.audio.AdhanDownloadService::class.java.name
        )
    }

    @Test
    fun `the beep fallback still plays when only the beep is on disk`() {
        alertStyles.getValue("FAJR").value = PrayerAlertStyle.ADHAN
        selectedAdhanSound.value = AdhanSound.entries.first { it != AdhanSound.SIMPLE_BEEP }.name
        every { adhanAudioManager.isDownloaded(any(), any()) } answers {
            firstArg<AdhanSound>() == AdhanSound.SIMPLE_BEEP
        }

        receive(prayerIntent(type = "FAJR", name = "Fajr"))

        // Two services start, in this order: the re-download for the missing variant, then the
        // beep. Asserting on the *first* one started would pin the repair and miss the point.
        awaitStartedService()
        val started = listOf(awaitStartedService()) + startedServices()
        assertThat(started.map { it.action }).contains(AdhanPlaybackService.ACTION_PLAY)
        assertThat(shadowOf(notificationManager).allNotifications).isEmpty()
    }

    @Test
    fun `do not disturb silences the audio but still shows the prayer`() {
        // DND gates the adhan only. Suppressing the notification too would mean a user in a
        // meeting is never told a prayer came in at all.
        alertStyles.getValue("DHUHR").value = PrayerAlertStyle.ADHAN
        adhanRespectDnd.value = true
        notificationManager.setInterruptionFilter(
            NotificationManager.INTERRUPTION_FILTER_PRIORITY
        )

        receive(prayerIntent(type = "DHUHR", name = "Dhuhr"))

        awaitNotification("Dhuhr".hashCode())
        assertThat(startedServices()).isEmpty()
    }

    @Test
    fun `the global adhan switch overrides a per-prayer adhan style`() {
        alertStyles.getValue("DHUHR").value = PrayerAlertStyle.ADHAN
        adhanEnabled.value = false

        receive(prayerIntent(type = "DHUHR", name = "Dhuhr"))

        awaitNotification("Dhuhr".hashCode())
        assertThat(startedServices()).isEmpty()
    }

    // ── Pre-reminders ───────────────────────────────────────────────────────────

    @Test
    fun `a pre-reminder uses the lead time carried on the alarm, not the current preference`() {
        // The lead time is per prayer now. Reading the global back at fire time is how a
        // notification armed 45 minutes out ended up saying "in 15 minutes".
        reminderMinutes.value = 15

        receive(prayerIntent(type = "ASR", name = "Asr", preReminder = true, leadMinutes = 45))

        val notification = awaitNotification("Asr_reminder".hashCode())
        val title = notification.extras.getString(Notification.EXTRA_TITLE).orEmpty()
        val text = notification.extras.getString(Notification.EXTRA_TEXT).orEmpty()
        assertThat("$title $text").contains("45")
    }

    @Test
    fun `a pre-reminder with no lead on the alarm falls back to the stored preference`() {
        // Alarms armed by an older build carry no EXTRA_REMINDER_MINUTES.
        reminderMinutes.value = 20

        receive(prayerIntent(type = "ASR", name = "Asr", preReminder = true))

        val notification = awaitNotification("Asr_reminder".hashCode())
        val title = notification.extras.getString(Notification.EXTRA_TITLE).orEmpty()
        val text = notification.extras.getString(Notification.EXTRA_TEXT).orEmpty()
        assertThat("$title $text").contains("20")
    }

    @Test
    fun `a pre-reminder never plays the adhan, whatever the style says`() {
        alertStyles.getValue("ASR").value = PrayerAlertStyle.ADHAN

        receive(prayerIntent(type = "ASR", name = "Asr", preReminder = true, leadMinutes = 10))

        awaitNotification("Asr_reminder".hashCode())
        assertThat(startedServices()).isEmpty()
    }

    // ── Daily summary ───────────────────────────────────────────────────────────

    @Test
    fun `the daily summary counts prayed and missed, ignoring sunrise`() {
        // Sunrise is in the records but is not a prayer; counting it would tell everyone they
        // missed one every single day.
        every { prayerRepository.getPrayerRecordsForDate(any()) } returns flowOf(
            listOf(
                record(PrayerName.FAJR, PrayerStatus.PRAYED),
                record(PrayerName.SUNRISE, PrayerStatus.NOT_PRAYED),
                record(PrayerName.DHUHR, PrayerStatus.LATE),
                record(PrayerName.ASR, PrayerStatus.MISSED),
                record(PrayerName.MAGHRIB, PrayerStatus.NOT_PRAYED),
                record(PrayerName.ISHA, PrayerStatus.PRAYED),
            )
        )

        receive(Intent(PrayerNotificationScheduler.ACTION_DAILY_SUMMARY))

        val notification = awaitNotification("daily_summary".hashCode())
        val body = notification.extras.getString(Notification.EXTRA_TEXT).orEmpty() + " " +
            notification.extras.getString(Notification.EXTRA_BIG_TEXT).orEmpty()
        // Three counted as prayed (PRAYED, LATE, PRAYED), two missed — sunrise in neither.
        assertThat(body).contains("3")
        assertThat(body).doesNotContain("Sunrise")
    }

    @Test
    fun `the daily summary respects the master notification switch`() {
        every { preferences.userPreferences } returns
            MutableStateFlow(userPrefs(notificationsEnabled = false))

        receive(Intent(PrayerNotificationScheduler.ACTION_DAILY_SUMMARY))

        awaitNothing()
    }

    @Test
    fun `a perfect day and a missed day are coloured differently`() {
        every { prayerRepository.getPrayerRecordsForDate(any()) } returns flowOf(
            PrayerName.entries.filter { it != PrayerName.SUNRISE }
                .map { record(it, PrayerStatus.PRAYED) }
        )

        receive(Intent(PrayerNotificationScheduler.ACTION_DAILY_SUMMARY))

        // Green for a complete day; the orange arm is what a user sees far more often, so the
        // two must not collapse to one colour.
        assertThat(awaitNotification("daily_summary".hashCode()).color)
            .isEqualTo(0xFF4CAF50.toInt())
    }

    // ── Friday ──────────────────────────────────────────────────────────────────

    @Test
    fun `the friday reminder posts when both switches are still on at fire time`() {
        receive(Intent(PrayerNotificationScheduler.ACTION_FRIDAY_REMINDER))

        awaitNotification("friday_reminder".hashCode())
    }

    @Test
    fun `the friday reminder is dropped if it was turned off after the alarm was armed`() {
        fridayReminderEnabled.value = false

        receive(Intent(PrayerNotificationScheduler.ACTION_FRIDAY_REMINDER))

        awaitNothing()
    }

    // ── Khatam ──────────────────────────────────────────────────────────────────

    @Test
    fun `the khatam reminder says how much to read to get back on pace`() {
        // The number is the target plus the accumulated shortfall — a flat "10 ayahs" when
        // someone is 200 behind is a reminder that never closes the gap.
        every { khatamRepository.observeActiveKhatam() } returns flowOf(
            khatam(dailyTarget = 20, totalAyahsRead = 20, startedAt = daysAgo(5))
        )

        receive(Intent(PrayerNotificationScheduler.ACTION_KHATAM_REMINDER))

        val notification = awaitNotification("khatam_reminder".hashCode())
        val body = notification.extras.getString(Notification.EXTRA_TEXT).orEmpty()
        assertThat(body).isNotEmpty()
        assertThat(notification.channelId)
            .isEqualTo(NimazChannels.KHATAM)
    }

    @Test
    fun `no active khatam means no reminder, rather than a reminder about nothing`() {
        every { khatamRepository.observeActiveKhatam() } returns flowOf(null)

        receive(Intent(PrayerNotificationScheduler.ACTION_KHATAM_REMINDER))

        awaitNothing()
    }

    @Test
    fun `the khatam reminder is dropped when the preference was turned off`() {
        khatamReminderEnabled.value = false
        every { khatamRepository.observeActiveKhatam() } returns flowOf(
            khatam(dailyTarget = 20, totalAyahsRead = 20, startedAt = daysAgo(5))
        )

        receive(Intent(PrayerNotificationScheduler.ACTION_KHATAM_REMINDER))

        awaitNothing()
    }

    // ── Worship reminders ───────────────────────────────────────────────────────

    @Test
    fun `a worship reminder posts on the gentle worship channel`() {
        receive(worshipIntent(WorshipReminderType.TAHAJJUD))

        val notification = awaitNotification(("worship_tahajjud").hashCode())
        assertThat(notification.channelId)
            .isEqualTo(NimazChannels.WORSHIP)
        assertThat(notification.priority).isEqualTo(NotificationCompat.PRIORITY_DEFAULT)
    }

    @Test
    fun `a worship reminder turned off after arming is dropped at fire time`() {
        worshipEnabled.getValue(WorshipReminderType.IFTAR.key).value = false

        receive(worshipIntent(WorshipReminderType.IFTAR))

        awaitNothing()
    }

    @Test
    fun `an unknown worship type on the intent is ignored rather than crashing`() {
        // An alarm armed by a build that knew a type this one does not is a real upgrade path.
        val intent = Intent(context, BootReceiver::class.java).apply {
            action = PrayerNotificationScheduler.ACTION_WORSHIP_REMINDER
            putExtra(PrayerNotificationScheduler.EXTRA_WORSHIP_TYPE, "NOT_A_REAL_TYPE")
        }

        receive(intent)

        awaitNothing()
    }

    @Test
    fun `a worship reminder with no type at all is ignored`() {
        val intent = Intent(context, BootReceiver::class.java).apply {
            action = PrayerNotificationScheduler.ACTION_WORSHIP_REMINDER
        }

        receive(intent)

        awaitNothing()
    }

    @Test
    fun `worship reminders respect the master notification switch`() {
        every { preferences.userPreferences } returns
            MutableStateFlow(userPrefs(notificationsEnabled = false))

        receive(worshipIntent(WorshipReminderType.WITR))

        awaitNothing()
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private fun worshipIntent(type: WorshipReminderType) =
        Intent(context, BootReceiver::class.java).apply {
            action = PrayerNotificationScheduler.ACTION_WORSHIP_REMINDER
            putExtra(PrayerNotificationScheduler.EXTRA_WORSHIP_TYPE, type.name)
            putExtra(
                PrayerNotificationScheduler.EXTRA_WORSHIP_EVENT_TIME,
                LocalDateTime.now().toString()
            )
        }

    private fun awaitStartedService(): Intent {
        repeat(200) {
            shadowOf(context as Application).nextStartedService?.let { return it }
            Thread.sleep(10)
        }
        throw AssertionError("no service was started within 2s")
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

    private fun daysAgo(days: Long) =
        java.time.LocalDate.now().minusDays(days).atStartOfDay()
            .toInstant(java.time.ZoneOffset.UTC).toEpochMilli()

    private fun khatam(dailyTarget: Int, totalAyahsRead: Int, startedAt: Long) = Khatam(
        id = 1,
        name = "Ramadan khatam",
        isActive = true,
        dailyTarget = dailyTarget,
        totalAyahsRead = totalAyahsRead,
        startedAt = startedAt,
    )

    private companion object {
        val PRAYERS = listOf("FAJR", "SUNRISE", "DHUHR", "ASR", "MAGHRIB", "ISHA")
    }
}
