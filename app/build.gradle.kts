plugins {
    id("com.android.library")
    kotlin("android")
    `maven-publish`
}

val currentVersion = "1.0.12"

group = "com.acsbendi"
version = currentVersion

android {
    compileSdk = 31
    namespace = "com.acsbendi.requestinspectorwebview"

    defaultConfig {
        minSdk = 21
        targetSdk = 31

        version = currentVersion
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = group as String
            artifactId = "requestinspectorwebview"
            version = currentVersion

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}
