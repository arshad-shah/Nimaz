package com.arshadshah.nimaz.core.util

import android.content.Context
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.KhatamPace
import java.time.LocalTime

/**
 * Helper class for generating beautiful, engaging notification content
 * with Islamic greetings and contextual messages.
 */
object NotificationContentHelper {

    // Islamic greetings
    private val islamicGreetings = listOf(
        "As-salamu alaykum",
        "Peace be upon you",
        "Bismillah"
    )

    // Motivational messages for each prayer
    private val fajrMessages = listOf(
        "\"Prayer is better than sleep.\" Start your day blessed.",
        "The angels witness Fajr. Be among those who answer the call.",
        "Wake up to Allah's mercy. The early morning holds special blessings.",
        "\"The two rakats of Fajr are better than the world and all it contains.\"",
        "Begin your day in remembrance of Allah. Success follows those who pray."
    )

    private val dhuhrMessages = listOf(
        "Pause from your work and connect with your Creator.",
        "The midday prayer brings tranquility to a busy day.",
        "Take a moment to recharge your soul with Dhuhr.",
        "\"Indeed, prayer prohibits immorality and wrongdoing.\"",
        "A few minutes with Allah can transform your entire day."
    )

    private val asrMessages = listOf(
        "\"Guard strictly the prayers, especially the middle prayer.\"",
        "The afternoon prayer - don't let it pass unnoticed.",
        "Asr: A moment of reflection as the day progresses.",
        "The Prophet (PBUH) emphasized the importance of Asr. Answer the call.",
        "Pause and pray. Allah awaits your conversation."
    )

    private val maghribMessages = listOf(
        "As the sun sets, rise in worship of the Most High.",
        "End your day's work with gratitude to Allah.",
        "The night begins with Maghrib. Start it right.",
        "\"Whoever prays the two cool prayers will enter Paradise.\"",
        "Witness the beauty of sunset and thank your Creator."
    )

    private val ishaMessages = listOf(
        "Complete your daily prayers with Isha.",
        "\"If they only knew what was in Isha and Fajr...\"",
        "The night prayer - your final conversation with Allah today.",
        "Isha: Seal your day with blessings and forgiveness.",
        "Let your last act of the day be worship."
    )

    // Reflections shown *below* the short "sun has risen" line — keep them distinct
    // from notif_short_sunrise so the expanded notification doesn't repeat itself.
    private val sunriseMessages = listOf(
        "Whoever prays Fajr, then remembers Allah until sunrise and prays two rak'ahs, earns the reward of a Hajj and Umrah.",
        "A new day of mercy and opportunity begins — seize its blessings.",
        "The Duha prayer is a charity due for every joint of the body."
    )

    // Pre-reminder messages
    private val preReminderMessages = listOf(
        "Prepare your heart and make wudu.",
        "Time to get ready for prayer.",
        "A reminder to prepare for worship.",
        "Get ready to stand before your Lord."
    )

    // Daily summary - all prayers completed
    private val allPrayersCompletedMessages = listOf(
        "Masha'Allah! You've completed all your prayers today.",
        "Alhamdulillah! A perfect day of prayer.",
        "Well done! All 5 prayers completed today.",
        "Allah is pleased with those who maintain their prayers.",
        "Barakallahu feek! You've fulfilled your duty today."
    )

    // Daily summary - some prayers missed
    private val somePrayersMissedMessages = listOf(
        "Remember, it's never too late to make up missed prayers.",
        "Don't lose hope in Allah's mercy. Make qada for missed prayers.",
        "Tomorrow is a new opportunity to maintain all prayers.",
        "Seek Allah's forgiveness and strive to do better."
    )

    // Daily summary - all prayers missed
    private val allPrayersMissedMessages = listOf(
        "Today's prayers were missed, but Allah's door is always open.",
        "Make tawbah and start fresh tomorrow, insha'Allah.",
        "Don't despair. Allah loves those who return to Him."
    )

    /**
     * Notification title for a prayer: the prayer name with its time appended so
     * the user reads *what* and *when* at a glance (e.g. "Fajr · 5:30 AM").
     * Replaces the previous long, random title phrases — tighter and consistent.
     */
    fun getPrayerTitle(prayerName: String, prayerTime: String = ""): String {
        val name = prayerDisplayName(prayerName)
        return if (prayerTime.isNotBlank()) "$name · $prayerTime" else name
    }

    /** Clean, title-cased prayer name from a raw type/name string. */
    private fun prayerDisplayName(prayerName: String): String = when (prayerName.uppercase()) {
        "FAJR" -> "Fajr"
        "SUNRISE" -> "Sunrise"
        "DHUHR" -> "Dhuhr"
        "ASR" -> "Asr"
        "MAGHRIB" -> "Maghrib"
        "ISHA" -> "Isha"
        else -> prayerName.lowercase().replaceFirstChar { it.uppercase() }
    }

    /**
     * A short reflection (hadith / aya / encouragement) shown on the expanded
     * notification, below the reminder line.
     */
    fun getPrayerMessage(prayerName: String): String {
        return when (prayerName.uppercase()) {
            "FAJR" -> fajrMessages.random()
            "SUNRISE" -> sunriseMessages.random()
            "DHUHR" -> dhuhrMessages.random()
            "ASR" -> asrMessages.random()
            "MAGHRIB" -> maghribMessages.random()
            "ISHA" -> ishaMessages.random()
            else -> "It's time for ${prayerDisplayName(prayerName)} prayer."
        }
    }

    /**
     * Short, calm one-line reminder for the collapsed notification (and the
     * first line when expanded). No nested quotes — the time already lives in
     * the title, so this is purely the gentle nudge to pray.
     */
    fun getShortMessage(context: Context, prayerName: String): String {
        return when (prayerName.uppercase()) {
            "FAJR" -> context.getString(R.string.notif_short_fajr)
            "SUNRISE" -> context.getString(R.string.notif_short_sunrise)
            "DHUHR" -> context.getString(R.string.notif_short_dhuhr)
            "ASR" -> context.getString(R.string.notif_short_asr)
            "MAGHRIB" -> context.getString(R.string.notif_short_maghrib)
            "ISHA" -> context.getString(R.string.notif_short_isha)
            else -> context.getString(R.string.notif_short_generic, prayerDisplayName(prayerName))
        }
    }

    /**
     * Get a pre-reminder notification title.
     */
    fun getPreReminderTitle(context: Context, prayerName: String, minutesBefore: Int): String {
        return context.getString(R.string.notif_pre_reminder_title, prayerName, minutesBefore)
    }

    /**
     * Get a pre-reminder notification message.
     */
    fun getPreReminderMessage(context: Context, prayerName: String): String {
        return listOf(
            context.getString(R.string.notif_pre_reminder_1),
            context.getString(R.string.notif_pre_reminder_2),
            context.getString(R.string.notif_pre_reminder_3),
            context.getString(R.string.notif_pre_reminder_4)
        ).random()
    }

    /** Title for the weekly Friday (Jummah) reminder. */
    fun getFridayReminderTitle(context: Context): String =
        context.getString(R.string.notif_friday_title)

    /** Short one-line body for the Friday reminder. */
    fun getFridayReminderMessage(context: Context): String = listOf(
        context.getString(R.string.notif_friday_1),
        context.getString(R.string.notif_friday_2),
        context.getString(R.string.notif_friday_3)
    ).random()

    /** Title for the daily khatam reading reminder. */
    fun getKhatamReminderTitle(context: Context): String =
        context.getString(R.string.notif_khatam_title)

    /**
     * Body for the khatam reminder. The wording is chosen from [pace] so a reader who
     * has slipped gets a nudge to catch up rather than the same neutral line.
     */
    fun getKhatamReminderBody(
        context: Context,
        khatamName: String,
        ayahsToday: Int,
        pace: KhatamPace
    ): String = when (pace) {
        KhatamPace.ON_TRACK ->
            context.getString(R.string.notif_khatam_body_on_track, khatamName, ayahsToday)

        KhatamPace.BEHIND, KhatamPace.SLIGHTLY_BEHIND ->
            context.getString(R.string.notif_khatam_body_behind, khatamName, ayahsToday)

        KhatamPace.NOT_STARTED ->
            context.getString(R.string.notif_khatam_body_generic, khatamName, ayahsToday)
    }

    /** Expanded body for the Friday reminder — the Sunnah preparations. */
    fun getFridayReminderBigText(context: Context): String =
        context.getString(R.string.notif_friday_bigtext)

    /**
     * Get a contextual greeting based on time of day.
     */
    fun getTimeBasedGreeting(context: Context): String {
        val hour = LocalTime.now().hour
        return when {
            hour < 6 -> context.getString(R.string.notif_greeting_predawn)
            hour < 12 -> context.getString(R.string.notif_greeting_morning)
            hour < 17 -> context.getString(R.string.notif_greeting_afternoon)
            hour < 20 -> context.getString(R.string.notif_greeting_evening)
            else -> context.getString(R.string.notif_greeting_night)
        }
    }

    /**
     * Generate daily summary notification content.
     */
    fun getDailySummaryContent(
        context: Context,
        prayedCount: Int,
        missedCount: Int,
        missedPrayers: List<String>
    ): DailySummaryContent {
        val totalPrayers = 5 // Excluding sunrise

        return when {
            prayedCount == totalPrayers -> {
                DailySummaryContent(
                    title = context.getString(R.string.notif_summary_title_all_complete),
                    message = allPrayersCompletedMessages.random(),
                    bigText = "You've completed all $totalPrayers prayers today.\n\n" +
                            "Keep up this beautiful consistency!\n" +
                            "\"Those who maintain their prayers will have light, proof, and salvation on the Day of Resurrection.\"",
                    isPositive = true
                )
            }

            missedCount == totalPrayers -> {
                DailySummaryContent(
                    title = context.getString(R.string.notif_summary_title_default),
                    message = allPrayersMissedMessages.random(),
                    bigText = "Today's prayers have passed.\n\n" +
                            "Don't lose hope - Allah's mercy is vast.\n" +
                            "Consider making up missed prayers (Qada).\n\n" +
                            "\"Say: O My servants who have transgressed against themselves, do not despair of the mercy of Allah.\" - Quran 39:53",
                    isPositive = false
                )
            }

            else -> {
                val missedList = missedPrayers.joinToString(", ")
                DailySummaryContent(
                    title = context.getString(R.string.notif_summary_title_default),
                    message = context.getString(
                        R.string.notif_summary_count,
                        prayedCount,
                        totalPrayers
                    ),
                    bigText = "Today's Progress: $prayedCount/$totalPrayers prayers\n\n" +
                            (if (missedPrayers.isNotEmpty()) "Missed: $missedList\n\n" else "") +
                            somePrayersMissedMessages.random() + "\n\n" +
                            "May tomorrow be a better day, insha'Allah.",
                    isPositive = prayedCount > missedCount
                )
            }
        }
    }

    data class DailySummaryContent(
        val title: String,
        val message: String,
        val bigText: String,
        val isPositive: Boolean
    )
}
