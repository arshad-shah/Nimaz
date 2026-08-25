plugins {
    id("nimaz.android.library")
    id("nimaz.android.hilt")
    alias(libs.plugins.kotlin.serialization)
    jacoco
}

android {
    namespace = "com.arshadshah.nimaz.core.data"

    testOptions {
        unitTests {
            // QaidaRepositoryImplTest and UserDataRepositoryImplTest are Robolectric, and several
            // impls log through `android.util.Log` — a stub that *throws* on the JVM classpath
            // unless this is set. See core/data/src/test/resources/robolectric.properties for the
            // SDK pin, which is a resource of the source set and does not travel with its tests.
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }
}

/**
 * Locked (#611). The repositories, the device-to-device sync, the adhan file store and the
 * platform adapters are tested, and `check` now fails if that stops being true — see
 * `COVERAGE_EXCLUSIONS` and `coverageFloor` in `build-logic` for what is measured and how the
 * ratchet works.
 *
 * **Both floors are the agreed 80%**, set at the standard rather than at whatever the module
 * reports today (88.1% lines / 84.6% branches at the time of locking). A floor pinned to the
 * current number turns every unrelated refactor into a coverage failure. There is no Compose in
 * this module, so there is no `$dirty` bitmask to soften the branch floor for: every branch here
 * is one somebody wrote.
 *
 * One thing is permanently at zero and `COVERAGE_EXCLUSIONS` was **not** widened for it. The
 * exclusion list is shared with every locked module, so widening it moves their numbers too.
 * `AdhanAudioManager.downloadFile` opens a [java.net.HttpURLConnection] against the CDN URL baked
 * into `AdhanSound`, and there is no seam: exercising it would need either a real network request
 * from a unit test or a JVM-wide `URLStreamHandlerFactory`, which is global, one-shot per process
 * and would make every other Robolectric class in the module order-dependent. The transfer and its
 * retry loop therefore stay uncovered — about 75 lines and 50 branches — and the 80% is made up
 * elsewhere. Everything the manager does *around* the transfer is covered, including the generated
 * chime, the audio-magic-byte validation and the URL-version invalidation. See `docs/TESTING.md`.
 */
nimazCoverage {
    lineFloor.set(0.80)
    branchFloor.set(0.80)
}

// The 18 repository implementations that map onto `:core:domain`'s interfaces, plus the platform
// adapters behind the domain ports: locale, compass, haptics, device location, strings, the AI
// client and the announcement mapper.
//
// Four things deliberately stay in `:app`, each for a reason that is not obvious:
//   · `LibraryRepositoryImpl` reads `R.raw.aboutlibraries`, which the AboutLibraries plugin
//     generates from the *applying* project's runtime classpath — anywhere but `:app` produces a
//     shorter licence list, silently, and no test catches it.
//   · `NimazMessagingService` is a `<service>` in the manifest.
//   · `WorkManagerWidgetRefresher` imports the widget workers, so it belongs to `:feature:widget`.
//   · `ServiceAdhanDownloader`, split out of `AndroidAppLocale` here, drives a foreground service.
dependencies {
    api(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.adhan)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.play.integrity)
    implementation(libs.play.services.location)
    // Nearby Connections, for `data/sync` — device-to-device transfer of the user's own records.
    // The slice arrives here rather than in `:feature:settings` for a reason no amount of reading
    // the screen would suggest: it imports **21 DAOs and 14 entities** directly, and
    // `:core:database` is not on a feature module's classpath.
    implementation(libs.play.services.nearby)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)

    // RecordingWidgetRefresher and the other fakes of :core:domain ports.
    testImplementation(testFixtures(project(":core:domain")))

    testImplementation(libs.junit)
    testImplementation(libs.google.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.ktor.client.mock)
    // A real org.json. `AsmaUlHusnaRepositoryImpl` and `ProphetRepositoryImpl` parse their JSON
    // array columns with `org.json.JSONArray`, which on a JVM unit-test classpath is the stubbed
    // android.jar: the constructor is a no-op, `length()` returns 0, and the tests fail asserting
    // an empty list rather than erroring. `:app` has carried this same line since its tests were
    // written; it did not travel with them. Verified rather than guessed — a probe printed
    // `length=0 impl=…/transformed/android.jar` here against `length=2 impl=…/json-20231013.jar`
    // in `:app`.
    testImplementation(libs.json)

}
