plugins {
    id("nimaz.jvm.library")
    // `UseCaseModule` and its two siblings live here, and Hilt only aggregates a `@Module` from a
    // project that runs its processor. Without these two lines `:core:domain` compiles, `:app`
    // compiles, and `:app:hiltJavaCompileDebug` then fails with sixteen `[Dagger/MissingBinding]`
    // errors — the same shape as the missing `@HiltWorker` processor in PR 13 and the missing
    // `kotlin-serialization` plugin in PR 19, and the third time in this epic that a forgotten
    // plugin has surfaced somewhere other than the module that forgot it.
    alias(libs.plugins.ksp)
    // Fakes that both sides of the seam need — :core:domain's own use-case tests and :app's
    // ViewModel tests — are published from `src/testFixtures` rather than duplicated.
    `java-test-fixtures`
    // Produces build/jacoco/test.exec, which :app:jacocoTestReport merges. Without it this
    // module would leave :app's coverage report the moment its code did.
    jacoco
}

/**
 * Locked. The domain layer is the one every other module compiles against, so a regression here
 * is a regression everywhere; `check` now fails if its coverage slips — see `COVERAGE_EXCLUSIONS`
 * and `coverageFloor` in `build-logic` for what is measured and how the ratchet works.
 *
 * **80/80, both floors, unlike `:feature:quran`.** That module sets its branch floor at 60%
 * because the Compose compiler emits an unreachable `$dirty` bitmask branch per parameter of
 * every restartable composable. There is no Compose here — no AGP at all — so every branch in
 * this module is one somebody wrote and a test can take both sides of. The standard applies
 * unmodified.
 */
nimazCoverage {
    lineFloor.set(0.80)
    branchFloor.set(0.80)
}

// The pure layer. No AGP, no Android SDK on the classpath — `import android.content.Context` in a
// use case is a compile error here, which is the whole reason this module exists. The
// `domainDependencyGuard` task the convention plugin wires into `check` keeps that true after a
// future `implementation(...)` line, which the compiler alone would not.
dependencies {
    api(libs.javax.inject)
    // `UseCaseModule` lives beside the use cases it provides, as of PR 22 of #551. `hilt-core` is
    // the JVM half of Hilt — `@InstallIn`, `SingletonComponent`, and Dagger's `@Module`/`@Provides`
    // transitively — so it carries no Android, and `androidFreeClasspath` stays green. The domain
    // layer already depended on `javax.inject`; this only lets it say *where* its bindings install.
    api(libs.hilt.core)
    ksp(libs.hilt.compiler)
    api(libs.kotlinx.coroutines.core)

    // Prayer-time calculation is domain logic, and adhan2 is a pure-Kotlin library with a JVM
    // variant, so the calculator lives here rather than behind a port.
    implementation(libs.adhan)
    implementation(libs.kotlinx.datetime)

    testFixturesApi(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.google.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlinx.serialization.json)
}
