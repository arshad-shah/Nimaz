package com.arshadshah.nimaz.buildlogic

import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory

/**
 * The credential [FetchNimazDataTask] uses to read the private content repository.
 *
 * Three sources, in precedence order: the `NIMAZ_DATA_TOKEN` environment variable, the
 * `nimazDataToken` Gradle property, and `gh auth token`. All three go through providers so that
 * nothing reads the environment at execution time — `project.findProperty` and a bare
 * `ProcessBuilder` in a `doLast` were two of the reasons the configuration cache could not be
 * turned on (#503).
 *
 * ### Each source is tested for blankness *individually*
 *
 * This is the whole reason [chain] exists as a named, tested function rather than as three
 * inline `orElse` calls. `orElse` falls through on *absence*, not on emptiness — so a single
 * `isNotBlank` applied to the combined result would let a **set-but-empty** `NIMAZ_DATA_TOKEN`
 * short-circuit the chain and fail the build with "no credential", never trying the other two
 * sources.
 *
 * That is not hypothetical. A pull request from a fork cannot read repository secrets, so
 * `env.NIMAZ_DATA_TOKEN: ${{ secrets.… }}` expands to the empty string there — set, and empty.
 * The behaviour being preserved is the original script's, which applied `takeIf { isNotBlank() }`
 * to each source in turn.
 */
object NimazDataCredentials {

    const val ENV_VAR = "NIMAZ_DATA_TOKEN"
    const val GRADLE_PROPERTY = "nimazDataToken"

    /**
     * The precedence chain, over three already-built providers so it can be tested without an
     * environment to manipulate.
     *
     * Lazy throughout: [gh] is only queried when the first two are absent *or blank*, so a build
     * that has a token in its environment never spawns a process.
     */
    fun chain(
        env: Provider<String>,
        gradleProperty: Provider<String>,
        gh: Provider<String>,
    ): Provider<String> =
        env.filter { it.isNotBlank() }
            .orElse(gradleProperty.filter { it.isNotBlank() })
            .orElse(gh.filter { it.isNotBlank() })

    /** The chain wired to the real sources. */
    fun of(providers: ProviderFactory): Provider<String> = chain(
        env = providers.environmentVariable(ENV_VAR),
        gradleProperty = providers.gradleProperty(GRADLE_PROPERTY),
        gh = providers.of(GhAuthTokenValueSource::class.java) {},
    )
}
