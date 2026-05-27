pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
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

rootProject.name = "SpecTechAndroid"

include(":app")

include(":core:domain")
include(":core:network")
include(":core:data")
include(":core:ui-kit")
include(":core:platform")

include(":features:auth")
include(":features:marketplace")
include(":features:create-order")
include(":features:orders")
include(":features:garage")
include(":features:bidding")
include(":features:profile")
include(":features:news")
include(":features:notifications")
include(":features:support")
