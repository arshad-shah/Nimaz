package com.arshadshah.nimaz.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test

/**
 * Generates `app/src/release/generated/baselineProfiles/`.
 *
 * The journey is deliberately narrow: cold start, then the two screens that dominate
 * real usage, then the one interaction the audit named as janky on first run — the
 * first scroll of the Quran reader, which is interpreted rather than AOT-compiled
 * without a profile.
 *
 * **A generation device is always a fresh install**, so the app opens onto onboarding
 * rather than Home, and onboarding asks for permissions. The first version of this file
 * did not account for that: it produced 2,818 app rules covering
 * `presentation/screens/onboarding` and nothing else — a profile for a screen most users
 * see exactly once. Everything before [openQuranAndScroll] exists to get past that.
 *
 * Run with `./gradlew :app:generateBaselineProfile`. It boots the `pixel6Api34` managed
 * device, so it needs no emulator open, but it does download a system image on first run.
 */
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    /**
     * Cold start only, and this is the one that feeds `startup-prof.txt`.
     *
     * Kept separate from [journey] deliberately. With a single `collect` covering the
     * whole journey and `includeInStartupProfile = true`, the startup profile came out
     * byte-identical to the baseline profile — 35,105 rules, the Quran reader included —
     * so install-time dexopt would do the work of the entire journey to open the app.
     * The startup profile should be the lean subset.
     */
    @Test
    fun startup() = rule.collect(
        packageName = PACKAGE,
        maxIterations = 3,
        stableIterations = 2,
        includeInStartupProfile = true,
    ) {
        pressHome()
        startActivityAndWait()
        dismissOnboarding()
    }

    /**
     * The journey past start-up: Home, the surah list, and the reader scroll — the
     * interaction §2.5 named as janky on first run.
     */
    @Test
    fun journey() = rule.collect(
        packageName = PACKAGE,
        // The plugin's own guidance: enough iterations for the profile to stabilise,
        // few enough that a run stays minutes rather than tens of minutes.
        maxIterations = 3,
        stableIterations = 2,
        includeInStartupProfile = false,
    ) {
        pressHome()
        startActivityAndWait()

        dismissOnboarding()
        openQuranAndScroll()
    }

    /**
     * Skip onboarding, answering any permission dialog it raises.
     *
     * Permissions are denied rather than granted: granting location makes the app wait
     * on a fix, and a profile should record the screens rendering, not a network round
     * trip. The prayer-time screens still compose with a fallback location.
     */
    private fun MacrobenchmarkScope.dismissOnboarding() {
        repeat(MAX_ONBOARDING_STEPS) {
            dismissPermissionDialog()

            val skip = device.findObject(By.text(SKIP)) ?: device.findObject(By.textContains(SKIP))
            if (skip != null) {
                skip.click()
                device.waitForIdle()
                return@repeat
            }

            // No Skip on this step — advance instead. Get Started is the last one, so
            // once it is clicked onboarding is done.
            val advance = device.findObject(By.text(GET_STARTED))
                ?: device.findObject(By.text(NEXT))
                ?: return
            advance.click()
            device.waitForIdle()
        }

        dismissPermissionDialog()
        device.waitForIdle()
    }

    /** System permission dialogs are not the app's package, so they need matching by text. */
    private fun MacrobenchmarkScope.dismissPermissionDialog() {
        for (label in PERMISSION_DENY_LABELS) {
            val button = device.findObject(By.text(label)) ?: continue
            button.click()
            device.waitForIdle()
        }
    }

    private fun MacrobenchmarkScope.openQuranAndScroll() {
        // Navigating by label rather than resource id: the bottom bar is Compose, so
        // there are no view ids to match on, and the labels are the strings a user reads.
        val quran = device.findObject(By.descContains(QURAN))
            ?: device.findObject(By.textContains(QURAN))
            ?: return
        quran.click()
        device.waitForIdle()

        // Scroll the surah list first, so the list is profiled and not only the reader.
        scrollOnce(Direction.DOWN)
        scrollOnce(Direction.UP)

        // Open a surah, then scroll the reader — the interaction §2.5 named.
        val surah = device.findObject(By.clickable(true)) ?: return
        surah.click()
        device.wait(Until.hasObject(SCROLLABLE), TIMEOUT_MS)

        repeat(READER_SCROLLS) { scrollOnce(Direction.DOWN) }
    }

    /**
     * Find the scrollable and scroll it once, tolerating a recomposition in between.
     *
     * Two things are deliberate. The object is re-found on **every** call rather than
     * held across scrolls: this is Compose, so a scroll can recompose the subtree and
     * recycle the node, and reusing the handle throws [StaleObjectException] — which is
     * exactly how the first version of this file failed, on the reader.
     *
     * And the gesture margin comes from the display, not from `visibleBounds`, because
     * reading the object's bounds is itself a node access that can throw. The margin
     * matters: UiAutomator's default can start a swipe inside the system gesture zone,
     * where the system consumes it instead of the app — the scroll silently does nothing
     * and the profile is short of exactly the frames it was meant to capture.
     */
    private fun MacrobenchmarkScope.scrollOnce(direction: Direction) {
        device.wait(Until.hasObject(SCROLLABLE), TIMEOUT_MS)
        runCatching {
            val target = device.findObject(SCROLLABLE) ?: return
            target.setGestureMargin(device.displayWidth / MARGIN_DIVISOR)
            target.scroll(direction, SCROLL_PERCENT)
        }
        device.waitForIdle()
    }

    private companion object {
        const val PACKAGE = "com.arshadshah.nimaz"
        const val TIMEOUT_MS = 5_000L
        const val SCROLL_PERCENT = 0.8f
        const val READER_SCROLLS = 3
        const val MARGIN_DIVISOR = 5
        const val MAX_ONBOARDING_STEPS = 12

        // Must match res/values/strings.xml: onboarding_skip, onboarding_next,
        // onboarding_get_started. The generator runs against the default locale.
        const val SKIP = "Skip"
        const val NEXT = "Next"
        const val GET_STARTED = "Get Started"
        const val QURAN = "Quran"

        val SCROLLABLE: BySelector = By.scrollable(true)

        val PERMISSION_DENY_LABELS = listOf(
            "Don’t allow",
            "Don't allow",
            "Deny",
            "No thanks",
        )
    }
}
