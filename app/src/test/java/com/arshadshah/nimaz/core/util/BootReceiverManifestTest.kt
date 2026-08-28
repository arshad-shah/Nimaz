package com.arshadshah.nimaz.core.util

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

/**
 * That the manifest agrees with the receivers about which broadcasts reach them.
 *
 * **This is the defect the `BootReceiver` split was written around.** Three actions —
 * `ACTION_LOCKED_BOOT_COMPLETED` and the two `QUICKBOOT_POWERON` spellings — had a branch in
 * `onReceive` and no `<intent-filter>` anywhere. They read as supported, they were covered by a
 * unit test that called `onReceive` directly, and on a real device they never arrived once. A
 * receiver's contract is half Kotlin and half XML, and nothing in the build checks that the two
 * halves say the same thing.
 *
 * `:app` unit tests run with the module directory as their working directory, so the manifest is
 * a plain relative path from here.
 */
class BootReceiverManifestTest {

    private val manifest: String = File(MANIFEST).also {
        check(it.isFile) { "manifest not found at ${it.absolutePath}" }
    }.readText()

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
    fun `PrayerAlarmReceiver is declared, or every alarm resolves nothing`() {
        // A receiver reached only by explicit PendingIntent still has to be in the manifest.
        // Without the declaration every alarm fires into a component the system cannot resolve,
        // which fails silently — no crash, no log, no notification.
        assertThat(manifest).contains("android:name=\".core.util.PrayerAlarmReceiver\"")
    }

    @Test
    fun `PrayerAlarmReceiver declares no filter, because nothing broadcasts to it implicitly`() {
        assertThat(actionsIn(receiverBlock("PrayerAlarmReceiver"))).isEmpty()
    }

    /**
     * The `<receiver …>…</receiver>` (or self-closing) block for `.core.util.[name]`.
     *
     * Bounded at the next sibling element rather than at the first `/>`, so that a self-closing
     * declaration and one with filters are read the same way — a `<receiver>` with filters ends
     * with `/>` on its *first inner* `<action>`, and stopping there would report a block with no
     * actions in it. Running to the end of the file instead is the other trap: the FCM service
     * below carries an `<action>` of its own, so `PrayerAlarmReceiver` would look like it filters
     * `MESSAGING_EVENT`.
     */
    private fun receiverBlock(name: String): String {
        val marker = manifest.indexOf("android:name=\".core.util.$name\"")
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
        const val MANIFEST = "src/main/AndroidManifest.xml"

        /** Anything that can follow a `<receiver>` inside `<application>`. */
        val SIBLINGS = listOf("<receiver", "<service", "<provider", "<activity", "</application>")
        val ACTION = Regex("""<action android:name="([^"]+)" */>""")
    }
}
