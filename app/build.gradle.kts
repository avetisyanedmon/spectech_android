plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.spectech.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.spectech.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        vectorDrawables.useSupportLibrary = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin { jvmToolchain(17) }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // Lint catches missing Russian translations across every module via the
    // standard `MissingTranslation` check. We promote it to an error so a
    // dropped key in `values-ru/strings.xml` blocks the build before CI
    // notices. `ExtraTranslation` is informational and stays at warning so
    // a temporary EN-only key during development doesn't break the build.
    lint {
        checkDependencies = true
        warningsAsErrors = false
        error += "MissingTranslation"
        disable += "GradleDependency" // noisy BOM-pinned coordinates
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
    implementation(libs.androidx.appcompat)
    implementation(libs.android.material)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.splashscreen)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.kotlinx.serialization.json)

    // Firebase Cloud Messaging. The Google Services Gradle plugin is NOT
    // applied by default — the build works without `google-services.json`,
    // but push won't actually fire until the team:
    //   1. Drops the real `google-services.json` into `app/`
    //   2. Adds `id("com.google.gms.google-services")` to this plugins block
    // FcmService is registered in the manifest regardless; it stays inert
    // while Firebase is uninitialized.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    implementation(libs.timber)

    testImplementation(libs.junit.jupiter)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
