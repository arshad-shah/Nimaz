package com.arshadshah.nimaz.buildlogic

import javax.xml.parsers.DocumentBuilderFactory
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.register
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.w3c.dom.Element

/**
 * Class-file noise that must never count toward coverage, in one place.
 *
 * Lived in `app/build.gradle.kts` until the per-module floor existed. It cannot any more: a floor
 * a module is locked at and the merged report `:app` publishes have to be measuring the *same*
 * classes, or a module passes its own gate at 82% and reads 61% in the report nobody can
 * reconcile it with. One list, imported by both.
 *
 * Three groups, and the third is the one that needed a decision.
 *
 * **Generated code** — `R`, `BuildConfig`, Hilt/Dagger, Room's `_Impl`, and the
 * `ComposableSingletons` class the Compose compiler emits per file to hold `@Preview` lambdas.
 * Nobody wrote it, so nobody can test it.
 *
 * **`$DefaultImpls`** — the static bridge Kotlin emits beside an interface's default methods.
 * Since the compiler started emitting real JVM default methods it is generated for binary
 * compatibility and **never called**: the body is measured on the interface itself, and the
 * bridge is dead the moment it is written. `:core:database` alone carries 45 lines of it across
 * eight DAOs, all of them permanently 0%, and no test that could ever be written would reach
 * them. Excluding it is the difference between a floor that measures testing and one that
 * measures how many `@Transaction` helpers a DAO happens to have.
 *
 * **Room entities and DAO row projections** — the schema written as Kotlin: a primary constructor
 * and nothing else. Every "uncovered line" in one is a generated `equals`/`hashCode`/`toString`/
 * `copy`/`componentN` that JaCoCo counts and no honest test asserts on. On `:core:database` they
 * are 798 of 1,310 measurable lines — 61% of the module — so leaving them in means a floor that
 * is mostly a statement about how many columns the database has. Matched by suffix rather than by
 * package, because entities are declared in three places (`entity/`, `user/UserEntities.kt`, and
 * beside the DAO that projects them) and a package glob would miss two of them. `*Row` is
 * anchored to the end of the name on purpose: `*Row*` would also swallow
 * `ObserveKhatamRowProgressUseCase`.
 *
 * ## What is deliberately *not* excluded
 *
 * The `$Companion` glob was on this list and has been taken off. It reads like more generated
 * noise and is not: this codebase keeps its Room migrations in `NimazDatabase.Companion`, so the
 * pattern was hiding all eighteen of them — 210 lines of the riskiest code in the app, and the
 * one place where a mistake is a crash on launch rather than a wrong screen. A companion object
 * is somewhere people put code, and the report has to say whether that code is tested.
 */
val COVERAGE_EXCLUSIONS: List<String> = listOf(
    "**/R.class",
    "**/R$*.class",
    "**/BuildConfig.*",
    "**/Manifest*.*",
    "**/*Test*.*",
    "**/*ComposableSingletons*.*",
    // Hilt / Dagger / Room generated code.
    "**/di/**",
    "**/*_Factory*.*",
    "**/*_HiltModules*.*",
    "**/*_Impl*.*",
    "**/hilt_aggregated_deps/**",
    "**/dagger/hilt/**",
    "**/*Hilt_*.*",
    // The dead static bridge beside an interface's default methods.
    "**/*\$DefaultImpls*.*",
    // Room entities and the row projections DAOs return: a constructor and generated members.
    "**/*Entity.class",
    "**/*Entity\$*.class",
    "**/*Entities.class",
    "**/*Entities\$*.class",
    "**/*Row.class",
    "**/*Row\$*.class",
)

/**
 * The coverage a module is locked at.
 *
 * A module joins the floor when its tests are written, and from then on the floor is a **gate**:
 * `check` fails if the module falls below it. That is the whole mechanism. Modules are brought up
 * one at a time and locked one at a time, so the ratchet only ever turns one way and a module
 * that has been done cannot quietly rot while the next one is being worked on.
 *
 * Absent floors are not a failure — a module that has not been reached yet simply has no gate,
 * and `moduleCoverage` still reports its number.
 */
abstract class NimazCoverageExtension {
    /** Minimum fraction of lines covered, 0.0..1.0. Unset means this module is not locked yet. */
    abstract val lineFloor: Property<Double>

    /** Minimum fraction of branches covered, 0.0..1.0. Unset means branches are not gated. */
    abstract val branchFloor: Property<Double>

    /**
     * Measure the **ASM-transformed** classes rather than the compiler output.
     *
     * Set this in a module that has `@AndroidEntryPoint` classes a *unit test constructs*, and
     * nowhere else. The Hilt Gradle plugin rewrites such a class through AGP's ASM pipeline — an
     * `@AndroidEntryPoint` service's `onCreate` gains a `super` call — and the test loads the
     * rewritten copy, whose JaCoCo class id does not match the compiler-output one. JaCoCo then
     * discards that class's execution data and reports it as **0% however thoroughly it is
     * tested**, saying so only in a line nobody reads:
     *
     *     [ant:jacocoReport] Execution data for class …/AdhanPlaybackService does not match.
     *
     * The signature is a file at 50% whose *outer* class is at 0% while its nested lambdas report
     * normally. On `:core:audio` it was three services and 537 lines, and the module read 45%
     * against tests that actually cover it.
     *
     * `:app` found this first and fixed it inline, when it was the only module with such a class.
     * `:core:audio` is the second, so the mechanism lives here. It stays **opt-in** rather than
     * automatic: the transformed root also carries the Java that KSP and Dagger generate, which
     * the compiler-output root never did, and naming *both* roots hands JaCoCo two class files
     * per class and aborts the whole report.
     */
    abstract val measureTransformedClasses: Property<Boolean>
}

/** One JaCoCo counter, as a fraction. */
data class CoverageRatio(val covered: Int, val total: Int) {
    val fraction: Double get() = if (total == 0) 1.0 else covered.toDouble() / total
    val percent: String get() = String.format("%.1f%%", fraction * 100)
    override fun toString(): String = "$covered/$total ($percent)"
}

/**
 * The report-level LINE and BRANCH counters of a JaCoCo XML report.
 *
 * Pulled out as a function over a string so the gate's arithmetic — including the two cases that
 * matter, an empty report and a zero denominator — is unit-testable without running Gradle.
 * External DTD loading is off: JaCoCo stamps a `report.dtd` DOCTYPE that is not on disk, and a
 * parser that tries to fetch it either reaches the network or throws.
 */
fun parseJacocoCounters(xml: String): Map<String, CoverageRatio> {
    val factory = DocumentBuilderFactory.newInstance().apply {
        setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        isValidating = false
    }
    val document = factory.newDocumentBuilder().parse(xml.byteInputStream())
    val report = document.documentElement ?: return emptyMap()
    return (0 until report.childNodes.length)
        .mapNotNull { report.childNodes.item(it) as? Element }
        .filter { it.tagName == "counter" }
        .associate { counter ->
            val covered = counter.getAttribute("covered").toIntOrNull() ?: 0
            val missed = counter.getAttribute("missed").toIntOrNull() ?: 0
            counter.getAttribute("type") to CoverageRatio(covered, covered + missed)
        }
}

/**
 * Whether [ratio] clears [floor], and the message if it does not.
 *
 * Separate from the task for the same reason as [parseJacocoCounters]: the interesting cases are
 * "exactly at the floor" and "the report described nothing", and neither is worth a Gradle build
 * to assert.
 */
fun coverageShortfall(
    label: String,
    ratio: CoverageRatio?,
    floor: Double,
    modulePath: String,
): String? = when {
    ratio == null || ratio.total == 0 ->
        "$modulePath: the coverage report describes no $label at all. That reads identically to " +
            "0% and is almost always a class-output path that has moved rather than a module " +
            "with nothing in it."
    ratio.fraction + 1e-9 < floor ->
        "$modulePath: $label coverage ${ratio.percent} is below the floor this module is locked " +
            "at (${String.format("%.1f%%", floor * 100)}), covering $ratio."
    else -> null
}

/**
 * Makes Robolectric-executed code count toward coverage, and gives the module its own report
 * and — once it is locked — its own gate.
 *
 * See [configureRobolectricCoverage] for the first half. The second half is `moduleCoverage`, a
 * JaCoCo report scoped to this module alone, and `coverageFloor`, which reads it back and fails
 * `check` when the module has slipped below what it was locked at.
 *
 * Both are registered only where the `jacoco` plugin is applied, through `withPlugin` rather than
 * eagerly, because each module applies `jacoco` in its own `plugins {}` block — which runs *after*
 * the convention plugin.
 */
internal fun Project.configureModuleCoverage() {
    pluginManager.withPlugin("jacoco") {
        val coverage = extensions.create("nimazCoverage", NimazCoverageExtension::class.java)

        val report = tasks.register<JacocoReport>("moduleCoverage") {
            group = "verification"
            description = "JaCoCo line/branch coverage for this module's own classes."

            // Whichever this module has. An Android library reports through
            // `testDebugUnitTest`; a `kotlin-jvm` module has no variants and so no such task.
            dependsOn(tasks.matching { it.name == "testDebugUnitTest" || it.name == "test" })

            reports {
                xml.required.set(true)
                html.required.set(true)
                csv.required.set(false)
            }
            classDirectories.setFrom(
                if (coverage.measureTransformedClasses.getOrElse(false)) {
                    transformedClassDirs()
                } else {
                    moduleClassDirs()
                }
            )
            sourceDirectories.setFrom(
                files("src/main/kotlin", "src/main/java")
            )
            executionData.setFrom(moduleExecutionData())
        }

        val gate = tasks.register("coverageFloor") {
            group = "verification"
            description = "Fails if this module has slipped below the coverage it is locked at."
            dependsOn(report)

            // Plain values captured here, never a Project or a task: this build runs with
            // `configuration-cache=problems=fail`, and a task action that reads a script or
            // project object fails at cache-storage time rather than at execution.
            val modulePath = path
            val xml = layout.buildDirectory.file("reports/jacoco/moduleCoverage/moduleCoverage.xml")
            val lineFloor = coverage.lineFloor
            val branchFloor = coverage.branchFloor
            inputs.file(xml)

            doLast {
                if (!lineFloor.isPresent && !branchFloor.isPresent) return@doLast
                val counters = parseJacocoCounters(xml.get().asFile.readText())
                val failures = listOfNotNull(
                    lineFloor.orNull?.let {
                        coverageShortfall("line", counters["LINE"], it, modulePath)
                    },
                    branchFloor.orNull?.let {
                        coverageShortfall("branch", counters["BRANCH"], it, modulePath)
                    },
                )
                check(failures.isEmpty()) {
                    failures.joinToString(
                        separator = "\n",
                        postfix = "\n\nRaise the tests, not the floor: it is a ratchet, and a " +
                            "module that has been brought up to it is not meant to come back down.",
                    )
                }
            }
        }

        tasks.matching { it.name == "check" }.configureEach { dependsOn(gate) }
    }
}

/**
 * This module's compiler output, minus [COVERAGE_EXCLUSIONS].
 *
 * **Compiler output only.** AGP also writes an ASM-transformed copy of the same classes under
 * `intermediates/classes/debug`; naming both roots hands JaCoCo two class files per class and it
 * aborts the whole report with *"Can't add different class with same name"* as soon as the two
 * copies differ. The three globs below are the three places a Nimaz module's classes can be —
 * current AGP, older AGP, and a `kotlin-jvm` module — and they never overlap in one build.
 */
private fun Project.moduleClassDirs(): FileCollection =
    fileTree(layout.buildDirectory) {
        include(
            "intermediates/built_in_kotlinc/debug/**/classes/**",
            "tmp/kotlin-classes/debug/**",
            "classes/kotlin/main/**",
        )
        exclude(COVERAGE_EXCLUSIONS)
    }

/**
 * This module's classes **after** AGP's ASM transform, minus [COVERAGE_EXCLUSIONS].
 *
 * A single complete root — it carries the compiler output too — so the duplicate-class problem
 * that rules out naming both never arises. See [NimazCoverageExtension.measureTransformedClasses]
 * for when a module should measure this instead.
 */
private fun Project.transformedClassDirs(): FileCollection =
    fileTree(layout.buildDirectory.dir("intermediates/classes/debug/transformDebugClassesWithAsm/dirs")) {
        exclude(COVERAGE_EXCLUSIONS)
    }

/** Wherever this module's test task wrote its execution data — again, all three spellings. */
private fun Project.moduleExecutionData(): FileCollection =
    fileTree(layout.buildDirectory) {
        include(
            "jacoco/testDebugUnitTest.exec",
            "jacoco/test.exec",
            "outputs/unit_test_code_coverage/**/*.exec",
        )
    }
