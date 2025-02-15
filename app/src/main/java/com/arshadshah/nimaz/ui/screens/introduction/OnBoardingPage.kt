package com.arshadshah.nimaz.ui.screens.introduction

import androidx.compose.runtime.Composable
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.components.intro.IntroBatteryExemption
import com.arshadshah.nimaz.ui.components.intro.IntroCalculation
import com.arshadshah.nimaz.ui.components.intro.IntroLocation
import com.arshadshah.nimaz.ui.components.intro.IntroNotification


/**
 * Represents a single page in the onboarding flow
 * @param image Resource ID for the illustration
 * @param title Page title
 * @param description Page description
 * @param category Optional category for grouping related pages
 * @param extra Optional composable content for additional UI elements
 */
data class OnBoardingPage(
    val image: Int,
    val title: String,
    val description: String,
    val category: String = "",
    val extra: (@Composable () -> Unit)? = null,
)

/**
 * Contains all onboarding pages for the app
 */
object OnBoardingPages {
    private val pages = listOf(
        OnBoardingPage(
            image = R.drawable.praying,
            title = "Assalamu alaykum",
            category = "Welcome",
            description = "Experience peaceful prayer times with our beautifully designed, " +
                    "accurate, and completely Ad-Free Islamic companion."
        ),
        OnBoardingPage(
            image = R.drawable.quran,
            title = "Spiritual Guidance",
            category = "Features",
            description = "Access the Holy Quran with Urdu and English translations, " +
                    "audio recitations, and bookmarking for your spiritual journey."
        ),
        OnBoardingPage(
            image = R.drawable.tracker_icon,
            title = "Track Your Journey",
            category = "Features",
            description = "Stay connected with your spiritual practices through our " +
                    "intuitive Prayer and Fasting tracker."
        ),
        OnBoardingPage(
            image = R.drawable.adhan,
            title = "Prayer Reminders",
            category = "Setup",
            description = "Never miss a prayer with customizable Adhan notifications " +
                    "that respect your schedule and preferences.",
            extra = { IntroNotification() }
        ),
        OnBoardingPage(
            image = R.drawable.location_pin,
            title = "Precise Timing",
            category = "Setup",
            description = "Get accurate prayer times and Qibla direction based on your location. " +
                    "Manual location setting is also available for your convenience.",
            extra = { IntroLocation() }
        ),
        OnBoardingPage(
            image = R.drawable.battery,
            title = "Reliable Notifications",
            category = "Setup",
            description = "Ensure timely notifications by optimizing battery settings " +
                    "for uninterrupted prayer reminders.",
            extra = { IntroBatteryExemption() }
        ),
        OnBoardingPage(
            image = R.drawable.time_calculation,
            title = "Prayer Time Calculation",
            category = "Setup",
            description = "Choose between the Muslim World League method or automatic " +
                    "calculations based on the sun's position for precise prayer times.",
            extra = { IntroCalculation() }
        ),
        OnBoardingPage(
            image = R.drawable.check_mark,
            title = "Ready to Begin",
            category = "Complete",
            description = "Your app is now perfectly configured for your spiritual journey. " +
                    "Settings can be adjusted anytime. Please keep us in your prayers."
        )
    )

    operator fun get(index: Int) = pages.getOrNull(index)
    val size get() = pages.size
    // Group pages by category for progress tracking
    val categories = pages.groupBy { it.category }
}