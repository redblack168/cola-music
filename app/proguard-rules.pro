# Keep Subsonic API model classes (kotlinx.serialization)
-keep class com.colamusic.core.network.dto.** { *; }
-keep class com.colamusic.core.model.** { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keep,includedescriptorclasses class com.colamusic.**$$serializer { *; }
-keepclassmembers class com.colamusic.** {
    *** Companion;
}
-keepclasseswithmembers class com.colamusic.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Media3
-keep class androidx.media3.** { *; }

# OkHttp
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Hilt
-keep,allowobfuscation,allowshrinking @dagger.hilt.android.lifecycle.HiltViewModel class *
