plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.staffsyncapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.staffsyncapp"
        minSdk = 29
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures { // Allow binding
        viewBinding = true
        dataBinding = true
    }
}

dependencies {

    // navigation components for fragment and UI navigation - version 2.7.5
    implementation(libs.navigation.fragment.v275) 
    implementation(libs.navigation.ui.v275)

    // core Android libraries for UI and material design
    implementation(libs.appcompat) // Provides backward compatibility for Android UI
    implementation(libs.material) // Includes Material Design components
    implementation(libs.activity) // Activity support for app lifecycle handling
    implementation(libs.constraintlayout) // Flexible UI layout system

    // navigation libraries for fragment and UI (default/latest versions)
    implementation(libs.navigation.fragment) // Fragment navigation support
    implementation(libs.navigation.ui) // Navigation UI components

    // Google Play Services library for location services
    implementation(libs.play.services.location) // Access to location-based features

    // networking library for HTTP requests
    implementation(libs.volley) // Handles network operations efficiently

    // testing dependencies
    testImplementation(libs.junit) // Unit testing framework
    androidTestImplementation(libs.ext.junit) // Extended Android-specific testing
    androidTestImplementation(libs.espresso.core) // UI testing framework
}