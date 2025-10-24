# ===== BUILDCONFIG =====
-keep class com.example.appui.BuildConfig { *; }

# ===== RETROFIT & OKHTTP =====
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class retrofit2.** { *; }

# ===== MOSHI =====
-dontwarn com.squareup.moshi.**
-keep class com.squareup.moshi.** { *; }
-keepclassmembers @com.squareup.moshi.JsonClass class * {
    <init>(...);
    <fields>;
}
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}

# ===== API MODELS =====
-keep class com.example.appui.data.remote.elevenlabs.** { *; }
-keep class com.example.appui.data.remote.github.** { *; }
-keep class com.example.appui.data.model.** { *; }
-keep class com.example.appui.domain.model.** { *; }

# ===== GSON =====
-keep class com.google.gson.** { *; }

# ===== HILT/DAGGER =====
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-dontwarn com.google.errorprone.annotations.**

# ===== AUDIO & PCM - CRITICAL =====
-keep class com.example.appui.core.audio.** { *; }
-keep class com.example.appui.core.capture.** { *; }

# Android Media APIs
-keep class android.media.AudioRecord { *; }
-keep class android.media.AudioTrack { *; }
-keep class android.media.AudioFormat { *; }
-keep class android.media.AudioManager { *; }

# ===== LIVEKIT & WEBRTC =====
-keep class io.livekit.** { *; }
-keep interface io.livekit.** { *; }
-dontwarn io.livekit.**

-keep class org.webrtc.** { *; }
-keep interface org.webrtc.** { *; }
-dontwarn org.webrtc.**

-keep class livekit.org.webrtc.** { *; }
-dontwarn livekit.org.webrtc.**

# JNI Zero
-keep class livekit.org.jni_zero.** { *; }
-keep class org.jni_zero.** { *; }
-dontwarn livekit.org.jni_zero.**
-dontwarn org.jni_zero.**

# Native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# ===== ROOM DATABASE =====
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# ===== DATA CLASSES =====
-keep class com.example.appui.data.local.** { *; }
-keep class com.example.appui.domain.repository.** { *; }

# ===== KOTLIN =====
-keep class kotlin.Metadata { *; }
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ===== WORKMANAGER =====
-keep class * extends androidx.work.Worker
-keep class androidx.work.impl.** { *; }

# ===== DATASTORE =====
-keep class androidx.datastore.*.** { *; }

# ===== RIVE ANIMATION =====
-keep class app.rive.runtime.** { *; }
-dontwarn app.rive.runtime.**

# ===== KIOSK & DEVICE OWNER =====
-keep class com.example.appui.deviceowner.** { *; }
-keep class com.example.appui.kiosk.** { *; }

# ===== ENUMS =====
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ===== SERIALIZABLE =====
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
}

# ===== ATTRIBUTES =====
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes RuntimeVisibleAnnotations
-keepattributes AnnotationDefault
-keepattributes SourceFile
-keepattributes LineNumberTable

# ===== SUPPRESS WARNINGS =====
-dontwarn javax.**
-dontwarn sun.misc.**
-dontwarn androidx.compose.**
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
