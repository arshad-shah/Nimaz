plugins {
    `kotlin-dsl`
}

group = "com.arshadshah.nimaz.buildlogic"

kotlin {
    jvmToolchain(21)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    // compileOnly, not implementation: the real plugin classes come from the consuming build's
    // plugin classpath at runtime. Putting them on the runtime classpath here would pin a second
    // copy of AGP/KGP and break plugin resolution in the main build.
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.compose.compiler.gradle.plugin)
    compileOnly(libs.ksp.gradle.plugin)
    compileOnly(libs.hilt.gradle.plugin)

    testImplementation(libs.junit)
    testImplementation(libs.google.truth)
    testImplementation(gradleTestKit())
}

// TestKit fixtures need to reach the real included build and the real version catalog, so both
// paths are injected rather than guessed relative to the test's working directory.
tasks.withType<Test>().configureEach {
    systemProperty("nimaz.buildLogic.root", rootDir.absolutePath)
    systemProperty("nimaz.repo.root", rootDir.parentFile.absolutePath)
    // Each test spawns a real Gradle build; the default 2-minute-ish assumptions do not apply.
    testLogging { showStandardStreams = true }
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "nimaz.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "nimaz.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("jvmLibrary") {
            id = "nimaz.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "nimaz.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("androidHilt") {
            id = "nimaz.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("androidFeature") {
            id = "nimaz.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
    }
}
