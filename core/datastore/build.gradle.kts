plugins {
    id("nimaz.android.library")
    id("nimaz.android.hilt")
    // AnnouncementLocalDataSource persists its dismissal state as JSON via @Serializable.
    alias(libs.plugins.kotlin.serialization)
    jacoco
}

android {
    namespace = "com.arshadshah.nimaz.core.datastore"
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
}
