plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.spectech.uikit"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin { jvmToolchain(17) }
}

dependencies {
    api(project(":core:domain"))

    api(platform(libs.compose.bom))
    api(libs.compose.ui)
    api(libs.compose.material3)
    api(libs.compose.ui.graphics)
    api(libs.compose.material.icons.extended)
    api(libs.compose.ui.tooling.preview)
    debugApi(libs.compose.ui.tooling)

    api(libs.androidx.lifecycle.runtime.compose)
    api(libs.coil.compose)
    implementation(libs.androidx.core.ktx)
}
