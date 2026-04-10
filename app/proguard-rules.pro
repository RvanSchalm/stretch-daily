# --- kotlinx.serialization ---
# Keep @Serializable data classes and their generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.stretchdaily.app.**$$serializer { *; }
-keepclassmembers class com.stretchdaily.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.stretchdaily.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Room ---
# Room generates code that reflection-proofs itself, but keep entity
# class names for error messages and migration debugging.
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# --- Hilt / Dagger ---
# Hilt's generated components reference user code by name. The Hilt
# Gradle plugin already injects the bulk of the rules, but keep the
# annotation-processor-generated modules safe.
-dontwarn dagger.hilt.internal.**
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper

# --- Compose ---
# Keep Compose-generated lambdas from being removed or renamed in
# ways that break recomposition.
-dontwarn androidx.compose.**
