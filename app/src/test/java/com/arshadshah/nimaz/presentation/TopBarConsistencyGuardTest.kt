package com.arshadshah.nimaz.presentation

import com.arshadshah.nimaz.testing.PresentationSourceRoots
import java.io.File
import org.junit.Test

/**
 * Every screen's top bar is `NimazTopAppBar` / `NimazBackTopAppBar` — the frosted-pill bar in
 * `organisms/TopAppBar.kt` — so it looks and behaves the same everywhere.
 *
 * It drifted twice in the same way. Three readers (Quran, Tafseer, Qaida) used Material's raw
 * `TopAppBar`, an opaque band nothing else in the app has; and six Quran screens passed their own
 * back arrow into `NimazTopAppBar`'s `navigationIcon`, re-building `NimazBackTopAppBar` six times
 * with nothing to keep the copies in step. Learn to Pray then hand-rolled a third back button on
 * its posture stage. Each looked fine alone; together they were three top-bar designs.
 *
 * The one deliberate exception is the Prayer Times sky hero (`PrayerSkyScene`), whose pills are
 * real glass frosting the animated sky — the same pill language, drawn against a live backdrop
 * the shared bar cannot see. It uses neither pattern below, so it needs no allowance here.
 */
class TopBarConsistencyGuardTest {

    private val sharedBar = "TopAppBar.kt"

    private fun sources(): Sequence<File> {
        PresentationSourceRoots.assertAllExist(PresentationSourceRoots.ALL)
        return PresentationSourceRoots.ALL.asSequence()
            .flatMap { File(it).walkTopDown() }
            .filter { it.isFile && it.extension == "kt" && it.name != sharedBar }
    }

    @Test
    fun `no screen builds its top bar from Material's raw app bars`() {
        val raw = Regex("""^import androidx\.compose\.material3\.(TopAppBar|CenterAlignedTopAppBar|MediumTopAppBar|LargeTopAppBar)\s*$""", RegexOption.MULTILINE)
        val offenders = sources().mapNotNull { file ->
            raw.find(file.readText())?.let { "${file.name}: ${it.value.trim()}" }
        }.toList()

        assert(offenders.isEmpty()) {
            "Use NimazTopAppBar / NimazBackTopAppBar instead of Material's raw app bars:\n" +
                offenders.joinToString("\n")
        }
    }

    @Test
    fun `no screen hand-rolls a back arrow into a top bar`() {
        // `navigationIcon = { … ArrowBack … }` is NimazBackTopAppBar written out by hand.
        val handRolledBack = Regex("""navigationIcon\s*=\s*\{[^}]{0,400}?ArrowBack""", RegexOption.DOT_MATCHES_ALL)
        val offenders = sources().mapNotNull { file ->
            val text = file.readText()
            handRolledBack.find(text)?.let { match ->
                val line = text.substring(0, match.range.first).count { it == '\n' } + 1
                "${file.name}:$line"
            }
        }.toList()

        assert(offenders.isEmpty()) {
            "Use NimazBackTopAppBar(onBackClick = …) rather than a back arrow in navigationIcon:\n" +
                offenders.joinToString("\n")
        }
    }
}
