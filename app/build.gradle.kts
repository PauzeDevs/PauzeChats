/*
 * Copyright © 2026 Aarav Singh (Pauze). All rights reserved.
 */

import java.net.URI

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val apiBaseUrl = (
    providers.gradleProperty("pauzeApiBaseUrl").orNull
        ?: providers.environmentVariable("PAUZE_API_BASE_URL").orNull
        ?: ""
    ).trim().trimEnd('/')
val matrixHomeserverUrl = providers.gradleProperty("pauzeMatrixHomeserverUrl").orNull ?: ""

val parsedApiBaseUrl = runCatching { URI(apiBaseUrl) }.getOrNull()
require(
    apiBaseUrl.isBlank() || (
        parsedApiBaseUrl?.scheme?.equals("https", ignoreCase = true) == true &&
            !parsedApiBaseUrl?.host.isNullOrBlank() &&
            parsedApiBaseUrl?.userInfo == null &&
            parsedApiBaseUrl?.query == null &&
            parsedApiBaseUrl?.fragment == null
        )
) {
    "PauzeChats API base URL must be an HTTPS URL without credentials, query, or fragment"
}

val apiBaseUrlBuildConfigValue =
    "\"" + apiBaseUrl.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "com.pauze.chats"
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        applicationId = "com.pauze.chats"
        buildConfigField("String", "PAUZE_API_BASE_URL", apiBaseUrlBuildConfigValue)
        buildConfigField("String", "PAUZE_MATRIX_HOMESERVER_URL", "\"$matrixHomeserverUrl\"")
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0-alpha01"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation("io.coil-kt.coil3:coil-gif:3.3.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:5.5.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("org.matrix.rustcomponents:sdk-android:26.09.9")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("org.jetbrains.kotlin:kotlin-test:2.2.10")
    testImplementation("junit:junit:4.13.2")
}
