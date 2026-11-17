plugins {
    kotlin("jvm") version "2.4.21"
    id("org.jetbrains.intellij.platform")
}

group = "io.hamlook.aetheria"
version = "0.1.5"

kotlin { jvmToolchain(21) }

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.3")
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.kotlin")
        pluginVerifier()
    }
}

intellijPlatform {
    pluginConfiguration {
        name = "Aetheria Multi-Version"
        version = project.version.toString()
        description = "Edit and validate Essential Gradle Toolkit multi-version sources."
        ideaVersion { sinceBuild = "243" }
    }
}

tasks.patchPluginXml {
    sinceBuild.set("243")
}
