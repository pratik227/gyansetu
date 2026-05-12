import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

android {
    namespace = "com.gyansetu"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.gyansetu"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        // Gemma 4 .litertlm model file is NOT bundled into the APK (~2.5 GB).
        // The model is downloaded on first launch into the app's private files dir.
        // Override via local.properties or BuildConfig at build time.
        //
        // litert-community/gemma-4-E2B-it-litert-lm/gemma-4-E2B-it.litertlm — Apache 2.0,
        // the LiteRT-LM proprietary format (magic header "LITERTLM"). Loaded by the
        // com.google.ai.edge.litertlm:litertlm-android library — MediaPipe tasks-genai
        // cannot read it (the .task file in the same repo is for the web JS runtime only).
        buildConfigField(
            "String", "GEMMA_LITERTLM_URL",
            "\"https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm\""
        )
        buildConfigField("String", "GEMMA_LITERTLM_FILE", "\"gemma-4-E2B-it.litertlm\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug { isMinifyEnabled = false }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    sourceSets["main"].kotlin.srcDirs("src/main/kotlin")

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // CameraX (camera scan)
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")

    // Room (offline syllabus DB)
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // LiteRT-LM (on-device Gemma 4 .litertlm runtime). Loads the LITERTLM-format
    // model file Google publishes; supports GPU + CPU + multimodal (image/audio).
    implementation("com.google.ai.edge.litertlm:litertlm-android:0.10.2")

    // ML Kit on-device object detection (used as a fast first-pass classifier
    // before handing off to Gemma for the bilingual story generation).
    implementation("com.google.mlkit:object-detection:17.0.2")

    // Networking — only used to download the Gemma model on first launch
    // when not present locally. After download the app is fully offline.
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // DataStore — preferences (language, font size, sound, stars).
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // AppCompat — needed for runtime per-app locale switching via
    // AppCompatDelegate.setApplicationLocales (works back to API 14, vs the
    // system LocaleManager which is API 33+).
    implementation("androidx.appcompat:appcompat:1.7.0")
}
