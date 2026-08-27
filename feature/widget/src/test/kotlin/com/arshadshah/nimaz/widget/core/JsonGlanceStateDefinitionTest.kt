package com.arshadshah.nimaz.widget.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.arshadshah.nimaz.widget.hijridate.HijriDateData
import com.arshadshah.nimaz.widget.hijridate.HijriDateStateDefinition
import com.arshadshah.nimaz.widget.hijridate.HijriDateWidgetState
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * How a widget's state survives between worker runs.
 *
 * The lenient decode and the corruption handler are not style choices. With the strict default,
 * the first release to drop or rename a field in a widget's state made every subsequent read
 * throw, and with nothing installed to handle it the widget was stuck on its error frame
 * permanently for everyone who had it placed — only clearing app data brought it back.
 */
@RunWith(RobolectricTestRunner::class)
class JsonGlanceStateDefinitionTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    /** A state shaped like a widget's, on its own file so these tests do not share one store. */
    @Serializable
    data class ProbeState(val label: String = "", val count: Int = 0)

    private object ProbeDefinition : JsonGlanceStateDefinition<ProbeState>(
        fileName = "probe_widget_state",
        serializer = ProbeState.serializer(),
        defaultValue = ProbeState("default"),
        dataLabel = "Probe",
    )

    @Test
    fun `state written by one run is read back by the next`() = runBlocking {
        val store = ProbeDefinition.getDataStore(context, "any")

        store.updateData { ProbeState("Shawwal", 9) }

        assertThat(store.data.first()).isEqualTo(ProbeState("Shawwal", 9))
    }

    /** One DataStore per file, process-wide — the `by dataStore` delegate guaranteed this. */
    @Test
    fun `the same file name always yields the same store whatever key is asked for`() =
        runBlocking {
            assertThat(ProbeDefinition.getDataStore(context, "a"))
                .isSameInstanceAs(ProbeDefinition.getDataStore(context, "b"))
        }

    @Test
    fun `the state file is named after the definition, under the datastore directory`() {
        val file = ProbeDefinition.getLocation(context, "ignored")

        assertThat(file.name).isEqualTo("probe_widget_state")
        assertThat(file.path).contains("datastore")
    }

    /**
     * Unreadable state costs one refresh, not the widget: the read falls back to the default and
     * the next worker run fills it in. Before the handler was installed this threw on *every*
     * read, for ever.
     */
    @Test
    fun `a truncated state file falls back to the default instead of throwing for ever`() =
        runBlocking {
            val definition = object : JsonGlanceStateDefinition<ProbeState>(
                fileName = "corrupt_probe_state",
                serializer = ProbeState.serializer(),
                defaultValue = ProbeState("recovered"),
                dataLabel = "CorruptProbe",
            ) {}
            definition.getLocation(context, "any").apply {
                parentFile?.mkdirs()
                writeText("""{"label":"half-writ""")
            }

            val store = definition.getDataStore(context, "any")

            assertThat(store.data.first()).isEqualTo(ProbeState("recovered"))
            // And the store is usable again afterwards, not poisoned.
            store.updateData { ProbeState("fresh", 1) }
            assertThat(store.data.first()).isEqualTo(ProbeState("fresh", 1))
        }

    /**
     * The forward-compatibility contract: a payload carrying a field this release has never heard
     * of still decodes, because the alternative is a widget that never recovers.
     */
    @Test
    fun `a field written by a newer release is ignored rather than treated as corruption`() =
        runBlocking {
            val definition = object : JsonGlanceStateDefinition<ProbeState>(
                fileName = "future_probe_state",
                serializer = ProbeState.serializer(),
                defaultValue = ProbeState("default"),
                dataLabel = "FutureProbe",
            ) {}
            definition.getLocation(context, "any").apply {
                parentFile?.mkdirs()
                writeText("""{"label":"Rajab","count":3,"fieldFromTheFuture":true}""")
            }

            assertThat(definition.getDataStore(context, "any").data.first())
                .isEqualTo(ProbeState("Rajab", 3))
        }

    /** Each shipped widget declares its own file, so no two can overwrite each other's state. */
    @Test
    fun `the shipped hijri-date definition round-trips its real state type`() = runBlocking {
        val store = HijriDateStateDefinition.getDataStore(context, "any")
        val state = HijriDateWidgetState.Success(HijriDateData(hijriDay = 3, hijriMonth = "Rajab"))

        store.updateData { state }

        assertThat(store.data.first()).isEqualTo(state)
        assertThat(HijriDateStateDefinition.getLocation(context, "any").name)
            .isEqualTo("hijri_date_widget")
    }
}
