plugins {
    id("nimaz.android.library")
    id("nimaz.android.compose")
}

android {
    namespace = "com.arshadshah.rail"
}

// Rail — scrollbars for Jetpack Compose — vendored from arshad-shah/rail so it can be validated
// inside the real app before it is published anywhere. That repository has no remote Maven
// publication and is private, so neither a coordinate nor a git submodule would resolve on CI;
// a source copy is the only form every workflow (PR checks, internal builds) can build.
//
// `src/main` is a verbatim copy of `rail/src/main` at the commit recorded in `RAIL_VERSION`.
// Do not edit it here: fix it upstream and re-copy, so the two never drift. Rail's own suite
// (Robolectric + instrumented) lives and runs upstream; nothing is duplicated here.
//
// Deliberately Compose foundation/UI/animation only — no Material — exactly as upstream. The
// Nimaz look (theme colours, sizes) is applied one layer up by `NimazScrollbar` in `:core:ui`,
// which is the only module that depends on this one.
dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.animation)
}
