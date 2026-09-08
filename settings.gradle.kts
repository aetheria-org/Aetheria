import io.hamlook.aetheria.sharedvariables.ProjectTarget

pluginManagement {
    // FORCES JAVA 21 COMPLIANT GSON INTO THE CLASSLOADER BEFORE LOOM INITIALIZES
    buildscript {
        repositories {
            mavenCentral()
        }
        dependencies {
            constraints {
                classpath("com.google.code.gson:gson:2.10.1") {
                    because("Fixes the Gson 2.9.1 ReflectionAccessFilter crash on Java 21")
                }
            }
        }
    }

    includeBuild("sharedVariables")

    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://repo.essential.gg/repository/maven-public/")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://maven.architectury.dev/")
        maven("https://maven.fabricmc.net")
        maven("https://maven.minecraftforge.net/")
        maven("https://files.minecraftforge.net/maven/")
        maven("https://repo.spongepowered.org/maven/")
        maven("https://jitpack.io")
    }
    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "gg.essential.loom" -> useModule("gg.essential:architectury-loom:${requested.version}")
                "dev.architectury.loom" -> useModule("dev.architectury:architectury-loom:${requested.version}")
                else -> if (requested.id.id.startsWith("gg.essential.")) {
                    useModule("com.github.aetheria-org.essential-gradle-toolkit:${requested.id.id}.gradle.plugin:0.7.2kotlin.1")
                }
            }
        }
    }
    plugins {
        id("gg.essential.multi-version.root") version "0.7.2kotlin.1"
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.6.0")
    id("io.hamlook.aetheria.shared-variables")
}

rootProject.name = "Aetheria"
rootProject.buildFileName = "root.gradle.kts"

ProjectTarget.values().forEach { target ->
    include(target.projectPath)
    project(target.projectPath).apply {
        projectDir = file("versions/${target.projectName}")
        buildFileName = "../../build.gradle.kts"
    }
}
