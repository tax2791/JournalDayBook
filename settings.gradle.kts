@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
        }
        mavenLocal()
        flatDir {
            dirs("libs")
        }
        gradlePluginPortal()
    }
    plugins {
        id("org.jetbrains.kotlin.android") version "2.2.21" apply false
        id("org.jetbrains.kotlin.plugin.compose") version "2.2.21" apply false
        id("com.google.dagger.hilt.android") version "2.57.2" apply false
        id("com.google.devtools.ksp") version "2.3.6" apply false
        id("com.google.gms.google-services") apply false
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "My Application"
include(":app")
