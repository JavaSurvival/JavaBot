pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
    }

    plugins {
        kotlin("plugin.serialization") version "1.5.10"
        id("com.google.devtools.ksp") version "1.5.10-1.0.0-beta02"
    }
}

rootProject.name = "javabot"
