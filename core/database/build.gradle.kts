plugins {
    id("nimaz.android.library")
    // Brings KSP with it, which the Room compiler below also needs. `UserDataMigrator` and
    // `ContentSearchIndex` are `@Inject`-constructed and `UserDataMigrator` takes
    // `@ApplicationContext`, so the annotations have to resolve here rather than only in `:app`.
    id("nimaz.android.hilt")
    jacoco
}

android {
    namespace = "com.arshadshah.nimaz.core.database"

    testOptions {
        unitTests {
            // Both are inherited expectations of the tests that moved here from `:app`, which
            // sets the same pair. Without `isReturnDefaultValues`, `android.util.Log` on the JVM
            // classpath is a stub whose every method *throws* — and `ContentSearchIndex.probe`
            // wraps its body in `runCatching`, so a stub throwing inside it is swallowed and the
            // index reports `Absent` instead of `Mismatched`. Two tests failed that way, both
            // asserting on the wrong thing rather than erroring, which is the worst shape for a
            // stub to fail in.
            isReturnDefaultValues = true
            // AyahWithTextViewTest is Robolectric and reads merged resources.
            isIncludeAndroidResources = true
        }
    }

    ksp {
        // Follows the Room compiler. This is the module the compiler now runs in, so leaving the
        // argument on `:app` would make it inert and stop the schemas being exported at all —
        // and an un-exported schema is not a build failure, it is a missing file that
        // MigrationTestHelper discovers at runtime.
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

// Both Room databases, their entities, DAOs and migrations, the exported `schemas/`, and the two
// pieces that cannot be separated from them: the user-data slice (`UserDataMigrator` reads
// `ContentArtifactStore`) and the content-artifact installer that replaces the shipped database
// before Room opens it.
//
// It does **not** own `BuildConfig.CONTENT_ARTIFACT_SHA256` — a library's BuildConfig does not
// carry the app's fields — so `ContentArtifactInstaller` takes the installed hash as a
// constructor parameter and `:app` supplies it. See `DatabaseModule`.
dependencies {
    api(project(":core:domain"))
    implementation(project(":core:common"))

    implementation(libs.androidx.core.ktx)
    api(libs.room.runtime)
    api(libs.room.ktx)
    ksp(libs.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.google.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
    // org.json is an Android stub on the JVM classpath, so a plain unit test cannot
    // parse the exported Room schemas with it — the same reason
    // ArabicSearchNormaliserTest in :core:domain uses kotlinx.serialization.
    testImplementation(libs.kotlinx.serialization.json)
}
