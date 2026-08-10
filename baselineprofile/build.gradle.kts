plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.arshadshah.nimaz.baselineprofile"
    compileSdk = 37

    defaultConfig {
        // 29 matches the app. A baseline profile is generated on one device and
        // consumed by all of them, so this only needs to be a version the app runs on.
        minSdk = 29
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Profile generation must run against a non-debuggable, minified build: a debuggable
    // app is never AOT-compiled from a profile, so a profile collected from one would
    // describe code paths the release build does not take.
    targetProjectPath = ":app"

    // A managed device, so generation does not depend on whoever runs it having an
    // emulator open. `./gradlew :app:generateBaselineProfile` boots this headlessly.
    testOptions.managedDevices.localDevices.create("pixel6Api34") {
        device = "Pixel 6"
        apiLevel = 34
        systemImageSource = "aosp"
    }
}

baselineProfile {
    managedDevices += "pixel6Api34"
    // Off: connected devices are a fallback for local runs, and leaving it on makes a
    // missing emulator look like a plugin failure.
    useConnectedDevices = false
}

dependencies {
    implementation(libs.androidx.junit)
    implementation(libs.androidx.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}
