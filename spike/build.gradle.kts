// Copyright 2026 Agenterie. Apache-2.0.
// APK jetable — instrument de mesure P0. Ne pas fusionner dans :app.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.agenterie.jarvis.spike"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.agenterie.jarvis.spike"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0-spike"
    }

    buildFeatures {
        // BuildConfig.DEBUG utilisé par InjectWakeReceiver pour se neutraliser en release.
        buildConfig = true
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    sourceSets {
        // Source set partagé : réutilise WakePipeline + WakeGate de JA-T4 sans les dupliquer.
        // Le manifest spike ne déclare QUE SpikeActivity + SpikeWakeService.
        getByName("main") {
            java.srcDirs(
                "src/main/java",
                "${rootProject.projectDir}/app/src/main/java"
            )
            assets.srcDirs(
                "src/main/assets",
                "${rootProject.projectDir}/app/src/main/assets",
                // Rend fixtures/hey_jarvis_espeak.wav accessible via assets.open()
                // dans InjectWakeReceiver sans dupliquer le fichier.
                "${rootProject.projectDir}/app/src/test/resources"
            )
        }
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.19.2")
    // Tests JVM (SpikeRotationGuardTest — pas de contexte Android requis)
    testImplementation("junit:junit:4.13.2")
}
