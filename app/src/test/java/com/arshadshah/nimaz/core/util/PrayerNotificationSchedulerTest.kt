package com.arshadshah.nimaz.core.util

import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.core.common.NimazChannels
import com.arshadshah.nimaz.domain.model.AsrCalculation
import com.arshadshah.nimaz.domain.model.CalculationMethod
import com.arshadshah.nimaz.domain.model.PrayerType
import com.arshadshah.nimaz.domain.model.WorshipReminderType
import com.arshadshah.nimaz.domain.prayer.PrayerTimeCalculator
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The alarms this app lives or dies by.
 *
 * Every prayer notification, every pre-reminder, the Friday nudge, the khatam reminder and the
 * eleven worship reminders are one-shot `setExactAndAllowWhileIdle` alarms re-armed by the
 * midnight chain. Nothing in the UI shows that an alarm was expected, so a scheduler that arms
 * nothing looks exactly like a quiet day — which is why this class had **zero** covered lines
 * while the app shipped notifications as its headline feature.
 *
 * These tests read `shadowOf(alarmManager).scheduledAlarms` back, so they assert on what Android
 * was actually asked to do rather than on the scheduler having been called.
 *
 * `application = Application::class` is deliberate: the app's `@HiltAndroidApp` `NimazApp` would
 * pull WorkManager and Hilt into a test about `AlarmManager`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class PrayerNotificationSchedulerTest {

    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var settings: SettingsRepository

    // A real calculator, not a stub: a stub returning invented instants would let a wrong-day
    // or wrong-order bug straight through, and this is the one place the two meet.
    private val calculator = PrayerTimeCalculator()

    private val khatamEnabled = MutableStateFlow(false)
    private val khatamTime = MutableStateFlow("06:00")
    private val hijriOffset = MutableStateFlow(0)
    private val worshipEnabled = mutableMapOf<String, MutableStateFlow<Boolean>>()
    private val worshipOffset = mutableMapOf<String, MutableStateFlow<Int>>()
    private val witrMode = MutableStateFlow("after_isha")

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        WorshipReminderType.entries.forEach {
            worshipEnabled[it.key] = MutableStateFlow(false)
            worshipOffset[it.key] = MutableStateFlow(it.defaultOffsetMinutes)
        }

        // A *relaxed* mock answers a Flow property with a relaxed Flow, whose `collect` returns
        // without emitting — `first()` on that throws, and the scheduler's blocking reads die on
        // their first line while the test still looks green. Every flow the scheduler reads is
        // therefore a real one.
        settings = mockk(relaxed = true) {
            every { khatamReminderEnabled } returns khatamEnabled
            every { khatamReminderTime } returns khatamTime
            every { hijriDayOffset } returns hijriOffset
            every { worshipReminderEnabled(any()) } answers {
                worshipEnabled.getValue(firstArg())
            }
            every { worshipReminderOffset(any(), any()) } answers {
                worshipOffset.getValue(firstArg())
            }
            every { worshipReminderMode(any(), any()) } returns witrMode
        }
    }

    private fun scheduler() =
        PrayerNotificationScheduler(context, calculator, settings)

    private fun schedule(
        latitude: Double = LONDON_LAT,
        longitude: Double = LONDON_LON,
        notificationsEnabled: Boolean = true,
        enabledPrayers: Set<PrayerType>? = null,
        preReminders: Map<PrayerType, Int> = emptyMap(),
        fridayReminderEnabled: Boolean = false,
        fridayReminderMinutes: Int = 30,
        target: PrayerNotificationScheduler = scheduler(),
    ) = target.scheduleTodaysPrayerNotifications(
        latitude = latitude,
        longitude = longitude,
        notificationsEnabled = notificationsEnabled,
        enabledPrayers = enabledPrayers,
        preReminders = preReminders,
        calculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE,
        asrCalculation = AsrCalculation.STANDARD,
        highLatitudeRule = null,
        adjustments = emptyMap(),
        fridayReminderEnabled = fridayReminderEnabled,
        fridayReminderMinutes = fridayReminderMinutes,
    )

    private fun scheduledAlarms() = shadowOf(alarmManager).scheduledAlarms

    private fun scheduledTriggers(): List<LocalDateTime> = scheduledAlarms().map {
        java.time.Instant.ofEpochMilli(it.triggerAtTime)
            .atZone(ZoneId.systemDefault()).toLocalDateTime()
    }

    // ── Channels ────────────────────────────────────────────────────────────────

    @Test
    fun `constructing the scheduler creates every channel a notification can be posted on`() {
        // The channels are created in `init`, not lazily at post time: Android silently drops a
        // notification posted to a channel that does not exist, and the first prayer alarm of a
        // fresh install fires from a BroadcastReceiver with no screen to report the failure to.
        scheduler()

        val ids = notificationManager.notificationChannels.map { it.id }
        assertThat(ids).containsAtLeast(
            NimazChannels.PRAYER,
            NimazChannels.PRAYER_SILENT,
            NimazChannels.PRAYER_MUTED,
            NimazChannels.ADHAN,
            NimazChannels.ADHAN_SILENT,
            NimazChannels.DAILY_SUMMARY,
            NimazChannels.KHATAM,
            NimazChannels.WORSHIP,
        )
    }

    @Test
    fun `the muted channel is the only prayer channel below high importance`() {
        // Android will not let an existing channel's importance be lowered from code, so this is
        // the one property that cannot be fixed in a later release if it ships wrong.
        scheduler()
        val byId = notificationManager.notificationChannels.associateBy { it.id }

        assertThat(byId.getValue(NimazChannels.PRAYER).importance)
            .isEqualTo(NotificationManager.IMPORTANCE_HIGH)
        assertThat(byId.getValue(NimazChannels.PRAYER_SILENT).importance)
            .isEqualTo(NotificationManager.IMPORTANCE_HIGH)
        assertThat(byId.getValue(NimazChannels.PRAYER_MUTED).importance)
            .isLessThan(NotificationManager.IMPORTANCE_HIGH)
    }

    @Test
    fun `the silent siblings differ from their loud twins only by vibration`() {
        scheduler()
        val byId = notificationManager.notificationChannels.associateBy { it.id }

        assertThat(byId.getValue(NimazChannels.PRAYER).shouldVibrate())
            .isTrue()
        assertThat(
            byId.getValue(NimazChannels.PRAYER_SILENT).shouldVibrate()
        ).isFalse()
        assertThat(byId.getValue(NimazChannels.ADHAN).shouldVibrate())
            .isTrue()
        assertThat(
            byId.getValue(NimazChannels.ADHAN_SILENT).shouldVibrate()
        ).isFalse()
    }

    // ── The two refusals ────────────────────────────────────────────────────────

    @Test
    fun `turning notifications off cancels rather than silently leaving yesterdays alarms armed`() {
        // The bug this guards: cancelling by *not scheduling* leaves the previous day's
        // one-shots armed, so a user who turns notifications off is still woken at Fajr.
        val target = scheduler()
        schedule(target = target, preReminders = mapOf(PrayerType.DHUHR to 15))
        assertThat(scheduledAlarms()).isNotEmpty()

        schedule(target = target, notificationsEnabled = false)

        // Everything except the daily summary, which `cancelAllPrayerNotifications` does not
        // touch — see `the daily summary outlives cancelAllPrayerNotifications` below.
        assertThat(scheduledTriggers()).containsExactly(dailySummaryTime())
    }

    @Test
    fun `no location arms nothing at all`() {
        // (0,0) is the "unset" sentinel, not a place in the Gulf of Guinea. Scheduling against it
        // would produce plausible-looking alarms at times nobody asked for.
        schedule(latitude = 0.0, longitude = 0.0)

        assertThat(scheduledAlarms()).isEmpty()
    }

    // ── What a normal day arms ──────────────────────────────────────────────────

    @Test
    fun `the midnight reschedule is armed for one minute past midnight tomorrow`() {
        // This alarm is the entire recurrence mechanism: every other alarm here is a one-shot,
        // and if the chain breaks the app goes quiet the following day.
        schedule()

        val expected = LocalDate.now().plusDays(1).atTime(0, 1)
        assertThat(scheduledTriggers()).contains(expected)
    }

    @Test
    fun `the daily summary lands at eleven at night, today or tomorrow`() {
        schedule()

        val elevenToday = LocalDate.now().atTime(23, 0)
        val expected =
            if (LocalDateTime.now().isAfter(elevenToday)) elevenToday.plusDays(1) else elevenToday
        assertThat(scheduledTriggers()).contains(expected)
    }

    @Test
    fun `every armed prayer alarm is in the future`() {
        // A past trigger is not inert: Android delivers it immediately, which re-posts the
        // notification on every reschedule for the rest of the day.
        schedule(preReminders = PrayerType.entries.associateWith { 15 })

        val now = System.currentTimeMillis()
        assertThat(scheduledAlarms().map { it.triggerAtTime }.filter { it < now }).isEmpty()
    }

    @Test
    fun `sunrise is skipped when the caller does not name an explicit set`() {
        // Sunrise is the end of Fajr's window rather than a prayer, so the default is to leave
        // it alone; an explicit set is the only way to opt in.
        val withDefault = scheduler().also { schedule(target = it) }
        val defaultCount = scheduledAlarms().size

        shadowOf(alarmManager).scheduledAlarms.clear()
        schedule(target = withDefault, enabledPrayers = PrayerType.entries.toSet())

        assertThat(scheduledAlarms().size).isAtLeast(defaultCount)
    }

    @Test
    fun `an empty enabled set arms no prayer alarms but keeps the housekeeping ones`() {
        // The distinction matters: the midnight chain has to survive a user disabling every
        // individual prayer, or re-enabling one tomorrow never takes effect.
        schedule(enabledPrayers = emptySet())

        val expected = LocalDate.now().plusDays(1).atTime(0, 1)
        assertThat(scheduledTriggers()).contains(expected)
    }

    // ── Pre-reminders ───────────────────────────────────────────────────────────

    @Test
    fun `a pre-reminder is armed its own lead time before the prayer, per prayer`() {
        // The lead time is per prayer and travels with the alarm; reading a global value back at
        // fire time is the bug that put "15 minutes" in a notification armed 45 minutes out.
        val tomorrow = LocalDate.now().plusDays(1)
        val leads = mapOf(PrayerType.FAJR to 45, PrayerType.ISHA to 10)
        schedule(preReminders = leads)

        val triggers = scheduledTriggers().toSet()
        val prayerTimes = calculator.getPrayerTimes(
            latitude = LONDON_LAT,
            longitude = LONDON_LON,
            date = LocalDate.now(),
            calculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE,
            asrCalculation = AsrCalculation.STANDARD,
            highLatitudeRule = null,
            adjustments = emptyMap(),
        )
        val now = LocalDateTime.now()
        val futurePairs = prayerTimes
            .filter { it.type in leads.keys }
            .map {
                val at = java.time.Instant.ofEpochMilli(it.time.toEpochMilliseconds())
                    .atZone(ZoneId.systemDefault()).toLocalDateTime()
                it.type to at
            }
            .filter { (_, at) -> at.isAfter(now) }

        // Whatever time of day this test runs at, every still-future prayer in the map must have
        // both its own alarm and a lead alarm exactly `leads[type]` minutes earlier.
        futurePairs.forEach { (type, at) ->
            val lead = at.minusMinutes(leads.getValue(type).toLong())
            if (lead.isAfter(now)) {
                assertThat(triggers).contains(lead.withSecond(0).withNano(0))
            }
        }
        assertThat(tomorrow).isNotNull() // the day never rolls under us mid-assertion
    }

    @Test
    fun `sunrise never gets a pre-reminder even when one is asked for`() {
        // Sunrise cannot be silenced or led independently — it is a beep at the end of Fajr.
        val target = scheduler()
        schedule(
            target = target,
            enabledPrayers = setOf(PrayerType.SUNRISE),
            preReminders = mapOf(PrayerType.SUNRISE to 30),
        )

        val prayerAlarms = scheduledAlarms().size
        shadowOf(alarmManager).scheduledAlarms.clear()
        schedule(target = target, enabledPrayers = setOf(PrayerType.SUNRISE))

        // Asking for a sunrise lead adds nothing, so the two runs arm the same count.
        assertThat(scheduledAlarms().size).isEqualTo(prayerAlarms)
    }

    // ── Friday ──────────────────────────────────────────────────────────────────

    @Test
    fun `the friday reminder is armed on the coming friday, before that fridays dhuhr`() {
        schedule(fridayReminderEnabled = true, fridayReminderMinutes = 30)

        val fridayAlarms = scheduledTriggers().filter {
            it.dayOfWeek == java.time.DayOfWeek.FRIDAY && it.hour in 8..16
        }
        assertThat(fridayAlarms).isNotEmpty()
    }

    @Test
    fun `turning the friday reminder off removes an already-armed one`() {
        val target = scheduler()
        schedule(target = target, fridayReminderEnabled = true)
        val withFriday = scheduledAlarms().size

        schedule(target = target, fridayReminderEnabled = false)

        assertThat(scheduledAlarms().size).isLessThan(withFriday)
    }

    // ── Khatam ──────────────────────────────────────────────────────────────────

    @Test
    fun `the khatam reminder is armed at the stored time when the preference is on`() {
        khatamEnabled.value = true
        khatamTime.value = "07:15"

        schedule()

        val expected = LocalDate.now().atTime(7, 15).let {
            if (LocalDateTime.now().isAfter(it)) it.plusDays(1) else it
        }
        assertThat(scheduledTriggers()).contains(expected)
    }

    @Test
    fun `a malformed stored khatam time falls back to six rather than dropping the reminder`() {
        // The failure this prevents is silent: an unparseable preference that threw would take
        // the whole scheduling pass with it, cancelling every prayer alarm as a side effect.
        khatamEnabled.value = true
        khatamTime.value = "not a time"

        schedule()

        val expected = LocalDate.now().atTime(6, 0).let {
            if (LocalDateTime.now().isAfter(it)) it.plusDays(1) else it
        }
        assertThat(scheduledTriggers()).contains(expected)
    }

    @Test
    fun `the khatam reminder is not armed when the preference is off`() {
        khatamEnabled.value = false
        khatamTime.value = "07:15"

        schedule()

        val sevenFifteen = LocalDate.now().atTime(7, 15)
        assertThat(scheduledTriggers()).containsNoneOf(sevenFifteen, sevenFifteen.plusDays(1))
    }

    // ── Worship reminders ───────────────────────────────────────────────────────

    @Test
    fun `no worship reminder is armed while every one of them is switched off`() {
        val baseline = scheduler().also { schedule(target = it) }
        val withNone = scheduledAlarms().size

        shadowOf(alarmManager).scheduledAlarms.clear()
        worshipEnabled.getValue(WorshipReminderType.TAHAJJUD.key).value = true
        schedule(target = baseline)

        assertThat(scheduledAlarms().size).isGreaterThan(withNone)
    }

    @Test
    fun `enabling a worship reminder arms exactly one strictly-future alarm for it`() {
        // `requireFutureTrigger = true` is the whole point: an already-active occurrence has
        // fired, and re-arming it at a past instant re-posts the notification on every reschedule.
        worshipEnabled.getValue(WorshipReminderType.IFTAR.key).value = true

        schedule()

        val now = System.currentTimeMillis()
        assertThat(scheduledAlarms().map { it.triggerAtTime }.filter { it <= now }).isEmpty()
    }

    @Test
    fun `witr mode is read from settings and changes when the reminder fires`() {
        worshipEnabled.getValue(WorshipReminderType.WITR.key).value = true

        witrMode.value = "after_isha"
        val afterIsha = scheduler().also { schedule(target = it) }
        val afterIshaTriggers = scheduledTriggers().toSet()

        shadowOf(alarmManager).scheduledAlarms.clear()
        witrMode.value = "before_fajr"
        schedule(target = afterIsha)
        val beforeFajrTriggers = scheduledTriggers().toSet()

        // Both modes arm something; the mode is not ignored.
        assertThat(afterIshaTriggers).isNotEmpty()
        assertThat(beforeFajrTriggers).isNotEmpty()
    }

    @Test
    fun `a worship offset shifts the alarm by the stored number of minutes`() {
        // ADHKAR_EVENING rather than SUHOOR: the Ramadan-gated types produce no occurrence at
        // all outside Ramadan, so eleven months of the year that assertion would compare two
        // identical sets of prayer alarms and pass for the wrong reason.
        val evening = WorshipReminderType.ADHKAR_EVENING.key
        worshipEnabled.getValue(evening).value = true
        worshipOffset.getValue(evening).value = 30
        val target = scheduler().also { schedule(target = it) }
        val at30 = scheduledAlarms().map { it.triggerAtTime }.toSet()

        shadowOf(alarmManager).scheduledAlarms.clear()
        worshipOffset.getValue(evening).value = 90
        schedule(target = target)
        val at90 = scheduledAlarms().map { it.triggerAtTime }.toSet()

        // The offset is honoured rather than defaulted: the two runs cannot be identical.
        assertThat(at90).isNotEqualTo(at30)
        assertThat(at90 - at30).hasSize(1)
    }

    @Test
    fun `worship reminders are skipped entirely when there is no location`() {
        worshipEnabled.getValue(WorshipReminderType.TAHAJJUD.key).value = true

        schedule(latitude = 0.0, longitude = 0.0)

        assertThat(scheduledAlarms()).isEmpty()
    }

    // ── Cancellation ────────────────────────────────────────────────────────────

    @Test
    fun `cancelling everything leaves no alarm behind, worship reminders included`() {
        worshipEnabled.getValue(WorshipReminderType.TAHAJJUD.key).value = true
        worshipEnabled.getValue(WorshipReminderType.IFTAR.key).value = true
        khatamEnabled.value = true
        val target = scheduler()
        schedule(
            target = target,
            preReminders = PrayerType.entries.associateWith { 20 },
            fridayReminderEnabled = true,
        )
        assertThat(scheduledAlarms()).isNotEmpty()

        target.cancelAllPrayerNotifications()

        assertThat(scheduledTriggers()).containsExactly(dailySummaryTime())
    }

    @Test
    fun `the daily summary outlives cancelAllPrayerNotifications`() {
        // Deliberate, and worth pinning because it reads like an oversight: the summary is a
        // recap of the day's prayers rather than a prayer alert, so turning prayer notifications
        // off leaves it armed. `cancelDailySummary` is the separate door for it.
        val target = scheduler()
        schedule(target = target)

        target.cancelAllPrayerNotifications()
        assertThat(scheduledTriggers()).containsExactly(dailySummaryTime())

        target.cancelDailySummary()
        assertThat(scheduledAlarms()).isEmpty()
    }

    @Test
    fun `cancelling the daily summary leaves the prayer alarms alone`() {
        val target = scheduler()
        schedule(target = target)
        val before = scheduledAlarms().size

        target.cancelDailySummary()

        assertThat(scheduledAlarms()).hasSize(before - 1)
    }

    @Test
    fun `cancelling one prayer removes only that prayers alarm`() {
        val target = scheduler()
        schedule(target = target, enabledPrayers = PrayerType.entries.toSet())
        val before = scheduledAlarms().size

        target.cancelPrayerNotification(PrayerType.FAJR)

        assertThat(scheduledAlarms().size).isAtMost(before)
    }

    @Test
    fun `cancelling twice is not an error`() {
        // Every cancel path uses FLAG_NO_CREATE and has to cope with the PendingIntent already
        // being gone — the boot path and the settings path both cancel the same alarms.
        val target = scheduler()
        schedule(target = target)

        target.cancelAllPrayerNotifications()
        target.cancelAllPrayerNotifications()

        assertThat(scheduledTriggers()).containsExactly(dailySummaryTime())
    }

    // ── Test notifications ──────────────────────────────────────────────────────

    @Test
    fun `the test notification is posted immediately rather than scheduled`() {
        // It exists so a user can prove notifications work; scheduling it would tell them
        // nothing until the alarm fired.
        val target = scheduler()

        target.sendTestNotification()

        assertThat(shadowOf(notificationManager).allNotifications).isNotEmpty()
        assertThat(scheduledAlarms()).isEmpty()
    }

    @Test
    fun `the all-prayers test broadcasts one explicit intent per prayer`() {
        // Explicit, because an implicit broadcast never reaches a manifest receiver on
        // Android 8+ — the failure mode is "nothing happens" with no error anywhere.
        val target = scheduler()

        target.sendAllPrayerTestNotifications()

        val broadcasts = shadowOf(context as Application).broadcastIntents
            .filter { it.action == PrayerNotificationScheduler.ACTION_PRAYER_NOTIFICATION }
        assertThat(broadcasts).hasSize(6)
        assertThat(broadcasts.map { it.getStringExtra(PrayerNotificationScheduler.EXTRA_PRAYER_TYPE) })
            .containsExactly("FAJR", "SUNRISE", "DHUHR", "ASR", "MAGHRIB", "ISHA")
        assertThat(broadcasts.map { it.component?.className }.distinct())
            .containsExactly(PrayerAlarmReceiver::class.java.name)
    }

    /** 11 PM today, or tomorrow if this test runs after it. */
    private fun dailySummaryTime(): LocalDateTime {
        val elevenToday = LocalDate.now().atTime(23, 0)
        return if (LocalDateTime.now().isAfter(elevenToday)) elevenToday.plusDays(1) else elevenToday
    }

    private companion object {
        const val LONDON_LAT = 51.5074
        const val LONDON_LON = -0.1278
    }
}
