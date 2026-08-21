package com.arshadshah.nimaz.buildlogic

import groovy.json.JsonSlurper

/**
 * The parts of `data.lock.json` this build actually reads.
 *
 * Parsing lives in a plain data type rather than in a `doLast` block so it can be unit-tested
 * without a Gradle build, and so the two readers of the lockfile — [FetchNimazDataTask] and
 * `:app`'s `CONTENT_ARTIFACT_SHA256` build-config field — cannot drift into two different
 * interpretations of the same file.
 */
data class NimazDataLock(
    val repo: String,
    val tag: String,
    val artifact: NimazDataArtifact,
)

/**
 * One pinned asset.
 *
 * [sha256] is always the hash of the *decompressed* bytes: [compressedFile] names the gzipped
 * asset the fetch prefers, and verifying the wrapper instead would let a re-compression change
 * what the pin asserts without the hash moving.
 */
data class NimazDataArtifact(
    val file: String,
    val assetPath: String,
    val sha256: String,
    val compressedFile: String?,
)

object NimazDataLockParser {

    /**
     * Parses the lockfile text.
     *
     * Text, not a [java.io.File]: at configuration time the caller reads it through
     * `providers.fileContents(...)` so the configuration cache is invalidated when the pin
     * changes, and at execution time the task reads its own declared `@InputFile`. Neither
     * path may reach for the file itself from inside a lazily-evaluated block.
     */
    @Suppress("UNCHECKED_CAST")
    fun parse(text: String): NimazDataLock {
        val root = JsonSlurper().parseText(text) as Map<String, Any?>
        val artifact = root["artifact"] as? Map<String, Any?>
            ?: error("nimaz-data: data.lock.json has no `artifact` entry.")
        val compressed = artifact["compressed"] as? Map<String, Any?>
        return NimazDataLock(
            repo = requireString(root, "repo"),
            tag = requireString(root, "tag"),
            artifact = NimazDataArtifact(
                file = requireString(artifact, "file"),
                assetPath = requireString(artifact, "assetPath"),
                sha256 = requireString(artifact, "sha256"),
                compressedFile = compressed?.get("file") as? String,
            ),
        )
    }

    private fun requireString(from: Map<String, Any?>, key: String): String =
        from[key] as? String
            ?: error("nimaz-data: data.lock.json is missing the `$key` entry.")

    /**
     * Which release asset to download.
     *
     * Prefers the gzipped asset when the lockfile pins one — the artifact is 170 MB raw and
     * about 5x smaller compressed, and every CI runner is a cold fetch by construction. Falls
     * back to the raw `.db` for a tag published before releases carried one, so an old pin still
     * builds.
     */
    fun chooseAsset(artifact: NimazDataArtifact, availableNames: List<String>): String? =
        availableNames.firstOrNull { it == artifact.compressedFile }
            ?: availableNames.firstOrNull { it == artifact.file }
}
