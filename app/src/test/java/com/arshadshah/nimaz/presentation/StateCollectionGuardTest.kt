package com.arshadshah.nimaz.presentation

import org.junit.Test
import java.io.File

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
 * Runs from the module dir, so source paths are relative to `app/`.
 */
class StateCollectionGuardTest {

    /** Files exempt from the rule, each for a stated reason. */
    private val ALLOWED = emptySet<String>()

    @Test
    fun `presentation sources collect state lifecycle-aware`() {
        val dir = File("src/main/java/com/arshadshah/nimaz/presentation")
        assert(dir.isDirectory) { "Presentation source dir not found at ${dir.absolutePath}" }

        val offenders = mutableListOf<String>()
        dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
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
}
