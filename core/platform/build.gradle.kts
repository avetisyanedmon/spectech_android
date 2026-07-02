import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

// The HMAC signing secret must never be committed. Resolution order:
//   1. Gradle property   (-PSPECTECH_API_CLIENT_SECRET=... — CI)
//   2. Environment var   (SPECTECH_API_CLIENT_SECRET — CI)
//   3. local.properties  (gitignored — local development)
// The build fails fast with instructions when none is present.
val apiClientSecret: String = providers.gradleProperty("SPECTECH_API_CLIENT_SECRET").orNull
    ?: providers.environmentVariable("SPECTECH_API_CLIENT_SECRET").orNull
    ?: rootProject.file("local.properties").takeIf { it.exists() }?.let { file ->
        Properties()
            .apply { file.inputStream().use { load(it) } }
            .getProperty("SPECTECH_API_CLIENT_SECRET")
    }
    ?: error(
        "SPECTECH_API_CLIENT_SECRET is not set. Add " +
            "`SPECTECH_API_CLIENT_SECRET=<secret>` to local.properties, or pass it as " +
            "a Gradle property / environment variable in CI. See SETUP.md → 'API client secret'."
    )

android {
    namespace = "com.spectech.platform"
    compileSdk = 35

    defaultConfig {
        minSdk = 26

        buildConfigField("String", "API_BASE_URL", "\"https://spectech-backoffice.onrender.com/api\"")
        buildConfigField("String", "API_CLIENT_ID", "\"ios-app\"")
        buildConfigField("String", "API_CLIENT_SECRET", "\"$apiClientSecret\"")
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
