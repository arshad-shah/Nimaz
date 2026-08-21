package com.arshadshah.nimaz.buildlogic

import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.security.MessageDigest
import java.util.zip.GZIPInputStream

/**
 * Fetches the content artifact this app is built against, pinned by sha256 in `data.lock.json`.
 *
 * The prepackaged database used to be a 140 MB Git-LFS blob in this repository. LFS keeps every
 * version whole — no delta applies — so a one-character content correction cost another whole
 * copy of the file, forever. `.git/lfs` reached 508 MB holding eleven objects.
 *
 * It is now compiled by the `nz` data console in a separate repository and published as a
 * release asset. This task resolves the release, downloads the asset, **verifies its sha256
 * before anything else touches it**, and caches the result under the Gradle user home. The cache
 * directory is registered as a generated assets source root, so
 * `createFromAsset("database/nimaz_prepopulated.db")` keeps working with no Kotlin change.
 *
 * The trade being accepted: a build now needs network access and a token for a private repo. In
 * exchange the repository stops growing by 140 MB every time a row changes.
 *
 * ### Why this is a task class and not a `doLast` in a script plugin
 *
 * It used to be the latter, and that single `doLast` was the only thing keeping the Gradle
 * **configuration cache** off for the whole build (#503): its closure captured script-level
 * helpers, so Gradle refused to serialise it — *"cannot serialize Gradle script object
 * references"*. A typed task with declared inputs and outputs serialises fine. The task
 * *registration* deliberately stays in the consuming project — today `app/build.gradle.kts`,
 * which is the only project that consumes the generated assets. A convention plugin that
 * registered it centrally, leaving libraries to depend on `:app:fetchNimazData`, would point a
 * library at the app, which is the inversion the multi-module epic exists to remove.
 *
 * ### The credential is [Internal] on purpose
 *
 * [dataToken] must never be an `@Input`. Input values are written into the configuration cache
 * entry on disk; a GitHub token is not something to leave lying in `.gradle/configuration-cache`.
 * It is `@Internal`, so it takes no part in up-to-date checks either — which is correct: the
 * artifact's identity is its sha256, not the credential used to fetch it.
 */
abstract class FetchNimazDataTask : DefaultTask() {

    /** `data.lock.json`. The only thing that decides *what* gets fetched. */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val lockFile: RegularFileProperty

    /** The generated assets source root the merged APK assets are built from. */
    @get:OutputDirectory
    abstract val generatedAssets: DirectoryProperty

    /**
     * Shared download cache under the Gradle user home, keyed by sha256.
     *
     * `@Internal`: it lives outside the project, it is content-addressed, and declaring it as an
     * output would make every build snapshot a 170 MB file for nothing.
     */
    @get:Internal
    abstract val cacheRoot: DirectoryProperty

    /** See the class KDoc — deliberately `@Internal`, never `@Input`. */
    @get:Internal
    abstract val dataToken: Property<String>

    @TaskAction
    fun fetch() {
        val lock = NimazDataLockParser.parse(lockFile.get().asFile.readText())
        val artifact = lock.artifact
        val cached = cacheRoot.get().asFile.resolve("${artifact.sha256}/${artifact.file}")

        if (!cached.isFile || sha256Of(cached) != artifact.sha256) {
            download(lock, cached)
        }

        val target = generatedAssets.get().asFile.resolve(artifact.assetPath)
        target.parentFile.mkdirs()
        cached.copyTo(target, overwrite = true)
        logger.lifecycle("nimaz-data: 1 asset(s) verified from ${lock.repo}@${lock.tag}")
    }

    private fun download(lock: NimazDataLock, cached: File) {
        val artifact = lock.artifact
        val token = dataToken.orNull?.takeIf { it.isNotBlank() } ?: throw GradleException(
            """
            nimaz-data: no credential for the private content repository.

            Provide one of:
              export NIMAZ_DATA_TOKEN=<a fine-grained PAT with read access to contents>
              echo 'nimazDataToken=<token>' >> ~/.gradle/gradle.properties
              gh auth login
            """.trimIndent()
        )

        @Suppress("UNCHECKED_CAST")
        val release = JsonSlurper().parseText(
            githubApi("https://api.github.com/repos/${lock.repo}/releases/tags/${lock.tag}", token)
        ) as Map<String, Any?>

        @Suppress("UNCHECKED_CAST")
        val assets = release["assets"] as List<Map<String, Any?>>
        val names = assets.map { it["name"].toString() }
        val fetchedName = NimazDataLockParser.chooseAsset(artifact, names)
            ?: throw GradleException(
                "nimaz-data: release ${lock.tag} has no asset named ${artifact.file}. " +
                    "Present: ${names.joinToString()}"
            )
        val asset = assets.first { it["name"] == fetchedName }
        val compressed = fetchedName.endsWith(".gz")

        logger.lifecycle("nimaz-data: fetching $fetchedName from ${lock.repo}@${lock.tag}")
        cached.parentFile.mkdirs()
        // Retried like the metadata call, and for a stronger reason: this is a 54 MB transfer, so
        // it has far more time in which to be interrupted. Restarting from zero rather than
        // resuming is the right trade for a file this size — a resumed download that silently
        // misjoins is exactly the corruption the sha256 below is there to catch, and re-fetching
        // costs a minute.
        retrying("downloading $fetchedName") {
            val conn = URI(asset["url"] as String).toURL().openConnection() as HttpURLConnection
            conn.setRequestProperty("Authorization", "Bearer $token")
            // The API serves metadata for this URL unless octet-stream is asked for.
            conn.setRequestProperty("Accept", "application/octet-stream")
            conn.instanceFollowRedirects = true
            conn.inputStream.use { input ->
                // Decompressed on the way in, so what lands in the cache is always the artifact
                // itself and the sha256 below always means the same thing. The lockfile pins the
                // *decompressed* bytes — verifying the wrapper instead would let a
                // re-compression change what the pin asserts without the hash moving.
                val source = if (compressed) GZIPInputStream(input, 1 shl 16) else input
                cached.outputStream().use { source.copyTo(it) }
            }
        }

        val actual = sha256Of(cached)
        if (actual != artifact.sha256) {
            cached.delete()
            throw GradleException(
                "nimaz-data: ${artifact.file} failed verification.\n" +
                    "  fetched  $fetchedName" +
                    (if (compressed) " (gzip, verified after decompressing)" else "") + "\n" +
                    "  expected sha256 ${artifact.sha256}\n" +
                    "  actual   sha256 $actual\n" +
                    "The lockfile and the release disagree. Do not build from this."
            )
        }
    }

    private fun githubApi(url: String, token: String): String = retrying("resolving $url") {
        val conn = URI(url).toURL().openConnection() as HttpURLConnection
        conn.setRequestProperty("Authorization", "Bearer $token")
        conn.setRequestProperty("Accept", "application/vnd.github+json")

        // These three are the failures that actually happen, and each has a different fix.
        // Letting them surface as a bare IOException — which is what used to happen for 403 —
        // costs an afternoon.
        when (conn.responseCode) {
            404 -> throw GradleException(
                "nimaz-data: $url returned 404.\n" +
                    "  The tag does not exist, or the credential cannot see the repository at all."
            )

            401 -> throw GradleException(
                "nimaz-data: $url returned 401 — the credential was rejected.\n" +
                    "  A GitHub App installation token lasts an hour; a stale one looks like this."
            )

            403 -> throw GradleException(
                "nimaz-data: $url returned 403 — authenticated, but not permitted.\n" +
                    "  The App is installed on the repository (otherwise the token would not\n" +
                    "  have minted), so this is almost always the permission set:\n" +
                    "    github.com/settings/apps -> your App -> Permissions & events\n" +
                    "    Repository permissions -> Contents -> Read-only\n" +
                    "  Changing permissions raises a request the installation must accept:\n" +
                    "    github.com/settings/installations -> Review request\n" +
                    "  See docs/CONTENT_REPO_AUTH.md."
            )
        }
        conn.inputStream.bufferedReader().use { it.readText() }
    }

    /**
     * Runs [block], retrying the failures that are GitHub having a moment rather than this build
     * being wrong.
     *
     * Every fetch here is a cold one on CI, and a release asset that exists and a credential that
     * works still produce the occasional `HTTP response code: 500` — twice on `dev`, each time
     * reddening a deploy that had nothing wrong with it and each time green on a plain re-run. An
     * `IOException` from `HttpURLConnection` covers that case (the 500 surfaces as one when the
     * stream is opened) along with resets and read timeouts.
     *
     * Deliberately narrow: [GradleException] passes straight through, so the 401/403/404
     * diagnoses above — which are all *permanent* — still fail on the first attempt with their
     * explanation intact. Retrying those would turn a clear message into a slow one.
     *
     * Logs through the task's own [logger]. The script-plugin version reached for
     * `project.logger`, which is `Task.project` at execution time — a configuration-cache
     * violation in its own right, separate from the unserialisable closure.
     */
    private fun <T> retrying(what: String, block: () -> T): T {
        var wait = 2_000L
        var last: IOException? = null
        repeat(4) { attempt ->
            try {
                return block()
            } catch (e: IOException) {
                last = e
                if (attempt == 3) return@repeat
                logger.lifecycle("nimaz-data: $what failed (${e.message}); retrying in ${wait / 1000}s")
                Thread.sleep(wait)
                wait *= 2
            }
        }
        throw GradleException(
            "nimaz-data: $what failed on all 4 attempts. Last error: ${last?.message}",
            last,
        )
    }

    internal companion object {
        fun sha256Of(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { stream ->
                val buffer = ByteArray(1 shl 20)
                while (true) {
                    val read = stream.read(buffer)
                    if (read <= 0) break
                    digest.update(buffer, 0, read)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
    }
}

/**
 * `gh auth token`, as a configuration-cache-legal external process.
 *
 * A bare `ProcessBuilder` in build logic is an untracked read of the outside world: the
 * configuration cache has no way to know the answer changed. A [ValueSource] is the sanctioned
 * form — Gradle re-obtains it when checking whether a cached entry is still usable.
 *
 * It is only ever consumed as the last `orElse` of the credential chain, so a build with
 * `NIMAZ_DATA_TOKEN` or `nimazDataToken` set never spawns `gh` at all, and a machine without the
 * `gh` CLI yields an absent provider rather than a failure — the task then reports the missing
 * credential itself, with the three ways to supply one.
 */
abstract class GhAuthTokenValueSource :
    ValueSource<String, ValueSourceParameters.None> {

    override fun obtain(): String? = runCatching {
        val proc = ProcessBuilder("gh", "auth", "token").redirectErrorStream(true).start()
        proc.inputStream.bufferedReader().use { it.readText() }.trim()
    }.getOrNull()?.takeIf { it.isNotBlank() && !it.contains(" ") }
}
