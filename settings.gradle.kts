pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        // The Xposed API (compile-only) for the Nothing Settings hook.
        maven { url = uri("https://api.xposed.info/") }
    }
}

rootProject.name = "CMF Stereo Manager"
include(":app")
include(":modetest")
