plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "dev.miguelehr.truequeropa"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.miguelehr.truequeropa"
        minSdk = 33
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Actualizar estas versiones:
    implementation("androidx.navigation:navigation-compose:2.9.6")  // era 2.8.3
    implementation("androidx.compose.material:material-icons-extended")
    implementation("io.coil-kt:coil-compose:2.7.0")  // era 2.6.0
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")  // era 2.8.6
    implementation("androidx.activity:activity-compose:1.12.0")  // era 1.9.0

    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Firebase BoM - Actualizar versión:
    implementation(platform("com.google.firebase:firebase-bom:34.6.0"))  // era 34.4.0
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")

    // Coroutines - Actualizar versión:
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")  // era 1.8.1
}
    apply(plugin = "com.google.gms.google-services")