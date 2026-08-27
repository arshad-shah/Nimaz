package com.arshadshah.nimaz.buildlogic

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The lockfile is read twice — by `fetchNimazData` to decide what to download and verify, and by
 * `:app` to bake `CONTENT_ARTIFACT_SHA256` into the APK. Both go through this parser precisely so
 * they cannot disagree about what the pin says, which makes it worth holding to tests.
 */
class NimazDataLockParserTest {

    private val lock = """
        {
          "_comment": ["ignored"],
          "repo": "arshad-shah/nimaz-data",
          "tag": "data-v9",
          "schemaVersion": 25,
          "artifact": {
            "file": "nimaz-4dac2879.db",
            "assetPath": "database/nimaz_prepopulated.db",
            "sha256": "4dac2879",
            "bytes": 180117504,
            "compressed": {
              "file": "nimaz-4dac2879.db.gz",
              "sha256": "37a77384",
              "bytes": 55740402
            }
          }
        }
    """.trimIndent()

    @Test
    fun `reads the pin the build is judged against`() {
        val parsed = NimazDataLockParser.parse(lock)

        assertThat(parsed.repo).isEqualTo("arshad-shah/nimaz-data")
        assertThat(parsed.tag).isEqualTo("data-v9")
        assertThat(parsed.artifact.file).isEqualTo("nimaz-4dac2879.db")
        assertThat(parsed.artifact.assetPath).isEqualTo("database/nimaz_prepopulated.db")
        assertThat(parsed.artifact.compressedFile).isEqualTo("nimaz-4dac2879.db.gz")
    }

    @Test
    fun `sha256 is the hash of the decompressed artifact, never the wrapper`() {
        // Verifying the gzip instead would let a re-compression change what the pin asserts
        // without the hash moving. `37a77384` is the wrapper and must never be what is returned.
        assertThat(NimazDataLockParser.parse(lock).artifact.sha256).isEqualTo("4dac2879")
    }

    @Test
    fun `prefers the compressed asset — 54 MB rather than 170 MB on every cold CI fetch`() {
        val artifact = NimazDataLockParser.parse(lock).artifact
        val chosen = NimazDataLockParser.chooseAsset(
            artifact,
            listOf("nimaz-4dac2879.db", "nimaz-4dac2879.db.gz", "checksums.txt"),
        )
        assertThat(chosen).isEqualTo("nimaz-4dac2879.db.gz")
    }

    @Test
    fun `falls back to the raw db so a tag published before gzip still builds`() {
        val artifact = NimazDataLockParser.parse(lock).artifact
        val chosen = NimazDataLockParser.chooseAsset(artifact, listOf("nimaz-4dac2879.db"))
        assertThat(chosen).isEqualTo("nimaz-4dac2879.db")
    }

    @Test
    fun `a lockfile with no compressed entry resolves to the raw db`() {
        val noGz = """
            {
              "repo": "arshad-shah/nimaz-data",
              "tag": "data-v3",
              "artifact": {
                "file": "nimaz-4dac2879.db",
                "assetPath": "database/nimaz_prepopulated.db",
                "sha256": "4dac2879"
              }
            }
        """.trimIndent()
        val artifact = NimazDataLockParser.parse(noGz).artifact
        assertThat(artifact.compressedFile).isNull()
        assertThat(NimazDataLockParser.chooseAsset(artifact, listOf("nimaz-4dac2879.db")))
            .isEqualTo("nimaz-4dac2879.db")
    }

    @Test
    fun `returns null rather than guessing when the release has neither asset`() {
        val artifact = NimazDataLockParser.parse(lock).artifact
        assertThat(NimazDataLockParser.chooseAsset(artifact, listOf("something-else.db"))).isNull()
    }

    @Test
    fun `a lockfile with no artifact entry fails loudly`() {
        val error = runCatching {
            NimazDataLockParser.parse("""{"repo":"a/b","tag":"t"}""")
        }.exceptionOrNull()
        assertThat(error).hasMessageThat().contains("no `artifact` entry")
    }

    @Test
    fun `a missing sha256 fails rather than shipping an empty pin`() {
        val error = runCatching {
            NimazDataLockParser.parse(
                """{"repo":"a/b","tag":"t","artifact":{"file":"f","assetPath":"p"}}"""
            )
        }.exceptionOrNull()
        assertThat(error).hasMessageThat().contains("sha256")
    }
}
