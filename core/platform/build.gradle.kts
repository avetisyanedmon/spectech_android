plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.spectech.platform"
    compileSdk = 35

    defaultConfig {
        minSdk = 26

        buildConfigField("String", "API_BASE_URL", "\"https://spectech-backoffice.onrender.com/api\"")
        buildConfigField("String", "API_CLIENT_ID", "\"ios-app\"")
        buildConfigField(
            "String",
            "API_CLIENT_SECRET",
            "\"66ff056ee8fa15b144a54ab472222b0a7534fe16286d1cfb893f6495fe65be96\""
        )
        buildConfigField("boolean", "BYPASS_AUTH_FLOW", "false")
        buildConfigField("String", "BYPASS_PHONE", "\"+79990000000\"")
        buildConfigField("String", "BYPASS_CODE", "\"111111\"")
        // Default: pin certs (matches iOS). debug build type below opts out so
        // local proxy tooling (Charles, mitmproxy) still works during dev.
        buildConfigField("boolean", "PIN_CERTIFICATES", "true")
    }

    buildTypes {
        getByName("debug") {
            buildConfigField("boolean", "PIN_CERTIFICATES", "false")
        }
        getByName("release") {
            buildConfigField("boolean", "PIN_CERTIFICATES", "true")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin { jvmToolchain(17) }
}

dependencies {
    api(project(":core:domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.browser)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.timber)
}
