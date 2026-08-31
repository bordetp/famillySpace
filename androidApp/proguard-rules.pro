-keepattributes *Annotation*
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keep,includedescriptorclasses class com.zam.shared.**$$serializer { *; }
-keepclassmembers class com.zam.shared.** { *; }

-keep class com.zam.photos.app.BuildConfig { *; }

-keep class io.ktor.client.plugins.auth.** { *; }
-keep class io.ktor.client.plugins.websocket.** { *; }
-keep class io.ktor.websocket.** { *; }
-dontwarn kotlinx.serialization.**
-dontwarn org.slf4j.**

-keep class coil.** { *; }

-keep class com.google.android.gms.auth.api.identity.** { *; }
-keep class com.google.android.gms.common.** { *; }
-keep class com.google.android.play.core.** { *; }
-dontwarn com.google.android.play.core.**

-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
