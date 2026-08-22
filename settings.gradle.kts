pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
    // Convention plugins (`nimaz.android.*`, `nimaz.jvm.library`) live in an included build so
    // every module shares one definition of compileSdk / minSdk / Java 21 / Compose / Hilt.
    includeBuild("build-logic")
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "nimaz"
include(":app")
include(":core:domain")
include(":core:common")
include(":core:database")
include(":core:datastore")
include(":core:data")
include(":core:ui")
include(":core:navigation")
include(":feature:widget")
include(":feature:onboarding")
include(":feature:about")
include(":feature:tools")
include(":feature:calendar")
include(":feature:search")
include(":baselineprofile")
