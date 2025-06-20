plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("io.realm.kotlin")
    kotlin("kapt") // For Realm annotations
}

android {
    namespace = "com.example.bprogress"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.bprogress"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.1") // added latest
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core) // last from default doc
    // Realm Kotlin SDK
    implementation("io.realm.kotlin:library-base:2.3.0") // Use the same LATEST_REALM_VERSION as in the project-level file

    // Using Realm Sync (optional)
    implementation("io.realm.kotlin:library-sync:2.3.0")

    // Kotlin Coroutines (Realm Kotlin uses coroutines extensively)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2") // Or latest
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2") // Or latest


}
