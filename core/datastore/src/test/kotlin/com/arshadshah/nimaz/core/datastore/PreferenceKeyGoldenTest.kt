package com.arshadshah.nimaz.core.datastore

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import org.junit.Test

/**
 * Every preference key this app stores, pinned to a golden file.
 *
 * ## What this is protecting
 *
 * A DataStore key **is** the storage format. Rename one and the old key is simply never read
 * again: the user's setting is gone, silently and permanently, on the next launch after they
 * update. No crash, no migration failure, no log line — the preference reads back as its default
 * and looks like the user never set it. There is no way to notice this in review, and no way to
 * recover it afterwards.
 *
 * ## Why the existing test was not enough
 *
 * `PreferenceCodecTest` already scrapes the declarations out of `PreferencesDataStore.kt` and
 * asserts `PreferenceCodec.typeOf(name)` agrees, so a rename in *one* of those two files fails
 * today. The gap is a rename in **both** — which is precisely what an IDE "rename symbol" does,
 * because `PreferenceCodec.TYPES` is the second copy and it updates in lockstep. Two files
 * agreeing with each other is not evidence that either agrees with what is on disk.
 *
 * The golden file is a third copy that nothing automatic updates, which is the whole point.
 *
 * ## The asymmetry that makes this reviewable
 *
 * **Additions regenerate freely; removals require an explicit retirement entry.** A new key is
 * harmless — nothing has it stored yet. A removed key is a reset setting for every existing user,
 * so it has to be stated as a deliberate act in `retired-preference-keys.txt` rather than
 * absorbed by regenerating the golden and moving on.
 *
 * ## The templates are the interesting part
 *
 * Six of the 106 keys are composed at runtime — `worship_${'$'}{key}_enabled`,
 * `${'$'}{key}_reminder_minutes` and so on — and are stored here in their literal template form,
 * exactly as they appear in source. `PreferencesDataStore.kt` carries a comment specifically
 * preserving that shape for a scanner. A golden built from *resolved* keys would need to
 * enumerate every worship type and every prayer, would churn whenever one was added, and would
 * miss a template rename entirely.
 */
class PreferenceKeyGoldenTest {

    private companion object {
        /**
         * Relative to the module directory, which is the CWD for a module's unit tests.
         * `src/main/kotlin` rather than `src/main/java` — that is the source root a `:core:*`
         * module uses.
         */
        const val PREFERENCES_SOURCE =
            "src/main/kotlin/com/arshadshah/nimaz/core/datastore/PreferencesDataStore.kt"

        const val GOLDEN_RESOURCE = "/preference-keys.golden"

        /**
         * A floor, so that a scan finding nothing fails loudly instead of passing over an empty
         * set — the same shape as `check_docs.py`'s scan floors (#553). Deliberately well below
         * the real count so an intentional retirement does not have to touch it.
         */
        const val MINIMUM_KEYS = 100

        val KEY_DECLARATION = Regex("""(\w+)PreferencesKey\("([^"]+)"\)""")
    }

    private fun declaredKeys(): Set<String> {
        val source = File(PREFERENCES_SOURCE)
        assertWithMessage(
            "PreferencesDataStore.kt not found at $PREFERENCES_SOURCE. This test reads the key " +
                "declarations straight from source, so a wrong path finds zero keys — and " +
                "without the floor below it would then pass having checked nothing."
        ).that(source.isFile).isTrue()

        return KEY_DECLARATION.findAll(source.readText())
            .map { "${it.groupValues[2]}\t${it.groupValues[1]}" }
            .toSet()
    }

    private fun goldenKeys(): Set<String> {
        val stream = javaClass.getResourceAsStream(GOLDEN_RESOURCE)
        assertWithMessage(
            "$GOLDEN_RESOURCE is missing. It is the record of what this app has stored on real " +
                "devices; deleting it does not make the check pass, it removes the check."
        ).that(stream).isNotNull()

        return requireNotNull(stream).bufferedReader().use { it.readLines() }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .toSet()
    }

    private fun retiredKeys(): Set<String> {
        val file = File("src/test/resources/retired-preference-keys.txt")
        if (!file.isFile) return emptySet()
        return file.readLines()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .map { it.substringBefore('\t').trim() }
            .toSet()
    }

    @Test
    fun `the golden file is not vacuous`() {
        // Guarding the guard: an empty or truncated golden would make every comparison below
        // trivially true.
        assertThat(goldenKeys().size).isAtLeast(MINIMUM_KEYS)
        assertThat(declaredKeys().size).isAtLeast(MINIMUM_KEYS)
    }

    @Test
    fun `no stored preference key has been removed or renamed`() {
        val removed = (goldenKeys() - declaredKeys())
            .filterNot { it.substringBefore('\t') in retiredKeys() }
            .sorted()

        assertWithMessage(
            "These preference keys are in the golden file but no longer declared:\n" +
                removed.joinToString("\n") { "  $it" } +
                "\n\nA removed key is a reset setting for every existing user — silently, on " +
                "their next launch, with no way to recover it. If a key was *renamed*, the old " +
                "name has to keep being read (or migrated) rather than disappearing. If it was " +
                "genuinely retired, add it to src/test/resources/retired-preference-keys.txt " +
                "with the versionCode and a one-line reason, then regenerate the golden."
        ).that(removed).isEmpty()
    }

    @Test
    fun `the golden file lists every key the app declares`() {
        // The other direction, and the cheap half: a new key is harmless to users but should
        // still be recorded, so that its later removal is caught by the test above.
        val unrecorded = (declaredKeys() - goldenKeys()).sorted()

        assertWithMessage(
            "These declared preference keys are not in the golden file:\n" +
                unrecorded.joinToString("\n") { "  $it" } +
                "\n\nAdding keys is safe — regenerate the golden. It only exists so that a " +
                "future *removal* is visible."
        ).that(unrecorded).isEmpty()
    }

    @Test
    fun `the runtime-composed keys keep their template shape`() {
        // These are stored in the golden in literal `${'$'}{key}` form. If someone resolves them
        // at declaration time — or renames the template — the golden diff is what shows it, and
        // that only works while the shape is preserved. `PreferencesDataStore.kt` has a comment
        // to the same effect.
        val templates = goldenKeys()
            .map { it.substringBefore('\t') }
            .filter { "\${key}" in it }

        assertWithMessage(
            "the runtime-composed keys are gone from the golden — either they were resolved at " +
                "declaration time, or the scrape regex stopped matching them"
        ).that(templates).containsExactly(
            "\${key}_alert_style",
            "\${key}_reminder_enabled",
            "\${key}_reminder_minutes",
            "worship_\${key}_enabled",
            "worship_\${key}_mode",
            "worship_\${key}_offset",
        )
    }

    @Test
    fun `every DataStore file name is pinned`() {
        // Three named stores, and the name *is* the filename on disk. Renaming one orphans
        // everything in it, exactly like renaming a key but for all of them at once. SUB-06 in
        // check_docs.py scans for these too, but a doc scan and a test failing for the same
        // reason is the point — one of them runs without a Python toolchain.
        val sources = listOf(
            File(PREFERENCES_SOURCE),
            File("src/main/kotlin/com/arshadshah/nimaz/core/datastore/AnnouncementLocalDataSource.kt"),
            File("src/main/kotlin/com/arshadshah/nimaz/core/datastore/DeviceIdProvider.kt"),
        )
        sources.forEach { assertWithMessage(it.path).that(it.isFile).isTrue() }

        val names = sources.flatMap { file ->
            Regex("""["'](nimaz_[a-z_]+)["']""").findAll(file.readText())
                .map { it.groupValues[1] }
        }.toSet()

        assertThat(names).containsAtLeast(
            "nimaz_preferences",
            "nimaz_announcements",
            "nimaz_ai_device",
        )
    }
}
