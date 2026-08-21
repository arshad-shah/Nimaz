package com.arshadshah.nimaz.buildlogic

import com.google.common.truth.Truth.assertThat
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

/**
 * The regression these tests exist for: converting the credential lookup to providers (#503) very
 * nearly turned "each source is checked for blankness" into "the *result* is checked for
 * blankness". Those differ exactly when a source is **set but empty** — which is what
 * `NIMAZ_DATA_TOKEN` is on a pull request from a fork, because a fork cannot read repository
 * secrets and the expansion yields an empty string. The wrong version fails the build with
 * "no credential" instead of falling through to `gh auth token`.
 */
class NimazDataCredentialsTest {

    private val project: Project = ProjectBuilder.builder().build()

    private fun value(v: String?): Provider<String> = project.providers.provider { v }

    private val absent: Provider<String> get() = value(null)

    @Test
    fun `the environment variable wins when it holds a token`() {
        val token = NimazDataCredentials.chain(value("env-token"), value("prop"), value("gh"))
        assertThat(token.get()).isEqualTo("env-token")
    }

    @Test
    fun `an empty environment variable falls through to the gradle property`() {
        // The fork-PR case. `set, and empty` must behave like `not set`.
        val token = NimazDataCredentials.chain(value(""), value("prop-token"), value("gh"))
        assertThat(token.get()).isEqualTo("prop-token")
    }

    @Test
    fun `a whitespace-only environment variable falls through too`() {
        val token = NimazDataCredentials.chain(value("   "), value("prop-token"), value("gh"))
        assertThat(token.get()).isEqualTo("prop-token")
    }

    @Test
    fun `an empty environment variable and an empty property fall through to gh`() {
        val token = NimazDataCredentials.chain(value(""), value(""), value("gh-token"))
        assertThat(token.get()).isEqualTo("gh-token")
    }

    @Test
    fun `an absent environment variable falls through`() {
        val token = NimazDataCredentials.chain(absent, value("prop-token"), value("gh"))
        assertThat(token.get()).isEqualTo("prop-token")
    }

    @Test
    fun `the gradle property is used when the environment variable is not set`() {
        val token = NimazDataCredentials.chain(absent, absent, value("gh-token"))
        assertThat(token.get()).isEqualTo("gh-token")
    }

    @Test
    fun `three blank or absent sources leave the provider absent, not blank`() {
        // Absent, so the task can raise its own "no credential" message naming all three ways to
        // supply one — rather than handing an empty Bearer header to GitHub and reporting a 401.
        assertThat(NimazDataCredentials.chain(value(""), value("  "), absent).isPresent).isFalse()
        assertThat(NimazDataCredentials.chain(absent, absent, absent).isPresent).isFalse()
    }

    @Test
    fun `later sources are not queried when an earlier one supplies a token`() {
        // Laziness is a functional requirement, not a nicety: the last source shells out to
        // `gh auth token`, and a build that already has a credential must never spawn it.
        var ghQueried = false
        val gh = project.providers.provider { ghQueried = true; "gh-token" }

        assertThat(NimazDataCredentials.chain(value("env-token"), absent, gh).get())
            .isEqualTo("env-token")
        assertThat(ghQueried).isFalse()
    }

    @Test
    fun `the source names match what the docs and the error message tell people to set`() {
        assertThat(NimazDataCredentials.ENV_VAR).isEqualTo("NIMAZ_DATA_TOKEN")
        assertThat(NimazDataCredentials.GRADLE_PROPERTY).isEqualTo("nimazDataToken")
    }
}
