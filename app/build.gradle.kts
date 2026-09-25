@file:Suppress("DEPRECATION", "UnstableApiUsage")

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")              // ✅ Needed for Hilt compiler
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")         // ✅ Hilt plugin
    id("com.google.devtools.ksp")                // ✅ For Room
    id("com.google.gms.google-services")
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
}

android {
    namespace = "com.kushal.mealapp"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.kushal.mealapp"
        minSdk = 28
        //noinspection OldTargetApi
        targetSdk = 36
        versionCode = 27
        versionName = "2.7"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/INDEX.LIST",
                "/META-INF/DEPENDENCIES",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*"
            )
        }
    }

    buildToolsVersion = "36.0.0"
}

configurations.all {
    exclude(group = "com.intellij", module = "annotations")
}


dependencies {


    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.multidex)

    // Jetpack Compose
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material)
    implementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material3.window.size.class1)
    implementation(libs.androidx.runtime.livedata)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)

    // --- Room with KSP ---
    implementation(libs.androidx.room.runtime.v261)
    implementation(libs.androidx.room.ktx.v261)
    implementation(libs.animated.vector.drawable)
    implementation(libs.androidx.room.compiler)
    implementation(libs.androidx.runtime)
    ksp(libs.androidx.room.compiler)

    // --- Hilt with KAPT ---
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler) // ✅ REQUIRED for Hilt

    // Networking
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.volley)
    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)


    // Google Play Services
    implementation(libs.play.services.auth)
    implementation(libs.play.services.auth.api.phone)
    implementation(libs.play.services.ads)
    implementation(libs.play.services.auth.v2100)
    implementation(libs.gms.play.services.ads)
    implementation(libs.gms.play.services.auth)
    implementation(libs.gms.play.services.auth.api.phone)


    // Google APIs
    implementation(libs.google.api.client)
    implementation(libs.google.api.client.android)
    implementation(libs.google.api.client.android.v1332)
    implementation(libs.google.api.services.drive)
    implementation(libs.google.api.services.drive.vv3rev20230815200)
    implementation(libs.google.api.services.drive.vv3rev2021250)
    implementation(libs.google.api.services.storage)
    implementation(libs.google.api.client)
    implementation(libs.google.http.client.gson)
    implementation(libs.google.oauth.client.jetty)
    implementation(libs.google.oauth.client.jetty.v1341)
    implementation(platform(libs.firebase.bom))
    implementation(libs.com.google.firebase.firebase.auth.ktx2)
    val firebaseBom = platform("com.google.firebase:firebase-bom:33.1.2")
    implementation(firebaseBom)
    implementation(libs.com.google.firebase.firebase.auth.ktx)
    implementation(libs.com.google.firebase.firebase.firestore.ktx)
    implementation(libs.firebase.storage.ktx)


    // Import the BoM for Firebase
    // Firebase
    implementation(libs.google.firebase.auth)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.google.firebase.firestore.ktx)
    implementation(libs.google.firebase.auth.ktx)
    implementation(libs.google.firebase.storage.ktx)

// or latest version
    implementation(libs.annotations)


    // Declare the Firebase libraries you need (no version numbers)
    implementation(libs.com.google.firebase.firebase.auth.ktx)
    implementation(libs.com.google.firebase.firebase.firestore.ktx)
    implementation(libs.firebase.storage.ktx)


    // Browser support
    implementation(libs.customtabs)
    implementation(libs.androidx.browser)

    // DateTime
    implementation(libs.datetime)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)


    val composeBom = platform("androidx.compose:compose-bom:2025.10.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Choose one of the following:
    // Material Design 3
    implementation(libs.material3)
    // or skip Material Design and build directly on top of foundational components
    implementation(libs.androidx.foundation)
    // or only import the main APIs for the underlying toolkit systems,
    // such as input and measurement/layout
    implementation(libs.ui)

    // Android Studio Preview support
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.ui.tooling)

    // UI Tests
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Optional - Add window size utils
    implementation(libs.androidx.adaptive)

    // Optional - Integration with activities
    implementation(libs.androidx.activity.compose)
    // Optional - Integration with ViewModels
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // Optional - Integration with LiveData
    implementation(libs.androidx.runtime.livedata.v1100alpha05)
    // Optional - Integration with RxJava
    implementation(libs.androidx.runtime.rxjava2)
    implementation(libs.integrity)


    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)


    // Vico Charts for Jetpack Compose
    implementation(libs.compose.m3)
    implementation(libs.vico.core)
    implementation(libs.compose)


    implementation(libs.app.update)
    implementation(libs.app.update.ktx)


}

