import groovy.json.JsonSlurper
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.security.MessageDigest
import java.util.zip.GZIPInputStream

/**
 * Fetches the content artifact this app is built against, pinned by sha256 in data.lock.json.
 *
 * The prepackaged database used to be a 140 MB Git-LFS blob in this repository. LFS keeps every
 * version whole — no delta applies — so a one-character content correction cost another whole
 * copy of the file, forever. `.git/lfs` reached 508 MB holding eleven objects.
 *
 * It is now compiled by the `nz` data console in a separate repository and published as a
 * release asset. This task resolves the release, downloads each asset, **verifies its sha256
 * before anything else touches it**, and caches the result under the Gradle user home. The
 * cache directory is registered as a generated assets source root, so
 * `createFromAsset("database/nimaz_prepopulated.db")` keeps working with no Kotlin change.
 *
 * The trade being accepted: a build now needs network access and a token for a private repo.
 * In exchange the repository stops growing by 140 MB every time a row changes.
 *
 * This is a script plugin rather than a `buildSrc` task class on purpose — `buildSrc` would add
 * a compiled module to every build for one task, and a script plugin cannot export a type
 * across the `apply(from = …)` boundary anyway.
 */

val lockFile: File = rootProject.file("data.lock.json")
val generatedAssets: Provider<Directory> = layout.buildDirectory.dir("generated/nimazData/assets")
val cacheRoot: File = gradle.gradleUserHomeDir.resolve("caches/nimaz-data")

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

fun resolveDataToken(): String {
    System.getenv("NIMAZ_DATA_TOKEN")?.takeIf { it.isNotBlank() }?.let { return it }
    (project.findProperty("nimazDataToken") as? String)?.takeIf { it.isNotBlank() }
        ?.let { return it }
    runCatching {
        val proc = ProcessBuilder("gh", "auth", "token").redirectErrorStream(true).start()
        proc.inputStream.bufferedReader().use { it.readText() }.trim()
    }.getOrNull()?.takeIf { it.isNotBlank() && !it.contains(" ") }?.let { return it }

    throw GradleException(
        """
        nimaz-data: no credential for the private content repository.

        Provide one of:
          export NIMAZ_DATA_TOKEN=<a fine-grained PAT with read access to contents>
          echo 'nimazDataToken=<token>' >> ~/.gradle/gradle.properties
          gh auth login
        """.trimIndent()
    )
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
 * Deliberately narrow: [GradleException] passes straight through, so the 401/403/404 diagnoses
 * below — which are all *permanent* — still fail on the first attempt with their explanation
 * intact. Retrying those would turn a clear message into a slow one.
 */
fun <T> retrying(what: String, block: () -> T): T {
    var wait = 2_000L
    var last: IOException? = null
    repeat(4) { attempt ->
        try {
            return block()
        } catch (e: IOException) {
            last = e
            if (attempt == 3) return@repeat
            project.logger.lifecycle(
                "nimaz-data: $what failed (${e.message}); retrying in ${wait / 1000}s"
            )
            Thread.sleep(wait)
            wait *= 2
        }
    }
    throw GradleException(
        "nimaz-data: $what failed on all 4 attempts. Last error: ${last?.message}",
        last,
    )
}

fun githubApi(url: String, token: String): String = retrying("resolving $url") {
    val conn = URI(url).toURL().openConnection() as HttpURLConnection
    conn.setRequestProperty("Authorization", "Bearer $token")
    conn.setRequestProperty("Accept", "application/vnd.github+json")

    // These three are the failures that actually happen, and each has a
    // different fix. Letting them surface as a bare IOException — which is what
    // used to happen for 403 — costs an afternoon.
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

tasks.register("fetchNimazData") {
    description = "Fetches and sha256-verifies the pinned content artifact."
    group = "build setup"

    inputs.file(lockFile)
    outputs.dir(generatedAssets)

    doLast {
        @Suppress("UNCHECKED_CAST")
        val lock = JsonSlurper().parse(lockFile) as Map<String, Any>
        val repo = lock["repo"] as String
        val tag = lock["tag"] as String
        // The artifact, and only the artifact. `lock["patch"]` used to be fetched beside it for
        // ContentPatchSeeder; the seeder is gone — a release now reaches existing installs by
        // replacing the database (ContentArtifactInstaller), so there is no second asset.
        val entries = listOfNotNull(lock["artifact"] as? Map<*, *>)

        var token: String? = null
        var release: Map<*, *>? = null

        for (entry in entries) {
            val name = entry["file"] as String
            val sha = entry["sha256"] as String
            val cached = cacheRoot.resolve("$sha/$name")

            if (!cached.isFile || sha256Of(cached) != sha) {
                if (token == null) token = resolveDataToken()
                if (release == null) {
                    @Suppress("UNCHECKED_CAST")
                    release = JsonSlurper().parseText(
                        githubApi("https://api.github.com/repos/$repo/releases/tags/$tag", token!!)
                    ) as Map<String, Any>
                }
                @Suppress("UNCHECKED_CAST")
                val assets = release!!["assets"] as List<Map<String, Any>>

                // Prefer the gzipped asset when the lockfile pins one: the artifact is
                // 170 MB raw and about 5x smaller compressed, and every CI runner is a
                // cold fetch by construction. Falls back to the raw `.db` for a tag
                // published before releases carried one, so an old pin still builds.
                @Suppress("UNCHECKED_CAST")
                val gz = entry["compressed"] as? Map<*, *>
                val wanted = (gz?.get("file") as? String) ?: name
                val asset = assets.firstOrNull { it["name"] == wanted }
                    ?: assets.firstOrNull { it["name"] == name }
                    ?: throw GradleException(
                        "nimaz-data: release $tag has no asset named $name. Present: " +
                            assets.joinToString { it["name"].toString() }
                    )
                val fetchedName = asset["name"] as String
                val compressed = fetchedName.endsWith(".gz")

                logger.lifecycle("nimaz-data: fetching $fetchedName from $repo@$tag")
                cached.parentFile.mkdirs()
                // Retried like the metadata call, and for a stronger reason: this is a 54 MB
                // transfer, so it has far more time in which to be interrupted. Restarting from
                // zero rather than resuming is the right trade for a file this size — a resumed
                // download that silently misjoins is exactly the corruption the sha256 below is
                // there to catch, and re-fetching costs a minute.
                retrying("downloading $fetchedName") {
                    val conn = URI(asset["url"] as String).toURL()
                        .openConnection() as HttpURLConnection
                    conn.setRequestProperty("Authorization", "Bearer ${token!!}")
                    // The API serves metadata for this URL unless octet-stream is asked for.
                    conn.setRequestProperty("Accept", "application/octet-stream")
                    conn.instanceFollowRedirects = true
                    conn.inputStream.use { input ->
                        // Decompressed on the way in, so what lands in the cache is always
                        // the artifact itself and the sha256 below always means the same
                        // thing. The lockfile pins the *decompressed* bytes — verifying the
                        // wrapper instead would let a re-compression change what the pin
                        // asserts without the hash moving.
                        val source = if (compressed) GZIPInputStream(input, 1 shl 16) else input
                        cached.outputStream().use { source.copyTo(it) }
                    }
                }

                val actual = sha256Of(cached)
                if (actual != sha) {
                    cached.delete()
                    throw GradleException(
                        "nimaz-data: $name failed verification.\n" +
                            "  fetched  $fetchedName" +
                            (if (compressed) " (gzip, verified after decompressing)" else "") +
                            "\n" +
                            "  expected sha256 $sha\n" +
                            "  actual   sha256 $actual\n" +
                            "The lockfile and the release disagree. Do not build from this."
                    )
                }
            }

            val target = generatedAssets.get().asFile.resolve(entry["assetPath"] as String)
            target.parentFile.mkdirs()
            cached.copyTo(target, overwrite = true)
        }
        logger.lifecycle("nimaz-data: ${entries.size} asset(s) verified from $repo@$tag")
    }
}
