plugins {
    id("nimaz.android.library")
    id("nimaz.android.hilt")
    // Merged into :app:jacocoTestReport via `coverageModules`. A module that leaves :app without
    // this makes the reported coverage rise by measuring less.
    jacoco
}

android {
    namespace = "com.arshadshah.nimaz.core.audio"

    testOptions {
        unitTests {
            // The three services are built with `Robolectric.buildService` and what they produce
            // is a notification, so the tests read real Android resources back.
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

/**
 * Locked. See `COVERAGE_EXCLUSIONS` and `coverageFloor` in `build-logic` for what is measured and
 * how the ratchet works.
 *
 * **70 lines / 50 branches — the lowest floors in the repo, and measured rather than conceded.**
 * Nothing here draws, so none of it is the unreachable Compose `$dirty` branch that makes
 * `:feature:quran` soften. What it is instead is two managers whose remaining code needs hardware
 * the JVM does not have, and both gaps were documented long before this module existed:
 *
 *   · **`QuranAudioManager`** (294/497 lines, 81/248 branches). Its `Player.Listener` fires on
 *     ExoPlayer reaching `STATE_ENDED` or transitioning items, which needs a player actually
 *     decoding audio — 180 branches of it. `docs/TESTING.md` lists it under "deliberately left at
 *     zero" for exactly that reason.
 *   · **`AdhanAudioManager`** (211/335). `downloadFile` opens a `HttpURLConnection` against the
 *     CDN URL baked into `AdhanSound`, with no seam; `:core:data`'s build file recorded that while
 *     the class lived there.
 *
 * Everything else clears comfortably — the three services are at 80%, 85% and 86% lines once the
 * ASM-transformed classes are measured. Those two are the whole gap, and the floors are set where
 * the module honestly sits rather than where the others do. **It is still a ratchet**: put a seam
 * behind either one and the floor goes up with it.
 */
nimazCoverage {
    // **Measure the ASM-transformed classes, not the compiler output.** The three services are
    // `@AndroidEntryPoint`, so the Hilt Gradle plugin rewrites them and the tests load the
    // rewritten copies — whose JaCoCo class ids do not match the compiler output, which makes
    // JaCoCo discard their execution data and report them at 0% however thoroughly they are
    // tested. Without this the module reads 45.1% lines against tests that cover it properly.
    // `:app` hit this first; see `NimazCoverageExtension.measureTransformedClasses`.
    measureTransformedClasses.set(true)
    lineFloor.set(0.70)
    branchFloor.set(0.50)
}

// Every engine that makes a sound, and the download pipeline behind one of them: the Quran
// recitation session, the adhan player, the adhan downloader (service + WorkManager fallback),
// and `AdhanAudioManager`/`AdhanSound`, which came from `:core:data` so that all of it is in one
// place rather than split across two modules by an accident of extraction order.
//
// ## The cycle this module could not have existed with
//
// `AdhanPlaybackService` read `PrayerNotificationScheduler.CHANNEL_ID_ADHAN`, which is a
// `:core:audio` -> `:core:notifications` edge; `BootReceiver` starting the two adhan services is
// the same edge back. Gradle fails on a circular project dependency, and `moduleBoundary` would
// not have caught it first, because both sides are `:core:*`. The channel ids moved to
// `NimazChannels` in `:core:common` — below both — before either module was created.
//
// ## The two couplings that were unpicked
//
//   · `QuranAudioService` built its content `PendingIntent` with
//     `Intent(this, MainActivity::class.java)`. It resolves the launcher *component* now, the way
//     `:feature:widget` does, and sets the same action on it.
//   · All three services drew `AppR.drawable.ic_stat_nimaz`. That icon is `:core:ui`'s now — it
//     is the only drawable `:app` had, and `:core:notifications` needs it next.
//
// ## Why it depends on `:core:ui`
//
// For `R` alone: the notification icon and about a dozen `adhan_download_*` strings, which
// `AdhanDownloadService` already read from `com.arshadshah.nimaz.core.ui.R` before the move. That
// puts Compose on the compile classpath unused — the same trade `:core:share` makes, and for the
// same reason: the alternative is duplicating strings across five translations.
dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(project(":core:datastore"))
    // For `R` — the notification icon and the adhan-download strings. See the note above.
    implementation(project(":core:ui"))

    implementation(libs.androidx.core.ktx)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)

    implementation(libs.work.runtime.ktx)
    implementation(libs.hilt.work)
    // `AdhanDownloadWorker` is a `@HiltWorker`, and **the processor has to be applied in the
    // module that declares one**. Omitting it compiles fine — `@HiltWorker` is only an annotation
    // — and fails at *run time* with `NoSuchMethodException`, because no `_AssistedFactory` is
    // generated and `HiltWorkerFactory` has no entry for the class. `HiltWorkerProcessorTest` in
    // `:app` exists because that shipped once.
    ksp(libs.hilt.work.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.google.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.androidx.work.testing)
    testImplementation(testFixtures(project(":core:domain")))
}
