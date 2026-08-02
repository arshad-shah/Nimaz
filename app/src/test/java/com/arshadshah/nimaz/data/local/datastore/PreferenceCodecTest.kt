package com.arshadshah.nimaz.data.local.datastore

import androidx.datastore.preferences.core.Preferences
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

/**
 * Preferences must come back from a sync with the **type** they were stored under.
 *
 * `exportAllPreferences` flattens every value with `toString()`, so the wire carries
 * `Map<String, String>` and the type is gone. `importPreferences` used to guess it back from the
 * *shape of the value* and *substrings of the key name* — "true"/"false" means Boolean, numeric
 * plus a key containing "offset"/"minutes"/"adjustment"/"location_id" means Int, and everything
 * else means String.
 *
 * DataStore keys are typed, and reading one back at the wrong type throws. So every key the
 * heuristic missed was not merely wrong, it was a **crash on next read** after any sync:
 *
 * | key | stored | guessed |
 * |---|---|---|
 * | `tasbih_preset_seed_version` | Int | String |
 * | `content_patch_version` | Int | String |
 * | `ai_consent_timestamp` | Long | String |
 * | `tasbih_selected_preset` | Long | String |
 * | `current_location_id` | Long | **Int** |
 * | `tasbih_favorites` | Set&lt;String&gt; | String |
 *
 * `tasbih_preset_seed_version` is read with `.first()` in `TasbihViewModel`'s init, and
 * `current_location_id` resolves the active location for prayer times.
 *
 * The fix is to stop guessing: [PreferenceCodec] holds the declared type of every key.
 */
class PreferenceCodecTest {

    @Test
    fun `the six keys the heuristic mistyped now round-trip`() {
        assertRoundTrip("tasbih_preset_seed_version", 3)
        assertRoundTrip("content_patch_version", 12)
        assertRoundTrip("ai_consent_timestamp", 1_762_000_000_000L)
        assertRoundTrip("tasbih_selected_preset", 42L)
        assertRoundTrip("current_location_id", 7L)
        assertRoundTrip("tasbih_favorites", setOf("11", "12"))
    }

    @Test
    fun `every declared type round-trips`() {
        assertRoundTrip("dynamic_color", true)
        assertRoundTrip("theme_mode", "dark")
        assertRoundTrip("latitude", 53.349805)
        assertRoundTrip("quran_arabic_font_size", 28f)
    }

    @Test
    fun `an empty string set round-trips as an empty set, not a set holding one blank`() {
        assertRoundTrip("tasbih_favorites", emptySet<String>())
    }

    @Test
    fun `a string whose value reads as a boolean stays a string`() {
        // The old heuristic keyed off the value: any "true"/"false" became a Boolean, whatever
        // the key was actually declared as.
        val (key, value) = PreferenceCodec.decode("theme_mode", "true")!!
        assertThat(key.name).isEqualTo("theme_mode")
        assertThat(value).isInstanceOf(String::class.java)
        assertThat(value).isEqualTo("true")
    }

    @Test
    fun `onboarding is never imported`() {
        // Importing it would drop the receiver into, or out of, onboarding on someone else's say-so.
        assertThat(PreferenceCodec.decode("onboarding_completed", "true")).isNull()
    }

    @Test
    fun `runtime-composed worship keys are typed by shape`() {
        // Built per WorshipReminderType, so they cannot be listed by name.
        assertRoundTrip("worship_tahajjud_enabled", true)
        assertRoundTrip("worship_iftar_offset", 30)
        assertRoundTrip("worship_witr_mode", "before_fajr")
    }

    @Test
    fun `an unknown key from a newer sender is kept as a string rather than dropped`() {
        val (key, value) = PreferenceCodec.decode("some_future_setting", "hello")!!
        assertThat(key.name).isEqualTo("some_future_setting")
        assertThat(value).isEqualTo("hello")
    }

    @Test
    fun `the registry covers every declared key`() {
        // A new preference must register its type, or it silently reverts to the guessing
        // behaviour this class exists to remove. Reads the declarations straight from source,
        // in the shape of WidgetGlyphGuardTest; runs from the module dir.
        val source = File(
            "src/main/java/com/arshadshah/nimaz/data/local/datastore/PreferencesDataStore.kt"
        )
        assertThat(source.isFile).isTrue()

        val declared = Regex("""(\w+)PreferencesKey\("([^"]+)"\)""")
            .findAll(source.readText())
            .associate { it.groupValues[2] to it.groupValues[1] }
            // Keys composed at runtime are matched by shape, so stand a real worship
            // type in for the template before looking them up.
            .mapKeys { (name, _) -> name.replace(TEMPLATE, "tahajjud") }
        assertThat(declared).isNotEmpty()

        val expected = mapOf(
            "boolean" to PrefType.BOOLEAN, "int" to PrefType.INT, "long" to PrefType.LONG,
            "float" to PrefType.FLOAT, "double" to PrefType.DOUBLE,
            "string" to PrefType.STRING, "stringSet" to PrefType.STRING_SET,
        )
        val wrong = declared.mapNotNull { (name, kind) ->
            val want = expected.getValue(kind)
            val got = PreferenceCodec.typeOf(name)
            if (got != want) "$name: declared $want, registry says ${got ?: "missing"}" else null
        }
        assertThat(wrong).isEmpty()
    }

    /** The literal Kotlin template that appears in the declaration, e.g. worship_${'$'}{key}_mode. */
    private val TEMPLATE = "${'$'}" + "{key}"

    private fun assertRoundTrip(keyName: String, value: Any) {
        val onTheWire = PreferenceCodec.encode(value)
        val decoded = PreferenceCodec.decode(keyName, onTheWire)
        assertThat(decoded).isNotNull()
        val (key, decodedValue) = decoded!!
        assertThat(key).isInstanceOf(Preferences.Key::class.java)
        assertThat(key.name).isEqualTo(keyName)
        assertThat(decodedValue).isEqualTo(value)
    }
}
