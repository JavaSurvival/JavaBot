pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
    }

    plugins {
        kotlin("jvm") version "1.8.20"
        kotlin("plugin.serialization") version "1.8.20"

        id("com.google.devtools.ksp") version "1.8.20-1.0.11"
        id("com.github.jakemarsden.git-hooks") version "0.0.1"
        id("com.github.johnrengelman.shadow") version "5.2.0"
        id("io.gitlab.arturbosch.detekt") version "1.22.0"
    }
}

rootProject.name = "javabot"

enableFeaturePreview("VERSION_CATALOGS")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("libs.versions.toml"))
        }
    }
}