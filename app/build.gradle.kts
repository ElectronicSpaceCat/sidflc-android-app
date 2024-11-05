plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    kotlin("plugin.serialization")
}

android {
    namespace = "com.android.app"
    compileSdk = 34

    // Note: Need the NDK attribute to ensure library debug symbols get removed.
    //       The version can be found in Tools -> SDK Manager -> SDK Tools -> NDK (Side by side)
    //       Click "Show package details" to view the versions.
    ndkVersion = "26.3.11579264"

    defaultConfig {
        applicationId = "com.android.app"
        minSdk = 24
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
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Materials
    implementation("com.google.android.material:material:1.12.0")

    // Kotlin lang
    implementation("androidx.core:core-ktx:1.13.1")

    // App compat and UI things
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.6")

    // Navigation library
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.1")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.1")

    // Preference library
    implementation("androidx.preference:preference-ktx:1.2.1")

    // CameraX library
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    //WindowManager
    implementation("androidx.window:window:1.3.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Nordic Semiconductor libraries
    // The libraries may be included from jcenter. If you want to modify the code,
    // clone the projects from GitHub and put them in the project root folder.
    // Then add the library in the settings.gradle.kts file.
    implementation("no.nordicsemi.android:log:2.5.0")
    implementation("no.nordicsemi.android.support.v18:scanner:1.6.0")
    implementation("no.nordicsemi.android:dfu:2.6.0")
    implementation("no.nordicsemi.android:ble:2.8.0")
    implementation("no.nordicsemi.android:ble-common:2.8.0")
    implementation("no.nordicsemi.android:ble-livedata:2.8.0")

//    implementation(project(":dfu"))          // https://github.com/NordicSemiconductor/Android-DFU-Library
//    implementation(project(":ble"))          // https://github.com/NordicSemiconductor/Android-BLE-Library/tree/main/ble
//    implementation(project(":ble-common"))   // https://github.com/NordicSemiconductor/Android-BLE-Library/tree/main/ble-common
//    implementation(project(":ble-livedata")) // https://github.com/NordicSemiconductor/Android-BLE-Library/tree/main/ble-livedata

//    // Unit testing
//    testImplementation("androidx.test.ext:junit:1.1.3")
//    testImplementation("androidx.test:rules:1.4.0")
//    testImplementation("androidx.test:runner:1.4.0")
//    testImplementation("androidx.test.espresso:espresso-core:3.4.0")
//    testImplementation("org.robolectric:robolectric:4.4")
//
//    // Instrumented testing
//    androidTestImplementation("androidx.test.ext:junit:1.1.3")
//    androidTestImplementation("androidx.test:core:1.4.0")
//    androidTestImplementation("androidx.test:rules:1.4.0")
//    androidTestImplementation("androidx.test:runner:1.4.0")
//    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
//    implementation("org.jetbrains.kotlin:kotlin-script-runtime:1.7.10")
}
