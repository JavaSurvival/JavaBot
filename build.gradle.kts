import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application

    kotlin("jvm")
    kotlin("plugin.serialization")

    id("com.github.jakemarsden.git-hooks")
    id("com.github.johnrengelman.shadow")
    id("io.gitlab.arturbosch.detekt")
    id("com.google.devtools.ksp")
}

group = "javasurvival"
version = "1.2-SNAPSHOT"

repositories {
    // You can remove this if you're not testing locally-installed KordEx builds
    mavenLocal()
    mavenCentral()

    maven {
        name = "Sonatype Snapshots"
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }

    maven {
        name = "Kotlin Discord"
        url = uri("https://maven.kotlindiscord.com/repository/maven-public/")
    }

    maven {
        name = "QuiltMC"
        url = uri("https://maven.quiltmc.org/repository/snapshot/")
    }
}

dependencies {
    detektPlugins(libs.detekt)

    ksp(libs.kordex.annotationProcessor)

    implementation(libs.kordex.annotations)
    implementation(libs.kordex.core)

    implementation(libs.jansi)
    implementation(libs.logback)
    implementation(libs.logging)
    implementation(libs.groovy)

    implementation(platform(libs.kotlin.bom))
    implementation(libs.kotlin.stdlib)
    implementation(libs.kx.ser)

    implementation(libs.kordx.emoji)
}

application {
    // This is deprecated, but the Shadow plugin requires it
    mainClassName = "javasurvival.AppKt"
}

gitHooks {
    setHooks(
        mapOf("pre-commit" to "detekt")
    )
}

// If you don't want the import, remove it and use org.jetbrains.kotlin.gradle.tasks.KotlinCompile
tasks.withType<KotlinCompile> {
    // Current LTS version of Java
    kotlinOptions.jvmTarget = "16"

    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "javasurvival.AppKt"
        )
    }
}

java {
    // Current LTS version of Java
    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16
}

detekt {
    buildUponDefaultConfig = true
    config = rootProject.files("detekt.yml")
}

sourceSets {
    main {
        java {
            srcDir(file("$buildDir/generated/ksp/main/kotlin/"))
        }
    }

    test {
        java {
            srcDir(file("$buildDir/generated/ksp/test/kotlin/"))
        }
    }
}
