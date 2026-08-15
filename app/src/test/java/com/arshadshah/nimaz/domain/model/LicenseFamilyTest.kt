package com.arshadshah.nimaz.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The classifier exists because AboutLibraries reports whatever spelling the POM declared, and
 * the licence screen groups by family. These are the spellings actually present in this app's
 * dependency graph, plus the two substring traps that make a naive `contains` wrong.
 */
class LicenseFamilyTest {

    @Test
    fun `every spelling of Apache is Apache`() {
        listOf(
            "Apache License 2.0",
            "Apache-2.0",
            "The Apache Software License, Version 2.0",
            "apache license, version 2.0",
        ).forEach {
            assertThat(LicenseFamily.of(it)).isEqualTo(LicenseFamily.APACHE_2)
        }
    }

    @Test
    fun `the short names are matched as words, not as substrings`() {
        assertThat(LicenseFamily.of("MIT License")).isEqualTo(LicenseFamily.MIT)
        assertThat(LicenseFamily.of("BSD 3-Clause License")).isEqualTo(LicenseFamily.BSD)
        // "permitted" contains "mit" and "absd" would contain "bsd" — a bare substring test
        // filed licences under families they have nothing to do with.
        assertThat(LicenseFamily.of("A licence permitted for any use")).isEqualTo(LicenseFamily.OTHER)
    }

    @Test
    fun `copyleft is recognised by either its acronym or its full name`() {
        listOf("GPL-3.0", "GNU Lesser General Public License v2.1", "LGPL").forEach {
            assertThat(LicenseFamily.of(it)).isEqualTo(LicenseFamily.GPL)
        }
    }

    @Test
    fun `the font licence is recognised by name and by acronym`() {
        assertThat(LicenseFamily.of("SIL Open Font License 1.1")).isEqualTo(LicenseFamily.OFL)
        assertThat(LicenseFamily.of("OFL-1.1")).isEqualTo(LicenseFamily.OFL)
    }

    @Test
    fun `an absent or unrecognised licence is OTHER, never a crash`() {
        assertThat(LicenseFamily.of(null)).isEqualTo(LicenseFamily.OTHER)
        assertThat(LicenseFamily.of("")).isEqualTo(LicenseFamily.OTHER)
        assertThat(LicenseFamily.of("Eclipse Public License 2.0")).isEqualTo(LicenseFamily.OTHER)
    }

    @Test
    fun `a library speaks for its first licence and exposes its Maven group`() {
        val dual = OpenSourceLibrary(
            id = 1,
            name = "Dual",
            coordinate = "com.example.group:artifact",
            version = "1.0",
            author = null,
            website = null,
            licenses = listOf(
                LibraryLicense("MIT License", url = null, content = null),
                LibraryLicense("Apache License 2.0", url = null, content = null),
            ),
        )

        assertThat(dual.family).isEqualTo(LicenseFamily.MIT)
        assertThat(dual.group).isEqualTo("com.example.group")
    }

    @Test
    fun `a coordinate that is not group colon artifact has no group`() {
        val bundled = OpenSourceLibrary(
            id = 2,
            name = "Amiri",
            coordinate = "amiri",
            version = null,
            author = null,
            website = null,
            licenses = emptyList(),
        )

        assertThat(bundled.group).isNull()
        assertThat(bundled.family).isEqualTo(LicenseFamily.OTHER)
    }
}
