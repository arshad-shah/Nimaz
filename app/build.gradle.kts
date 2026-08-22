import org.gradle.api.tasks.PathSensitivity
import com.arshadshah.nimaz.buildlogic.FetchNimazDataTask
import com.arshadshah.nimaz.buildlogic.NimazDataCredentials
import com.arshadshah.nimaz.buildlogic.NimazDataLockParser
import com.arshadshah.nimaz.buildlogic.orderAssetConsumersAfter

plugins {
    // Convention plugins from the `build-logic` included build. They carry compileSdk 37,
    // minSdk 29, Java 21, the `-Xannotation-default-target=param-property` compiler arg, the
    // Compose compiler + `buildFeatures.compose`, and the Hilt/KSP wiring — so this file no
    // longer states any of it and eighteen future modules will not restate it either.
    //
    // AGP 9 compiles Kotlin via built-in support; neither this file nor the convention plugins
    // apply the standalone org.jetbrains.kotlin.android plugin.
    id("nimaz.android.application")
    id("nimaz.android.compose")
    id("nimaz.android.hilt")
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-parcelize")
    alias(libs.plugins.about.libs.plugin)
    alias(libs.plugins.baselineprofile)
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
//
// Read through `providers.fileContents`, not `file(...).exists()`: the presence of this file
// decides which plugins are applied, so it has to be a *tracked* configuration input. An
// untracked filesystem probe would let a cached configuration survive CI dropping the file in,
// and the cached build would then produce a release APK with no Crashlytics in it.
val googleServicesConfig =
    providers.fileContents(layout.projectDirectory.file("google-services.json")).asText
if (googleServicesConfig.isPresent) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
    apply(plugin = libs.plugins.firebase.crashlytics.get().pluginId)
    apply(plugin = libs.plugins.firebase.perf.get().pluginId)
}

// Compose compiler metrics and reports (#467).
//
// Measurement only — nothing in this PR acts on the output. The presentation layer has
// 4 @Immutable, 1 @Stable and no immutable collections across 375 files, against 48
// List<…> and 6 Map<…> parameters in presentation/components alone. Kotlin 2.3's strong
// skipping compares unstable parameters by identity rather than treating them as
// always-changed, so the picture is better than those numbers suggest — but a List
// rebuilt on every emission still fails that comparison and forces a redraw.
//
// Read `<build>/compose_compiler/*-composables.txt` for restartable-but-not-skippable
// functions and fix only the ones on a hot path: the reader, the home carousel, the
// search results list. Do not rewrite on suspicion — that is why this landed on its own.
composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
    metricsDestination = layout.buildDirectory.dir("compose_compiler")
}

// The baseline profile plugin derives `nonMinifiedRelease` and `benchmarkRelease` from
// `release`, copying its signing config — a keystore only CI has (KEYSTORE_FILE and
// friends come from the environment). Left alone, `generateBaselineProfile` fails for
// anyone without release keys, which is everyone on a developer machine, which is where
// it is most likely to be run. Sign them with the debug key instead: these variants are
// never published, and a profile does not depend on the signature.
//
// finalizeDsl, not buildTypes.configureEach — the plugin copies the signing config
// after the DSL block runs, so configureEach is overwritten and the build still asks
// for keystore.jks.
androidComponents.finalizeDsl { extension ->
    extension.buildTypes.configureEach {
        if (name.startsWith("nonMinified") || name.startsWith("benchmark")) {
            signingConfig = extension.signingConfigs.getByName("debug")
        }
    }
}

android {
    namespace = "com.arshadshah.nimaz"
    // compileSdk, minSdk and Java 21 come from `nimaz.android.application`.

    defaultConfig {
        applicationId = "com.arshadshah.nimaz"
        targetSdk = 36
        // Source of truth for the app version. CI bumps these at build time and
        // pushes the change back to dev (with a bypass GitHub App token) after a successful
        // deploy, so the committed baseline stays in sync for the next build.

        versionCode = 428
        versionName = "3.0.127"

        // Custom runner swaps in HiltTestApplication so instrumented tests run on
        // the full Hilt graph without NimazApp's Firebase / AppInitializer / device
        // service bootstrap. See androidTest/.../support/HiltTestRunner.kt.
        testInstrumentationRunner = "com.arshadshah.nimaz.support.HiltTestRunner"

        // Room schema export
        ksp {
            // room.schemaLocation moved to :core:database with the Room compiler — see
            // core/database/build.gradle.kts. Nothing in :app declares an @Entity any more.
        }

        // Cloud project number backing the Play Integrity standard request. Driven
        // by the gradle property `playIntegrityCloudProjectNumber` (placeholder 0
        // until the real Google Cloud project is wired up — see gradle.properties).
        val playIntegrityProjectNumber =
            providers.gradleProperty("playIntegrityCloudProjectNumber").getOrElse("0")
        buildConfigField(
            "long",
            "PLAY_INTEGRITY_CLOUD_PROJECT_NUMBER",
            "${playIntegrityProjectNumber}L"
        )

        // The identity of the content artifact this APK was built against, so the app can tell
        // at runtime whether the database on disk is the one it ships with. Read from the same
        // data.lock.json the fetch task verifies against, so the two cannot disagree — see
        // ContentArtifactInstaller.
        //
        // `providers.fileContents`, not `JsonSlurper().parse(File)`: parsing the file directly
        // is an untracked read, so with the configuration cache on, a changed pin would not
        // invalidate the cached configuration and a **stale sha would be baked into the APK** —
        // silent, and wrong in the one place that decides whether the shipped database is
        // replaced on upgrade. The parser is shared with `fetchNimazData` so the two readers of
        // this file cannot drift.
        val contentArtifactSha = NimazDataLockParser.parse(
            providers.fileContents(
                rootProject.layout.projectDirectory.file("data.lock.json")
            ).asText.get()
        ).artifact.sha256
        buildConfigField("String", "CONTENT_ARTIFACT_SHA256", "\"$contentArtifactSha\"")
    }

    // Ship the exported Room schemas as androidTest assets so MigrationTestHelper can
    // load them on-device (it looks for `<DatabaseClass>/<version>.json` under assets).
    //
    // Repointed at `:core:database`, which owns the schemas and the `room.schemaLocation` arg
    // that writes them. The migration and DAO tests themselves deliberately stay here:
    // `android_instrumented_tests.yml` runs exactly one artifact —
    // `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` — so instrumented
    // tests moved into a library module are not run by anything, and the lane stays green
    // having lost them. That is 72 tests, 14 of them the migration suite. See #558.
    sourceSets {
        getByName("androidTest") {
            assets.srcDir(rootProject.layout.projectDirectory.dir("core/database/schemas"))
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

    // Base URL of the Nimaz AI Worker. Driven by gradle properties so the real
    // *.workers.dev URL is never committed: debug reads `nimazAiWorkerUrlDebug`,
    // release reads `nimazAiWorkerUrl`; both fall back to a placeholder (the app
    // then simply surfaces the network-error state — it never crashes).
    val aiWorkerUrlPlaceholder = "https://nimaz-ai.REPLACE_ME.workers.dev"
    val aiWorkerUrlDebug = providers.gradleProperty("nimazAiWorkerUrlDebug")
        .getOrElse(aiWorkerUrlPlaceholder)
    val aiWorkerUrlRelease = providers.gradleProperty("nimazAiWorkerUrl")
        .getOrElse(aiWorkerUrlPlaceholder)

    buildTypes {
        debug {
            buildConfigField("String", "AI_WORKER_BASE_URL", "\"$aiWorkerUrlDebug\"")
        }
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
            buildConfigField("String", "AI_WORKER_BASE_URL", "\"$aiWorkerUrlRelease\"")
        }
    }
    buildFeatures {
        // compose = true comes from `nimaz.android.compose`.
        buildConfig = true
    }

    bundle {
        language {
            // Settings lets the user pick an app language independently of the
            // device locale (see LocaleHelper). With language splits on, Play
            // only delivers the resources for the device's locale, so choosing
            // any other language would silently fall back to English on a Play
            // install - a bug that never reproduces on a locally built APK.
            // Ship every locale in the base APK instead.
            enableSplit = false
        }
    }

    testOptions {
        unitTests {
            // Required so Robolectric can read merged Android resources/manifest,
            // which the Compose UI tests for the atoms rely on.
            isIncludeAndroidResources = true
            isReturnDefaultValues = true

            all {
                // DeviceStateCorpusTest doubles as the corpus harness: given an output
                // path it writes the migrated + fully seeded database for `nz vault seal`.
                // Test JVMs are forked and inherit nothing, so the property has to be
                // forwarded explicitly. Without a value it stays a plain assertion test.
                it.systemProperty(
                    "nimaz.corpus.out",
                    providers.systemProperty("nimaz.corpus.out").getOrElse("")
                )
                it.testLogging { showStandardStreams = true }
            }
        }
    }
}

// Content data is fetched from arshad-shah/nimaz-data and pinned by sha256 in data.lock.json —
// see FetchNimazDataTask in build-logic for what the task does and why the artifact is no longer
// tracked in this repository.
//
// The task *class* lives in build-logic; the *registration* stays here on purpose. The task
// belongs to the project that consumes the generated assets, and a convention plugin that
// registered it centrally — leaving libraries to depend on `:app:fetchNimazData` — would point a
// library at the app, which is the inversion the multi-module epic exists to remove.
//
// This lived in a `gradle/nimaz-data.gradle.kts` script plugin until #503. A script applied with
// `apply(from = …)` is compiled against its own classpath and cannot see build-logic's types, so
// that file could not survive the task becoming one; its wiring is the block below.
tasks.register<FetchNimazDataTask>("fetchNimazData") {
    description = "Fetches and sha256-verifies the pinned content artifact."
    group = "build setup"

    lockFile.set(rootProject.layout.projectDirectory.file("data.lock.json"))
    generatedAssets.set(layout.buildDirectory.dir("generated/nimazData/assets"))
    cacheRoot.set(layout.dir(provider { gradle.gradleUserHomeDir.resolve("caches/nimaz-data") }))

    // NIMAZ_DATA_TOKEN, then the nimazDataToken gradle property, then `gh auth token` — entirely
    // through providers, because `project.findProperty` and a bare `ProcessBuilder` at execution
    // time were both configuration-cache violations. Each source is tested for blankness
    // individually (a fork PR gets a *set but empty* NIMAZ_DATA_TOKEN), and the chain is lazy, so
    // a build that already has a token never spawns `gh`. See NimazDataCredentials.
    dataToken.set(NimazDataCredentials.of(providers))
}

// AGP 9 refuses a Provider here. The Variant API's addGeneratedSourceDirectory would take one,
// and now that fetchNimazData *is* a typed task the option is finally open — but switching to it
// changes how the directory reaches the variant, which is a behaviour change and belongs in its
// own change. Registering the directory statically and hanging the ordering off every
// asset-consuming task gives the same guarantee today.
android.sourceSets.getByName("main").assets.srcDir(
    layout.buildDirectory.dir("generated/nimazData/assets").get().asFile
)

// The matcher itself lives in build-logic (GeneratedAssetOrdering) so the next module that
// generates assets cannot get it subtly wrong. The task *registration* stays here on purpose:
// a library convention plugin reaching for `:app:fetchNimazData` would point a library at the
// app, which is the inversion the multi-module epic exists to remove.
orderAssetConsumersAfter("fetchNimazData")

dependencies {
    // Consumes app/src/main/baseline-prof.txt, generated by :baselineprofile.
    // Regenerate with `./gradlew :app:generateBaselineProfile` after a change that
    // moves the startup path or the reader — a stale profile is not wrong, just less
    // useful, so this is maintenance rather than a gate.
    baselineProfile(project(":baselineprofile"))

    // The pure layer. `api`-exposed javax.inject and coroutines-core come with it, so nothing
    // here declares them again.
    implementation(project(":core:domain"))
    // Formatting helpers, the telemetry seam and the string seam. `api`-exposes :core:domain.
    implementation(project(":core:common"))
    // Both Room databases. `api`-exposes room-runtime, so nothing here declares it again.
    implementation(project(":core:database"))
    // PreferencesDataStore and the three DataStore files. `api`-exposes datastore-preferences.
    implementation(project(":core:datastore"))
    // The 18 repository implementations and the platform adapters behind the domain ports.
    implementation(project(":core:data"))
    // The design system and every string, colour and font. `api`-exposes the Compose BOM,
    // ui, ui-graphics and material3, so nothing here re-declares them. Note that this module owns
    // `R.string.*` now: `com.arshadshah.nimaz.R` holds only the widget and notification resources
    // that stayed, so presentation code imports `com.arshadshah.nimaz.core.ui.R`.
    implementation(project(":core:ui"))
    // The route vocabulary — Routes, ScreenTags, taggedComposable, the announcement and help
    // deep-link grammars. `api`-exposes navigation-compose. NavGraph.kt itself is still here;
    // it is decomposed in PR 12.
    implementation(project(":core:navigation"))
    // The six Glance widgets and their workers — the first feature module (#564). It brings its
    // own manifest entries, its own widget_colors/drawables/layouts and the 17 strings nothing
    // else uses, so nothing widget-shaped is left here. (The other 16 it references stay in
    // `:core:ui` because `WidgetsScreen`'s gallery reads them too.)
    implementation(project(":feature:widget"))
    // The first-run flow (#565). Extracted with no couplings to unpick — see its build file.
    implementation(project(":feature:onboarding"))
    // About, Help and More — one destination, so one module (#565). Six couplings to `:app` were
    // unpicked to get it out; its build file lists them.
    implementation(project(":feature:about"))
    // FakeTodayProvider / FakeSearchSettings / FakeStringProvider / RecordingWidgetRefresher —
    // one definition each, used by the ViewModel tests here and the tests over there.
    testImplementation(testFixtures(project(":core:domain")))
    testImplementation(testFixtures(project(":core:common")))

    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.zxing.core)
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

    // Hilt — the runtime and the compiler come from `nimaz.android.hilt`.
    implementation(libs.hilt.navigation.compose)

    // Room
    // Room itself comes from :core:database, which `api`-exposes runtime and ktx. The compiler
    // is not needed here: nothing in :app declares an @Entity, @Dao or @Database any more.

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

    // Ktor client (AI Worker API). Logging plugin is on the classpath but only
    // installed at runtime for debug builds (gated on BuildConfig.DEBUG).
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)

    // Play Integrity (device attestation for the AI Worker)
    implementation(libs.play.integrity)

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

    // Firebase (Crashlytics + Analytics + Performance + Messaging) — versions pinned by the BoM
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.perf)
    implementation(libs.firebase.messaging)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.google.truth)
    testImplementation(libs.robolectric)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.json)

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
    // Espresso's accessibility validator: missing labels and sub-48dp touch targets, on the
    // instrumented lane we already run (audit §4).
    androidTestImplementation(libs.androidx.espresso.accessibility)
    // UI Automator drives the app from *outside* Compose's test harness. Required for the
    // live-countdown tests: a ComposeTestRule puts the frame clock under test control, so a test
    // that sleeps in real time never sees the UI redraw. See LiveCountdownTest.
    androidTestImplementation(libs.androidx.uiautomator)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.turbine)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.google.truth)
    androidTestImplementation(libs.hilt.testing)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.kotlinx.coroutines.android)
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

    // Source roots in *other* modules that `:app` unit tests read off disk.
    //
    // `AnalyticsReachabilityTest` scans every place a UI event can be dispatched from, and since
    // PRs 10, 11 and 13 of #551 three of those roots live outside this module. Gradle has no way
    // to know that: a file scan is not a compile dependency, so without these declarations
    // `testDebugUnitTest` stays UP-TO-DATE when the scanned sources change and the assertion
    // simply does not run. That exact failure hid a broken assertion in `:core:common` through
    // two full local gate sweeps — an assertion only fires if its task runs.
    mapOf(
        "designSystemSources" to "core/ui/src/main/kotlin/com/arshadshah/nimaz/presentation",
        "navigationSources" to "core/navigation/src/main/kotlin/com/arshadshah/nimaz/core/navigation",
        "widgetSources" to "feature/widget/src/main/kotlin/com/arshadshah/nimaz/widget",
    ).forEach { (name, path) ->
        inputs.dir(rootProject.layout.projectDirectory.dir(path))
            .withPropertyName(name)
            .withPathSensitivity(PathSensitivity.RELATIVE)
    }

    // `LicenceCatalogueTest` reads the AboutLibraries catalogue, which is a *build output* of
    // this module rather than a source file, so nothing put it on the test task's input set.
    // Without this the task stays UP-TO-DATE and the assertion does not run — verified the hard
    // way: truncating the catalogue to 40 entries produced a green build, because the test never
    // executed. `builtBy` is what stops that being an implicit-dependency error.
    inputs.files(
        files(layout.buildDirectory.file("generated/aboutLibraries/debug/res/raw/aboutlibraries.json"))
            .builtBy("prepareLibraryDefinitionsDebug")
    ).withPropertyName("licenceCatalogue").withPathSensitivity(PathSensitivity.RELATIVE)
        .optional()

    // `HiltWorkerProcessorTest` reads every module's build file. Sources reach the test task
    // through the runtime classpath already; build files do not, and a build file is exactly
    // what that test exists to check.
    inputs.files(
        rootProject.layout.projectDirectory.asFileTree.matching {
            include("*/build.gradle.kts", "*/*/build.gradle.kts")
            exclude("**/build/**")
        }
    ).withPropertyName("moduleBuildFiles").withPathSensitivity(PathSensitivity.RELATIVE)
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

// Where the debug classes live, which is not where it used to be.
//
// These tasks pointed at `tmp/kotlin-classes/debug` alone. AGP 9 does not write there
// — compiled classes land under `intermediates/classes/debug/…` — so the directory
// simply did not exist, `classDirectories` resolved to nothing, and every one of the
// four jacoco reports came out as a 237-byte file containing a `sessioninfo` and no
// classes at all. Coverage was not merely unreported (#464); it was not being measured.
//
// Both paths are listed so the report is correct on either AGP, and so this cannot
// fail silently again: an empty report is indistinguishable from 0% at a glance.
val debugClassRoots = listOf(
    layout.buildDirectory.dir("intermediates/classes/debug").get().asFile,
    layout.buildDirectory.dir("tmp/kotlin-classes/debug").get().asFile,
)
val buildOutputDir = layout.buildDirectory.get().asFile

fun classTree(vararg includes: String): FileCollection =
    files(
        debugClassRoots.map { root ->
            fileTree(root) {
                if (includes.isNotEmpty()) include(*includes)
                exclude(coverageExclusions)
            }
        }
    )

fun atomsClassTree(): FileCollection = classTree("**/presentation/components/atoms/**")

fun moleculesClassTree(): FileCollection = classTree("**/presentation/components/molecules/**")

fun organismsClassTree(): FileCollection = classTree("**/presentation/components/organisms/**")

fun debugClassTree(): FileCollection = classTree()

/**
 * A module whose classes belong in the merged coverage report.
 *
 * Referenced by directory rather than through `project(":core:domain")`: a live `Project` in a
 * task action is a configuration-cache failure, and this build runs with `problems=fail`.
 * [packageRoot] is what the merged report must actually contain — see `coverageFloor`.
 *
 * [classesGlobs] and [execGlobs] are globs under the module's `build/` rather than fixed paths,
 * and they differ per module because Gradle and AGP do not agree on where anything goes:
 *
 * | | classes | exec |
 * |---|---|---|
 * | `kotlin-jvm` (`:core:domain`) | `classes/kotlin/main` | `jacoco/test.exec` |
 * | `com.android.library` (`:core:common`) | `intermediates/built_in_kotlinc/debug/**/classes` | `jacoco/testDebugUnitTest.exec` |
 *
 * Note that an Android **library** has no `intermediates/classes` at all — that is an
 * application-module output. Pointing at one anyway is precisely the #464 failure: the directory
 * does not exist, the tree resolves to nothing, and the report is a valid file describing zero
 * classes. `coverageFloor` is the assertion that catches it.
 */
data class CoverageModule(
    val gradlePath: String,
    val projectDir: Directory,
    val testTask: String,
    val classesGlobs: List<String>,
    val execGlobs: List<String>,
    val sourceDir: String,
    val packageRoot: String,
) {
    val buildDir: Directory get() = projectDir.dir("build")
}

val coverageModules = listOf(
    CoverageModule(
        gradlePath = ":core:domain",
        projectDir = rootProject.layout.projectDirectory.dir("core/domain"),
        testTask = "test",
        classesGlobs = listOf("classes/kotlin/main/**"),
        execGlobs = listOf("jacoco/test.exec"),
        sourceDir = "src/main/kotlin",
        packageRoot = "com/arshadshah/nimaz/domain",
    ),
    CoverageModule(
        gradlePath = ":core:common",
        projectDir = rootProject.layout.projectDirectory.dir("core/common"),
        testTask = "testDebugUnitTest",
        // Both spellings, so this keeps working whichever one a future AGP writes — the same
        // belt-and-braces the `:app` roots above use, and for the same reason.
        classesGlobs = listOf(
            "intermediates/built_in_kotlinc/debug/**/classes/**",
            "intermediates/classes/debug/**",
            "tmp/kotlin-classes/debug/**",
        ),
        execGlobs = listOf(
            "jacoco/testDebugUnitTest.exec",
            "outputs/unit_test_code_coverage/**/*.exec",
        ),
        sourceDir = "src/main/kotlin",
        packageRoot = "com/arshadshah/nimaz/core/common",
    ),
    CoverageModule(
        gradlePath = ":core:database",
        projectDir = rootProject.layout.projectDirectory.dir("core/database"),
        testTask = "testDebugUnitTest",
        classesGlobs = listOf(
            "intermediates/built_in_kotlinc/debug/**/classes/**",
            "intermediates/classes/debug/**",
            "tmp/kotlin-classes/debug/**",
        ),
        execGlobs = listOf(
            "jacoco/testDebugUnitTest.exec",
            "outputs/unit_test_code_coverage/**/*.exec",
        ),
        sourceDir = "src/main/kotlin",
        packageRoot = "com/arshadshah/nimaz/data/local",
    ),
    CoverageModule(
        gradlePath = ":core:datastore",
        projectDir = rootProject.layout.projectDirectory.dir("core/datastore"),
        testTask = "testDebugUnitTest",
        classesGlobs = listOf(
            "intermediates/built_in_kotlinc/debug/**/classes/**",
            "intermediates/classes/debug/**",
            "tmp/kotlin-classes/debug/**",
        ),
        execGlobs = listOf(
            "jacoco/testDebugUnitTest.exec",
            "outputs/unit_test_code_coverage/**/*.exec",
        ),
        sourceDir = "src/main/kotlin",
        packageRoot = "com/arshadshah/nimaz/core/datastore",
    ),
    CoverageModule(
        gradlePath = ":core:data",
        projectDir = rootProject.layout.projectDirectory.dir("core/data"),
        testTask = "testDebugUnitTest",
        classesGlobs = listOf(
            "intermediates/built_in_kotlinc/debug/**/classes/**",
            "intermediates/classes/debug/**",
            "tmp/kotlin-classes/debug/**",
        ),
        execGlobs = listOf(
            "jacoco/testDebugUnitTest.exec",
            "outputs/unit_test_code_coverage/**/*.exec",
        ),
        sourceDir = "src/main/kotlin",
        packageRoot = "com/arshadshah/nimaz/data/repository",
    ),
    CoverageModule(
        gradlePath = ":core:ui",
        projectDir = rootProject.layout.projectDirectory.dir("core/ui"),
        testTask = "testDebugUnitTest",
        classesGlobs = listOf(
            "intermediates/built_in_kotlinc/debug/**/classes/**",
            "intermediates/classes/debug/**",
            "tmp/kotlin-classes/debug/**",
        ),
        execGlobs = listOf(
            "jacoco/testDebugUnitTest.exec",
            "outputs/unit_test_code_coverage/**/*.exec",
        ),
        sourceDir = "src/main/kotlin",
        packageRoot = "com/arshadshah/nimaz/presentation/components/atoms",
    ),
    CoverageModule(
        gradlePath = ":core:navigation",
        projectDir = rootProject.layout.projectDirectory.dir("core/navigation"),
        testTask = "testDebugUnitTest",
        classesGlobs = listOf(
            "intermediates/built_in_kotlinc/debug/**/classes/**",
            "intermediates/classes/debug/**",
            "tmp/kotlin-classes/debug/**",
        ),
        execGlobs = listOf(
            "jacoco/testDebugUnitTest.exec",
            "outputs/unit_test_code_coverage/**/*.exec",
        ),
        sourceDir = "src/main/kotlin",
        packageRoot = "com/arshadshah/nimaz/core/navigation",
    ),
    CoverageModule(
        gradlePath = ":feature:widget",
        projectDir = rootProject.layout.projectDirectory.dir("feature/widget"),
        testTask = "testDebugUnitTest",
        classesGlobs = listOf(
            "intermediates/built_in_kotlinc/debug/**/classes/**",
            "intermediates/classes/debug/**",
            "tmp/kotlin-classes/debug/**",
        ),
        execGlobs = listOf(
            "jacoco/testDebugUnitTest.exec",
            "outputs/unit_test_code_coverage/**/*.exec",
        ),
        sourceDir = "src/main/kotlin",
        packageRoot = "com/arshadshah/nimaz/widget",
    ),
    CoverageModule(
        gradlePath = ":feature:onboarding",
        projectDir = rootProject.layout.projectDirectory.dir("feature/onboarding"),
        testTask = "testDebugUnitTest",
        classesGlobs = listOf(
            "intermediates/built_in_kotlinc/debug/**/classes/**",
            "intermediates/classes/debug/**",
            "tmp/kotlin-classes/debug/**",
        ),
        execGlobs = listOf(
            "jacoco/testDebugUnitTest.exec",
            "outputs/unit_test_code_coverage/**/*.exec",
        ),
        sourceDir = "src/main/kotlin",
        packageRoot = "com/arshadshah/nimaz/presentation",
    ),
    CoverageModule(
        gradlePath = ":feature:about",
        projectDir = rootProject.layout.projectDirectory.dir("feature/about"),
        testTask = "testDebugUnitTest",
        classesGlobs = listOf(
            "intermediates/built_in_kotlinc/debug/**/classes/**",
            "intermediates/classes/debug/**",
            "tmp/kotlin-classes/debug/**",
        ),
        execGlobs = listOf(
            "jacoco/testDebugUnitTest.exec",
            "outputs/unit_test_code_coverage/**/*.exec",
        ),
        sourceDir = "src/main/kotlin",
        packageRoot = "com/arshadshah/nimaz/presentation",
    ),
)

fun coverageExecutionData(): FileCollection =
    files(
        fileTree(buildOutputDir) {
            include(
                "**/jacoco/testDebugUnitTest.exec",
                "**/outputs/unit_test_code_coverage/**/*.exec",
            )
        },
        // Every extracted module's exec data, or the number silently improves as it measures
        // less. From PR 5 (:core:domain) onward `:app` no longer holds most of the codebase, and
        // a report scoped to `:app` alone would show a *rising* percentage over a shrinking
        // tree — a metric that gets better by covering fewer classes is worse than none.
        files(
            coverageModules.map { module ->
                fileTree(module.buildDir) { include(module.execGlobs) }
            }
        ),
    )

fun coverageClassDirs(): FileCollection =
    files(
        debugClassTree(),
        files(
            coverageModules.map { module ->
                fileTree(module.buildDir) {
                    include(module.classesGlobs)
                    exclude(coverageExclusions)
                }
            }
        ),
    )

val coverageSourceDirs = files(
    "src/main/java",
    coverageModules.map { it.projectDir.dir(it.sourceDir) },
)

/**
 * What the merged report must contain, as plain strings, for the floor asserted in
 * `jacocoTestReport`'s `doLast`.
 *
 * Flattened here rather than read off [coverageModules] inside the task action, and asserted
 * inline rather than through a helper function. **Both are configuration-cache requirements, not
 * style**: a lambda that calls a build-script function — or that touches an instance of a
 * build-script class such as [CoverageModule] — captures the script object, and Gradle cannot
 * serialize one. This build runs with `configuration-cache=problems=fail`, so that is a failed
 * build rather than a warning.
 */
val coverageFloor: List<Pair<String, String>> =
    coverageModules.map { it.gradlePath to it.packageRoot }

// Module-wide coverage report — satisfies "add code coverage to this app".
tasks.register<JacocoReport>("jacocoTestReport") {
    group = "verification"
    description = "Generates a JaCoCo coverage report for the debug unit tests."
    dependsOn("testDebugUnitTest")
    coverageModules.forEach { dependsOn("${it.gradlePath}:${it.testTask}") }

    reports {
        html.required.set(true)
        xml.required.set(true)
        csv.required.set(false)
        // Pinned: the default XML destination is derived from the task name, and
        // scripts/coverage_summary.py (wired into pr_checks.yml) reads this path.
        xml.outputLocation.set(
            layout.buildDirectory.file("reports/jacoco/jacocoTestReport.xml")
        )
    }

    classDirectories.setFrom(coverageClassDirs())
    sourceDirectories.setFrom(coverageSourceDirs)
    executionData.setFrom(coverageExecutionData())

    // The floor #553 established for the doc scans, applied to coverage: an aggregate that
    // quietly drops a module reads exactly like one that includes it, only with a nicer
    // percentage. Every later extraction PR adds its module to `coverageModules`, so forgetting
    // to wire one up is a red build rather than a number nobody questions.
    val reportXml = layout.buildDirectory.file("reports/jacoco/jacocoTestReport.xml")
    val floor = coverageFloor
    doLast {
        val report = reportXml.get().asFile
        if (report.isFile) {
            val text = report.readText()
            val missing = floor.filter { (_, packageRoot) ->
                """<package name="$packageRoot""" !in text
            }
            check(missing.isEmpty()) {
                "The merged coverage report contains no classes from " +
                    missing.joinToString { (gradlePath, _) -> gradlePath } +
                    ". Its exec data or class directory is not wired into " +
                    ":app:jacocoTestReport, so coverage is being reported over a smaller " +
                    "codebase than the app actually ships."
            }
        }
    }
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
