plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.agenterie.jarvis"
    // android-34 n'est pas installé sur le GEEKOM et n'a pas à l'être :
    // compileSdk >= targetSdk suffit (platforms/android-35 présent).
    compileSdk = 35

    defaultConfig {
        applicationId = "com.agenterie.jarvis"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0-p1"
    }

    buildTypes {
        release {
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
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    // ONNX Runtime — AAR réel mesuré : 27.7 Mo (estimation architecture : 12–15 Mo)
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.19.2")
    testImplementation("com.microsoft.onnxruntime:onnxruntime:1.19.2")
    testImplementation("junit:junit:4.13.2")
}
