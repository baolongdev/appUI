# ===== KEEP BUILDCONFIG =====
-keep class com.example.appui.BuildConfig { *; }
-keepclassmembers class com.example.appui.BuildConfig {
    public static <fields>;
}

# ===== RETROFIT & OKHTTP =====
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class retrofit2.** { *; }
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# ===== MOSHI - CRITICAL FOR FIXING LINKEDHASHTREEMAP ERROR =====
-dontwarn com.squareup.moshi.**
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }

# Keep Moshi annotations
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations

# Keep JsonAdapter
-keep,allowobfuscation,allowshrinking class com.squareup.moshi.JsonAdapter

# Keep @JsonClass annotations
-keepclassmembers @com.squareup.moshi.JsonClass class * {
    <init>(...);
    <fields>;
}

# Keep @Json annotated fields
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}

# Keep ToJson/FromJson methods
-keepclassmembers class * {
    @com.squareup.moshi.FromJson <methods>;
    @com.squareup.moshi.ToJson <methods>;
}

# Keep Moshi Kotlin support
-keep class kotlin.reflect.jvm.internal.** { *; }
-keep class kotlin.Metadata { *; }

# ===== KEEP ALL API MODELS (CRITICAL FIX) =====
# Keep all data classes used with Retrofit/Moshi
-keep class com.example.appui.data.remote.**.models.** { *; }
-keep class com.example.appui.data.remote.**.model.** { *; }
-keep class com.example.appui.domain.model.** { *; }
-keep class com.example.appui.data.model.** { *; }

# Keep all fields and constructors for data classes
-keepclassmembers class com.example.appui.data.remote.**.models.** {
    <init>(...);
    <fields>;
}
-keepclassmembers class com.example.appui.data.remote.**.model.** {
    <init>(...);
    <fields>;
}
-keepclassmembers class com.example.appui.domain.model.** {
    <init>(...);
    <fields>;
}

# ===== SPECIFICALLY KEEP ELEVENLABS MODELS =====
-keep class com.example.appui.data.remote.elevenlabs.models.** { *; }
-keepclassmembers class com.example.appui.data.remote.elevenlabs.models.** {
    *;
}

# Keep AgentSummary and related classes
-keep class com.example.appui.data.remote.elevenlabs.models.AgentSummary { *; }
-keep class com.example.appui.data.remote.elevenlabs.models.AgentConfig { *; }
-keep class com.example.appui.data.remote.elevenlabs.models.ConversationConfig { *; }
-keep class com.example.appui.data.remote.elevenlabs.models.* { *; }

# ===== GSON (for GitHub API) =====
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep GitHub API models
-keep class com.example.appui.data.remote.github.models.** { *; }

# ===== HILT/DAGGER =====
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-dontwarn com.google.errorprone.annotations.**

# ===== COMPOSE =====
-keep class androidx.compose.** { *; }
-keep interface androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ===== ELEVENLABS SDK =====
-keep class io.elevenlabs.** { *; }
-keep interface io.elevenlabs.** { *; }
-dontwarn io.elevenlabs.**

# ===== ROOM DATABASE =====
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-dontwarn androidx.room.paging.**

# ===== KOTLIN =====
# Keep data classes
-keepclassmembers class * {
    public <init>(...);
}

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }
-keepattributes RuntimeVisibleAnnotations
-keepattributes AnnotationDefault

# ===== KOTLIN COROUTINES =====
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ===== WORKMANAGER =====
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.InputMerger
-keep class androidx.work.impl.** { *; }

# ===== DATASTORE =====
-keep class androidx.datastore.*.** { *; }

# ===== KEEP SERIALIZABLE =====
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ===== KEEP ENUMS =====
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ===== REMOVE LOGGING IN RELEASE =====
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# ===== REFLECTION =====
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod
