plugins {
    // `nimaz.android.library` alone: no Compose, no Hilt. Not one of the five files here imports
    // Compose — this is a Canvas-and-Intent rendering service — and `ContentShareManager` is an
    // object taking `Context` parameters rather than an injected class.
    id("nimaz.android.library")
    // Merged into :app:jacocoTestReport via `coverageModules`. A module that leaves :app without
    // this makes the reported coverage rise by measuring less.
    jacoco
}

android {
    namespace = "com.arshadshah.nimaz.core.share"

    testOptions {
        unitTests {
            // `ShareCardRenderer` paints into a real `android.graphics.Canvas` and reads three
            // fonts and 36 strings out of merged resources; `ContentShareManager` builds real
            // `Intent`s. Both need Robolectric with resources, and `src/test/resources/
            // robolectric.properties` pins the SDK and the Application class.
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

/**
 * Locked. See `COVERAGE_EXCLUSIONS` and `coverageFloor` in `build-logic` for what is measured and
 * how the ratchet works.
 *
 * **80/80.** Nothing here is a composable, so the module carries none of the unreachable `$dirty`
 * bitmask branches that make `:feature:quran` soften its branch floor — the reason `:core:ui`
 * clears 80 by half a point does not apply once the Canvas engine is measured on its own.
 */
nimazCoverage {
    lineFloor.set(0.80)
    branchFloor.set(0.80)
}

// Sharing: the branded-card `Canvas` renderer, the domain-model-to-`Shareable` builders, the
// `Intent` wrappers and the QR encoder. 1,019 lines that lived in `:core:ui`.
//
// **It was never part of the design system.** Not one of the five files imports Compose; they
// were in `:core:ui` because that is where the strings and the fonts are, which is a dependency,
// not a membership. Sharing is a *consumer* of the design system rather than a piece of it, and
// with this module out, `:core:ui` stops carrying a 461-line `Canvas` engine.
//
// Five feature modules already call it — `:feature:quran`, `:feature:content`, `:feature:about`,
// `:feature:tools`, `:feature:prayer` — so the reuse case is proven rather than speculative. The
// package name is unchanged (`com.arshadshah.nimaz.core.share`), so every call site's import
// reads exactly as it did.
//
// ## Why it still depends on `:core:ui`
//
// The one judgement call. `ShareCardRenderer` reads three fonts (`amiri_regular`,
// `outfit_variable`, `plus_jakarta_sans_variable`) and 36 string resources, and **not all of those
// strings are share-specific**: `dua_reader_source_label`, `hadith_narrated_by_format`,
// `settings_value_with_qualifier`, `zakat_rate_subtitle`, `zakat_below_nisab_subtitle` and the
// `zakat_status_*` family are shared with feature screens. Splitting them would duplicate copy
// that has to stay in sync across five translations, and duplicating three font files is worse.
//
// So Compose lands on this module's compile classpath without being used. That costs nothing at
// runtime and little at build time, since `:core:ui` is built anyway — and the direction is still
// the right one: `:core:ui` does not depend on `:core:share`, and never should.
dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:common"))
    // For `R` — the fonts and the 36 strings the card renderer draws. See the note above.
    implementation(project(":core:ui"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.zxing.core)

    testImplementation(libs.junit)
    testImplementation(libs.google.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
    testImplementation(testFixtures(project(":core:domain")))
}
