# Compose + Room + MediaPipe-friendly defaults.
-keep class com.google.mediapipe.** { *; }
-keep class androidx.room.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }
