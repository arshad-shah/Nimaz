plugins {
    id("nimaz.android.library")
    id("nimaz.android.hilt")
    id("nimaz.android.compose")
    alias(libs.plugins.kotlin.serialization)
    jacoco
}

android {
    namespace = "com.arshadshah.nimaz.feature.widget"
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
// It keeps only the strings *nothing else* uses. The first attempt moved all 27 the widget code
// references and broke `:app`: twelve of them — the short prayer names, the khatam labels, the
// next-prayer caption — are also read by `WidgetsScreen`'s previews, `KhatamCards` and
// `PrayerTrackerDayCard`. "Used by the widget" is not "used only by the widget", and the compiler
// is what said so. Those twelve went back to `:core:ui`, which is why this depends on it despite
// drawing none of the design system.
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
    implementation(libs.hilt.work)

    testImplementation(libs.junit)
    testImplementation(libs.google.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
