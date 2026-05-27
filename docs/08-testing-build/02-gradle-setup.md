# 02 — Gradle Setup

A starter Gradle configuration tuned to the multi-module layout described in
[02-architecture/01-module-layout.md](../02-architecture/01-module-layout.md).

## `settings.gradle.kts`

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
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
```

## `gradle/libs.versions.toml`

```toml
[versions]
agp = "8.7.3"
kotlin = "2.0.21"
ksp = "2.0.21-1.0.27"
compose-bom = "2024.12.01"
hilt = "2.52"
ktor = "3.0.2"
kotlinx-serialization = "1.7.3"
kotlinx-datetime = "0.6.1"
kotlinx-coroutines = "1.9.0"
coil = "2.7.0"
firebase-bom = "33.7.0"
google-services = "4.4.2"
security-crypto = "1.1.0-alpha06"
datastore = "1.1.1"
navigation = "2.8.5"
timber = "5.0.1"
junit5 = "5.11.4"
mockk = "1.13.13"
turbine = "1.2.0"
robolectric = "4.14"
kotest = "5.9.1"

[libraries]
kotlin-stdlib = { group = "org.jetbrains.kotlin", name = "kotlin-stdlib", version.ref = "kotlin" }
kotlinx-coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "kotlinx-coroutines" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "kotlinx-coroutines" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinx-serialization" }
kotlinx-datetime = { group = "org.jetbrains.kotlinx", name = "kotlinx-datetime", version.ref = "kotlinx-datetime" }

androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version = "1.15.0" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version = "2.8.7" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version = "2.8.7" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version = "1.9.3" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation" }
androidx-hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version = "1.2.0" }
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
androidx-security-crypto = { group = "androidx.security", name = "security-crypto", version.ref = "security-crypto" }
androidx-splashscreen = { group = "androidx.core", name = "core-splashscreen", version = "1.0.1" }
androidx-appcompat = { group = "androidx.appcompat", name = "appcompat", version = "1.7.0" }
androidx-browser = { group = "androidx.browser", name = "browser", version = "1.8.0" }
androidx-exifinterface = { group = "androidx.exifinterface", name = "exifinterface", version = "1.3.7" }

compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }

hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }

ktor-client-core = { group = "io.ktor", name = "ktor-client-core", version.ref = "ktor" }
ktor-client-okhttp = { group = "io.ktor", name = "ktor-client-okhttp", version.ref = "ktor" }
ktor-client-content-negotiation = { group = "io.ktor", name = "ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-kotlinx-json = { group = "io.ktor", name = "ktor-serialization-kotlinx-json", version.ref = "ktor" }
ktor-client-logging = { group = "io.ktor", name = "ktor-client-logging", version.ref = "ktor" }

coil-compose = { group = "io.coil-kt", name = "coil-compose", version.ref = "coil" }

firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebase-bom" }
firebase-messaging = { group = "com.google.firebase", name = "firebase-messaging" }
firebase-analytics = { group = "com.google.firebase", name = "firebase-analytics" }

places = { group = "com.google.android.libraries.places", name = "places", version = "4.1.0" }

timber = { group = "com.jakewharton.timber", name = "timber", version.ref = "timber" }

junit-jupiter = { group = "org.junit.jupiter", name = "junit-jupiter", version.ref = "junit5" }
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }
robolectric = { group = "org.robolectric", name = "robolectric", version.ref = "robolectric" }
kotest-assertions = { group = "io.kotest", name = "kotest-assertions-core", version.ref = "kotest" }
mockwebserver = { group = "com.squareup.okhttp3", name = "mockwebserver", version = "4.12.0" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
google-services = { id = "com.google.gms.google-services", version.ref = "google-services" }
```

## Root `build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.google-services) apply false
}
```

## `app/build.gradle.kts` (sketch)

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google-services)
}

android {
    namespace = "ru.spectech.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "ru.spectech.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        // See 04-app-configuration.md for the buildConfigFields
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    kotlinOptions { jvmTarget = "17" }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        debug { isMinifyEnabled = false }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:network"))
    implementation(project(":core:data"))
    implementation(project(":core:ui-kit"))
    implementation(project(":core:platform"))
    implementation(project(":features:auth"))
    implementation(project(":features:marketplace"))
    implementation(project(":features:create-order"))
    implementation(project(":features:orders"))
    implementation(project(":features:garage"))
    implementation(project(":features:bidding"))
    implementation(project(":features:profile"))
    implementation(project(":features:news"))
    implementation(project(":features:notifications"))
    implementation(project(":features:support"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    implementation(libs.timber)
}
```

## ProGuard

Add to `app/proguard-rules.pro`:

```
# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Hilt types
-keep class * extends androidx.lifecycle.ViewModel

# Keep Ktor + OkHttp
-keep class io.ktor.** { *; }
-keep class okhttp3.** { *; }
-dontwarn org.bouncycastle.**

# Keep our domain model classes for serialization
-keep class ru.spectech.domain.** { *; }
```

## Build performance

- Enable Gradle build cache: `org.gradle.caching=true` in `gradle.properties`.
- Enable parallel execution: `org.gradle.parallel=true`.
- Allocate more JVM heap: `org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8`.
- Use KSP (faster than KAPT) — already wired above for Hilt.

```properties
# gradle.properties
org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
android.useAndroidX=true
kotlin.code.style=official
```
