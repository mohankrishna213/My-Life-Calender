-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }

-keepclassmembers class com.mohanbuilds.focus.** { *** Companion; }
-keepclasseswithmembers class com.mohanbuilds.focus.** { kotlinx.serialization.KSerializer serializer(...); }
-keepclassmembers,includedescriptorclasses class com.mohanbuilds.focus.**$$serializer { *; }

-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keepclassmembers class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

-keep class com.mohanbuilds.focus.data.** { *; }

# Tink / security-crypto transitive annotation dependencies
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.concurrent.GuardedBy
