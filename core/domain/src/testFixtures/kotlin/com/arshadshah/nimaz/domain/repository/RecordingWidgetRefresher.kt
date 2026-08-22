package com.arshadshah.nimaz.domain.repository

/**
 * A [WidgetRefresher] that counts calls instead of touching WorkManager.
 *
 * A test fixture because both sides of the seam need it: `WidgetSettingsWatcherTest` in
 * `:core:data`, which drives the port, and the ViewModel tests in `:app`. It is the fourth fake
 * to end up here — after `FakeTodayProvider`, `FakeSearchSettings` and `FakeStringProvider` — and
 * the pattern is reliable enough now to expect rather than discover: **a fake of a `:core:domain`
 * port is needed by whichever module implements it and by whichever module drives it**, so it
 * belongs beside the port rather than in either.
 */
class RecordingWidgetRefresher : WidgetRefresher {
    var refreshCount = 0
        private set

    /**
     * Counted apart from [refreshCount]: "the tracker changed" and "everything changed" are
     * different claims, and a test asserting one should not be satisfied by the other.
     */
    var refreshAllCount = 0
        private set

    override fun refreshPrayerTracker() {
        refreshCount++
    }

    override fun refreshAll() {
        refreshAllCount++
    }
}
