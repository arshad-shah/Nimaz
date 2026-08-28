package com.arshadshah.nimaz.core.common

/**
 * Every notification channel id the app ships, in one place.
 *
 * **These strings are user data, not identifiers.** A channel id is the key Android files a
 * user's per-channel sound, importance and vibration choices under. Renaming one does not
 * migrate those choices — it creates a *new* channel at its declared defaults and orphans the
 * old one, silently resetting a preference the user set deliberately. So the literals below may
 * be added to and may be retired, but a live one is never edited. `NimazChannelsTest` pins each
 * literal for exactly that reason, and `scripts/check_docs.py` (SUB-05) fails if one is absent
 * from the §0.6 table in `docs/SUBSYSTEMS.md`.
 *
 * **Why they live here rather than beside the code that creates them.** They were spread across
 * four files — eight on `PrayerNotificationScheduler`, one each on the three foreground services,
 * one on `AnnouncementBootstrap` — and `AdhanPlaybackService` reached across for
 * `PrayerNotificationScheduler.CHANNEL_ID_ADHAN` because a channel id is the one thing an audio
 * service and the notification scheduler genuinely share. That single reference is a
 * `:core:audio` → `:core:notifications` edge, and `BootReceiver` starting `AdhanPlaybackService`
 * is the same edge back: a Gradle circular dependency, and one `moduleBoundary` would not have
 * caught first because both sides are `:core:*`. Naming the vocabulary in a module *below* both
 * removes the cycle rather than papering over it.
 *
 * `:core:common` is the right floor: an id is a plain `String` with no Android type in sight, so
 * nothing here needs a `Context`, a resource or `R`.
 */
object NimazChannels {

    // ── Prayer and adhan ──────────────────────────────────────────────────────────────────────
    // Created by PrayerNotificationScheduler.init. See docs/SUBSYSTEMS.md §4.

    const val PRAYER = "prayer_notifications"
    const val ADHAN = "adhan_notifications"
    const val DAILY_SUMMARY = "daily_summary_notifications"
    const val KHATAM = "khatam_notifications"

    /**
     * Extended worship reminders (Tahajjud, Suhoor, Iftar, …) — a gentle DEFAULT-importance
     * nudge like [KHATAM], never an alarm. See spec §2 (epic #300).
     */
    const val WORSHIP = "worship_reminders"

    /**
     * Silent (no-vibration) siblings — Android ignores `enableVibration()` changes after a
     * channel exists, so the vibration preference is honoured by posting on the matching channel
     * instead. See [forPrayer] and [forAdhan].
     */
    const val PRAYER_SILENT = "prayer_notifications_silent"
    const val ADHAN_SILENT = "adhan_notifications_silent"

    /**
     * A prayer set to the SILENT alert style posts here: no sound, no vibration, no heads-up.
     * [PRAYER_SILENT] and [ADHAN_SILENT] are only *no-vibration* siblings — they still carry the
     * channel's sound at `IMPORTANCE_HIGH` — and Android will not let an existing channel's
     * importance be lowered from code, so silence needs its own.
     */
    const val PRAYER_MUTED = "prayer_notifications_muted"

    // ── Foreground services ───────────────────────────────────────────────────────────────────
    // Created in each service's onCreate. See docs/SUBSYSTEMS.md §1.

    const val ADHAN_DOWNLOAD = "adhan_download_channel"

    /**
     * `AdhanPlaybackService` creates this and then posts its *visible* notification on [ADHAN].
     * It is kept because a foreground service must declare some channel at start. See §4.
     */
    const val ADHAN_PLAYBACK = "adhan_playback_channel"

    const val QURAN_AUDIO = "quran_audio_channel"

    // ── Engagement ────────────────────────────────────────────────────────────────────────────

    /**
     * Created by `AnnouncementBootstrap`, and also named as a **string literal** in the app
     * manifest's `com.google.firebase.messaging.default_notification_channel_id` meta-data — a
     * manifest cannot reference a Kotlin constant, so that one duplicate is unavoidable.
     * `NimazChannelsTest` pins the literal so the two cannot drift apart. See §12.
     */
    const val ANNOUNCEMENTS = "nimaz_announcements"

    /**
     * Channel id for a standalone prayer notification honouring the vibration pref.
     *
     * [muted] wins over [vibrate]: a prayer the user has silenced posts on the muted channel
     * whatever the vibration preference says.
     */
    fun forPrayer(vibrate: Boolean, muted: Boolean = false): String = when {
        muted -> PRAYER_MUTED
        vibrate -> PRAYER
        else -> PRAYER_SILENT
    }

    /** Channel id for an adhan notification honouring the vibration pref. */
    fun forAdhan(vibrate: Boolean): String = if (vibrate) ADHAN else ADHAN_SILENT
}
