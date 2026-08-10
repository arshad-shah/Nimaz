package com.arshadshah.nimaz.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test

/**
 * Generates `app/src/main/baseline-prof.txt`.
 *
 * The journey is deliberately narrow: cold start, then the two screens that dominate
 * real usage, then the one interaction the audit named as janky on first run — the
 * first scroll of the Quran reader, which is interpreted rather than AOT-compiled
 * without a profile.
 *
 * Run with `./gradlew :app:generateBaselineProfile`. It boots the `pixel6Api34`
 * managed device, so it needs no emulator open, but it does download a system image
 * on first run.
 */
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(
        packageName = PACKAGE,
        // Three iterations is the plugin's own guidance: enough for the profile to
        // stabilise, few enough that a run stays minutes rather than tens of minutes.
        maxIterations = 3,
        stableIterations = 2,
        includeInStartupProfile = true,
    ) {
        pressHome()
        startActivityAndWait()

        // Home is the launch destination; give its first frame time to settle before
        // navigating, or the profile records the navigation and not the screen.
        device.waitForIdle()

        openQuranAndScroll()
    }

    private fun androidx.benchmark.macro.MacrobenchmarkScope.openQuranAndScroll() {
        // Navigating by content description rather than resource id: the bottom bar is
        // Compose, so there are no view ids to match on, and the labels are the same
        // strings a user reads.
        val quran = device.findObject(By.descContains("Quran")) ?: return
        quran.click()
        device.waitForIdle()

        // Open the first surah in the list, then scroll the reader. Al-Fatiha is short,
        // so scroll the surah list first to make sure the list itself is profiled too.
        device.wait(Until.hasObject(By.scrollable(true)), TIMEOUT_MS)
        device.findObject(By.scrollable(true))?.let { list ->
            list.setGestureMargin(device.displayWidth / MARGIN_DIVISOR)
            list.scroll(Direction.DOWN, SCROLL_PERCENT)
            device.waitForIdle()
            list.scroll(Direction.UP, SCROLL_PERCENT)
        }
        device.waitForIdle()

        val surah = device.findObject(By.clickable(true)) ?: return
        surah.click()
        device.wait(Until.hasObject(By.scrollable(true)), TIMEOUT_MS)

        device.findObject(By.scrollable(true))?.let { reader ->
            reader.setGestureMargin(device.displayWidth / MARGIN_DIVISOR)
            repeat(READER_SCROLLS) {
                reader.scroll(Direction.DOWN, SCROLL_PERCENT)
                device.waitForIdle()
            }
        }
    }

    private companion object {
        const val PACKAGE = "com.arshadshah.nimaz"
        const val TIMEOUT_MS = 5_000L
        const val SCROLL_PERCENT = 0.8f
        const val READER_SCROLLS = 3

        // UiAutomator's default gesture margin can start a swipe inside the system
        // gesture zone, which the system consumes instead of the app.
        const val MARGIN_DIVISOR = 5
    }
}
