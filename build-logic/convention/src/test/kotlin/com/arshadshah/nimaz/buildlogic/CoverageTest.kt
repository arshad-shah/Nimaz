package com.arshadshah.nimaz.buildlogic

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The arithmetic behind the per-module floor, and the exclusion list it measures against.
 *
 * Both are worth a plain unit test rather than a TestKit build: the interesting cases are a
 * module sitting exactly on its floor and a report that describes nothing at all, and the second
 * is the one that has to be a *failure* — an empty JaCoCo report is a valid file, and it reads
 * identically to 0% at a glance, so a gate that treated it as "no lines missed" would pass every
 * module whose class output had quietly moved.
 */
class CoverageTest {

    private fun report(vararg counters: Pair<String, Pair<Int, Int>>): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<!DOCTYPE report PUBLIC "-//JACOCO//DTD Report 1.1//EN" "report.dtd">""")
        append("<report name=\"fixture\">")
        // A package the counters are made of, so the fixture is shaped like a real report:
        // JaCoCo writes the report-level counters *after* every package, and a parser that took
        // the first `counter` it found anywhere would read a package's numbers as the total.
        append("<package name=\"com/example\">")
        append("<counter type=\"LINE\" missed=\"999\" covered=\"1\"/>")
        append("</package>")
        counters.forEach { (type, value) ->
            val (covered, missed) = value
            append("<counter type=\"$type\" missed=\"$missed\" covered=\"$covered\"/>")
        }
        append("</report>")
    }

    // ---- Reading the report ----

    @Test
    fun `the report-level counters are the ones read, not a package's`() {
        val counters = parseJacocoCounters(report("LINE" to (80 to 20), "BRANCH" to (7 to 3)))

        assertThat(counters["LINE"]).isEqualTo(CoverageRatio(covered = 80, total = 100))
        assertThat(counters["BRANCH"]).isEqualTo(CoverageRatio(covered = 7, total = 10))
    }

    @Test
    fun `a report describing nothing parses rather than throwing`() {
        // What JaCoCo writes when `classDirectories` resolves to nothing — a 237-byte file with
        // a sessioninfo and no classes. It has to reach the gate to be rejected there.
        val counters = parseJacocoCounters(
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
                """<!DOCTYPE report PUBLIC "-//JACOCO//DTD Report 1.1//EN" "report.dtd">""" +
                """<report name="fixture"><sessioninfo id="x" start="1" dump="2"/></report>"""
        )

        assertThat(counters).isEmpty()
    }

    @Test
    fun `the DOCTYPE is not fetched`() {
        // JaCoCo stamps a `report.dtd` that is not on disk. A parser left to resolve it either
        // reaches the network or throws — either way the gate stops working on a CI runner.
        assertThat(parseJacocoCounters(report("LINE" to (1 to 0)))).isNotEmpty()
    }

    // ---- The ratio ----

    @Test
    fun `a ratio reports the fraction it covers`() {
        assertThat(CoverageRatio(covered = 3, total = 4).fraction).isEqualTo(0.75)
        assertThat(CoverageRatio(covered = 3, total = 4).percent).isEqualTo("75.0%")
    }

    @Test
    fun `a ratio with nothing to cover is complete rather than a division by zero`() {
        assertThat(CoverageRatio(covered = 0, total = 0).fraction).isEqualTo(1.0)
    }

    // ---- The gate ----

    @Test
    fun `a module above its floor passes`() {
        assertThat(
            coverageShortfall("line", CoverageRatio(85, 100), floor = 0.80, modulePath = ":m")
        ).isNull()
    }

    @Test
    fun `a module exactly on its floor passes`() {
        // Otherwise a module locked at the number it reports fails its own gate the day it is
        // locked, and floating-point equality decides whether the build is green.
        assertThat(
            coverageShortfall("line", CoverageRatio(80, 100), floor = 0.80, modulePath = ":m")
        ).isNull()
    }

    @Test
    fun `a module below its floor is named, with the numbers behind it`() {
        val message =
            coverageShortfall("line", CoverageRatio(79, 100), floor = 0.80, modulePath = ":core:x")

        assertThat(message).isNotNull()
        assertThat(message).contains(":core:x")
        assertThat(message).contains("79.0%")
        assertThat(message).contains("80.0%")
    }

    @Test
    fun `a report that measured nothing fails rather than passing vacuously`() {
        assertThat(coverageShortfall("line", null, floor = 0.80, modulePath = ":m"))
            .contains("describes no line at all")
        assertThat(
            coverageShortfall("branch", CoverageRatio(0, 0), floor = 0.80, modulePath = ":m")
        ).contains("describes no branch at all")
    }

    // ---- What is measured ----

    @Test
    fun `generated and unreachable classes are excluded`() {
        assertThat(COVERAGE_EXCLUSIONS).containsAtLeast(
            "**/*_Impl*.*",
            "**/*_Factory*.*",
            "**/di/**",
            // The static bridge beside an interface's default methods: generated for binary
            // compatibility, never called, and permanently 0% in every DAO that has one.
            "**/*\$DefaultImpls*.*",
        )
    }

    @Test
    fun `entities and row projections are excluded, anchored so behaviour is not`() {
        assertThat(COVERAGE_EXCLUSIONS).containsAtLeast("**/*Entity.class", "**/*Row.class")

        // `*Row*` rather than `*Row` would also swallow `ObserveKhatamRowProgressUseCase` and
        // `ObserveQaidaRowProgressUseCase`, which are behaviour and are tested.
        assertThat(COVERAGE_EXCLUSIONS).doesNotContain("**/*Row*.*")
        assertThat(COVERAGE_EXCLUSIONS).doesNotContain("**/*Entity*.*")
    }

    @Test
    fun `companion objects are measured`() {
        // This codebase keeps its Room migrations in `NimazDatabase.Companion`. Excluding
        // companions hid all eighteen of them — the code where a mistake is a crash on launch.
        assertThat(COVERAGE_EXCLUSIONS.none { it.contains("Companion") }).isTrue()
    }
}
