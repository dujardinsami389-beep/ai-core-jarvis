plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "fr.aicore.jarvis"
    compileSdk = 35
    defaultConfig {
        applicationId = "fr.aicore.jarvis"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "2.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("com.google.ai.edge.litertlm:litertlm-android:latest.release")
}
