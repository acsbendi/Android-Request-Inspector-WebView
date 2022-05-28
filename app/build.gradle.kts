plugins {
    id("com.android.library")
    kotlin("android")
}

group = "com.acsbendi.requestinspectorwebview"
version = "0.0.1"

android {
    compileSdk = 31
    namespace = "com.acsbendi.requestinspectorwebview"

    defaultConfig {
        minSdk = 21
        targetSdk = 31
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
