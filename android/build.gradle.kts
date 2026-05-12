// Top-level build file. Plugin versions live here so subprojects share them.
plugins {
    id("com.android.application") version "8.7.0" apply false
    // Kotlin 2.3.x is required by com.google.ai.edge.litertlm:litertlm-android:0.10.2
    // (its metadata is tagged 2.3.0; older compilers reject it).
    id("org.jetbrains.kotlin.android") version "2.3.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
    id("com.google.devtools.ksp") version "2.3.7" apply false
}
