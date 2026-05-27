# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep our serializable domain models
-keep,allowobfuscation,allowshrinking class com.spectech.domain.** { *; }

# ViewModels (kept by Hilt/AndroidX already, but defensive)
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Ktor + OkHttp
-keep class io.ktor.** { *; }
-keep class okhttp3.** { *; }
-keepclassmembers class okhttp3.** { *; }
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
