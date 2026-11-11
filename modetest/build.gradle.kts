// CMF L/R Test: plays a left-only sound in each of Dirac's speaker tunings.
// Nothing's audio service (com.nothing.audioeffect) picks the tuning from the
// name of the process that plays: it must contain an entry of its built-in
// lists (music, video, navigation, game, checked in that order). Each test
// screen runs in its own process, named so it lands in one tuning.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.tiimoo.cmfstereo.modetest"
    compileSdk = 35
    ndkVersion = "27.2.12479018"

    defaultConfig {
        applicationId = "com.tiimoo.cmfstereo.modetest"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

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
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.findByName("release")
                ?: signingConfigs.getByName("debug")
        }
    }

    // The AAudio player (a game's low-latency path is native code only).
    externalNativeBuild {
        ndkBuild { path = file("src/main/jni/Android.mk") }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

// SyncAnalysis is plain Kotlin: its unit tests run on the computer
// (./gradlew :modetest:testDebugUnitTest), on made-up recordings and on real ones.
dependencies {
    testImplementation("junit:junit:4.13.2")
}

// A real recording for SyncAnalysisTest: -Psync.wav=/path/cmf-sync.wav
tasks.withType<Test>().configureEach {
    (project.findProperty("sync.wav") as String?)?.let { systemProperty("sync.wav", it) }
}
