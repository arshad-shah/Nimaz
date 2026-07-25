package com.arshadshah.nimaz.domain.model

import java.time.LocalDateTime

/**
 * Extended, opt-in worship & fasting reminders — Sunnah occasions that are *derived* from the
 * daily prayer times, the Sunnah night times ([PrayerTimes] + adhan2 `SunnahTimes`) or the
 * Hijri calendar. These are **not** fard prayers, so they are modelled here rather than added to
 * [PrayerType] (which drives prayer tracking/stats).
 *
 * The whole feature is data-driven off this enum: the scheduler, the notification content, the
 * settings rows and the Home "Next Worship" card all iterate [WorshipReminderType] instead of
 * hand-coding each reminder. See the design spec (epic #300).
 */
enum class WorshipReminderCategory {
    /** Night worship — Tahajjud, Witr. */
    NIGHT,

    /** Ramadan-only — auto-gated to Ramadan and auto-hidden in settings otherwise. */
    RAMADAN,

    /** Sunnah fasting & daily dhikr — Hijri/calendar driven. */
    FASTING_DHIKR
}

enum class WorshipReminderType(
    /** Stable key: DataStore pref suffix, deep-link segment, analytics label. Never rename. */
    val key: String,
    val category: WorshipReminderCategory,
    /** Ramadan-gated reminders only fire during Ramadan and hide outside it. */
    val ramadanOnly: Boolean = category == WorshipReminderCategory.RAMADAN,
    /** Whether the settings row exposes an editable offset (minutes) stepper. */
    val hasOffset: Boolean = false,
    /** Default offset in minutes (sign is applied by the calculator per type). */
    val defaultOffsetMinutes: Int = 0
) {
    // ── A · Night worship ────────────────────────────────────────────────
    TAHAJJUD("tahajjud", WorshipReminderCategory.NIGHT),
    WITR("witr", WorshipReminderCategory.NIGHT, hasOffset = true, defaultOffsetMinutes = 30),

    // ── C · Ramadan ──────────────────────────────────────────────────────
    SUHOOR("suhoor", WorshipReminderCategory.RAMADAN, hasOffset = true, defaultOffsetMinutes = 30),
    IFTAR("iftar", WorshipReminderCategory.RAMADAN, hasOffset = true, defaultOffsetMinutes = 0),
    TARAWEEH("taraweeh", WorshipReminderCategory.RAMADAN, hasOffset = true, defaultOffsetMinutes = 15),
    LAYLATUL_QADR("laylatul_qadr", WorshipReminderCategory.RAMADAN),

    // ── D · Fasting & Dhikr ──────────────────────────────────────────────
    ADHKAR_MORNING("adhkar_morning", WorshipReminderCategory.FASTING_DHIKR, hasOffset = true, defaultOffsetMinutes = 30),
    ADHKAR_EVENING("adhkar_evening", WorshipReminderCategory.FASTING_DHIKR, hasOffset = true, defaultOffsetMinutes = 30),
    MONDAY_THURSDAY_FAST("mon_thu_fast", WorshipReminderCategory.FASTING_DHIKR),
    WHITE_DAYS_FAST("white_days_fast", WorshipReminderCategory.FASTING_DHIKR),
    ARAFAH_ASHURA_FAST("arafah_ashura_fast", WorshipReminderCategory.FASTING_DHIKR);

    companion object {
        fun fromKey(key: String?): WorshipReminderType? = entries.firstOrNull { it.key == key }
    }
}

/**
 * A concrete, scheduled instance of a worship reminder: *when it fires* ([triggerAt]) and the
 * wall-clock instant it is *about* ([eventAt], e.g. the exact Maghrib for Iftar or the start of
 * the last third for Tahajjud). The Home card shows [eventAt] + a countdown; the scheduler arms
 * an alarm at [triggerAt]. For most reminders the two are equal; they differ when the reminder
 * leads the event (Suhoor fires before Fajr, Witr before it, etc.).
 *
 * [subKey] optionally distinguishes a variant within a type (e.g. `monday`/`thursday`,
 * `arafah`/`ashura`) so the content helper can pick the right copy.
 */
data class WorshipReminderOccurrence(
    val type: WorshipReminderType,
    val triggerAt: LocalDateTime,
    val eventAt: LocalDateTime,
    val subKey: String? = null
)
