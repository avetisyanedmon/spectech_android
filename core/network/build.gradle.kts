plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(project(":core:domain"))

    api(libs.ktor.client.core)
    api(libs.ktor.client.okhttp)
    api(libs.ktor.client.content.negotiation)
    api(libs.ktor.serialization.kotlinx.json)
    api(libs.ktor.client.logging)

    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.mockwebserver)
}

tasks.withType<Test> { useJUnitPlatform() }
