plugins {
    id("nimaz.android.library")
    id("nimaz.android.hilt")
    // Merged into :app:jacocoTestReport via `coverageModules`. A module that leaves :app without
    // this makes the reported coverage rise by measuring less.
    jacoco
}

android {
    namespace = "com.arshadshah.nimaz.core.notifications"

    testOptions {
        unitTests {
            // Everything here is asserted through Robolectric's shadow `AlarmManager` and shadow
            // `NotificationManager`, and the notifications it builds read real resources.
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

/**
 * Locked. See `COVERAGE_EXCLUSIONS` and `coverageFloor` in `build-logic` for what is measured and
 * how the ratchet works.
 *
 * **80/80, at 95.5% lines and 80.8% branches** — the highest line coverage of any module in the
 * repo, which is not a coincidence: everything here is invisible when it breaks. A prayer alarm
 * that is never armed produces no crash, no error state and no screen that says one was expected,
 * so the whole surface was brought up to a floor before it was a module.
 *
 * `PrayerAlarmReceiver` is `@AndroidEntryPoint` and its tests construct it, so this module
 * measures the **ASM-transformed** classes like `:core:audio` and `:app` — see
 * `NimazCoverageExtension.measureTransformedClasses`. Without that the receiver reads 0% however
 * thoroughly it is tested, which here would be 499 of the module's 1,170 lines.
 */
nimazCoverage {
    measureTransformedClasses.set(true)
    lineFloor.set(0.80)
    branchFloor.set(0.80)
}

// The prayer-notification machinery: what arms every alarm the app lives by, what answers one
// when it fires, and the text it puts on screen.
//
// ## This overturns a decision `docs/ARCHITECTURE.md` §2 recorded
//
// That section said these files stay in `:app` permanently, and the reasoning was sound at the
// time: they did not go to `:feature:prayer` because their consumers are the settings surface and
// `AppInitializer`, so moving them there would have created the `:feature:settings` ->
// `:feature:prayer` edge #571 forbids. **That argument does not reach a `:core:*` module** — a
// feature may depend on one freely — and `:app` was only ever the place they had not been moved
// out of. The doc is rewritten in the same change rather than left contradicting the code.
//
// ## What had to happen first
//
//   · **The channel ids.** `AdhanPlaybackService` read `PrayerNotificationScheduler`'s directly
//     while `BootReceiver` started that service — a cycle between this module and `:core:audio`
//     that `moduleBoundary` cannot catch, both sides being `:core:*`. They are `NimazChannels` in
//     `:core:common` now, below both.
//   · **The `BootReceiver` split.** It called `WidgetUpdateScheduler.ensureScheduled`, and a
//     `:core:*` module naming that is the `:core:*` -> `:feature:*` edge `moduleBoundary` *does*
//     fail on. Alarm delivery is `PrayerAlarmReceiver`, which names nothing in `:feature:widget`;
//     boot recovery stayed in `:app` with the call.
//   · **The notification icon.** All of this drew `AppR.drawable.ic_stat_nimaz`, the one drawable
//     `:app` had. It went to `:core:ui` with `:core:audio`.
//
// ## What stays in `:app`
//
// `BootReceiver` (above), `InAppUpdateManager` (it holds an `Activity` and drives the Play
// in-app update flow) and `core/init`, which is a composition root. Those three are the whole of
// what §2 claimed for this package, and they are the part of the claim that was right.
dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(project(":core:datastore"))
    // For `R` — the notification icon, the khatam and worship channel names, and the reminder
    // copy `NotificationContentHelper` and `WorshipReminderContent` resolve.
    implementation(project(":core:ui"))
    // `PrayerAlarmReceiver` plays the adhan through `AdhanPlaybackService` and asks
    // `AdhanAudioManager` whether the file is there. One-way: nothing in `:core:audio` names
    // anything here, which is what the channel-id move bought.
    implementation(project(":core:audio"))

    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.google.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
    testImplementation(testFixtures(project(":core:domain")))
}
