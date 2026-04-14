import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.mediakasir.apotekpos"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mediakasir.apotekpos"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Produksi; debug override lewat local.properties → dev.baseUrl (Laravel lokal / staging).
        buildConfigField("String", "BASE_URL", "\"https://apoapps.sekawanputrapratama.com/api/\"")
        buildConfigField("String", "DEV_API_HOST_HEADER", "\"\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"\"")
    }

    buildTypes {
        debug {
            // local.properties (jangan commit): dev.baseUrl wajib untuk API lokal — tanpa ini, debug pakai BASE_URL produksi.
            // Emulator → PC: http://10.0.2.2:8000/api/ (artisan), atau http://10.0.2.2/api/ + dev.apiHostHeader=apoapps.test (Laragon)
            // dev.googleWebClientId, dev.apiHostHeader, adb reverse: lihat komentar di local.properties contoh proyek.
            val lp = Properties()
            val localFile = rootProject.file("local.properties")
            if (localFile.exists()) {
                localFile.inputStream().use { lp.load(it) }
            }
            lp.getProperty("dev.baseUrl")?.trim()?.takeIf { it.isNotEmpty() }?.let { url ->
                buildConfigField("String", "BASE_URL", "\"$url\"")
            }
            lp.getProperty("dev.apiHostHeader")?.trim()?.takeIf { it.isNotEmpty() }?.let { host ->
                buildConfigField("String", "DEV_API_HOST_HEADER", "\"$host\"")
            }
            lp.getProperty("dev.googleWebClientId")?.trim()?.takeIf { it.isNotEmpty() }?.let { id ->
                buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$id\"")
            }
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
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
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.datastore.preferences)
    implementation(libs.gson)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("androidx.lifecycle:lifecycle-process:2.8.6")
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.google.id)
    debugImplementation(libs.androidx.ui.tooling)
}
