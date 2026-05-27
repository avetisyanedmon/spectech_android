plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}

subprojects {
    tasks.register("unitTestClasses") {
        description = "Provides a common task name for compiling unit test classes across Android and JVM modules"
        group = "verification"
        dependsOn(tasks.matching {
            it.name == "testDebugUnitTestClasses" || 
            it.name == "testClasses" ||
            it.name == "compileDebugUnitTestSources"
        })
    }
}
