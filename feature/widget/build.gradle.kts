plugins {
    // library + compose + hilt in one id. This module listed the three separately because it was
    // the first feature module and `nimaz.android.feature` had no user yet; with a second and
    // third arriving in PR 14, the point of the shared id — that a feature module's plugin set
    // cannot drift from its siblings — starts applying.
    id("nimaz.android.feature")
    alias(libs.plugins.kotlin.serialization)
    jacoco
}

android {
    namespace = "com.arshadshah.nimaz.feature.widget"

    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }

    lint {
        // `RestrictedApi`, 18 times, all of them Glance's `ColorProvider(@ColorRes)`:
        //
        //   ColorProviderKt.ColorProvider can only be called from within the same library group
        //   (referenced groupId=androidx.glance from groupId=nimaz.feature)
        //
        // Not a change in what the code does. These exact calls produced no lint error while
        // `widget/` lived in `:app`, because the check compares the *Maven group* of the calling
        // project and an application module has none to compare — moving the same source into a
        // library gives it one, and the comparison starts running. Nothing about the API usage
        // moved with it.
        //
        // Disabled rather than worked around because the alternatives are worse. Glance's
        // unrestricted overloads take a `Color` or a day/night pair, so avoiding the restricted
        // one means abandoning `widget_colors.xml` — and with it the `values-night` variant that
        // gives every widget its dark theme. That is a real behaviour change traded for a lint
        // clean, in a module whose runtime verification cannot be done here.
        //
        // Scoped to this module. If Glance ever publishes a public resource-based ColorProvider,
        // delete this and use it.
        disable += "RestrictedApi"
    }
}

/**
 * Locked. See `COVERAGE_EXCLUSIONS` and `coverageFloor` in `build-logic` for what is measured and
 * how the ratchet works.
 *
 * **80/80, and nothing here is permanently uncoverable.** That was not obvious going in: a Glance
 * composable renders to `RemoteViews`, not to a semantics tree, so none of the campaign's
 * `onNodeWithText` machinery reaches it, and `androidx.glance:glance-appwidget-testing` is
 * assertion-only — its unit-test DSL has no `performClick`. What does work is
 * `GlanceAppWidget.compose(context, state = …)`: it runs the widget's real `provideGlance` against
 * a supplied state and hands back the `RemoteViews` the launcher would get, which inflate under
 * Robolectric into an ordinary view tree. See `WidgetRenderer` in the test source set and
 * `docs/TESTING.md`.
 *
 * The one thing that route cannot do is fire a tap: a Glance `clickable { }` is a lambda action
 * resolved by the AppWidget host. `togglePrayerStatus` — the only widget action that writes user
 * data — is therefore `internal` rather than `private` and called directly by its test.
 */
nimazCoverage {
    lineFloor.set(0.80)
    branchFloor.set(0.80)
}

// The six Glance widgets, their receivers, the tick receiver and the six refresh Workers.
//
// **First of the eleven feature modules, deliberately.** `widget/` is the only top-level package
// with zero imports from `presentation/`, so it proves the feature-module pipeline — convention
// plugins, Hilt slicing, manifest merging — without any risk of breaking a screen. If any of that
// is wrong, it surfaces here cheaply (#564).
//
// It takes the widget resources with it. They stayed in `:app` through PR 10 of #551 because they
// are widget assets rather than design system: `res/xml/*_widget_info.xml` (the provider
// descriptors the manifest points at), `res/drawable/ic_widget_*`, the preview layouts, and
// `values/widget_colors.xml` with its `values-night` variant. This is the module that was waiting
// for them.
//
// It keeps only the strings *nothing else* uses. The first attempt moved every string the widget
// code touches and broke `:app`: the short prayer names, the khatam labels and the next-prayer
// caption are also read by `WidgetsScreen`'s widget gallery, `KhatamCards` and
// `PrayerTrackerDayCard`. "Used by the widget" is not "used only by the widget", and the compiler
// is what said so.
//
// The split, counted rather than remembered: this module **references 33** string resources
// across its Kotlin, its `res/xml` descriptors and its manifest; it **declares 17** — every one of
// which nothing outside it uses — and resolves the other **16** from `:core:ui`. That is why it
// depends on `:core:ui` despite drawing none of the design system.
//
// Five of those 16 (`widget_*_description`) have no consumer outside this module and could move.
// They stay deliberately: `khatam_widget_description` is read by `WidgetsScreen` and cannot, so
// moving the rest would split one set of six provider descriptions across two modules to no end.
dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:datastore"))
    // For the twelve strings shared with the widget-settings previews in `:app`. Not for the
    // design system: a Glance widget draws from its own `widget_colors.xml`, not the palette.
    implementation(project(":core:ui"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    // The widgets persist their state as JSON through Glance's GlanceStateDefinition.
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)

    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
    implementation(libs.work.runtime.ktx)
    // The six refresh Workers are @HiltWorker. `nimaz.android.hilt` deliberately leaves this
    // processor to the module that needs it, and **omitting it fails at runtime, not at build
    // time**: without it no `_AssistedFactory` is generated, `HiltWorkerFactory` returns null for
    // these classes, and WorkManager falls back to reflecting a `(Context, WorkerParameters)`
    // constructor that an @AssistedInject worker does not have. It compiled, `:feature:widget:check`
    // was green, and six `WidgetWorkersTest` cases died on the emulator with NoSuchMethodException.
    // `HiltWorkerProcessorTest` now fails the build instead.
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.google.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.androidx.work.testing)
}
