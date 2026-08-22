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
