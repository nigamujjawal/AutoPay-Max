import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.google.services)
}

// Backend base URL lives only in the gitignored local.properties, never in a committed file -
// keeps it out of source control while still being available to the app via BuildConfig.
val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        load(FileInputStream(localFile))
    }
}
val soundBoxApiBaseUrl: String = localProperties.getProperty("API_BASE_URL")
    ?: error("Missing API_BASE_URL in local.properties - add it as API_BASE_URL=<backend base url>")
val appStorysAppId: String = localProperties.getProperty("APPSTORYS_APP_ID")
    ?: error("Missing APPSTORYS_APP_ID in local.properties - add it as APPSTORYS_APP_ID=<app id>")
val appStorysAccountId: String = localProperties.getProperty("APPSTORYS_ACCOUNT_ID")
    ?: error("Missing APPSTORYS_ACCOUNT_ID in local.properties - add it as APPSTORYS_ACCOUNT_ID=<account id>")

android {
    namespace = "com.autopaymax"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.autopaymax"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "API_BASE_URL", "\"$soundBoxApiBaseUrl\"")
        buildConfigField("String", "APPSTORYS_APP_ID", "\"$appStorysAppId\"")
        buildConfigField("String", "APPSTORYS_ACCOUNT_ID", "\"$appStorysAccountId\"")
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
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    
    // Material Icons Extended
    implementation(libs.androidx.compose.material.icons.extended)

    // Navigation & Hilt Integration
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Hilt Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // DataStore Preferences & WorkManager & Biometrics
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.biometric)

    // Firebase (Google-federated auth bridge + Remote Config)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.config)
    implementation(libs.kotlinx.coroutines.play.services)

    // In-app Google sign-in (Credential Manager) + Gmail readonly authorization
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.google.identity.googleid)
    implementation(libs.play.services.auth)

    // Networking (SoundBox backend)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation("com.github.appversal:AppStorys-Android-SDK-Downgraded:3.9.5")

    // Source: https://mvnrepository.com/artifact/com.appsflyer/af-android-sdk
    implementation("com.appsflyer:af-android-sdk:7.0.1")
}