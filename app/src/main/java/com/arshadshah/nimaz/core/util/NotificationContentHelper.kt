package com.arshadshah.nimaz.core.util

import com.arshadshah.nimaz.domain.model.PrayerName
import com.arshadshah.nimaz.domain.model.PrayerStatus
import java.time.LocalTime
import kotlin.random.Random

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

    // Prayer-specific titles with variety
    private val fajrTitles = listOf(
        "Fajr - The Dawn Prayer",
        "Rise for Fajr",
        "Fajr Time Has Arrived",
        "The Morning Prayer Awaits"
    )

    private val dhuhrTitles = listOf(
        "Dhuhr - The Noon Prayer",
        "Time for Dhuhr",
        "Dhuhr Prayer Time",
        "The Midday Prayer"
    )

    private val asrTitles = listOf(
        "Asr - The Afternoon Prayer",
        "Time for Asr",
        "Asr Prayer Time",
        "The Afternoon Prayer"
    )

    private val maghribTitles = listOf(
        "Maghrib - The Sunset Prayer",
        "Time for Maghrib",
        "Break Your Fast with Maghrib",
        "The Evening Prayer"
    )

    private val ishaTitles = listOf(
        "Isha - The Night Prayer",
        "Time for Isha",
        "Isha Prayer Time",
        "The Night Prayer Awaits"
    )

    private val sunriseTitles = listOf(
        "Sunrise - Ishraq Time",
        "The Sun Has Risen",
        "Sunrise Alert"
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
     * Get a notification title for a specific prayer.
     */
    fun getPrayerTitle(prayerName: String): String {
        return when (prayerName.uppercase()) {
            "FAJR" -> fajrTitles.random()
            "SUNRISE" -> sunriseTitles.random()
            "DHUHR" -> dhuhrTitles.random()
            "ASR" -> asrTitles.random()
            "MAGHRIB" -> maghribTitles.random()
            "ISHA" -> ishaTitles.random()
            else -> "$prayerName Time"
        }
    }

    /**
     * Get a motivational message for a specific prayer.
     */
    fun getPrayerMessage(prayerName: String, prayerTime: String = ""): String {
        val timeInfo = if (prayerTime.isNotEmpty()) " ($prayerTime)" else ""
        val message = when (prayerName.uppercase()) {
            "FAJR" -> fajrMessages.random()
            "SUNRISE" -> sunriseMessages.random()
            "DHUHR" -> dhuhrMessages.random()
            "ASR" -> asrMessages.random()
            "MAGHRIB" -> maghribMessages.random()
            "ISHA" -> ishaMessages.random()
            else -> "It's time for $prayerName prayer."
        }
        return message
    }

    /**
     * Get a short message for notification content.
     */
    fun getShortMessage(prayerName: String): String {
        return when (prayerName.uppercase()) {
            "FAJR" -> "\"Prayer is better than sleep.\" - Answer the Fajr call."
            "SUNRISE" -> "The sun has risen. Ishraq time begins."
            "DHUHR" -> "Pause your day. Connect with Allah."
            "ASR" -> "\"Guard strictly the middle prayer.\" - Time for Asr."
            "MAGHRIB" -> "The sun has set. Time for Maghrib."
            "ISHA" -> "Complete your day with Isha prayer."
            else -> "It's time for $prayerName prayer."
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

    /**
     * Get an emoji for the prayer (for use in notification when supported).
     */
    fun getPrayerEmoji(prayerName: String): String {
        return when (prayerName.uppercase()) {
            "FAJR" -> "🌅"
            "SUNRISE" -> "☀️"
            "DHUHR" -> "🕐"
            "ASR" -> "🌤️"
            "MAGHRIB" -> "🌅"
            "ISHA" -> "🌙"
            else -> "🕌"
        }
    }

    data class DailySummaryContent(
        val title: String,
        val message: String,
        val bigText: String,
        val isPositive: Boolean
    )
}
