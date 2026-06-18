plugins {
    alias(libs.plugins.android.application)
    // AGP 9 compiles Kotlin via built-in support; the standalone kotlin.android
    // plugin must not be applied.
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-parcelize")
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.about.libs.plugin)
    jacoco
}

jacoco {
    toolVersion = "0.8.12"
}

// Firebase (Crashlytics + Analytics) is configured via google-services.json,
// which CI injects from secrets only for the release/deploy build. PR checks and
// local debug builds run without it, so apply the Google plugins only when the
// file is present — the google-services plugin otherwise fails the build. The
// Firebase SDK calls in the app are guarded to no-op when Firebase is not
// initialized, so builds without the config still run correctly (just without
// crash/analytics reporting).
if (file("google-services.json").exists()) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
    apply(plugin = libs.plugins.firebase.crashlytics.get().pluginId)
    apply(plugin = libs.plugins.firebase.perf.get().pluginId)
}

android {
    namespace = "com.arshadshah.nimaz"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.arshadshah.nimaz"
        minSdk = 29
        targetSdk = 36
        // Source of truth for the app version. CI bumps these at build time and
        // pushes the change back to dev (with a bypass GitHub App token) after a successful
        // deploy, so the committed baseline stays in sync for the next build.
        versionCode = 317
        versionName = "3.0.17"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Room schema export
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_FILE") ?: "keystore.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            // Required so Robolectric can read merged Android resources/manifest,
            // which the Compose UI tests for the atoms rely on.
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

kotlin {
    compilerOptions {
        // Opt in to applying annotations (e.g. Hilt's @ApplicationContext) to both
        // the value parameter and the backing field/property. This is the future
        // default and silences the KT-73255 deprecation warning emitted for
        // constructor-injected parameters. See https://youtrack.jetbrains.com/issue/KT-73255
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Compose
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.adaptive.layout)
    implementation(libs.androidx.compose.material3.adaptive.navigation)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.material.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // DataStore
    implementation(libs.datastore.preferences)

    // Adhan (Prayer Times)
    implementation(libs.adhan)

    // Glance (Widgets)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    // WorkManager
    implementation(libs.work.runtime.ktx)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // Media3 ExoPlayer
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.media3.session)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // DateTime
    implementation(libs.kotlinx.datetime)

    // Location
    implementation(libs.play.services.location)

    // Nearby Connections (device-to-device sync)
    implementation(libs.play.services.nearby)

    // CameraX
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // In-App Updates
    implementation(libs.play.app.update)
    implementation(libs.play.app.update.ktx)

    // AboutLibraries
    implementation(libs.aboutlibraries.core)
    implementation(libs.aboutlibraries.compose.m3)

    // In-App Review
    implementation(libs.app.review)
    implementation(libs.app.review.ktx)

    // Firebase (Crashlytics + Analytics + Performance) — versions pinned by the BoM
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.perf)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.google.truth)
    testImplementation(libs.robolectric)

    // Compose UI test harness for the Robolectric atom tests in src/testDebug
    // (createComposeRule, onNodeWithText, performClick, …). Declared here as a
    // test-only dependency at an explicit version — NOT via the Compose BOM —
    // so it does not perturb AGP's consistent resolution for the androidTest
    // classpath. The Compose runtime / material3 / icons it builds against are
    // inherited from the module's main `implementation` deps, and the
    // ComponentActivity it launches comes from `debugImplementation(ui-test-manifest)`.
    testImplementation(libs.androidx.ui.test.junit4)

    // Instrumented Testing
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.turbine)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.google.truth)
    androidTestImplementation(libs.hilt.testing)
    androidTestImplementation(libs.room.testing)
    kspAndroidTest(libs.hilt.compiler)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// ── Code coverage (JaCoCo) ──────────────────────────────────────────────────
// Robolectric runs the unit tests off-device, so coverage of the Compose
// "atoms" is collected from the standard `testDebugUnitTest` execution data.
tasks.withType<Test>().configureEach {
    extensions.configure(JacocoTaskExtension::class) {
        // Needed for Robolectric + inline/synthetic classes to be reported.
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

// Class-file noise that should never count toward coverage (generated code,
// Compose compiler artifacts, DI, framework stubs, and @Preview singletons).
val coverageExclusions = listOf(
    "**/R.class",
    "**/R$*.class",
    "**/BuildConfig.*",
    "**/Manifest*.*",
    "**/*Test*.*",
    "**/*\$Companion*.*",
    // Compose compiler generates a ComposableSingletons class per file that
    // holds the lambdas used only by the @Preview functions — exclude it.
    "**/*ComposableSingletons*.*",
    // Hilt / Dagger generated code
    "**/di/**",
    "**/*_Factory*.*",
    "**/*_HiltModules*.*",
    "**/*_Impl*.*",
    "**/hilt_aggregated_deps/**",
    "**/dagger/hilt/**",
    "**/*Hilt_*.*",
)

val kotlinDebugClassesDir = layout.buildDirectory.dir("tmp/kotlin-classes/debug").get().asFile
val buildOutputDir = layout.buildDirectory.get().asFile

fun atomsClassTree(): ConfigurableFileTree =
    fileTree(kotlinDebugClassesDir) {
        include("**/presentation/components/atoms/**")
        exclude(coverageExclusions)
    }

fun moleculesClassTree(): ConfigurableFileTree =
    fileTree(kotlinDebugClassesDir) {
        include("**/presentation/components/molecules/**")
        exclude(coverageExclusions)
    }

fun organismsClassTree(): ConfigurableFileTree =
    fileTree(kotlinDebugClassesDir) {
        include("**/presentation/components/organisms/**")
        exclude(coverageExclusions)
    }

fun debugClassTree(): ConfigurableFileTree =
    fileTree(kotlinDebugClassesDir) {
        exclude(coverageExclusions)
    }

fun coverageExecutionData(): ConfigurableFileTree =
    fileTree(buildOutputDir) {
        include("**/jacoco/testDebugUnitTest.exec", "**/outputs/unit_test_code_coverage/**/*.exec")
    }

val coverageSourceDirs = files("src/main/java")

// Module-wide coverage report — satisfies "add code coverage to this app".
tasks.register<JacocoReport>("jacocoTestReport") {
    group = "verification"
    description = "Generates a JaCoCo coverage report for the debug unit tests."
    dependsOn("testDebugUnitTest")

    reports {
        html.required.set(true)
        xml.required.set(true)
        csv.required.set(false)
    }

    classDirectories.setFrom(debugClassTree())
    sourceDirectories.setFrom(coverageSourceDirs)
    executionData.setFrom(coverageExecutionData())
}

// Focused report for the presentation atoms package.
tasks.register<JacocoReport>("jacocoAtomsReport") {
    group = "verification"
    description = "Generates a JaCoCo coverage report scoped to the presentation atoms."
    dependsOn("testDebugUnitTest")

    reports {
        html.required.set(true)
        xml.required.set(true)
        csv.required.set(false)
    }

    classDirectories.setFrom(atomsClassTree())
    sourceDirectories.setFrom(coverageSourceDirs)
    executionData.setFrom(coverageExecutionData())
}

// Optional gate the team can run locally/CI to enforce atom coverage. Kept out
// of the default `check` graph so it never blocks the existing CI lane.
tasks.register<JacocoCoverageVerification>("jacocoAtomsCoverageVerification") {
    group = "verification"
    description = "Verifies coverage thresholds for the presentation atoms."
    dependsOn("testDebugUnitTest")

    classDirectories.setFrom(atomsClassTree())
    sourceDirectories.setFrom(coverageSourceDirs)
    executionData.setFrom(coverageExecutionData())

    violationRules {
        rule {
            element = "PACKAGE"
            includes = listOf("com.arshadshah.nimaz.presentation.components.atoms")
            limit {
                counter = "INSTRUCTION"
                value = "COVEREDRATIO"
                minimum = "0.90".toBigDecimal()
            }
        }
    }
}

// Focused report for the presentation molecules package.
tasks.register<JacocoReport>("jacocoMoleculesReport") {
    group = "verification"
    description = "Generates a JaCoCo coverage report scoped to the presentation molecules."
    dependsOn("testDebugUnitTest")

    reports {
        html.required.set(true)
        xml.required.set(true)
        csv.required.set(false)
    }

    classDirectories.setFrom(moleculesClassTree())
    sourceDirectories.setFrom(coverageSourceDirs)
    executionData.setFrom(coverageExecutionData())
}

// Optional gate the team can run locally/CI to enforce molecule coverage. Kept
// out of the default `check` graph so it never blocks the existing CI lane.
tasks.register<JacocoCoverageVerification>("jacocoMoleculesCoverageVerification") {
    group = "verification"
    description = "Verifies coverage thresholds for the presentation molecules."
    dependsOn("testDebugUnitTest")

    classDirectories.setFrom(moleculesClassTree())
    sourceDirectories.setFrom(coverageSourceDirs)
    executionData.setFrom(coverageExecutionData())

    violationRules {
        rule {
            element = "PACKAGE"
            includes = listOf("com.arshadshah.nimaz.presentation.components.molecules")
            limit {
                counter = "INSTRUCTION"
                value = "COVEREDRATIO"
                minimum = "0.90".toBigDecimal()
            }
        }
    }
}

// Focused report for the presentation organisms package.
tasks.register<JacocoReport>("jacocoOrganismsReport") {
    group = "verification"
    description = "Generates a JaCoCo coverage report scoped to the presentation organisms."
    dependsOn("testDebugUnitTest")

    reports {
        html.required.set(true)
        xml.required.set(true)
        csv.required.set(false)
    }

    classDirectories.setFrom(organismsClassTree())
    sourceDirectories.setFrom(coverageSourceDirs)
    executionData.setFrom(coverageExecutionData())
}

// Optional gate the team can run locally/CI to enforce organism coverage. Kept
// out of the default `check` graph so it never blocks the existing CI lane.
tasks.register<JacocoCoverageVerification>("jacocoOrganismsCoverageVerification") {
    group = "verification"
    description = "Verifies coverage thresholds for the presentation organisms."
    dependsOn("testDebugUnitTest")

    classDirectories.setFrom(organismsClassTree())
    sourceDirectories.setFrom(coverageSourceDirs)
    executionData.setFrom(coverageExecutionData())

    violationRules {
        rule {
            element = "PACKAGE"
            includes = listOf("com.arshadshah.nimaz.presentation.components.organisms")
            limit {
                counter = "INSTRUCTION"
                value = "COVEREDRATIO"
                minimum = "0.90".toBigDecimal()
            }
        }
    }
}
