package com.arshadshah.nimaz.core.datastore

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

/**
 * "Ask with Proof" is **off on a fresh install**, and every other AI preference starts inert.
 *
 * This is a privacy guarantee, not a product default: the feature sends the user's question text
 * off the device, so a silent flip from `false` to `true` would start doing that without anyone
 * having asked. `docs/ai-ask-with-proof.md` states it and #567 lists *"the feature is off on a
 * fresh install"* as an exit criterion.
 *
 * **Nothing asserted it.** `AskViewModelTest`, `AskViewModelGuardTest` and
 * `SearchSettingsViewModelTest` all touch `aiAskEnabled`, and all three mock the `AiSettings`
 * seam and supply their own value — correct for what they assert, and blind to the default.
 * `preference-keys.golden` records each key and its type, never its default. The whole guarantee
 * was one literal on one line with no test under it.
 *
 * It is asserted at the source rather than by reading a store, because a real
 * `PreferencesDataStore` needs a `Context` and this module's tests are pure JVM. That is not a
 * weaker check: the literal below *is* the guarantee, and it is what a careless edit would change.
 *
 * The whole seam is pinned rather than just the on/off flag, since consent, history and the hint
 * together are the opt-in surface — a consent timestamp defaulting to anything but `0` would read
 * as consent already given.
 */
class AiOptInDefaultsTest {

    @Test
    fun `every AI preference defaults to off`() {
        val source = File(PREFERENCES_DATA_STORE)
        assertThat(source.isFile).isTrue()

        val text = source.readText()

        // A floor first: a renamed file or a changed call shape would otherwise make every
        // assertion below vacuously true by matching nothing.
        assertThat(DEFAULTS.size).isEqualTo(EXPECTED_PREFERENCES)

        val wrong = DEFAULTS.filterNot { (key, expected) ->
            Regex("""preference\(\s*PreferencesKeys\.$key\s*,\s*${Regex.escape(expected)}\s*\)""")
                .containsMatchIn(text)
        }.keys

        assertThat(wrong).isEmpty()
    }

    private companion object {
        const val PREFERENCES_DATA_STORE =
            "src/main/kotlin/com/arshadshah/nimaz/core/datastore/PreferencesDataStore.kt"

        /**
         * The `AiSettings` seam, in declaration order.
         *
         * `AI_ASK_ENABLED` is the one that matters most — it gates the network call — but a
         * `AI_CONSENT_TIMESTAMP` other than `0L` would mean the app believes consent was already
         * given, which reaches the same place by a different route.
         */
        val DEFAULTS = mapOf(
            "AI_ASK_ENABLED" to "false",
            "AI_CONSENT_TIMESTAMP" to "0L",
            "AI_HISTORY_ENABLED" to "false",
            "AI_ASK_HINT_DISMISSED" to "false",
            "AI_QUESTION_HISTORY" to "\"\"",
        )

        /** Adding a preference to `AiSettings` must mean deciding its default here. */
        const val EXPECTED_PREFERENCES = 5
    }
}
