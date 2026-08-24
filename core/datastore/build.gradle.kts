plugins {
    id("nimaz.android.library")
    id("nimaz.android.hilt")
    // AnnouncementLocalDataSource persists its dismissal state as JSON via @Serializable.
    alias(libs.plugins.kotlin.serialization)
    jacoco
}

android {
    namespace = "com.arshadshah.nimaz.core.datastore"

    testOptions {
        unitTests {
            // The DataStore round-trip tests run against a real Context under Robolectric.
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

/**
 * Locked. See `COVERAGE_EXCLUSIONS` and `coverageFloor` in `build-logic` for what is measured.
 *
 * **80/80, both floors** — nothing here draws.
 *
 * `PreferencesDataStore` was 329 lines at 0%, for a structural reason worth remembering: no
 * screen constructs it (they read through a `SettingsSeams` interface, by design), so nothing in
 * a test did either. "Nothing constructs it" is a design success and a coverage blind spot at the
 * same time.
 */
nimazCoverage {
    lineFloor.set(0.80)
    branchFloor.set(0.80)
}

// The three DataStore files and everything that reads or writes them: `PreferencesDataStore`
// (990 LOC, ~109 key literals), its `PreferenceCodec` type registry, the prayer-notification
// preference migration, the announcement store, and `DeviceIdProvider` — which owns
// `nimaz_ai_device` and would otherwise leave SUB-06's subject split across two modules.
//
// It implements the eleven `SettingsSeams` interfaces, all of which live in `:core:domain`, so
// every feature module can depend on the seam it needs without seeing this implementation.
dependencies {
    api(project(":core:domain"))

    implementation(libs.androidx.core.ktx)
    api(libs.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.google.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)

    // `PreferencesDataStore` reaches its file through a `Context` extension delegate, so the only
    // way to exercise it is against a real one. Robolectric gives a sandboxed files directory per
    // test class, which is what lets a fresh store assert a first-run default.
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
}
