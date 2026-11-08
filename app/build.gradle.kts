/**
 * Version derived from the commit count, so every build is distinguishable
 * without anyone remembering to bump a number - the first three builds all
 * shipped as versionCode 1 precisely because that relied on memory.
 *
 * Needs full history: CI must check out with fetch-depth: 0, or this falls
 * back to 1.
 */
fun gitCommitCount(): Int = try {
    val proc = ProcessBuilder("git", "rev-list", "--count", "HEAD")
        .directory(rootDir)
        .redirectErrorStream(true)
        .start()
    proc.inputStream.bufferedReader().readText().trim().toIntOrNull() ?: 1
} catch (e: Exception) {
    1
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.tiimoo.cmfstereo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tiimoo.cmfstereo"
        minSdk = 26
        targetSdk = 35
        versionCode = gitCommitCount()
        versionName = "1.0.${gitCommitCount()}-beta"
    }

    // Two apps from one codebase, installable side by side:
    //   pro   - the full manager with the developer tools (the original app)
    //   clean - for everyday use: Nothing's design language, NDot type, plain
    //           1-10 levels instead of register values, no developer tools
    // Both drive the same module through the same StereoCtl (src/main).
    flavorDimensions += "edition"
    productFlavors {
        create("pro") {
            dimension = "edition"
            resValue("string", "app_name", "CMF Stereo Pro")
            // Quick Settings tile: each edition named, so two installed ones differ.
            resValue("string", "qs_label", "Stereo Pro")
        }
        create("clean") {
            dimension = "edition"
            applicationIdSuffix = ".clean"
            resValue("string", "app_name", "CMF Stereo")
            resValue("string", "qs_label", "Stereo")
        }
    }

    // One permanent key, so every release installs over the last one (and app
    // stores such as Komi Store can update it). CI writes the keystore from the
    // repository secrets; without them (a local build) release falls back to
    // the debug key.
    val keystore = System.getenv("SIGNING_KEYSTORE")?.let { file(it) }?.takeIf { it.exists() }
    signingConfigs {
        if (keystore != null) {
            create("release") {
                storeFile = keystore
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // R8 strips the unused parts of Compose and the libraries; without it
            // the release APK carried all of them (it was over 90 MB).
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
                ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    // LSPosed / Xposed API, provided by the framework at runtime (nothing/SettingsHook).
    compileOnly("de.robv.android.xposed:api:82")
}
