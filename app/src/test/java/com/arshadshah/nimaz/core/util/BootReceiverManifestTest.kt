package com.arshadshah.nimaz.core.util

import com.google.common.truth.Truth.assertThat
import java.io.File
import java.util.Properties
import org.junit.Test

/**
 * That the manifest agrees with the receivers about which broadcasts reach them.
 *
 * **This is the defect the `BootReceiver` split was written around.** Three actions —
 * `ACTION_LOCKED_BOOT_COMPLETED` and the two `QUICKBOOT_POWERON` spellings — had a branch in
 * `onReceive` and no `<intent-filter>` anywhere. They read as supported, they were covered by a
 * unit test that called `onReceive` directly, and on a real device they never arrived once. A
 * receiver's contract is half Kotlin and half XML, and nothing in the build checked that the two
 * halves say the same thing.
 *
 * **It reads the merged manifest, not a source one**, and that stopped being a nicety when
 * `PrayerAlarmReceiver` moved to `:core:notifications`: the two receivers are declared in two
 * different modules now, and what ships is the merge. AGP hands a unit test the path in
 * `com/android/tools/test_config.properties` — the same file Robolectric reads — so this checks
 * the artefact rather than an input to it.
 */
class BootReceiverManifestTest {

    private val manifest: String = mergedManifest()

    @Test
    fun `every action BootReceiver handles has a filter that can deliver it`() {
        val declared = actionsIn(receiverBlock("BootReceiver"))

        assertThat(declared).containsAtLeastElementsIn(BootReceiver.TRIGGERS.keys)
    }

    @Test
    fun `BootReceiver filters nothing it does not handle`() {
        // The other direction, and the one that regressed: the five alarm actions were filtered
        // here long after they were being delivered by explicit PendingIntent, so an implicit
        // broadcast of any of them would have reached a receiver that no longer answers it.
        val declared = actionsIn(receiverBlock("BootReceiver"))

        assertThat(BootReceiver.TRIGGERS.keys).containsAtLeastElementsIn(declared)
    }

    @Test
    fun `PrayerAlarmReceiver reaches the merged manifest from its own module`() {
        // A receiver reached only by explicit PendingIntent still has to be in the manifest.
        // Without the declaration every alarm fires into a component the system cannot resolve,
        // which fails silently — no crash, no log, no notification. It is `:core:notifications`
        // that declares it now, so this also checks that the merge brought it across.
        assertThat(manifest).contains("com.arshadshah.nimaz.core.util.PrayerAlarmReceiver")
    }

    @Test
    fun `PrayerAlarmReceiver declares no filter, because nothing broadcasts to it implicitly`() {
        assertThat(actionsIn(receiverBlock("PrayerAlarmReceiver"))).isEmpty()
    }

    @Test
    fun `the alarm permissions survive the merge from core notifications`() {
        // They are declared in `:core:notifications`, not here, so that the module is
        // self-describing. Losing them is an app whose notifications never arrive.
        listOf("SCHEDULE_EXACT_ALARM", "USE_EXACT_ALARM", "WAKE_LOCK", "RECEIVE_BOOT_COMPLETED")
            .forEach { assertThat(manifest).contains("android.permission.$it") }
    }

    /**
     * The `<receiver …>…</receiver>` (or self-closing) block for `[name]`.
     *
     * Bounded at the next sibling element rather than at the first `/>`, so that a self-closing
     * declaration and one with filters are read the same way — a `<receiver>` with filters ends
     * with `/>` on its *first inner* `<action>`, and stopping there would report a block with no
     * actions in it. Running to the end of the file instead is the other trap: the FCM service
     * carries an `<action>` of its own, so a receiver near it would look like it filters
     * `MESSAGING_EVENT`.
     */
    private fun receiverBlock(name: String): String {
        val marker = manifest.indexOf("$RECEIVER_PACKAGE$name\"")
        assertThat(marker).isGreaterThan(0)
        val open = manifest.lastIndexOf("<receiver", marker)
        val end = SIBLINGS
            .mapNotNull { tag -> manifest.indexOf(tag, marker).takeIf { it >= 0 } }
            .minOrNull()
            ?: manifest.length
        return manifest.substring(open, end)
    }

    private fun actionsIn(block: String): List<String> =
        ACTION.findAll(block).map { it.groupValues[1] }.toList()

    private companion object {
        /**
         * The merged manifest AGP built for this variant.
         *
         * `:app` unit tests run with the module directory as their working directory and the
         * property is recorded relative to it. Both halves are asserted rather than defaulted: a
         * missing config file or a missing manifest is a broken harness, and silently reading
         * nothing is the failure mode every scan in this repo carries a floor against.
         */
        fun mergedManifest(): String {
            val stream = checkNotNull(
                BootReceiverManifestTest::class.java.classLoader
                    ?.getResourceAsStream("com/android/tools/test_config.properties")
            ) { "AGP's unit-test config is not on the classpath" }
            val path = stream.use { Properties().apply { load(it) } }
                .getProperty("android_merged_manifest")
            checkNotNull(path) { "android_merged_manifest is not recorded in test_config.properties" }
            val file = File(path)
            check(file.isFile) { "merged manifest not found at ${file.absolutePath}" }
            return file.readText()
        }

        /** The manifest merger rewrites every `android:name` to its fully qualified form. */
        const val RECEIVER_PACKAGE = "com.arshadshah.nimaz.core.util."

        /** Anything that can follow a `<receiver>` inside `<application>`. */
        val SIBLINGS = listOf("<receiver", "<service", "<provider", "<activity", "</application>")

        val ACTION = Regex("""<action android:name="([^"]+)"\s*/>""")
    }
}
