plugins {
    id("nimaz.jvm.library")
    // Fakes that both sides of the seam need — :core:domain's own use-case tests and :app's
    // ViewModel tests — are published from `src/testFixtures` rather than duplicated.
    `java-test-fixtures`
    // Produces build/jacoco/test.exec, which :app:jacocoTestReport merges. Without it this
    // module would leave :app's coverage report the moment its code did.
    jacoco
}

// The pure layer. No AGP, no Android SDK on the classpath — `import android.content.Context` in a
// use case is a compile error here, which is the whole reason this module exists. The
// `domainDependencyGuard` task the convention plugin wires into `check` keeps that true after a
// future `implementation(...)` line, which the compiler alone would not.
dependencies {
    api(libs.javax.inject)
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
