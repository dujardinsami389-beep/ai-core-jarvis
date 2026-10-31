plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "fr.aicore.jarvis"
    compileSdk = 35
    defaultConfig {
        applicationId = "fr.aicore.jarvis.fresh"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "2.1"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies {
    implementation("com.google.ai.edge.litertlm:litertlm-android:latest.release")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
