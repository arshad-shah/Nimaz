package com.arshadshah.nimaz.core.util

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

    private val sunriseMessages = listOf(
        "The sun has risen. Time for Ishraq if you've prayed Fajr.",
        "A new day of mercy and opportunity begins.",
        "The prohibited time for prayer is ending soon."
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
    fun getShortMessage(prayerName: String): String {
        return when (prayerName.uppercase()) {
            "FAJR" -> "It's time to pray. Begin your day with Allah."
            "SUNRISE" -> "The sun has risen — Ishraq time begins."
            "DHUHR" -> "It's time to pray. Pause and turn to Allah."
            "ASR" -> "It's time to pray. Don't let Asr pass by."
            "MAGHRIB" -> "It's time to pray as the day draws to a close."
            "ISHA" -> "It's time to pray. Seal your day with Isha."
            else -> "It's time for ${prayerDisplayName(prayerName)} prayer."
        }
    }

    /**
     * Get a pre-reminder notification title.
     */
    fun getPreReminderTitle(prayerName: String, minutesBefore: Int): String {
        return "$prayerName in $minutesBefore minutes"
    }

    /**
     * Get a pre-reminder notification message.
     */
    fun getPreReminderMessage(prayerName: String): String {
        return preReminderMessages.random()
    }

    /**
     * Get a contextual greeting based on time of day.
     */
    fun getTimeBasedGreeting(): String {
        val hour = LocalTime.now().hour
        return when {
            hour < 6 -> "May your morning be blessed"
            hour < 12 -> "Good morning, may Allah bless your day"
            hour < 17 -> "Good afternoon, stay mindful of your prayers"
            hour < 20 -> "Good evening, may your worship be accepted"
            else -> "May your night be peaceful"
        }
    }

    /**
     * Generate daily summary notification content.
     */
    fun getDailySummaryContent(
        prayedCount: Int,
        missedCount: Int,
        missedPrayers: List<String>
    ): DailySummaryContent {
        val totalPrayers = 5 // Excluding sunrise

        return when {
            prayedCount == totalPrayers -> {
                DailySummaryContent(
                    title = "Masha'Allah! All Prayers Complete",
                    message = allPrayersCompletedMessages.random(),
                    bigText = "You've completed all $totalPrayers prayers today.\n\n" +
                            "Keep up this beautiful consistency!\n" +
                            "\"Those who maintain their prayers will have light, proof, and salvation on the Day of Resurrection.\"",
                    isPositive = true
                )
            }
            missedCount == totalPrayers -> {
                DailySummaryContent(
                    title = "Daily Prayer Summary",
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
                    title = "Daily Prayer Summary",
                    message = "$prayedCount of $totalPrayers prayers completed",
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
