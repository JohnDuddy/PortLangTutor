# Duddy Portugues release hardening.
#
# Keep rules here are intentionally scoped to reflection-heavy libraries and
# app classes that are serialized, loaded by Room, or parsed across network
# boundaries. R8 can still shrink and optimize the rest of the release build.

# Supabase / Ktor
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-dontwarn kotlinx.coroutines.debug.**

# Room
-keep class com.duddy.portugues.data.local.entity.** { *; }
-keep class com.duddy.portugues.data.local.dao.** { *; }
-keep class com.duddy.portugues.data.local.DuddyDatabase { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keep,includedescriptorclasses class com.duddy.portugues.**$$serializer { *; }
-keepclassmembers class com.duddy.portugues.** {
    *** Companion;
}
-keepclasseswithmembers class com.duddy.portugues.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# App models exchanged with local assets, backend JSON, and persisted state
-keep class com.duddy.portugues.data.model.** { *; }
-keep class com.duddy.portugues.data.auth.** { *; }

# OkHttp / Okio
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# JSON parsing through org.json should preserve enum names used in persisted state.
-keepclassmembers enum com.duddy.portugues.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
