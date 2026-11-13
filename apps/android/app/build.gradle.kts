plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.azuredevid.cassy"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.azuredevid.cassy"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            buildConfigField("String", "BASE_API_URL", "\"http://10.0.2.2:8080\"")
            buildConfigField("Boolean", "IS_DEMO_ENVIRONMENT", "true")
        }
        create("staging") {
            dimension = "environment"
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            buildConfigField("String", "BASE_API_URL", "\"https://demo.abbdullaziz.online\"")
            buildConfigField("Boolean", "IS_DEMO_ENVIRONMENT", "true")
        }
        create("prod") {
            dimension = "environment"
            buildConfigField("String", "BASE_API_URL", "\"https://api.cassy.id\"")
            buildConfigField("Boolean", "IS_DEMO_ENVIRONMENT", "false")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.biometric:biometric:1.2.0-alpha05")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Ktor 3 Client & Network Socket HAL (Port 9100)
    val ktorVersion = "3.0.1"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-auth:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-client-websockets:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-network:$ktorVersion")

    // Koin Dependency Injection
    implementation("io.insert-koin:koin-androidx-compose:4.0.0")

    // Security & Persistence: Google Tink AEAD (Android Keystore) + Jetpack DataStore
    implementation("androidx.datastore:datastore:1.1.1")
    implementation("com.google.crypto.tink:tink-android:1.15.0")

    // Image Loading for WebP Menu Catalog from Cloudflare R2 CDN
    implementation("io.coil-kt:coil-compose:2.7.0")

    // CameraX & Barcode Scanner for Store Pairing QR
    val cameraxVersion = "1.4.0"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    // QR Code Generator (ZXing) for Dynamic Midtrans QRIS Bitmaps
    implementation("com.google.zxing:core:3.5.3")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")

    debugImplementation("androidx.compose.ui:ui-tooling")
}

