plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "org.sosnetwork.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.sosnetwork.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Firebase Cloud Messaging package name
        resValue("string", "gcm_defaultSenderId", "YOUR_GOOGLE_CLOUD_PROJECT_SENDER_ID")
        resValue("string", "project_id", "YOUR_GOOGLE_CLOUD_PROJECT_ID")
        resValue("string", "google_crashlytics_badge_key", "123456789") // Get from Firebase console
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(project(":protocol"))
    
    // AndroidX Core
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    
    // Lifecycle & Coroutines
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    
    // Security & Crypto
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    
    // Google Play Services - Enhanced for SOS Network
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.android.gms:play-services-nearby:24.0.0") // Updated Nearby Connections
    implementation("com.google.firebase:firebase-core:21.1.1")
    implementation("com.google.firebase:firebase-crashlytics-ktx:19.3.0")
    implementation("com.google.firebase:firebase-messaging-ktx:24.1.2")
    
    // Coroutines & Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    
    // DataStore for Preferences
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    
    // Google Play Billing (for potential premium features)
    implementation("com.android.billingclient:billing:7.0.0")
}
