package com.arshadshah.nimaz

import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.Test
import java.io.File

/**
 * The licence list must keep listing the whole app's dependencies.
 *
 * `com.mikepenz.aboutlibraries.plugin.android` generates `R.raw.aboutlibraries` from the
 * **applying project's runtime classpath**. Applied to `:app` that is every dependency the app
 * ships; applied to a library it would be that library's own graph and nothing else.
 *
 * That makes it the quietest failure in this migration. Moving the plugin into `:feature:about`
 * alongside `LicensesScreen` — the obvious tidy-up, and what #565 flagged as worth deciding
 * deliberately — would not break the build, fail a test, or throw at runtime. The screen would
 * simply render a shorter list, and the app would ship crediting a fraction of the software it
 * uses. It is the same reasoning that left `LibraryRepositoryImpl` as the one repository of
 * nineteen still in `:app` after PR 9 of #560.
 *
 * So the count is the assertion. [MINIMUM_LIBRARIES] sits below today's total by enough that
 * ordinary dependency churn never touches it, and far enough above a single module's graph that
 * a move cannot pass.
 */
class LicenceCatalogueTest {

    @Test
    fun `the generated catalogue covers the whole app's dependency graph`() {
        val catalogue = File(CATALOGUE)
        assertThat(catalogue.isFile).isTrue()

        val libraries = JSONObject(catalogue.readText()).getJSONArray("libraries")
        assertThat(libraries.length()).isAtLeast(MINIMUM_LIBRARIES)
    }

    private companion object {
        /**
         * Written by the AboutLibraries plugin into `:app`'s build directory, and packaged as
         * `res/raw`. Named as a *file*: a directory check would stay true if the plugin stopped
         * producing anything.
         */
        const val CATALOGUE = "build/generated/aboutLibraries/debug/res/raw/aboutlibraries.json"

        /**
         * 272 today. `:feature:about`'s own graph is a few dozen, so any number in the low
         * hundreds separates "the app" from "one module" unambiguously.
         */
        const val MINIMUM_LIBRARIES = 200
    }
}
