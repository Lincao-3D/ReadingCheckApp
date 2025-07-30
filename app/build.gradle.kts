import java.util.Properties
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    id("androidx.room")
    alias(libs.plugins.google.services)
}

// OLD kapt annotations plugin
// id("org.jetbrains.kotlin.kapt")

// Load API key from local.properties safely BEFORE android{}
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        try {
            localPropertiesFile.inputStream().use { load(it) }
        } catch (_: Exception) {
            // Optionally log failure
        }
    }
}
val openaiApiKey: String = localProperties.getProperty("OPENAI_API_KEY") ?: ""
println("DEBUG: Raw openaiApiKey from local.properties: '$openaiApiKey'") // <-- DEBUG LINE

android {
    namespace = "com.example.bprogress"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.bprogress"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Use the read openaiApiKey variable for resValue and buildConfigField
        resValue("string", "openai_api_key", openaiApiKey)

        val processedApiKeyForBuildConfig = "\"$openaiApiKey\""
        println("DEBUG: Value being passed to buildConfigField: '$processedApiKeyForBuildConfig'") // <-- DEBUG LINE
        buildConfigField("String", "OPENAI_API_KEY", processedApiKeyForBuildConfig)
    }
    /*        OLD KAPT IMPLEMENTATION ABOVE
                javaCompileOptions {
                annotationProcessorOptions {
                    arguments += mapOf("room.schemaLocation" to "$projectDir/schemas")
                }
            }*/
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
        freeCompilerArgs += listOf("-opt-in=kotlin.RequiresOptIn")
    }
    // enabled build configures down below, to use local properties.
    buildFeatures {
        viewBinding = true
        buildConfig   = true
    }
    room {
        schemaDirectory("$projectDir/schemas")
    }
}


dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.cardview)
    implementation("com.google.android.gms:play-services-ads:22.0.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    /*    implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.coroutines.android)*/

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    /*    val room_version = "2.7.2"
        ksp("androidx.room:room-compiler:$room_version") // Correct for Room KSP*/
    implementation(libs.androidx.room.ktx)

    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.media)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    val work_version = "2.9.0"
    implementation(libs.androidx.work.runtime.ktx) // Core WorkManager runtime
    // Optional - Kotlin extensions and coroutine support
    //val work_version = "1.1.2"

    // (Java only)
    implementation("androidx.work:work-runtime:$work_version")

    // Kotlin + coroutines
    implementation("androidx.work:work-runtime-ktx:$work_version")

    // optional - RxJava2 support
    implementation("androidx.work:work-rxjava2:$work_version")

    // optional - GCMNetworkManager support
    implementation("androidx.work:work-gcm:$work_version")

    // optional - Test helpers
    androidTestImplementation("androidx.work:work-testing:$work_version")

    // optional - Multiprocess support
    implementation("androidx.work:work-multiprocess:$work_version")

    //HILT
    val hiltVersion = "2.48"          //  Hilt core version
    val androidxHiltVersion = "1.1.0"   //  Androidx.hilt version
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    ksp("com.google.dagger:hilt-android-compiler:$hiltVersion")

    implementation("androidx.hilt:hilt-work:$androidxHiltVersion")
    ksp("androidx.hilt:hilt-compiler:$androidxHiltVersion")
    // Hilt Dependencies - Using TOML aliases
    /*    implementation(libs.hilt.android)      // com.google.dagger:hilt-android:2.52.2
        kapt(libs.hilt.compiler)               // com.google.dagger:hilt-compiler:2.52.2
            implementation(libs.hilt.work)         // androidx.hilt:hilt-work:1.2.0
        ksp(libs.hilt.android.compiler)       // androidx.hilt:hilt-compiler:1.2.0*/
    ksp(libs.androidx.room.compiler) // <<--- JUST ADDED THIS
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")

    // AI LOGIC
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    //okhttp3
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
}
