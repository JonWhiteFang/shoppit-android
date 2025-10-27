# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ================================================================================================
# GENERAL OPTIMIZATION AND SECURITY
# ================================================================================================

# Preserve line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable

# Hide the original source file name for security
-renamesourcefileattribute SourceFile

# Keep generic signatures for reflection
-keepattributes Signature

# Keep annotations for runtime processing
-keepattributes *Annotation*

# Keep exception information
-keepattributes Exceptions

# Optimization: Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

-assumenosideeffects class timber.log.Timber {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# ================================================================================================
# HILT / DAGGER
# ================================================================================================

-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Keep Hilt generated classes
-keep class **_HiltModules { *; }
-keep class **_HiltComponents { *; }
-keep class **_Factory { *; }
-keep class **_MembersInjector { *; }

# ================================================================================================
# ROOM DATABASE
# ================================================================================================

-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.paging.**

# Keep Room generated classes
-keep class **_Impl { *; }

# Keep database entities and their fields
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public abstract **Dao *();
}

# ================================================================================================
# RETROFIT & NETWORKING
# ================================================================================================

-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Keep data classes used for JSON serialization
-keep class com.shoppit.app.data.remote.dto.** { *; }
-keepclassmembers class com.shoppit.app.data.remote.dto.** { *; }

# ================================================================================================
# OKHTTP
# ================================================================================================

-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ================================================================================================
# GSON
# ================================================================================================

-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ================================================================================================
# KOTLIN
# ================================================================================================

-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ================================================================================================
# COMPOSE
# ================================================================================================

-keep class androidx.compose.** { *; }
-keep @androidx.compose.runtime.Composable class * { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

# ================================================================================================
# DOMAIN MODELS
# ================================================================================================

# Keep domain models and their fields for serialization
-keep class com.shoppit.app.domain.model.** { *; }
-keepclassmembers class com.shoppit.app.domain.model.** { *; }

# ================================================================================================
# SECURITY
# ================================================================================================

# Keep encrypted SharedPreferences classes
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }

# Keep security-related classes
-keep class javax.crypto.** { *; }
-keep class java.security.** { *; }

# ================================================================================================
# PARCELABLE
# ================================================================================================

-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# ================================================================================================
# SERIALIZABLE
# ================================================================================================

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}