package com.arshadshah.nimaz.data.local.content

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The three facts that decide whether a content release reaches a device.
 *
 * All three are read at a moment when nothing can wait — `installedArtifact` while
 * `DatabaseModule` is building the database, before Room opens the file — so they live in
 * SharedPreferences rather than DataStore. That makes their *defaults* the interesting part: a
 * fresh install has to read as "nothing installed", "import not done" and "no deferrals", and a
 * second store over the same file has to see what the first one wrote, because these values
 * outlive the process that recorded them.
 */
@RunWith(RobolectricTestRunner::class)
class SharedPreferencesContentArtifactStoreTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private lateinit var store: ContentArtifactStore

    @Before
    fun setUp() {
        context.getSharedPreferences("nimaz_content_artifact", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        store = SharedPreferencesContentArtifactStore(context)
    }

    @Test
    fun `a fresh install has installed nothing and imported nothing`() {
        assertThat(store.installedArtifact()).isNull()
        assertThat(store.legacyImportComplete()).isFalse()
        assertThat(store.consecutiveDeferrals()).isEqualTo(0)
    }

    @Test
    fun `the recorded artifact survives a new store over the same file`() {
        store.setInstalledArtifact("sha-v8")

        // Standing in for the next launch: the whole point of the write is that it outlives the
        // process, which is why it is a `commit` rather than an `apply`.
        assertThat(SharedPreferencesContentArtifactStore(context).installedArtifact())
            .isEqualTo("sha-v8")
    }

    @Test
    fun `a later release replaces the artifact recorded before it`() {
        store.setInstalledArtifact("sha-v7")
        store.setInstalledArtifact("sha-v8")

        assertThat(store.installedArtifact()).isEqualTo("sha-v8")
    }

    @Test
    fun `the legacy import is recorded once and stays recorded`() {
        store.setLegacyImportComplete()

        assertThat(store.legacyImportComplete()).isTrue()
        assertThat(SharedPreferencesContentArtifactStore(context).legacyImportComplete()).isTrue()

        store.setLegacyImportComplete()
        assertThat(store.legacyImportComplete()).isTrue()
    }

    @Test
    fun `deferrals accumulate so a stuck device can be recognised`() {
        repeat(3) { store.recordDeferral() }

        // A count that keeps climbing is the only signal that a device has stopped receiving
        // content releases altogether — it has to add up across launches, not reset.
        assertThat(store.consecutiveDeferrals()).isEqualTo(3)
        assertThat(SharedPreferencesContentArtifactStore(context).consecutiveDeferrals())
            .isEqualTo(3)
    }

    @Test
    fun `taking a release clears the run of deferrals`() {
        repeat(3) { store.recordDeferral() }

        store.clearDeferrals()

        assertThat(store.consecutiveDeferrals()).isEqualTo(0)

        store.recordDeferral()
        assertThat(store.consecutiveDeferrals()).isEqualTo(1)
    }

    @Test
    fun `the three values do not disturb one another`() {
        store.setInstalledArtifact("sha-v8")
        store.recordDeferral()
        store.setLegacyImportComplete()

        store.clearDeferrals()

        assertThat(store.installedArtifact()).isEqualTo("sha-v8")
        assertThat(store.legacyImportComplete()).isTrue()
        assertThat(store.consecutiveDeferrals()).isEqualTo(0)
    }
}
