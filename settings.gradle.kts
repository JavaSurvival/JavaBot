pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
    }

    plugins {
        kotlin("jvm") version "1.9.24"
        kotlin("plugin.serialization") version "1.9.24"

        id("com.google.devtools.ksp") version "1.9.24-1.0.20"
        id("com.github.jakemarsden.git-hooks") version "0.0.1"
        id("com.gradleup.shadow") version "8.3.5"
        id("io.gitlab.arturbosch.detekt") version "1.22.0"
    }
}

rootProject.name = "javabot"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("libs.versions.toml"))
        }
    }
}