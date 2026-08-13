// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    dependencies {
        // AGP 9 provides built-in Kotlin support but pins the Kotlin Gradle plugin
        // (and KSP) to an older baseline (KGP 2.2.10). Force them up to the versions
        // used by the Compose / serialization compiler plugins so the Kotlin compiler
        // and its plugins stay on the same version. Keep these in sync with the
        // `kotlin` and `ksp` entries in gradle/libs.versions.toml.
        // See https://developer.android.com/build/migrate-to-built-in-kotlin
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.21")
        classpath("com.google.devtools.ksp:symbol-processing-gradle-plugin:2.3.9")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    // com.android.test backs :baselineprofile. Declared here because AGP is already on
    // the build classpath, so requesting it with a version in the module fails with
    // "already on the classpath with an unknown version".
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.baselineprofile) apply false
    // Note: the standalone org.jetbrains.kotlin.android plugin is intentionally NOT
    // applied — AGP 9 compiles Kotlin through its built-in Kotlin support.
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.about.libs.plugin) apply false
    // Put the Firebase plugins on the classpath. The app module applies them
    // conditionally (only when google-services.json is present) so PR-check and
    // local debug builds without the config file still succeed.
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.firebase.perf) apply false
}
