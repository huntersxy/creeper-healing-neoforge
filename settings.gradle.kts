pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.parchmentmc.org") { name = "ParchmentMC" }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("dev.kikugie.stonecutter") version "0.9.7"
}

rootProject.name = "CreeperHealing"

stonecutter {
    create(rootProject) {
        listOf("1.21.1", "1.21.8", "1.21.10").forEach { version ->
            version("$version-neoforge", version).buildscript = "build.neoforge.gradle.kts"
        }
        vcsVersion = "1.21.1-neoforge"
    }
}