package com.arshadshah.nimaz.presentation

import com.arshadshah.nimaz.testing.PresentationSourceRoots
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Regression guard: composables collect ViewModel state **lifecycle-aware**.
 *
 * `collectAsState()` subscribes for as long as the composition lives, which on Android means
 * it keeps collecting while the app is in the background — the screen is not visible, but its
 * flows still run, still wake Room, and still recompose state nobody can see. The app had both
 * spellings in the tree (roughly forty screens on `collectAsState`, eleven on the lifecycle-aware
 * form), so the same class of screen behaved differently depending on which one its author
 * happened to copy.
 *
 * `collectAsStateWithLifecycle()` stops at `STOPPED` and resubscribes at `STARTED`. Every
 * ViewModel here exposes `StateFlow`, which replays its current value to a new collector, so
 * nothing can be missed across the pause. It also makes `SharingStarted.WhileSubscribed()`
 * mean what it says: with `collectAsState` the subscriber count never drops in the background,
 * so those upstreams never actually stopped.
 *
 * **If you ever expose a replay-less `SharedFlow` or `Channel` for one-shot events**, this rule
 * does not fit it — an event emitted while stopped would be dropped. Collect those in a
 * `repeatOnLifecycle` block instead, and add the file to [ALLOWED] with a note saying why.
 *
 * **It scans every module's presentation sources, not `:app`'s.** It used to name
 * `app/src/main/java/...presentation` directly, so from PR 10 of #551 onwards it was checking a
 * shrinking fraction of the app without saying so - and when `:feature:home` took the last of
 * that directory it failed outright, which is the only reason anybody noticed. Reading
 * [PresentationSourceRoots] is what stops the next module move quietly narrowing it again.
 */
class StateCollectionGuardTest {

    /** Files exempt from the rule, each for a stated reason. */
    private val ALLOWED = emptySet<String>()

    @Test
    fun `presentation sources collect state lifecycle-aware`() {
        PresentationSourceRoots.assertAllExist()
        val files = PresentationSourceRoots.sources()
        // A floor, not a filter: a mis-rooted scan finds nothing and would otherwise pass.
        assertThat(files.size).isAtLeast(MINIMUM_FILES)

        val offenders = mutableListOf<String>()
        files.forEach { file ->
            if (file.name in ALLOWED) return@forEach
            file.readLines().forEachIndexed { i, line ->
                // `collectAsStateWithLifecycle(` shares the prefix, so match the call form
                // exactly: `collectAsState(` — with or without an initial value argument.
                if (Regex("""\bcollectAsState\s*\(""").containsMatchIn(line)) {
                    offenders += "${file.name}:${i + 1}  ${line.trim()}"
                }
            }
        }

        assert(offenders.isEmpty()) {
            "Use collectAsStateWithLifecycle() instead of collectAsState() " +
                "(${offenders.size} site(s)):\n" + offenders.joinToString("\n")
        }
    }

    private companion object {
        /** Presentation sources across every module - several hundred. See the class KDoc. */
        const val MINIMUM_FILES = 100
    }
}
