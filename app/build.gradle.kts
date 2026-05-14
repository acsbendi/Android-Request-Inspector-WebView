import org.gradle.api.publish.PublishingExtension

plugins {
    id("com.android.library")
    kotlin("android")
}

apply(plugin = "maven-publish")

val currentVersion = "1.1.2"

group = "com.acsbendi"
version = currentVersion

android {
    compileSdk = 36
    namespace = "com.acsbendi.requestinspectorwebview"

    defaultConfig {
        minSdk = 21
        testOptions {
            targetSdk = 36
        }
        lint {
            targetSdk = 36
        }
        version = currentVersion
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

kotlin {
    jvmToolchain(21)
}

configure<PublishingExtension> {
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

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
}
